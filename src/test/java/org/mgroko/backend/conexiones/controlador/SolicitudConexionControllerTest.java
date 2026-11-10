package org.mgroko.backend.conexiones.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.mgroko.backend.conexiones.dto.BuscarSolicitudesConexionFiltro;
import org.mgroko.backend.conexiones.dto.DireccionSolicitudConexion;
import org.mgroko.backend.conexiones.dto.EnviarSolicitudConexionRequest;
import org.mgroko.backend.conexiones.dto.PerfilConexionResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionBusquedaResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionGestionadaResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionResponse;
import org.mgroko.backend.conexiones.exception.ConexionYaExistenteException;
import org.mgroko.backend.conexiones.exception.RangoFechasSolicitudConexionInvalidoException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionDuplicadaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoEncontradaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPendienteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPermitidaException;
import org.mgroko.backend.conexiones.servicio.BuscarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.CancelarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.EnviarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.GestionarSolicitudConexionService;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
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

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = SolicitudConexionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SolicitudConexionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnviarSolicitudConexionService enviarSolicitudConexionService;

    @MockitoBean
    private CancelarSolicitudConexionService cancelarSolicitudConexionService;

    @MockitoBean
    private BuscarSolicitudConexionService buscarSolicitudConexionService;

    @MockitoBean
    private GestionarSolicitudConexionService gestionarSolicitudConexionService;

    @Test
    void enviar_solicitudValida_devuelve201() throws Exception {
        when(enviarSolicitudConexionService.enviar(
                eq(1L), eq(10L), any(EnviarSolicitudConexionRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/solicitudes-conexion")
                        .principal(autenticacion(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EnviarSolicitudConexionRequest(20L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idSolicitudConexion").value(100))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.remitente.idPerfil").value(10))
                .andExpect(jsonPath("$.destinatario.idPerfil").value(20));
    }

    @Test
    void enviar_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(post("/solicitudes-conexion")
                        .principal(autenticacion(null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EnviarSolicitudConexionRequest(20L))))
                .andExpect(status().isNotFound());

        verify(enviarSolicitudConexionService, never())
                .enviar(any(), any(), any());
    }

    @Test
    void enviar_bodyInvalido_devuelve400() throws Exception {
        mockMvc.perform(post("/solicitudes-conexion")
                        .principal(autenticacion(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enviar_solicitudDuplicada_devuelve409ConCodigo() throws Exception {
        when(enviarSolicitudConexionService.enviar(
                eq(1L), eq(10L), any(EnviarSolicitudConexionRequest.class)))
                .thenThrow(new SolicitudConexionDuplicadaException(
                        "Ya existe una solicitud de conexión pendiente con ese perfil."));

        mockMvc.perform(post("/solicitudes-conexion")
                        .principal(autenticacion(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EnviarSolicitudConexionRequest(20L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_CONEXION_PENDIENTE"));
    }

    @Test
    void enviar_perfilInexistente_devuelve404() throws Exception {
        when(enviarSolicitudConexionService.enviar(
                eq(1L), eq(10L), any(EnviarSolicitudConexionRequest.class)))
                .thenThrow(new PerfilNoEncontradoException());

        mockMvc.perform(post("/solicitudes-conexion")
                        .principal(autenticacion(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EnviarSolicitudConexionRequest(20L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelar_solicitudPropiaPendiente_devuelve204() throws Exception {
        doNothing().when(cancelarSolicitudConexionService)
                .cancelar(1L, 10L, 100L);

        mockMvc.perform(delete("/solicitudes-conexion/100")
                        .principal(autenticacion(10L)))
                .andExpect(status().isNoContent());

        verify(cancelarSolicitudConexionService).cancelar(1L, 10L, 100L);
    }

    @Test
    void cancelar_solicitudNoPendiente_devuelve409() throws Exception {
        org.mockito.Mockito.doThrow(new SolicitudConexionNoPendienteException(
                        "Sólo se pueden cancelar solicitudes de conexión pendientes."))
                .when(cancelarSolicitudConexionService)
                .cancelar(1L, 10L, 100L);

        mockMvc.perform(delete("/solicitudes-conexion/100")
                        .principal(autenticacion(10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_CONEXION_NO_PENDIENTE"));
    }

    @Test
    void cancelar_solicitudAjena_devuelve403() throws Exception {
        org.mockito.Mockito.doThrow(new SolicitudConexionNoPermitidaException(
                        "La solicitud no pertenece al perfil activo."))
                .when(cancelarSolicitudConexionService)
                .cancelar(1L, 10L, 100L);

        mockMvc.perform(delete("/solicitudes-conexion/100")
                        .principal(autenticacion(10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_CONEXION_NO_PERMITIDA"));
    }

    @Test
    void cancelar_solicitudInexistente_devuelve404() throws Exception {
        org.mockito.Mockito.doThrow(new SolicitudConexionNoEncontradaException())
                .when(cancelarSolicitudConexionService)
                .cancelar(1L, 10L, 100L);

        mockMvc.perform(delete("/solicitudes-conexion/100")
                        .principal(autenticacion(10L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscar_sinFiltros_devuelve200ConPagina() throws Exception {
        when(buscarSolicitudConexionService.buscar(
                eq(10L), any(BuscarSolicitudesConexionFiltro.class), eq(0), eq(20)))
                .thenReturn(pagina());

        mockMvc.perform(get("/solicitudes-conexion")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idSolicitudConexion").value(100))
                .andExpect(jsonPath("$.contenido[0].direccion").value("RECIBIDA"))
                .andExpect(jsonPath("$.contenido[0].puedeAceptar").value(true))
                .andExpect(jsonPath("$.contenido[0].puedeCancelar").value(false))
                .andExpect(jsonPath("$.contenido[0].enLinea").value(false))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void buscar_rangoFechasInvalido_devuelve400() throws Exception {
        when(buscarSolicitudConexionService.buscar(
                eq(10L), any(BuscarSolicitudesConexionFiltro.class), eq(0), eq(20)))
                .thenThrow(new RangoFechasSolicitudConexionInvalidoException());

        mockMvc.perform(get("/solicitudes-conexion")
                        .param("fechaDesde", "2026-11-10")
                        .param("fechaHasta", "2026-11-09")
                        .principal(autenticacion(10L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("RANGO_FECHAS_INVALIDO"));
    }

    @Test
    void buscar_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(get("/solicitudes-conexion")
                        .principal(autenticacion(null)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(buscarSolicitudConexionService);
    }

    @Test
    void aceptar_solicitudValida_devuelve200ConConexion() throws Exception {
        when(gestionarSolicitudConexionService.aceptar(1L, 10L, 100L))
                .thenReturn(new SolicitudConexionGestionadaResponse(
                        100L,
                        EstadoSolicitud.ACEPTADA,
                        LocalDateTime.of(2026, 11, 9, 10, 30),
                        55L));

        mockMvc.perform(post("/solicitudes-conexion/100/aceptar")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSolicitudConexion").value(100))
                .andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andExpect(jsonPath("$.idConexion").value(55));
    }

    @Test
    void rechazar_solicitudValida_devuelve200SinConexion() throws Exception {
        when(gestionarSolicitudConexionService.rechazar(1L, 10L, 100L))
                .thenReturn(new SolicitudConexionGestionadaResponse(
                        100L,
                        EstadoSolicitud.RECHAZADA,
                        LocalDateTime.of(2026, 11, 9, 10, 30),
                        null));

        mockMvc.perform(post("/solicitudes-conexion/100/rechazar")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSolicitudConexion").value(100))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.idConexion")
                        .value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void aceptar_solicitudAjena_devuelve403() throws Exception {
        when(gestionarSolicitudConexionService.aceptar(1L, 10L, 100L))
                .thenThrow(new SolicitudConexionNoPermitidaException(
                        "La solicitud no pertenece al perfil activo."));

        mockMvc.perform(post("/solicitudes-conexion/100/aceptar")
                        .principal(autenticacion(10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_CONEXION_NO_PERMITIDA"));
    }

    @Test
    void aceptar_solicitudYaGestionada_devuelve409() throws Exception {
        when(gestionarSolicitudConexionService.aceptar(1L, 10L, 100L))
                .thenThrow(new SolicitudConexionNoPendienteException(
                        "Sólo se pueden gestionar solicitudes de conexión pendientes."));

        mockMvc.perform(post("/solicitudes-conexion/100/aceptar")
                        .principal(autenticacion(10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_CONEXION_NO_PENDIENTE"));
    }

    @Test
    void aceptar_conexionExistente_devuelve409() throws Exception {
        when(gestionarSolicitudConexionService.aceptar(1L, 10L, 100L))
                .thenThrow(new ConexionYaExistenteException(
                        "Ya existe una conexión vigente con ese perfil."));

        mockMvc.perform(post("/solicitudes-conexion/100/aceptar")
                        .principal(autenticacion(10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONEXION_YA_EXISTENTE"));
    }

    @Test
    void aceptar_perfilInactivo_devuelve409() throws Exception {
        when(gestionarSolicitudConexionService.aceptar(1L, 10L, 100L))
                .thenThrow(new PerfilEnBajaException(
                        "El perfil remitente no se encuentra activo."));

        mockMvc.perform(post("/solicitudes-conexion/100/aceptar")
                        .principal(autenticacion(10L)))
                .andExpect(status().isConflict());
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idPerfilActivo) {
        var authentication = new UsernamePasswordAuthenticationToken("1", null, java.util.List.of());
        authentication.setDetails(
                new ContextoAutenticacion(1L, idPerfilActivo, "Ana Moda"));
        return authentication;
    }

    private SolicitudConexionResponse response() {
        return new SolicitudConexionResponse(
                100L,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 8, 12, 0),
                null,
                new PerfilConexionResponse(10L, "Ana Moda", "Ana", "Perez", 1L, "Modelo", null, null),
                new PerfilConexionResponse(20L, "Bruno Estilo", "Bruno", "Gomez", 2L, "Fotógrafo", null, null));
    }

    private PaginaResponse<SolicitudConexionBusquedaResponse> pagina() {
        var item = new SolicitudConexionBusquedaResponse(
                100L,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 8, 12, 0),
                null,
                DireccionSolicitudConexion.RECIBIDA,
                10L,
                20L,
                new PerfilConexionResponse(10L, "Ana Moda", "Ana", "Perez", 1L, "Modelo", null, null),
                true,
                true,
                false);

        return new PaginaResponse<>(List.of(item), 0, 20, 1, 1, true, true);
    }
}
