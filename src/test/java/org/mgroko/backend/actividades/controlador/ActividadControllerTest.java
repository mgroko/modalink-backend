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
import org.mgroko.backend.actividades.exception.CicloActividadException;
import org.mgroko.backend.actividades.exception.RangoDuracionInvalidoException;
import org.mgroko.backend.actividades.exception.RequerimientoConVinculosException;
import org.mgroko.backend.actividades.exception.TransicionEstadoActividadInvalidaException;
import org.mgroko.backend.actividades.servicio.BuscarActividadService;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.actividades.servicio.EliminarActividadService;
import org.mgroko.backend.actividades.servicio.ModificarActividadService;
import org.mgroko.backend.actividades.servicio.ObtenerActividadService;
import org.mgroko.backend.common.dto.PaginaResponse;
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
    private EliminarActividadService eliminarActividadService;

    @Test
    void eliminar_conDatosValidos_devuelve204() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        mockMvc.perform(delete("/proyectos/100/actividades/55")
                        .param("motivo", "Falta de presupuesto")
                        .principal(auth))
                .andExpect(status().isNoContent());

        verify(eliminarActividadService)
                .eliminar(1L, 10L, 100L, 55L, "Falta de presupuesto");
    }

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
                "PENDIENTE",
                "NORMAL",
                null,
                List.of(),
                List.of(),
                List.of());

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
                "EN_CURSO",
                "NORMAL",
                null,
                List.of(),
                List.of(),
                List.of());

        when(modificarActividadService.modificar(
                eq(1L), eq(10L), eq(100L), eq(55L), any(ModificarActividadRequest.class)))
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
    void modificar_conCicloDePredecesoras_devuelve409() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        when(modificarActividadService.modificar(
                eq(1L), eq(10L), eq(100L), eq(55L), any(ModificarActividadRequest.class)))
                .thenThrow(new CicloActividadException(
                        "La actividad 'Montaje final' genera un ciclo de predecesoras."));

        mockMvc.perform(put("/proyectos/100/actividades/55")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Montaje final",
                                  "duracion": 2,
                                  "unidadDuracion": "HORAS",
                                  "predecesoras": [56],
                                  "requerimientos": [],
                                  "estado": "PENDIENTE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409))
                .andExpect(jsonPath("$.message").value(
                        "La actividad 'Montaje final' genera un ciclo de predecesoras."));
    }

    @Test
    void modificar_conTransicionDeEstadoInvalida_devuelve409() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        when(modificarActividadService.modificar(
                eq(1L), eq(10L), eq(100L), eq(55L), any(ModificarActividadRequest.class)))
                .thenThrow(new TransicionEstadoActividadInvalidaException(
                        EstadoActividad.PENDIENTE,
                        EstadoActividad.FINALIZADA));

        mockMvc.perform(put("/proyectos/100/actividades/55")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Montaje final",
                                  "duracion": 2,
                                  "unidadDuracion": "HORAS",
                                  "predecesoras": [],
                                  "requerimientos": [],
                                  "estado": "FINALIZADA"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409))
                .andExpect(jsonPath("$.message").value(
                        "No se puede cambiar el estado de la actividad de "
                                + "PENDIENTE a FINALIZADA."));
    }

    @Test
    void modificar_conRequerimientoVinculado_devuelve409() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        when(modificarActividadService.modificar(
                eq(1L), eq(10L), eq(100L), eq(55L), any(ModificarActividadRequest.class)))
                .thenThrow(new RequerimientoConVinculosException(7L));

        mockMvc.perform(put("/proyectos/100/actividades/55")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Montaje final",
                                  "duracion": 2,
                                  "unidadDuracion": "HORAS",
                                  "predecesoras": [],
                                  "requerimientos": [
                                    {
                                      "idRequerimiento": 7,
                                      "cantidad": 1,
                                      "idProfesion": 2,
                                      "caracteristicas": [],
                                      "habilidades": []
                                    }
                                  ],
                                  "estado": "PENDIENTE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409))
                .andExpect(jsonPath("$.message").value(
                        "No se puede modificar ni eliminar el requerimiento 7 "
                                + "porque tiene postulaciones o invitaciones asociadas."));
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
                "EN_CURSO",
                "NORMAL",
                null,
                List.of(),
                List.of(),
                List.of(new ProfesionalAsignadoResponse(
                        77L,
                        88L,
                        10L,
                        1L,
                        "Luna",
                        2L,
                        "Modelo")));

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
    void modificar_sinListasObligatorias_devuelve400() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        mockMvc.perform(put("/proyectos/100/actividades/55")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Montaje final",
                                  "duracion": 2,
                                  "unidadDuracion": "HORAS",
                                  "estado": "EN_CURSO"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(modificarActividadService, never()).modificar(
                any(), any(), any(), any(), any());
    }

    @Test
    void modificar_conElementoNuloEnLista_devuelve400() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        mockMvc.perform(put("/proyectos/100/actividades/55")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Montaje final",
                                  "duracion": 2,
                                  "unidadDuracion": "HORAS",
                                  "predecesoras": [null],
                                  "requerimientos": [],
                                  "estado": "EN_CURSO"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(modificarActividadService, never()).modificar(
                any(), any(), any(), any(), any());
    }

    @Test
    void buscar_conPerfilActivo_devuelvePagina() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 10L, "Luna"));

        ActividadBusquedaResponse actividad = new ActividadBusquedaResponse(
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
                        7L, 2, "Dos modelos", 2L, "Modelo")),
                List.of());

        PaginaResponse<ActividadBusquedaResponse> pagina = new PaginaResponse<>(
                List.of(actividad), 0, 20, 1, 1, true, true);

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
                .andExpect(jsonPath("$.totalElementos").value(1));

        verify(buscarActividadService).buscar(
                eq(10L), eq(100L),
                org.mockito.ArgumentMatchers.argThat(filtro ->
                        filtro instanceof BuscarActividadesFiltro f
                                && "mont".equals(f.nombre())
                                && Integer.valueOf(60).equals(f.duracionMin())
                                && Integer.valueOf(120).equals(f.duracionMax())
                                && List.of(EstadoActividad.PENDIENTE).equals(f.estado())
                                && "modelo".equals(f.requerimiento())),
                eq(0), eq(20), eq(false));
    }

    @Test
    void buscar_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, null, null));

        mockMvc.perform(get("/proyectos/100/actividades/buscar").principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No hay un perfil activo seleccionado en la sesi\u00f3n."));

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
                .thenThrow(new RangoDuracionInvalidoException(
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
