package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
