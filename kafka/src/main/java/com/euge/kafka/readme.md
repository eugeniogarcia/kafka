# Introducción

Tenemos una aplicación spring que va a producir y consumir mensajes en Kafka, al mismo tiempo.

## application.properties

Definimos una serie de propiedades que vamos a utilizar en la aplicación. Tenemos la direccion del servidor de kafka, y los nombres de los cuatro topics que vamos a utilizar:

```txt
kafka.bootstrapAddress=localhost:32768
message.topic.name=baeldung
greeting.topic.name=greeting
filtered.topic.name=filtered
partitioned.topic.name=partitioned
```

## Greeting.java

Simplemente declara una clase que usaremos como payload para demostrar que podemos enviar y recibir mensajes de kafka que serializamos/deserealizamos como json string

## KafkaTopicConfig.java

Los topics que la aplicación va a utilizar se crean de forma dinámica a medida que tenemos un consumidor o un productor que se conecte a kafka y haga referencia a ellos (esto porque hemos configurado así nuestro servidor kafka). Cuando esto suceda el topic se creará con una serie de propiedades por defecto.

En esta clase config lo que vamos a hacer es configurar explicitamente un topic en el servidor, con unas características concretas, de modo que se "creará" de antemano, antes de que haya un consumidor o un productor utilizandolo.

Con esta bean creamos el topic:

```java
@Bean
public NewTopic topic1() {
    return new NewTopic("partitioned", 5, (short) 1);
}
```

El topic que estamos creando se llama `partitioned` y tiene 5 particiones

Indicamos en que servidor crear el topic con otra bean:

```java
@Bean
public KafkaAdmin kafkaAdmin() {
    final Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    return new KafkaAdmin(configs);
}
```

## KafkaProducerConfig.java

Con esta clase de configuración configuraremos nuestros producers. Los aspectos claves que hay que considerar son:
- Definir una Factoría con las características del productor. Indicaremos el tipo del key y el value, así como los serializadores a utilizar
- Definir un template a utilizar con la Factoria. El template nos permitirá enviar mensajes, producirlos

En esta clase de configuración vamos a definir dos productores, uno con un topico <string,string>, y otro que enviará <string,Greetings>

### Factoria

En la factoria indicamos el key y el value que se usara:

```java
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
```

En esta bean hemos creado una <string,string>. En esta otra usamos un string y un serializador de Json. Esto nos permitirá pasar nuestra clase Greeting y la factoría la serializará a json:

```java
@Bean
public ProducerFactory<String, Greeting> greetingProducerFactory() {
    final Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    //El valor no es un string en este productor, sino un JSON
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(configProps);
}
```

### Template

Finalmente definimos la bean que implementa la plantilla para producir los mensajes. __Nótese como la plantilla hace uso de la factoría__:

```java
@Bean
public KafkaTemplate<String, Greeting> greetingKafkaTemplate() {
    return new KafkaTemplate<>(greetingProducerFactory());
}
```

## KafkaConsumerConfig.java

En esta clase de configuración vamos a definir como consumir mensajes de un servidor Kafka. Los mensajes se consumen con un Listener que se subscribirá a un topic, y opcionalmente a un groupid. Todos los consumidores que se conecten al mismo groupid consumiran los mensajes publicados en el topic de forma independiente de otros consumidores conectados a otro groupid. Los mensajes solo será consumidos una vez por un consumidor dentro de un mismo groupid.

Como en el caso anterior tendremos dos elementos, la factoria y el listener (que hace las veces del template en el caso del consumidor).

### Factoria

En la factoria indicamos el servidor, el __groupid__, y los serializadores para el key value. En este ejemplo esperamos <string,string>:

```java
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
```

En esta otra factoria esperamos un string y un json:

```java
public ConsumerFactory<String, Greeting> greetingConsumerFactory() {
    final Map<String, Object> props = new HashMap<>();
    //Dirección del nodo
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    //Usaremos este grupo para el consumidor. Es otro grupo diferente
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "greeting");
    //Serializadores para la clave y el valor. Esperamos un string en la clave, pero un JSON en el valor
    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(Greeting.class));
}
```

### Listener Container Factory

