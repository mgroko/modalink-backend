package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminTipoContratoCreateRequest;
import org.mgroko.backend.admin.dto.AdminTipoContratoEstadoRequest;
import org.mgroko.backend.admin.dto.AdminTipoContratoResponse;
import org.mgroko.backend.admin.dto.AdminTipoContratoResumenResponse;
import org.mgroko.backend.admin.dto.AdminTipoContratoUpdateRequest;
import org.mgroko.backend.admin.dto.ValidacionPlantillaResponse;
import org.mgroko.backend.admin.dto.ValidarPlantillaRequest;
import org.mgroko.backend.admin.exception.PlantillaContratoInvalidaException;
import org.mgroko.backend.admin.exception.TipoContratoDuplicadoException;
import org.mgroko.backend.admin.exception.TipoContratoEnUsoException;
import org.mgroko.backend.admin.exception.TipoContratoNoEncontradoException;
import org.mgroko.backend.admin.servicio.AdminTipoContratoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = AdminTipoContratoController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTipoContratoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminTipoContratoService adminTipoContratoService;

    private UsernamePasswordAuthenticationToken autenticacionAdmin() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private AdminTipoContratoResumenResponse resumen() {
        return new AdminTipoContratoResumenResponse(
                1L,
                "ACUERDO_GENERAL",
                "Acuerdo general",
                "Acuerdo general entre las partes.",
                true,
                0,
                0,
                0,
                true,
                true,
                true,
                null,
                null,
                null);
    }

    private AdminTipoContratoResponse detalle() {
        return new AdminTipoContratoResponse(
                1L,
                "ACUERDO_GENERAL",
                "Acuerdo general",
                "Acuerdo general entre las partes.",
                "Contrato {{tipo}} {{proyecto}} {{partes}}",
                true,
                0,
                0,
                0,
                true,
                true,
                true,
                null,
                null,
                null);
    }

    @Test
    void listar_retorna200() throws Exception {
        when(adminTipoContratoService.listar("acuerdo", true))
                .thenReturn(List.of(resumen()));

        mockMvc.perform(get("/admin/tipos-contrato")
                        .param("nombre", "acuerdo")
                        .param("incluirInactivos", "true")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("ACUERDO_GENERAL"));
    }

    @Test
    void obtener_existente_retorna200() throws Exception {
        when(adminTipoContratoService.obtener(1L)).thenReturn(detalle());

        mockMvc.perform(get("/admin/tipos-contrato/1")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTipoAcuerdo").value(1))
                .andExpect(jsonPath("$.puedeEditar").value(true));
    }

    @Test
    void obtener_inexistente_retorna404() throws Exception {
        when(adminTipoContratoService.obtener(99L))
                .thenThrow(new TipoContratoNoEncontradoException(99L));

        mockMvc.perform(get("/admin/tipos-contrato/99")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TIPO_CONTRATO_NO_ENCONTRADO"));
    }

    @Test
    void crear_datosValidos_retorna201() throws Exception {
        when(adminTipoContratoService.crear(any(AdminTipoContratoCreateRequest.class)))
                .thenReturn(detalle());

        mockMvc.perform(post("/admin/tipos-contrato")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Acuerdo general",
                                  "descripcion": "Acuerdo general entre las partes.",
                                  "plantilla": "Contrato {{tipo}} {{proyecto}} {{partes}}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("ACUERDO_GENERAL"));
    }

    @Test
    void crear_nombreVacio_retorna400() throws Exception {
        mockMvc.perform(post("/admin/tipos-contrato")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "   ",
                                  "plantilla": "Contrato"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void crear_nombreDuplicado_retorna409() throws Exception {
        when(adminTipoContratoService.crear(any(AdminTipoContratoCreateRequest.class)))
                .thenThrow(new TipoContratoDuplicadoException(
                        "Ya existe un tipo de contrato con el nombre Acuerdo general."));

        mockMvc.perform(post("/admin/tipos-contrato")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Acuerdo general",
                                  "plantilla": "Contrato"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TIPO_CONTRATO_DUPLICADO"));
    }

    @Test
    void actualizar_retorna200() throws Exception {
        when(adminTipoContratoService.actualizar(
                eq(1L), any(AdminTipoContratoUpdateRequest.class)))
                .thenReturn(detalle());

        mockMvc.perform(put("/admin/tipos-contrato/1")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Acuerdo general",
                                  "descripcion": "Acuerdo general entre las partes.",
                                  "plantilla": "Contrato {{tipo}} {{proyecto}} {{partes}}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("ACUERDO_GENERAL"));
    }

    @Test
    void cambiarEstado_retorna200() throws Exception {
        when(adminTipoContratoService.cambiarEstado(
                eq(1L), any(AdminTipoContratoEstadoRequest.class)))
                .thenReturn(detalle());

        mockMvc.perform(patch("/admin/tipos-contrato/1/estado")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void eliminar_sinUso_retorna204() throws Exception {
        mockMvc.perform(delete("/admin/tipos-contrato/1")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isNoContent());

        verify(adminTipoContratoService).eliminar(1L);
    }

    @Test
    void eliminar_enUso_retorna409() throws Exception {
        doThrow(new TipoContratoEnUsoException("Tiene contratos asociados."))
                .when(adminTipoContratoService).eliminar(1L);

        mockMvc.perform(delete("/admin/tipos-contrato/1")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TIPO_CONTRATO_EN_USO"));
    }

    @Test
    void validarPlantilla_retornaResultado() throws Exception {
        when(adminTipoContratoService.validarPlantilla(any(ValidarPlantillaRequest.class)))
                .thenReturn(new ValidacionPlantillaResponse(
                        false,
                        10,
                        List.of("{{foo}}"),
                        List.of(),
                        List.of("La plantilla no incluye {{partes}}."),
                        false));

        mockMvc.perform(post("/admin/tipos-contrato/validar-plantilla")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantilla\":\"{{foo}}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(false))
                .andExpect(jsonPath("$.placeholdersDesconocidos[0]").value("{{foo}}"));
    }

    @Test
    void plantillaInvalida_retorna400ConDetalle() throws Exception {
        when(adminTipoContratoService.crear(any(AdminTipoContratoCreateRequest.class)))
                .thenThrow(new PlantillaContratoInvalidaException(
                        "La plantilla del contrato no es válida.",
                        List.of("Placeholders desconocidos: {{foo}}."),
                        new ValidacionPlantillaResponse(
                                false, 7, List.of("{{foo}}"), List.of(), List.of(), false)));

        mockMvc.perform(post("/admin/tipos-contrato")
                        .principal(autenticacionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Acuerdo raro",
                                  "plantilla": "{{foo}}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo")
                        .value("TIPO_CONTRATO_PLANTILLA_INVALIDA"))
                .andExpect(jsonPath("$.errores[0]").exists());
    }
}
