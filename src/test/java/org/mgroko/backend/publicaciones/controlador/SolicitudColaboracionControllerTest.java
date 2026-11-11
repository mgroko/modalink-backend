package org.mgroko.backend.publicaciones.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.BuscarSolicitudesColaboracionFiltro;
import org.mgroko.backend.publicaciones.dto.DireccionSolicitudColaboracion;
import org.mgroko.backend.publicaciones.dto.EnviarSolicitudColaboracionRequest;
import org.mgroko.backend.publicaciones.dto.PublicacionSolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionBusquedaResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionGestionadaResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.exception.FiltroSolicitudColaboracionInvalidoException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionDuplicadaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionInvalidaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPermitidaException;
import org.mgroko.backend.publicaciones.servicio.BuscarSolicitudColaboracionService;
import org.mgroko.backend.publicaciones.servicio.EnviarSolicitudColaboracionService;
import org.mgroko.backend.publicaciones.servicio.GestionarSolicitudColaboracionService;
import org.mgroko.backend.publicaciones.servicio.ListarPublicacionesSolicitudColaboracionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.mockito.ArgumentCaptor;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = SolicitudColaboracionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SolicitudColaboracionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnviarSolicitudColaboracionService enviarSolicitudColaboracionService;

    @MockitoBean
    private BuscarSolicitudColaboracionService buscarSolicitudColaboracionService;

    @MockitoBean
    private GestionarSolicitudColaboracionService gestionarSolicitudColaboracionService;

    @MockitoBean
    private ListarPublicacionesSolicitudColaboracionService
            listarPublicacionesSolicitudColaboracionService;

    private static UsernamePasswordAuthenticationToken auth(Long idUsuario, Long idPerfilActivo) {
        var token = new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(idUsuario, idPerfilActivo, "Luna Diseños"));
        return token;
    }

    private static SolicitudColaboracionResponse response() {
        return new SolicitudColaboracionResponse(
                50L,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 10, 20, 30),
                99L,
                "Editorial Otoño",
                new AutorPublicacionResponse(7L, "Luna Diseños", "Diseñadora", "/uploads/luna.webp"),
                new AutorPublicacionResponse(8L, "Sol", "Fotógrafa", "/uploads/sol.webp"));
    }

    @Test
    void enviar_devuelve201ConLaSolicitudCreada() throws Exception {
        when(enviarSolicitudColaboracionService.enviar(eq(1L), eq(7L), eq(99L), any()))
                .thenReturn(response());

        mockMvc.perform(post("/publicaciones/{idPublicacion}/solicitudes-colaboracion", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":8}")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idSolicitudColaboracion").value(50L))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.idPublicacion").value(99L))
                .andExpect(jsonPath("$.remitente.idPerfil").value(7L))
                .andExpect(jsonPath("$.destinatario.idPerfil").value(8L));

        ArgumentCaptor<EnviarSolicitudColaboracionRequest> captor =
                ArgumentCaptor.forClass(EnviarSolicitudColaboracionRequest.class);
        verify(enviarSolicitudColaboracionService).enviar(
                eq(1L), eq(7L), eq(99L), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                8L, captor.getValue().idPerfilDestinatario());
    }

    @Test
    void enviar_sinDestinatario_devuelve400() throws Exception {
        mockMvc.perform(post("/publicaciones/{idPublicacion}/solicitudes-colaboracion", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enviar_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(post("/publicaciones/{idPublicacion}/solicitudes-colaboracion", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":8}")
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void enviar_destinatarioFueraDeRed_devuelve409ConCodigo() throws Exception {
        when(enviarSolicitudColaboracionService.enviar(eq(1L), eq(7L), eq(99L), any()))
                .thenThrow(new SolicitudColaboracionInvalidaException(
                        "El perfil destinatario no pertenece a tu red de contactos.",
                        "DESTINATARIO_FUERA_DE_RED"));

        mockMvc.perform(post("/publicaciones/{idPublicacion}/solicitudes-colaboracion", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":8}")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DESTINATARIO_FUERA_DE_RED"));
    }

    @Test
    void enviar_solicitudDuplicada_devuelve409ConCodigo() throws Exception {
        when(enviarSolicitudColaboracionService.enviar(eq(1L), eq(7L), eq(99L), any()))
                .thenThrow(new SolicitudColaboracionDuplicadaException(
                        "Ya existe una solicitud de colaboración pendiente para ese perfil."));

        mockMvc.perform(post("/publicaciones/{idPublicacion}/solicitudes-colaboracion", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPerfilDestinatario\":8}")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_COLABORACION_PENDIENTE"));
    }

    private static SolicitudColaboracionBusquedaResponse busquedaResponse() {
        return new SolicitudColaboracionBusquedaResponse(
                50L,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 10, 20, 30),
                null,
                DireccionSolicitudColaboracion.RECIBIDA,
                7L,
                8L,
                new AutorPublicacionResponse(7L, "Luna Diseños", "Diseñadora", "/uploads/luna.webp"),
                new AutorPublicacionResponse(8L, "Sol", "Fotógrafa", "/uploads/sol.webp"),
                new PublicacionSolicitudColaboracionResponse(
                        99L, "Editorial Otoño", "/uploads/publicaciones/a.webp", "ACTIVA"),
                true,
                true,
                false);
    }

    @Test
    void buscar_devuelvePaginaDeSolicitudes() throws Exception {
        PaginaResponse<SolicitudColaboracionBusquedaResponse> pagina =
                new PaginaResponse<>(List.of(busquedaResponse()), 0, 20, 1, 1, true, true);
        when(buscarSolicitudColaboracionService.buscar(
                eq(7L), any(BuscarSolicitudesColaboracionFiltro.class), eq(0), eq(20)))
                .thenReturn(pagina);

        mockMvc.perform(get("/solicitudes-colaboracion")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].idSolicitudColaboracion").value(50L))
                .andExpect(jsonPath("$.contenido[0].direccion").value("RECIBIDA"))
                .andExpect(jsonPath("$.contenido[0].publicacion.idPublicacion").value(99L))
                .andExpect(jsonPath("$.contenido[0].puedeAceptar").value(true));
    }

    @Test
    void buscar_mapeaFiltrosDeQueryParams() throws Exception {
        PaginaResponse<SolicitudColaboracionBusquedaResponse> pagina =
                new PaginaResponse<>(List.of(), 0, 10, 0, 0, true, true);
        when(buscarSolicitudColaboracionService.buscar(
                eq(7L), any(BuscarSolicitudesColaboracionFiltro.class), eq(1), eq(10)))
                .thenReturn(pagina);

        mockMvc.perform(get("/solicitudes-colaboracion")
                        .param("direccion", "ENVIADA")
                        .param("idPerfilEmisor", "7")
                        .param("idPerfilReceptor", "8")
                        .param("estado", "ACEPTADA")
                        .param("fechaDesde", "2026-01-01")
                        .param("fechaHasta", "2026-12-31")
                        .param("idPublicacion", "99")
                        .param("page", "1")
                        .param("size", "10")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk());

        ArgumentCaptor<BuscarSolicitudesColaboracionFiltro> captor =
                ArgumentCaptor.forClass(BuscarSolicitudesColaboracionFiltro.class);
        verify(buscarSolicitudColaboracionService).buscar(
                eq(7L), captor.capture(), eq(1), eq(10));

        BuscarSolicitudesColaboracionFiltro filtro = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(
                DireccionSolicitudColaboracion.ENVIADA, filtro.direccion());
        org.junit.jupiter.api.Assertions.assertEquals(7L, filtro.idPerfilEmisor());
        org.junit.jupiter.api.Assertions.assertEquals(8L, filtro.idPerfilReceptor());
        org.junit.jupiter.api.Assertions.assertEquals(EstadoSolicitud.ACEPTADA, filtro.estado());
        org.junit.jupiter.api.Assertions.assertEquals(
                LocalDate.of(2026, 1, 1), filtro.fechaDesde());
        org.junit.jupiter.api.Assertions.assertEquals(
                LocalDate.of(2026, 12, 31), filtro.fechaHasta());
        org.junit.jupiter.api.Assertions.assertEquals(99L, filtro.idPublicacion());
    }

    @Test
    void buscar_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(get("/solicitudes-colaboracion")
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPublicaciones_devuelveOpcionesDelFiltro() throws Exception {
        when(listarPublicacionesSolicitudColaboracionService.listar(
                eq(7L), eq(DireccionSolicitudColaboracion.RECIBIDA)))
                .thenReturn(List.of(new PublicacionSolicitudColaboracionResponse(
                        99L, "Editorial Otoño", "/uploads/publicaciones/a.webp", "ACTIVA")));

        mockMvc.perform(get("/solicitudes-colaboracion/publicaciones")
                        .param("direccion", "RECIBIDA")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idPublicacion").value(99L))
                .andExpect(jsonPath("$[0].titulo").value("Editorial Otoño"));
    }

    @Test
    void listarPublicaciones_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(get("/solicitudes-colaboracion/publicaciones")
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscar_filtroInvalido_devuelve400() throws Exception {
        when(buscarSolicitudColaboracionService.buscar(
                eq(7L), any(BuscarSolicitudesColaboracionFiltro.class), eq(0), eq(20)))
                .thenThrow(new FiltroSolicitudColaboracionInvalidoException(
                        "No se puede combinar todosLosEstados con un estado específico."));

        mockMvc.perform(get("/solicitudes-colaboracion")
                        .param("todosLosEstados", "true")
                        .param("estado", "PENDIENTE")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isBadRequest());
    }

    private static SolicitudColaboracionGestionadaResponse gestionada(
            EstadoSolicitud estado, boolean colaboradorAgregado) {
        return new SolicitudColaboracionGestionadaResponse(
                50L,
                estado,
                LocalDateTime.of(2026, 11, 10, 21, 0),
                99L,
                "Editorial Otoño",
                colaboradorAgregado);
    }

    @Test
    void aceptar_devuelve200ConLaSolicitudAceptada() throws Exception {
        when(gestionarSolicitudColaboracionService.aceptar(1L, 7L, 50L))
                .thenReturn(gestionada(EstadoSolicitud.ACEPTADA, true));

        mockMvc.perform(post("/solicitudes-colaboracion/{id}/aceptar", 50L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSolicitudColaboracion").value(50L))
                .andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andExpect(jsonPath("$.colaboradorAgregado").value(true));
    }

    @Test
    void rechazar_devuelve200ConLaSolicitudRechazada() throws Exception {
        when(gestionarSolicitudColaboracionService.rechazar(1L, 7L, 50L))
                .thenReturn(gestionada(EstadoSolicitud.RECHAZADA, false));

        mockMvc.perform(post("/solicitudes-colaboracion/{id}/rechazar", 50L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSolicitudColaboracion").value(50L))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.colaboradorAgregado").value(false));
    }

    @Test
    void aceptar_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(post("/solicitudes-colaboracion/{id}/aceptar", 50L)
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void aceptar_solicitudAjena_devuelve403() throws Exception {
        when(gestionarSolicitudColaboracionService.aceptar(1L, 7L, 50L))
                .thenThrow(new SolicitudColaboracionNoPermitidaException(
                        "Solo el perfil destinatario puede gestionar la solicitud."));

        mockMvc.perform(post("/solicitudes-colaboracion/{id}/aceptar", 50L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_COLABORACION_NO_PERMITIDA"));
    }
}
