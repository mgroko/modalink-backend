package org.mgroko.backend.usuario.controlador;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.exception.EdadInvalidaException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.usuario.dto.DatosPersonalesRequest;
import org.mgroko.backend.usuario.dto.DatosPersonalesResponse;
import org.mgroko.backend.usuario.servicio.DatosPersonalesService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = DatosPersonalesController.class)
@AutoConfigureMockMvc(addFilters = false)
class DatosPersonalesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DatosPersonalesService datosPersonalesService;

    @Test
    void actualizar_datosValidos_devuelve200() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        DatosPersonalesResponse response = new DatosPersonalesResponse(
                1L, "Maria", "Flores",
                LocalDate.of(1990, 6, 15),
                "mujer", null);

        when(datosPersonalesService.actualizar(anyLong(), any(DatosPersonalesRequest.class)))
                .thenReturn(response);

        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "Maria", "Flores",
                LocalDate.of(1990, 6, 15),
                "mujer", null);

        mockMvc.perform(put("/usuario/datos-personales")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Maria"))
                .andExpect(jsonPath("$.apellido").value("Flores"))
                .andExpect(jsonPath("$.genero").value("mujer"));
    }

    @Test
    void actualizar_nombreVacio_devuelve400() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "", "Flores",
                LocalDate.of(1990, 6, 15),
                "mujer", null);

        mockMvc.perform(put("/usuario/datos-personales")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(datosPersonalesService, never()).actualizar(anyLong(), any());
    }

    @Test
    void actualizar_fechaFutura_devuelve400() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "Maria", "Flores",
                LocalDate.now().plusDays(1),
                "mujer", null);

        mockMvc.perform(put("/usuario/datos-personales")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(datosPersonalesService, never()).actualizar(anyLong(), any());
    }

    @Test
    void actualizar_usuarioNoExiste_devuelve401() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("999", null, List.of());

        when(datosPersonalesService.actualizar(anyLong(), any(DatosPersonalesRequest.class)))
                .thenThrow(new UsuarioNoEncontradoException("Usuario no encontrado."));

        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "Maria", "Flores",
                LocalDate.of(1990, 6, 15),
                "mujer", null);

        mockMvc.perform(put("/usuario/datos-personales")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado."));
    }

    @Test
    void actualizar_menorDeEdad_devuelve400() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, List.of());

        when(datosPersonalesService.actualizar(anyLong(), any(DatosPersonalesRequest.class)))
                .thenThrow(new EdadInvalidaException("El usuario debe ser mayor o igual a 18 años."));

        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "Ana", "Gomez",
                LocalDate.now().minusYears(10),
                "mujer", null);

        mockMvc.perform(put("/usuario/datos-personales")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El usuario debe ser mayor o igual a 18 años."));
    }
}
