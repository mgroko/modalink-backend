package org.mgroko.backend.proyectos.controlador;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.NombreProyectoDuplicadoException;
import org.mgroko.backend.proyectos.exception.RangoFechasProyectoInvalidoException;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ProyectoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProyectoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CrearProyectoService crearProyectoService;

    @Test
    void crear_conPerfilActivo_devuelve201() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile Primavera",
                "Colección primavera-verano",
                Privacidad.Publico,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20),
                true,
                null,
                null
        );

        ProyectoResponse response = new ProyectoResponse(
                100L,
                "Desfile Primavera",
                "Colección primavera-verano",
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20),
                "Borrador",
                "Publico",
                true,
                null,
                10L,
                "Luna Diseños",
                List.of()
        );

        when(crearProyectoService.crear(eq(1L), eq(10L), any(CrearProyectoRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/proyectos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProyecto").value(100L))
                .andExpect(jsonPath("$.nombre").value("Desfile Primavera"))
                .andExpect(jsonPath("$.estado").value("Borrador"))
                .andExpect(jsonPath("$.idDirector").value(10L));
    }

    @Test
    void crear_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile Primavera",
                "Colección primavera-verano",
                Privacidad.Publico,
                LocalDate.now().plusDays(10),
                null,
                false,
                null,
                null
        );

        mockMvc.perform(post("/proyectos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay un perfil activo seleccionado en la sesión."));
    }

    @Test
    void crear_nombreDuplicado_devuelve409() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile Repetido",
                "Colección primavera-verano",
                Privacidad.Publico,
                LocalDate.now().plusDays(10),
                null,
                false,
                null,
                null
        );

        when(crearProyectoService.crear(eq(1L), eq(10L), any(CrearProyectoRequest.class)))
                .thenThrow(new NombreProyectoDuplicadoException("Desfile Repetido"));

        mockMvc.perform(post("/proyectos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya tienes un proyecto con el nombre 'Desfile Repetido'."));
    }

    @Test
    void crear_fechasInvalidas_devuelve400() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile",
                "Colección",
                Privacidad.Publico,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 5),
                false,
                null,
                null
        );

        when(crearProyectoService.crear(eq(1L), eq(10L), any(CrearProyectoRequest.class)))
                .thenThrow(new RangoFechasProyectoInvalidoException("La fecha de finalización o entrega no puede ser anterior a la fecha de inicio."));

        mockMvc.perform(post("/proyectos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de finalización o entrega no puede ser anterior a la fecha de inicio."));
    }
}
