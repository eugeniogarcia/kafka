## Arrancar Kafka

### Individual

```ps
podman network create my_kafka

podman run --network=my_kafka --rm --detach --name zookeeper -e ZOOKEEPER_CLIENT_PORT=2181 docker.io/confluentinc/cp-zookeeper:latest

podman run --network=my_kafka --rm --detach --name broker -p 9092:9092 -e KAFKA_BROKER_ID=1 -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 docker.io/confluentinc/cp-kafka:latest
```

### Cluster

Arrancar el cluster:

```ps
podman-compose up -d
```

Parar y eliminar los contenedores:

```ps
podman-compose down
```

Solo para los contenedores:

```ps
podman-compose stop
```

Reinicia los contenedores:

```ps
podman-compose restart
```

Podemos inspeccionar las propiedades de un determinado contenedor para, por ejemplo, recuperar la ip asignada:

```ps
podman inspect -f '{{ .NetworkSettings.IPAddress }}' f51b0a6f
```

#### network_mode

Podemos trabajar con dos network modes:
- bridge: se crea una red propia en el que cada contenedor y el propio host tiene asignada una ip. El nombre de cada servicio en el `docker-compose.yaml` se resuelve a la ip asignada al contenedor
- host: cuando trabajamos en modo host host y contenedores usan la misma red, ocupan el mismo espacio, y más concretamente, usan las misma ip. Esto es, a cada contenedor le tenemos que asignar puertos diferentes porque todos los contenedores usan la misma ip

En modo bridge la red en la que estan conectados los contenedores esta aislada del resto.

```ps
netstat  -b -an -f|findstr 21181
```

Hay creados dos archivos `docker-compose.yaml`. Cuando trabajamos en modo __host__, todos los contenedores usan la misma ip que el host. Los puertos usados en cada contenedor tiene que ser diferentes - porque comparten la red. Cuando trabajamos en modo __bridge__ cada contenedor tiene su propia ip, y por lo tanto, puede usar sus propios puertos. En este caso haremos port-forwarding para poder acceder al contenedor desde el exterior.

__Comentarios:__

- Los puertos en cada contenedor no estan aislados unos de otros porque no estan aislados a nivel de red. No se hace forwarding de los puertos porque el contenedor tienen la misma ip que el host
- Cada contenedor tiene su propia ip y esta aislada del resto de contenedores. Los puertos que se necesita exponer de cada contenedor solo son accesibles usando _port forwarding_


## Configuracion

### Zookeeper

