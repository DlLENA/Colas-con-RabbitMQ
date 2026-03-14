package umg.edu.gt.banco.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String API_URL = "https://hly784ig9d.execute-api.us-east-1.amazonaws.com/default/transacciones";
    private static final Set<String> processedIds = ConcurrentHashMap.newKeySet(); // Evita enviar duplicados
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        // Configuramos la conexión a RabbitMQ
        String rabbitHost = System.getenv().getOrDefault("RABBIT_HOST", "localhost");
        int rabbitPort = Integer.parseInt(System.getenv().getOrDefault("RABBIT_PORT", "5672"));
        String rabbitUser = System.getenv().getOrDefault("dilena13grijalva@gmail.com", "guest");
        String rabbitPassword = System.getenv().getOrDefault("123456", "guest");
        int pollingSeconds = Integer.parseInt(System.getenv().getOrDefault("POLLING_SECONDS", "10"));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
        factory.setPort(rabbitPort);
        factory.setUsername(rabbitUser);
        factory.setPassword(rabbitPassword);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            System.out.println("✅ Producer iniciado. RabbitMQ en " + rabbitHost + ":" + rabbitPort);

            // Bucle infinito para consultar la API cada 10 segundos
            while (true) {
                int published = fetchAndPublish(client, channel);
                System.out.println("🔄 Ciclo completado. Transacciones nuevas publicadas: " + published);
                TimeUnit.SECONDS.sleep(pollingSeconds);
            }
        }
    }

    //metodo para enviar las solicitudes de las transacciones a RabbitMQ 
    private static int fetchAndPublish(HttpClient client, Channel channel) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json") // Header solicitado en instrucciones
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
            
            if (response.statusCode() != 200) {
                System.err.println("❌ API devolvió status " + response.statusCode());
                return 0;
            }

            //  Convertimos el JSON directamente a tu clase LoteTransacciones
            LoteTransacciones lote = mapper.readValue(response.body(), LoteTransacciones.class);
            
            // Si la API no devolvió transacciones, salimos
            if (lote.getTransacciones() == null || lote.getTransacciones().isEmpty()) {
                return 0;
            }

            int published = 0;
            
            // Recorremos la lista de objetos para las Transacciones que se le enviaran RabbitMQ 
            for (Transaccion tx : lote.getTransacciones()) {
                String id = tx.getIdTransaccion();
                String bank = tx.getBancoDestino();
             

                // Validamos que vengan datos correctos
                if (id == null || id.isEmpty() || bank == null || bank.isEmpty()) {
                    continue;
                }
                
                // Limpiamos el nombre del banco por si viene con espacios
                bank = bank.toUpperCase().trim(); 

                // Verificamos si ya procesamos esta transacción antes
                if (!processedIds.add(id)) {
                    continue; 
                }

                // Al poner esto aquí, si mañana la API manda un banco nuevo, 
                // el sistema creará la cola automáticamente sin tener que cambiar el código.
                channel.queueDeclare(bank, true, false, false, null);

                // Convertimos el objeto Transaccion individual de vuelta a JSON (bytes) para enviarlo
                byte[] payload = mapper.writeValueAsBytes(tx);
                
                // Publicamos en la cola dinámica
                channel.basicPublish("", bank, null, payload);
                published++;
          
            }
            return published;
            
        } catch (Exception ex) {
            System.err.println("💥 Error en fetchAndPublish: " + ex.getMessage());
            return 0;
        }
    }
}
