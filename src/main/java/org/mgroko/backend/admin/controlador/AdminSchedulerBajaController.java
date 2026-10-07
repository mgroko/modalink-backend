package org.mgroko.backend.admin.controlador;

import org.mgroko.backend.admin.dto.ConfiguracionSchedulerBajaResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerBajaRequest;
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
@RequestMapping("/admin/configuracion/schedulers/baja")
public class AdminSchedulerBajaController {

    private final ConfiguracionSistemaService configuracionSistemaService;

    public AdminSchedulerBajaController(ConfiguracionSistemaService configuracionSistemaService) {
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<ConfiguracionSchedulerBajaResponse> obtenerConfiguracion() {
        return ResponseEntity.ok(configuracionSistemaService.obtenerConfiguracionBaja());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<ConfiguracionSchedulerBajaResponse> actualizarConfiguracion(
            @Valid @RequestBody ConfigurarSchedulerBajaRequest request) {
        return ResponseEntity.ok(configuracionSistemaService.actualizarConfiguracionBaja(request));
    }

    @PostMapping("/ejecutar-ahora")
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<EjecutarSchedulerResponse> ejecutarAhora() {
        return ResponseEntity.ok(configuracionSistemaService.ejecutarBajaManual());
    }
}
