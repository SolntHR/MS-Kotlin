package cl.induztek.instalaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import cl.induztek.instalaciones.client.UsuarioClient;
import cl.induztek.instalaciones.dto.EquipoResponse;
import cl.induztek.instalaciones.service.CatalogoService;
import cl.induztek.instalaciones.service.EquipoService;
import cl.induztek.instalaciones.service.InstalacionService;

@SpringBootTest
class InstalacionesApplicationTests {

    @MockitoBean
    private UsuarioClient usuarioClient;

    @Autowired
    private CatalogoService catalogoService;

    @Autowired
    private InstalacionService instalacionService;

    @Autowired
    private EquipoService equipoService;

    @Test
    void cargaLosDatosInicialesIgualQueLaApp() {
        when(usuarioClient.nombresClientes()).thenReturn(Map.of(8L, "Envasadora del Pacífico S.A."));

        assertThat(catalogoService.comunas(null)).hasSize(6);
        assertThat(catalogoService.tiposEquipo()).hasSize(5);
        assertThat(instalacionService.listar(8L)).singleElement()
                .satisfies(i -> assertThat(i.clienteNombre()).isEqualTo("Envasadora del Pacífico S.A."));
        assertThat(equipoService.listar(null, null, null)).hasSize(5);
    }

    @Test
    void filtraLosEquiposPorClienteInstalacionYTipo() {
        assertThat(equipoService.listar(null, 8L, null)).extracting(EquipoResponse::tag)
                .containsExactly("TR-01", "TR-02");
        assertThat(equipoService.listar(1L, null, 1L)).hasSize(2);
        assertThat(equipoService.listar(null, null, 3L)).extracting(EquipoResponse::tag).containsExactly("CEL-MT-03");
    }
}
