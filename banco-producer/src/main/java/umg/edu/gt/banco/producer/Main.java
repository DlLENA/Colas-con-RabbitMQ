package umg.edu.gt.banco.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import umg.edu.gt.banco.producer.models.LoteTransacciones;
import umg.edu.gt.banco.producer.models.Transaccion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String API_URL = "https://hly784ig9d.execute-api.us-east-1.amazonaws.com/default/transacciones";
    private static final Set<String> processedIds = ConcurrentHashMap.newKeySet();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String rabbitHost = System.getenv().getOrDefault("RABBIT_HOST", "localhost");
        int rabbitPort = Integer.parseInt(System.getenv().getOrDefault("RABBIT_PORT", "5672"));
        String rabbitUser = System.getenv().getOrDefault("dilena13grijalva@gmail.com", "guest");
        String rabbitPassword = System.getenv().getOrDefault("123456", "guest");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
        factory.setPort(rabbitPort);
        factory.setUsername(rabbitUser);
        factory.setPassword(rabbitPassword);

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            System.out.println("✅ Producer con PRIORIDADES iniciado...");
            while (true) {
                int published = fetchAndPublish(client, channel);
                System.out.println("🔄 Ciclo completado. Publicadas: " + published);
                TimeUnit.SECONDS.sleep(10);
            }
        }
    }

    private static int fetchAndPublish(HttpClient client, Channel channel) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL)).GET().header("Content-Type", "application/json").build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) return 0;

            LoteTransacciones lote = mapper.readValue(response.body(), LoteTransacciones.class);
            if (lote.getTransacciones() == null || lote.getTransacciones().isEmpty()) return 0;

            int published = 0;
            for (Transaccion tx : lote.getTransacciones()) {
                String id = tx.getIdTransaccion();
                String bank = tx.getBancoDestino();

                if (id == null || bank == null || !processedIds.add(id)) continue;
                bank = bank.toUpperCase().trim();

                // esto onfigurara la cola para que soporte prioridades máximo de 10
                Map<String, Object> args = new HashMap<>();
                args.put("x-max-priority", 10);
                channel.queueDeclare(bank, true, false, false, args);

                // aqui vamos a asignarle prioridad basada en el monto Mayor a 10,000 = Alta
                int prioridadAsignada = (tx.getMonto() > 10000) ? 10 : 1;
                
                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                        .priority(prioridadAsignada)
                        .build();

                byte[] payload = mapper.writeValueAsBytes(tx);
                
                // Publicamos enviando las propiedades props
                channel.basicPublish("", bank, props, payload);
                published++;
            }
            return published;
        } catch (Exception ex) {
            return 0;
        }
    }
}