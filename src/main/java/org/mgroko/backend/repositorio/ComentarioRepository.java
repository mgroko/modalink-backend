package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.Comentario;
import org.mgroko.backend.repositorio.proyeccion.ConteoPorPublicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    /**
     * Comentarios de una publicación en orden cronológico ascendente. Las
     * asociaciones del autor se cargan en la misma consulta para armar la
     * respuesta sin N+1.
     */
    @EntityGraph(attributePaths = {
            "perfil",
            "perfil.profesion",
            "perfil.imagen"
    })
    Page<Comentario> findByPublicacion_IdPublicacionOrderByFechaComentarioAscIdComentarioAsc(
            Long idPublicacion,
            Pageable pageable);

    @Query("""
            SELECT c.publicacion.idPublicacion AS idPublicacion, COUNT(c) AS total
            FROM Comentario c
            WHERE c.publicacion.idPublicacion IN :idsPublicaciones
            GROUP BY c.publicacion.idPublicacion
            """)
    List<ConteoPorPublicacion> contarPorPublicaciones(
            @Param("idsPublicaciones") Collection<Long> idsPublicaciones);

    @Query("""
            SELECT COUNT(c)
            FROM Comentario c
            WHERE c.publicacion.idPublicacion = :idPublicacion
            """)
    long contarPorPublicacion(@Param("idPublicacion") Long idPublicacion);
}
