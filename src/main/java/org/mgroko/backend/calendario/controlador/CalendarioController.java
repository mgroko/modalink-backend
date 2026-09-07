package org.mgroko.backend.calendario.controlador;

import org.mgroko.backend.calendario.dto.BloqueoResponse;
import org.mgroko.backend.calendario.dto.CalendarioResponse;
import org.mgroko.backend.calendario.dto.ConfigJornadaRequest;
import org.mgroko.backend.calendario.dto.ConfigJornadaResponse;
import org.mgroko.backend.calendario.dto.MarcarNoDisponibleRequest;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/calendario")
public class CalendarioController {

    private final CalendarioService calendarioService;

    public CalendarioController(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    @GetMapping
    public ResponseEntity<CalendarioResponse> obtener(Authentication authentication) {
        Long idUsuario = idUsuario(authentication);
        return ResponseEntity.ok(calendarioService.obtener(idUsuario));
    }

    @PutMapping("/jornada")
    public ResponseEntity<ConfigJornadaResponse> configurarJornada(
            @Valid @RequestBody ConfigJornadaRequest request,
            Authentication authentication) {
        Long idUsuario = idUsuario(authentication);
        return ResponseEntity.ok(calendarioService.configurarJornada(idUsuario, request));
    }

    @PostMapping("/bloqueos")
    public ResponseEntity<BloqueoResponse> marcarNoDisponible(
            @Valid @RequestBody MarcarNoDisponibleRequest request,
            Authentication authentication) {
        Long idUsuario = idUsuario(authentication);
        return ResponseEntity.ok(calendarioService.marcarNoDisponible(idUsuario, request));
    }

    @DeleteMapping("/bloqueos/{idBloqueo}")
    public ResponseEntity<Void> marcarDisponible(
            @PathVariable Long idBloqueo,
            Authentication authentication) {
        Long idUsuario = idUsuario(authentication);
        calendarioService.marcarDisponible(idUsuario, idBloqueo);
        return ResponseEntity.noContent().build();
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }
}