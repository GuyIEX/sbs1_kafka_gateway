package com.ironeaglex.sbs1.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({KafkaProperties.class, Sbs1FeedProperties.class})
public class Sbs1KafkaGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(Sbs1KafkaGatewayApplication.class, args);
	}

}
