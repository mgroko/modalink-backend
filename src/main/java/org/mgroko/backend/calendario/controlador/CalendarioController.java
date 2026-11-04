package org.mgroko.backend.calendario.controlador;

import java.util.Objects;

import org.mgroko.backend.calendario.dto.BloqueoResponse;
import org.mgroko.backend.calendario.dto.CalendarioResponse;
import org.mgroko.backend.calendario.dto.ConfigJornadaRequest;
import org.mgroko.backend.calendario.dto.ConfigJornadaResponse;
import org.mgroko.backend.calendario.dto.MarcarNoDisponibleRequest;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.proyectos.dto.ResumenReprogramacionResponse;
import org.mgroko.backend.proyectos.servicio.JornadaReprogramacionService;
import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;
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
    private final JornadaReprogramacionService jornadaReprogramacionService;

    public CalendarioController(
            CalendarioService calendarioService,
            JornadaReprogramacionService jornadaReprogramacionService) {
        this.calendarioService = calendarioService;
        this.jornadaReprogramacionService = jornadaReprogramacionService;
    }

    @GetMapping
    public ResponseEntity<CalendarioResponse> obtener(Authentication authentication) {
        Long idUsuario = idUsuario(authentication);
        return ResponseEntity.ok(calendarioService.obtener(idUsuario));
    }

    @GetMapping("/perfil/{id}")
    public ResponseEntity<CalendarioResponse> obtenerPorPerfil(@PathVariable("id") Long id) {
        return ResponseEntity.ok(calendarioService.obtenerPublico(id));
    }

    @PutMapping("/jornada")
    public ResponseEntity<ConfigJornadaResponse> configurarJornada(
            @Valid @RequestBody ConfigJornadaRequest request,
            Authentication authentication) {
        Long idUsuario = idUsuario(authentication);

        ConfigJornadaResponse anterior = calendarioService.obtenerJornada(idUsuario);
        ConfigJornadaResponse actual = calendarioService.configurarJornada(idUsuario, request);

        MotivoReprogramacion motivo = Objects.equals(
                anterior.margenActividadMinutos(),
                actual.margenActividadMinutos())
                ? MotivoReprogramacion.CAMBIO_JORNADA
                : MotivoReprogramacion.CAMBIO_MARGEN;
        ResumenReprogramacionResponse reprogramacion = anterior.equals(actual)
                ? ResumenReprogramacionResponse.vacio()
                : jornadaReprogramacionService.reprogramarAfectados(
                        idUsuario, motivo);

        return ResponseEntity.ok(new ConfigJornadaResponse(
                actual.margenActividadMinutos(),
                actual.dias(),
                reprogramacion));
    }

    @PostMapping("/bloqueos")
    public ResponseEntity<BloqueoResponse> marcarNoDisponible(
            @Valid @RequestBody MarcarNoDisponibleRequest request,
            Authentication authentication) {
        Long idUsuario = idUsuario(authentication);
        BloqueoResponse bloqueo = calendarioService.marcarNoDisponible(idUsuario, request);
        jornadaReprogramacionService.reprogramarAfectados(
                idUsuario, MotivoReprogramacion.CAMBIO_BLOQUEO);
        return ResponseEntity.ok(bloqueo);
    }

    @DeleteMapping("/bloqueos/{idBloqueo}")
    public ResponseEntity<Void> marcarDisponible(
            @PathVariable Long idBloqueo,
            Authentication authentication) {
        Long idUsuario = idUsuario(authentication);
        calendarioService.marcarDisponible(idUsuario, idBloqueo);
        jornadaReprogramacionService.reprogramarAfectados(
                idUsuario, MotivoReprogramacion.CAMBIO_BLOQUEO);
        return ResponseEntity.noContent().build();
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }
}