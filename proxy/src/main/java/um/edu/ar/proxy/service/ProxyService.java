package um.edu.ar.proxy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import um.edu.ar.proxy.dto.*;
import um.edu.ar.proxy.entity.ProcessedRequest;
import um.edu.ar.proxy.repository.ProcessedRequestRepository;
import um.edu.ar.proxy.redis.RedisSeatManager;

import java.time.Instant;

/**
 * Lógica de negocio principal del proxy:
 * - idempotencia
 * - bloqueo (via Redis + Lua)
 * - venta (coordinación con cátedra)
 */
@Service
@RequiredArgsConstructor
public class ProxyService {

    private final ProcessedRequestRepository processedRequestRepository;
    private final RedisSeatManager redisSeatManager;
    private final CatedraClient catedraClient;
    private final NotificationService notificationService;

    public AsientosResponse getAsientos(Long eventoId) {
        return redisSeatManager.getAsientosForEvent(eventoId);
    }

    @Transactional
    public BloqueoResponse bloquear(BloqueoRequest req) {
        if (req.getSolicitudId() == null) {
            BloqueoResponse br = new BloqueoResponse();
            br.setResultado(false);
            br.setDescripcion("solicitudId es obligatorio");
            return br;
        }
        if (processedRequestRepository.existsById(req.getSolicitudId())) {
            BloqueoResponse br = new BloqueoResponse();
            br.setResultado(false);
            br.setDescripcion("Solicitud ya procesada");
            return br;
        }
        if (req.getAsientos() == null || req.getAsientos().isEmpty() || req.getAsientos().size() > 4) {
            BloqueoResponse br = new BloqueoResponse();
            br.setResultado(false);
            br.setDescripcion("Cantidad de asientos invalida (1..4)");
            return br;
        }

        BloqueoResponse resp = redisSeatManager.blockSeats(req.getEventoId(), req.getAsientos(), req.getUsuario(), req.getSessionId());

        // Guardar idempotencia (solo marca que se procesó la solicitud)
        ProcessedRequest pr = new ProcessedRequest();
        pr.setSolicitudId(req.getSolicitudId());
        pr.setTipo("BLOQUEO");
        pr.setCreatedAt(Instant.now());
        processedRequestRepository.save(pr);

        return resp;
    }

    @Transactional
    public VentaResponse venta(VentaRequest req) {
        if (req.getSolicitudId() == null) {
            return VentaResponse.failure(req.getEventoId(), "solicitudId es obligatorio");
        }
        if (processedRequestRepository.existsById(req.getSolicitudId())) {
            return VentaResponse.failure(req.getEventoId(), "Solicitud ya procesada");
        }

        boolean valid = redisSeatManager.validateBlockedBy(req.getEventoId(), req.getAsientos(), req.getSessionId(), req.getUsuario());
        if (!valid) {
            return VentaResponse.failure(req.getEventoId(), "Asientos no bloqueados por esta session/usuario");
        }

        VentaResponse cResp;
        try {
            cResp = catedraClient.realizarVenta(req);
        } catch (Exception ex) {
            // Si falla la comunicacion con catedra, liberamos los asientos (policy elegida)
            redisSeatManager.releaseSeats(req.getEventoId(), req.getAsientos());
            return VentaResponse.failure(req.getEventoId(), "Error comunicando con cátedra: " + ex.getMessage());
        }

        if (cResp.isResultado()) {
            // marcar vendidos en Redis
            redisSeatManager.markAsSold(req.getEventoId(), req.getAsientos(), cResp.getVentaId());

            // persistir idempotency
            ProcessedRequest pr = new ProcessedRequest();
            pr.setSolicitudId(req.getSolicitudId());
            pr.setTipo("VENTA");
            pr.setCreatedAt(Instant.now());
            processedRequestRepository.save(pr);

            // notificar a backends registrados (async)
            notificationService.enqueueEventSale(req.getEventoId(), cResp);

            return cResp;
        } else {
            // liberamos bloqueos si la venta fue rechazada
            redisSeatManager.releaseSeats(req.getEventoId(), req.getAsientos());
            return VentaResponse.failure(req.getEventoId(), cResp.getDescripcion());
        }
    }

    public void registerBackend(Object body) {
        // Guardar webhook info en DB - implementacion mínima (stub)
    }
}
