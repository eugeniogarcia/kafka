package com.euge.kafka;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

@SpringBootApplication
public class KafkaApplication {

	public static void main(String[] args) {
		final ConfigurableApplicationContext context = SpringApplication.run(KafkaApplication.class, args);

		final MessageProducer producer = context.getBean(MessageProducer.class);
		final MessageListener listener = context.getBean(MessageListener.class);

		/*
		 * Sending a Hello World message to topic 'baeldung'.
		 * Must be recieved by both listeners with group foo
		 * and bar with containerFactory fooKafkaListenerContainerFactory
		 * and barKafkaListenerContainerFactory respectively.
		 * It will also be recieved by the listener with
		 * headersKafkaListenerContainerFactory as container factory
		 */
		// Envia al topico llamado baeldung
		producer.sendMessage("Hello, World!");
		try {
			// Esperamos a que se consuma tres veces, por tres consumidores
			listener.latch.await(10, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		/*
		 * Sending message to a topic with 5 partition,
		 * each message to a different partition. But as per
		 * listener configuration, only the messages from
		 * partition 0 and 3 will be consumed.
		 */
		// Hemos creado este topic, el topic llamado partitioned, con cinco particiones.
		// Ver clase KafkaTopicConfig
		// De no haberlo hecho asi, el topic se habria creado por defecto en el momento
		// que lo usemos, y el valor
		// por defecto es crearlo con una particion, con lo habria fallado
		for (int i = 0; i < 5; i++) {
			producer.sendMessageToPartion("Hello To Partioned Topic!", i);
		}
		try {
			listener.partitionLatch.await(10, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		/*
		 * Sending message to 'filtered' topic. As per listener
		 * configuration, all messages with char sequence
		 * 'World' will be discarded.
		 */
		producer.sendMessageToFiltered("Hello Baeldung!");
		producer.sendMessageToFiltered("Hello World!");
		try {
			listener.filterLatch.await(10, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		/*
		 * Sending message to 'greeting' topic. This will send
		 * and recieved a java object with the help of
		 * greetingKafkaListenerContainerFactory.
		 */
		producer.sendGreetingMessage(new Greeting("Greetings", "World!"));
		try {
			listener.greetingLatch.await(10, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < 100; i++) {
			producer.sendMessageToPartioned(String.valueOf(i), "Mensaje " + i);
		}
		try {
			listener.partitionAllLatch.await(10, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		context.close();
	}

	@Bean
	public MessageProducer messageProducer() {
		return new MessageProducer();
	}

	@Bean
	public MessageListener messageListener() {
		return new MessageListener();
	}

	public static class MessageProducer {

		// Template con la configuración cliente que usa un string serializer para el
		// key y el valor
		@Autowired
		private KafkaTemplate<String, String> kafkaTemplate;

		// Template con la configuración cliente que usa un string serializer para el
		// key pero un Greeting serializer para el valor
		@Autowired
		private KafkaTemplate<String, Greeting> greetingKafkaTemplate;

		// Tenemos varios topicos
		@Value(value = "${message.topic.name}")
		private String topicName;

		@Value(value = "${partitioned.topic.name}")
		private String partionedTopicName;

		@Value(value = "${filtered.topic.name}")
		private String filteredTopicName;

		@Value(value = "${greeting.topic.name}")
		private String greetingTopicName;

		@SuppressWarnings("null")
		public void sendMessage(String message) {
			kafkaTemplate.send(topicName, message);
		}

		@SuppressWarnings("null")
		public void sendMessageToPartion(String message, int partition) {
			kafkaTemplate.send(partionedTopicName, partition, null, message);
		}

		@SuppressWarnings("null")
		public void sendMessageToPartioned(String key, String message) {
			kafkaTemplate.send(partionedTopicName, key, message);
		}

		@SuppressWarnings("null")
		public void sendMessageToFiltered(String message) {
			kafkaTemplate.send(filteredTopicName, message);
		}

		@SuppressWarnings("null")
		public void sendGreetingMessage(Greeting greeting) {
			greetingKafkaTemplate.send(greetingTopicName, greeting);
		}
	}

	public static class MessageListener {

		private final CountDownLatch latch = new CountDownLatch(3);

		private final CountDownLatch partitionLatch = new CountDownLatch(2);

		private final CountDownLatch filterLatch = new CountDownLatch(2);

		private final CountDownLatch greetingLatch = new CountDownLatch(1);

		private final CountDownLatch partitionAllLatch = new CountDownLatch(100);

		// Escucha el topico baeldung con el grupo foo
		@KafkaListener(topics = "${message.topic.name}", groupId = "foo", containerFactory = "fooKafkaListenerContainerFactory")
		public void listenGroupFoo(String message) {
			System.out.println("------------------------------");
			System.out.println("Received Message in group 'foo': " + message);
			System.out.println("------------------------------");
			latch.countDown();
		}

		// Escucha el topico baeldung con el grupo bar
		@KafkaListener(topics = "${message.topic.name}", groupId = "bar", containerFactory = "barKafkaListenerContainerFactory")
		public void listenGroupBar(String message) {
			System.out.println("------------------------------");
			System.out.println("Received Message in group 'bar': " + message);
			System.out.println("------------------------------");
			latch.countDown();
		}

		// Escucha en el topico baeldung, con un grupo por defecto, sin especificar
		// grupo
		@KafkaListener(topics = "${message.topic.name}", containerFactory = "headersKafkaListenerContainerFactory")
		public void listenWithHeaders(@Payload String message,
				@Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
			System.out.println("------------------------------");
			System.out.println("Received Message: " + message + " from partition: " + partition);
			System.out.println("------------------------------");
			latch.countDown();
		}

		@KafkaListener(groupId = "partitions", containerFactory = "partitionsKafkaListenerContainerFactory", topicPartitions = @TopicPartition(topic = "${partitioned.topic.name}", partitions = {
				"0", "3" }))
		public void listenToParition(@Payload String message,
				@Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
			System.out.println("------------------------------");
			System.out.println("Received Message: " + message + " from partition: " + partition);
			System.out.println("------------------------------");
			this.partitionLatch.countDown();
		}

		@KafkaListener(groupId = "partitionsAll", containerFactory = "partitionsKafkaListenerContainerFactoryAll", topics = "${partitioned.topic.name}")
		public void listenToParitionAll(@Payload String message,
				@Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
			System.out.println("------------------------------");
			System.out.println("Mensaje: " + message + " en la particion " + partition);
			System.out.println("------------------------------");
			this.partitionAllLatch.countDown();
		}

		@KafkaListener(topics = "${filtered.topic.name}", containerFactory = "filterKafkaListenerContainerFactory")
		public void listenWithFilter(String message) {
			System.out.println("------------------------------");
			System.out.println("Recieved Message in filtered listener: " + message);
			System.out.println("------------------------------");
			this.filterLatch.countDown();
		}

		@KafkaListener(topics = "${greeting.topic.name}", containerFactory = "greetingKafkaListenerContainerFactory")
		public void greetingListener(Greeting greeting) {
			System.out.println("------------------------------");
			System.out.println("Recieved greeting message: " + greeting);
			System.out.println("------------------------------");
			this.greetingLatch.countDown();
		}
	}

}
