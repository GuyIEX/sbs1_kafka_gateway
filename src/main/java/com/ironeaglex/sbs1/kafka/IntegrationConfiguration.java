/**
 * sbs1_kafka_gateway
 * Copyright (C) 2020  Iron EagleX
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ironeaglex.sbs1.kafka;

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
			// Build the payload from the template
			String payload = Sbs1Json.sbs1ToJson(csv);

			// Get rid of any CRLF characters on the end of the string
			csv = csv.trim();

			// Split the message into parts
			Object[] parts = csv.split(",");

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