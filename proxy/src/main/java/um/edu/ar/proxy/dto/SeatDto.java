package um.edu.ar.proxy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Esta parte del codigo representa un asiento (fila, columna).
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatDto {
    private int fila;
    private int columna;

    public String toKey() {
        return fila + ":" + columna;
    }
}
