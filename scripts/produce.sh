# kafkactl produce discounts --separator='|' --file topic-data/discounts
kafkactl produce discount-profiles-by-user --separator='|' --file topic-data/discount-profiles-by-user
kafkactl produce orders-by-user --separator='|' --file topic-data/orders-by-user
kafkactl produce payments --separator='|' --file topic-data/payments
