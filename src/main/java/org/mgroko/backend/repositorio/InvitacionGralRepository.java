package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitacionGralRepository extends JpaRepository<InvitacionGral, Long> {

    @Query("""
        SELECT i FROM InvitacionGral i
        JOIN FETCH i.perfilDestinatario perfil
        JOIN FETCH perfil.usuario
        WHERE i.proyecto.idProyecto = :idProyecto
          AND i.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<InvitacionGral> findPendientesDeProyectoConDestinatario(@Param("idProyecto") Long idProyecto);

    /**
     * Invitaciones generales en las que el perfil participa como remitente o
     * destinatario, con los datos necesarios para UC-30.
     */
    @Query("""
        SELECT i FROM InvitacionGral i
        JOIN FETCH i.perfilRemitente r
        JOIN FETCH r.usuario ru
        JOIN FETCH r.profesion rp
        JOIN FETCH i.perfilDestinatario d
        JOIN FETCH d.usuario du
        JOIN FETCH d.profesion dp
        JOIN FETCH i.proyecto p
        LEFT JOIN FETCH i.requerimientoGral rg
        WHERE r.idPerfil = :idPerfil OR d.idPerfil = :idPerfil
        ORDER BY i.fechaEnvio DESC, i.idInvitacionGral DESC
    """)
    List<InvitacionGral> buscarPorPerfil(@Param("idPerfil") Long idPerfil);

    @Query("""
        SELECT i FROM InvitacionGral i
        JOIN FETCH i.perfilDestinatario perfil
        JOIN FETCH perfil.usuario
        JOIN FETCH i.proyecto proy
        WHERE proy.idProyecto = :idProyecto
          AND perfil.idPerfil = :idPerfil
          AND i.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<InvitacionGral> findPendientesDeProyectoYDestinatario(
            @Param("idProyecto") Long idProyecto,
            @Param("idPerfil") Long idPerfil);

    @Query("""
        SELECT i FROM InvitacionGral i
        JOIN FETCH i.perfilDestinatario perfil
        JOIN FETCH i.requerimientoGral r
        WHERE perfil.idPerfil = :idPerfil
          AND r.idRequerimientoGral IN :idsRequerimiento
          AND i.estado <> org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<InvitacionGral> findHistoricasPorDestinatarioYRequerimientos(
            @Param("idPerfil") Long idPerfil,
            @Param("idsRequerimiento") Collection<Long> idsRequerimiento);

    @Query("""
        SELECT i FROM InvitacionGral i
        JOIN FETCH i.perfilDestinatario perfil
        WHERE i.requerimientoGral.idRequerimientoGral = :idRequerimientoGral
          AND i.estado <> org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<InvitacionGral> findHistoricasPorRequerimientoGral(
            @Param("idRequerimientoGral") Long idRequerimientoGral);

    /**
     * Resuelve el proyecto de cada invitación general indicada. Se usa para
     * enriquecer las notificaciones sin persistir un dato derivable.
     */
    @Query("""
        SELECT i.idInvitacionGral AS entidadId,
               i.proyecto.idProyecto AS idProyecto
        FROM InvitacionGral i
        WHERE i.idInvitacionGral IN :ids
    """)
    List<ProyectoDeEntidad> findProyectoPorIds(@Param("ids") Collection<Long> ids);

    boolean existsByRequerimientoGral_IdRequerimientoGralAndEstado(
            Long idRequerimientoGral, EstadoSolicitud estado);

    boolean existsByRequerimientoGral_IdRequerimientoGral(Long idRequerimientoGral);

    boolean existsByProyecto_IdProyectoAndPerfilDestinatario_IdPerfilAndEstado(
            Long idProyecto, Long idPerfil, EstadoSolicitud estado);

    void deleteByRequerimientoGral_IdRequerimientoGral(Long idRequerimientoGral);

    /**
     * Devuelve los requerimientos generales del conjunto que tienen al menos
     * una invitación en el estado indicado. Se usa para calcular en bloque si
     * un requerimiento está protegido en UC-36.
     */
    @Query("""
        SELECT DISTINCT i.requerimientoGral.idRequerimientoGral
        FROM InvitacionGral i
        WHERE i.requerimientoGral.idRequerimientoGral IN :ids
          AND i.estado = :estado
    """)
    List<Long> findIdsRequerimientoGralPorEstado(
            @Param("ids") Collection<Long> ids,
            @Param("estado") EstadoSolicitud estado);
}
