package org.mgroko.backend.auditoria.controlador;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaFiltroResuelto;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenResponse;
import org.mgroko.backend.auditoria.dto.TipoEventoAuditoriaResponse;
import org.mgroko.backend.auditoria.enums.FormatoExportacion;
import org.mgroko.backend.auditoria.servicio.AuditoriaExportService;
import org.mgroko.backend.auditoria.servicio.AuditoriaResumenService;
import org.mgroko.backend.auditoria.servicio.ListarAuditoriaService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-67: informe de auditoría para administradores.
 */
@RestController
@RequestMapping("/admin/auditoria")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminAuditoriaController {

    private static final MediaType CSV = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final ListarAuditoriaService listarAuditoriaService;
    private final AuditoriaResumenService auditoriaResumenService;
    private final AuditoriaExportService auditoriaExportService;

    public AdminAuditoriaController(
            ListarAuditoriaService listarAuditoriaService,
            AuditoriaResumenService auditoriaResumenService,
            AuditoriaExportService auditoriaExportService) {
        this.listarAuditoriaService = listarAuditoriaService;
        this.auditoriaResumenService = auditoriaResumenService;
        this.auditoriaExportService = auditoriaExportService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<AuditoriaEventoResponse>> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) List<String> tipos,
            @RequestParam(required = false) String operador,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(listarAuditoriaService.listar(
                fechaDesde, fechaHasta, tipos, operador, texto, page, size));
    }

    @GetMapping("/tipos")
    public ResponseEntity<List<TipoEventoAuditoriaResponse>> listarTipos() {
        return ResponseEntity.ok(listarAuditoriaService.listarTipos());
    }

    @GetMapping("/resumen")
    public ResponseEntity<AuditoriaResumenResponse> resumen(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) List<String> tipos,
            @RequestParam(required = false) String operador,
            @RequestParam(required = false) String texto) {

        return ResponseEntity.ok(auditoriaResumenService.obtenerResumen(
                fechaDesde, fechaHasta, tipos, operador, texto));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) List<String> tipos,
            @RequestParam(required = false) String operador,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String formato,
            Authentication authentication) {

        FormatoExportacion formatoExportacion = FormatoExportacion.desde(formato);
        AuditoriaFiltroResuelto filtro =
                auditoriaExportService.validarExportacion(
                        fechaDesde, fechaHasta, tipos, operador, texto);

        String extension = formatoExportacion == FormatoExportacion.CSV ? "csv" : "pdf";
        String filename = "auditoria_" + fechaDesde + "_" + fechaHasta + "." + extension;

        // Se genera en memoria para que cualquier error se resuelva como una
        // respuesta JSON normal, antes de enviar headers de descarga. El tope
        // de registros (AUDITORIA_EXPORT_MAX_REGISTROS) acota la memoria.
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        if (formatoExportacion == FormatoExportacion.CSV) {
            auditoriaExportService.exportarCsv(filtro, salida);
        } else {
            auditoriaExportService.exportarPdf(filtro, idUsuario(authentication), salida);
        }

        MediaType contentType = formatoExportacion == FormatoExportacion.CSV
                ? CSV
                : MediaType.APPLICATION_PDF;

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(salida.toByteArray());
    }

    private Long idUsuario(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String valor && valor.matches("^[0-9]+$")) {
            return Long.valueOf(valor);
        }
        return null;
    }
}
