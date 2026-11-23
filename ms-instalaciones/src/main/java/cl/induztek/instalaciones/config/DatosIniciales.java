package cl.induztek.instalaciones.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.instalaciones.model.Clasificacion;
import cl.induztek.instalaciones.model.Comuna;
import cl.induztek.instalaciones.model.Equipo;
import cl.induztek.instalaciones.model.Instalacion;
import cl.induztek.instalaciones.model.Region;
import cl.induztek.instalaciones.model.TipoEquipo;
import cl.induztek.instalaciones.repository.ComunaRepository;
import cl.induztek.instalaciones.repository.EquipoRepository;
import cl.induztek.instalaciones.repository.InstalacionRepository;
import cl.induztek.instalaciones.repository.RegionRepository;
import cl.induztek.instalaciones.repository.TipoEquipoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final RegionRepository regionRepository;
    private final ComunaRepository comunaRepository;
    private final TipoEquipoRepository tipoEquipoRepository;
    private final InstalacionRepository instalacionRepository;
    private final EquipoRepository equipoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (regionRepository.count() > 0) {
            return;
        }
        Region metropolitana = regionRepository.save(Region.builder().nombre("Región Metropolitana").build());
        List<Comuna> comunas = comunaRepository.saveAll(List.of(
                comuna(metropolitana, "Lampa"),
                comuna(metropolitana, "San Bernardo"),
                comuna(metropolitana, "Til Til"),
                comuna(metropolitana, "La Florida"),
                comuna(metropolitana, "Puente Alto"),
                comuna(metropolitana, "Huechuraba")));

        List<TipoEquipo> tipos = tipoEquipoRepository.saveAll(List.of(
                tipo("Transformador", Clasificacion.PRIMARIO),
                tipo("Interruptor", Clasificacion.PRIMARIO),
                tipo("Celda MT", Clasificacion.PRIMARIO),
                tipo("Cable de poder", Clasificacion.PRIMARIO),
                tipo("Relé de protección", Clasificacion.SECUNDARIO)));

        List<Instalacion> instalaciones = instalacionRepository.saveAll(List.of(
                instalacion(8L, "Planta Lampa", "Sector industrial", comunas.get(0), "Inducción obligatoria de 30 min"),
                instalacion(9L, "Planta Elevadora Sur", "Sector rural poniente", comunas.get(1),
                        "Sala eléctrica subterránea, sin señal"),
                instalacion(10L, "Faena Til Til", "Acceso por Ruta 5 Norte", comunas.get(2), "Sin señal en toda la faena"),
                instalacion(11L, "Edificio Clínico", "Zona urbana", comunas.get(3), "Cortes solo domingos 06:00-10:00")));

        equipoRepository.saveAll(List.of(
                equipo(instalaciones.get(0), "TR-01", tipos.get(0), "Rhona", "RH-18733", 1500.0, 12.0, 72.0, null),
                equipo(instalaciones.get(0), "TR-02", tipos.get(0), "Rhona", "RH-21904", 1000.0, 12.0, 48.0, null),
                equipo(instalaciones.get(1), "TR-A", tipos.get(0), "Tusan", "TS-5521", 2500.0, 23.0, 62.0, null),
                equipo(instalaciones.get(3), "CEL-MT-03", tipos.get(2), "Ormazabal", "OR-77812", null, 12.0, 630.0, 60.0),
                equipo(instalaciones.get(2), "INT-52-1", tipos.get(1), "Schneider", "SE-449021", null, 23.0, 1250.0, 45.0)));
        log.info("Se cargaron los catálogos, {} instalaciones y 5 equipos de prueba", instalaciones.size());
    }

    private Comuna comuna(Region region, String nombre) {
        return Comuna.builder().region(region).nombre(nombre).build();
    }

    private TipoEquipo tipo(String nombre, Clasificacion clasificacion) {
        return TipoEquipo.builder().nombre(nombre).clasificacion(clasificacion).build();
    }

    private Instalacion instalacion(Long clienteId, String nombre, String ubicacion, Comuna comuna, String acceso) {
        return Instalacion.builder().clienteId(clienteId).nombre(nombre).ubicacion(ubicacion).comuna(comuna)
                .observacionesAcceso(acceso).build();
    }

    private Equipo equipo(Instalacion instalacion, String tag, TipoEquipo tipo, String marca, String serie,
            Double potencia, Double tension, Double corriente, Double maxContactos) {
        return Equipo.builder().instalacion(instalacion).tag(tag).tipoEquipo(tipo).marca(marca).modelo(serie)
                .numeroSerie(serie).potenciaKva(potencia).tensionKv(tension).corrienteA(corriente)
                .maxContactosUohm(maxContactos).build();
    }
}
