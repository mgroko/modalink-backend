package org.mgroko.backend.auditoria.controlador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaFiltroResuelto;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenEntidadResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenTipoResponse;
import org.mgroko.backend.auditoria.dto.TipoEventoAuditoriaResponse;
import org.mgroko.backend.auditoria.exception.AuditoriaFiltroInvalidoException;
import org.mgroko.backend.auditoria.servicio.AuditoriaExportService;
import org.mgroko.backend.auditoria.servicio.AuditoriaResumenService;
import org.mgroko.backend.auditoria.servicio.ListarAuditoriaService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = AdminAuditoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAuditoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarAuditoriaService listarAuditoriaService;

    @MockitoBean
    private AuditoriaResumenService auditoriaResumenService;

    @MockitoBean
    private AuditoriaExportService auditoriaExportService;

    @Test
    void listar_devuelvePaginaDeEventos() throws Exception {
        var evento = new AuditoriaEventoResponse(
                10L,
                LocalDateTime.of(2026, 11, 7, 10, 15),
                "USUARIO_DESHABILITADO",
                "Deshabilitación de usuario",
                "usuario",
                null,
                45L,
                1L,
                "Admin ModaLink",
                "modalink_api",
                "203.0.113.10",
                "USUARIO_DESHABILITADO",
                false,
                "Admin ModaLink",
                List.of(new AuditoriaDetalleResponse("estado", "ACTIVO", "DESHABILITADO")));

        when(listarAuditoriaService.listar(
                eq(LocalDate.of(2026, 11, 1)),
                eq(LocalDate.of(2026, 11, 7)),
                any(),
                isNull(),
                isNull(),
                eq(0),
                eq(20)))
                .thenReturn(new PaginaResponse<>(List.of(evento), 0, 20, 1L, 1, true, true));

        mockMvc.perform(get("/admin/auditoria")
                        .principal(new UsernamePasswordAuthenticationToken("1", null, List.of()))
                        .param("fechaDesde", "2026-11-01")
                        .param("fechaHasta", "2026-11-07")
                        .param("tipos", "USUARIO_DESHABILITADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].tipoEvento")
                        .value("USUARIO_DESHABILITADO"))
                .andExpect(jsonPath("$.contenido[0].idUsuario").value(1))
                .andExpect(jsonPath("$.contenido[0].detalle[0].campo").value("estado"));
    }

    @Test
    void listarSinFechas_devuelve400ConCodigo() throws Exception {
        when(listarAuditoriaService.listar(
                isNull(), isNull(), any(), isNull(), isNull(), anyInt(), anyInt()))
                .thenThrow(new AuditoriaFiltroInvalidoException(
                        "FECHAS_OBLIGATORIAS",
                        "Las fechas desde y hasta son obligatorias."));

        mockMvc.perform(get("/admin/auditoria")
                        .principal(new UsernamePasswordAuthenticationToken("1", null, List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHAS_OBLIGATORIAS"));
    }

    @Test
    void controlador_estaRestringidoAAdministrador() {
        PreAuthorize annotation = AdminAuditoriaController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMINISTRADOR')");
    }

    @Test
    void resumen_devuelveTotales() throws Exception {
        when(auditoriaResumenService.obtenerResumen(
                eq(LocalDate.of(2026, 11, 1)),
                eq(LocalDate.of(2026, 11, 7)),
                any(),
                isNull(),
                isNull()))
                .thenReturn(new AuditoriaResumenResponse(
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 7),
                        List.of(),
                        2L,
                        List.of(new AuditoriaResumenTipoResponse(
                                "USUARIO_ALTA", "Alta de usuario", 2L)),
                        List.of(new AuditoriaResumenEntidadResponse("usuario", 2L))));

        mockMvc.perform(get("/admin/auditoria/resumen")
                        .principal(new UsernamePasswordAuthenticationToken("1", null, List.of()))
                        .param("fechaDesde", "2026-11-01")
                        .param("fechaHasta", "2026-11-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRegistros").value(2))
                .andExpect(jsonPath("$.porTipoEvento[0].codigo").value("USUARIO_ALTA"));
    }

    @Test
    void exportarCsv_configuraHeadersYDelegaEnServicio() throws Exception {
        AuditoriaFiltroResuelto filtro = new AuditoriaFiltroResuelto(
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 7, 23, 59, 59),
                null,
                null,
                null,
                Map.of());

        when(auditoriaExportService.validarExportacion(
                eq(LocalDate.of(2026, 11, 1)),
                eq(LocalDate.of(2026, 11, 7)),
                any(),
                isNull(),
                isNull()))
                .thenReturn(filtro);

        doAnswer(invocation -> {
            OutputStream salida = invocation.getArgument(1);
            salida.write("contenido".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(auditoriaExportService)
                .exportarCsv(eq(filtro), any(OutputStream.class));

        AdminAuditoriaController controller = new AdminAuditoriaController(
                listarAuditoriaService, auditoriaResumenService, auditoriaExportService);

        var response = controller.exportar(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 7),
                null,
                null,
                null,
                "CSV",
                new UsernamePasswordAuthenticationToken("1", null, List.of()));

        assertThat(response.getHeaders().getContentType().toString())
                .contains("text/csv");
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("auditoria_2026-11-01_2026-11-07.csv");
        assertThat(response.getBody())
                .isEqualTo("contenido".getBytes(StandardCharsets.UTF_8));

        verify(auditoriaExportService).exportarCsv(eq(filtro), any(OutputStream.class));
    }

    @Test
    void listarTipos_devuelveCatalogo() throws Exception {
        when(listarAuditoriaService.listarTipos())
                .thenReturn(List.of(new TipoEventoAuditoriaResponse(
                        "USUARIO_ALTA", "Alta de usuario", "Registro", "USUARIOS")));

        mockMvc.perform(get("/admin/auditoria/tipos")
                        .principal(new UsernamePasswordAuthenticationToken("1", null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("USUARIO_ALTA"))
                .andExpect(jsonPath("$[0].modulo").value("USUARIOS"));
    }
}
