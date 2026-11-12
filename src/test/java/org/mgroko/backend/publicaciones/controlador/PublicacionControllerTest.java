package org.mgroko.backend.publicaciones.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.BuscarPublicacionesFiltro;
import org.mgroko.backend.publicaciones.dto.ImagenPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ProyectoPublicacionFiltroResponse;
import org.mgroko.backend.publicaciones.dto.PublicacionOcultadaResponse;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.servicio.CrearPublicacionService;
import org.mgroko.backend.publicaciones.servicio.EliminarPublicacionService;
import org.mgroko.backend.publicaciones.servicio.ListarFeedPublicacionesService;
import org.mgroko.backend.publicaciones.servicio.ListarProyectosPublicacionesPerfilService;
import org.mgroko.backend.publicaciones.servicio.ListarPublicacionesOcultasService;
import org.mgroko.backend.publicaciones.servicio.ListarPublicacionesPerfilService;
import org.mgroko.backend.publicaciones.servicio.ModificarPublicacionService;
import org.mgroko.backend.publicaciones.servicio.ObtenerPublicacionDetalleService;
import org.mgroko.backend.publicaciones.servicio.ObtenerPublicacionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = PublicacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrearPublicacionService crearPublicacionService;

    @MockitoBean
    private ListarPublicacionesPerfilService listarPublicacionesPerfilService;

    @MockitoBean
    private ListarProyectosPublicacionesPerfilService listarProyectosPublicacionesPerfilService;

    @MockitoBean
    private ListarFeedPublicacionesService listarFeedPublicacionesService;

    @MockitoBean
    private ListarPublicacionesOcultasService listarPublicacionesOcultasService;

    @MockitoBean
    private ObtenerPublicacionService obtenerPublicacionService;

    @MockitoBean
    private ObtenerPublicacionDetalleService obtenerPublicacionDetalleService;

    @MockitoBean
    private ModificarPublicacionService modificarPublicacionService;

    @MockitoBean
    private EliminarPublicacionService eliminarPublicacionService;

    private static UsernamePasswordAuthenticationToken auth(Long idUsuario, Long idPerfilActivo) {
        var token = new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(idUsuario, idPerfilActivo, "Luna Diseños"));
        return token;
    }

    private static MockMultipartFile archivo() {
        return new MockMultipartFile(
                "archivos", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, "bytes".getBytes());
    }

    private static PublicacionResponse response() {
        return new PublicacionResponse(
                99L,
                "Editorial",
                "Descripción",
                LocalDateTime.of(2026, 11, 10, 12, 0),
                "ACTIVA",
                new AutorPublicacionResponse(7L, "Luna Diseños", "Diseñadora", "/uploads/perfiles/a.webp"),
                null,
                null,
                List.of(new ImagenPublicacionResponse(10L, "/uploads/publicaciones/a.webp", 0)),
                List.of(),
                0,
                0,
                false);
    }

    @Test
    void crear_multipartValido_devuelve201() throws Exception {
        when(crearPublicacionService.crear(eq(1L), eq(7L), any()))
                .thenReturn(response());

        mockMvc.perform(multipart("/publicaciones")
                        .file(archivo())
                        .param("titulo", "Editorial")
                        .param("descripcion", "Descripción")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPublicacion").value(99L))
                .andExpect(jsonPath("$.titulo").value("Editorial"))
                .andExpect(jsonPath("$.imagenes.length()").value(1));
    }

    @Test
    void crear_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(multipart("/publicaciones")
                        .file(archivo())
                        .param("titulo", "Editorial")
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_sinTitulo_devuelve400() throws Exception {
        mockMvc.perform(multipart("/publicaciones")
                        .file(archivo())
                        .principal(auth(1L, 7L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_sinArchivos_devuelve400() throws Exception {
        mockMvc.perform(multipart("/publicaciones")
                        .param("titulo", "Editorial")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtener_devuelveLaPublicacionPropia() throws Exception {
        when(obtenerPublicacionService.obtener(1L, 7L, 99L)).thenReturn(response());

        mockMvc.perform(get("/publicaciones/{idPublicacion}", 99L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPublicacion").value(99L))
                .andExpect(jsonPath("$.titulo").value("Editorial"));
    }

    @Test
    void obtener_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(get("/publicaciones/{idPublicacion}", 99L)
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtener_publicacionAjena_devuelve404() throws Exception {
        when(obtenerPublicacionService.obtener(1L, 7L, 99L))
                .thenThrow(new org.mgroko.backend.publicaciones.exception
                        .PublicacionNoEncontradaException(99L));

        mockMvc.perform(get("/publicaciones/{idPublicacion}", 99L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerDetalle_devuelveLaPublicacionVisible() throws Exception {
        when(obtenerPublicacionDetalleService.obtener(1L, 7L, 99L))
                .thenReturn(response());

        mockMvc.perform(get("/publicaciones/{idPublicacion}/detalle", 99L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPublicacion").value(99L))
                .andExpect(jsonPath("$.titulo").value("Editorial"));
    }

    @Test
    void obtenerDetalle_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(get("/publicaciones/{idPublicacion}/detalle", 99L)
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void modificar_multipartValido_devuelve200() throws Exception {
        when(modificarPublicacionService.modificar(eq(1L), eq(7L), eq(99L), any()))
                .thenReturn(response());

        mockMvc.perform(multipart(HttpMethod.PUT, "/publicaciones/{idPublicacion}", 99L)
                        .file(archivo())
                        .param("titulo", "Editorial")
                        .param("descripcion", "Descripción")
                        .param("imagenesConservadas", "10")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPublicacion").value(99L))
                .andExpect(jsonPath("$.titulo").value("Editorial"));

        ArgumentCaptor<org.mgroko.backend.publicaciones.dto.ModificarPublicacionRequest> captor =
                ArgumentCaptor.forClass(
                        org.mgroko.backend.publicaciones.dto.ModificarPublicacionRequest.class);
        verify(modificarPublicacionService).modificar(eq(1L), eq(7L), eq(99L), captor.capture());
        assertEquals("Editorial", captor.getValue().getTitulo());
        assertEquals(List.of(10L), captor.getValue().getImagenesConservadas());
        assertEquals(1, captor.getValue().getArchivos().size());
    }

    @Test
    void modificar_sinTitulo_devuelve400() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/publicaciones/{idPublicacion}", 99L)
                        .file(archivo())
                        .param("imagenesConservadas", "10")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modificar_sinImagenesConservadas_devuelve400() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/publicaciones/{idPublicacion}", 99L)
                        .file(archivo())
                        .param("titulo", "Editorial")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modificar_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/publicaciones/{idPublicacion}", 99L)
                        .file(archivo())
                        .param("titulo", "Editorial")
                        .param("imagenesConservadas", "10")
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_devuelvePaginaDePublicaciones() throws Exception {
        PaginaResponse<PublicacionResponse> pagina = new PaginaResponse<>(
                List.of(response()), 0, 10, 1, 1, true, true);
        when(listarPublicacionesPerfilService.listar(
                eq(7L), eq(1L), eq(7L), any(BuscarPublicacionesFiltro.class), eq(0), eq(10)))
                .thenReturn(pagina);

        mockMvc.perform(get("/perfiles/{idPerfil}/publicaciones", 7L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Editorial"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listarOcultas_devuelveTituloYDescripcion() throws Exception {
        PaginaResponse<PublicacionOcultadaResponse> pagina = new PaginaResponse<>(
                List.of(new PublicacionOcultadaResponse(
                        99L,
                        "Editorial oculta",
                        "Descripción oculta",
                        LocalDateTime.of(2026, 11, 10, 12, 0),
                        org.mgroko.backend.modelo.enums.EstadoPublicacion.OCULTA)),
                0, 10, 1, 1, true, true);

        when(listarPublicacionesOcultasService.listar(1L, 7L, "Editorial", 0, 10))
                .thenReturn(pagina);

        mockMvc.perform(get("/publicaciones/ocultas")
                        .principal(auth(1L, 7L))
                        .param("texto", "Editorial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Editorial oculta"))
                .andExpect(jsonPath("$.contenido[0].descripcion").value("Descripción oculta"))
                .andExpect(jsonPath("$.contenido[0].estado").value("OCULTA"));
    }

    @Test
    void feed_devuelvePaginaDeContactos() throws Exception {
        PaginaResponse<PublicacionResponse> pagina = new PaginaResponse<>(
                List.of(response()), 0, 10, 1, 1, true, true);
        when(listarFeedPublicacionesService.listar(1L, 7L, true, 0, 10))
                .thenReturn(pagina);

        mockMvc.perform(get("/publicaciones/feed")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Editorial"));
    }

    @Test
    void feed_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(get("/publicaciones/feed")
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarProyectos_devuelveOpcionesDelSelector() throws Exception {
        when(listarProyectosPublicacionesPerfilService.listar(7L, 1L, 7L))
                .thenReturn(List.of(new ProyectoPublicacionFiltroResponse(25L, "Campaña")));

        mockMvc.perform(get("/perfiles/{idPerfil}/publicaciones/proyectos", 7L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idProyecto").value(25L))
                .andExpect(jsonPath("$[0].nombre").value("Campaña"));
    }

    @Test
    void listar_conFiltros_mapeaParametrosDeBusqueda() throws Exception {
        PaginaResponse<PublicacionResponse> pagina = new PaginaResponse<>(
                List.of(response()), 0, 10, 1, 1, true, true);
        when(listarPublicacionesPerfilService.listar(
                eq(7L), eq(1L), eq(7L), any(BuscarPublicacionesFiltro.class), eq(0), eq(10)))
                .thenReturn(pagina);

        mockMvc.perform(get("/perfiles/{idPerfil}/publicaciones", 7L)
                        .param("fechaDesde", "2026-01-01")
                        .param("fechaHasta", "2026-12-31")
                        .param("idProyecto", "25")
                        .param("titulo", "Editorial")
                        .param("descripcion", "Otoño")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk());

        ArgumentCaptor<BuscarPublicacionesFiltro> captor =
                ArgumentCaptor.forClass(BuscarPublicacionesFiltro.class);
        verify(listarPublicacionesPerfilService).listar(
                eq(7L), eq(1L), eq(7L), captor.capture(), eq(0), eq(10));

        assertEquals(LocalDate.of(2026, 1, 1), captor.getValue().fechaDesde());
        assertEquals(LocalDate.of(2026, 12, 31), captor.getValue().fechaHasta());
        assertEquals(25L, captor.getValue().idProyecto());
        assertEquals("Editorial", captor.getValue().titulo());
        assertEquals("Otoño", captor.getValue().descripcion());
    }

    @Test
    void eliminar_devuelve204() throws Exception {
        mockMvc.perform(delete("/publicaciones/{idPublicacion}", 99L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isNoContent());

        verify(eliminarPublicacionService).eliminar(1L, 7L, 99L);
    }

    @Test
    void eliminar_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(delete("/publicaciones/{idPublicacion}", 99L)
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());

        verify(eliminarPublicacionService, org.mockito.Mockito.never())
                .eliminar(any(), any(), any());
    }

    @Test
    void eliminar_publicacionAjena_devuelve404() throws Exception {
        doThrow(new PublicacionNoEncontradaException(99L))
                .when(eliminarPublicacionService).eliminar(1L, 7L, 99L);

        mockMvc.perform(delete("/publicaciones/{idPublicacion}", 99L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_perfilEnBaja_devuelve409() throws Exception {
        doThrow(new PerfilEnBajaException("El perfil no se encuentra activo."))
                .when(eliminarPublicacionService).eliminar(1L, 7L, 99L);

        mockMvc.perform(delete("/publicaciones/{idPublicacion}", 99L)
                        .principal(auth(1L, 7L)))
                .andExpect(status().isConflict());
    }
}
