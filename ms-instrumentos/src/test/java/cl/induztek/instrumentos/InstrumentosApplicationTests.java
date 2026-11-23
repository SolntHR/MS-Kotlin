package cl.induztek.instrumentos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.induztek.instrumentos.dto.InstrumentoResponse;
import cl.induztek.instrumentos.service.InstrumentoService;

@SpringBootTest
class InstrumentosApplicationTests {

    @Autowired
    private InstrumentoService instrumentoService;

    @Test
    void cargaLosInstrumentosConSuUltimaCalibracion() {
        assertThat(instrumentoService.listar(null)).hasSize(5)
                .allSatisfy(i -> assertThat(i.fechaVencimiento()).isNotNull())
                .extracting(InstrumentoResponse::numeroSerie).first().isEqualTo("DLR-10882");
    }

    @Test
    void laVigenciaConsideraElRangoDeLaCalibracion() {
        assertThat(instrumentoService.vigencia(1L, LocalDate.of(2026, 9, 26)).vigente()).isTrue();
        assertThat(instrumentoService.vigencia(1L, LocalDate.of(2026, 1, 1)).vigente()).isFalse();
        assertThat(instrumentoService.vigencia(4L, LocalDate.of(2026, 9, 26)).vigente()).isFalse();
    }
}
