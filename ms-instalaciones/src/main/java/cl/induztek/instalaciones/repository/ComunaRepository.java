package cl.induztek.instalaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.induztek.instalaciones.model.Comuna;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {

    @EntityGraph(attributePaths = "region")
    List<Comuna> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "region")
    List<Comuna> findByRegionIdOrderByNombreAsc(Long regionId);
}