El esemble de zookeeper se [configura](https://zookeeper.apache.org/doc/r3.3.3/zookeeperAdmin.html#sc_configuration):

- `ZOOKEEPER_CLIENT_PORT`. Puerto por el que el servidor de zookeeper admite conexiones de clientes - para consultar o escribir datos
- `ZOOKEEPER_SERVERS`. hostname:port1:port2. Identifica a cada servidor en el essemble de zookeeper. Se indica un hostname, el puerto, _port1_, por el que el todos los nodos se conectarán con el lider y opcionalmente, un segundo puerto, _port2_, que se utilizará en el proceso de elección del lider
- `ZOOKEEPER_TICK_TIME`. Unidad de tiempo básico de zookeeper, expresado en milisegundos.
-  `ZOOKEEPER_SYNC_LIMIT`. Tiempo expresado en _ticks_ que se da a los followers para conectarse y sincronizarse con el lider. Si se supera este tiempo, el follower se considera no disponible
- `ZOOKEEPER_INIT_LIMIT`. Tiempo expresado en _ticks_ que se da a los followers para conectarse y sincronizarse con el lider. Este tiempo se utiliza en la inicialización, es decir, cuando un follower se conecta y sincroniza con el lider por primera vez. El sync limit es el tiempo máximo que se permite que un follower este por detrás del lider antes de que se le considere no disponible en el essemble

### Kafka

Cada uno de los servidores del cluster se llama broker. Los parametros de configuración principales son:

- `KAFKA_BROKER_ID`: Id de cada broker
- `KAFKA_ZOOKEEPER_CONNECT`: Direcciones de los nodos del essemble de Zookeeper. Kafka se conectará como "cliente" al cluster de Zookeeper
- `KAFKA_LISTENERS`: is an environment variable or configuration setting that defines the listeners on which Kafka brokers accept incoming client connections. It specifies the network interface and port combinations that Kafka brokers bind to and listen on for client connections. Each listener configuration consists of a protocol, hostname, and port, separated by ://. Common protocols include PLAINTEXT, SSL, SASL_PLAINTEXT, and SASL_SSL. Kafka brokers can be configured to listen on multiple listeners simultaneously, allowing clients to connect using different network protocols or security mechanisms
- `KAFKA_ADVERTISED_LISTENERS`: It specifies the externally accessible addresses (hostname and port) that clients use to connect to Kafka brokers. This address may be different from the address used for internal communication - set in `KAFKA_LISTENERS` 
- `KAFKA_ADVERTISED_HOST_NAME`:
- `KAFKA_MESSAGE_MAX_BYTES`: 

Para más detalles sobre sobre como funcionan los listeners podemos consultar [aqui](https://www.confluent.io/blog/kafka-listeners-explained/). 

- LISTENERS: son los interfaces en los que Kafka se expone.
- ADVERTISED_LISTENERS: indica como pueden conectarse los clientes

En este ejemplo:

![caso1](.\\imagenes\\caso_1.png)

con esta configuración:

```yaml
kafka0:
  image: "confluentinc/cp-enterprise-kafka:5.2.1"
  ports:
    - '9092:9092'
    - '29094:29094'
  depends_on:
    - zookeeper
  environment:
    KAFKA_BROKER_ID: 0
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_LISTENERS: LISTENER_BOB://kafka0:29092,LISTENER_FRED://kafka0:9092,LISTENER_ALICE://kafka0:29094
    KAFKA_ADVERTISED_LISTENERS: LISTENER_BOB://kafka0:29092,LISTENER_FRED://localhost:9092,LISTENER_ALICE://never-gonna-give-you-up:29094
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: LISTENER_BOB:PLAINTEXT,LISTENER_FRED:PLAINTEXT,LISTENER_ALICE:PLAINTEXT
    KAFKA_INTER_BROKER_LISTENER_NAME: LISTENER_BOB
```

tenemos lo siguiente:
- tres listeners configurados en `KAFKA_LISTENERS` llamados LISTENER_BOB, LISTENER_FRED Y LISTENER_ALICE. Estan configurados en kafka0:29092, kafka0:9092 y kafka0:29094 respectivamente. Estos tres listeners son accesibles desde la red interna de docker
- Hay dos puertos accesibles desde el exterior, el 9092 y el 29094
- Tenemos tres listeners publicados en `KAFKA_ADVERTISED_LISTENERS`, LISTENER_BOB, LISTENER_FRED Y LISTENER_ALICE, los tres que indicamos antes. __Notese__ que LISTENER_FRED y LISTENER_ALICE estan accesibles desde localhost:9092 y never-gonna-give-you-up:29094 respectivamente
- por último, la comunicación en estos tres listeners es _PLAINTEXT_

#### General

- if you want to customize any Kafka parameters, simply add them as environment variables, e.g. in order to increase the `message.max.bytes` parameter set the environment to `KAFKA_MESSAGE_MAX_BYTES: 2000000`. To turn off automatic topic creation set `KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'false'`

- Kafka's log4j usage can be customized by adding environment variables prefixed with `LOG4J_`. These will be mapped to `log4j.properties`. For example: `LOG4J_LOGGER_KAFKA_AUTHORIZER_LOGGER=DEBUG, authorizerAppender`

#### Broker IDs

You can configure the broker id in different ways:

1. explicitly, using `KAFKA_BROKER_ID`
2. via a command, using `BROKER_ID_COMMAND`, e.g. `BROKER_ID_COMMAND: "hostname | awk -F'-' '{print $$2}'"`

#### Advertised hostname

Modify the `KAFKA_ADVERTISED_HOST_NAME` to match your docker host IP (Note: __Do not use localhost or 127.0.0.1 as the host ip if you want to run multiple brokers__)

You can configure the advertised hostname in different ways

1. explicitly, using `KAFKA_ADVERTISED_HOST_NAME`
2. via a command, using `HOSTNAME_COMMAND`, e.g. `HOSTNAME_COMMAND: "route -n | awk '/UG[ \t]/{print $$2}'"`

When using commands, make sure you review the "Variable Substitution" section in [https://docs.docker.com/compose/compose-file/](https://docs.docker.com/compose/compose-file/)

If `KAFKA_ADVERTISED_HOST_NAME` is specified, it takes precedence over `HOSTNAME_COMMAND`.

For AWS deployment, you can use the Metadata service to get the container host's IP:

```yaml
HOSTNAME_COMMAND=wget -t3 -T2 -qO-  http://169.254.169.254/latest/meta-data/local-ipv4
```

Reference: http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html

##### Injecting HOSTNAME_COMMAND into configuration

If you require the value of `HOSTNAME_COMMAND` in any of your other `KAFKA_XXX` variables, use the `_{HOSTNAME_COMMAND}` string in your variable value, i.e.:

```
KAFKA_ADVERTISED_LISTENERS=SSL://_{HOSTNAME_COMMAND}:9093,PLAINTEXT://9092
```

#### Advertised port

If the required advertised port is not static, it may be necessary to determine this programatically. This can be done with the `PORT_COMMAND` environment variable.

```yml
PORT_COMMAND: "docker port $$(hostname) 9092/tcp | cut -d: -f2
```

This can be then interpolated in any other `KAFKA_XXX` config using the `_{PORT_COMMAND}` string, i.e.:

```yml
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://1.2.3.4:_{PORT_COMMAND}
```

#### Automatically create topics

If you want to have kafka-docker automatically create topics in Kafka during creation, a `KAFKA_CREATE_TOPICS` environment variable can be added in `docker-compose.yml`.

Here is an example snippet from `docker-compose.yml`:

```yaml
environment:
  KAFKA_CREATE_TOPICS: "Topic1:1:3,Topic2:1:1:compact"
```

In this example, `Topic 1` will have 1 partition and 3 replicas, `Topic 2` will have 1 partition, 1 replica and a `cleanup.policy` set to `compact`.

If you wish to use multi-line YAML or some other delimiter between your topic definitions, override the default `,` separator by specifying the `KAFKA_CREATE_TOPICS_SEPARATOR` environment variable.

For example, `KAFKA_CREATE_TOPICS_SEPARATOR: "$$'\n'"` would use a newline to split the topic definitions. Syntax has to follow docker-compose escaping rules, and [ANSI-C](https://www.gnu.org/software/bash/manual/html_node/ANSI_002dC-Quoting.html) quoting.

#### Verifica Kafka

##### Create and verify a topic  

```bash
/bin/kafka-topics --create --bootstrap-server kafka-1:19082 --topic test --partitions 3
```

```bash
/bin/kafka-topics --describe --bootstrap-server kafka-1:19082 --topic test
```

##### Produce messages to a test topic  

```bash
/bin/kafka-console-producer --bootstrap-server kafka-1:19082 --topic test
```

##### Consume messages from a test topic  

```bash
/bin/kafka-console-consumer --bootstrap-server kafka-1:19082 --topic test --from-beginning
```

#### Rules

* No listeners may share a port number.
* An advertised.listener must be present by protocol name and port number in the list of listeners.

#### Example

The example environment below:

```yaml
HOSTNAME_COMMAND: curl http://169.254.169.254/latest/meta-data/public-hostname
KAFKA_ADVERTISED_LISTENERS: INSIDE://:9092,OUTSIDE://_{HOSTNAME_COMMAND}:9094
KAFKA_LISTENERS: INSIDE://:9092,OUTSIDE://:9094
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INSIDE:PLAINTEXT,OUTSIDE:PLAINTEXT
KAFKA_INTER_BROKER_LISTENER_NAME: INSIDE
```

Will result in the following broker config:

```yaml
advertised.listeners = OUTSIDE://ec2-xx-xx-xxx-xx.us-west-2.compute.amazonaws.com:9094,INSIDE://:9092
listeners = OUTSIDE://:9094,INSIDE://:9092
inter.broker.listener.name = INSIDE
```

It uses the AWS metada service available on each EC2 instance to find out what is the name of the vm, and uses it as hostname.

## Monitorización

Para poder observar el cluster vamos a utilizar un par de opciones:

- zoo navigator. Proporciona una imagen que escucha en el puerto 9000. Nos permite conectarnos al zookeeper y ver todos los metadatos que estan guardados

- Offset Explorer. Aplicacion que nos permite conectarnos a los brokers de Kafka y al zookeeper

## JMX

For monitoring purposes you may wish to configure JMX. Additional to the standard JMX parameters, problems could arise from the underlying RMI protocol used to connect

* java.rmi.server.hostname - interface to bind listening port
* com.sun.management.jmxremote.rmi.port - The port to service RMI requests

For example, to connect to a kafka running locally (assumes exposing port 1099)

```yaml
KAFKA_JMX_OPTS: "-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.rmi.port=1099"
JMX_PORT: 1099
```

Jconsole can now connect at ```jconsole 192.168.99.100:1099```

