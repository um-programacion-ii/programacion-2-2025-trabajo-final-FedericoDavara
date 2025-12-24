package um.edu.ar.proxy.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import um.edu.ar.proxy.service.NotificationService;
import um.edu.ar.proxy.redis.RedisSeatManager;

import java.util.Map;

/**
 * Consumer simple del topic de cambios. Realiza dedupe (pendiente) y notifica backends.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final RedisSeatManager redisSeatManager;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.events:eventos-changes}", groupId = "${kafka.groupId:proxy-group}")
    public void listen(String message) {
        try {
            Map<?,?> map = objectMapper.readValue(message, Map.class);
            String tipo = (String) map.get("tipo");
            Long eventoId = map.get("eventoId") == null ? null : Long.valueOf(map.get("eventoId").toString());
            String changeId = map.get("changeId") == null ? null : map.get("changeId").toString();

            log.info("Kafka message tipo={} eventoId={} changeId={}", tipo, eventoId, changeId);

            // TODO: dedupe por changeId (guardar en tabla incoming_events)
            // Si es ASIENTO_ACTUALIZADO -> podríamos actualizar Redis si el payload incluye asientos.
            // Para este skeleton solo notifiacamos al backend.
            notificationService.enqueueEventSale(eventoId, null);

        } catch (Exception e) {
            log.error("Error procesando mensaje kafka", e);
        }
    }
}
