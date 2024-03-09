
```ps
podman network create my_kafka

podman run --network=my_kafka --rm --detach --name zookeeper -e ZOOKEEPER_CLIENT_PORT=2181 docker.io/confluentinc/cp-zookeeper:latest

podman run --network=my_kafka --rm --detach --name broker -p 9092:9092 -e KAFKA_BROKER_ID=1 -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 docker.io/confluentinc/cp-kafka:latest
```