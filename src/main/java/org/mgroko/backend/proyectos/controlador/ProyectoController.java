package org.mgroko.backend.proyectos.controlador;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class ProyectoController {

    private final CrearProyectoService crearProyectoService;

    public ProyectoController(CrearProyectoService crearProyectoService) {
        this.crearProyectoService = crearProyectoService;
    }

    // UC-24: Crear proyecto
    @PostMapping("/proyectos")
    public ResponseEntity<ProyectoResponse> crear(
            @Valid @RequestBody CrearProyectoRequest request,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        ProyectoResponse response = crearProyectoService.crear(idUsuario, idPerfilActivo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
