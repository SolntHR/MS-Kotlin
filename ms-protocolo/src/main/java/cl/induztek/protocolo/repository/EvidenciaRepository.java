package cl.induztek.protocolo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.induztek.protocolo.model.Evidencia;

public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {

    List<Evidencia> findByProtocoloIdOrderByFechaHoraAsc(String protocoloId);
}
