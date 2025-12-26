package um.edu.ar.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import um.edu.ar.backend.dto.AsientoDTO;
import um.edu.ar.backend.dto.VentaRequest;
import um.edu.ar.backend.entity.AsientoVendido;
import um.edu.ar.backend.entity.Venta;
import um.edu.ar.backend.kafka.VentaProducer;
import um.edu.ar.backend.repository.AsientoVendidoRepository;
import um.edu.ar.backend.repository.VentaRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final AsientoVendidoRepository asientoRepository;
    private final VentaProducer producer;

    public void confirmarVenta(VentaRequest request) {

        Venta venta = new Venta(null,
                request.getEventoId(),
                request.getUsuarioId(),
                LocalDateTime.now());

        venta = ventaRepository.save(venta);

        for (AsientoDTO a : request.getAsientos()) {
            asientoRepository.save(
                    new AsientoVendido(null, venta.getId(), a.getFila(), a.getColumna())
            );
        }

        producer.publicarVenta(request);
    }
}

