package cl.induztek.protocolo.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.induztek.protocolo.dto.ConteoProtocolos;
import cl.induztek.protocolo.model.EstadoProtocolo;
import cl.induztek.protocolo.model.Protocolo;

public interface ProtocoloRepository extends JpaRepository<Protocolo, String> {

    @EntityGraph(attributePaths = "visita")
    @Query("""
            select p from Protocolo p
            where (:estado is null or p.estado = :estado)
              and (:equipoId is null or p.equipoId = :equipoId)
              and (:visitaId is null or p.visita.id = :visitaId)
              and (:usuarioId is null or p.usuarioRegistraId = :usuarioId)
            order by p.fechaHoraPrueba desc
            """)
    List<Protocolo> buscar(@Param("estado") EstadoProtocolo estado, @Param("equipoId") Long equipoId,
            @Param("visitaId") Long visitaId, @Param("usuarioId") Long usuarioId);

    @EntityGraph(attributePaths = "visita")
    List<Protocolo> findByVisitaIdOrderByFechaHoraPruebaAsc(Long visitaId);

    @EntityGraph(attributePaths = "visita")
    List<Protocolo> findByEquipoIdAndEstadoInOrderByFechaHoraPruebaDesc(Long equipoId,
            Collection<EstadoProtocolo> estados);

    @Query("""
            select new cl.induztek.protocolo.dto.ConteoProtocolos(p.visita.id, p.estado, count(p))
            from Protocolo p group by p.visita.id, p.estado
            """)
    List<ConteoProtocolos> contarPorVisita();

    boolean existsByVisitaIdAndEquipoId(Long visitaId, Long equipoId);

    boolean existsByNumero(String numero);

    boolean existsByProtocoloReemplazadoId(String protocoloId);

    long countByEstado(EstadoProtocolo estado);
}
