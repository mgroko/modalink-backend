package org.mgroko.backend.notificaciones.controlador;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;
import org.mgroko.backend.notificaciones.dto.NotificacionResumenResponse;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacionService notificacionService;

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken("10", null, List.of());
    }

    private NotificacionResponse notificacion() {
        return new NotificacionResponse(
                30L, "HITO_CUMPLIDO", "Hito cumplido", "Hito completado", "HITO", 40L,
                false, null, "SERVICIO", LocalDateTime.of(2026, 10, 21, 12, 0),
                20L, 105L, null, 40L, null);
    }

    @Test
    void listar_usaUsuarioAutenticadoYFiltroDePerfil() throws Exception {
        PaginaResponse<NotificacionResponse> pagina = new PaginaResponse<>(
                List.of(notificacion()), 0, 10, 1, 1, true, true);
        when(notificacionService.listar(10L, 20L, 0, 10)).thenReturn(pagina);

        mockMvc.perform(get("/notificaciones")
                        .param("idPerfilContexto", "20")
                        .param("page", "0")
                        .param("size", "10")
                        .principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idNotificacion").value(30))
                .andExpect(jsonPath("$.contenido[0].idPerfilContexto").value(20))
                .andExpect(jsonPath("$.contenido[0].idProyecto").value(105))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Hito cumplido"))
                .andExpect(jsonPath("$.contenido[0].idHito").value(40))
                .andExpect(jsonPath("$.contenido[0].idActividad").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.contenido[0].idInvitacion").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listar_sinPerfil_noEnviaFiltro() throws Exception {
        PaginaResponse<NotificacionResponse> pagina = new PaginaResponse<>(
                List.of(), 0, 20, 0, 0, true, true);
        when(notificacionService.listar(10L, null, 0, 20)).thenReturn(pagina);

        mockMvc.perform(get("/notificaciones").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty());
    }

    @Test
    void resumen_devuelveTotales() throws Exception {
        when(notificacionService.resumen(10L, 20L))
                .thenReturn(new NotificacionResumenResponse(5, 2));

        mockMvc.perform(get("/notificaciones/resumen")
                        .param("idPerfilContexto", "20")
                        .principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.noLeidas").value(2));
    }

    @Test
    void marcarLeida_devuelveNotificacionActualizada() throws Exception {
        NotificacionResponse leida = new NotificacionResponse(
                30L, "HITO_CUMPLIDO", "Hito cumplido", "Hito completado", "HITO", 40L,
                true, LocalDateTime.of(2026, 10, 21, 13, 0), "SERVICIO",
                LocalDateTime.of(2026, 10, 21, 12, 0), 20L, null, null, 40L, null);
        when(notificacionService.marcarLeida(10L, 30L)).thenReturn(leida);

        mockMvc.perform(patch("/notificaciones/30/leida").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true))
                .andExpect(jsonPath("$.fechaLeida").exists());
    }

    @Test
    void marcarTodas_devuelveCantidadActualizada() throws Exception {
        when(notificacionService.marcarTodasLeidas(10L, 20L)).thenReturn(3);

        mockMvc.perform(patch("/notificaciones/leidas")
                        .param("idPerfilContexto", "20")
                        .principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizadas").value(3));
    }
}
