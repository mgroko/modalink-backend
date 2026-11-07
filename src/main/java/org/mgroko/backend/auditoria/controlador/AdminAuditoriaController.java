package org.mgroko.backend.auditoria.controlador;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.auditoria.dto.TipoEventoAuditoriaResponse;
import org.mgroko.backend.auditoria.servicio.ListarAuditoriaService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final ListarAuditoriaService listarAuditoriaService;

    public AdminAuditoriaController(ListarAuditoriaService listarAuditoriaService) {
        this.listarAuditoriaService = listarAuditoriaService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<AuditoriaEventoResponse>> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) List<String> tipos,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(listarAuditoriaService.listar(
                fechaDesde, fechaHasta, tipos, page, size));
    }

    @GetMapping("/tipos")
    public ResponseEntity<List<TipoEventoAuditoriaResponse>> listarTipos() {
        return ResponseEntity.ok(listarAuditoriaService.listarTipos());
    }
}
