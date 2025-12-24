package um.edu.ar.proxy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "processed_requests")
@Data
public class ProcessedRequest {

    @Id
    private String solicitudId;

    @Column(nullable = false)
    private String tipo;

    @Column
    private Instant createdAt;
}
