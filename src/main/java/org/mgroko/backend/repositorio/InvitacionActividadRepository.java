package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitacionActividadRepository extends JpaRepository<InvitacionActividad, Long> {

    boolean existsByRequerimiento_IdRequerimiento(Long idRequerimiento);

    boolean existsByRequerimiento_IdRequerimientoAndEstado(
            Long idRequerimiento, EstadoSolicitud estado);

    /**
     * Indica si el perfil ya tiene una invitación de actividad en el estado
     * indicado para cualquier requerimiento del proyecto.
     */
    @Query("""
        SELECT COUNT(i) > 0 FROM InvitacionActividad i
        WHERE i.requerimiento.actividad.planificacion.proyecto.idProyecto = :idProyecto
          AND i.perfilDestinatario.idPerfil = :idPerfil
          AND i.estado = :estado
    """)
    boolean existsDeProyectoYDestinatarioEnEstado(
            @Param("idProyecto") Long idProyecto,
            @Param("idPerfil") Long idPerfil,
            @Param("estado") EstadoSolicitud estado);

    void deleteByRequerimiento_IdRequerimiento(Long idRequerimiento);

    @Query("""
        SELECT i FROM InvitacionActividad i
        JOIN FETCH i.perfilDestinatario perfil
        JOIN FETCH perfil.usuario
        WHERE i.requerimiento.actividad.planificacion.proyecto.idProyecto = :idProyecto
          AND i.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<InvitacionActividad> findPendientesDeProyectoConDestinatario(@Param("idProyecto") Long idProyecto);

    @Query("""
        SELECT i FROM InvitacionActividad i
        JOIN FETCH i.perfilDestinatario perfil
        JOIN FETCH perfil.usuario
        JOIN FETCH i.requerimiento r
        JOIN FETCH r.actividad a
        JOIN FETCH a.planificacion pl
        JOIN FETCH pl.proyecto proy
        WHERE proy.idProyecto = :idProyecto
          AND perfil.idPerfil = :idPerfil
          AND i.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<InvitacionActividad> findPendientesDeProyectoYDestinatario(
            @Param("idProyecto") Long idProyecto,
            @Param("idPerfil") Long idPerfil);

    @Query("""
        SELECT i FROM InvitacionActividad i
        JOIN FETCH i.perfilDestinatario perfil
        JOIN FETCH i.requerimiento r
        WHERE perfil.idPerfil = :idPerfil
          AND r.idRequerimiento IN :idsRequerimiento
          AND i.estado <> org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<InvitacionActividad> findHistoricasPorDestinatarioYRequerimientos(
            @Param("idPerfil") Long idPerfil,
            @Param("idsRequerimiento") Collection<Long> idsRequerimiento);

    @Query("""
        SELECT i FROM InvitacionActividad i
        JOIN FETCH i.perfilDestinatario perfil
        WHERE i.requerimiento.idRequerimiento = :idRequerimiento
          AND i.estado <> org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<InvitacionActividad> findHistoricasPorRequerimiento(
            @Param("idRequerimiento") Long idRequerimiento);

    @Query("""
        SELECT i FROM InvitacionActividad i
        JOIN FETCH i.perfilDestinatario perfil
        JOIN FETCH perfil.usuario
        WHERE i.requerimiento.actividad.idActividad = :idActividad
    """)
    List<InvitacionActividad> findByActividadIdConDestinatario(
            @Param("idActividad") Long idActividad);

    /**
     * Resuelve el proyecto de cada invitación de actividad indicada. Se usa
     * para enriquecer las notificaciones sin persistir un dato derivable.
     */
    @Query("""
        SELECT i.idInvitacion AS entidadId,
               i.requerimiento.actividad.planificacion.proyecto.idProyecto AS idProyecto
        FROM InvitacionActividad i
        WHERE i.idInvitacion IN :ids
    """)
    List<ProyectoDeEntidad> findProyectoPorIds(@Param("ids") Collection<Long> ids);
}
