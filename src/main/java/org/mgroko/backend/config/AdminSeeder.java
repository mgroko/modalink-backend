package org.mgroko.backend.config;

import java.time.LocalDate;

import org.mgroko.backend.auth.exception.GeneroNoEncontradoException;
import org.mgroko.backend.auth.exception.RolGlobalNoEncontradoException;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.ProveedorAuth;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.RolGlobalRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolGlobalRepository rolGlobalRepository;
    private final GeneroRepository generoRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.admin.correo}")
    private String adminCorreo;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.dni:00000000}")
    private String adminDni;

    @Override
    public void run(String... args) {
        String correo = adminCorreo.trim().toLowerCase();

        if (usuarioRepository.existsByCorreo(correo)) {
            return; // ya existe, no lo vuelve a crear
        }

        RolGlobal rolAdministrador = rolGlobalRepository.findByNombre("Administrador")
                .orElseThrow(() -> new RolGlobalNoEncontradoException(
                        "No se encontró el rol global 'Administrador'."));

        Genero genero = generoRepository.findByCodigo("no_decirlo")
                .orElseThrow(() -> new GeneroNoEncontradoException(
                        "El género 'no_decirlo' no existe."));

        Usuario admin = Usuario.builder()
                .nombre("Admin")
                .apellido("ModaLink")
                .dni(adminDni)
                .fechaNacimiento(LocalDate.now().minusYears(22)) 
                .correo(correo)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .proveedorAuth(ProveedorAuth.LOCAL)
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rolAdministrador)
                .genero(genero)
                .build();

        usuarioRepository.save(admin);
    }
}