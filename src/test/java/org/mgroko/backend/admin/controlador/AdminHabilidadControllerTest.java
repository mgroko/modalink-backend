package org.mgroko.backend.admin.controlador;

import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminHabilidadRequest;
import org.mgroko.backend.admin.exception.HabilidadDuplicadaException;
import org.mgroko.backend.admin.exception.HabilidadEnUsoException;
import org.mgroko.backend.admin.servicio.AdminHabilidadService;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class, controllers = AdminHabilidadController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminHabilidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminHabilidadService adminHabilidadService;

    private UsernamePasswordAuthenticationToken autenticacionAdmin() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private HabilidadResponse habilidad() {
        return new HabilidadResponse(1L, "FOTO_ESTUDIO",
                "Fotografía de Estudio", "Iluminación y sets.");
    }

    // ------------------------------------------------------------- listar

    @Test
    void listar_conNombre_retorna200() throws Exception {
        when(adminHabilidadService.listar("foto")).thenReturn(List.of(habilidad()));

        mockMvc.perform(get("/admin/habilidades")
                        .param("nombre", "foto")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("FOTO_ESTUDIO"));
    }

    @Test
    void listar_sinNombre_retorna200() throws Exception {
        when(adminHabilidadService.listar(null)).thenReturn(List.of());

        mockMvc.perform(get("/admin/habilidades")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------ obtener

    @Test
    void obtener_existente_retorna200() throws Exception {
        when(adminHabilidadService.obtener(1L)).thenReturn(habilidad());

        mockMvc.perform(get("/admin/habilidades/1")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idHabilidad").value(1))
                .andExpect(jsonPath("$.nombre").value("Fotografía de Estudio"));
    }

    @Test
    void obtener_inexistente_retorna404() throws Exception {
        when(adminHabilidadService.obtener(99L))
                .thenThrow(new HabilidadNoEncontradaException(99L));

        mockMvc.perform(get("/admin/habilidades/99")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpStatus").value(404));
    }

    // -------------------------------------------------------------- crear

    @Test
    void crear_datosValidos_retorna201() throws Exception {
        when(adminHabilidadService.crear(any(AdminHabilidadRequest.class)))
                .thenReturn(habilidad());

        mockMvc.perform(post("/admin/habilidades")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Fotografía de Estudio\",\"descripcion\":\"Iluminación y sets.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idHabilidad").value(1))
                .andExpect(jsonPath("$.codigo").value("FOTO_ESTUDIO"));
    }

    @Test
    void crear_nombreVacio_retorna400() throws Exception {
        mockMvc.perform(post("/admin/habilidades")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"   \",\"descripcion\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());

        verify(adminHabilidadService, never()).crear(any(AdminHabilidadRequest.class));
    }

    @Test
    void crear_nombreDuplicado_retorna409() throws Exception {
        when(adminHabilidadService.crear(any(AdminHabilidadRequest.class)))
                .thenThrow(new HabilidadDuplicadaException(
                        "Ya existe una habilidad con el nombre Fotografía de Estudio."));

        mockMvc.perform(post("/admin/habilidades")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Fotografía de Estudio\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409));
    }

    // ---------------------------------------------------------- actualizar

    @Test
    void actualizar_datosValidos_retorna200() throws Exception {
        when(adminHabilidadService.actualizar(any(Long.class), any(AdminHabilidadRequest.class)))
                .thenReturn(new HabilidadResponse(1L, "FOTO_ESTUDIO",
                        "Fotografía Editorial", "Nueva descripción"));

        mockMvc.perform(put("/admin/habilidades/1")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Fotografía Editorial\",\"descripcion\":\"Nueva descripción\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Fotografía Editorial"));
    }

    @Test
    void actualizar_inexistente_retorna404() throws Exception {
        when(adminHabilidadService.actualizar(any(Long.class), any(AdminHabilidadRequest.class)))
                .thenThrow(new HabilidadNoEncontradaException(99L));

        mockMvc.perform(put("/admin/habilidades/99")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otra\"}"))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------- eliminar

    @Test
    void eliminar_sinUso_retorna204() throws Exception {
        mockMvc.perform(delete("/admin/habilidades/1")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isNoContent());

        verify(adminHabilidadService).eliminar(1L);
    }

    @Test
    void eliminar_enUso_retorna409() throws Exception {
        doThrow(new HabilidadEnUsoException(
                "La habilidad está en uso por requerimientos de proyectos y no puede eliminarse."))
                .when(adminHabilidadService).eliminar(1L);

        mockMvc.perform(delete("/admin/habilidades/1")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409));
    }
}
