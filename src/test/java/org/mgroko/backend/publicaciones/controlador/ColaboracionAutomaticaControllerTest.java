package org.mgroko.backend.publicaciones.controlador;

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
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoConfirmacionColaboracion;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.BuscarColaboracionesAutomaticasFiltro;
import org.mgroko.backend.publicaciones.dto.ColaboracionAutomaticaResponse;
import org.mgroko.backend.publicaciones.dto.ImagenColaboracionAutomaticaResponse;
import org.mgroko.backend.publicaciones.dto.PublicacionColaboracionAutomaticaResponse;
import org.mgroko.backend.publicaciones.servicio.BuscarColaboracionAutomaticaService;
import org.mgroko.backend.publicaciones.servicio.GestionarColaboracionAutomaticaService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = ColaboracionAutomaticaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ColaboracionAutomaticaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuscarColaboracionAutomaticaService buscarService;

    @MockitoBean
    private GestionarColaboracionAutomaticaService gestionarService;

    @Test
    void listar_devuelvePaginaDeColaboraciones() throws Exception {
        when(buscarService.buscar(
                eq(7L), any(BuscarColaboracionesAutomaticasFiltro.class), eq(0), eq(20)))
                .thenReturn(new PaginaResponse<>(
                        List.of(response()), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/colaboraciones-automaticas")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idColaboracionAutomatica").value(50L))
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenido[0].puedeConfirmar").value(true));
    }

    @Test
    void confirmar_devuelveLaColaboracion() throws Exception {
        when(gestionarService.confirmar(1L, 7L, 50L)).thenReturn(response());

        mockMvc.perform(post("/colaboraciones-automaticas/{id}/confirmar", 50L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idColaboracionAutomatica").value(50L));
    }

    @Test
    void listar_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(get("/colaboraciones-automaticas")
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    private static ColaboracionAutomaticaResponse response() {
        return new ColaboracionAutomaticaResponse(
                50L,
                EstadoColaboracionAutomatica.PENDIENTE,
                EstadoConfirmacionColaboracion.PENDIENTE,
                EstadoConfirmacionColaboracion.CONFIRMADA,
                LocalDateTime.of(2026, 11, 11, 12, 0),
                null,
                null,
                new PublicacionColaboracionAutomaticaResponse(
                        10L, "Editorial Otoño", "/uploads/a.webp", "ACTIVA", false),
                new PublicacionColaboracionAutomaticaResponse(
                        20L, "Campaña Verano", "/uploads/b.webp", "ACTIVA", false),
                new AutorPublicacionResponse(
                        8L, "Sol", "Fotógrafa", "/uploads/sol.webp"),
                new ImagenColaboracionAutomaticaResponse(99L, "/uploads/compartida.webp"),
                true,
                true);
    }

    private static UsernamePasswordAuthenticationToken auth(
            Long idUsuario,
            Long idPerfilActivo) {
        var token = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(
                idUsuario, idPerfilActivo, "Luna Diseños"));
        return token;
    }
}