En la Listener Container Factory indicamos que factoria utilizar (como se desiarilizarán los key values):

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> barKafkaListenerContainerFactory() {
    final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory("bar"));
    return factory;
}
```

en este otro caso estamos además especificando un filtro, de modo que ciertos mensajes serán excluidos de los consumidores:

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> filterKafkaListenerContainerFactory() {
    final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory("filter"));
    //Filtra de modo que solo se consumiran por aqui mensajes que contengan el valor "World"
    factory.setRecordFilterStrategy(record -> record.value()
            .contains("World"));
    return factory;
}
```

## Aplicacion

Obtenemos directamente las dos beans que usaremos para producir y consumir mensajes - en lugar de inyectarlas:

```java
final MessageProducer producer = context.getBean(MessageProducer.class);
final MessageListener listener = context.getBean(MessageListener.class);
```

Estas dos beans se han declarado en la propia aplicación

```java
@Bean
public MessageProducer messageProducer() {
    return new MessageProducer();
}

@Bean
public MessageListener messageListener() {
    return new MessageListener();
}
```

### Producer

En el producer inyectamos la bean con el template kafka a una variable:

```java
@Autowired
private KafkaTemplate<String, String> kafkaTemplate;
@Autowired
private KafkaTemplate<String, Greeting> greetingKafkaTemplate;
```

inyectamos las propiedades desde nuestro archivo de configuración:

```java
// Tenemos varios topicos
@Value(value = "${message.topic.name}")
private String topicName;

@Value(value = "${partitioned.topic.name}")
private String partionedTopicName;

@Value(value = "${filtered.topic.name}")
private String filteredTopicName;

@Value(value = "${greeting.topic.name}")
private String greetingTopicName;
```

y se declaran los helpers para producir mensajes:

- envio de un mensaje a un topico:

```java
@SuppressWarnings("null")
public void sendMessage(String message) {
    kafkaTemplate.send(topicName, message);
}
```

- envio de un mensaje a un topico particionado. Indicamos cual es la key y kafka determinará cual es la partición destino a partir del valor de la key:

```java
@SuppressWarnings("null")
public void sendMessageToPartioned(String key, String message) {
    kafkaTemplate.send(partionedTopicName, key, message);
}
```

- envio de un mensaje a una particion concreta de un topico - en lugar de indicar una key y que kafka seleccione la partición a partir de la key, forzamos que se envie a una partición concreta:

```java
@SuppressWarnings("null")
public void sendMessageToPartion(String message, int partition) {
    kafkaTemplate.send(partionedTopicName, partition, null, message);
}
```

### Listener

Para consumir mensajes crearemos unos listener que estarán continuamente monitorizando la llegada de nuevos mensajes a Kafka para consumirles.

El listener tiene que:

- indicar el topic al que queremos subscribirnos
- la factoría que vamos a utilizar. En la factoría estamos indicando:
    - el __groupid__ que identifica al listener. Podemos tener varios programas (procesos, hilos, aplicaciones) consumiendo mensajes de un mismo topic. Todos aquellos que esten identificados con el mismo grouid se tratan como parte de un mismo grupo, de modo que se garantice que un determinado mensaje solo sea consumido por uno de los participantes del grupo
    - __serializadores__ para el key y el valor
    - __filtros__ a aplicar
    - direcion del __broker server__

```java
@KafkaListener(topics = "${greeting.topic.name}", containerFactory = "greetingKafkaListenerContainerFactory")
public void greetingListener(Greeting greeting) {
    System.out.println("------------------------------");
    System.out.println("Recieved greeting message: " + greeting);
    System.out.println("------------------------------");
    this.greetingLatch.countDown();
}
```

Podemos también indicar en la anotación el __groupid__.

### Otros

En la aplicación se define un Latch, en este ejemplo inicializado a 3.

```java
private final CountDownLatch latch = new CountDownLatch(3);
```

con `latch.countDown()` decrementamos el valor del latch. Si el valor resultante es mayor que cero, el thread se bloquea, hasta que valor llegue a cero. Si el valor resultante es cero, la ejecución continua al tiempo que se desbloquean otros threads que pudieran estar bloqueados. Si el latch ya estaba en cero, no sucede nada, la ejecución continua.