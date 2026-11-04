package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.perfiles.dto.AgregarHabilidadPerfilRequest;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.perfiles.exception.HabilidadNoAsociadaException;
import org.mgroko.backend.perfiles.exception.HabilidadYaAsociadaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.servicio.GestionHabilidadPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PerfilHabilidadController.class)
@AutoConfigureMockMvc(addFilters = false)
class PerfilHabilidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestionHabilidadPerfilService gestionHabilidadPerfilService;

    private UsernamePasswordAuthenticationToken autenticacion() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private HabilidadResponse habilidad() {
        return new HabilidadResponse(3L, "EDICION_DIGITAL",
                "Retoque y Edición Digital", "Postproducción digital.");
    }

    // ------------------------------------------------------------- listar

    @Test
    void listar_retorna200() throws Exception {
        when(gestionHabilidadPerfilService.listar(1L, 5L)).thenReturn(List.of(habilidad()));

        mockMvc.perform(get("/perfiles/5/habilidades")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idHabilidad").value(3))
                .andExpect(jsonPath("$[0].codigo").value("EDICION_DIGITAL"));
    }

    @Test
    void listar_perfilAjeno_retorna404() throws Exception {
        when(gestionHabilidadPerfilService.listar(1L, 99L))
                .thenThrow(new PerfilNoEncontradoException());

        mockMvc.perform(get("/perfiles/99/habilidades")
                        .principal(autenticacion()))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ agregar

    @Test
    void agregar_retorna201() throws Exception {
        when(gestionHabilidadPerfilService.agregar(1L, 5L, 3L)).thenReturn(habilidad());

        mockMvc.perform(post("/perfiles/5/habilidades")
                        .principal(autenticacion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idHabilidad\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idHabilidad").value(3))
                .andExpect(jsonPath("$.nombre").value("Retoque y Edición Digital"));
    }

    @Test
    void agregar_sinIdHabilidad_retorna400() throws Exception {
        mockMvc.perform(post("/perfiles/5/habilidades")
                        .principal(autenticacion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.idHabilidad").exists());

        verify(gestionHabilidadPerfilService, never())
                .agregar(any(Long.class), any(Long.class), any(Long.class));
    }

    @Test
    void agregar_yaAsociada_retorna409() throws Exception {
        when(gestionHabilidadPerfilService.agregar(1L, 5L, 3L))
                .thenThrow(new HabilidadYaAsociadaException(
                        "El perfil ya tiene asociada la habilidad."));

        mockMvc.perform(post("/perfiles/5/habilidades")
                        .principal(autenticacion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idHabilidad\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409));
    }

    // -------------------------------------------------------------- quitar

    @Test
    void quitar_retorna204() throws Exception {
        mockMvc.perform(delete("/perfiles/5/habilidades/3")
                        .principal(autenticacion()))
                .andExpect(status().isNoContent());

        verify(gestionHabilidadPerfilService).quitar(1L, 5L, 3L);
    }

    @Test
    void quitar_noAsociada_retorna409() throws Exception {
        doThrow(new HabilidadNoAsociadaException(
                "El perfil no tiene asociada la habilidad."))
                .when(gestionHabilidadPerfilService).quitar(eq(1L), eq(5L), eq(3L));

        mockMvc.perform(delete("/perfiles/5/habilidades/3")
                        .principal(autenticacion()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409));
    }
}
