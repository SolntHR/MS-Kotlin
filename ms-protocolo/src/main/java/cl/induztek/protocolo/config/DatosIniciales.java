package cl.induztek.protocolo.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.protocolo.model.EstadoProtocolo;
import cl.induztek.protocolo.model.Medicion;
import cl.induztek.protocolo.model.Parametro;
import cl.induztek.protocolo.model.Protocolo;
import cl.induztek.protocolo.model.Prueba;
import cl.induztek.protocolo.model.RecepcionActa;
import cl.induztek.protocolo.model.Resultado;
import cl.induztek.protocolo.model.TipoEquipoPrueba;
import cl.induztek.protocolo.model.TipoPrueba;
import cl.induztek.protocolo.model.TipoVisita;
import cl.induztek.protocolo.model.Visita;
import cl.induztek.protocolo.repository.ProtocoloRepository;
import cl.induztek.protocolo.repository.TipoEquipoPruebaRepository;
import cl.induztek.protocolo.repository.TipoPruebaRepository;
import cl.induztek.protocolo.repository.VisitaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final TipoPruebaRepository tipoPruebaRepository;
    private final TipoEquipoPruebaRepository tipoEquipoPruebaRepository;
    private final VisitaRepository visitaRepository;
    private final ProtocoloRepository protocoloRepository;
    private final Clock reloj;

    private List<TipoPrueba> tipos;

    @Override
    @Transactional
    public void run(String... args) {
        if (tipoPruebaRepository.count() > 0) {
            return;
        }
        cargarCatalogo();
        List<Visita> visitas = cargarVisitas();
        cargarProtocolos(visitas);
        log.info("Se cargaron los tipos de prueba, {} visitas y 6 protocolos de prueba", visitas.size());
    }

    private void cargarCatalogo() {
        TipoPrueba aislacion = tipo("AISLACION", "Resistencia de aislación",
                parametro("LECTURA_1MIN", "Lectura a 1 min", "MΩ"),
                parametro("LECTURA_10MIN", "Lectura a 10 min", "MΩ"),
                parametro("TENSION_PRUEBA", "Tensión de prueba", "kV"));
        TipoPrueba ttr = tipo("TTR", "Relación de transformación",
                parametro("RELACION_TEORICA", "Relación teórica", "-"),
                parametro("RELACION_MEDIDA", "Relación medida", "-"),
                parametro("POSICION_TAP", "Posición del cambiador de derivaciones", "-"));
        TipoPrueba devanados = tipo("DEVANADOS", "Resistencia de devanados",
                parametro("RESISTENCIA_FASE", "Resistencia por fase", "mΩ"));
        TipoPrueba contactos = tipo("CONTACTOS", "Resistencia de contactos",
                parametro("RESISTENCIA_POLO", "Resistencia por polo", "µΩ"));
        TipoPrueba rele = tipo("OPERACION_RELE", "Prueba de operación (inyección secundaria)",
                parametro("CORRIENTE_AJUSTE", "Ajuste de corriente de operación", "A"),
                parametro("CORRIENTE_MEDIDA", "Corriente medida de operación", "A"),
                parametro("TIEMPO_ESPERADO", "Tiempo esperado", "ms"),
                parametro("TIEMPO_MEDIDO", "Tiempo medido", "ms"));
        tipos = tipoPruebaRepository.saveAll(List.of(aislacion, ttr, devanados, contactos, rele));
        tipoEquipoPruebaRepository.saveAll(List.of(
                aplica(1L, aislacion), aplica(1L, ttr), aplica(1L, devanados),
                aplica(2L, aislacion), aplica(2L, contactos),
                aplica(3L, aislacion), aplica(3L, contactos),
                aplica(4L, aislacion),
                aplica(5L, rele)));
    }

    private List<Visita> cargarVisitas() {
        LocalDate hoy = LocalDate.now(reloj);
        return visitaRepository.saveAll(List.of(
                visita("OT-2026-1187", 1L, LocalDate.of(2026, 9, 26), TipoVisita.SOLO_PRUEBAS, 5L, null, 1L),
                visita("OT-2026-1154", 4L, LocalDate.of(2026, 9, 20), TipoVisita.PRUEBAS_Y_MANT, 6L,
                        recepcion("Tomás Leiva Araya", "18.390.452-3", "Supervisor Eléctrico", "2026-09-20T09:55"),
                        3L),
                visita("OT-2024-0951", 1L, LocalDate.of(2024, 9, 14), TipoVisita.SOLO_PRUEBAS, 5L,
                        recepcion("Marcela Fuentes Rojas", "14.226.791-8", "Jefa de Mantenimiento", "2024-09-14T12:10"),
                        1L),
                visita("OT-2025-1032", 1L, LocalDate.of(2025, 9, 13), TipoVisita.SOLO_PRUEBAS, 5L,
                        recepcion("Marcela Fuentes Rojas", "14.226.791-8", "Jefa de Mantenimiento", "2025-09-13T11:45"),
                        2L),
                visita("OT-2026-1102", 3L, LocalDate.of(2026, 9, 8), TipoVisita.SOLO_PRUEBAS, 6L,
                        recepcion("Patricio Navarro Soto", "13.349.876-1", "Superintendente de Mantenimiento",
                                "2026-09-08T13:20"),
                        2L),
                visita("OT-2026-1215", 1L, hoy, TipoVisita.SOLO_PRUEBAS, 5L, null, 1L, 2L),
                visita("OT-2026-1216", 3L, hoy, TipoVisita.PRUEBAS_Y_MANT, 6L, null, 1L, 2L, 5L)));
    }

    private void cargarProtocolos(List<Visita> visitas) {
        Protocolo pt0312 = protocolo("PT-2024-0312", visitas.get(2), 1L, 1L, "2024-09-14T10:00", 18.0, 55.0, null,
                "Prueba de rutina");
        pt0312.agregarPrueba(prueba(0, "AT-T", 1L, Resultado.APROBADO, null, 12500.0, 31250.0, 5.0));
        historial(pt0312, 5L, EstadoProtocolo.FORMALIZADO, "2024-09-16T15:00", null);

        Protocolo pt0288 = protocolo("PT-2025-0288", visitas.get(3), 1L, 2L, "2025-09-13T09:30", 21.0, 48.0, null,
                "Prueba anual");
        pt0288.agregarPrueba(prueba(0, "AT-T", 1L, Resultado.APROBADO, null, 9800.0, 21560.0, 5.0));
        historial(pt0288, 5L, EstadoProtocolo.FORMALIZADO, "2025-09-15T10:00", null);

        Protocolo pt0401 = protocolo("PT-2026-0401", visitas.get(0), 1L, 1L, "2026-09-26T09:40", 19.0, 60.0,
                "Limpieza de aisladores BT", "IP bajo en AT-T, revisar");
        pt0401.agregarPrueba(prueba(0, "AT-T", 1L, Resultado.OBSERVADO, "IP bajo en AT-T, revisar", 6100.0, 10980.0,
                5.0));
        pt0401.agregarPrueba(Prueba.builder().tipoPrueba(tipos.get(1)).elemento("Fase H1-X1").momento("UNICA")
                .realizada(false).motivoNoRealizada("Cliente no autorizó desenergizar el lado BT").build());
        historial(pt0401, 1L, null, null, null);

        Protocolo pt0402 = protocolo("PT-2026-0402", visitas.get(0), 2L, 1L, "2026-09-26T11:15", 19.0, 60.0, null,
                null);
        pt0402.agregarPrueba(prueba(0, "AT-T", 1L, Resultado.APROBADO, null, 15200.0, 36480.0, 5.0));
        pt0402.agregarPrueba(prueba(1, "Fase H1-X1", 3L, Resultado.APROBADO, null, 30.0, 30.09, 3.0));
        historial(pt0402, 1L, null, null, null);

        Protocolo pt0389 = protocolo("PT-2026-0389", visitas.get(1), 4L, 3L, "2026-09-20T11:00", 22.0, 45.0,
                "Ajuste de bornes", "Falta n° de serie instrumento");
        pt0389.agregarPrueba(prueba(0, "Polo 1", 1L, Resultado.APROBADO, null, 8500.0, null, 5.0));
        pt0389.agregarPrueba(prueba(3, "Polo 1", null, Resultado.OBSERVADO, "Falta n° de serie del instrumento", 65.0));
        historial(pt0389, 6L, EstadoProtocolo.OBSERVADO, "2026-09-21T09:00", "Falta n° de serie instrumento");

        Protocolo pt0360 = protocolo("PT-2026-0360", visitas.get(4), 5L, 2L, "2026-09-08T10:30", 27.0, 20.0, null,
                null);
        pt0360.agregarPrueba(prueba(3, "Polo 1", null, Resultado.RECHAZADO, "Resistencia sobre el máximo", 56.0));
        historial(pt0360, 6L, EstadoProtocolo.FORMALIZADO, "2026-09-10T12:00", null);

        protocoloRepository.saveAll(List.of(pt0312, pt0288, pt0401, pt0402, pt0389, pt0360));
    }

    private TipoPrueba tipo(String codigo, String nombre, Parametro... parametros) {
        TipoPrueba tipo = TipoPrueba.builder().codigo(codigo).nombre(nombre).build();
        for (Parametro parametro : parametros) {
            parametro.setTipoPrueba(tipo);
            tipo.getParametros().add(parametro);
        }
        return tipo;
    }

    private Parametro parametro(String codigo, String nombre, String unidad) {
        return Parametro.builder().codigo(codigo).nombre(nombre).unidad(unidad).build();
    }

    private TipoEquipoPrueba aplica(Long tipoEquipoId, TipoPrueba tipo) {
        return TipoEquipoPrueba.builder().tipoEquipoId(tipoEquipoId).tipoPrueba(tipo).build();
    }

    private RecepcionActa recepcion(String nombre, String rut, String cargo, String fechaHora) {
        return RecepcionActa.builder().nombre(nombre).rut(rut).cargo(cargo).fechaHora(LocalDateTime.parse(fechaHora))
                .build();
    }

    private Visita visita(String ot, Long instalacionId, LocalDate fecha, TipoVisita tipo, Long ingenieroId,
            RecepcionActa recepcion, Long... participantes) {
        return Visita.builder().numeroOt(ot).instalacionId(instalacionId).fecha(fecha).tipoVisita(tipo)
                .ingenieroCargoId(ingenieroId).participantes(new LinkedHashSet<>(List.of(participantes)))
                .recepcion(recepcion).fechaHoraCierre(recepcion == null ? null : recepcion.getFechaHora()).build();
    }

    private Protocolo protocolo(String numero, Visita visita, Long equipoId, Long usuarioId, String fechaHora,
            Double temperatura, Double humedad, String mantenimiento, String observaciones) {
        return Protocolo.builder().id(numero).numero(numero).visita(visita).equipoId(equipoId)
                .usuarioRegistraId(usuarioId).fechaHoraPrueba(LocalDateTime.parse(fechaHora)).temperaturaC(temperatura)
                .humedadPct(humedad).descripcionMantenimiento(mantenimiento).observaciones(observaciones).build();
    }

    private Prueba prueba(int tipo, String elemento, Long instrumentoId, Resultado resultado, String comentario,
            Double... valores) {
        TipoPrueba tipoPrueba = tipos.get(tipo);
        Prueba prueba = Prueba.builder().tipoPrueba(tipoPrueba).elemento(elemento).momento("UNICA").realizada(true)
                .instrumentoId(instrumentoId).resultado(resultado).comentario(comentario).build();
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] == null) {
                continue;
            }
            Parametro parametro = tipoPrueba.getParametros().get(i);
            prueba.agregarMedicion(Medicion.builder().parametro(parametro).valor(valores[i])
                    .unidad(parametro.getUnidad()).orden(i + 1).build());
        }
        return prueba;
    }

    private void historial(Protocolo protocolo, Long revisorId, EstadoProtocolo estadoFinal, String fechaRevision,
            String motivo) {
        LocalDateTime inicio = protocolo.getFechaHoraPrueba();
        protocolo.registrarCambio(EstadoProtocolo.BORRADOR, protocolo.getUsuarioRegistraId(), inicio,
                "Creación del protocolo");
        protocolo.registrarCambio(EstadoProtocolo.REGISTRADO_TERRENO, protocolo.getUsuarioRegistraId(),
                inicio.plusMinutes(45), null);
        if (estadoFinal != null) {
            protocolo.registrarCambio(estadoFinal, revisorId, LocalDateTime.parse(fechaRevision), motivo);
        }
    }
}
