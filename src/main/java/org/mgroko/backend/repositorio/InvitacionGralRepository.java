package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitacionGralRepository extends JpaRepository<InvitacionGral, Long> {

    @Query("""
        SELECT i FROM InvitacionGral i
        JOIN FETCH i.perfilDestinatario perfil
        JOIN FETCH perfil.usuario
        WHERE i.requerimientoGral.proyecto.idProyecto = :idProyecto
          AND i.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<InvitacionGral> findPendientesDeProyectoConDestinatario(@Param("idProyecto") Long idProyecto);

    /**
     * Resuelve el proyecto de cada invitación general indicada. Se usa para
     * enriquecer las notificaciones sin persistir un dato derivable.
     */
    @Query("""
        SELECT i.idInvitacionGral AS entidadId,
               i.requerimientoGral.proyecto.idProyecto AS idProyecto
        FROM InvitacionGral i
        WHERE i.idInvitacionGral IN :ids
    """)
    List<ProyectoDeEntidad> findProyectoPorIds(@Param("ids") Collection<Long> ids);
}
