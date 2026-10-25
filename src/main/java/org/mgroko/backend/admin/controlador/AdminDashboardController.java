package org.mgroko.backend.admin.controlador;

import org.mgroko.backend.admin.dto.AdminDashboardResumen;
import org.mgroko.backend.admin.dto.AdminProyectoList;
import org.mgroko.backend.admin.servicio.AdminDashboardService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/admin")
@Validated
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard/resumen")
    @PreAuthorize("hasAuthority('VER_USUARIOS')")
    public AdminDashboardResumen obtenerResumen(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        return adminDashboardService.obtenerResumen(size);
    }

    @GetMapping("/proyectos")
    @PreAuthorize("hasAuthority('VER_USUARIOS')")
    public PaginaResponse<AdminProyectoList> listarProyectos(
            @RequestParam(required = false) EstadoProyecto[] estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return adminDashboardService.listarProyectos(estado, page, size);
    }
}