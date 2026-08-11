package org.mgroko.backend.auth;

import org.mgroko.backend.dto.AuthResponse;
import org.mgroko.backend.dto.LoginRequest;
import org.mgroko.backend.dto.RegistroRequest;
import org.mgroko.backend.dto.UsuarioResponse;
import org.mgroko.backend.exception.CorreoDuplicadoException;
import org.mgroko.backend.exception.CredencialesInvalidasException;
import org.mgroko.backend.exception.DniDuplicadoException;
import org.mgroko.backend.exception.RolGlobalNoEncontradoException;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
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

        return new AuthResponse(null, UsuarioMapper.toResponse(usuario));
    }
}