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
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final Set<String> BANK_QUEUES = Set.of("BANRURAL", "GYT", "BAC", "BI");
                                                  
    private static final ObjectMapper mapper = new ObjectMapper();
    
    // URL del Endpoint POST 
    private static final String API_POST_URL = "https://7e0d9ogwzd.execute-api.us-east-1.amazonaws.com/default/guardarTransacciones";
    
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

        for (String queue : BANK_QUEUES) {
            channel.queueDeclare(queue, true, false, false, null);
            startConsumer(channel, queue);
        }

        System.out.println("✅ Sistema Consumer listo y escuchando transacciones en: " + BANK_QUEUES);
        new CountDownLatch(1).await();
    }

    private static void startConsumer(Channel channel, String queue) throws Exception {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            boolean success = processAndPostMessage(queue, delivery);
            
            // Si responde 200 -> hace ACK / Si responde error -> no confirma
            if (success) {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } else {
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
        };

        CancelCallback cancelCallback = consumerTag ->
                System.out.println("Consumer cancelado para cola " + queue + " tag=" + consumerTag);

        channel.basicConsume(queue, false, deliverCallback, cancelCallback);
    }

    private static boolean processAndPostMessage(String queue, Delivery delivery) {
        try {
            String payload = new String(delivery.getBody(), StandardCharsets.UTF_8);
            
            // Deserializa JSON a Objeto Java
            Transaccion tx = mapper.readValue(payload, Transaccion.class);

            // datos solicitados
            tx.setNombre("Dilena Grijalva");         
            tx.setCarnet("0905-24-12697");        
            tx.setCorreo("dgrijalvat1@miumg.edu.gt");
          

            String finalJson = mapper.writeValueAsString(tx);
            
            //para poder visualizar como se vera antes de enviarse a la base el inge walter 
            //System.out.println("JSON armado: " + finalJson);

            // Invoca este endpoint POST (Header: application/json)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_POST_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(finalJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Si responde 200 (o 201 Created)
            if (response.statusCode() == 200 || response.statusCode() == 201) {
               
                System.out.println("✅ Éxito para: " + tx.getNombre() + " (" + tx.getCarnet() + ") " + "(" + tx.getCorreo() + "). Enviando ACK...");
                return true;
            } else {
                System.err.println("❌ POST Fallido (HTTP " + response.statusCode() + ") para " + tx.getIdTransaccion());
                return false;
            }

        } catch (Exception ex) {
            System.err.println("💥 Error procesando mensaje: " + ex.getMessage());
            return false;
        }
    }
}

