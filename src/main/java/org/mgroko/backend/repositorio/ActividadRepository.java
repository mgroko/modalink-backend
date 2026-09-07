package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    /**
     * Devuelve las actividades de los proyectos activos a las que está
     * asignado un usuario, a través de todos sus perfiles y membresías
     * activas. Estas actividades determinan los bloqueos calculados del
     * calendario del usuario (no persistidos en bloqueo_agenda).
     *
     * @param idUsuario el ID del usuario
     * @param estados   estados de proyecto considerados activos
     * @return actividades asignadas al usuario en proyectos activos
     */
    @Query("""
        SELECT DISTINCT a
        FROM Actividad a
        JOIN AsignacionActividad asg ON asg.actividad = a
        JOIN asg.miembro m
        JOIN m.perfil per
        WHERE per.usuario.id = :idUsuario
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.Activo
          AND a.planificacion.proyecto.estado IN :estados
    """)
    List<Actividad> findActividadesDeUsuario(
            @Param("idUsuario") Long idUsuario,
            @Param("estados") Collection<EstadoProyecto> estados);
}