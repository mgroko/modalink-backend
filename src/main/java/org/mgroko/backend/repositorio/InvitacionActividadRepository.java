package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.InvitacionActividad;
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
}
