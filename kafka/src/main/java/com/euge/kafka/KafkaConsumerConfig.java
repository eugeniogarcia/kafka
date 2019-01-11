package com.euge.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

//@EnableKafka annotation is required on the configuration class to enable detection of @KafkaListener annotation 
//on spring managed beans.
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

	//Dirección del nodo de Kafka
	@Value(value = "${kafka.bootstrapAddress}")
	private String bootstrapAddress;

	//Configuración para el consumidor
	public ConsumerFactory<String, String> consumerFactory(String groupId) {
		final Map<String, Object> props = new HashMap<>();
		//Dirección del nodo
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		//Usaremos este grupo para el consumidor
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		//Serializadores para la clave y el valor. Esperamos un string
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		return new DefaultKafkaConsumerFactory<>(props);
	}

	//Consume en el grupo foo
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> fooKafkaListenerContainerFactory() {
		final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory("foo"));
		return factory;
	}

	//Consume en el grupo bar
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> barKafkaListenerContainerFactory() {
		final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory("bar"));
		return factory;
	}

	//Consume en el grupo headers
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> headersKafkaListenerContainerFactory() {
		final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory("headers"));
		return factory;
	}

	//Consume en el grupo partitions
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> partitionsKafkaListenerContainerFactory() {
		final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory("partitions"));
		return factory;
	}

	//Consume en el grupo partitions
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> partitionsKafkaListenerContainerFactoryAll() {
		final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory("partitionsAll"));
		return factory;
	}

	//Consume en el grupo filter
	//Este listener filtra mensajes
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> filterKafkaListenerContainerFactory() {
		final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory("filter"));
		//Filtra de modo que solo se consumiran por aqui mensajes que contengan el valor "World"
		factory.setRecordFilterStrategy(record -> record.value()
				.contains("World"));
		return factory;
	}

	//Configuración otro consumidor, en este caso esperamos en el valor un JSON
	public ConsumerFactory<String, Greeting> greetingConsumerFactory() {
		final Map<String, Object> props = new HashMap<>();
		//Dirección del nodo
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		//Usaremos este grupo para el consumidor. Es otro grupo diferente
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "greeting");
		//Serializadores para la clave y el valor. Esperamos un string en la clave, pero un JSON en el valor
		return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(Greeting.class));
	}

	//Consume mensajes de grupo greeting. Estos mensajes en el valor reciben un JSON
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, Greeting> greetingKafkaListenerContainerFactory() {
		final ConcurrentKafkaListenerContainerFactory<String, Greeting> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(greetingConsumerFactory());
		return factory;
	}

}
