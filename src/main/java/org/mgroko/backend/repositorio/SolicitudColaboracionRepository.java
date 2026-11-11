package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SolicitudColaboracionRepository
        extends JpaRepository<SolicitudColaboracion, Long>,
                JpaSpecificationExecutor<SolicitudColaboracion> {

    /**
     * UC-61: impide más de una solicitud para la misma publicación y
     * destinatario mientras la anterior siga pendiente. El índice único
     * parcial de la base refuerza la misma regla ante concurrencia.
     */
    boolean existsByPublicacionIdPublicacionAndPerfilDestinatarioIdPerfilAndEstado(
            Long idPublicacion,
            Long idPerfilDestinatario,
            EstadoSolicitud estado);

    /**
     * UC-65: carga el detalle de la página sin N+1. Las imágenes de la
     * publicación se cargan en lote mediante {@code @BatchSize}.
     */
    @EntityGraph(attributePaths = {
            "publicacion",
            "perfilRemitente",
            "perfilRemitente.profesion",
            "perfilRemitente.imagen",
            "perfilDestinatario",
            "perfilDestinatario.profesion",
            "perfilDestinatario.imagen"
    })
    @Query("""
            SELECT s
            FROM SolicitudColaboracion s
            WHERE s.idSolicitudColaboracion IN :ids
            """)
    List<SolicitudColaboracion> findAllByIdInWithDetalle(
            @Param("ids") Collection<Long> ids);

    /**
     * UC-62/63: bloquea la solicitud para que aceptar y rechazar no compitan
     * entre sí.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM SolicitudColaboracion s
            WHERE s.idSolicitudColaboracion = :idSolicitudColaboracion
            """)
    Optional<SolicitudColaboracion> findByIdForUpdate(
            @Param("idSolicitudColaboracion") Long idSolicitudColaboracion);
}
