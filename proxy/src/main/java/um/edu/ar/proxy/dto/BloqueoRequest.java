package um.edu.ar.proxy.dto;

import lombok.Data;
import java.util.List;

@Data
public class BloqueoRequest {
    private Long eventoId;
    private List<SeatDto> asientos;
    private String usuario;
    private String sessionId;
    private String solicitudId;
}
