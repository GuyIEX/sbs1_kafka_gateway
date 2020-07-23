# SBS-1 (BaseStation) to Kafka Gateway

This application will connect to a decoded SBS-1 data feed, such as produced by dump1090, convert the messages into JSON then publish them to a topic. It is intended that this be running independent of other systems. See [@GuyIEX/sbs1_kafka_connector](https://github.com/GuyIEX/sbs1_kafka_connector) for a more Kafka integrated solution that uses Kafka Connect. The Kafka messages are keyed using the message type, transmission type and hex identifier. For example, here is a list of messages and their generated keys.

| Message (truncated) | Key |
| --- | --- |
| ``AIR,,333,272,AD2328,372,2020/07/21,18:05:22.301,2020/07/21,18:05:2...`` | ``AIR--AD2328``  |
| ``ID,,333,272,AD2328,372,2020/07/21,18:05:22.301,2020/07/21,18:05:22...`` | ``ID--AD2328``   |
| ``MSG,3,333,272,AD2328,372,2020/07/21,18:05:22.417,2020/07/21,18:05:...`` | ``MSG-3-AD2328`` |
| ``MSG,4,333,272,AD2328,372,2020/07/21,18:05:22.417,2020/07/21,18:05:...`` | ``MSG-4-AD2328`` |

Only the fields associated with each specific message type are included in the generated JSON. The following is an example JSON message published to Kafka.

```json
{
    "MT": "MSG",
    "TT": "3",
    "SID": "333",
    "AID": "485",
    "HEX": "A2B081",
    "FID": "585",
    "DMG": "2020/07/23",
    "TMG": "15:46:49.878",
    "DML": "2020/07/23",
    "TML": "15:46:49.878",
    "ALT": "23325",
    "LAT": "40.65258",
    "LNG": "-75.60273",
    "ALRT": "0",
    "EMER": "0",
    "SPI": "0",
    "GND": "0"
}
```

### Build

This project uses the standard Spring Boot maven wrapper. Please adjust the following command for your environment.

```
./mvnw clean package
```

### Configure

All configuration is encapsulated in the standard Spring Boot file [application.yml](src/main/resources/application.yml), but may be overridden on the command line using normal Spring Boot methods. The properties of most interest are

* **sbs1feed.host**: Hostname or IP address of the SBS-1 feed (default: 127.0.0.1)
* **sbs1feed.port**: Port of the decoded SBS-1 feed (default: 30003)
* **kafka.topic**: Kafka topic to publish messages to (default: dump1090)
* **spring.kafka.bootstrap-servers**: List of Kafka brokers (default: localhost:9092)

### Running

You must have both a decoded SBS-1 feed (like from dump1090) and a Kafka instance available. Replace the respective hostname or IP address values in the following command to have the gateway start receiving SBS-1 messages and generating Kafka messages.

```
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--sbs1feed.host=X.X.X.X,--spring.kafka.bootstrap-servers=Y.Y.Y.Y:9092"
```

You can see the JSON produced by using the Kafka console consumer. Replace the respective hostname or IP address value in the following command to connect to Kafka and view the messages from the default topic name.

```
kafka-console-consumer --bootstrap-server Y.Y.Y.Y:9092 --topic dump1090 --from-beginning
```

### Future Work

1. Add tests
1. Make the key configurable with a template

### References

* http://woodair.net/sbs/Article/Barebones42_Socket_Data.htm
* https://github.com/antirez/dump1090
* https://www.confluent.io/blog/noise-mapping-ksql-raspberry-pi-software-defined-radio/
* https://eventador.io/blog/planestream-the-ads-b-datasource/
