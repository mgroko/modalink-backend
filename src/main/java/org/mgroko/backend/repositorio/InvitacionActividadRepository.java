package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitacionActividadRepository extends JpaRepository<InvitacionActividad, Long> {

    boolean existsByRequerimiento_IdRequerimiento(Long idRequerimiento);

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
