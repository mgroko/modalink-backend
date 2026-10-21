package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.Optional;

import org.mgroko.backend.modelo.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuarioDestino_IdUsuario(Long idUsuario, Pageable pageable);

    Page<Notificacion> findByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(
            Long idUsuario, Long idPerfilContexto, Pageable pageable);

    Optional<Notificacion> findByIdNotificacionAndUsuarioDestino_IdUsuario(
            Long idNotificacion, Long idUsuario);

    long countByUsuarioDestino_IdUsuario(Long idUsuario);

    long countByUsuarioDestino_IdUsuarioAndLeidaFalse(Long idUsuario);

    long countByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(
            Long idUsuario, Long idPerfilContexto);

    long countByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfilAndLeidaFalse(
            Long idUsuario, Long idPerfilContexto);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notificacion n
               SET n.leida = true,
                   n.fechaLeida = :fechaLeida
             WHERE n.usuarioDestino.idUsuario = :idUsuario
               AND n.leida = false
            """)
    int marcarTodasLeidas(
            @Param("idUsuario") Long idUsuario,
            @Param("fechaLeida") LocalDateTime fechaLeida);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notificacion n
               SET n.leida = true,
                   n.fechaLeida = :fechaLeida
             WHERE n.usuarioDestino.idUsuario = :idUsuario
               AND n.perfilContexto.idPerfil = :idPerfilContexto
               AND n.leida = false
            """)
    int marcarTodasLeidasDePerfil(
            @Param("idUsuario") Long idUsuario,
            @Param("idPerfilContexto") Long idPerfilContexto,
            @Param("fechaLeida") LocalDateTime fechaLeida);
}
