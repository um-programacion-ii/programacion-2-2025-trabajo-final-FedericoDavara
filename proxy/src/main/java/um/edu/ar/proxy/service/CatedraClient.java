package um.edu.ar.proxy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import um.edu.ar.proxy.dto.VentaRequest;
import um.edu.ar.proxy.dto.VentaResponse;

/**
 * Cliente HTTP simple para el servidor de la cátedra.
 * Se puede mejorar con Resilience4j (retry/circuit-breaker) - ya incluido en pom.
 */
@Component
@RequiredArgsConstructor
public class CatedraClient {

    private final RestTemplate restTemplate;

    @Value("${catedra.base}")
    private String catedraBase;

    public VentaResponse realizarVenta(VentaRequest req) {
        String url = catedraBase + "/api/endpoints/v1/realizar-venta";
        return restTemplate.postForObject(url, req, VentaResponse.class);
    }
}
