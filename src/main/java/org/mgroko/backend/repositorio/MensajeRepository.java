package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.mensajeria.dto.UltimoMensajeRow;
import org.mgroko.backend.modelo.Mensaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    @EntityGraph(attributePaths = {
            "perfilEmisor",
            "perfilEmisor.usuario",
            "perfilEmisor.profesion",
            "perfilEmisor.imagen"
    })
    @Query("""
        SELECT m
        FROM Mensaje m
        WHERE m.conversacion.idConversacion = :idConversacion
    """)
    Page<Mensaje> buscarDeConversacion(
            @Param("idConversacion") Long idConversacion,
            Pageable pageable);

    @Query("""
        SELECT new org.mgroko.backend.mensajeria.dto.UltimoMensajeRow(
            m.conversacion.idConversacion,
            m.idMensaje,
            m.contenido,
            m.fechaEnvio,
            m.perfilEmisor.idPerfil)
        FROM Mensaje m
        WHERE m.conversacion.idConversacion IN :idsConversaciones
          AND m.idMensaje = (
              SELECT MAX(m2.idMensaje)
              FROM Mensaje m2
              WHERE m2.conversacion.idConversacion = m.conversacion.idConversacion
          )
    """)
    List<UltimoMensajeRow> buscarUltimosPorConversaciones(
            @Param("idsConversaciones") Collection<Long> idsConversaciones);

    @Query("""
        SELECT DISTINCT m.conversacion.idConversacion
        FROM Mensaje m
        WHERE m.conversacion.idConversacion IN :idsConversaciones
          AND m.perfilEmisor.idPerfil <> :idPerfilActivo
          AND m.leido = false
    """)
    List<Long> buscarIdsConNoLeidos(
            @Param("idsConversaciones") Collection<Long> idsConversaciones,
            @Param("idPerfilActivo") Long idPerfilActivo);

    @Query("""
        SELECT COUNT(DISTINCT m.conversacion.idConversacion)
        FROM Mensaje m
        WHERE (m.conversacion.perfil1.idPerfil = :idPerfilActivo
               OR m.conversacion.perfil2.idPerfil = :idPerfilActivo)
          AND m.perfilEmisor.idPerfil <> :idPerfilActivo
          AND m.leido = false
    """)
    long contarConversacionesNoLeidas(
            @Param("idPerfilActivo") Long idPerfilActivo);

    @Query("""
        SELECT COUNT(m) > 0
        FROM Mensaje m
        WHERE m.conversacion.idConversacion = :idConversacion
          AND m.perfilEmisor.idPerfil <> :idPerfilActivo
          AND m.leido = false
    """)
    boolean existeNoLeidoDeContraparte(
            @Param("idConversacion") Long idConversacion,
            @Param("idPerfilActivo") Long idPerfilActivo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Mensaje m
           SET m.leido = true,
               m.fechaLectura = :fechaLectura
         WHERE m.conversacion.idConversacion = :idConversacion
           AND m.perfilEmisor.idPerfil <> :idPerfilActivo
           AND m.leido = false
           AND (:hastaIdMensaje IS NULL OR m.idMensaje <= :hastaIdMensaje)
    """)
    int marcarLeidos(
            @Param("idConversacion") Long idConversacion,
            @Param("idPerfilActivo") Long idPerfilActivo,
            @Param("hastaIdMensaje") Long hastaIdMensaje,
            @Param("fechaLectura") LocalDateTime fechaLectura);

    @Query("""
        SELECT MAX(m.idMensaje)
        FROM Mensaje m
        WHERE m.conversacion.idConversacion = :idConversacion
          AND m.perfilEmisor.idPerfil <> :idPerfilActivo
    """)
    Optional<Long> buscarUltimoIdDeContraparte(
            @Param("idConversacion") Long idConversacion,
            @Param("idPerfilActivo") Long idPerfilActivo);

    Optional<Mensaje> findByIdMensajeAndConversacion_IdConversacion(
            Long idMensaje, Long idConversacion);
}
