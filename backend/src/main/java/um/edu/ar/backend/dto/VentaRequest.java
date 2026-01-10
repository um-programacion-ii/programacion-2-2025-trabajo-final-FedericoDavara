package um.edu.ar.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class VentaRequest {
    private Long eventoId;
    private Long usuarioId;
    private List<AsientoDTO> asientos;
}
