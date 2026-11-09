package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.SolicitudConexion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SolicitudConexionRepository
        extends JpaRepository<SolicitudConexion, Long>,
                JpaSpecificationExecutor<SolicitudConexion> {

    /**
     * Indica si existe una solicitud pendiente entre dos perfiles, en
     * cualquiera de las dos direcciones. El contrato de UC-43 bloquea tanto la
     * solicitud repetida como la solicitud inversa.
     */
    @Query("""
        SELECT COUNT(s) > 0
        FROM SolicitudConexion s
        WHERE s.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
          AND (
            (s.perfilRemitente.idPerfil = :idPerfilA
             AND s.perfilDestinatario.idPerfil = :idPerfilB)
            OR
            (s.perfilRemitente.idPerfil = :idPerfilB
             AND s.perfilDestinatario.idPerfil = :idPerfilA)
          )
    """)
    boolean existsPendienteEntre(@Param("idPerfilA") Long idPerfilA,
            @Param("idPerfilB") Long idPerfilB);

    /**
     * Devuelve la solicitud pendiente entre dos perfiles, sin importar la
     * dirección. Se usa para calcular el estado de la relación en el detalle
     * del perfil.
     */
    @Query("""
        SELECT s
        FROM SolicitudConexion s
        WHERE s.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
          AND (
            (s.perfilRemitente.idPerfil = :idPerfilA
             AND s.perfilDestinatario.idPerfil = :idPerfilB)
            OR
            (s.perfilRemitente.idPerfil = :idPerfilB
             AND s.perfilDestinatario.idPerfil = :idPerfilA)
          )
    """)
    Optional<SolicitudConexion> findPendienteEntre(@Param("idPerfilA") Long idPerfilA,
            @Param("idPerfilB") Long idPerfilB);

    @EntityGraph(attributePaths = {
            "perfilRemitente",
            "perfilRemitente.usuario",
            "perfilRemitente.profesion",
            "perfilDestinatario",
            "perfilDestinatario.usuario",
            "perfilDestinatario.profesion"
    })
    Optional<SolicitudConexion> findByIdSolicitudConexion(Long idSolicitudConexion);

    /**
     * Carga las solicitudes de una página con sus perfiles, usuarios,
     * profesiones e imágenes para evitar N+1 al mapear UC-46.
     */
    @EntityGraph(attributePaths = {
            "perfilRemitente",
            "perfilRemitente.usuario",
            "perfilRemitente.profesion",
            "perfilRemitente.imagen",
            "perfilDestinatario",
            "perfilDestinatario.usuario",
            "perfilDestinatario.profesion",
            "perfilDestinatario.imagen"
    })
    @Query("""
        SELECT s
        FROM SolicitudConexion s
        WHERE s.idSolicitudConexion IN :ids
    """)
    List<SolicitudConexion> findAllByIdInWithPerfiles(
            @Param("ids") Collection<Long> ids);

    /**
     * Bloquea la solicitud para que UC-45 no la acepte y rechace a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM SolicitudConexion s
        WHERE s.idSolicitudConexion = :idSolicitudConexion
    """)
    Optional<SolicitudConexion> findByIdForUpdate(
            @Param("idSolicitudConexion") Long idSolicitudConexion);
}
