package org.mgroko.backend.admin.controlador;

import org.mgroko.backend.admin.dto.AdminProyectoDetalleResponse;
import org.mgroko.backend.admin.dto.AdminProyectoList;
import org.mgroko.backend.admin.servicio.AdminProyectoService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Consulta administrativa de proyectos. Solo lectura.
 */
@RestController
@RequestMapping("/admin/proyectos")
@PreAuthorize("hasAuthority('VER_PROYECTOS')")
@Validated
public class AdminProyectoController {

    private final AdminProyectoService adminProyectoService;

    public AdminProyectoController(AdminProyectoService adminProyectoService) {
        this.adminProyectoService = adminProyectoService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<AdminProyectoList>> listar(
            @RequestParam(required = false) EstadoProyecto[] estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return ResponseEntity.ok(adminProyectoService.listarProyectos(estado, page, size));
    }

    @GetMapping("/{idProyecto}")
    public ResponseEntity<AdminProyectoDetalleResponse> detalle(
            @PathVariable Long idProyecto) {

        return ResponseEntity.ok(adminProyectoService.obtenerDetalle(idProyecto));
    }
}
