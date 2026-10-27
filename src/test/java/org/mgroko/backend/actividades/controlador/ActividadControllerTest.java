package org.mgroko.backend.actividades.controlador;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadBusquedaResponse;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.BuscarActividadesFiltro;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.ModificarActividadRequest;
import org.mgroko.backend.actividades.dto.ProfesionalAsignadoResponse;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResumenResponse;
import org.mgroko.backend.actividades.exception.ActividadCanceladaException;
import org.mgroko.backend.actividades.exception.CicloActividadException;
import org.mgroko.backend.actividades.exception.RangoDuracionInvalidoException;
import org.mgroko.backend.actividades.exception.RequerimientoConVinculosException;
import org.mgroko.backend.actividades.exception.TransicionEstadoActividadInvalidaException;
import org.mgroko.backend.actividades.servicio.BuscarActividadService;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.actividades.servicio.EliminarActividadService;
import org.mgroko.backend.actividades.servicio.ModificarActividadService;
import org.mgroko.backend.actividades.servicio.ObtenerActividadService;
import org.mgroko.backend.actividades.servicio.ObtenerDisponibilidadActividadService;
import org.mgroko.backend.calendario.dto.DetalleMiembroDisponibilidad;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.servicio.DisponibilidadCalculator.Intervalo;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;

