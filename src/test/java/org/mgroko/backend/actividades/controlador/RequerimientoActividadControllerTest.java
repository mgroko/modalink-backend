package org.mgroko.backend.actividades.controlador;

import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.BuscarRequerimientosActividadFiltro;
import org.mgroko.backend.actividades.dto.RequerimientoActividadBusquedaResponse;
import org.mgroko.backend.actividades.servicio.BuscarRequerimientoActividadService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class, controllers = RequerimientoActividadController.class)
@AutoConfigureMockMvc(addFilters = false)
class RequerimientoActividadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuscarRequerimientoActividadService buscarRequerimientoActividadService;

    @Test
    void buscar_conPerfilActivo_devuelve200() throws Exception {
        when(buscarRequerimientoActividadService.buscar(
                any(), any(), any(BuscarRequerimientosActividadFiltro.class), any(Integer.class),
                any(Integer.class), any(Boolean.class)))
                .thenReturn(new PaginaResponse<>(
                        List.of(new RequerimientoActividadBusquedaResponse(
                                30L, 2, "Requerimiento", 1L, "Fotógrafo",
                                40L, "Actividad", "PENDIENTE", 5L, "Proyecto",
                                List.of(), List.of())),
                        0, 20, 1L, 1, true, true));

        mockMvc.perform(get("/proyectos/5/requerimientos-actividad/buscar")
                        .principal(autenticacion(1L, 10L))
                        .param("idActividad", "40")
                        .param("idProfesion", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idRequerimiento").value(30))
                .andExpect(jsonPath("$.contenido[0].idActividad").value(40))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void buscar_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(get("/proyectos/5/requerimientos-actividad/buscar")
                        .principal(auth))
                .andExpect(status().isNotFound());

        verify(buscarRequerimientoActividadService, never())
                .buscar(any(), any(), any(), any(Integer.class), any(Integer.class), any(Boolean.class));
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario, Long idPerfil) {
        var token = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(idUsuario, idPerfil, "Perfil activo"));
        return token;
    }
}
