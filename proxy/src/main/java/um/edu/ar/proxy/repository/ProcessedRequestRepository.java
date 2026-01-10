package um.edu.ar.proxy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import um.edu.ar.proxy.entity.ProcessedRequest;

public interface ProcessedRequestRepository extends JpaRepository<ProcessedRequest, String> {
}
