package org.mgroko.backend.admin.controlador;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminCaracteristicaTecnicaRequest;
import org.mgroko.backend.admin.dto.AdminValorCaracteristicaRequest;
import org.mgroko.backend.admin.exception.CaracteristicaCodigoDuplicadoException;
import org.mgroko.backend.admin.exception.CaracteristicaEnUsoException;
import org.mgroko.backend.admin.exception.CaracteristicaTecnicaNoEncontradaException;
import org.mgroko.backend.admin.exception.TipoDatoInvalidoException;
import org.mgroko.backend.admin.exception.ValorCaracteristicaAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.ValorCodigoDuplicadoException;
import org.mgroko.backend.admin.exception.ValorEnUsoException;
import org.mgroko.backend.admin.servicio.AdminCaracteristicaTecnicaService;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.dto.ValorCaracteristicaResponse;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
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

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AdminCaracteristicaTecnicaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCaracteristicaTecnicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminCaracteristicaTecnicaService adminCaracteristicaTecnicaService;

    private UsernamePasswordAuthenticationToken autenticacionAdmin() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private AdminCaracteristicaTecnicaRequest requestCaracteristica(String codigo, String tipoDato) {
        return new AdminCaracteristicaTecnicaRequest(codigo, "cm", 1L, tipoDato, List.of());
    }

    private AdminValorCaracteristicaRequest requestValor(String codigo, String colorHex) {
        return new AdminValorCaracteristicaRequest(null, codigo, colorHex);
    }

    // --- GET /admin/caracteristicas-tecnicas ---

    @Test
    void listar_retorna200YLista() throws Exception {
        CaracteristicaTecnicaResponse c = new CaracteristicaTecnicaResponse(
                1L, "altura", "cm", 1L, "modelo", "NUMERICO", List.of());

        when(adminCaracteristicaTecnicaService.listar()).thenReturn(List.of(c));

        mockMvc.perform(get("/admin/caracteristicas-tecnicas")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("altura"));
    }

    // --- POST /admin/caracteristicas-tecnicas ---

    @Test
    void crear_valido_retorna201() throws Exception {
        AdminCaracteristicaTecnicaRequest req = requestCaracteristica("altura", "NUMERICO");
        CaracteristicaTecnicaResponse res = new CaracteristicaTecnicaResponse(
                10L, "altura", "cm", 1L, "modelo", "NUMERICO", List.of());

        when(adminCaracteristicaTecnicaService.crear(any())).thenReturn(res);

        mockMvc.perform(post("/admin/caracteristicas-tecnicas")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCaracteristica").value(10))
                .andExpect(jsonPath("$.codigo").value("altura"));
    }

    @Test
    void crear_codigoDuplicado_retorna409() throws Exception {
        AdminCaracteristicaTecnicaRequest req = requestCaracteristica("altura", "NUMERICO");

        when(adminCaracteristicaTecnicaService.crear(any()))
                .thenThrow(new CaracteristicaCodigoDuplicadoException("Ya existe código"));

        mockMvc.perform(post("/admin/caracteristicas-tecnicas")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe código"));
    }

    @Test
    void crear_profesionNoExiste_retorna400() throws Exception {
        AdminCaracteristicaTecnicaRequest req = requestCaracteristica("altura", "NUMERICO");

        when(adminCaracteristicaTecnicaService.crear(any()))
                .thenThrow(new ProfesionNoEncontradaException("Profesión no encontrada"));

        mockMvc.perform(post("/admin/caracteristicas-tecnicas")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Profesión no encontrada"));
    }

    @Test
    void crear_tipoDatoInvalido_retorna400() throws Exception {
        AdminCaracteristicaTecnicaRequest req = requestCaracteristica("altura", "FECHA");

        when(adminCaracteristicaTecnicaService.crear(any()))
                .thenThrow(new TipoDatoInvalidoException("tipoDato no admitido"));

        mockMvc.perform(post("/admin/caracteristicas-tecnicas")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("tipoDato no admitido"));
    }

    // --- PUT /admin/caracteristicas-tecnicas/{id} ---

    @Test
    void actualizar_valido_retorna200() throws Exception {
        AdminCaracteristicaTecnicaRequest req = requestCaracteristica("altura_nueva", "NUMERICO");
        CaracteristicaTecnicaResponse res = new CaracteristicaTecnicaResponse(
                5L, "altura_nueva", "cm", 1L, "modelo", "NUMERICO", List.of());

        when(adminCaracteristicaTecnicaService.actualizar(eq(5L), any())).thenReturn(res);

        mockMvc.perform(put("/admin/caracteristicas-tecnicas/5")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("altura_nueva"));
    }

    @Test
    void actualizar_inexistente_retorna404() throws Exception {
        AdminCaracteristicaTecnicaRequest req = requestCaracteristica("altura", "NUMERICO");

        when(adminCaracteristicaTecnicaService.actualizar(eq(999L), any()))
                .thenThrow(new CaracteristicaTecnicaNoEncontradaException("No encontrada"));

        mockMvc.perform(put("/admin/caracteristicas-tecnicas/999")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No encontrada"));
    }

    @Test
    void actualizar_caracteristicaEnUso_retorna409() throws Exception {
        AdminCaracteristicaTecnicaRequest req = requestCaracteristica("ojos", "NUMERICO");

        when(adminCaracteristicaTecnicaService.actualizar(eq(5L), any()))
                .thenThrow(new CaracteristicaEnUsoException("Posee valores asociados"));

        mockMvc.perform(put("/admin/caracteristicas-tecnicas/5")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Posee valores asociados"));
    }

    // --- DELETE /admin/caracteristicas-tecnicas/{id} ---

    @Test
    void eliminar_valido_retorna204() throws Exception {
        doNothing().when(adminCaracteristicaTecnicaService).eliminar(5L);

        mockMvc.perform(delete("/admin/caracteristicas-tecnicas/5")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isNoContent());

        verify(adminCaracteristicaTecnicaService).eliminar(5L);
    }

    @Test
    void eliminar_enUso_retorna409() throws Exception {
        doThrow(new CaracteristicaEnUsoException("Está en uso"))
                .when(adminCaracteristicaTecnicaService).eliminar(5L);

        mockMvc.perform(delete("/admin/caracteristicas-tecnicas/5")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Está en uso"));
    }

    // --- POST /admin/caracteristicas-tecnicas/{id}/valores ---

    @Test
    void agregarValor_valido_retorna201() throws Exception {
        AdminValorCaracteristicaRequest req = requestValor("AZUL", "#0000FF");
        ValorCaracteristicaResponse res = new ValorCaracteristicaResponse(20L, "AZUL", "#0000FF");

        when(adminCaracteristicaTecnicaService.agregarValor(eq(10L), any())).thenReturn(res);

        mockMvc.perform(post("/admin/caracteristicas-tecnicas/10/valores")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idValor").value(20))
                .andExpect(jsonPath("$.codigo").value("AZUL"));
    }

    @Test
    void agregarValor_codigoDuplicado_retorna409() throws Exception {
        AdminValorCaracteristicaRequest req = requestValor("AZUL", "#0000FF");

        when(adminCaracteristicaTecnicaService.agregarValor(eq(10L), any()))
                .thenThrow(new ValorCodigoDuplicadoException("Valor duplicado"));

        mockMvc.perform(post("/admin/caracteristicas-tecnicas/10/valores")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Valor duplicado"));
    }

    // --- PUT /admin/caracteristicas-tecnicas/valores/{idValor} ---

    @Test
    void actualizarValor_valido_retorna200() throws Exception {
        AdminValorCaracteristicaRequest req = requestValor("CELESTE", "#00FFFF");
        ValorCaracteristicaResponse res = new ValorCaracteristicaResponse(20L, "CELESTE", "#00FFFF");

        when(adminCaracteristicaTecnicaService.actualizarValor(eq(20L), any())).thenReturn(res);

        mockMvc.perform(put("/admin/caracteristicas-tecnicas/valores/20")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("CELESTE"));
    }

    @Test
    void actualizarValor_noEncontrado_retorna404() throws Exception {
        AdminValorCaracteristicaRequest req = requestValor("CELESTE", "#00FFFF");

        when(adminCaracteristicaTecnicaService.actualizarValor(eq(99L), any()))
                .thenThrow(new ValorCaracteristicaAdminNoEncontradoException("Valor no encontrado"));

        mockMvc.perform(put("/admin/caracteristicas-tecnicas/valores/99")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Valor no encontrado"));
    }

    // --- DELETE /admin/caracteristicas-tecnicas/valores/{idValor} ---

    @Test
    void eliminarValor_valido_retorna204() throws Exception {
        doNothing().when(adminCaracteristicaTecnicaService).eliminarValor(20L);

        mockMvc.perform(delete("/admin/caracteristicas-tecnicas/valores/20")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isNoContent());

        verify(adminCaracteristicaTecnicaService).eliminarValor(20L);
    }

    @Test
    void eliminarValor_enUso_retorna409() throws Exception {
        doThrow(new ValorEnUsoException("El valor está en uso por perfiles"))
                .when(adminCaracteristicaTecnicaService).eliminarValor(20L);

        mockMvc.perform(delete("/admin/caracteristicas-tecnicas/valores/20")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El valor está en uso por perfiles"));
    }
}
