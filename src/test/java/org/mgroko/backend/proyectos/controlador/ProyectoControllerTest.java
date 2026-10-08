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

    @MockitoBean
    private org.mgroko.backend.proyectos.servicio.ObtenerProyectoService obtenerProyectoService;

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
                null,
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
                List.of(),
                List.of(),
                null
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
                null,
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
                null,
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
                null,
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

    @Test
    void crear_conRequerimientosYMoodboard_devuelve201ConCampos() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        String payload = """
                {
                  "nombre": "Desfile Requerimientos",
                  "descripcion": "Producción con equipo técnico",
                  "privacidad": "PUBLICO",
                  "fechaInicio": "2026-06-01",
                  "requerimientosGral": [
                    {
                      "cantidad": 2,
                      "idProfesion": 2,
                      "descripcion": "Dos modelos",
                      "caracteristicas": [
                        {
                          "idCaracteristica": 5,
                          "valores": [1]
                        }
                      ],
                      "habilidades": [1, 4]
                    }
                  ],
                  "moodboard": {
                    "descripcion": "Paleta fría"
                  }
                }
                """;

        var requeriaResp = new org.mgroko.backend.proyectos.dto.RequerimientoGralResponse(
                10L, 2, "Dos modelos", 2L, "Modelo",
                List.of(new org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse(
                        5L, "COLOR_OJOS", "ENUMERADO", null, null, List.of(1L))),
                List.of(1L, 4L));
        var moodboardResp = new org.mgroko.backend.proyectos.dto.MoodboardResponse(
                77L, "Paleta fría", java.time.LocalDateTime.of(2026, 6, 1, 10, 0));

        ProyectoResponse response = new ProyectoResponse(
                101L,
                "Desfile Requerimientos",
                "Producción con equipo técnico",
                LocalDate.of(2026, 6, 1),
                null,
                "Borrador",
                "Publico",
                false,
                null,
                10L,
                "Luna Diseños",
                List.of(),
                List.of(requeriaResp),
                moodboardResp
        );

        when(crearProyectoService.crear(eq(1L), eq(10L), any(CrearProyectoRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/proyectos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProyecto").value(101L))
                .andExpect(jsonPath("$.requerimientosGral[0].idRequerimientoGral").value(10L))
                .andExpect(jsonPath("$.requerimientosGral[0].cantidad").value(2))
                .andExpect(jsonPath("$.requerimientosGral[0].idProfesion").value(2))
                .andExpect(jsonPath("$.requerimientosGral[0].caracteristicas[0].idCaracteristica").value(5))
                .andExpect(jsonPath("$.requerimientosGral[0].caracteristicas[0].valores[0]").value(1))
                .andExpect(jsonPath("$.requerimientosGral[0].habilidades[0]").value(1))
                .andExpect(jsonPath("$.moodboard.idMoodboard").value(77L))
                .andExpect(jsonPath("$.moodboard.descripcion").value("Paleta fría"));
    }

    @Test
    void crear_conRequerimientoInvalido_devuelve400() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        String payload = """
                {
                  "nombre": "Desfile Invalido",
                  "descripcion": "Producción",
                  "privacidad": "PUBLICO",
                  "fechaInicio": "2026-06-01",
                  "requerimientosGral": [
                    {
                      "cantidad": 0,
                      "idProfesion": 2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/proyectos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['requerimientosGral[0].cantidad']").exists());
    }

    // ---------------------------------------------------------------------
    // GET /proyectos/{id} — dashboard
    // ---------------------------------------------------------------------

    @Test
    void obtener_proyectoExistente_devuelve200() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        ProyectoResponse response = new ProyectoResponse(
                100L,
                "Desfile Primavera",
                "Colección",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15),
                "Borrador",
                "PUBLICO",
                false,
                null,
                10L,
                "Luna Diseños",
                List.of(),
                List.of(),
                null
        );

        when(obtenerProyectoService.obtener(eq(1L), eq(10L), eq(100L))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/100")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProyecto").value(100L))
                .andExpect(jsonPath("$.nombre").value("Desfile Primavera"))
                .andExpect(jsonPath("$.estado").value("Borrador"))
                .andExpect(jsonPath("$.idDirector").value(10L));
    }

    @Test
    void obtener_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/100")
                        .principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay un perfil activo seleccionado en la sesión."));
    }

    @Test
    void obtener_proyectoInexistente_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        when(obtenerProyectoService.obtener(eq(1L), eq(10L), eq(999L)))
                .thenThrow(new org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException(999L));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/999")
                        .principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El proyecto con id 999 no existe."));
    }

    @Test
    void obtener_accesoDenegado_devuelve403() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        when(obtenerProyectoService.obtener(eq(1L), eq(10L), eq(200L)))
                .thenThrow(new org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException(
                        "No tienes acceso a este proyecto."));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/200")
                        .principal(auth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes acceso a este proyecto."));
    }
}
