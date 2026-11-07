package org.mgroko.backend.perfiles.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.servicio.GestionTerminosCondicionesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = PerfilTerminosCondicionesController.class)
@AutoConfigureMockMvc(addFilters = false)
class PerfilTerminosCondicionesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestionTerminosCondicionesService gestionTerminosCondicionesService;

    private UsernamePasswordAuthenticationToken autenticacion() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private TerminosCondicionesResponse vigente() {
        return new TerminosCondicionesResponse(
                5L,
                7L,
                "Texto vigente",
                LocalDateTime.of(2026, 11, 1, 10, 0),
                null,
                false);
    }

    private TerminosCondicionesResponse plantilla() {
        return new TerminosCondicionesResponse(
                5L, null, "Plantilla", null, null, true);
    }

    @Test
    void obtener_conVersion_retorna200() throws Exception {
        when(gestionTerminosCondicionesService.obtener(1L, 5L)).thenReturn(vigente());

        mockMvc.perform(get("/perfiles/5/terminos-condiciones")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTyc").value(7))
                .andExpect(jsonPath("$.descripcion").value("Texto vigente"))
                .andExpect(jsonPath("$.esPlantilla").value(false));
    }

    @Test
    void obtener_sinVersion_retornaPlantilla() throws Exception {
        when(gestionTerminosCondicionesService.obtener(1L, 5L)).thenReturn(plantilla());

        mockMvc.perform(get("/perfiles/5/terminos-condiciones")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTyc").doesNotExist())
                .andExpect(jsonPath("$.esPlantilla").value(true));
    }

    @Test
    void obtener_perfilAjeno_retorna404() throws Exception {
        when(gestionTerminosCondicionesService.obtener(1L, 99L))
                .thenThrow(new PerfilNoEncontradoException());

        mockMvc.perform(get("/perfiles/99/terminos-condiciones")
                        .principal(autenticacion()))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_retorna200() throws Exception {
        when(gestionTerminosCondicionesService.actualizar(
                eq(1L), eq(5L), any())).thenReturn(vigente());

        mockMvc.perform(put("/perfiles/5/terminos-condiciones")
                        .principal(autenticacion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Texto vigente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTyc").value(7));
    }

    @Test
    void actualizar_textoVacio_retorna400() throws Exception {
        mockMvc.perform(put("/perfiles/5/terminos-condiciones")
                        .principal(autenticacion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").exists());

        verify(gestionTerminosCondicionesService, never())
                .actualizar(any(), any(), any());
    }

    @Test
    void actualizar_textoMuyLargo_retorna400() throws Exception {
        String texto = "x".repeat(5001);

        mockMvc.perform(put("/perfiles/5/terminos-condiciones")
                        .principal(autenticacion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"" + texto + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").exists());
    }

    @Test
    void actualizar_perfilEnBaja_retorna409() throws Exception {
        when(gestionTerminosCondicionesService.actualizar(
                eq(1L), eq(5L), any()))
                .thenThrow(new PerfilEnBajaException("El perfil no se encuentra activo."));

        mockMvc.perform(put("/perfiles/5/terminos-condiciones")
                        .principal(autenticacion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Texto\"}"))
                .andExpect(status().isConflict());
    }
}
