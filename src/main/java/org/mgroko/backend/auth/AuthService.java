package org.mgroko.backend.auth;

import java.time.LocalDate;
import java.time.Period;

import org.mgroko.backend.auth.dto.AuthResponse;
import org.mgroko.backend.auth.dto.LoginRequest;
import org.mgroko.backend.auth.dto.RegistroRequest;
import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.auth.exception.CorreoDuplicadoException;
import org.mgroko.backend.auth.exception.CredencialesInvalidasException;
import org.mgroko.backend.auth.exception.DniDuplicadoException;
import org.mgroko.backend.auth.exception.EdadInvalidaException;
import org.mgroko.backend.auth.exception.RolGlobalNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioDeshabilitadoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.ProveedorAuth;
import org.mgroko.backend.repositorio.RolGlobalRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolGlobalRepository rolGlobalRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(
            UsuarioRepository usuarioRepository,
            RolGlobalRepository rolGlobalRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolGlobalRepository = rolGlobalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new CorreoDuplicadoException("Ya existe un usuario registrado con ese correo.");
        }

        if (usuarioRepository.existsByDni(request.dni())) {
            throw new DniDuplicadoException("Ya existe un usuario registrado con ese DNI.");
        }
        if (Period.between(request.fechaNacimiento(), LocalDate.now()).getYears() < 18) {
            throw new EdadInvalidaException("Debés ser mayor de 18 años para registrarte.");
        }

        RolGlobal rolUsuario = rolGlobalRepository.findByNombre("Usuario")
                .orElseThrow(() -> new RolGlobalNoEncontradoException("No se encontró el rol global 'Usuario'."));

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .dni(request.dni())
                .fechaNacimiento(request.fechaNacimiento())
                .correo(request.correo())
                .passwordHash(passwordEncoder.encode(request.password()))
                .proveedorAuth(ProveedorAuth.LOCAL)
                .rolGlobal(rolUsuario)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return UsuarioMapper.toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña inválidos."));

        if (usuario.getPasswordHash() == null || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Correo o contraseña inválidos.");
        }

        return new AuthResponse(UsuarioMapper.toResponse(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioActual(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        if (usuario.getEstado() != EstadoUsuario.Activo) {
            throw new UsuarioDeshabilitadoException("Tu usuario no está activo. Contactá al administrador.");
        }

    return UsuarioMapper.toResponse(usuario);
}
    

}