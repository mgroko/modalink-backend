package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.EditarPerfilRequest;
import org.mgroko.backend.perfiles.dto.EliminarPerfilResponse;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.mapper.PerfilMapper;
import org.mgroko.backend.perfiles.servicio.ActivarPerfilService;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.perfiles.servicio.EditarPerfilService;
import org.mgroko.backend.perfiles.servicio.EliminarPerfilService;
import org.mgroko.backend.perfiles.servicio.ReactivarPerfilService;
import org.mgroko.backend.perfiles.servicio.UsuarioPerfilService;
import org.mgroko.backend.security.JwtCookieFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class PerfilController {

    private final CrearPerfilService crearPerfilService;
    private final UsuarioPerfilService usuarioPerfilService;
    private final EditarPerfilService editarPerfilService;
    private final EliminarPerfilService eliminarPerfilService;
    private final ReactivarPerfilService reactivarPerfilService;
    private final ActivarPerfilService activarPerfilService;
    private final JwtCookieFactory jwtCookieFactory;

    public PerfilController(CrearPerfilService crearPerfilService,
            UsuarioPerfilService usuarioPerfilService,
            EditarPerfilService editarPerfilService,
            EliminarPerfilService eliminarPerfilService,
            ReactivarPerfilService reactivarPerfilService,
            ActivarPerfilService activarPerfilService,
            JwtCookieFactory jwtCookieFactory) {
        this.crearPerfilService = crearPerfilService;
        this.usuarioPerfilService = usuarioPerfilService;
        this.editarPerfilService = editarPerfilService;
        this.eliminarPerfilService = eliminarPerfilService;
        this.reactivarPerfilService = reactivarPerfilService;
        this.activarPerfilService = activarPerfilService;
        this.jwtCookieFactory = jwtCookieFactory;
    }

    @PostMapping("/perfiles")
    public ResponseEntity<PerfilResponse> crear(
            @Valid @RequestBody CrearPerfilRequest request,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        PerfilResponse response = crearPerfilService.crear(idUsuario, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/usuarios/me/perfiles")
    public ResponseEntity<List<PerfilResponse>> listarPerfilesPropios(Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        List<PerfilResponse> response = usuarioPerfilService.listarPerfilesPropios(idUsuario);
        return ResponseEntity.ok(response);
    }

    // UC-11 - Paso 2: recuperar los datos actuales del perfil
    @GetMapping("/perfiles/{idPerfil}")
    public ResponseEntity<PerfilResponse> obtener(
            @PathVariable Long idPerfil,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        PerfilResponse response = usuarioPerfilService.obtenerPerfilPropio(idUsuario, idPerfil);
        return ResponseEntity.ok(response);
    }

    // UC-11 - Editar perfil
    @PutMapping("/perfiles/{idPerfil}")
    public ResponseEntity<PerfilResponse> editar(
            @PathVariable Long idPerfil,
            @Valid @RequestBody EditarPerfilRequest request,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        PerfilResponse response = editarPerfilService.editar(idUsuario, idPerfil, request);
        return ResponseEntity.ok(response);
    }

    // UC-12 - Solicitar baja del perfil (cuenta regresiva de 30 días)
    @DeleteMapping("/perfiles/{idPerfil}")
    public ResponseEntity<EliminarPerfilResponse> eliminar(
            @PathVariable Long idPerfil,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        EliminarPerfilResponse response = eliminarPerfilService.eliminar(idUsuario, idPerfil);
        return ResponseEntity.ok(response);
    }

// UC-12 - Reactivar el perfil dentro de los 30 días
    @PostMapping("/perfiles/{idPerfil}/reactivar")
    public ResponseEntity<PerfilResponse> reactivar(
            @PathVariable Long idPerfil,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        PerfilResponse response = PerfilMapper.toResponse(reactivarPerfilService.reactivar(idUsuario, idPerfil));
        return ResponseEntity.ok(response);
    }

// UC-13 - Cambiar perfil activo (sirve tanto para la primera selección como para alternar entre perfiles)
    @PatchMapping("/perfiles/{idPerfil}/activar")
    public ResponseEntity<PerfilResponse> activar(
            @PathVariable Long idPerfil,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        ActivarPerfilService.ActivarPerfilResultado resultado = activarPerfilService.activar(idUsuario, idPerfil);

        ResponseCookie cookie = jwtCookieFactory.crear(resultado.token());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(resultado.perfil());
    }

}