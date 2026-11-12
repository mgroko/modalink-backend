package org.mgroko.backend.publicaciones.servicio;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-66: valida que el perfil activo pueda interactuar con la publicación.
 *
 * <p>Reutiliza las reglas de UC-22/UC-14: perfil activo, publicación
 * {@code ACTIVA} y autor visible para el usuario autenticado.</p>
 */
@Service
public class PublicacionInteraccionService {

    private final PublicacionRepository publicacionRepository;
    private final PublicacionDatosService publicacionDatosService;
    private final VerPerfilService verPerfilService;

    public PublicacionInteraccionService(
            PublicacionRepository publicacionRepository,
            PublicacionDatosService publicacionDatosService,
            VerPerfilService verPerfilService) {
        this.publicacionRepository = publicacionRepository;
        this.publicacionDatosService = publicacionDatosService;
        this.verPerfilService = verPerfilService;
    }

    @Transactional(readOnly = true)
    public ContextoInteraccionPublicacion resolver(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion) {

        Perfil perfilActivo = publicacionDatosService
                .validarPerfilActivo(idUsuario, idPerfilActivo);

        Publicacion publicacion = publicacionRepository
                .findByIdPublicacion(idPublicacion)
                .orElseThrow(() -> new PublicacionNoEncontradaException(idPublicacion));

        if (publicacion.getEstado() != EstadoPublicacion.ACTIVA) {
            throw new PublicacionNoEncontradaException(idPublicacion);
        }

        verPerfilService.obtenerPerfilVisible(
                publicacion.getPerfil().getIdPerfil(), idUsuario);

        return new ContextoInteraccionPublicacion(perfilActivo, publicacion);
    }

    public record ContextoInteraccionPublicacion(
            Perfil perfilActivo,
            Publicacion publicacion) {
    }
}
