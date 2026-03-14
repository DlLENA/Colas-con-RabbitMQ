package umg.edu.gt.banco.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import umg.edu.gt.banco.consumer.models.Transaccion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final Set<String> BANK_QUEUES = Set.of("BANRURAL", "GYT", "BAC", "BI");
    private static final Set<String> transaccionesProcesadas = ConcurrentHashMap.newKeySet();
    
    // las colas nuevas de rabbit 
    private static final String COLA_DUPLICADOS = "cola_duplicados";
    private static final String COLA_ERRORES = "cola_errores"; // esta es la nueva cola en rabbit para los errores
                                                  
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private static final String API_POST_URL = "https://7e0d9ogwzd.execute-api.us-east-1.amazonaws.com/default/guardarTransacciones";
    
    private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public static void main(String[] args) throws Exception {
        String rabbitHost = System.getenv().getOrDefault("RABBIT_HOST", "localhost");
        int rabbitPort = Integer.parseInt(System.getenv().getOrDefault("RABBIT_PORT", "5672"));
        String rabbitUser = System.getenv().getOrDefault("RABBIT_USER", "guest");
        String rabbitPassword = System.getenv().getOrDefault("RABBIT_PASSWORD", "guest");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
        factory.setPort(rabbitPort);
        factory.setUsername(rabbitUser);
        factory.setPassword(rabbitPassword);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(COLA_DUPLICADOS, true, false, false, null);
        channel.queueDeclare(COLA_ERRORES, true, false, false, null); // Creamos cola de errores 

        Map<String, Object> argsQueue = new HashMap<>();
        argsQueue.put("x-max-priority", 10);

        for (String queue : BANK_QUEUES) {
            channel.queueDeclare(queue, true, false, false, argsQueue);
            startConsumer(channel, queue);
        }

        System.out.println("✅ Sistema Consumer listo...");
        new CountDownLatch(1).await();
    }

    private static void startConsumer(Channel channel, String queue) throws Exception {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            boolean success = processAndPostMessage(queue, delivery, channel);
            // Siempre hacemos ACK aquí porque si hay error lo desviaremos manualmente adentro del método
            if (success) {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        channel.basicConsume(queue, false, deliverCallback, consumerTag -> {});
    }

    private static boolean processAndPostMessage(String queue, Delivery delivery, Channel channel) {
        try {
            String payload = new String(delivery.getBody(), StandardCharsets.UTF_8);
            Transaccion tx = mapper.readValue(payload, Transaccion.class);
            String idTx = tx.getIdTransaccion();
            
            // aqui esto leera la prioridad del mensaje y la clasificara si es alta o nomal 
            Integer prioridad = delivery.getProperties().getPriority();
            String textoPrioridad = (prioridad != null && prioridad == 10) ? "PRIORIDAD ALTA" : "PRIORIDAD NORMAL";

            System.out.println("\n---------------------------------------------------");
            System.out.println("COLA: " + queue + " | ID: " + idTx + " | " + textoPrioridad);

            if (transaccionesProcesadas.contains(idTx)) {
                channel.basicPublish("", COLA_DUPLICADOS, null, payload.getBytes(StandardCharsets.UTF_8));
                System.out.println("   -> Estado: DUPLICADA. Enviada a: " + COLA_DUPLICADOS);
                return true; 
            }

            tx.setNombre("Dilena Grijalva");         
            tx.setCarnet("0905-24-12697");        
            tx.setCorreo("dgrijalvat1@miumg.edu.gt");
            String finalJson = mapper.writeValueAsString(tx);

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_POST_URL))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(finalJson)).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                transaccionesProcesadas.add(idTx);
                System.out.println("   -> Monto: Q" + tx.getMonto());
                System.out.println("✅ POST EXITOSO");
                return true;
            } else {
                //  Si la API da error por que va a dar que le cambiaremos la url se estara enviando a la cola de errores en rabbit 
                channel.basicPublish("", COLA_ERRORES, null, payload.getBytes(StandardCharsets.UTF_8));
                System.err.println("❌ ERROR API HTTP " + response.statusCode() + ". Mensaje enviado a: " + COLA_ERRORES);
                return true; // Retornamos true para hacer ACK para sacarla de la original y enviarla a la cola de errores 
            }

        } catch (Exception ex) {
            // Si hay fallo de red o conexión caída, también a cola_errores
            try {
                channel.basicPublish("", COLA_ERRORES, null, delivery.getBody());
                System.err.println("💥 EXCEPCIÓN. Mensaje enviado a: " + COLA_ERRORES);
            } catch (Exception ignored) {}
            return true;
        }
    }
}
