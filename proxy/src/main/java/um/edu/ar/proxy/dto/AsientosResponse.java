package um.edu.ar.proxy.dto;

import lombok.Data;

import java.util.List;

@Data
public class AsientosResponse {
    private Long eventoId;
    private int filaAsientos;
    private int columnaAsientos;
    private List<AsientoInfo> asientos;
    private String timestamp;

    @Data
    public static class AsientoInfo {
        private int fila;
        private int columna;
        private String estado; // BLOQUEADO | VENDIDO
        private String blockedBy;
        private String blockedUntil;
        private Long ventaId;
    }
}
