package org.mgroko.backend.admin.controlador;

import org.mgroko.backend.admin.dto.ConfiguracionSchedulerLimpiezaImagenesResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerLimpiezaImagenesRequest;
import org.mgroko.backend.admin.dto.EjecutarLimpiezaImagenesResponse;
import org.mgroko.backend.admin.servicio.AdminLimpiezaImagenesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/configuracion/schedulers/limpieza-imagenes")
public class AdminSchedulerLimpiezaImagenesController {

    private final AdminLimpiezaImagenesService adminLimpiezaImagenesService;

    public AdminSchedulerLimpiezaImagenesController(
            AdminLimpiezaImagenesService adminLimpiezaImagenesService) {
        this.adminLimpiezaImagenesService = adminLimpiezaImagenesService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<ConfiguracionSchedulerLimpiezaImagenesResponse> obtenerConfiguracion() {
        return ResponseEntity.ok(adminLimpiezaImagenesService.obtenerConfiguracion());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<ConfiguracionSchedulerLimpiezaImagenesResponse> actualizarConfiguracion(
            @Valid @RequestBody ConfigurarSchedulerLimpiezaImagenesRequest request) {
        return ResponseEntity.ok(adminLimpiezaImagenesService.actualizarConfiguracion(request));
    }

    @PostMapping("/ejecutar-ahora")
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<EjecutarLimpiezaImagenesResponse> ejecutarAhora() {
        return ResponseEntity.ok(adminLimpiezaImagenesService.ejecutarAhora());
    }
}
