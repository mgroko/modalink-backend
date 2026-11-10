package org.mgroko.backend.publicaciones.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.BuscarPublicacionesFiltro;
import org.mgroko.backend.publicaciones.dto.ImagenPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.servicio.CrearPublicacionService;
import org.mgroko.backend.publicaciones.servicio.ListarPublicacionesPerfilService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
                0);
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
}
