package um.edu.ar.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import um.edu.ar.backend.entity.Evento;
import um.edu.ar.backend.repository.EventoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventoRepository;

    public List<Evento> listarEventos() {
        return eventoRepository.findAll();
    }

    public Evento obtenerEvento(Long id) {
        return eventoRepository.findById(id).orElseThrow();
    }
}
