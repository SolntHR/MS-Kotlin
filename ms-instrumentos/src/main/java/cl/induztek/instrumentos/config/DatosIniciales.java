package cl.induztek.instrumentos.config;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.instrumentos.model.Calibracion;
import cl.induztek.instrumentos.model.Instrumento;
import cl.induztek.instrumentos.repository.CalibracionRepository;
import cl.induztek.instrumentos.repository.InstrumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final InstrumentoRepository instrumentoRepository;
    private final CalibracionRepository calibracionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (instrumentoRepository.count() > 0) {
            return;
        }
        List<Instrumento> instrumentos = instrumentoRepository.saveAll(List.of(
                instrumento("Megóhmetro 5 kV", "Megger", "MIT525", "MIT-0045821"),
                instrumento("Megóhmetro 10 kV", "Megger", "MIT1025", "MIT-0071233"),
                instrumento("Medidor de relación (TTR)", "Megger", "TTR300", "TTR-30044"),
                instrumento("Micro-ohmímetro", "Megger", "DLRO10X", "DLR-10882"),
                instrumento("Equipo de inyección secundaria", "Omicron", "CMC 356", "CMC-HB411")));
        calibracionRepository.saveAll(List.of(
                calibracion(instrumentos.get(0), "2026-03-15", "2027-03-15"),
                calibracion(instrumentos.get(1), "2025-10-10", "2026-10-10"),
                calibracion(instrumentos.get(2), "2026-06-02", "2027-06-02"),
                calibracion(instrumentos.get(3), "2025-08-20", "2026-08-20"),
                calibracion(instrumentos.get(4), "2026-01-05", "2027-01-05")));
        log.info("Se cargaron {} instrumentos con su calibración", instrumentos.size());
    }

    private Instrumento instrumento(String tipo, String marca, String modelo, String serie) {
        return Instrumento.builder().nombreTipo(tipo).marca(marca).modelo(modelo).numeroSerie(serie).build();
    }

    private Calibracion calibracion(Instrumento instrumento, String desde, String hasta) {
        return Calibracion.builder().instrumento(instrumento).fechaCalibracion(LocalDate.parse(desde))
                .fechaVencimiento(LocalDate.parse(hasta)).build();
    }
}
