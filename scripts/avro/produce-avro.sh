# create a topic
kafkactl create topic avro_topic
# add a schema for the topic value
# curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
# --data '{"schema": "{\"type\": \"record\", \"name\": \"LongList\", \"fields\" : [{\"name\": \"next\", \"type\": [\"null\", \"LongList\"], \"default\": null}]}"}' \
# http://localhost:8080/subjects/avro_topic-value/versions

# fixed
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
--data '{"schema":'"$(jq -Rs . < schema.avsc)"'}' \
http://localhost:8080/subjects/LongList-value/versions



# produce a message
# kafkactl produce avro_topic --value {\"next\":{\"LongList\":{}}}
kafkactl produce LongList --value $(jq -Rs . < schema.avsc)

# consume the message
kafkactl consume avro_topic --from-beginning --print-schema -o yaml
