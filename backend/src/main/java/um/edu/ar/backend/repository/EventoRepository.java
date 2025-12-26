package um.edu.ar.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import um.edu.ar.backend.entity.Evento;

public interface EventoRepository extends JpaRepository<Evento, Long> {}
