package org.mgroko.backend.proyectos.controlador;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.proyectos.servicio.ModificarProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = RequerimientoGralProyectoController.class)
@AutoConfigureMockMvc(addFilters = false)
class RequerimientoGralProyectoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ModificarProyectoService modificarProyectoService;

    @Test
    void crear_conPerfilActivo_devuelve201() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        var request = new org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest(
                1, 2L, "nuevo", null, null);
        var response = new RequerimientoGralResponse(
                30L, 1, "nuevo", 2L, "Modelo", List.of(), List.of(),
                false, 0L, 1, false, false);

        when(modificarProyectoService.crearRequerimiento(
                eq(1L), eq(10L), eq(100L), any()))
                .thenReturn(response);

        mockMvc.perform(post("/proyectos/100/requerimientos-generales")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRequerimientoGral").value(30L))
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.idProfesion").value(2L));
    }

    @Test
    void crear_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        var request = new org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest(
                1, 2L, "nuevo", null, null);

        mockMvc.perform(post("/proyectos/100/requerimientos-generales")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No hay un perfil activo seleccionado en la sesión."));
    }

    @Test
    void crear_cantidadInvalida_devuelve400() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        mockMvc.perform(post("/proyectos/100/requerimientos-generales")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cantidad":0,"idProfesion":2}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminar_conPerfilActivo_devuelve204() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        doNothing().when(modificarProyectoService)
                .eliminarRequerimiento(1L, 10L, 100L, 30L);

        mockMvc.perform(delete("/proyectos/100/requerimientos-generales/30")
                        .principal(auth))
                .andExpect(status().isNoContent());

        verify(modificarProyectoService).eliminarRequerimiento(1L, 10L, 100L, 30L);
    }
}
