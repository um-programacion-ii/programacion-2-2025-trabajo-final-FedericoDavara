package um.edu.ar.proxy.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import um.edu.ar.proxy.dto.*;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.script.DefaultRedisScript;


import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Manager encargado de:
 *  - bloquear asientos atómicamente (LUA)
 *  - validar bloqueos
 *  - marcar vendidos (escribir JSON en hash)
 *  - liberar asientos (eliminar campo => libre)
 *
 * Redis schema:
 *   Hash key: evento:{eventoId}:asientos
 *   Field: "fila:columna" -> JSON string { estado: "BLOQUEADO"| "VENDIDO", blockedBy, sessionId, blockedUntil, ventaId }
 *
 * Nota del profesor: Redis solo almacena los asientos BLOQUEADOS o VENDIDOS.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSeatManager {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String blockScript;

    @PostConstruct
    private void init() {
        try {
            ClassPathResource res = new ClassPathResource("lua/block_seats.lua");
            blockScript = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Cannot load lua script", e);
            throw new RuntimeException(e);
        }
    }

    private String keyForEvent(Long eventoId) {
        return "evento:" + eventoId + ":asientos";
    }

    public BloqueoResponse blockSeats(Long eventoId, List<SeatDto> seats, String usuario, String sessionId) {

        String key = keyForEvent(eventoId);
        String blockedUntil = Instant.now().plusSeconds(300).toString(); // 5 minutos

        List<String> args = new ArrayList<>();
        args.add(usuario);
        args.add(sessionId);
        args.add(blockedUntil);
        for (SeatDto s : seats) {
            args.add(s.toKey());
        }

        RedisScript<String> redisScript =
                new DefaultRedisScript<>(blockScript, String.class);

        Object eval = redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                args.toArray()
        );

        try {
            String json = (eval == null)
                    ? "{\"resultado\":false,\"descripcion\":\"Error ejecutando LUA\"}"
                    : eval.toString();

            Map<?, ?> map = objectMapper.readValue(json, Map.class);

            Object resultadoObj = map.get("resultado");
            boolean resultado = resultadoObj != null &&
                    Boolean.parseBoolean(resultadoObj.toString());

            BloqueoResponse resp = new BloqueoResponse();
            resp.setResultado(resultado);
            resp.setEventoId(eventoId);

            if (resultado) {
                resp.setDescripcion("Asientos bloqueados correctamente");
                resp.setBlockedUntil(blockedUntil);
            } else {
                Object descObj = map.get("descripcion");
                resp.setDescripcion(
                        descObj != null ? descObj.toString() : "Bloqueo fallido"
                );
            }

            return resp;

        } catch (Exception e) {
            log.error("Error procesando respuesta LUA", e);
            BloqueoResponse error = new BloqueoResponse();
            error.setResultado(false);
            error.setDescripcion("Error interno procesando bloqueo");
            return error;
        }
    }


    public boolean validateBlockedBy(Long eventoId, List<VentaRequest.VentaSeat> seats, String sessionId, String usuario) {
        String key = keyForEvent(eventoId);
        for (VentaRequest.VentaSeat vs : seats) {
            String field = vs.getFila() + ":" + vs.getColumna();
            Object valObj = redisTemplate.opsForHash().get(key, field);
            if (valObj == null) return false; // not stored => LIBRE => invalid
            String value = valObj.toString();
            try {
                Map<?, ?> obj = objectMapper.readValue(value, Map.class);
                String estado = (String) obj.get("estado");
                String sess = obj.get("sessionId") == null ? null : obj.get("sessionId").toString();
                String blockedBy = obj.get("blockedBy") == null ? null : obj.get("blockedBy").toString();
                if (!"BLOQUEADO".equalsIgnoreCase(estado)) return false;
                if (!Objects.equals(sess, sessionId) && !Objects.equals(blockedBy, usuario)) return false;
                String blockedUntil = obj.get("blockedUntil") == null ? null : obj.get("blockedUntil").toString();
                if (blockedUntil != null && Instant.parse(blockedUntil).isBefore(Instant.now())) return false;
            } catch (Exception e) {
                log.error("Error parsing redis value", e);
                return false;
            }
        }
        return true;
    }

    public void markAsSold(Long eventoId, List<VentaRequest.VentaSeat> seats, Long ventaId) {
        String key = keyForEvent(eventoId);
        for (VentaRequest.VentaSeat vs : seats) {
            String field = vs.getFila() + ":" + vs.getColumna();
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("estado", "VENDIDO");
            obj.put("blockedBy", null);
            obj.put("sessionId", null);
            obj.put("blockedUntil", null);
            obj.put("ventaId", ventaId);
            try {
                redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(obj));
            } catch (Exception e) {
                log.error("Error marking sold", e);
            }
        }
    }

    public void releaseSeats(Long eventoId, List<VentaRequest.VentaSeat> seats) {
        String key = keyForEvent(eventoId);
        for (VentaRequest.VentaSeat vs : seats) {
            String field = vs.getFila() + ":" + vs.getColumna();
            redisTemplate.opsForHash().delete(key, field);
        }
    }

    @SuppressWarnings("unchecked")
    public AsientosResponse getAsientosForEvent(Long eventoId) {
        String key = keyForEvent(eventoId);
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        AsientosResponse res = new AsientosResponse();
        res.setEventoId(eventoId);
        List<AsientosResponse.AsientoInfo> list = new ArrayList<>();
        for (var entry : map.entrySet()) {
            String field = entry.getKey().toString();
            String value = entry.getValue().toString();
            try {
                Map<?, ?> obj = objectMapper.readValue(value, Map.class);
                var parts = field.split(":");
                AsientosResponse.AsientoInfo ai = new AsientosResponse.AsientoInfo();
                ai.setFila(Integer.parseInt(parts[0]));
                ai.setColumna(Integer.parseInt(parts[1]));
                ai.setEstado(obj.get("estado") == null ? null : obj.get("estado").toString());
                ai.setBlockedBy(obj.get("blockedBy") == null ? null : obj.get("blockedBy").toString());
                ai.setBlockedUntil(obj.get("blockedUntil") == null ? null : obj.get("blockedUntil").toString());
                ai.setVentaId(obj.get("ventaId") == null ? null : Long.valueOf(obj.get("ventaId").toString()));
                list.add(ai);
            } catch (Exception e) {
                log.error("Error parsing redis value for field " + field, e);
            }
        }
        res.setAsientos(list);
        res.setTimestamp(Instant.now().toString());
        // Defaults (client should get real fila/col metadata from cátedra)
        res.setFilaAsientos(50);
        res.setColumnaAsientos(50);
        return res;
    }
}
