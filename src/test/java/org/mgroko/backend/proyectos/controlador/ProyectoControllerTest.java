package org.mgroko.backend.proyectos.controlador;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.NombreProyectoDuplicadoException;
import org.mgroko.backend.proyectos.exception.FechaEntregaInvalidaException;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    @MockitoBean
    private org.mgroko.backend.proyectos.servicio.BuscarProyectoService buscarProyectoService;

    @MockitoBean
    private org.mgroko.backend.proyectos.servicio.BuscarProyectosPropiosService buscarProyectosPropiosService;

    @Test
    void crear_conPerfilActivo_devuelve201() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile Primavera",
                "Colección primavera-verano",
                Privacidad.Publico,
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
    void crear_fechaEntregaAnteriorAHoy_devuelve400() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile",
                "Colección",
                Privacidad.Publico,
                LocalDate.now().minusDays(1),
                false,
                null,
                null,
                null,
                null
        );

        when(crearProyectoService.crear(eq(1L), eq(10L), any(CrearProyectoRequest.class)))
                .thenThrow(new FechaEntregaInvalidaException(
                        "La fecha de entrega estipulada no puede ser anterior a la fecha actual."));

        mockMvc.perform(post("/proyectos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La fecha de entrega estipulada no puede ser anterior a la fecha actual."));
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
                  "fechaEntregaEstipulada": "2026-06-15",
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
                77L, "Paleta fría", java.time.LocalDateTime.of(2026, 6, 1, 10, 0), List.of());

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

    // ---------------------------------------------------------------------
    // GET /proyectos/buscar — UC-29 Buscar proyecto
    // ---------------------------------------------------------------------

    @Test
    void buscar_conFiltros_devuelve200ConResultados() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        var contenido = List.of(
                new org.mgroko.backend.proyectos.dto.ProyectoBusquedaResponse(
                        100L, "Desfile Primavera", "Colección primavera",
                        LocalDate.of(2026, 6, 1), "Borrador", "Publico",
                        false, 10L, "Luna Diseños"));
        var pagina = new org.mgroko.backend.common.dto.PaginaResponse<>(
                contenido, 0, 20, 1, 1, true, true);

        when(buscarProyectoService.buscarProyectos(
                org.mockito.ArgumentMatchers.eq(10L),
                any(org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro.class),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(20),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(pagina);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/buscar")
                        .param("nombre", "Desfile")
                        .param("estado", "PUBLICADO")
                        .param("director", "Luna")
                        .param("page", "0")
                        .param("size", "20")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].idProyecto").value(100L))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Desfile Primavera"))
                .andExpect(jsonPath("$.contenido[0].estado").value("Borrador"))
                .andExpect(jsonPath("$.contenido[0].nombreDirector").value("Luna Diseños"));

        org.mockito.ArgumentCaptor<org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro> captor =
                org.mockito.ArgumentCaptor.forClass(org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro.class);
        verify(buscarProyectoService).buscarProyectos(
                org.mockito.ArgumentMatchers.eq(10L),
                captor.capture(),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(20),
                org.mockito.ArgumentMatchers.eq(false));

        var filtro = captor.getValue();
        assertThat(filtro.nombre()).isEqualTo("Desfile");
        assertThat(filtro.estado()).isEqualTo(org.mgroko.backend.modelo.enums.EstadoProyecto.PUBLICADO);
        assertThat(filtro.director()).isEqualTo("Luna");
    }

    @Test
    void buscar_sinResultados_devuelve200ListaVacia() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        var pagina = new org.mgroko.backend.common.dto.PaginaResponse<org.mgroko.backend.proyectos.dto.ProyectoBusquedaResponse>(
                List.of(), 0, 20, 0, 0, true, true);

        when(buscarProyectoService.buscarProyectos(
                org.mockito.ArgumentMatchers.eq(10L),
                any(org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro.class),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(pagina);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/buscar")
                        .param("nombre", "Inexistente")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void buscar_sinCriterios_devuelve200ConTodosLosVisibles() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        var contenido = List.of(
                new org.mgroko.backend.proyectos.dto.ProyectoBusquedaResponse(
                        100L, "Desfile Primavera", "Colección primavera",
                        LocalDate.of(2026, 6, 1), "Publicado", "Publico",
                        false, 10L, "Luna Diseños"));
        var pagina = new org.mgroko.backend.common.dto.PaginaResponse<>(contenido, 0, 20, 1, 1, true, true);

        when(buscarProyectoService.buscarProyectos(
                org.mockito.ArgumentMatchers.eq(10L),
                any(org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro.class),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(pagina);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/buscar")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Desfile Primavera"));
    }

    @Test
    void buscar_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/buscar")
                        .param("nombre", "Desfile")
                        .principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay un perfil activo seleccionado en la sesión."));
    }

    // ---------------------------------------------------------------------
    // GET /proyectos/propios — UC-29 bis Buscar proyectos propios
    // ---------------------------------------------------------------------

    @Test
    void buscarPropios_conPerfilActivo_devuelve200ConRol() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        var contenido = List.of(
                new org.mgroko.backend.proyectos.dto.ProyectoPropioResponse(
                        100L, "Desfile Primavera", "Colección",
                        LocalDate.of(2026, 6, 1), "Borrador", "Publico",
                        org.mgroko.backend.modelo.enums.RolProyectoUsuario.DIRECTOR,
                        new org.mgroko.backend.proyectos.dto.ActividadResumenResponse(
                                500L, "Sesion",
                                java.time.LocalDateTime.of(2026, 6, 1, 15, 0), 180)));
        var pagina = new org.mgroko.backend.common.dto.PaginaResponse<org.mgroko.backend.proyectos.dto.ProyectoPropioResponse>(
                contenido, 0, 20, 1, 1, true, true);

        when(buscarProyectosPropiosService.buscar(
                org.mockito.ArgumentMatchers.eq(10L),
                any(org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro.class),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(20),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(pagina);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/propios")
                        .param("nombre", "Desfile")
                        .param("estado", "PUBLICADO")
                        .param("rol", "DIRECTOR")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].idProyecto").value(100L))
                .andExpect(jsonPath("$.contenido[0].rolUsuario").value("DIRECTOR"))
                .andExpect(jsonPath("$.contenido[0].proximaActividad.nombre").value("Sesion"))
                .andExpect(jsonPath("$.contenido[0].proximaActividad.duracionMinutos").value(180));

        org.mockito.ArgumentCaptor<org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro> captor =
                org.mockito.ArgumentCaptor.forClass(org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro.class);
        verify(buscarProyectosPropiosService).buscar(
                org.mockito.ArgumentMatchers.eq(10L),
                captor.capture(),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(20),
                org.mockito.ArgumentMatchers.eq(false));

        var filtro = captor.getValue();
        assertThat(filtro.nombre()).isEqualTo("Desfile");
        assertThat(filtro.estado()).isEqualTo(org.mgroko.backend.modelo.enums.EstadoProyecto.PUBLICADO);
        assertThat(filtro.rol()).isEqualTo(org.mgroko.backend.modelo.enums.RolProyectoUsuario.DIRECTOR);
    }

    @Test
    void buscarPropios_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/propios")
                        .principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay un perfil activo seleccionado en la sesión."));
    }

    @Test
    void buscarPropios_sinFiltros_devuelve200ListaVacia() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna Diseños"));

        var pagina = new org.mgroko.backend.common.dto.PaginaResponse<org.mgroko.backend.proyectos.dto.ProyectoPropioResponse>(
                List.of(), 0, 20, 0, 0, true, true);

        when(buscarProyectosPropiosService.buscar(
                org.mockito.ArgumentMatchers.eq(10L),
                any(org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro.class),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(pagina);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/proyectos/propios")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }
}
