package com.stockalert.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.stockalert.common.AlertChangeEvent;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class KafkaConfig {

	@Value("${confluent.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${confluent.api-key}")
	private String apiKey;

	@Value("${confluent.api-secret}")
	private String apiSecret;

	@Value("${confluent.topic.alert.changes}")
	private String alertWatcherChanges;

	@Bean
	ProducerFactory<String, AlertChangeEvent> producerFactory() {
		log.info("Inside producefactory configuration");

		return new DefaultKafkaProducerFactory<>(kafkaConfiguration());
	}

	private Map<String, Object> kafkaConfiguration() {
		Map<String, Object> config = new HashMap<>();
		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
		config.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
		config.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required "
				+ "username=\"" + apiKey + "\" password=\"" + apiSecret + "\";");
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		return config;
	}

	@Bean
	NewTopic pricesTopic() {
		return TopicBuilder.name(alertWatcherChanges).partitions(3).replicas(3).build();
	}

	@Bean
	KafkaAdmin admin() {
		return new KafkaAdmin(kafkaConfiguration());
	}

	@Bean
	KafkaTemplate<String, AlertChangeEvent> priceKafkaTemplate() {
		log.info("Inside AlertChangeEvent configuration");
		return new KafkaTemplate<>(producerFactory());
	}
}