package org.mgroko.backend.polaroids.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.polaroids.dto.CrearPolaroidRequest;
import org.mgroko.backend.polaroids.dto.GrupoPolaroidResponse;
import org.mgroko.backend.polaroids.dto.PolaroidResponse;
import org.mgroko.backend.polaroids.dto.PolaroidsPerfilResponse;
import org.mgroko.backend.polaroids.dto.TipoPolaroidResponse;
import org.mgroko.backend.polaroids.servicio.CrearPolaroidPerfilService;
import org.mgroko.backend.polaroids.servicio.EliminarPolaroidPerfilService;
import org.mgroko.backend.polaroids.servicio.ListarPolaroidsPerfilService;
import org.mgroko.backend.polaroids.servicio.ModificarPolaroidPerfilService;
import org.mgroko.backend.polaroids.servicio.PolaroidCatalogoService;
import org.mgroko.backend.polaroids.servicio.ReordenarPolaroidsPerfilService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = PolaroidPerfilController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolaroidPerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PolaroidCatalogoService polaroidCatalogoService;

    @MockitoBean
    private ListarPolaroidsPerfilService listarPolaroidsPerfilService;

    @MockitoBean
    private CrearPolaroidPerfilService crearPolaroidPerfilService;

    @MockitoBean
    private ModificarPolaroidPerfilService modificarPolaroidPerfilService;

    @MockitoBean
    private ReordenarPolaroidsPerfilService reordenarPolaroidsPerfilService;

    @MockitoBean
    private EliminarPolaroidPerfilService eliminarPolaroidPerfilService;

    @Test
    void listarTipos_devuelveCatalogoAgrupado() throws Exception {
        when(polaroidCatalogoService.listar()).thenReturn(List.of(
                new GrupoPolaroidResponse(
                        1L,
                        "PRIMER_PLANO",
                        "Primer plano",
                        List.of(new TipoPolaroidResponse(
                                2L,
                                "PRIMER_PLANO_PERFIL",
                                "De perfil",
                                "Primer plano · De perfil")))));

        mockMvc.perform(get("/polaroids/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("PRIMER_PLANO"))
                .andExpect(jsonPath("$[0].tipos[0].nombreCompleto").value("Primer plano · De perfil"));
    }

    @Test
    void listar_devuelveMaximoYContenido() throws Exception {
        when(listarPolaroidsPerfilService.listar(7L, 1L))
                .thenReturn(new PolaroidsPerfilResponse(7L, 8, 1, List.of(polaroid())));

        mockMvc.perform(get("/perfiles/7/polaroids").principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPerfil").value(7L))
                .andExpect(jsonPath("$.maximo").value(8))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.polaroids[0].tipo.nombreCompleto")
                        .value("Primer plano · De perfil"));
    }

    @Test
    void crear_devuelve201() throws Exception {
        when(crearPolaroidPerfilService.crear(eq(1L), eq(7L), eq(7L), any(CrearPolaroidRequest.class)))
                .thenReturn(polaroid());

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "pola.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/perfiles/7/polaroids")
                        .file(archivo)
                        .param("idTipoPolaroid", "2")
                        .principal(autenticacion()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPolaroid").value(10L))
                .andExpect(jsonPath("$.orden").value(1));
    }

    private PolaroidResponse polaroid() {
        return new PolaroidResponse(
                10L,
                20L,
                "/uploads/polaroids/polaroid_1.webp",
                1,
                LocalDateTime.of(2026, 11, 10, 14, 30),
                new TipoPolaroidResponse(
                        2L,
                        "PRIMER_PLANO_PERFIL",
                        "De perfil",
                        "Primer plano · De perfil"));
    }

    private UsernamePasswordAuthenticationToken autenticacion() {
        var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
        auth.setDetails(new ContextoAutenticacion(1L, 7L, "Modelo"));
        return auth;
    }
}
