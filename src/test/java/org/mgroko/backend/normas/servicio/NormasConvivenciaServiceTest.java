package org.mgroko.backend.normas.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

class NormasConvivenciaServiceTest {

    private final NormasConvivenciaService service = new NormasConvivenciaService(
            new ObjectMapper(),
            new ClassPathResource("normas-convivencia/normas-convivencia.json"));

    @Test
    void vigente_devuelveLaVersionPublicadaYSuContenido() {
        var respuesta = service.vigente();

        assertThat(respuesta.titulo()).isEqualTo("Normas de convivencia");
        assertThat(respuesta.version()).isEqualTo("1.0");
        assertThat(respuesta.vigenteDesde()).isEqualTo("2026-11-26");
        assertThat(respuesta.vigenteHasta()).isNull();
        assertThat(respuesta.regla()).contains("se entiende aceptada");
        assertThat(respuesta.secciones()).isNotEmpty();
        assertThat(respuesta.secciones())
                .extracting(seccion -> seccion.id())
                .contains("que-son", "como-se-aceptan", "donde-consultar");
        assertThat(respuesta.historial()).hasSize(1);
    }

    @Test
    void versiones_devuelveElHistorialOrdenado() {
        var versiones = service.versiones();

        assertThat(versiones).hasSize(1);
        assertThat(versiones.get(0).version()).isEqualTo("1.0");
        assertThat(versiones.get(0).vigenteHasta()).isNull();
    }

    @Test
    void version_inexistente_devuelve404() {
        assertThatThrownBy(() -> service.version("9.9"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("9.9");
    }
}
