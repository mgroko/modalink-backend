package org.mgroko.backend.usuario.controlador;

import org.mgroko.backend.usuario.dto.DatosPersonalesRequest;
import org.mgroko.backend.usuario.dto.DatosPersonalesResponse;
import org.mgroko.backend.usuario.servicio.DatosPersonalesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuario")
public class DatosPersonalesController {

    private final DatosPersonalesService datosPersonalesService;

    public DatosPersonalesController(DatosPersonalesService datosPersonalesService) {
        this.datosPersonalesService = datosPersonalesService;
    }

    @PutMapping("/datos-personales")
    public ResponseEntity<DatosPersonalesResponse> actualizarDatosPersonales(
            @Valid @RequestBody DatosPersonalesRequest request,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        DatosPersonalesResponse response = datosPersonalesService.actualizar(idUsuario, request);
        return ResponseEntity.ok(response);
    }
}
