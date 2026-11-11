package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.List;
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

    boolean existsByUsuarioDestino_IdUsuarioAndTipoAndEntidadTipoAndEntidadIdAndLeidaFalse(
            Long idUsuario,
            String tipo,
            String entidadTipo,
            Long entidadId);

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM Notificacion n
             WHERE n.tipo = :tipo
               AND n.entidadTipo = :entidadTipo
               AND n.entidadId = :entidadId
            """)
    int deleteByTipoAndEntidadTipoAndEntidadId(
            @Param("tipo") String tipo,
            @Param("entidadTipo") String entidadTipo,
            @Param("entidadId") Long entidadId);

    /**
     * UC-64: elimina la notificación de una entidad para un usuario y perfil
     * concretos. Se usa para no borrar las notificaciones de otros
     * destinatarios de la misma publicación.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM Notificacion n
             WHERE n.tipo = :tipo
               AND n.entidadTipo = :entidadTipo
               AND n.entidadId = :entidadId
               AND n.usuarioDestino.idUsuario = :idUsuarioDestino
               AND n.perfilContexto.idPerfil = :idPerfilContexto
            """)
    int deleteByTipoAndEntidadTipoAndEntidadIdYPerfil(
            @Param("tipo") String tipo,
            @Param("entidadTipo") String entidadTipo,
            @Param("entidadId") Long entidadId,
            @Param("idUsuarioDestino") Long idUsuarioDestino,
            @Param("idPerfilContexto") Long idPerfilContexto);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notificacion n
               SET n.leida = true,
                   n.fechaLeida = :fechaLeida
             WHERE n.usuarioDestino.idUsuario = :idUsuario
               AND n.entidadTipo = 'CONVERSACION'
               AND n.entidadId = :idConversacion
               AND n.leida = false
            """)
    int marcarLeidasDeConversacion(
            @Param("idUsuario") Long idUsuario,
            @Param("idConversacion") Long idConversacion,
            @Param("fechaLeida") LocalDateTime fechaLeida);

    @Query("""
            SELECT DISTINCT n.perfilContexto.idPerfil
              FROM Notificacion n
             WHERE n.tipo = :tipo
               AND n.entidadTipo = 'PROYECTO'
               AND n.entidadId = :idProyecto
               AND n.leida = false
            """)
    List<Long> buscarPerfilesConNotificacionNoLeida(
            @Param("tipo") String tipo,
            @Param("idProyecto") Long idProyecto);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notificacion n
               SET n.leida = true,
                   n.fechaLeida = :fechaLeida
             WHERE n.usuarioDestino.idUsuario = :idUsuario
               AND n.perfilContexto.idPerfil = :idPerfilContexto
               AND n.tipo = 'MENSAJE_GRUPAL_RECIBIDO'
               AND n.entidadTipo = 'PROYECTO'
               AND n.entidadId = :idProyecto
               AND n.leida = false
            """)
    int marcarLeidasDeChatGrupal(
            @Param("idUsuario") Long idUsuario,
            @Param("idPerfilContexto") Long idPerfilContexto,
            @Param("idProyecto") Long idProyecto,
            @Param("fechaLeida") LocalDateTime fechaLeida);
}
