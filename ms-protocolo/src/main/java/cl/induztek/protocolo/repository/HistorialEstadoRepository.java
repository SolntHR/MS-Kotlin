package cl.induztek.protocolo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.induztek.protocolo.model.EstadoProtocolo;
import cl.induztek.protocolo.model.HistorialEstado;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    @EntityGraph(attributePaths = "protocolo")
    List<HistorialEstado> findByEstadoNuevo(EstadoProtocolo estadoNuevo);
}
