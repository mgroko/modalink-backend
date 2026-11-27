package org.mgroko.backend.proyectos.controlador;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.proyectos.servicio.ModificarProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-36: alta y baja individual de requerimientos generales de un proyecto.
 *
 * <p>El {@code PUT /proyectos/{idProyecto}} solo actualiza o elimina
 * requerimientos existentes, por lo que toda alta se hace por este controlador
 * y siempre obtiene un id nuevo.</p>
 */
@RestController
@RequestMapping("/proyectos/{idProyecto}/requerimientos-generales")
@PreAuthorize("hasRole('USUARIO')")
public class RequerimientoGralProyectoController {

    private final ModificarProyectoService modificarProyectoService;

    public RequerimientoGralProyectoController(
            ModificarProyectoService modificarProyectoService) {
        this.modificarProyectoService = modificarProyectoService;
    }

    @PostMapping
    public ResponseEntity<RequerimientoGralResponse> crear(
            @PathVariable Long idProyecto,
            @Valid @RequestBody CrearRequerimientoGralRequest request,
            Authentication authentication) {

        RequerimientoGralResponse response = modificarProyectoService.crearRequerimiento(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{idRequerimientoGral}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long idProyecto,
            @PathVariable Long idRequerimientoGral,
            Authentication authentication) {

        modificarProyectoService.eliminarRequerimiento(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                idRequerimientoGral);

        return ResponseEntity.noContent().build();
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }

    private Long idPerfilActivo(Authentication authentication) {
        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }
        return idPerfilActivo;
    }
}
