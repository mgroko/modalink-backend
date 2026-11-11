package org.mgroko.backend.publicaciones.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ComentarioResponse;
import org.mgroko.backend.publicaciones.dto.MeGustaResponse;
import org.mgroko.backend.publicaciones.servicio.CrearComentarioService;
import org.mgroko.backend.publicaciones.servicio.GestionarMeGustaPublicacionService;
import org.mgroko.backend.publicaciones.servicio.ListarComentariosService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = InteraccionPublicacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InteraccionPublicacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestionarMeGustaPublicacionService gestionarMeGustaService;

    @MockitoBean
    private CrearComentarioService crearComentarioService;

    @MockitoBean
    private ListarComentariosService listarComentariosService;

    private static UsernamePasswordAuthenticationToken auth(Long idUsuario, Long idPerfilActivo) {
        var token = new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(idUsuario, idPerfilActivo, "Luna Diseños"));
        return token;
    }

    @Test
    void darMeGusta_devuelve200ConEstadoYContador() throws Exception {
        when(gestionarMeGustaService.dar(1L, 7L, 99L))
                .thenReturn(new MeGustaResponse(99L, true, 5));

        mockMvc.perform(post("/publicaciones/99/me-gusta")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPublicacion").value(99L))
                .andExpect(jsonPath("$.meGusta").value(true))
                .andExpect(jsonPath("$.cantidadMeGusta").value(5));

        verify(gestionarMeGustaService).dar(1L, 7L, 99L);
    }

    @Test
    void quitarMeGusta_devuelve200ConEstadoYContador() throws Exception {
        when(gestionarMeGustaService.quitar(1L, 7L, 99L))
                .thenReturn(new MeGustaResponse(99L, false, 4));

        mockMvc.perform(delete("/publicaciones/99/me-gusta")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meGusta").value(false))
                .andExpect(jsonPath("$.cantidadMeGusta").value(4));

        verify(gestionarMeGustaService).quitar(1L, 7L, 99L);
    }

    @Test
    void comentar_devuelve201ConComentario() throws Exception {
        when(crearComentarioService.crear(eq(1L), eq(7L), eq(99L), any()))
                .thenReturn(new ComentarioResponse(
                        10L,
                        99L,
                        new AutorPublicacionResponse(
                                7L,
                                "Luna Diseños",
                                "Diseñadora",
                                "/uploads/perfiles/luna.webp"),
                        "Muy buena publicación",
                        LocalDateTime.of(2026, 11, 11, 12, 0)));

        mockMvc.perform(post("/publicaciones/99/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Muy buena publicación\"}")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idComentario").value(10L))
                .andExpect(jsonPath("$.idPublicacion").value(99L))
                .andExpect(jsonPath("$.texto").value("Muy buena publicación"));

        verify(crearComentarioService).crear(eq(1L), eq(7L), eq(99L), any());
    }

    @Test
    void comentar_textoVacio_devuelve400() throws Exception {
        mockMvc.perform(post("/publicaciones/99/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"   \"}")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isBadRequest());

        verify(crearComentarioService, org.mockito.Mockito.never())
                .crear(any(), any(), any(), any());
    }

    @Test
    void listarComentarios_devuelvePagina() throws Exception {
        PaginaResponse<ComentarioResponse> pagina = new PaginaResponse<>(
                List.of(),
                0,
                10,
                0,
                0,
                true,
                true);

        when(listarComentariosService.listar(1L, 7L, 99L, 0, 10))
                .thenReturn(pagina);

        mockMvc.perform(get("/publicaciones/99/comentarios")
                        .principal(auth(1L, 7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.totalElementos").value(0));

        verify(listarComentariosService).listar(1L, 7L, 99L, 0, 10);
    }
}
