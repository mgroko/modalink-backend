package org.mgroko.backend.home.controlador;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.home.controlador.HomeController;
import org.mgroko.backend.home.servicio.HomeOportunidadesService;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.UsuarioPerfilService;
import org.mgroko.backend.requerimientos.dto.OportunidadesHomeResponse;
import org.mgroko.backend.security.ContextoAutenticacion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioPerfilService usuarioPerfilService;

    @MockitoBean
    private HomeOportunidadesService homeOportunidadesService;

    @Test
    void health_devuelveStatusOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void resumenHome_conPerfilActivo_devuelve200ConPerfilYListasVacias() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        PerfilResponse perfil = new PerfilResponse(
                10L, "Luna", "Modelo profesional.", "Activo", "modelo", null, List.of());

        when(usuarioPerfilService.obtenerPerfilPropio(1L, 10L)).thenReturn(perfil);
        when(homeOportunidadesService.obtener(any(), anyInt()))
                .thenReturn(OportunidadesHomeResponse.vacio(5));

        mockMvc.perform(get("/home/resumen")
                .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilActivo.idPerfil").value(10))
                .andExpect(jsonPath("$.perfilActivo.nombreArtistico").value("Luna"))
                .andExpect(jsonPath("$.proyectosDestacados").isArray())
                .andExpect(jsonPath("$.proyectosDestacados").isEmpty())
                .andExpect(jsonPath("$.publicacionesRecientes").isArray())
                .andExpect(jsonPath("$.publicacionesRecientes").isEmpty())
                .andExpect(jsonPath("$.oportunidades").isArray())
                .andExpect(jsonPath("$.oportunidades").isEmpty())
                .andExpect(jsonPath("$.totalOportunidadesGenerales").value(0))
                .andExpect(jsonPath("$.totalOportunidadesActividad").value(0));
    }

    @Test
    void resumenHome_sinPerfilActivo_devuelve200ConPerfilActivoNull() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        when(homeOportunidadesService.obtener(any(), anyInt()))
                .thenReturn(OportunidadesHomeResponse.vacio(5));

        mockMvc.perform(get("/home/resumen")
                .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilActivo").doesNotExist())
                .andExpect(jsonPath("$.proyectosDestacados").isArray())
                .andExpect(jsonPath("$.publicacionesRecientes").isArray())
                .andExpect(jsonPath("$.oportunidades").isArray())
                .andExpect(jsonPath("$.oportunidades").isEmpty());
    }
}
