package cl.induztek.instalaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.induztek.instalaciones.model.Instalacion;

public interface InstalacionRepository extends JpaRepository<Instalacion, Long> {

    @EntityGraph(attributePaths = {"comuna", "comuna.region"})
    List<Instalacion> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = {"comuna", "comuna.region"})
    List<Instalacion> findByClienteIdOrderByNombreAsc(Long clienteId);

    boolean existsByClienteIdAndNombreIgnoreCase(Long clienteId, String nombre);

    boolean existsByClienteIdAndNombreIgnoreCaseAndIdNot(Long clienteId, String nombre, Long id);
}
