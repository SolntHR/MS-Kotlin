package cl.induztek.usuarios.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.usuarios.model.CodigoRecuperacion;
import cl.induztek.usuarios.model.Usuario;
import cl.induztek.usuarios.repository.CodigoRecuperacionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CodigoService {

    static final int MINUTOS_RECUPERACION = 10;
    static final int HORAS_ACTIVACION = 24;
    static final int SEGUNDOS_ENTRE_CODIGOS = 60;

    private final CodigoRecuperacionRepository codigoRepository;
    private final HashService hashService;
    private final CorreoService correoService;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public boolean puedeEnviarOtro(Usuario usuario) {
        LocalDateTime limite = LocalDateTime.now(reloj).minusSeconds(SEGUNDOS_ENTRE_CODIGOS);
        return codigoRepository.findFirstByUsuarioAndUsadoFalseOrderByIdDesc(usuario)
                .map(ultimo -> !ultimo.getCreadoEn().isAfter(limite))
                .orElse(true);
    }

    @Transactional
    public void enviarRecuperacion(Usuario usuario) {
        String codigo = generar(usuario, LocalDateTime.now(reloj).plusMinutes(MINUTOS_RECUPERACION));
        correoService.enviarCodigoRecuperacion(usuario.getCorreoPersonal(), usuario.getNombre(), codigo);
    }

    @Transactional
    public void enviarActivacion(Usuario usuario) {
        String codigo = generar(usuario, LocalDateTime.now(reloj).plusHours(HORAS_ACTIVACION));
        correoService.enviarCodigoActivacion(usuario.getCorreoPersonal(), usuario.getNombre(), usuario.getCorreo(),
                codigo);
    }

    private String generar(Usuario usuario, LocalDateTime expiraEn) {
        codigoRepository.findByUsuarioAndUsadoFalse(usuario).forEach(anterior -> {
            anterior.setUsado(true);
            codigoRepository.save(anterior);
        });
        String codigo = hashService.generarCodigo();
        String sal = hashService.generarSal();
        codigoRepository.save(CodigoRecuperacion.builder()
                .usuario(usuario)
                .codigoSal(sal)
                .codigoHash(hashService.hash(codigo, sal))
                .expiraEn(expiraEn)
                .creadoEn(LocalDateTime.now(reloj))
                .intentos(0)
                .usado(false)
                .build());
        return codigo;
    }
}