@WebMvcTest(controllers = ActividadController.class)
@AutoConfigureMockMvc(addFilters = false)
class ActividadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuscarActividadService buscarActividadService;

    @MockitoBean
    private CrearActividadService crearActividadService;

    @MockitoBean
    private ModificarActividadService modificarActividadService;

    @MockitoBean
    private ObtenerActividadService obtenerActividadService;

    @MockitoBean
    private ObtenerDisponibilidadActividadService obtenerDisponibilidadActividadService;

    @MockitoBean
    private EliminarActividadService eliminarActividadService;

    @Test
    void crear_conDatosValidos_devuelve201() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        ActividadResponse response = new ActividadResponse(
                55L,
                100L,
                "Desfile Primavera",
                "Montaje",
                "Montar escenario",
                90,
                LocalDateTime.of(2026, 11, 1, 22, 29),
                LocalDateTime.of(2026, 11, 1, 23, 59),
                null,
                "PENDIENTE",
                "NORMAL",
                null,          // tentative
                null,          // critica
                0L,            // holguraMakespan
                null,          // holguraEntrega
                null,          // rangoInicio
                null,          // rangoEjecucion
                null,          // ubicacion
                List.of(),     // predecesoras
                List.of(),     // requerimientos
                List.of()      // profesionalesAsignados
        );

        when(crearActividadService.crear(
                eq(1L), eq(10L), eq(100L), any(CrearActividadRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/proyectos/100/actividades")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Montaje",
                                  "descripcion": "Montar escenario",
                                  "duracion": 1,
                                  "unidadDuracion": "HORAS"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idActividad").value(55L))
                .andExpect(jsonPath("$.duracionMinutos").value(90));
    }

    @Test
    void modificar_conDatosValidos_devuelve200() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        ActividadResponse response = new ActividadResponse(
                55L,
                100L,
                "Desfile Primavera",
                "Montaje final",
                "Montar escenario",
                90,
                LocalDateTime.of(2026, 11, 1, 22, 29),
                LocalDateTime.of(2026, 11, 1, 23, 59),
                null,
                "EN_CURSO",
                "NORMAL",
                null,          // tentative
                null,          // critica
                0L,            // holguraMakespan
                null,          // holguraEntrega
                null,          // rangoInicio
                null,          // rangoEjecucion
                null,          // ubicacion
                List.of(),     // predecesoras
                List.of(),     // requerimientos
                List.of());    // profesionalesAsignados

        when(modificarActividadService.modificar(
                eq(1L), eq(10L), eq(100L), eq(55L), any(org.mgroko.backend.actividades.dto.ModificarActividadRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/proyectos/100/actividades/55")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Montaje final",
                                  "descripcion": "Montar escenario",
                                  "duracion": 2,
                                  "unidadDuracion": "HORAS",
                                  "predecesoras": [],
                                  "requerimientos": [],
                                  "estado": "EN_CURSO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idActividad").value(55L))
                .andExpect(jsonPath("$.nombre").value("Montaje final"))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.estadoCronograma").value("NORMAL"));
    }

    @Test
    void obtener_conPerfilActivo_devuelveDetalleCompleto() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        ActividadResponse response = new ActividadResponse(
                55L,
                100L,
                "Desfile Primavera",
                "Montaje final",
                "Montar escenario",
                90,
                LocalDateTime.of(2026, 11, 1, 22, 29),
                LocalDateTime.of(2026, 11, 1, 23, 59),
                null,
                "EN_CURSO",
                "NORMAL",
                null,          // tentative
                null,          // critica
                0L,            // holguraMakespan
                null,          // holguraEntrega
                null,          // rangoInicio
                null,          // rangoEjecucion
                null,          // ubicacion
                List.of(),     // predecesoras
                List.of(),     // requerimientos
                List.of(new ProfesionalAsignadoResponse(
                        77L,
                        88L,
                        10L,
                        1L,
                        "Luna",
                        2L,
                        "Modelo"))  // profesionalesAsignados
        );

        when(obtenerActividadService.obtener(1L, 10L, 100L, 55L))
                .thenReturn(response);

        mockMvc.perform(get("/proyectos/100/actividades/55").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idActividad").value(55L))
                .andExpect(jsonPath("$.estadoCronograma").value("NORMAL"))
                .andExpect(jsonPath("$.profesionalesAsignados[0].idPerfil").value(10L))
                .andExpect(jsonPath("$.profesionalesAsignados[0].nombreArtistico").value("Luna"));
    }

    @Test
    void obtenerDisponibilidad_sinIdPerfil_evaluaPerfilActivo() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        LocalDateTime inicioHueco = LocalDateTime.of(2026, 11, 1, 10, 0);
        LocalDateTime finHueco = LocalDateTime.of(2026, 11, 1, 11, 0);
        ResultadoDisponibilidad response = new ResultadoDisponibilidad(
                true,
                true,
                LocalDateTime.of(2026, 11, 1, 9, 0),
                LocalDateTime.of(2026, 11, 1, 18, 0),
                inicioHueco,
                finHueco,
                null,
                List.of(new DetalleMiembroDisponibilidad(
                        1L,
                        LocalDateTime.of(2026, 11, 1, 9, 0),
                        LocalDateTime.of(2026, 11, 1, 18, 0),
                        List.of(new Intervalo(inicioHueco, finHueco)),
                        true,
                        null)));

        when(obtenerDisponibilidadActividadService.obtener(1L, 10L, 100L, 55L, null))
                .thenReturn(response);

        mockMvc.perform(get("/proyectos/100/actividades/55/disponibilidad").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.provisional").value(true))
                .andExpect(jsonPath("$.inicioSeleccionado").value("2026-11-01T10:00:00"))
                .andExpect(jsonPath("$.inicio").doesNotExist())
                .andExpect(jsonPath("$.fin").doesNotExist())
                .andExpect(jsonPath("$.miembros[0].idUsuario").value(1L))
                .andExpect(jsonPath("$.miembros[0].huecos[0].inicio").value("2026-11-01T10:00:00"))
                .andExpect(jsonPath("$.miembros[0].huecos[0].minutos").doesNotExist());
    }

    @Test
    void obtenerDisponibilidad_conVariosPerfiles_evaluaHuecoComun() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        ResultadoDisponibilidad response = new ResultadoDisponibilidad(
                false,
                false,
                LocalDateTime.of(2026, 11, 1, 9, 0),
                LocalDateTime.of(2026, 11, 1, 18, 0),
                null,
                null,
                "SIN_HUECO_COMUN",
                List.of());

        when(obtenerDisponibilidadActividadService.obtener(
                eq(1L), eq(10L), eq(100L), eq(55L), eq(List.of(10L, 11L))))
                .thenReturn(response);

        mockMvc.perform(get("/proyectos/100/actividades/55/disponibilidad")
                        .principal(auth)
                        .param("idPerfil", "10")
                        .param("idPerfil", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.motivo").value("SIN_HUECO_COMUN"));

        verify(obtenerDisponibilidadActividadService).obtener(
                1L, 10L, 100L, 55L, List.of(10L, 11L));
    }

    @Test
    void obtenerDisponibilidad_actividadCancelada_devuelve409() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        when(obtenerDisponibilidadActividadService.obtener(1L, 10L, 100L, 55L, null))
                .thenThrow(new ActividadCanceladaException(55L));

        mockMvc.perform(get("/proyectos/100/actividades/55/disponibilidad").principal(auth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "La actividad 55 está cancelada y no admite consultas de disponibilidad."));
    }

    @Test
    void obtenerDisponibilidad_sinPerfilActivo_devuelve404() throws Exception {        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        mockMvc.perform(get("/proyectos/100/actividades/55/disponibilidad").principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No hay un perfil activo seleccionado en la sesión."));

        verify(obtenerDisponibilidadActividadService, never()).obtener(
                any(), any(), any(), any(), any());
    }

    @Test
    void buscar_conPerfilActivo_devuelvePagina() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        org.mgroko.backend.modelo.Actividad actividad = org.mgroko.backend.modelo.Actividad.builder()
                .idActividad(10L)
                .nombre("Montaje")
                .descripcion("Montar escenario")
                .duracionMinutos(90)
                .build();

        org.mgroko.backend.modelo.Planificacion planificacion = org.mgroko.backend.modelo.Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(org.mgroko.backend.modelo.Proyecto.builder()
                        .idProyecto(100L)
                        .nombre("Desfile Primavera")
                        .build())
                .fechaEntrega(java.time.LocalDate.of(2026, 11, 10))
                .build();

        actividad.setPlanificacion(planificacion);

        ActividadBusquedaResponse activBusqueda = new ActividadBusquedaResponse(
                10L,
                100L,
                "Desfile Primavera",
                "Montaje",
                "Montar escenario",
                90,
                LocalDateTime.of(2026, 11, 1, 9, 0),
                LocalDateTime.of(2026, 11, 1, 10, 30),
                "PENDIENTE",
                List.of(new RequerimientoActividadResumenResponse(
                        7L, 2, "Dos modelos", 2L, "Modelo", 1L, 1, false, true)),
                List.of());

        org.mgroko.backend.common.dto.PaginaResponse<ActividadBusquedaResponse> pagina = new org.mgroko.backend.common.dto.PaginaResponse<>(
                List.of(activBusqueda), 0, 20, 1, 1, true, true);

        when(buscarActividadService.buscar(
                eq(10L), eq(100L), any(BuscarActividadesFiltro.class),
                eq(0), eq(20), eq(false)))
                .thenReturn(pagina);

        mockMvc.perform(get("/proyectos/100/actividades/buscar")
                        .principal(auth)
                        .param("nombre", "mont")
                        .param("duracionMin", "60")
                        .param("duracionMax", "120")
                        .param("estado", "PENDIENTE")
                        .param("requerimiento", "modelo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idActividad").value(10L))
                .andExpect(jsonPath("$.contenido[0].idProyecto").value(100L))
                .andExpect(jsonPath("$.contenido[0].nombreProyecto").value("Desfile Primavera"))
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenido[0].requerimientos[0].nombreProfesion").value("Modelo"))
                .andExpect(jsonPath("$.contenido[0].requerimientos[0].cubiertos").value(1))
                .andExpect(jsonPath("$.contenido[0].requerimientos[0].faltantes").value(1))
                .andExpect(jsonPath("$.contenido[0].requerimientos[0].satisfecho").value(false))
                .andExpect(jsonPath("$.contenido[0].requerimientos[0].puedePostularse").value(true))
                .andExpect(jsonPath("$.totalElementos").value(1));

        verify(buscarActividadService).buscar(
                eq(10L), eq(100L),
                org.mockito.ArgumentMatchers.argThat(filtro ->
                        filtro != null
                                && "mont".equals(filtro.nombre())
                                && Integer.valueOf(60).equals(filtro.duracionMin())
                                && Integer.valueOf(120).equals(filtro.duracionMax())
                                && java.util.List.of(EstadoActividad.PENDIENTE).equals(filtro.estado())
                                && "modelo".equals(filtro.requerimiento())),
                eq(0), eq(20), eq(false));

        verify(buscarActividadService).buscar(
                eq(10L), eq(100L),
                org.mockito.ArgumentMatchers.argThat(filtro ->
                        filtro != null
                                && "mont".equals(filtro.nombre())
                                && Integer.valueOf(60).equals(filtro.duracionMin())
                                && Integer.valueOf(120).equals(filtro.duracionMax())
                                && java.util.List.of(EstadoActividad.PENDIENTE).equals(filtro.estado())
                                && "modelo".equals(filtro.requerimiento())),
                eq(0), eq(20), eq(false));
    }

    @Test
    void buscar_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        mockMvc.perform(get("/proyectos/100/actividades/buscar").principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No hay un perfil activo seleccionado en la sesión."));

        verify(buscarActividadService, never()).buscar(
                any(), any(), any(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void buscar_rangoDuracionInvalido_devuelve400() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        when(buscarActividadService.buscar(
                eq(10L), eq(100L), any(BuscarActividadesFiltro.class),
                eq(0), eq(20), eq(false)))
                .thenThrow(new org.mgroko.backend.actividades.exception.RangoDuracionInvalidoException(
                        "La duración mínima no puede ser mayor que la duración máxima."));

        mockMvc.perform(get("/proyectos/100/actividades/buscar")
                        .principal(auth)
                        .param("duracionMin", "120")
                        .param("duracionMax", "60"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La duración mínima no puede ser mayor que la duración máxima."));
    }
}