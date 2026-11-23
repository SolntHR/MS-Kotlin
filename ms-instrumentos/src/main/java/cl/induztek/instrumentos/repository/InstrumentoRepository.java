package cl.induztek.instrumentos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.induztek.instrumentos.model.Instrumento;

public interface InstrumentoRepository extends JpaRepository<Instrumento, Long> {

    boolean existsByNumeroSerieIgnoreCase(String numeroSerie);

    boolean existsByNumeroSerieIgnoreCaseAndIdNot(String numeroSerie, Long id);
}
