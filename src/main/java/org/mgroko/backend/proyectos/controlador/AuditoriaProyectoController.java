package org.mgroko.backend.proyectos.controlador;

import java.time.LocalDate;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.AuditoriaBajaResponse;
import org.mgroko.backend.proyectos.servicio.ListarAuditoriaBajasProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('USUARIO')")
public class AuditoriaProyectoController {

    private final ListarAuditoriaBajasProyectoService listarAuditoriaBajasProyectoService;

    public AuditoriaProyectoController(
            ListarAuditoriaBajasProyectoService listarAuditoriaBajasProyectoService) {
        this.listarAuditoriaBajasProyectoService = listarAuditoriaBajasProyectoService;
    }

    @GetMapping("/proyectos/{idProyecto}/auditoria/bajas")
    public ResponseEntity<PaginaResponse<AuditoriaBajaResponse>> listarBajas(
            @PathVariable Long idProyecto,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long idActividad,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean todos,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        return ResponseEntity.ok(listarAuditoriaBajasProyectoService.listar(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                estado,
                idActividad,
                fechaDesde,
                fechaHasta,
                page,
                size,
                todos));
    }
}
