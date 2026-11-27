package org.mgroko.backend.manual.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.manual.dto.ManualError;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

class ManualServiceTest {

    private final ManualService service = new ManualService(
            new ObjectMapper(),
            new ClassPathResource("manual/indice.json"));

    @Test
    void indiceUsuario_excluyeAdministracionEIncluyeNormas() {
        var indice = service.indice(false);

        assertThat(indice.version()).isEqualTo("1.0");
        assertThat(indice.capitulos())
                .extracting(capitulo -> capitulo.id())
                .contains("01-introduccion", "16-normas-de-convivencia", "anexo-a-permisos")
                .doesNotContain("10-administracion");
    }

    @Test
    void indiceAdmin_incluyeAdministracionYAnexoA() {
        var indice = service.indice(true);

        assertThat(indice.capitulos())
                .extracting(capitulo -> capitulo.id())
                .contains("10-administracion", "anexo-a-permisos")
                .doesNotContain("01-introduccion", "16-normas-de-convivencia");
    }

    @Test
    void capituloNormas_renderizaHtmlYSecciones() {
        var capitulo = service.capitulo("16-normas-de-convivencia", false);

        assertThat(capitulo.titulo()).isEqualTo("Normas de convivencia");
        assertThat(capitulo.html()).contains("regla de convivencia");
        assertThat(capitulo.secciones())
                .extracting(seccion -> seccion.titulo())
                .contains("16.1. Qué son", "16.3. Cómo se aceptan", "16.8. Dónde consultar");
        assertThat(capitulo.anterior()).isNotNull();
        assertThat(capitulo.siguiente()).isNotNull();
    }

    @Test
    void capituloAdministracion_noEstaDisponibleParaUsuario() {
        assertThatThrownBy(() -> service.capitulo("10-administracion", false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    void capituloAdministracion_estaDisponibleParaAdmin() {
        var capitulo = service.capitulo("10-administracion", true);

        assertThat(capitulo.titulo()).isEqualTo("Panel de Administración");
        assertThat(capitulo.html()).isNotBlank();
    }

    @Test
    void buscar_devuelveResultadosDelManualDeUsuario() {
        var resultados = service.buscar("normas de convivencia", false);

        assertThat(resultados).isNotEmpty();
        assertThat(resultados)
                .extracting(resultado -> resultado.capituloId())
                .contains("16-normas-de-convivencia");
    }

    @Test
    void buscar_sinCoincidencias_devuelveListaVacia() {
        assertThat(service.buscar("zzzz-no-existe", false)).isEmpty();
    }

    @Test
    void errores_deduplicaYContieneMensajesConocidos() {
        var errores = service.errores(false);

        assertThat(errores).hasSizeGreaterThan(300);
        assertThat(errores)
                .extracting(ManualError::mensaje)
                .doesNotHaveDuplicates()
                .contains("El perfil no se encuentra activo.");
        assertThat(errores)
                .allSatisfy(error -> {
                    assertThat(error.capituloId()).isNotBlank();
                    assertThat(error.mensaje()).isNotBlank();
                });
    }

    @Test
    void errores_adminIncluyeMensajesDeAdministracion() {
        var errores = service.errores(true);

        assertThat(errores)
                .extracting(ManualError::mensaje)
                .contains("No podés deshabilitar tu propia cuenta.");
    }
}
