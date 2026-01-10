package um.edu.ar.backend.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VentaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publicarVenta(Object venta) {
        kafkaTemplate.send("ventas-confirmadas", venta);
    }
}
