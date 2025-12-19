package um.edu.ar.proxy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import um.edu.ar.proxy.dto.*;
import um.edu.ar.proxy.service.ProxyService;

/**
 * Endpoints expuestos al backend del alumno.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;

    @GetMapping("/asientos/{eventoId}")
    public ResponseEntity<AsientosResponse> getAsientos(@PathVariable Long eventoId) {
        AsientosResponse res = proxyService.getAsientos(eventoId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/bloquear")
    public ResponseEntity<BloqueoResponse> bloquear(@RequestBody BloqueoRequest req) {
        BloqueoResponse resp = proxyService.bloquear(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/venta")
    public ResponseEntity<VentaResponse> venta(@RequestBody VentaRequest req) {
        VentaResponse resp = proxyService.venta(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register-backend")
    public ResponseEntity<?> registerBackend(@RequestBody Object body) {
        proxyService.registerBackend(body);
        return ResponseEntity.ok().body(java.util.Map.of("resultado", true));
    }
}
