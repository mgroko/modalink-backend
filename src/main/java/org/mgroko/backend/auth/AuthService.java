package org.mgroko.backend.auth;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.auth.dto.AuthResponse;
import org.mgroko.backend.auth.dto.LoginRequest;
import org.mgroko.backend.auth.dto.RegistroRequest;
import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.auth.exception.CorreoDuplicadoException;
import org.mgroko.backend.auth.exception.CredencialesInvalidasException;
import org.mgroko.backend.auth.exception.DniDuplicadoException;
import org.mgroko.backend.auth.exception.EdadInvalidaException;
import org.mgroko.backend.auth.exception.GeneroNoEncontradoException;
import org.mgroko.backend.auth.exception.RolGlobalNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioDeshabilitadoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PermisoGlobal;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.ProveedorAuth;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.RolGlobalRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

     private final UsuarioRepository usuarioRepository;
    private final RolGlobalRepository rolGlobalRepository;
    private final GeneroRepository generoRepository;
    private final PerfilRepository perfilRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            RolGlobalRepository rolGlobalRepository,
            GeneroRepository generoRepository,
            BCryptPasswordEncoder passwordEncoder,
            PerfilRepository perfilRepository,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolGlobalRepository = rolGlobalRepository;
        this.generoRepository = generoRepository;
        this.passwordEncoder = passwordEncoder;
        this.perfilRepository = perfilRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegistroResultado registrar(RegistroRequest request) {

        String correo = request.correo().trim().toLowerCase();
        String nombre = request.nombre().trim();
        String apellido = request.apellido().trim();
        String dni = request.dni().trim();

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new CorreoDuplicadoException("Ya existe un usuario registrado con ese correo.");
        }

        if (usuarioRepository.existsByDni(dni)) {
            throw new DniDuplicadoException("Ya existe un usuario registrado con ese DNI.");
        }
        if (Period.between(request.fechaNacimiento(), LocalDate.now()).getYears() < 18) {
            throw new EdadInvalidaException("Debés ser mayor de 18 años para registrarte.");
        }

        RolGlobal rolUsuario = rolGlobalRepository.findByNombre("Usuario")
                .orElseThrow(() -> new RolGlobalNoEncontradoException("No se encontró el rol global 'Usuario'."));

        Genero genero = generoRepository.findByCodigo(request.genero())
                .orElseThrow(() -> new GeneroNoEncontradoException(
                        "El género indicado no existe."));

        Usuario usuario = Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(dni)
                .fechaNacimiento(request.fechaNacimiento())
                .correo(correo)
                .passwordHash(passwordEncoder.encode(request.password()))
                .proveedorAuth(ProveedorAuth.LOCAL)
                .rolGlobal(rolUsuario)
                .genero(genero)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);

        String token = jwtService.generarToken(
                guardado.getIdUsuario().toString(),
                construirClaims(guardado, null)
        );

        return new RegistroResultado(token, UsuarioMapper.toResponse(guardado));
    }
    

    @Transactional(readOnly = true)
    public LoginResultado login(LoginRequest request) {
        String correo = request.correo().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña inválidos."));

        if (usuario.getPasswordHash() == null || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Correo o contraseña inválidos.");
        }

        if (!usuario.getEstado().permiteAcceso()) {
            throw new UsuarioDeshabilitadoException("Tu usuario está deshabilitado. Contactá al administrador.");
        }

        Perfil perfilActivoInicial = resolverPerfilActivoPorDefecto(usuario.getIdUsuario());

        String token = jwtService.generarToken(
                usuario.getIdUsuario().toString(),
                construirClaims(usuario, perfilActivoInicial)
        );

        UsuarioResponse usuarioResponse = UsuarioMapper.toResponseConPerfilActivo(
                usuario,
                perfilActivoInicial != null ? perfilActivoInicial.getIdPerfil() : null,
                perfilActivoInicial != null ? perfilActivoInicial.getNombreArtistico() : null
        );

        return new LoginResultado(token, new AuthResponse(usuarioResponse));
    } 

    @Transactional(readOnly = true)
        public UsuarioResponse obtenerUsuarioActual(Long idUsuario, Long idPerfilActivo, String nombreArtisticoActivo) {

            Usuario usuario = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

            return UsuarioMapper.toResponseConPerfilActivo(usuario, idPerfilActivo, nombreArtisticoActivo);
        }

    @Transactional(readOnly = true)
    public List<String> obtenerNombresPermisosGlobales(String nombreRol) {
        RolGlobal rol = rolGlobalRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RolGlobalNoEncontradoException("No se encontró el rol global '" + nombreRol + "'."));

        return rol.getPermisos().stream()
                .map(PermisoGlobal::getNombre)
                .toList();
    }

    public Map<String, Object> construirClaims(Usuario usuario, Perfil perfilActivo) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("correo", usuario.getCorreo());
    claims.put("rolGlobal", usuario.getRolGlobal().getNombre());
    claims.put("permisosGlobales", obtenerNombresPermisosGlobales(usuario.getRolGlobal().getNombre()));

    if (perfilActivo != null) {
        claims.put("idPerfilActivo", perfilActivo.getIdPerfil());
        claims.put("nombreArtisticoActivo", perfilActivo.getNombreArtistico());
    }
    return claims;
    }

    public Perfil resolverPerfilActivoPorDefecto(Long idUsuario) {
    List<Perfil> perfilesActivos = perfilRepository
            .findByUsuario_IdUsuarioAndEstado(idUsuario, EstadoPerfil.Activo);
    return perfilesActivos.size() == 1 ? perfilesActivos.get(0) : null;
    }

    public record LoginResultado(String token, AuthResponse response) {}

    public record RegistroResultado(String token, UsuarioResponse response) {}

}