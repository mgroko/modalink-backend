package org.mgroko.backend.usuario.servicio;

import java.time.LocalDate;
import java.time.Period;

import org.mgroko.backend.auth.exception.EdadInvalidaException;
import org.mgroko.backend.auth.exception.GeneroNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.mgroko.backend.usuario.dto.DatosPersonalesRequest;
import org.mgroko.backend.usuario.dto.DatosPersonalesResponse;
import org.mgroko.backend.usuario.mapper.DatosPersonalesMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatosPersonalesService {

    private final UsuarioRepository usuarioRepository;
    private final GeneroRepository generoRepository;
    private final UbicacionService ubicacionService;

    public DatosPersonalesService(UsuarioRepository usuarioRepository,
            GeneroRepository generoRepository,
            UbicacionService ubicacionService) {
        this.usuarioRepository = usuarioRepository;
        this.generoRepository = generoRepository;
        this.ubicacionService = ubicacionService;
    }

    @Transactional
    public DatosPersonalesResponse actualizar(Long idUsuario, DatosPersonalesRequest request) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        if (usuario.getEstado() != EstadoUsuario.Activo) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }

        int edad = Period.between(request.fechaNacimiento(), LocalDate.now()).getYears();
        if (edad < 18) {
            throw new EdadInvalidaException("El usuario debe ser mayor o igual a 18 años.");
        }

        Genero genero = generoRepository.findByCodigo(request.genero())
                .orElseThrow(() -> new GeneroNoEncontradoException("Género no encontrado: " + request.genero()));

        Ubicacion ubicacion = null;
        String localidadId = request.localidadId();
        if (localidadId != null && !localidadId.isBlank()) {
            ubicacion = ubicacionService.obtenerOCrear(localidadId);
        }

        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setGenero(genero);
        usuario.setUbicacion(ubicacion);

        Usuario guardado = usuarioRepository.save(usuario);
        return DatosPersonalesMapper.toResponse(guardado);
    }
}
