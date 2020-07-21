package com.ironeaglex.SBS1KafkaConnector;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLfSerializer;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@EnableIntegration
@IntegrationComponentScan
@Configuration
public class IntegrationConfiguration {

    protected static Logger logger = LoggerFactory.getLogger(IntegrationConfiguration.class);

    private static final String SBS1_MSG_JSON_TEMPLATE =
        "{\"messageType\":\"%1$s\"," +
        "\"transmissionType\":\"%2$s\"," +
        "\"sessionId\":\"%3$s\"," +
        "\"aircraftId\":\"%4$s\"," +
        "\"hexIdent\":\"%5$s\"," +
        "\"flightId\":\"%6$s\"," +
        "\"generatedDate\":\"%7$s\"," +
        "\"generatedTime\":\"%8$s\"," +
        "\"loggedDate\":\"%9$s\"," +
        "\"loggedTime\":\"%10$s\"," +
        "\"callsign\":\"%11$s\"," +
        "\"altitude\":\"%12$s\"," +
        "\"groundSpeed\":\"%13$s\"," +
        "\"track\":\"%14$s\"," +
        "\"latitude\":\"%15$s\"," +
        "\"longitude\":\"%16$s\"," +
        "\"verticalRate\":\"%17$s\"," +
        "\"squawk\":\"%18$s\"," +
        "\"alert\":\"%19$s\"," +
        "\"emergency\":\"%20$s\"," +
        "\"spi\":\"%21$s\"," +
        "\"isOnGround\":\"%22$s\"}";

    @Autowired
    private Sbs1FeedProperties sbs1feed;

    @Bean
    public AbstractClientConnectionFactory clientCF() {
        TcpNetClientConnectionFactory factory = new TcpNetClientConnectionFactory(sbs1feed.getHost(), sbs1feed.getPort());
        factory.setSingleUse(false);
        factory.setSoKeepAlive(true);
        factory.setSoTimeout(10000);
        factory.setLookupHost(false);
        factory.setDeserializer(new ByteArrayLfSerializer());
        return factory;
    }

    @Bean
    public TcpInboundGateway tcpInGate(AbstractClientConnectionFactory connectionFactory) {
        TcpInboundGateway inGate = new TcpInboundGateway();
        inGate.setConnectionFactory(connectionFactory);
        inGate.setClientMode(true);
        inGate.setRequestChannel(fromTcp());
        return inGate;
    }

    @Bean
    public MessageChannel fromTcp() {
        return new DirectChannel();
    }

    @MessageEndpoint
    public static class KafkaServiceEndpoint {

        @Autowired
        private KafkaProperties kafka;

        @Transformer(inputChannel = "fromTcp", outputChannel = "toSBS1Parser")
        public String convert(byte[] bytes) {
            return new String(bytes);
        }

        @Transformer(inputChannel = "toSBS1Parser", outputChannel = "toKafka")
        public Message<String> parse(String csv) {
            // Get rid of any CRLF characters on the end of the string
            csv = csv.trim();

            // Ensure there are 22 fields for the template
            Object[] parts = new Object[22];
            Arrays.fill(parts, "");
            Object[] temp = csv.split(",");
            System.arraycopy(temp, 0, parts, 0, temp.length);

            // Build the payload from the template
            String payload = String.format(SBS1_MSG_JSON_TEMPLATE, parts);

            // Build a key of the message type, transmission type and hex identifier
            String key = parts[0]+"-"+parts[1]+"-"+parts[4];

            return MessageBuilder
                .withPayload(payload.toString())
                .setHeader(KafkaHeaders.MESSAGE_KEY, key)
                .build();
        }

        @ServiceActivator(inputChannel = "toKafka")
        @Bean
        public MessageHandler handler(KafkaTemplate<String, String> kafkaTemplate) {
            logger.info("Creating Kafka MessageHandler");
            KafkaProducerMessageHandler<String, String> handler =
                    new KafkaProducerMessageHandler<>(kafkaTemplate);
            handler.setTopicExpression(new LiteralExpression(this.kafka.getTopic()));
            return handler;
        }
    }

}