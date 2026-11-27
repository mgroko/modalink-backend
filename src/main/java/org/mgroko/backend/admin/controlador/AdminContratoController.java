package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminContratoDetalleResponse;
import org.mgroko.backend.admin.dto.AdminContratoResumenResponse;
import org.mgroko.backend.admin.dto.BuscarContratosAdminFiltro;
import org.mgroko.backend.admin.servicio.AdminContratoService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.ContratoEventoResponse;
import org.mgroko.backend.contratos.servicio.ExportarContratoPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Consulta administrativa de contratos. Solo lectura.
 */
@RestController
@RequestMapping("/admin/contratos")
@PreAuthorize("hasAuthority('VER_CONTRATOS')")
@Validated
public class AdminContratoController {

    private final AdminContratoService adminContratoService;
    private final ExportarContratoPdfService exportarContratoPdfService;

    public AdminContratoController(
            AdminContratoService adminContratoService,
            ExportarContratoPdfService exportarContratoPdfService) {
        this.adminContratoService = adminContratoService;
        this.exportarContratoPdfService = exportarContratoPdfService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<AdminContratoResumenResponse>> listar(
            @ModelAttribute BuscarContratosAdminFiltro filtro,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "false") boolean todos) {

        return ResponseEntity.ok(adminContratoService.buscar(filtro, page, size, todos));
    }

    @GetMapping("/{idContrato}")
    public ResponseEntity<AdminContratoDetalleResponse> detalle(
            @PathVariable Long idContrato) {

        return ResponseEntity.ok(adminContratoService.obtenerDetalle(idContrato));
    }

    @GetMapping("/{idContrato}/eventos")
    public ResponseEntity<List<ContratoEventoResponse>> eventos(
            @PathVariable Long idContrato) {

        return ResponseEntity.ok(adminContratoService.listarEventos(idContrato));
    }

    @GetMapping(
            value = "/{idContrato}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportarPdf(
            @PathVariable Long idContrato,
            Authentication authentication) {

        byte[] contenido = exportarContratoPdfService.exportarAdministrativo(
                idUsuario(authentication), idContrato);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"contrato-"
                                + idContrato + ".pdf\"")
                .body(contenido);
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
