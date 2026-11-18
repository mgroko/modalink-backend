package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.NotificacionEntrega;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionEntregaRepository
        extends JpaRepository<NotificacionEntrega, Long>, NotificacionEntregaRepositoryCustom {

    @EntityGraph(attributePaths = {
            "notificacion",
            "notificacion.usuarioDestino",
            "notificacion.perfilContexto"
    })
    @Query("SELECT e FROM NotificacionEntrega e WHERE e.idEntrega = :idEntrega")
    Optional<NotificacionEntrega> findByIdConNotificacion(@Param("idEntrega") Long idEntrega);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificacionEntrega e
               SET e.estado = :estado,
                   e.error = :error,
                   e.proximoIntento = :proximoIntento,
                   e.fechaEnvio = :fechaEnvio
             WHERE e.idEntrega = :idEntrega
            """)
    int actualizarResultado(
            @Param("idEntrega") Long idEntrega,
            @Param("estado") String estado,
            @Param("error") String error,
            @Param("proximoIntento") java.time.LocalDateTime proximoIntento,
            @Param("fechaEnvio") java.time.LocalDateTime fechaEnvio);
}
