package com.euge.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

//Con este helper podemos gestionar Kafka, por ejemplo, creando un topico
//El contenedor de Kafka que tenemos aqui crea automaticamente el topic en cuanto tiene actividad (hay alguien produciendo o consumiendo 
//del topic; Cuando se crea el topic por defecto se crea con las propiedades definidas por defecto en cuando a # de particiones, etc.)
//En este ejemplo el topic llamado partitiones esperamos que tenga 5 particiones (el consumidor va a tratar de leer explicitamente de 
//cada una de ellas
@Configuration
public class KafkaTopicConfig {
	@Value(value = "${kafka.bootstrapAddress}")
	private String bootstrapAddress;

	@Bean
	public KafkaAdmin kafkaAdmin() {
		final Map<String, Object> configs = new HashMap<>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		return new KafkaAdmin(configs);
	}

	@Bean
	public NewTopic topic1() {
		return new NewTopic("partitioned", 5, (short) 1);
	}
}
