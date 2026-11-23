package cl.induztek.instalaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.model.Clasificacion;
import cl.induztek.instalaciones.model.Comuna;
import cl.induztek.instalaciones.model.Region;
import cl.induztek.instalaciones.model.TipoEquipo;
import cl.induztek.instalaciones.repository.ComunaRepository;
import cl.induztek.instalaciones.repository.RegionRepository;
import cl.induztek.instalaciones.repository.TipoEquipoRepository;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private ComunaRepository comunaRepository;

    @Mock
    private TipoEquipoRepository tipoEquipoRepository;

    @InjectMocks
    private CatalogoService catalogoService;

    private final Region metropolitana = Region.builder().id(1L).nombre("Región Metropolitana").build();
    private final Comuna lampa = Comuna.builder().id(1L).nombre("Lampa").region(metropolitana).build();
    private final TipoEquipo rele = TipoEquipo.builder().id(5L).nombre("Relé de protección")
            .clasificacion(Clasificacion.SECUNDARIO).build();

    @Test
    void listaRegionesYComunas() {
        when(regionRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(metropolitana));
        when(comunaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(lampa));
        when(regionRepository.existsById(1L)).thenReturn(true);
        when(comunaRepository.findByRegionIdOrderByNombreAsc(1L)).thenReturn(List.of(lampa));

        assertThat(catalogoService.regiones()).hasSize(1);
        assertThat(catalogoService.comunas(null).get(0).regionNombre()).isEqualTo("Región Metropolitana");
        assertThat(catalogoService.comunas(1L)).hasSize(1);
    }

    @Test
    void comunasDeRegionInexistenteLanza404() {
        when(regionRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> catalogoService.comunas(9L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void listaYObtieneTiposDeEquipo() {
        when(tipoEquipoRepository.findAllByOrderByIdAsc()).thenReturn(List.of(rele));
        when(tipoEquipoRepository.findById(5L)).thenReturn(Optional.of(rele));
        when(tipoEquipoRepository.findById(6L)).thenReturn(Optional.empty());

        assertThat(catalogoService.tiposEquipo().get(0).clasificacion()).isEqualTo("SECUNDARIO");
        assertThat(catalogoService.tipoEquipo(5L).nombre()).isEqualTo("Relé de protección");
        assertThatThrownBy(() -> catalogoService.tipoEquipo(6L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
