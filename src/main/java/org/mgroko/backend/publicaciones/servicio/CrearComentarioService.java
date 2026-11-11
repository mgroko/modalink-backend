package org.mgroko.backend.publicaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.mgroko.backend.modelo.Comentario;
import org.mgroko.backend.publicaciones.dto.ComentarioResponse;
import org.mgroko.backend.publicaciones.dto.CrearComentarioRequest;
import org.mgroko.backend.publicaciones.exception.ComentarioInvalidoException;
import org.mgroko.backend.publicaciones.mapper.ComentarioMapper;
import org.mgroko.backend.repositorio.ComentarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-66: registra un comentario y notifica a los perfiles asociados a la
 * publicación.
 */
@Service
public class CrearComentarioService {

    public static final int LIMITE_TEXTO = 1000;

    private final PublicacionInteraccionService publicacionInteraccionService;
    private final ComentarioRepository comentarioRepository;
    private final PublicacionInteraccionNotificacionService notificacionService;
    private final Clock reloj;

    public CrearComentarioService(
            PublicacionInteraccionService publicacionInteraccionService,
            ComentarioRepository comentarioRepository,
            PublicacionInteraccionNotificacionService notificacionService,
            Clock reloj) {
        this.publicacionInteraccionService = publicacionInteraccionService;
        this.comentarioRepository = comentarioRepository;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    @Transactional
    public ComentarioResponse crear(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion,
            CrearComentarioRequest request) {

        PublicacionInteraccionService.ContextoInteraccionPublicacion contexto =
                publicacionInteraccionService.resolver(
                        idUsuario, idPerfilActivo, idPublicacion);

        String texto = normalizarTexto(request);

        Comentario comentario = Comentario.builder()
                .texto(texto)
                .fechaComentario(LocalDateTime.now(reloj))
                .publicacion(contexto.publicacion())
                .perfil(contexto.perfilActivo())
                .build();

        comentario = comentarioRepository.saveAndFlush(comentario);

        notificacionService.notificarComentario(
                contexto.publicacion(), contexto.perfilActivo());

        return ComentarioMapper.toResponse(comentario);
    }

    private String normalizarTexto(CrearComentarioRequest request) {
        if (request == null || request.texto() == null) {
            throw new ComentarioInvalidoException(
                    "El comentario no puede estar vacío.");
        }

        String texto = request.texto().trim();
        if (texto.isEmpty()) {
            throw new ComentarioInvalidoException(
                    "El comentario no puede estar vacío.");
        }

        if (texto.length() > LIMITE_TEXTO) {
            throw new ComentarioInvalidoException(
                    "El comentario no puede superar los "
                            + LIMITE_TEXTO + " caracteres.");
        }

        return texto;
    }
}
