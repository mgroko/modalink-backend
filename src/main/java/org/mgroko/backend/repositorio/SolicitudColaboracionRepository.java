package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudColaboracionRepository
        extends JpaRepository<SolicitudColaboracion, Long> {

    /**
     * UC-61: impide más de una solicitud para la misma publicación y
     * destinatario mientras la anterior siga pendiente. El índice único
     * parcial de la base refuerza la misma regla ante concurrencia.
     */
    boolean existsByPublicacionIdPublicacionAndPerfilDestinatarioIdPerfilAndEstado(
            Long idPublicacion,
            Long idPerfilDestinatario,
            EstadoSolicitud estado);
}
