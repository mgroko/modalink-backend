package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
        WHERE per.usuario.idUsuario = :idUsuario
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
          AND a.planificacion.proyecto.estado IN :estados
    """)
    List<Actividad> findActividadesDeUsuario(
            @Param("idUsuario") Long idUsuario,
            @Param("estados") Collection<EstadoProyecto> estados);

    /**
     * Próxima actividad asignada al perfil en cada uno de los proyectos
     * indicados, ordenada por fecha de inicio: la primera fila de cada
     * proyecto es su próxima actividad pendiente (o en curso).
     *
     * @param idsProyectos proyectos a evaluar (los de la página actual)
     * @param idPerfil     perfil activo del usuario
     * @param ahora        corte temporal: solo actividades desde ese instante
     * @return actividades futuras pendientes/asignadas, ordenadas por inicio
     */
    @Query("""
        SELECT a FROM Actividad a
        JOIN AsignacionActividad asg ON asg.actividad = a
        JOIN asg.miembro m
        WHERE m.proyecto.idProyecto IN :idsProyectos
          AND m.perfil.idPerfil = :idPerfil
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
          AND a.fechaHoraInicio >= :ahora
          AND a.estado IN (org.mgroko.backend.modelo.enums.EstadoActividad.PENDIENTE,
                            org.mgroko.backend.modelo.enums.EstadoActividad.EN_CURSO)
        ORDER BY a.fechaHoraInicio ASC
    """)
    List<Actividad> findProximasActividadesAsignadas(
            @Param("idsProyectos") Collection<Long> idsProyectos,
            @Param("idPerfil") Long idPerfil,
            @Param("ahora") java.time.LocalDateTime ahora);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Actividad a
        SET a.estado = org.mgroko.backend.modelo.enums.EstadoActividad.CANCELADA
        WHERE a.planificacion.proyecto.idProyecto = :idProyecto
          AND a.estado IN (org.mgroko.backend.modelo.enums.EstadoActividad.PENDIENTE,
                            org.mgroko.backend.modelo.enums.EstadoActividad.EN_CURSO)
    """)
    int cancelarPendientesDeProyecto(@Param("idProyecto") Long idProyecto);
}