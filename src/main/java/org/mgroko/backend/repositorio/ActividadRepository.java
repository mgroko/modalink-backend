package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActividadRepository extends JpaRepository<Actividad, Long>, JpaSpecificationExecutor<Actividad> {

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
          AND a.estado IN (org.mgroko.backend.modelo.enums.EstadoActividad.PENDIENTE,
                            org.mgroko.backend.modelo.enums.EstadoActividad.EN_CURSO)
          AND a.fechaHoraInicio IS NOT NULL
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

    /**
     * Carga, para las actividades indicadas, sus predecesoras en una sola
     * consulta. Se usa en la búsqueda de actividades (UC-52) para evitar N+1.
     *
     * @param idsActividades actividades de la página actual
     * @return actividades con su colección de predecesoras inicializada
     */
    @Query("""
        SELECT DISTINCT a FROM Actividad a
        LEFT JOIN FETCH a.predecesoras
        WHERE a.idActividad IN :idsActividades
    """)
    List<Actividad> findConPredecesoras(
            @Param("idsActividades") Collection<Long> idsActividades);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Actividad a
        SET a.estado = org.mgroko.backend.modelo.enums.EstadoActividad.CANCELADA
        WHERE a.planificacion.proyecto.idProyecto = :idProyecto
          AND a.estado IN (org.mgroko.backend.modelo.enums.EstadoActividad.PENDIENTE,
                            org.mgroko.backend.modelo.enums.EstadoActividad.EN_CURSO)
    """)
    int cancelarPendientesDeProyecto(@Param("idProyecto") Long idProyecto);

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Actividad a
        WHERE a.planificacion.idPlanificacion = :idPlanificacion
          AND LOWER(TRIM(a.nombre)) = LOWER(TRIM(:nombre))
    """)
    boolean existeNombreEnPlanificacion(
            @Param("idPlanificacion") Long idPlanificacion,
            @Param("nombre") String nombre);

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Actividad a
        WHERE a.planificacion.idPlanificacion = :idPlanificacion
          AND LOWER(TRIM(a.nombre)) = LOWER(TRIM(:nombre))
          AND a.idActividad <> :idActividad
    """)
    boolean existeNombreEnPlanificacionExcluyendo(
            @Param("idPlanificacion") Long idPlanificacion,
            @Param("nombre") String nombre,
            @Param("idActividad") Long idActividad);

    @Query("""
        SELECT a FROM Actividad a
        WHERE a.idActividad = :idActividad
          AND a.planificacion.idPlanificacion = :idPlanificacion
    """)
    java.util.Optional<Actividad> findByIdActividadAndPlanificacionIdPlanificacion(
            @Param("idActividad") Long idActividad,
            @Param("idPlanificacion") Long idPlanificacion);

    @Query("""
        SELECT DISTINCT a FROM Actividad a
        LEFT JOIN FETCH a.predecesoras
        WHERE a.planificacion.idPlanificacion = :idPlanificacion
    """)
    List<Actividad> findConPredecesorasByPlanificacion(
            @Param("idPlanificacion") Long idPlanificacion);

    @Query("""
        SELECT a FROM Actividad a
        WHERE a.idActividad IN :idsActividades
          AND a.planificacion.idPlanificacion = :idPlanificacion
    """)
    List<Actividad> findByIdsAndPlanificacion(
            @Param("idsActividades") Collection<Long> idsActividades,
            @Param("idPlanificacion") Long idPlanificacion);

    @Modifying(flushAutomatically = true)
    @Query(value = """
        DELETE FROM dependencia_actividades
        WHERE id_actividad_sucesora = :idActividad
           OR id_actividad_predecesora = :idActividad
        """, nativeQuery = true)
    int deleteDependenciasByActividadId(@Param("idActividad") Long idActividad);

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM hito_actividad WHERE id_actividad = :idActividad",
            nativeQuery = true)
    int deleteHitoActividadByActividadId(@Param("idActividad") Long idActividad);
}