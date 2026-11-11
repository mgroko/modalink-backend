package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long>,
        JpaSpecificationExecutor<Publicacion> {

    /**
     * Publicaciones de un perfil en el estado indicado, más recientes primero.
     * Se inicializan las asociaciones necesarias para armar la tarjeta sin
     * provocar N+1; las imágenes se cargan en lote por {@code @BatchSize}.
     */
    @EntityGraph(attributePaths = {
            "perfil",
            "perfil.usuario",
            "perfil.profesion",
            "perfil.imagen",
            "proyecto",
            "ubicacion",
            "ubicacion.ciudad",
            "ubicacion.ciudad.provincia",
            "ubicacion.ciudad.provincia.pais"
    })
    @Query("""
            SELECT p
            FROM Publicacion p
            WHERE p.estado = :estado
              AND (
                p.perfil.idPerfil = :idPerfil
                OR (
                  EXISTS (SELECT c FROM p.colaboradores c WHERE c.idPerfil = :idPerfil)
                  AND p.perfil.estado = org.mgroko.backend.modelo.enums.EstadoPerfil.ACTIVO
                  AND p.perfil.usuario.estado = org.mgroko.backend.modelo.enums.EstadoUsuario.ACTIVO
                )
              )
            ORDER BY p.fechaPublicacion DESC, p.idPublicacion DESC
            """)
    Page<Publicacion> findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
            @Param("idPerfil") Long idPerfil,
            @Param("estado") EstadoPublicacion estado,
            Pageable pageable);

    /**
     * Proyectos distintos asociados a publicaciones activas del perfil.
     * Se usan para poblar el selector de proyecto de UC-22.
     */
    @Query("""
            SELECT DISTINCT p.proyecto
            FROM Publicacion p
            WHERE p.estado = :estado
              AND p.proyecto IS NOT NULL
              AND (
                p.perfil.idPerfil = :idPerfil
                OR (
                  EXISTS (SELECT c FROM p.colaboradores c WHERE c.idPerfil = :idPerfil)
                  AND p.perfil.estado = org.mgroko.backend.modelo.enums.EstadoPerfil.ACTIVO
                  AND p.perfil.usuario.estado = org.mgroko.backend.modelo.enums.EstadoUsuario.ACTIVO
                )
              )
            ORDER BY p.proyecto.nombre
            """)
    List<Proyecto> findProyectosConPublicaciones(
            @Param("idPerfil") Long idPerfil,
            @Param("estado") EstadoPublicacion estado);

    /**
     * UC-21: publicación propia con las asociaciones que necesita el
     * formulario de edición. Las colecciones ({@code imagenes} y
     * {@code colaboradores}) se cargan en lote para no armar un producto
     * cartesiano ni romper la paginación de los listados.
     */
    @EntityGraph(attributePaths = {
            "perfil",
            "perfil.profesion",
            "perfil.imagen",
            "proyecto",
            "ubicacion",
            "ubicacion.ciudad",
            "ubicacion.ciudad.provincia",
            "ubicacion.ciudad.provincia.pais"
    })
    Optional<Publicacion> findByIdPublicacionAndPerfilIdPerfil(
            Long idPublicacion, Long idPerfil);

    /**
     * UC-61: indica si un perfil ya es colaborador de la publicación.
     */
    @Query("""
            SELECT COUNT(c) > 0
            FROM Publicacion p
            JOIN p.colaboradores c
            WHERE p.idPublicacion = :idPublicacion
              AND c.idPerfil = :idPerfil
            """)
    boolean existsColaborador(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil);
}
