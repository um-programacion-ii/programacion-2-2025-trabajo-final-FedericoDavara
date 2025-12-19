package um.edu.ar.proxy.dto;

import lombok.Data;
import java.util.List;

@Data
public class BloqueoResponse {
    private boolean resultado;
    private String descripcion;
    private Long eventoId;
    private List<SeatDto> asientos;
    private String blockedUntil;
}
