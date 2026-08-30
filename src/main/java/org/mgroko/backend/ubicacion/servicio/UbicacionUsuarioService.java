package org.mgroko.backend.ubicacion.servicio;

import java.util.Optional;

import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de la ubicación del usuario. Cada usuario tiene una o ninguna
 * ubicación asociada. La creación/reutilización de la fila en la tabla
 * {@code ubicacion} se delega en {@link UbicacionService}, que es el mismo
 * punto de entrada que usarán futuros módulos (proyecto y actividad).
 */
@Service
public class UbicacionUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UbicacionService ubicacionService;

    public UbicacionUsuarioService(UsuarioRepository usuarioRepository, UbicacionService ubicacionService) {
        this.usuarioRepository = usuarioRepository;
        this.ubicacionService = ubicacionService;
    }

    /**
     * Devuelve la ubicación del usuario, o vacío si no tiene una asociada.
     */
    @Transactional(readOnly = true)
    public Optional<UbicacionResponse> obtener(Long idUsuario) {
        Usuario usuario = usuarioActivo(idUsuario);
        if (usuario.getUbicacion() == null) {
            return Optional.empty();
        }
        return Optional.of(UbicacionMapper.toResponse(usuario.getUbicacion()));
    }

    /**
     * Asigna (o reemplaza) la ubicación del usuario a partir de una localidad
     * del catálogo de Georef. Si esa localidad ya existe como fila en la tabla
     * {@code ubicacion}, se reutiliza en lugar de duplicarla.
     */
    @Transactional
    public UbicacionResponse asignar(Long idUsuario, UbicacionRequest request) {
        Usuario usuario = usuarioActivo(idUsuario);
        Ubicacion ubicacion = ubicacionService.obtenerOCrear(request.localidadId());
        usuario.setUbicacion(ubicacion);
        usuarioRepository.save(usuario);
        return UbicacionMapper.toResponse(ubicacion);
    }

    /**
     * Desasocia la ubicación del usuario (quedando sin ubicación).
     */
    @Transactional
    public void quitar(Long idUsuario) {
        Usuario usuario = usuarioActivo(idUsuario);
        usuario.setUbicacion(null);
        usuarioRepository.save(usuario);
    }

    private Usuario usuarioActivo(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
        if (usuario.getEstado() != EstadoUsuario.Activo) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }
        return usuario;
    }
}