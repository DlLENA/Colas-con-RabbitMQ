# 🏦 Sistema Distribuido de Transacciones Bancarias (Productor-Consumidor)

Este proyecto implementa una arquitectura empresarial basada en eventos utilizando **Java 17**, **Maven** y **RabbitMQ**. 
El sistema procesa lotes de transacciones financieras desde una API centralizada, las modela fuertemente utilizando POJOs, 
las enruta dinámicamente según el banco destino y las consume garantizando la persistencia de datos mediante confirmaciones manuales (ACK/NACK).

## 📐 Arquitectura del Sistema

El sistema aplica el patrón de diseño **Producer-Consumer** con una arquitectura de objetos limpios:

1. **API Origen (GET):** Provee el lote de transacciones simuladas.
2. **Componente A (Producer):** - Consume la API y mapea la respuesta JSON a una jerarquía de POJOs (`LoteTransacciones`, `Transaccion`, `Detalle`, `Referencias`).
   - Verifica transacciones duplicadas y extrae el `bancoDestino`.
   - Crea colas dinámicamente en RabbitMQ si el banco no existe.
   - Publica la transacción individual en formato JSON.
3. **Message Broker (RabbitMQ):** Almacena y enruta las transacciones de forma segura.
4. **Componente B (Consumer):**
   - Escucha las colas bancarias y deserializa los mensajes a POJOs.
   - Enriquece el objeto inyectando los datos de auditoría del estudiante (`nombre`, `carnet`, `correo`).
5. **API Destino (POST):** - Recibe la transacción procesada. 
   - El Consumer maneja códigos HTTP `200 OK` y `201 Created` para emitir un `basicAck` (confirmación de éxito). En caso de fallo, emite un `basicNack` para
   reencolar el mensaje y evitar la pérdida de datos.

## 📦 Estructura del Proyecto

El desarrollo está dividido en dos proyectos Maven (`banco-producer` y `banco-consumer`), ambos compartiendo los mismos modelos de datos (`models` package) 
para mantener la consistencia en la serialización/deserialización con Jackson.

## 🛠️ Tecnologías Utilizadas
* **Lenguaje:** Java 17
* **Gestor de dependencias:** Maven
* **Message Broker:** RabbitMQ 3 (Desplegado vía Docker)
* **Cliente HTTP:** `java.net.http.HttpClient` (Nativo)
* **Manejo de JSON:** FasterXML Jackson (`jackson-databind` v2.16.1)

## 🚀 Instrucciones de Ejecución

### Prerrequisitos
Tener instalado Docker, Java 17 y Eclipse IDE con Maven.

### Paso 1: Levantar RabbitMQ
Abrir una terminal o consola de comandos y ejecutar la siguiente instrucción para iniciar el contenedor de Docker con la interfaz de administración habilitada:
docker run -d --hostname mi-rabbit --name servidor-banco -p 15672:15672 -p 5672:5672 rabbitmq:3-management

### Paso 2: Ejecutar el Producer
Navegar al proyecto banco-producer en el IDE. Localizar la clase Main.java dentro del paquete principal y ejecutarla como Java Application. 
El sistema comenzará a consultar la API origen de Amazon y publicará las transacciones en RabbitMQ. Se puede verificar la creación automática de las colas ingresando a http://localhost:15672 en el navegador.

### Paso 3: Ejecutar el Consumer
Navegar al proyecto banco-consumer en el IDE. Localizar la clase Main.java y ejecutarla como Java Application. El consumidor comenzará a extraer los mensajes 
de las colas creadas por el Producer, inyectará los datos del alumno y enviará el POST a la API destino, mostrando en consola las confirmaciones de éxito y vaciando las colas.
