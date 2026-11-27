package org.mgroko.backend.postulaciones.casting.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.postulaciones.casting.dto.ProyectoCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.RequerimientoCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.TableroCastingResponse;
import org.mgroko.backend.postulaciones.casting.enums.OrdenCasting;
import org.mgroko.backend.postulaciones.casting.servicio.ObtenerTableroCastingService;
import org.mgroko.backend.postulaciones.dto.ConteosPostulacionesResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
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
        controllers = CastingController.class)
@AutoConfigureMockMvc(addFilters = false)
class CastingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObtenerTableroCastingService obtenerTableroCastingService;

    @Test
    void obtener_conPerfilActivo_devuelveTablero() throws Exception {
        when(obtenerTableroCastingService.obtener(
                eq(10L), eq(5L), eq(TipoPostulacion.GENERAL), eq(50L),
                any(), eq(OrdenCasting.CUMPLIMIENTO), eq(0), eq(77L)))
                .thenReturn(respuesta());

        mockMvc.perform(get("/proyectos/5/postulaciones/casting")
                        .param("tipo", "GENERAL")
                        .param("idRequerimiento", "50")
                        .param("idPostulacionDestacada", "77")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requerimiento.tipo").value("GENERAL"))
                .andExpect(jsonPath("$.requerimiento.faltantes").value(1))
                .andExpect(jsonPath("$.paginaActual").value(0))
                .andExpect(jsonPath("$.tamanoPagina").value(6))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void obtener_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(get("/proyectos/5/postulaciones/casting")
                        .param("tipo", "GENERAL")
                        .param("idRequerimiento", "50")
                        .principal(auth))
                .andExpect(status().isNotFound());
    }

    private TableroCastingResponse respuesta() {
        return new TableroCastingResponse(
                new ProyectoCastingResponse(5L, "Proyecto"),
                new RequerimientoCastingResponse(
                        TipoPostulacion.GENERAL,
                        50L,
                        null,
                        null,
                        "Dos modelos",
                        2L,
                        "MODELO",
                        "Modelo",
                        2,
                        1,
                        1,
                        false,
                        false,
                        3,
                        false,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of()),
                List.of(),
                0,
                6,
                0,
                0,
                true,
                true,
                new ConteosPostulacionesResponse(3, 0, 0, 3));
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario, Long idPerfil) {
        var auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of());
        auth.setDetails(new ContextoAutenticacion(idUsuario, idPerfil, "Luna Diseños"));
        return auth;
    }
}
