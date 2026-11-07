package org.mgroko.backend.contratos.controlador;

import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.GenerarContratoRequest;
import org.mgroko.backend.contratos.dto.OpcionesGeneracionContratoResponse;
import org.mgroko.backend.contratos.dto.PrevisualizacionContratoResponse;
import org.mgroko.backend.contratos.servicio.GenerarContratoService;
import org.mgroko.backend.contratos.servicio.ObtenerOpcionesContratoService;
import org.mgroko.backend.contratos.servicio.PrevisualizarContratoService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-39: generar contrato.
 *
 * <p>Los endpoints cuelgan de /proyectos/{idProyecto}/contratos, por lo que
 * quedan cubiertos por la regla existente de SecurityConfig para
 * /proyectos/** con rol USUARIO.</p>
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class ContratoController {

    private final ObtenerOpcionesContratoService opcionesService;
    private final PrevisualizarContratoService previsualizarService;
    private final GenerarContratoService generarService;

    public ContratoController(
            ObtenerOpcionesContratoService opcionesService,
            PrevisualizarContratoService previsualizarService,
            GenerarContratoService generarService) {
        this.opcionesService = opcionesService;
        this.previsualizarService = previsualizarService;
        this.generarService = generarService;
    }

    @GetMapping("/proyectos/{idProyecto}/contratos/opciones")
    public ResponseEntity<OpcionesGeneracionContratoResponse> opciones(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        return ResponseEntity.ok(opcionesService.obtener(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto));
    }

    @PostMapping("/proyectos/{idProyecto}/contratos/previsualizar")
    public ResponseEntity<PrevisualizacionContratoResponse> previsualizar(
            @PathVariable Long idProyecto,
            @Valid @RequestBody GenerarContratoRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(previsualizarService.previsualizar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                request));
    }

    @PostMapping("/proyectos/{idProyecto}/contratos")
    public ResponseEntity<ContratoGeneradoResponse> generar(
            @PathVariable Long idProyecto,
            @Valid @RequestBody GenerarContratoRequest request,
            Authentication authentication) {

        ContratoGeneradoResponse response = generarService.generar(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
