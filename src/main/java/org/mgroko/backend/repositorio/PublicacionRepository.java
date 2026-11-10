package org.mgroko.backend.repositorio;

import java.util.List;

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
    Page<Publicacion> findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
            Long idPerfil,
            EstadoPublicacion estado,
            Pageable pageable);

    /**
     * Proyectos distintos asociados a publicaciones activas del perfil.
     * Se usan para poblar el selector de proyecto de UC-22.
     */
    @Query("""
            SELECT DISTINCT p.proyecto
            FROM Publicacion p
            WHERE p.perfil.idPerfil = :idPerfil
              AND p.estado = :estado
              AND p.proyecto IS NOT NULL
            ORDER BY p.proyecto.nombre
            """)
    List<Proyecto> findProyectosConPublicaciones(
            @Param("idPerfil") Long idPerfil,
            @Param("estado") EstadoPublicacion estado);
}
