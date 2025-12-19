package um.edu.ar.proxy.dto;

import lombok.Data;

import java.util.List;

@Data
public class VentaRequest {
    private Long eventoId;
    private String fecha;
    private Double precioVenta;
    private List<VentaSeat> asientos;
    private String solicitudId;
    private String usuario;
    private String sessionId;

    @Data
    public static class VentaSeat {
        private int fila;
        private int columna;
        private String persona;
    }
}
