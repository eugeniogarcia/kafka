package com.euge.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaProducerConfig {

	//Dirección del nodo de Kafka
	@Value(value = "${kafka.bootstrapAddress}")
	private String bootstrapAddress;

	//Configuración del Productor
	@Bean
	public ProducerFactory<String, String> producerFactory() {
		final Map<String, Object> configProps = new HashMap<>();
		//Dirección
		configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		//Serializador para la clave y el valor
		configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		return new DefaultKafkaProducerFactory<>(configProps);
	}

	//Configura el template de Kafka
	@Bean
	public KafkaTemplate<String, String> kafkaTemplate() {
		//Configura el template usando la configuración definida en la bean anterior
		return new KafkaTemplate<>(producerFactory());
	}

	//Configuración otro Productor, este usando un JSON en el formato usado en el valor
	@Bean
	public ProducerFactory<String, Greeting> greetingProducerFactory() {
		final Map<String, Object> configProps = new HashMap<>();
		configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		//El valor no es un string en este productor, sino un JSON
		configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		return new DefaultKafkaProducerFactory<>(configProps);
	}

	//Configura otro template de Kafka
	@Bean
	public KafkaTemplate<String, Greeting> greetingKafkaTemplate() {
		return new KafkaTemplate<>(greetingProducerFactory());
	}

}
