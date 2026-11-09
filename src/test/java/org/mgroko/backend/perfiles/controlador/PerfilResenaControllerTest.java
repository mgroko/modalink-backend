package org.mgroko.backend.perfiles.controlador;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.dto.AutorResenaResponse;
import org.mgroko.backend.perfiles.dto.DistribucionResenasResponse;
import org.mgroko.backend.perfiles.dto.ProyectoResenaResponse;
import org.mgroko.backend.perfiles.dto.ResenaPerfilResponse;
import org.mgroko.backend.perfiles.dto.ResenasPerfilResponse;
import org.mgroko.backend.perfiles.dto.ResumenResenasResponse;
import org.mgroko.backend.perfiles.exception.ResenaNoEncontradaException;
import org.mgroko.backend.perfiles.servicio.ConsultarResenasPerfilService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = PerfilResenaController.class)
@AutoConfigureMockMvc(addFilters = false)
class PerfilResenaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultarResenasPerfilService consultarResenasPerfilService;

    @Test
    void listar_devuelveResumenYContenido() throws Exception {
        var auth = autenticacion();

        when(consultarResenasPerfilService.listar(5L, 1L, 0, 10))
                .thenReturn(respuestaListado());

        mockMvc.perform(get("/perfiles/5/calificaciones")
                        .param("page", "0")
                        .param("size", "10")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPerfil").value(5L))
                .andExpect(jsonPath("$.resumen.promedio").value(4.5))
                .andExpect(jsonPath("$.resumen.total").value(2))
                .andExpect(jsonPath("$.resumen.distribucion[0].valoracion").value(5))
                .andExpect(jsonPath("$.resumen.distribucion[0].cantidad").value(1))
                .andExpect(jsonPath("$.calificaciones.contenido[0].idCalificacion").value(1L))
                .andExpect(jsonPath("$.calificaciones.contenido[0].emisor.nombreArtistico")
                        .value("Lucía Benítez"))
                .andExpect(jsonPath("$.calificaciones.contenido[0].proyecto.nombre")
                        .value("Editorial Primavera 2026"));
    }

    @Test
    void listar_usaValoresPorDefectoDePaginacion() throws Exception {
        var auth = autenticacion();

        when(consultarResenasPerfilService.listar(5L, 1L, 0, 10))
                .thenReturn(respuestaListado());

        mockMvc.perform(get("/perfiles/5/calificaciones").principal(auth))
                .andExpect(status().isOk());
    }

    @Test
    void obtener_devuelveDetalle() throws Exception {
        var auth = autenticacion();

        when(consultarResenasPerfilService.obtener(5L, 1L, 1L))
                .thenReturn(resena());

        mockMvc.perform(get("/perfiles/5/calificaciones/1").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCalificacion").value(1L))
                .andExpect(jsonPath("$.valoracion").value(5))
                .andExpect(jsonPath("$.resenia").value("Excelente predisposición."))
                .andExpect(jsonPath("$.emisor.idPerfil").value(10L));
    }

    @Test
    void obtener_resenaInexistente_devuelve404() throws Exception {
        var auth = autenticacion();

        when(consultarResenasPerfilService.obtener(5L, 99L, 1L))
                .thenThrow(new ResenaNoEncontradaException());

        mockMvc.perform(get("/perfiles/5/calificaciones/99").principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La reseña no fue encontrada."));
    }

    private ResenasPerfilResponse respuestaListado() {
        return new ResenasPerfilResponse(
                5L,
                new ResumenResenasResponse(
                        4.5,
                        2,
                        List.of(
                                new DistribucionResenasResponse(5, 1),
                                new DistribucionResenasResponse(4, 1),
                                new DistribucionResenasResponse(3, 0),
                                new DistribucionResenasResponse(2, 0),
                                new DistribucionResenasResponse(1, 0))),
                new PaginaResponse<>(
                        List.of(resena()),
                        0,
                        10,
                        2,
                        1,
                        true,
                        true));
    }

    private ResenaPerfilResponse resena() {
        return new ResenaPerfilResponse(
                1L,
                5,
                "Excelente predisposición.",
                LocalDateTime.of(2026, 10, 12, 15, 30),
                new AutorResenaResponse(
                        10L,
                        "Lucía Benítez",
                        "Diseñadora de indumentaria",
                        "/uploads/perfiles/lucia.jpg"),
                new ProyectoResenaResponse(100L, "Editorial Primavera 2026"));
    }

    private UsernamePasswordAuthenticationToken autenticacion() {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Lucía Benítez"));
        return auth;
    }
}
