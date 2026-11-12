package org.mgroko.backend.publicaciones.servicio;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Comentario;
import org.mgroko.backend.publicaciones.dto.ComentarioResponse;
import org.mgroko.backend.publicaciones.mapper.ComentarioMapper;
import org.mgroko.backend.repositorio.ComentarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-66: listado paginado de comentarios de una publicación, del más antiguo
 * al más reciente.
 */
@Service
public class ListarComentariosService {

    private static final int TAMANIO_POR_DEFECTO = 10;
    private static final int TAMANIO_MAXIMO = 50;

    private final PublicacionInteraccionService publicacionInteraccionService;
    private final ComentarioRepository comentarioRepository;

    public ListarComentariosService(
            PublicacionInteraccionService publicacionInteraccionService,
            ComentarioRepository comentarioRepository) {
        this.publicacionInteraccionService = publicacionInteraccionService;
        this.comentarioRepository = comentarioRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ComentarioResponse> listar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion,
            int pagina,
            int tamanio) {

        publicacionInteraccionService.resolver(
                idUsuario, idPerfilActivo, idPublicacion);

        Pageable pageable = PageRequest.of(
                Math.max(pagina, 0),
                normalizarTamanio(tamanio));

        Page<Comentario> comentarios = comentarioRepository
                .findByPublicacion_IdPublicacionOrderByFechaComentarioAscIdComentarioAsc(
                        idPublicacion, pageable);

        return PaginaResponse.fromPage(comentarios.map(ComentarioMapper::toResponse));
    }

    private int normalizarTamanio(int tamanio) {
        if (tamanio <= 0) {
            return TAMANIO_POR_DEFECTO;
        }
        return Math.min(tamanio, TAMANIO_MAXIMO);
    }
}
