package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.mgroko.backend.admin.dto.InconsistenciaCronogramaResponse;
import org.mgroko.backend.admin.dto.RepararCronogramaRequest;
import org.mgroko.backend.admin.dto.ReparacionCronogramaResponse;
import org.mgroko.backend.admin.servicio.RepararCronogramaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mantenimiento administrativo del cronograma. No se ejecuta automáticamente.
 */
@RestController
@RequestMapping("/admin/cronograma")
public class AdminCronogramaController {

    private final RepararCronogramaService repararCronogramaService;

    public AdminCronogramaController(
            RepararCronogramaService repararCronogramaService) {
        this.repararCronogramaService = repararCronogramaService;
    }

    @GetMapping("/inconsistencias")
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<List<InconsistenciaCronogramaResponse>> inconsistencias(
            @RequestParam(required = false) Long idProyecto,
            @RequestParam(required = false) Long idActividad,
            @RequestParam(required = false) Integer limite) {

        return ResponseEntity.ok(repararCronogramaService.detectar(
                idProyecto, idActividad, limite));
    }

    @PostMapping("/reparar")
    @PreAuthorize("hasAuthority('ADMINISTRAR_CONFIGURACION')")
    public ResponseEntity<ReparacionCronogramaResponse> reparar(
            @RequestBody RepararCronogramaRequest request) {

        return ResponseEntity.ok(repararCronogramaService.reparar(request));
    }
}
