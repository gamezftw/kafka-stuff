package content

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.apache.kafka.common.serialization.Serde
import io.circe.Encoder
import io.circe.Decoder
import org.apache.kafka.streams
import org.apache.kafka.streams.scala.ImplicitConversions._
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.Duration
import java.time
import java.util.Properties

object KafkaStreams extends App {
  object Domain {
    type UserId = String
    type Profile = String
    type Product = String
    type OrderId = String
    type Status = String

    // {"orderId":"order1","user":"Daniel","products":[ "iPhone 13","MacBook Pro 15"],"amount":4000.0 }
    case class Order(
        orderId: OrderId,
        user: UserId,
        products: List[Product],
        amount: Double
    )
    case class Discount(
        profile: Profile,
        amount: Double
    )
    case class Payment(
        orderId: OrderId,
        status: Status
    )
  }

  object Topics {
    val OrdersByUser = "orders-by-user"
    val DiscountProfilesByUser = "discount-profiles-by-user"
    val Discounts = "discounts"
    val Orders = "orders"
    val Payments = "payments"
    val PaidOrders = "paid-orders"
  }

  import Domain._
  import Topics._
  // implicit val serdeOrder: Serde[Order] = {
  //   val serializer = (order: Order) => order.asJson.noSpaces.getBytes()
  //   val deserializer = (bytes: Array[Byte]) => {
  //     val string = new String(bytes)
  //     decode[Order](string).toOption
  //   }

  //   Serdes.fromFn[Order](serializer, deserializer)
  // }

  implicit def serde[A >: Null: Decoder: Encoder]: Serde[A] = {
    val serializer = (t: A) => t.asJson.noSpaces.getBytes()
    val deserializer = (bytes: Array[Byte]) => {
      val string = new String(bytes)
      // decode[A](string).toOption
      val aOrError = decode[A](string)
      println(string)
      aOrError match {
        case Right(a)    => Option(a)
        case Left(error) =>
          println(
            s"There was an error converting the message $aOrError, $error"
          )
          Option.empty
      }
    }
    streams.scala.Serdes.fromFn[A](serializer, deserializer)
  }

  // topology
  val builder = new streams.scala.StreamsBuilder()

  // KStream
  val usersOrdersStream: streams.scala.kstream.KStream[UserId, Order] =
    builder
      .stream[UserId, Order](OrdersByUser)
      .peek((key, data) => println(s"${key}, ${data}"))

  // KTable - distributed
  val usersProfilesTable: streams.scala.kstream.KTable[UserId, Profile] =
    builder.table[UserId, Profile](DiscountProfilesByUser)

  // GlobalKTable - copied to all the nodes
  val discountProfilesGTable: streams.kstream.GlobalKTable[Profile, Discount] =
    builder.globalTable[Profile, Discount](Discounts)

  // KStream transformation
  val expensiveOrders = usersOrdersStream
    .filter { (userId, order) =>
      order.amount > 1000
    }
    .peek((key, data) => println(s"${key}, ${data}"))

  val listOfProducts = usersOrdersStream
    .mapValues { order => order.products }
    .peek((key, data) => println(s"${key}, ${data}"))

  val productsStream = usersOrdersStream
    .flatMapValues(_.products)
    .peek((key, data) => println(s"${key}, ${data}"))

  // join
  val ordersWithUserProfiles = usersOrdersStream
    .join(usersProfilesTable) { (order, profile) =>
      (order, profile)
    }
    .peek((key, data) => println(s"${key}, ${data}"))

  val discountedOrdersStream =
    ordersWithUserProfiles
      .join(discountProfilesGTable)(
        { case (userId, (order, profile)) =>
          profile
        }, // key of the join - picked from the "left" stream
        { case ((order, profile), discount) =>
          order.copy(amount = order.amount - discount.amount)
        } // values of the matched records
      )
      .peek((key, data) => println(s"${key}, ${data}"))

  // pick another identifier
  val ordersStream = discountedOrdersStream
    .selectKey { (_, order) =>
      order.orderId
    }
    .peek((key, data) => println(s"${key}, ${data}"))

  val paymentStream: streams.scala.kstream.KStream[OrderId, Payment] =
    builder.stream[OrderId, Payment](Topics.Payments)
  val joinWindow =
    streams.kstream.JoinWindows.of(time.Duration.of(5, ChronoUnit.MINUTES))
  val joinOrdersPayments = (order: Order, payment: Payment) =>
    if (payment.status == "PAID") Option(order) else Option.empty[Order]

  val paidOrdersStream =
    ordersStream
      .join(paymentStream)(joinOrdersPayments, joinWindow)
      // .filter((orderId, maybeOrder) => maybeOrder.isDefined)
      .flatMapValues(maybeOrder => maybeOrder.toIterable)

  paidOrdersStream.peek((orderId, order) => {}).to(PaidOrders)

  val topology = builder.build()

  val props = new Properties()
  props.put(streams.StreamsConfig.APPLICATION_ID_CONFIG, "orders-application")
  props.put(streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:39094")
  props.put(
    streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
    streams.scala.serialization.Serdes.stringSerde.getClass
  )

  // println(topology.describe)

  val application = new streams.KafkaStreams(topology, props)

  application.start()

}
// object Main extends App {
//   List(
//     "orders-by-user",
//     "discount-profiles-by-user",
//     "discounts",
//     "orders",
//     "payments",
//     "paid-orders"
//   ).foreach { topic =>
//     println(
//       s"./kafka-topics.sh --bootstrap-server localhost:9092 --topic ${topic} --create"
//     )
//   }
// }
