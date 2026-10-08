package org.mgroko.backend.admin.controlador;

import org.mgroko.backend.admin.dto.ConfiguracionSchedulerResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerRequest;
import org.mgroko.backend.admin.dto.EjecutarSchedulerResponse;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/configuracion/schedulers/deshabilitacion")
public class AdminConfiguracionController {

    private final ConfiguracionSistemaService configuracionSistemaService;

    public AdminConfiguracionController(ConfiguracionSistemaService configuracionSistemaService) {
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<ConfiguracionSchedulerResponse> obtenerConfiguracion() {
        return ResponseEntity.ok(configuracionSistemaService.obtenerConfiguracionDeshabilitacion());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<ConfiguracionSchedulerResponse> actualizarConfiguracion(
            @Valid @RequestBody ConfigurarSchedulerRequest request) {
        return ResponseEntity.ok(configuracionSistemaService.actualizarConfiguracionDeshabilitacion(request));
    }

    @PostMapping("/ejecutar-ahora")
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<EjecutarSchedulerResponse> ejecutarAhora() {
        return ResponseEntity.ok(configuracionSistemaService.ejecutarDeshabilitacionManual());
    }
}
