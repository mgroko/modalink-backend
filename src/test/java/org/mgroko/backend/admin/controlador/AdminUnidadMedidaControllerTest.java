package org.mgroko.backend.admin.controlador;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminUnidadMedidaRequest;
import org.mgroko.backend.admin.dto.UnidadMedidaResponse;
import org.mgroko.backend.admin.exception.TipoDatoInvalidoException;
import org.mgroko.backend.admin.exception.UnidadMedidaDuplicadaException;
import org.mgroko.backend.admin.exception.UnidadMedidaEnUsoException;
import org.mgroko.backend.admin.exception.UnidadMedidaNoEncontradaException;
import org.mgroko.backend.admin.servicio.AdminUnidadMedidaService;
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

@WebMvcTest(controllers = AdminUnidadMedidaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUnidadMedidaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUnidadMedidaService adminUnidadMedidaService;

    private UsernamePasswordAuthenticationToken autenticacionAdmin() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private UnidadMedidaResponse unidadCm() {
        return new UnidadMedidaResponse(1L, "Centimetro", "cm", "NUMERICO");
    }

    // ------------------------------------------------------------- listar

    @Test
    void listar_sinFiltro_retornaTodas() throws Exception {
        when(adminUnidadMedidaService.listar(null)).thenReturn(List.of(
                unidadCm(),
                new UnidadMedidaResponse(2L, "Kilogramo", "kg", "NUMERICO")));

        mockMvc.perform(get("/admin/unidades-medida")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].simbolo").value("cm"))
                .andExpect(jsonPath("$[1].simbolo").value("kg"));
    }

    @Test
    void listar_conFiltroTipoDato_retornaFiltradas() throws Exception {
        when(adminUnidadMedidaService.listar("NUMERICO")).thenReturn(List.of(unidadCm()));

        mockMvc.perform(get("/admin/unidades-medida")
                        .param("tipoDato", "NUMERICO")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].simbolo").value("cm"));
    }

    @Test
    void listar_conFiltroInvalido_retornaError() throws Exception {
        when(adminUnidadMedidaService.listar("ENUMERADO"))
                .thenThrow(new TipoDatoInvalidoException(
                        "tipoDatoPermitido debe ser NUMERICO o TEXTO."));

        mockMvc.perform(get("/admin/unidades-medida")
                        .param("tipoDato", "ENUMERADO")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value(400));
    }

    // ------------------------------------------------------------- obtener

    @Test
    void obtener_existente_retornaUnidad() throws Exception {
        when(adminUnidadMedidaService.obtener(1L)).thenReturn(unidadCm());

        mockMvc.perform(get("/admin/unidades-medida/1")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUnidad").value(1))
                .andExpect(jsonPath("$.simbolo").value("cm"));
    }

    @Test
    void obtener_inexistente_retorna404() throws Exception {
        when(adminUnidadMedidaService.obtener(99L))
                .thenThrow(new UnidadMedidaNoEncontradaException("Unidad de medida no encontrada: 99"));

        mockMvc.perform(get("/admin/unidades-medida/99")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpStatus").value(404));
    }

    // -------------------------------------------------------------- crear

    @Test
    void crear_datosValidos_retorna201() throws Exception {
        when(adminUnidadMedidaService.crear(any(AdminUnidadMedidaRequest.class)))
                .thenReturn(new UnidadMedidaResponse(5L, "Centimetro", "cm", "NUMERICO"));

        mockMvc.perform(post("/admin/unidades-medida")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Centimetro\",\"simbolo\":\"cm\",\"tipoDatoPermitido\":\"NUMERICO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUnidad").value(5))
                .andExpect(jsonPath("$.nombre").value("Centimetro"))
                .andExpect(jsonPath("$.tipoDatoPermitido").value("NUMERICO"));
    }

    @Test
    void crear_camposObligatoriosVacios_retorna400() throws Exception {
        mockMvc.perform(post("/admin/unidades-medida")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"simbolo\":\"cm\",\"tipoDatoPermitido\":\"NUMERICO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());

        verify(adminUnidadMedidaService, never()).crear(any(AdminUnidadMedidaRequest.class));
    }

    @Test
    void crear_nombreDuplicado_retorna409() throws Exception {
        when(adminUnidadMedidaService.crear(any(AdminUnidadMedidaRequest.class)))
                .thenThrow(new UnidadMedidaDuplicadaException(
                        "Ya existe una unidad de medida con el nombre Centimetro."));

        mockMvc.perform(post("/admin/unidades-medida")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Centimetro\",\"simbolo\":\"cm2\",\"tipoDatoPermitido\":\"NUMERICO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409));
    }

    // ---------------------------------------------------------- actualizar

    @Test
    void actualizar_datosValidos_retorna200() throws Exception {
        when(adminUnidadMedidaService.actualizar(any(Long.class), any(AdminUnidadMedidaRequest.class)))
                .thenReturn(new UnidadMedidaResponse(1L, "Centimetros", "cm", "NUMERICO"));

        mockMvc.perform(put("/admin/unidades-medida/1")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Centimetros\",\"simbolo\":\"cm\",\"tipoDatoPermitido\":\"NUMERICO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Centimetros"));
    }

    @Test
    void actualizar_inexistente_retorna404() throws Exception {
        when(adminUnidadMedidaService.actualizar(any(Long.class), any(AdminUnidadMedidaRequest.class)))
                .thenThrow(new UnidadMedidaNoEncontradaException("Unidad de medida no encontrada: 99"));

        mockMvc.perform(put("/admin/unidades-medida/99")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Metro\",\"simbolo\":\"m\",\"tipoDatoPermitido\":\"NUMERICO\"}"))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------- eliminar

    @Test
    void eliminar_sinUso_retorna204() throws Exception {
        mockMvc.perform(delete("/admin/unidades-medida/1")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isNoContent());

        verify(adminUnidadMedidaService).eliminar(1L);
    }

    @Test
    void eliminar_asociadaACaracteristica_retorna409() throws Exception {
        doThrow(new UnidadMedidaEnUsoException(
                "La unidad de medida está asociada a una característica técnica y no puede eliminarse."))
                .when(adminUnidadMedidaService).eliminar(1L);

        mockMvc.perform(delete("/admin/unidades-medida/1")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409));
    }
}
