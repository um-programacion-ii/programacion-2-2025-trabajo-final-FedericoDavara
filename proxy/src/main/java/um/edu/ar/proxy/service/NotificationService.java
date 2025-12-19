package um.edu.ar.proxy.service;

import org.springframework.stereotype.Service;
import um.edu.ar.proxy.dto.VentaResponse;

/**
 * Servicio que encola notificaciones webhook al backend.
 * Implementacion simplificada: stub.
 */
@Service
public class NotificationService {

    public void enqueueEventSale(Long eventoId, VentaResponse ventaResponse) {
        // TODO: implementar tabla outgoing_notifications + worker con retries.
    }
}
