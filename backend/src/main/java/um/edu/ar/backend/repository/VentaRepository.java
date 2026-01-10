package um.edu.ar.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import um.edu.ar.backend.entity.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long> {}
