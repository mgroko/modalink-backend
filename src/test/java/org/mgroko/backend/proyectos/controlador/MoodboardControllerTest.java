package org.mgroko.backend.proyectos.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.proyectos.dto.ImagenMoodboardResponse;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.exception.LimiteImagenesMoodboardExcedidoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.MoodboardImagenesService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MoodboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class MoodboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MoodboardImagenesService moodboardImagenesService;

    private static UsernamePasswordAuthenticationToken auth(Long idUsuario, Long idPerfilActivo) {
        var token = new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null, List.of());
        token.setDetails(new ContextoAutenticacion(idUsuario, idPerfilActivo, "Luna Diseños"));
        return token;
    }

    @Test
    void subirImagenes_conPerfilActivo_devuelve200ConImagenes() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivos", "mood.jpg", MediaType.IMAGE_JPEG_VALUE, "bytes".getBytes());

        MoodboardResponse response = new MoodboardResponse(
                77L,
                "Paleta fría",
                LocalDateTime.of(2026, 6, 1, 10, 0),
                List.of(new ImagenMoodboardResponse(5L, "/uploads/perfiles/moodboard_1.webp")));

        when(moodboardImagenesService.subirImagenes(eq(1L), eq(10L), eq(101L), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/proyectos/{id}/moodboard/imagenes", 101L)
                        .file(archivo)
                        .principal(auth(1L, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMoodboard").value(77L))
                .andExpect(jsonPath("$.descripcion").value("Paleta fría"))
                .andExpect(jsonPath("$.imagenes.length()").value(1))
                .andExpect(jsonPath("$.imagenes[0].idImagen").value(5L))
                .andExpect(jsonPath("$.imagenes[0].url").value("/uploads/perfiles/moodboard_1.webp"));
    }

    @Test
    void subirImagenes_sinPerfilActivo_devuelve404() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivos", "mood.jpg", MediaType.IMAGE_JPEG_VALUE, "bytes".getBytes());

        mockMvc.perform(multipart("/proyectos/{id}/moodboard/imagenes", 101L)
                        .file(archivo)
                        .principal(auth(1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void subirImagenes_proyectoInexistente_devuelve404() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivos", "mood.jpg", MediaType.IMAGE_JPEG_VALUE, "bytes".getBytes());

        when(moodboardImagenesService.subirImagenes(eq(1L), eq(10L), eq(999L), any()))
                .thenThrow(new ProyectoNoEncontradoException(999L));

        mockMvc.perform(multipart("/proyectos/{id}/moodboard/imagenes", 999L)
                        .file(archivo)
                        .principal(auth(1L, 10L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void subirImagenes_excedeLimite_devuelve400() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivos", "mood.jpg", MediaType.IMAGE_JPEG_VALUE, "bytes".getBytes());

        when(moodboardImagenesService.subirImagenes(eq(1L), eq(10L), eq(101L), any()))
                .thenThrow(new LimiteImagenesMoodboardExcedidoException(10));

        mockMvc.perform(multipart("/proyectos/{id}/moodboard/imagenes", 101L)
                        .file(archivo)
                        .principal(auth(1L, 10L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El moodboard no puede contener más de 10 imágenes."));
    }
}
