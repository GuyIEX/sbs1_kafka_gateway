package com.ironeaglex.SBS1KafkaConnector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({KafkaProperties.class, Sbs1FeedProperties.class})
public class Sbs1KafkaConnectorApplication {

	public static void main(String[] args) {
		SpringApplication.run(Sbs1KafkaConnectorApplication.class, args);
	}

}
