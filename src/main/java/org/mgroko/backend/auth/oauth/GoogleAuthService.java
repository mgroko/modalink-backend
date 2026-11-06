package org.mgroko.backend.auth.oauth;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.mgroko.backend.auth.dto.CompletarRegistroGoogleRequest;
import org.mgroko.backend.auth.exception.CuentaGoogleNoVerificadaException;
import org.mgroko.backend.auth.exception.DniDuplicadoException;
import org.mgroko.backend.auth.exception.DniInvalidoException;
import org.mgroko.backend.auth.exception.EdadInvalidaException;
import org.mgroko.backend.auth.exception.GeneroNoEncontradoException;
import org.mgroko.backend.auth.exception.IdentidadGoogleConflictoException;
import org.mgroko.backend.auth.exception.RolGlobalNoEncontradoException;
import org.mgroko.backend.auth.servicio.AuthService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.UsuarioIdentidad;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.ProveedorAuth;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.RolGlobalRepository;
import org.mgroko.backend.repositorio.UsuarioIdentidadRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el usuario a partir de la identidad de Google, vincula cuentas
 * locales existentes y completa el registro diferido (UC-09).
 */
@Service
public class GoogleAuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioIdentidadRepository usuarioIdentidadRepository;
    private final RolGlobalRepository rolGlobalRepository;
    private final GeneroRepository generoRepository;
    private final UbicacionService ubicacionService;
    private final AuthService authService;
    private final RegistroPendienteService registroPendienteService;

    public GoogleAuthService(
            UsuarioRepository usuarioRepository,
            UsuarioIdentidadRepository usuarioIdentidadRepository,
            RolGlobalRepository rolGlobalRepository,
            GeneroRepository generoRepository,
            UbicacionService ubicacionService,
            AuthService authService,
            RegistroPendienteService registroPendienteService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioIdentidadRepository = usuarioIdentidadRepository;
        this.rolGlobalRepository = rolGlobalRepository;
        this.generoRepository = generoRepository;
        this.ubicacionService = ubicacionService;
        this.authService = authService;
        this.registroPendienteService = registroPendienteService;
    }

    @Transactional
    public GoogleAuthResult autenticar(GoogleUserData datos) {
        if (!datos.emailVerificado()) {
            throw new CuentaGoogleNoVerificadaException(
                    "Google no confirmó que el correo esté verificado.");
        }

        String correo = normalizarCorreo(datos.correo());

        Optional<UsuarioIdentidad> identidadGoogle = usuarioIdentidadRepository
                .findByProveedorAndIdExterno(ProveedorAuth.GOOGLE, datos.sub());
        if (identidadGoogle.isPresent()) {
            return iniciarSesion(identidadGoogle.get().getUsuario());
        }

        Optional<Usuario> usuarioPorCorreo = usuarioRepository.findByCorreo(correo);
        if (usuarioPorCorreo.isPresent()) {
            Usuario usuario = usuarioPorCorreo.get();
            vincularGoogle(usuario, datos);
            return iniciarSesion(usuario);
        }

        return GoogleAuthResult.registroPendiente(registroPendienteService.crearTicket(datos));
    }

    @Transactional
    public AuthService.LoginResultado completarRegistro(
            String ticket,
            CompletarRegistroGoogleRequest request) {

        GoogleUserData datos = registroPendienteService.leerTicket(ticket);
        if (!datos.emailVerificado()) {
            throw new CuentaGoogleNoVerificadaException(
                    "Google no confirmó que el correo esté verificado.");
        }

        String correo = normalizarCorreo(datos.correo());

        // Idempotencia: si la identidad Google ya existe (por ejemplo, el
        // ticket se reenvía después de crear la cuenta), se inicia sesión.
        Optional<UsuarioIdentidad> identidadExistente = usuarioIdentidadRepository
                .findByProveedorAndIdExterno(ProveedorAuth.GOOGLE, datos.sub());
        if (identidadExistente.isPresent()) {
            Usuario usuario = identidadExistente.get().getUsuario();
            authService.reactivarSiPendienteBaja(usuario);
            return authService.construirLoginResultado(usuario);
        }

        // Si la cuenta ya existe por correo, se vincula y se inicia sesión.
        Optional<Usuario> existente = usuarioRepository.findByCorreo(correo);
        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            vincularGoogle(usuario, datos);
            authService.reactivarSiPendienteBaja(usuario);
            return authService.construirLoginResultado(usuario);
        }

        String nombre = request.nombre().trim();
        String apellido = request.apellido().trim();
        String dni = request.dni().trim();

        if (!dni.matches("^[0-9]+$")) {
            throw new DniInvalidoException("El DNI debe contener únicamente dígitos numéricos.");
        }
        if (usuarioRepository.existsByDni(dni)) {
            throw new DniDuplicadoException("Ya existe un usuario registrado con ese DNI.");
        }
        if (Period.between(request.fechaNacimiento(), LocalDate.now()).getYears() < 18) {
            throw new EdadInvalidaException("Debés ser mayor de 18 años para registrarte.");
        }

        RolGlobal rolUsuario = rolGlobalRepository.findByNombre("Usuario")
                .orElseThrow(() -> new RolGlobalNoEncontradoException(
                        "No se encontró el rol global 'Usuario'."));

        Genero genero = generoRepository.findByCodigo(request.genero())
                .orElseThrow(() -> new GeneroNoEncontradoException(
                        "El género indicado no existe."));

        Ubicacion ubicacion = null;
        if (request.localidadId() != null && !request.localidadId().isBlank()) {
            ubicacion = ubicacionService.obtenerOCrear(request.localidadId());
        }

        Usuario usuario = Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(dni)
                .fechaNacimiento(request.fechaNacimiento())
                .correo(correo)
                .passwordHash(null)
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rolUsuario)
                .genero(genero)
                .ubicacion(ubicacion)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        usuarioIdentidadRepository.save(UsuarioIdentidad.builder()
                .usuario(guardado)
                .proveedor(ProveedorAuth.GOOGLE)
                .idExterno(datos.sub())
                .correo(correo)
                .correoVerificado(true)
                .build());

        return authService.construirLoginResultado(guardado);
    }

    private GoogleAuthResult iniciarSesion(Usuario usuario) {
        boolean reactivada = authService.reactivarSiPendienteBaja(usuario);
        return GoogleAuthResult.sesion(authService.construirLoginResultado(usuario), reactivada);
    }

    private void vincularGoogle(Usuario usuario, GoogleUserData datos) {
        Optional<UsuarioIdentidad> identidadPorSub = usuarioIdentidadRepository
                .findByProveedorAndIdExterno(ProveedorAuth.GOOGLE, datos.sub());
        if (identidadPorSub.isPresent()
                && !identidadPorSub.get().getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new IdentidadGoogleConflictoException(
                    "La cuenta de Google ya está vinculada a otro usuario.");
        }

        Optional<UsuarioIdentidad> identidadDelUsuario = usuarioIdentidadRepository
                .findByUsuario_IdUsuarioAndProveedor(usuario.getIdUsuario(), ProveedorAuth.GOOGLE);
        if (identidadDelUsuario.isPresent()) {
            if (!datos.sub().equals(identidadDelUsuario.get().getIdExterno())) {
                throw new IdentidadGoogleConflictoException(
                        "El usuario ya tiene otra cuenta de Google vinculada.");
            }
            return;
        }

        usuarioIdentidadRepository.save(UsuarioIdentidad.builder()
                .usuario(usuario)
                .proveedor(ProveedorAuth.GOOGLE)
                .idExterno(datos.sub())
                .correo(normalizarCorreo(datos.correo()))
                .correoVerificado(true)
                .build());
    }

    private String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase();
    }
}
