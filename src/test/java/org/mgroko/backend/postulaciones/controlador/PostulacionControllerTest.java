package org.mgroko.backend.postulaciones.controlador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.postulaciones.dto.ConteosPostulacionesResponse;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.postulaciones.dto.BuscarPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.MiPostulacionResponse;
import org.mgroko.backend.postulaciones.dto.MisPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.PostulacionGestionadaResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionesPaginaResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.PostulacionNoEncontradaException;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.postulaciones.exception.RangoFechasPostulacionInvalidoException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.postulaciones.servicio.AceptarPostulacionService;
import org.mgroko.backend.postulaciones.servicio.BuscarMisPostulacionesService;
import org.mgroko.backend.postulaciones.servicio.BuscarPostulacionesProyectoService;
import org.mgroko.backend.postulaciones.servicio.CrearPostulacionService;
import org.mgroko.backend.postulaciones.servicio.DarDeBajaPostulacionService;
import org.mgroko.backend.postulaciones.servicio.RechazarPostulacionService;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PostulacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostulacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CrearPostulacionService crearPostulacionService;

    @MockitoBean
    private BuscarPostulacionesProyectoService buscarPostulacionesProyectoService;

    @MockitoBean
    private AceptarPostulacionService aceptarPostulacionService;

    @MockitoBean
    private RechazarPostulacionService rechazarPostulacionService;

    @MockitoBean
    private BuscarMisPostulacionesService buscarMisPostulacionesService;

    @MockitoBean
    private DarDeBajaPostulacionService darDeBajaPostulacionService;

    @Test
    void aceptarGeneral_conPerfilActivo_devuelve200() throws Exception {
        var auth = autenticacion(1L, 10L);

        when(aceptarPostulacionService.aceptarGeneral(1L, 10L, 5L, 77L))
                .thenReturn(new PostulacionGestionadaResponse(
                        77L, TipoPostulacion.GENERAL, EstadoSolicitud.ACEPTADA,
                        5L, 30L, 12L, 88L, null));

        mockMvc.perform(post("/proyectos/5/postulaciones/general/77/aceptar")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPostulacion").value(77))
                .andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andExpect(jsonPath("$.idMiembro").value(88));
    }

    @Test
    void aceptarActividad_conPerfilActivo_devuelve200() throws Exception {
        var auth = autenticacion(1L, 10L);

        when(aceptarPostulacionService.aceptarActividad(1L, 10L, 5L, 88L))
                .thenReturn(new PostulacionGestionadaResponse(
                        88L, TipoPostulacion.ACTIVIDAD, EstadoSolicitud.ACEPTADA,
                        5L, 20L, 12L, 77L, 99L));

        mockMvc.perform(post("/proyectos/5/postulaciones/actividad/88/aceptar")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andExpect(jsonPath("$.idMiembro").value(77))
                .andExpect(jsonPath("$.idAsignacionActividad").value(99));
    }

    @Test
    void rechazarGeneral_conPerfilActivo_devuelve200() throws Exception {
        when(rechazarPostulacionService.rechazarGeneral(1L, 10L, 5L, 77L))
                .thenReturn(new PostulacionGestionadaResponse(
                        77L, TipoPostulacion.GENERAL, EstadoSolicitud.RECHAZADA,
                        5L, 30L, 12L, null, null));

        mockMvc.perform(post("/proyectos/5/postulaciones/general/77/rechazar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.idMiembro").doesNotExist());
    }

    @Test
    void rechazarActividad_conPerfilActivo_devuelve200() throws Exception {
        when(rechazarPostulacionService.rechazarActividad(1L, 10L, 5L, 88L))
                .thenReturn(new PostulacionGestionadaResponse(
                        88L, TipoPostulacion.ACTIVIDAD, EstadoSolicitud.RECHAZADA,
                        5L, 20L, 12L, null, null));

        mockMvc.perform(post("/proyectos/5/postulaciones/actividad/88/rechazar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.idAsignacionActividad").doesNotExist());
    }

    @Test
    void rechazarGeneral_yaGestionada_devuelve409() throws Exception {
        when(rechazarPostulacionService.rechazarGeneral(1L, 10L, 5L, 77L))
                .thenThrow(new PostulacionYaGestionadaException());

        mockMvc.perform(post("/proyectos/5/postulaciones/general/77/rechazar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isConflict());
    }

    @Test
    void aceptarGeneral_sinPermisoDeDirector_devuelve403() throws Exception {
        when(aceptarPostulacionService.aceptarGeneral(1L, 10L, 5L, 77L))
                .thenThrow(new AccesoDenegadoProyectoException("Sin permiso"));

        mockMvc.perform(post("/proyectos/5/postulaciones/general/77/aceptar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aceptarGeneral_postulacionInexistente_devuelve404() throws Exception {
        when(aceptarPostulacionService.aceptarGeneral(1L, 10L, 5L, 77L))
                .thenThrow(new PostulacionNoEncontradaException());

        mockMvc.perform(post("/proyectos/5/postulaciones/general/77/aceptar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void aceptarGeneral_yaGestionada_devuelve409() throws Exception {
        when(aceptarPostulacionService.aceptarGeneral(1L, 10L, 5L, 77L))
                .thenThrow(new PostulacionYaGestionadaException());

        mockMvc.perform(post("/proyectos/5/postulaciones/general/77/aceptar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isConflict());
    }

    @Test
    void aceptarGeneral_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(post("/proyectos/5/postulaciones/general/77/aceptar")
                        .principal(auth))
                .andExpect(status().isNotFound());

        verify(aceptarPostulacionService, never())
                .aceptarGeneral(any(), any(), any(), any());
    }

    @Test
    void buscar_conPerfilActivo_devuelvePaginaYFiltros() throws Exception {
        var auth = autenticacion(1L, 10L);

        when(buscarPostulacionesProyectoService.buscar(
                eq(10L), eq(5L), any(BuscarPostulacionesFiltro.class), eq(0), eq(20), eq(false)))
                .thenReturn(new PostulacionesPaginaResponse(
                        List.of(), 0, 20, 0L, 0, true, true,
                        new ConteosPostulacionesResponse(2, 3, 1, 6)));

        mockMvc.perform(get("/proyectos/5/postulaciones/buscar")
                        .principal(auth)
                        .param("estado", "PENDIENTE")
                        .param("tipo", "GENERAL")
                        .param("nombrePerfil", "Luna")
                        .param("idProfesion", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.paginaActual").value(0))
                .andExpect(jsonPath("$.conteosPorEstado.PENDIENTE").value(2))
                .andExpect(jsonPath("$.conteosPorEstado.ACEPTADA").value(3))
                .andExpect(jsonPath("$.conteosPorEstado.RECHAZADA").value(1))
                .andExpect(jsonPath("$.conteosPorEstado.TOTAL").value(6));

        verify(buscarPostulacionesProyectoService).buscar(
                eq(10L), eq(5L), any(BuscarPostulacionesFiltro.class), eq(0), eq(20), eq(false));
    }

    @Test
    void buscar_rangoDeFechasInvalido_devuelve400() throws Exception {
        when(buscarPostulacionesProyectoService.buscar(
                eq(10L), eq(5L), any(BuscarPostulacionesFiltro.class), eq(0), eq(20), eq(false)))
                .thenThrow(new RangoFechasPostulacionInvalidoException());

        mockMvc.perform(get("/proyectos/5/postulaciones/buscar")
                        .principal(autenticacion(1L, 10L))
                        .param("fechaDesde", "2026-10-30")
                        .param("fechaHasta", "2026-10-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postularAActividad_conPerfilActivo_devuelve201() throws Exception {
        var auth = autenticacion(1L, 10L);

        when(crearPostulacionService.darDeAltaPostulacionActividad(1L, 10L, 5L, 20L))
                .thenReturn(respuesta(TipoPostulacion.ACTIVIDAD, false));

        mockMvc.perform(post("/proyectos/5/postulaciones/actividad")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoActividad\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPostulacion").value(77))
                .andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.idProyecto").value(5))
                .andExpect(jsonPath("$.idRequerimiento").value(20))
                .andExpect(jsonPath("$.tentativa").value(false));
    }

    @Test
    void postularAGeneral_conPerfilActivo_devuelve201() throws Exception {
        var auth = autenticacion(1L, 10L);

        when(crearPostulacionService.darDeAltaPostulacionGeneral(1L, 10L, 5L, 30L))
                .thenReturn(respuesta(TipoPostulacion.GENERAL, false));

        mockMvc.perform(post("/proyectos/5/postulaciones/general")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoGral\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("GENERAL"))
                .andExpect(jsonPath("$.idRequerimiento").value(30));
    }

    @Test
    void postularAActividad_sinRequerimiento_devuelve400() throws Exception {
        mockMvc.perform(post("/proyectos/5/postulaciones/actividad")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(crearPostulacionService, never())
                .darDeAltaPostulacionActividad(any(), any(), any(), any());
    }

    @Test
    void postularAActividad_reglaDeNegocioIncumplida_devuelve409() throws Exception {
        when(crearPostulacionService.darDeAltaPostulacionActividad(1L, 10L, 5L, 20L))
                .thenThrow(new PostulacionNoPermitidaException("No tenés disponibilidad."));

        mockMvc.perform(post("/proyectos/5/postulaciones/actividad")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoActividad\":20}"))
                .andExpect(status().isConflict());
    }

    @Test
    void postularAGeneral_requerimientoInexistente_devuelve404() throws Exception {
        when(crearPostulacionService.darDeAltaPostulacionGeneral(1L, 10L, 5L, 30L))
                .thenThrow(new RequerimientoNoEncontradoException("No existe."));

        mockMvc.perform(post("/proyectos/5/postulaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoGral\":30}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postularAActividad_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(post("/proyectos/5/postulaciones/actividad")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoActividad\":20}"))
                .andExpect(status().isNotFound());

        verify(crearPostulacionService, never()).darDeAltaPostulacionActividad(any(), any(), any(), any());
    }

    @Test
    void postularAActividad_mapeaIdPerfilEIdProyectoDelPath() throws Exception {
        when(crearPostulacionService.darDeAltaPostulacionActividad(eq(1L), eq(10L), eq(9L), eq(20L)))
                .thenReturn(respuesta(TipoPostulacion.ACTIVIDAD, true));

        mockMvc.perform(post("/proyectos/9/postulaciones/actividad")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("idRequerimientoActividad", 20))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tentativa").value(true));

        verify(crearPostulacionService).darDeAltaPostulacionActividad(1L, 10L, 9L, 20L);
    }

    @Test
    void darDeBajaGeneral_conPerfilActivo_devuelve204() throws Exception {
        mockMvc.perform(delete("/proyectos/5/postulaciones/general/77")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isNoContent());

        verify(darDeBajaPostulacionService)
                .darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L);
    }

    @Test
    void darDeBajaActividad_conPerfilActivo_devuelve204() throws Exception {
        mockMvc.perform(delete("/proyectos/5/postulaciones/actividad/88")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isNoContent());

        verify(darDeBajaPostulacionService)
                .darDeBajaPostulacionActividad(1L, 10L, 5L, 88L);
    }

    @Test
    void darDeBajaGeneral_postulacionInexistenteODeOtroPerfil_devuelve404() throws Exception {
        doThrow(new PostulacionNoEncontradaException())
                .when(darDeBajaPostulacionService)
                .darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L);

        mockMvc.perform(delete("/proyectos/5/postulaciones/general/77")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void darDeBajaGeneral_yaGestionada_devuelve409() throws Exception {
        doThrow(new PostulacionYaGestionadaException())
                .when(darDeBajaPostulacionService)
                .darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L);

        mockMvc.perform(delete("/proyectos/5/postulaciones/general/77")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isConflict());
    }

    @Test
    void darDeBajaGeneral_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(delete("/proyectos/5/postulaciones/general/77")
                        .principal(auth))
                .andExpect(status().isNotFound());

        verify(darDeBajaPostulacionService, never())
                .darDeBajaPostulacionGeneral(any(), any(), any(), any());
    }

    @Test
    void misPostulaciones_conPerfilActivo_devuelvePaginaYFiltros() throws Exception {
        var respuesta = new MiPostulacionResponse(
                77L, TipoPostulacion.GENERAL, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 10, 26, 10, 0),
                5L, "Desfile", 30L, 2, "Modelo", "Una modelo",
                null, null, true);

        when(buscarMisPostulacionesService.buscar(
                eq(10L), any(MisPostulacionesFiltro.class), eq(0), eq(20), eq(false)))
                .thenReturn(new PaginaResponse<>(List.of(respuesta), 0, 20, 1L, 1, true, true));

        mockMvc.perform(get("/postulaciones/mias")
                        .principal(autenticacion(1L, 10L))
                        .param("idProyecto", "5")
                        .param("estado", "PENDIENTE")
                        .param("tipo", "GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].idPostulacion").value(77))
                .andExpect(jsonPath("$.contenido[0].tipo").value("GENERAL"))
                .andExpect(jsonPath("$.contenido[0].cantidad").value(2))
                .andExpect(jsonPath("$.contenido[0].nombreProfesion").value("Modelo"))
                .andExpect(jsonPath("$.contenido[0].puedeDarseDeBaja").value(true));

        verify(buscarMisPostulacionesService).buscar(
                eq(10L), any(MisPostulacionesFiltro.class), eq(0), eq(20), eq(false));
    }

    @Test
    void misPostulaciones_sinPerfilActivo_devuelve404() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(get("/postulaciones/mias").principal(auth))
                .andExpect(status().isNotFound());

        verify(buscarMisPostulacionesService, never())
                .buscar(any(), any(), anyInt(), anyInt(), anyBoolean());
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario, Long idPerfil) {
        var auth = new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null, List.of());
        auth.setDetails(new ContextoAutenticacion(idUsuario, idPerfil, "Luna Diseños"));
        return auth;
    }

    private PostulacionResponse respuesta(TipoPostulacion tipo, boolean tentativa) {
        Long idRequerimiento = tipo == TipoPostulacion.ACTIVIDAD ? 20L : 30L;
        return new PostulacionResponse(
                77L,
                tipo,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 10, 25, 15, 0),
                5L,
                idRequerimiento,
                tentativa);
    }
}
