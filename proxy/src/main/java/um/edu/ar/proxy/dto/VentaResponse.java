package um.edu.ar.proxy.dto;

import lombok.Data;

@Data
public class VentaResponse {
    private Long eventoId;
    private Long ventaId;
    private String fechaVenta;
    private boolean resultado;
    private String descripcion;

    public static VentaResponse success(Long eventoId, Long ventaId, String fechaVenta) {
        VentaResponse r = new VentaResponse();
        r.eventoId = eventoId;
        r.ventaId = ventaId;
        r.fechaVenta = fechaVenta;
        r.resultado = true;
        r.descripcion = "Venta realizada con exito";
        return r;
    }

    public static VentaResponse failure(Long eventoId, String descripcion) {
        VentaResponse r = new VentaResponse();
        r.eventoId = eventoId;
        r.resultado = false;
        r.descripcion = descripcion;
        return r;
    }
}
