package cl.induztek.protocolo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.induztek.protocolo.model.Parametro;

public interface ParametroRepository extends JpaRepository<Parametro, Long> {
}
