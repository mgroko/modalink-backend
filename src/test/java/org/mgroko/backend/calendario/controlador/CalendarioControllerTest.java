package org.mgroko.backend.calendario.controlador;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.calendario.dto.BloqueoActividadResponse;
import org.mgroko.backend.calendario.dto.BloqueoResponse;
import org.mgroko.backend.calendario.dto.CalendarioResponse;
import org.mgroko.backend.calendario.dto.ConfigJornadaRequest;
import org.mgroko.backend.calendario.dto.ConfigJornadaResponse;
import org.mgroko.backend.calendario.dto.JornadaDiaRequest;
import org.mgroko.backend.calendario.dto.JornadaDiaResponse;
import org.mgroko.backend.calendario.dto.MarcarNoDisponibleRequest;
import org.mgroko.backend.calendario.exception.BloqueoNoEncontradoException;
import org.mgroko.backend.calendario.exception.BloqueoSolapadoException;
import org.mgroko.backend.calendario.exception.HorarioComprometidoException;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = CalendarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class CalendarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CalendarioService calendarioService;

    private static final CalendarioResponse CALENDARIO = new CalendarioResponse(
            new ConfigJornadaResponse(60, List.of(
                    new JornadaDiaResponse(1, LocalTime.of(9, 0), LocalTime.of(18, 0)))),
            List.of(new BloqueoResponse(1L,
                    LocalDateTime.of(2026, 9, 15, 10, 0),
                    LocalDateTime.of(2026, 9, 15, 14, 0), "X")),
            List.of(new BloqueoActividadResponse(5L, "Sesión",
                    LocalDateTime.of(2026, 9, 10, 9, 0),
                    LocalDateTime.of(2026, 9, 10, 13, 0))));

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    @Test
    void obtener_devuelve200ConCuerpo() throws Exception {
        when(calendarioService.obtener(1L)).thenReturn(CALENDARIO);

        mockMvc.perform(get("/calendario").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jornada.margenActividadMinutos").value(60))
                .andExpect(jsonPath("$.jornada.dias[0].diaSemana").value(1))
                .andExpect(jsonPath("$.bloqueosManuales[0].idBloqueo").value(1))
                .andExpect(jsonPath("$.actividades[0].nombre").value("Sesión"));
    }

    @Test
    void configurarJornada_valido_devuelve200() throws Exception {
        ConfigJornadaRequest request = new ConfigJornadaRequest(60, List.of(
                new JornadaDiaRequest(1, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(calendarioService.configurarJornada(anyLong(), any(ConfigJornadaRequest.class)))
                .thenReturn(new ConfigJornadaResponse(60, List.of(
                        new JornadaDiaResponse(1, LocalTime.of(9, 0), LocalTime.of(18, 0)))));

        mockMvc.perform(put("/calendario/jornada")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(calendarioService).configurarJornada(anyLong(), any(ConfigJornadaRequest.class));
    }

    @Test
    void configurarJornada_diasVacio_devuelve400() throws Exception {
        ConfigJornadaRequest request = new ConfigJornadaRequest(60, List.of());

        mockMvc.perform(put("/calendario/jornada")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(calendarioService, never()).configurarJornada(anyLong(), any());
    }

    @Test
    void marcarNoDisponible_valido_devuelve200() throws Exception {
        MarcarNoDisponibleRequest request = new MarcarNoDisponibleRequest(
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 15, 14, 0), "X");
        when(calendarioService.marcarNoDisponible(anyLong(), any(MarcarNoDisponibleRequest.class)))
                .thenReturn(new BloqueoResponse(3L, request.fechaHoraInicio(), request.fechaHoraFin(), "X"));

        mockMvc.perform(post("/calendario/bloqueos")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idBloqueo").value(3));
    }

    @Test
    void marcarNoDisponible_finNulo_devuelve400() throws Exception {
        MarcarNoDisponibleRequest request = new MarcarNoDisponibleRequest(
                LocalDateTime.of(2026, 9, 15, 10, 0), null, "X");

        mockMvc.perform(post("/calendario/bloqueos")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(calendarioService, never()).marcarNoDisponible(anyLong(), any());
    }

    @Test
    void marcarNoDisponible_solapaActividad_devuelve409() throws Exception {
        MarcarNoDisponibleRequest request = new MarcarNoDisponibleRequest(
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 15, 14, 0), "X");
        when(calendarioService.marcarNoDisponible(anyLong(), any(MarcarNoDisponibleRequest.class)))
                .thenThrow(new HorarioComprometidoException(
                        "El periodo ya se encuentra bloqueado automáticamente por una actividad de un proyecto activo."));

        mockMvc.perform(post("/calendario/bloqueos")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "El periodo ya se encuentra bloqueado automáticamente por una actividad de un proyecto activo."));
    }

    @Test
    void marcarNoDisponible_solapaManual_devuelve400() throws Exception {
        MarcarNoDisponibleRequest request = new MarcarNoDisponibleRequest(
                LocalDateTime.of(2026, 9, 15, 12, 0),
                LocalDateTime.of(2026, 9, 15, 16, 0), "X");
        when(calendarioService.marcarNoDisponible(anyLong(), any(MarcarNoDisponibleRequest.class)))
                .thenThrow(new BloqueoSolapadoException(
                        "El periodo se superpone con un bloqueo existente de tu calendario."));

        mockMvc.perform(post("/calendario/bloqueos")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El periodo se superpone con un bloqueo existente de tu calendario."));
    }

    @Test
    void marcarDisponible_valido_devuelve204() throws Exception {
        mockMvc.perform(delete("/calendario/bloqueos/3").principal(auth()))
                .andExpect(status().isNoContent());

        verify(calendarioService).marcarDisponible(1L, 3L);
    }

    @Test
    void marcarDisponible_bloqueoInexistente_devuelve404() throws Exception {
        doThrow(new BloqueoNoEncontradoException("El bloqueo no existe o no pertenece a tu calendario."))
                .when(calendarioService).marcarDisponible(anyLong(), anyLong());

        mockMvc.perform(delete("/calendario/bloqueos/99").principal(auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void marcarDisponible_solapaActividad_devuelve409() throws Exception {
        doThrow(new HorarioComprometidoException(
                "No se puede marcar como disponible un horario comprometido con una actividad de un proyecto activo."))
                .when(calendarioService).marcarDisponible(anyLong(), anyLong());

        mockMvc.perform(delete("/calendario/bloqueos/3").principal(auth()))
                .andExpect(status().isConflict());
    }
}