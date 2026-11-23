package cl.induztek.instalaciones.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.induztek.instalaciones.model.Equipo;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    @EntityGraph(attributePaths = {"instalacion", "tipoEquipo"})
    @Query("""
            select e from Equipo e
            where (:instalacionId is null or e.instalacion.id = :instalacionId)
              and (:clienteId is null or e.instalacion.clienteId = :clienteId)
              and (:tipoEquipoId is null or e.tipoEquipo.id = :tipoEquipoId)
            order by e.instalacion.nombre, e.tag
            """)
    List<Equipo> buscar(@Param("instalacionId") Long instalacionId, @Param("clienteId") Long clienteId,
            @Param("tipoEquipoId") Long tipoEquipoId);

    @EntityGraph(attributePaths = {"instalacion", "tipoEquipo"})
    Optional<Equipo> findWithDetalleById(Long id);

    boolean existsByInstalacionIdAndTagIgnoreCase(Long instalacionId, String tag);

    boolean existsByInstalacionIdAndTagIgnoreCaseAndIdNot(Long instalacionId, String tag, Long id);

    long countByInstalacionId(Long instalacionId);
}
