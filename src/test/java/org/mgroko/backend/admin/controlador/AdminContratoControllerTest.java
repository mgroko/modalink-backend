package org.mgroko.backend.admin.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminContratoDetalleResponse;
import org.mgroko.backend.admin.dto.AdminContratoResumenResponse;
import org.mgroko.backend.admin.dto.BuscarContratosAdminFiltro;
import org.mgroko.backend.admin.servicio.AdminContratoService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.ContratoEventoResponse;
import org.mgroko.backend.contratos.exception.ContratoNoEncontradoException;
import org.mgroko.backend.contratos.servicio.ExportarContratoPdfService;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = AdminContratoController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminContratoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminContratoService adminContratoService;

    @MockitoBean
    private ExportarContratoPdfService exportarContratoPdfService;

    @Test
    void listar_devuelveContratosSinExcluirEstados() throws Exception {
        when(adminContratoService.buscar(
                any(BuscarContratosAdminFiltro.class), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(new PaginaResponse<>(List.of(resumen()), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/admin/contratos")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("ELIMINADO"))
                .andExpect(jsonPath("$.contenido[0].nombreProyecto").value("Proyecto Luna"));
    }

    @Test
    void listar_estadoUnico_seEnlazaAlFiltro() throws Exception {
        when(adminContratoService.buscar(
                any(BuscarContratosAdminFiltro.class), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/admin/contratos")
                        .param("estados", "BORRADOR")
                        .principal(autenticacion()))
                .andExpect(status().isOk());

        ArgumentCaptor<BuscarContratosAdminFiltro> captor =
                ArgumentCaptor.forClass(BuscarContratosAdminFiltro.class);
        verify(adminContratoService).buscar(captor.capture(), eq(0), eq(20), eq(false));
        assertEquals(List.of(EstadoContrato.BORRADOR), captor.getValue().estados());
    }

    @Test
    void detalle_incluyeTextoCompleto() throws Exception {
        when(adminContratoService.obtenerDetalle(5L)).thenReturn(detalle());

        mockMvc.perform(get("/admin/contratos/5")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idContrato").value(5))
                .andExpect(jsonPath("$.texto").value("Texto legal completo"))
                .andExpect(jsonPath("$.partes[0].estadoFirma").value("FIRMADO"));
    }

    @Test
    void eventos_devuelveHistorial() throws Exception {
        when(adminContratoService.listarEventos(5L)).thenReturn(List.of(
                new ContratoEventoResponse(
                        1L, "CREADO", LocalDateTime.now(), 1L, 7L,
                        "Luna Pérez", 1L, "Admin", 1, null)));

        mockMvc.perform(get("/admin/contratos/5/eventos")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoEvento").value("CREADO"))
                .andExpect(jsonPath("$[0].nombreParte").value("Luna Pérez"));
    }

    @Test
    void detalle_contratoInexistente_devuelve404() throws Exception {
        when(adminContratoService.obtenerDetalle(999L))
                .thenThrow(new ContratoNoEncontradoException(999L));

        mockMvc.perform(get("/admin/contratos/999")
                        .principal(autenticacion()))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportarPdf_devuelveArchivoAdjunto() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(exportarContratoPdfService.exportarAdministrativo(1L, 5L))
                .thenReturn(pdf);

        mockMvc.perform(get("/admin/contratos/5/pdf")
                        .principal(autenticacion()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"contrato-5.pdf\""))
                .andExpect(content().bytes(pdf));
    }

    private UsernamePasswordAuthenticationToken autenticacion() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private AdminContratoResumenResponse resumen() {
        return new AdminContratoResumenResponse(
                5L, "Contrato de prueba", "ELIMINADO", 2,
                10L, "Proyecto Luna", "CONFIRMADO",
                1L, "FOTOGRAFIA", "Contrato de fotografía",
                LocalDateTime.now(), LocalDateTime.now(), null, LocalDateTime.now(),
                "Luna Pérez", 2, 3);
    }

    private AdminContratoDetalleResponse detalle() {
        return new AdminContratoDetalleResponse(
                5L,
                "Contrato de prueba",
                "Descripción",
                "ACTIVO",
                1,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                "Texto legal completo",
                new AdminContratoDetalleResponse.ProyectoResponse(
                        10L, "Proyecto Luna", "Descripción", "CONFIRMADO"),
                new AdminContratoDetalleResponse.TipoAcuerdoResponse(
                        1L, "FOTOGRAFIA", "Contrato de fotografía", null),
                new AdminContratoDetalleResponse.EmisorResponse(
                        1L, 7L, "Luna", "Luna Pérez", "12345678"),
                List.of(new AdminContratoDetalleResponse.ParteResponse(
                        1L, 7L, "Luna", "Luna Pérez", "12345678",
                        "DIRECTOR", "FIRMADO", LocalDateTime.now())),
                1,
                1);
    }
}
