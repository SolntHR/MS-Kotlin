package cl.induztek.usuarios.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.usuarios.model.Rol;
import cl.induztek.usuarios.model.Usuario;
import cl.induztek.usuarios.repository.UsuarioRepository;
import cl.induztek.usuarios.service.HashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private static final String CONTRASENA_DEMO = "Induztek#2026";

    private final UsuarioRepository usuarioRepository;
    private final HashService hashService;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        List<Usuario> usuarios = List.of(
                personal(Rol.TECNICO, "17.284.519-3", "Matías Godoy Herrera", "mgodoy@induztek.ejemplo", "+56 9 8123 4567",
                        "¿Cuál es el nombre de tu primera mascota?", "firulais"),
                personal(Rol.TECNICO, "19.412.087-7", "Javiera Tapia Muñoz", "jtapia@induztek.ejemplo", "+56 9 6674 9021",
                        "¿En qué ciudad nacieron tus padres?", "santiago"),
                personal(Rol.TECNICO, "26.104.583-4", "Carlos Rondón Pérez", "crdon@induztek.ejemplo", "+56 9 5521 7743",
                        "¿Cuál es tu comida favorita?", "pizza"),
                personal(Rol.TECNICO, "20.137.648-3", "Diego Valenzuela Ríos", "dvalenzuela@induztek.ejemplo", "+56 9 7390 1856",
                        "¿Cuál fue el modelo de tu primer auto?", "toyota"),
                personal(Rol.INGENIERO, "15.906.334-8", "Andrea Contreras Silva", "acontreras@induztek.ejemplo", "+56 9 7456 2210",
                        "¿Cuál es el nombre de tu mejor amigo de la infancia?", "pedro"),
                personal(Rol.INGENIERO, "16.553.270-8", "Sebastián Morales Gutiérrez", "smorales@induztek.ejemplo", "+56 9 8812 4430",
                        "¿Cuál es el nombre de tu primera mascota?", "rocky"),
                personal(Rol.JEFE_TECNICO, "13.871.905-7", "Ricardo Espinoza Lagos", "respinoza@induztek.ejemplo", "+56 9 9345 1187",
                        "¿En qué ciudad nacieron tus padres?", "valparaiso"),
                cliente("76.482.915-8", "Envasadora del Pacífico S.A.", "mfuentes@envpacifico.ejemplo", "+56 9 6612 3480",
                        "Agroindustria", "Marcela Fuentes Rojas", "Jefa de Mantenimiento",
                        "¿Cuál es tu comida favorita?", "sushi"),
                cliente("96.731.540-0", "Sanitaria Valle Central S.A.", "jarancibia@svcentral.ejemplo", "+56 9 4471 2093",
                        "Servicios sanitarios", "Jorge Arancibia Leiva", "Jefe de Planta",
                        "¿Cuál fue el modelo de tu primer auto?", "nissan"),
                cliente("77.105.362-9", "Minera Cerro Alto SpA", "pnavarro@cerroalto.ejemplo", "+56 9 3308 6615",
                        "Minería", "Patricio Navarro Soto", "Jefe de Mantenimiento Eléctrico",
                        "¿Cuál es el nombre de tu mejor amigo de la infancia?", "juan"),
                cliente("76.903.218-5", "Clínica Los Aromos Ltda.", "cmendez@losaromos.ejemplo", "+56 9 5218 6604",
                        "Salud", "Carolina Méndez Pizarro", "Supervisora",
                        "¿Cuál es el nombre de tu primera mascota?", "luna"));
        usuarioRepository.saveAll(usuarios);
        log.info("Se cargaron {} usuarios de prueba (contraseña {})", usuarios.size(), CONTRASENA_DEMO);
    }

    private Usuario personal(Rol rol, String rut, String nombre, String correo, String telefono, String pregunta,
            String respuesta) {
        return base(rol, rut, nombre, correo, telefono, pregunta, respuesta).build();
    }

    private Usuario cliente(String rut, String nombre, String correo, String telefono, String giro, String contacto,
            String cargo, String pregunta, String respuesta) {
        return base(Rol.CLIENTE, rut, nombre, correo, telefono, pregunta, respuesta)
                .giro(giro)
                .contactoNombre(contacto)
                .contactoCargo(cargo)
                .build();
    }

    private Usuario.UsuarioBuilder base(Rol rol, String rut, String nombre, String correo, String telefono,
            String pregunta, String respuesta) {
        String salContrasena = hashService.generarSal();
        String salRespuesta = hashService.generarSal();
        return Usuario.builder()
                .rol(rol)
                .rut(rut)
                .nombre(nombre)
                .correo(correo)
                .correoPersonal(correo)
                .telefono(telefono)
                .activo(true)
                .passwordSal(salContrasena)
                .passwordHash(hashService.hash(hashService.hashCliente(correo, CONTRASENA_DEMO), salContrasena))
                .preguntaSeguridad(pregunta)
                .respuestaSal(salRespuesta)
                .respuestaHash(hashService.hash(hashService.normalizarRespuesta(respuesta), salRespuesta));
    }
}
