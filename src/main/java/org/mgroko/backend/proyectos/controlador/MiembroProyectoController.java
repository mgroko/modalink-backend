package org.mgroko.backend.proyectos.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.DarseDeBajaProyectoRequest;
import org.mgroko.backend.proyectos.dto.EliminarIntegranteRequest;
import org.mgroko.backend.proyectos.dto.MiembroProyectoResponse;
import org.mgroko.backend.proyectos.servicio.DarseDeBajaProyectoService;
import org.mgroko.backend.proyectos.servicio.EliminarIntegranteService;
import org.mgroko.backend.proyectos.servicio.ListarMiembrosProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-70: miembros activos del proyecto para seleccionar el destinatario de una
 * asignación.
 *
 * UC-38: el Director elimina lógicamente a un integrante del proyecto.
 *
 * UC-40: un integrante se da de baja voluntariamente del proyecto.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class MiembroProyectoController {

    private final ListarMiembrosProyectoService listarMiembrosProyectoService;
    private final EliminarIntegranteService eliminarIntegranteService;
    private final DarseDeBajaProyectoService darseDeBajaProyectoService;

    public MiembroProyectoController(
            ListarMiembrosProyectoService listarMiembrosProyectoService,
            EliminarIntegranteService eliminarIntegranteService,
            DarseDeBajaProyectoService darseDeBajaProyectoService) {
        this.listarMiembrosProyectoService = listarMiembrosProyectoService;
        this.eliminarIntegranteService = eliminarIntegranteService;
        this.darseDeBajaProyectoService = darseDeBajaProyectoService;
    }

    @GetMapping("/proyectos/{idProyecto}/miembros")
    public ResponseEntity<PaginaResponse<MiembroProyectoResponse>> listar(
            @PathVariable Long idProyecto,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Long idProfesion,
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

        return ResponseEntity.ok(listarMiembrosProyectoService.listar(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                nombre,
                rol,
                idProfesion,
                page,
                size,
                todos));
    }

    @DeleteMapping("/proyectos/{idProyecto}/miembros/{idMiembro}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long idProyecto,
            @PathVariable Long idMiembro,
            @Valid @RequestBody EliminarIntegranteRequest request,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        eliminarIntegranteService.eliminar(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                idMiembro,
                request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/proyectos/{idProyecto}/baja")
    public ResponseEntity<Void> darseDeBaja(
            @PathVariable Long idProyecto,
            @Valid @RequestBody DarseDeBajaProyectoRequest request,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        darseDeBajaProyectoService.darseDeBaja(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                request);

        return ResponseEntity.noContent().build();
    }
}
