package cl.induztek.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.usuarios.dto.LoginRequest;
import cl.induztek.usuarios.dto.RegistroRequest;
import cl.induztek.usuarios.dto.RegistroResponse;
import cl.induztek.usuarios.exception.CredencialesInvalidasException;
import cl.induztek.usuarios.model.Rol;
import cl.induztek.usuarios.repository.UsuarioRepository;
import cl.induztek.usuarios.service.CorreoService;
import cl.induztek.usuarios.service.HashService;
import cl.induztek.usuarios.service.RecuperacionService;
import cl.induztek.usuarios.service.UsuarioService;

@SpringBootTest
class UsuariosApplicationTests {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HashService hashService;

    @Autowired
    private RecuperacionService recuperacionService;

    @MockitoBean
    private CorreoService correoService;

    @Test
    void cargaLosUsuariosInicialesConContrasenasCifradas() {
        assertThat(usuarioRepository.count()).isEqualTo(11);
        assertThat(usuarioRepository.findAll())
                .allSatisfy(u -> {
                    assertThat(u.getPasswordHash()).hasSize(64).isNotEqualTo("123456");
                    assertThat(u.getCorreoPersonal()).isEqualTo(u.getCorreo());
                });
    }

    @Test
    void unUsuarioInicialPuedeIniciarSesionYLosFiltrosFuncionan() {
        String correo = usuarioRepository.findAll().get(0).getCorreo();

        String contrasenaCifradaEnLaApp = hashService.hashCliente(correo, "Induztek#2026");

        assertThat(usuarioService.login(new LoginRequest(correo, contrasenaCifradaEnLaApp)).correo()).isEqualTo(correo);
        assertThat(usuarioService.listar(Rol.CLIENTE, true, null)).isNotEmpty()
                .allSatisfy(u -> assertThat(u.rolCodigo()).isEqualTo("CLIENTE"));
        assertThat(usuarioService.listar(null, null, "induztek")).isNotEmpty();
    }

    @Test
    @Transactional
    void registroAsignacionDeRolYActivacionDeLaCuenta() {
        RegistroResponse registro = usuarioService.registrar(new RegistroRequest("Valentina Muñoz Pérez",
                "11.111.111-1", "vale.munoz@gmail.com", "+56 9 5555 6666", "¿Cuál es tu comida favorita?", "sushi"));
        String correoAcceso = registro.correoInterno();
        assertThat(correoAcceso).isEqualTo("vmunoz@induztek.ejemplo");
        String contrasena = hashService.hashCliente(correoAcceso, "Induztek#2027");
        assertThatThrownBy(() -> usuarioService.login(new LoginRequest(correoAcceso, contrasena)))
                .isInstanceOf(CredencialesInvalidasException.class);

        Long jefeId = usuarioRepository.findByCorreoIgnoreCase("respinoza@induztek.ejemplo").orElseThrow().getId();
        usuarioService.cambiarRol(registro.id(), Rol.TECNICO, jefeId,
                hashService.hashCliente("respinoza@induztek.ejemplo", "Induztek#2026"));

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarCodigoActivacion(eq("vale.munoz@gmail.com"), anyString(), eq(correoAcceso),
                codigo.capture());
        recuperacionService.restablecerConCodigo(correoAcceso, codigo.getValue(), contrasena);
        assertThat(usuarioService.login(new LoginRequest(correoAcceso, contrasena)).rolCodigo()).isEqualTo("TECNICO");
    }
}
