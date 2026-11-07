package org.mgroko.backend.invitaciones.controlador;

import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;

import static org.mockito.ArgumentMatchers.any;
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
import org.mgroko.backend.invitaciones.dto.BuscarInvitacionesFiltro;
import org.mgroko.backend.invitaciones.dto.DireccionInvitacion;
import org.mgroko.backend.invitaciones.dto.InvitacionBatchError;
import org.mgroko.backend.invitaciones.dto.InvitacionBatchResponse;
import org.mgroko.backend.invitaciones.dto.InvitacionBusquedaResponse;
import org.mgroko.backend.invitaciones.dto.InvitacionGestionadaResponse;
import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.dto.PerfilInvitacionResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.invitaciones.exception.InvitacionNoEncontradaException;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.invitaciones.exception.RangoFechasInvitacionInvalidoException;
import org.mgroko.backend.invitaciones.servicio.AceptarInvitacionService;
import org.mgroko.backend.invitaciones.servicio.BuscarInvitacionService;
import org.mgroko.backend.invitaciones.servicio.CrearInvitacionBatchService;
import org.mgroko.backend.invitaciones.servicio.CrearInvitacionService;
import org.mgroko.backend.invitaciones.servicio.EliminarInvitacionService;
import org.mgroko.backend.invitaciones.servicio.RechazarInvitacionService;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class, controllers = InvitacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvitacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrearInvitacionService crearInvitacionService;

    @MockitoBean
    private CrearInvitacionBatchService crearInvitacionBatchService;

    @MockitoBean
    private BuscarInvitacionService buscarInvitacionService;

    @MockitoBean
    private EliminarInvitacionService eliminarInvitacionService;

    @MockitoBean
    private AceptarInvitacionService aceptarInvitacionService;

    @MockitoBean
    private RechazarInvitacionService rechazarInvitacionService;

    @Test
    void aceptarGeneral_conPerfilActivo_devuelve200() throws Exception {
        when(aceptarInvitacionService.aceptarGeneral(1L, 10L, 77L))
                .thenReturn(respuestaGestionada(EstadoSolicitud.ACEPTADA));

        mockMvc.perform(post("/invitaciones/general/77/aceptar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("GENERAL"))
                .andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andExpect(jsonPath("$.idMiembro").value(88));
    }

    @Test
    void aceptarActividad_conPerfilActivo_devuelve200() throws Exception {
        when(aceptarInvitacionService.aceptarActividad(1L, 10L, 88L))
                .thenReturn(respuestaGestionada(TipoInvitacion.ACTIVIDAD,
                        EstadoSolicitud.ACEPTADA));

        mockMvc.perform(post("/invitaciones/actividad/88/aceptar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.estado").value("ACEPTADA"));
    }

    @Test
    void rechazarGeneral_conPerfilActivo_devuelve200() throws Exception {
        when(rechazarInvitacionService.rechazarGeneral(1L, 10L, 77L))
                .thenReturn(respuestaGestionada(EstadoSolicitud.RECHAZADA));

        mockMvc.perform(post("/invitaciones/general/77/rechazar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
    }

    @Test
    void rechazarActividad_conPerfilActivo_devuelve200() throws Exception {
        when(rechazarInvitacionService.rechazarActividad(1L, 10L, 88L))
                .thenReturn(respuestaGestionada(EstadoSolicitud.RECHAZADA));

        mockMvc.perform(post("/invitaciones/actividad/88/rechazar")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
    }

    @Test
    void aceptarGeneral_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(post("/invitaciones/general/77/aceptar").principal(auth))
                .andExpect(status().isNotFound());

        verify(aceptarInvitacionService, never())
                .aceptarGeneral(any(), any(), any());
    }

    @Test
    void eliminarGeneral_conPerfilActivo_devuelve204() throws Exception {
        mockMvc.perform(delete("/proyectos/5/invitaciones/general/77")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isNoContent());

        verify(eliminarInvitacionService)
                .eliminarInvitacionGeneral(1L, 10L, 5L, 77L);
    }

    @Test
    void eliminarActividad_conPerfilActivo_devuelve204() throws Exception {
        mockMvc.perform(delete("/proyectos/5/invitaciones/actividad/88")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isNoContent());

        verify(eliminarInvitacionService)
                .eliminarInvitacionActividad(1L, 10L, 5L, 88L);
    }

    @Test
    void eliminarGeneral_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(delete("/proyectos/5/invitaciones/general/77").principal(auth))
                .andExpect(status().isNotFound());

        verify(eliminarInvitacionService, never())
                .eliminarInvitacionGeneral(any(), any(), any(), any());
    }

    @Test
    void eliminarGeneral_inexistente_devuelve404() throws Exception {
        doThrow(new InvitacionNoEncontradaException())
                .when(eliminarInvitacionService)
                .eliminarInvitacionGeneral(1L, 10L, 5L, 77L);

        mockMvc.perform(delete("/proyectos/5/invitaciones/general/77")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarGeneral_noPendiente_devuelve409() throws Exception {
        doThrow(new InvitacionNoPermitidaException(
                        "Sólo se pueden eliminar invitaciones pendientes.",
                        EliminarInvitacionService.CODIGO_INVITACION_NO_PENDIENTE))
                .when(eliminarInvitacionService)
                .eliminarInvitacionGeneral(1L, 10L, 5L, 77L);

        mockMvc.perform(delete("/proyectos/5/invitaciones/general/77")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INVITACION_NO_PENDIENTE"));
    }

    @Test
    void eliminarActividad_sinPermiso_devuelve403() throws Exception {
        doThrow(new AccesoDenegadoProyectoException("Sin permiso"))
                .when(eliminarInvitacionService)
                .eliminarInvitacionActividad(1L, 10L, 5L, 88L);

        mockMvc.perform(delete("/proyectos/5/invitaciones/actividad/88")
                        .principal(autenticacion(1L, 10L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscar_conPerfilActivo_devuelvePagina() throws Exception {
        when(buscarInvitacionService.buscar(
                any(), any(BuscarInvitacionesFiltro.class), any(Integer.class),
                any(Integer.class), any(Boolean.class)))
                .thenReturn(new PaginaResponse<>(
                        List.of(respuestaBusqueda()), 0, 20, 1L, 1, true, true));

        mockMvc.perform(get("/invitaciones/buscar")
                        .principal(autenticacion(1L, 10L))
                        .param("direccion", "RECIBIDA")
                        .param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idInvitacion").value(77))
                .andExpect(jsonPath("$.contenido[0].tipo").value("GENERAL"))
                .andExpect(jsonPath("$.contenido[0].direccion").value("RECIBIDA"))
                .andExpect(jsonPath("$.contenido[0].nombreProyecto").value("Proyecto UC30"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void buscar_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(get("/invitaciones/buscar").principal(auth))
                .andExpect(status().isNotFound());

        verify(buscarInvitacionService, never())
                .buscar(any(), any(), any(Integer.class), any(Integer.class), any(Boolean.class));
    }

    @Test
    void buscar_rangoDeFechasInvalido_devuelve400() throws Exception {
        when(buscarInvitacionService.buscar(
                any(), any(BuscarInvitacionesFiltro.class), any(Integer.class),
                any(Integer.class), any(Boolean.class)))
                .thenThrow(new RangoFechasInvitacionInvalidoException());

        mockMvc.perform(get("/invitaciones/buscar")
                        .principal(autenticacion(1L, 10L))
                        .param("fechaDesde", "2026-11-30")
                        .param("fechaHasta", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitarGeneralBatch_conPerfiles_devuelveResultadoParcial() throws Exception {
        when(crearInvitacionBatchService.invitarGeneral(any(), any(), any(), any()))
                .thenReturn(new InvitacionBatchResponse(
                        List.of(respuesta(TipoInvitacion.GENERAL, null)),
                        List.of(new InvitacionBatchError(
                                21L, "El perfil ya tiene una invitación activa.",
                                CrearInvitacionService.CODIGO_INVITACION_DUPLICADA))));

        mockMvc.perform(post("/proyectos/5/invitaciones/general/batch")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsPerfilDestinatario\":[20,21],"
                                + "\"mensaje\":\"Hola\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadas[0].idInvitacion").value(77))
                .andExpect(jsonPath("$.errores[0].idPerfilDestinatario").value(21))
                .andExpect(jsonPath("$.errores[0].codigo").value("INVITACION_DUPLICADA"));
    }

    @Test
    void invitarActividadBatch_conPerfiles_devuelve200() throws Exception {
        when(crearInvitacionBatchService.invitarActividad(any(), any(), any(), any()))
                .thenReturn(new InvitacionBatchResponse(
                        List.of(respuesta(TipoInvitacion.ACTIVIDAD, 30L)),
                        List.of()));

        mockMvc.perform(post("/proyectos/5/invitaciones/actividad/batch")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoActividad\":30,"
                                + "\"idsPerfilDestinatario\":[20]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadas[0].tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.creadas[0].idRequerimientoActividad").value(30))
                .andExpect(jsonPath("$.errores").isEmpty());
    }

    @Test
    void invitarGeneralBatch_sinPerfiles_devuelve400() throws Exception {
        mockMvc.perform(post("/proyectos/5/invitaciones/general/batch")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsPerfilDestinatario\":[]}"))
                .andExpect(status().isBadRequest());

        verify(crearInvitacionBatchService, never())
                .invitarGeneral(any(), any(), any(), any());
    }

    @Test
    void invitarGeneral_conPerfilActivo_devuelve201() throws Exception {
        when(crearInvitacionService.invitarAProyecto(any(), any(), any(), any()))
                .thenReturn(respuesta(TipoInvitacion.GENERAL, null));

        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20,\"mensaje\":\"Hola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idInvitacion").value(77))
                .andExpect(jsonPath("$.tipo").value("GENERAL"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.idProyecto").value(5))
                .andExpect(jsonPath("$.idRequerimientoActividad").doesNotExist());
    }

    @Test
    void invitarActividad_conPerfilActivo_devuelve201() throws Exception {
        when(crearInvitacionService.invitarAProyectoActividad(any(), any(), any(), any()))
                .thenReturn(respuesta(TipoInvitacion.ACTIVIDAD, 30L));

        mockMvc.perform(post("/proyectos/5/invitaciones/actividad")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRequerimientoActividad\":30,"
                                + "\"idPerfilDestinatario\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.idRequerimientoActividad").value(30));
    }

    @Test
    void invitarGeneral_sinPerfilDestinatario_devuelve400() throws Exception {
        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(crearInvitacionService, never())
                .invitarAProyecto(any(), any(), any(), any());
    }

    @Test
    void invitarGeneral_conMensajeDemasiadoLargo_devuelve400() throws Exception {
        String mensaje = "x".repeat(201);

        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20,\"mensaje\":\""
                                + mensaje + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitarActividad_sinRequerimiento_devuelve400() throws Exception {
        mockMvc.perform(post("/proyectos/5/invitaciones/actividad")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitarGeneral_sinPerfilActivo_noInvocaAlServicio() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());

        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20}"))
                .andExpect(status().isNotFound());

        verify(crearInvitacionService, never())
                .invitarAProyecto(any(), any(), any(), any());
    }

    @Test
    void invitarGeneral_duplicada_devuelve409() throws Exception {
        when(crearInvitacionService.invitarAProyecto(any(), any(), any(), any()))
                .thenThrow(new InvitacionNoPermitidaException(
                        "El perfil ya tiene una invitación activa para este proyecto.",
                        CrearInvitacionService.CODIGO_INVITACION_DUPLICADA));

        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INVITACION_DUPLICADA"));
    }

    @Test
    void invitarGeneral_perfilInexistente_devuelve404() throws Exception {
        when(crearInvitacionService.invitarAProyecto(any(), any(), any(), any()))
                .thenThrow(new PerfilNoEncontradoException());

        mockMvc.perform(post("/proyectos/5/invitaciones/general")
                        .principal(autenticacion(1L, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":20}"))
                .andExpect(status().isNotFound());
    }

    private InvitacionGestionadaResponse respuestaGestionada(EstadoSolicitud estado) {
        return respuestaGestionada(TipoInvitacion.GENERAL, estado);
    }

    private InvitacionGestionadaResponse respuestaGestionada(
            TipoInvitacion tipo,
            EstadoSolicitud estado) {

        return new InvitacionGestionadaResponse(
                77L,
                tipo,
                estado,
                5L,
                10L,
                88L,
                null,
                null,
                List.of());
    }

    private InvitacionBusquedaResponse respuestaBusqueda() {
        return new InvitacionBusquedaResponse(
                77L,
                TipoInvitacion.GENERAL,
                DireccionInvitacion.RECIBIDA,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 4, 12, 0),
                null,
                5L,
                "Proyecto UC30",
                null,
                null,
                null,
                null,
                new PerfilInvitacionResponse(10L, "Remitente", "Ana", "Pérez", 1L, "Fotógrafo"),
                new PerfilInvitacionResponse(20L, "Destinatario", "Sol", "Gómez", 2L, "Modelo"),
                "Hola",
                false);
    }

    private InvitacionResponse respuesta(TipoInvitacion tipo, Long idRequerimientoActividad) {
        return new InvitacionResponse(
                77L,
                tipo,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 4, 12, 0),
                5L,
                null,
                idRequerimientoActividad,
                10L,
                20L,
                "Hola");
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idUsuario, Long idPerfil) {
        var token = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(idUsuario, idPerfil, "Perfil activo"));
        return token;
    }
}
