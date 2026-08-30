package org.mgroko.backend.ubicacion.controlador;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;
import org.mgroko.backend.ubicacion.exception.LocalidadNoEncontradaException;
import org.mgroko.backend.ubicacion.servicio.UbicacionUsuarioService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UbicacionUsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UbicacionUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UbicacionUsuarioService ubicacionUsuarioService;

    private static final UbicacionResponse UBICACION = new UbicacionResponse(
            10L,
            "0208401002",
            "Saavedra",
            "Ciudad Autónoma de Buenos Aires",
            "Argentina",
            null,
            new BigDecimal("-34.5548978526608"),
            new BigDecimal("-58.4863271154338"));

    @Test
    void obtener_conUbicacion_devuelve200ConDatos() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(ubicacionUsuarioService.obtener(1L)).thenReturn(Optional.of(UBICACION));

        mockMvc.perform(get("/usuario/ubicacion").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUbicacion").value(10))
                .andExpect(jsonPath("$.localidadId").value("0208401002"))
                .andExpect(jsonPath("$.localidad").value("Saavedra"))
                .andExpect(jsonPath("$.provincia").value("Ciudad Autónoma de Buenos Aires"));
    }

    @Test
    void obtener_sinUbicacion_devuelve200SinCuerpo() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(ubicacionUsuarioService.obtener(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/usuario/ubicacion").principal(authentication))
                .andExpect(status().isOk());
    }

    @Test
    void asignar_requestValido_devuelve200ConUbicacion() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(ubicacionUsuarioService.asignar(anyLong(), any(UbicacionRequest.class)))
                .thenReturn(UBICACION);

        mockMvc.perform(put("/usuario/ubicacion")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UbicacionRequest("0208401002"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localidad").value("Saavedra"));
    }

    @Test
    void asignar_localidadVacia_devuelve400() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(put("/usuario/ubicacion")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UbicacionRequest(""))))
                .andExpect(status().isBadRequest());

        verify(ubicacionUsuarioService, never()).asignar(anyLong(), any());
    }

    @Test
    void asignar_localidadInexistente_devuelve400() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(ubicacionUsuarioService.asignar(anyLong(), any(UbicacionRequest.class)))
                .thenThrow(new LocalidadNoEncontradaException("Localidad no encontrada con id: 999"));

        mockMvc.perform(put("/usuario/ubicacion")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UbicacionRequest("999"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Localidad no encontrada con id: 999"));
    }

    @Test
    void asignar_usuarioInexistente_devuelve401() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("999", null, List.of());

        when(ubicacionUsuarioService.asignar(anyLong(), any(UbicacionRequest.class)))
                .thenThrow(new UsuarioNoEncontradoException("Usuario no encontrado."));

        mockMvc.perform(put("/usuario/ubicacion")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UbicacionRequest("0208401002"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void quitar_devuelve204() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(delete("/usuario/ubicacion").principal(authentication))
                .andExpect(status().isNoContent());

        verify(ubicacionUsuarioService).quitar(1L);
    }
}