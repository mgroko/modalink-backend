package org.mgroko.backend.proyectos.controlador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.proyectos.pdf.InformeProyectoPdfRenderer;
import org.mgroko.backend.proyectos.servicio.GenerarInformeProyectoService;
import org.mgroko.backend.proyectos.support.InformeProyectoFixture;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = InformeProyectoController.class)
@AutoConfigureMockMvc(addFilters = false)
class InformeProyectoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerarInformeProyectoService generarInformeProyectoService;

    @MockitoBean
    private InformeProyectoPdfRenderer informeProyectoPdfRenderer;

    @Test
    void obtener_devuelveInformeJson() throws Exception {
        when(generarInformeProyectoService.generar(1L, 10L, 1L))
                .thenReturn(InformeProyectoFixture.minimo());

        mockMvc.perform(get("/proyectos/1/informe")
                        .principal(auth(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proyecto.nombre").value("Campaña Primavera"))
                .andExpect(jsonPath("$.resumen.estadoGeneral").value("ROJO"))
                .andExpect(jsonPath("$.resumen.areas[0].codigo").value("CRONOGRAMA"))
                .andExpect(jsonPath("$.resumen.riesgoEntrega.margenDias").value(3))
                .andExpect(jsonPath("$.alertas[0].area").value("EQUIPO"))
                .andExpect(jsonPath("$.generadoPor.nombreArtistico").value("Ana Dir"));
    }

    @Test
    void exportarPdf_devuelvePdfConNombreYNoStore() throws Exception {
        when(generarInformeProyectoService.generar(1L, 10L, 1L))
                .thenReturn(InformeProyectoFixture.minimo());

        byte[] pdf = "%PDF-1.7\ninforme".getBytes(StandardCharsets.US_ASCII);
        doAnswer(invocation -> {
            OutputStream salida = invocation.getArgument(1);
            salida.write(pdf);
            return null;
        }).when(informeProyectoPdfRenderer).render(any(), any());

        mockMvc.perform(get("/proyectos/1/informe/pdf")
                        .principal(auth(10L)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"informe_proyecto_1_20261125.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void obtener_sinPerfilActivoDevuelve404() throws Exception {
        mockMvc.perform(get("/proyectos/1/informe")
                        .principal(auth(null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void controladorEstaRestringidoAUsuario() {
        PreAuthorize annotation = InformeProyectoController.class
                .getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('USUARIO')");
    }

    private UsernamePasswordAuthenticationToken auth(Long idPerfilActivo) {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, idPerfilActivo, "Ana Dir"));
        return auth;
    }
}
