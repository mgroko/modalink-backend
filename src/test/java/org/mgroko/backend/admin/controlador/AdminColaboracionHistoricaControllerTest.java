package org.mgroko.backend.admin.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.ErrorProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.PreviewProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.ProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.servicio.AdminColaboracionHistoricaService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = AdminColaboracionHistoricaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminColaboracionHistoricaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminColaboracionHistoricaService service;

    @Test
    void previsualizar_devuelveContadores() throws Exception {
        when(service.previsualizar(any())).thenReturn(
                new PreviewProcesamientoHistoricoResponse(
                        10, 20, 2, 3, 0, 5));

        mockMvc.perform(post("/admin/colaboraciones-automaticas/historicas/previsualizar")
                        .principal(auth(1L))
                        .contentType("application/json")
                        .content("""
                                {"modo":"AMBOS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicaciones").value(10))
                .andExpect(jsonPath("$.imagenes").value(20))
                .andExpect(jsonPath("$.paresCandidatosEstimados").value(5));
    }

    @Test
    void procesar_devuelveAccepted() throws Exception {
        when(service.procesar(any(), eq(1L))).thenReturn(response());

        mockMvc.perform(post("/admin/colaboraciones-automaticas/historicas/procesar")
                        .principal(auth(1L))
                        .contentType("application/json")
                        .content("""
                                {"modo":"AMBOS","notificar":false,"tamanoLote":50}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idProcesamiento").value(7L))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.notificar").value(false));
    }

    @Test
    void listar_devuelvePagina() throws Exception {
        when(service.listar(any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new PaginaResponse<>(
                        List.of(response()), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/admin/colaboraciones-automaticas/historicas")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idProcesamiento").value(7L))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void obtener_devuelveDetalle() throws Exception {
        when(service.obtener(7L)).thenReturn(response());

        mockMvc.perform(get("/admin/colaboraciones-automaticas/historicas/7")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProcesamiento").value(7L));
    }

    @Test
    void listarErrores_devuelvePagina() throws Exception {
        when(service.listarErrores(7L, 0, 20)).thenReturn(
                new PaginaResponse<>(
                        List.of(new ErrorProcesamientoHistoricoResponse(
                                1L, 99L, 10L, "a.webp", "error",
                                LocalDateTime.of(2026, 11, 12, 12, 0))),
                        0, 20, 1, 1, true, true));

        mockMvc.perform(get("/admin/colaboraciones-automaticas/historicas/7/errores")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].mensaje").value("error"));
    }

    @Test
    void cancelar_devuelveEstado() throws Exception {
        when(service.cancelar(7L, 1L)).thenReturn(response());

        mockMvc.perform(post("/admin/colaboraciones-automaticas/historicas/7/cancelar")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProcesamiento").value(7L));
    }

    @Test
    void reanudar_devuelveAccepted() throws Exception {
        when(service.reanudar(7L, 1L)).thenReturn(response());

        mockMvc.perform(post("/admin/colaboraciones-automaticas/historicas/7/reanudar")
                        .principal(auth(1L)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idProcesamiento").value(7L));
    }

    private static ProcesamientoHistoricoResponse response() {
        return new ProcesamientoHistoricoResponse(
                7L,
                EstadoProcesamientoHistorico.PENDIENTE,
                ModoProcesamientoHistorico.AMBOS,
                null, null, null, null,
                false, 50, null,
                10, 0, 20, 0, 0, 0, 0, 0,
                null, 1L,
                LocalDateTime.of(2026, 11, 12, 12, 0),
                null, null);
    }

    private static UsernamePasswordAuthenticationToken auth(Long idUsuario) {
        var token = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(idUsuario, null, null));
        return token;
    }
}
