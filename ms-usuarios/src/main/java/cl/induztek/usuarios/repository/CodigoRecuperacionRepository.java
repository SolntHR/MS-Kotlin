package cl.induztek.usuarios.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.induztek.usuarios.model.CodigoRecuperacion;
import cl.induztek.usuarios.model.Usuario;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    Optional<CodigoRecuperacion> findFirstByUsuarioAndUsadoFalseOrderByIdDesc(Usuario usuario);

    List<CodigoRecuperacion> findByUsuarioAndUsadoFalse(Usuario usuario);
}
