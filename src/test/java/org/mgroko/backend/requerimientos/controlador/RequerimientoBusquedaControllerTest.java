package org.mgroko.backend.requerimientos.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.requerimientos.dto.BuscarRequerimientosGlobalFiltro;
import org.mgroko.backend.requerimientos.dto.RequerimientoOportunidadResponse;
import org.mgroko.backend.requerimientos.servicio.BuscarRequerimientosActividadGlobalService;
import org.mgroko.backend.requerimientos.servicio.BuscarRequerimientosGeneralesService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RequerimientoBusquedaController.class)
@AutoConfigureMockMvc(addFilters = false)
class RequerimientoBusquedaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuscarRequerimientosGeneralesService buscarGeneralesService;

    @MockitoBean
    private BuscarRequerimientosActividadGlobalService buscarActividadService;

    @Test
    void buscarGenerales_conPerfilActivo_devuelve200() throws Exception {
        when(buscarGeneralesService.buscar(
                any(), any(BuscarRequerimientosGlobalFiltro.class),
                anyInt(), anyInt(), anyBoolean()))
                .thenReturn(new PaginaResponse<>(
                        List.<RequerimientoOportunidadResponse>of(),
                        0, 20, 0, 0, true, true));

        mockMvc.perform(get("/requerimientos-generales/buscar")
                        .principal(autenticacion(1L, 10L))
                        .param("idProfesion", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void buscarActividades_conPerfilActivo_devuelve200() throws Exception {
        when(buscarActividadService.buscar(
                any(), any(BuscarRequerimientosGlobalFiltro.class),
                anyInt(), anyInt(), anyBoolean()))
                .thenReturn(new PaginaResponse<>(
                        List.<RequerimientoOportunidadResponse>of(),
                        0, 20, 0, 0, true, true));

        mockMvc.perform(get("/requerimientos-actividad/buscar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void buscarGenerales_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(get("/requerimientos-generales/buscar")
                        .principal(auth))
                .andExpect(status().isNotFound());

        verify(buscarGeneralesService, never())
                .buscar(any(), any(), anyInt(), anyInt(), anyBoolean());
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario, Long idPerfil) {
        var token = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(idUsuario, idPerfil, "Perfil activo"));
        return token;
    }
}
