package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RequerimientoActividadRepository
        extends JpaRepository<RequerimientoActividad, Long>,
                JpaSpecificationExecutor<RequerimientoActividad> {

    boolean existsByHabilidades_IdHabilidad(Long idHabilidad);

    @Query("""
        SELECT r FROM RequerimientoActividad r
        JOIN FETCH r.actividad
        WHERE r.actividad.idActividad = :idActividad
          AND r.fechaArchivado IS NULL
    """)
    List<RequerimientoActividad> findByActividad_IdActividad(
            @Param("idActividad") Long idActividad);

    /**
     * Incluye snapshots históricos. Se usa únicamente para la limpieza física
     * de una actividad, no para consultas operativas.
     */
    @Query("""
        SELECT r FROM RequerimientoActividad r
        JOIN FETCH r.actividad
        WHERE r.actividad.idActividad = :idActividad
    """)
    List<RequerimientoActividad> findByActividad_IdActividadIncludingArchived(
            @Param("idActividad") Long idActividad);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM RequerimientoActividad r
        WHERE r.actividad.idActividad = :idActividad
          AND r.fechaArchivado IS NULL
    """)
    List<RequerimientoActividad> findByActividadIdForUpdate(
            @Param("idActividad") Long idActividad);

    /**
     * Recupera los requerimientos de una actividad con su profesión y
     * actividad inicializadas. Se usa en UC-70 para resolver si el miembro
     * asignado cubre alguno de los requerimientos de la actividad.
     *
     * @param idActividad actividad cuyos requerimientos se quieren listar
     * @return requerimientos ordenados por identificador
     */
    @Query("""
        SELECT r FROM RequerimientoActividad r
        JOIN FETCH r.profesion
        JOIN FETCH r.actividad
        WHERE r.actividad.idActividad = :idActividad
          AND r.fechaArchivado IS NULL
        ORDER BY r.idRequerimiento ASC
    """)
    List<RequerimientoActividad> findConProfesionByActividadId(
            @Param("idActividad") Long idActividad);

    /**
     * Recupera los requerimientos de las actividades indicadas junto con su
     * profesión en una sola consulta, para armar el resumen de UC-52 sin N+1.
     *
     * @param idsActividades actividades de la página actual
     * @return requerimientos ordenados por actividad e identificador
     */
    @Query("""
        SELECT DISTINCT r FROM RequerimientoActividad r
        JOIN FETCH r.profesion
        JOIN FETCH r.actividad
        LEFT JOIN FETCH r.caracteristicas caracteristica
        LEFT JOIN FETCH caracteristica.caracteristica
        LEFT JOIN FETCH r.habilidades
        WHERE r.actividad.idActividad IN :idsActividades
          AND r.fechaArchivado IS NULL
        ORDER BY r.actividad.idActividad ASC, r.idRequerimiento ASC
    """)
    List<RequerimientoActividad> findConProfesionByActividadIds(
            @Param("idsActividades") Collection<Long> idsActividades);

    /**
     * Indica si el proyecto tiene al menos un requerimiento de actividad en
     * actividades que no se encuentren en el estado excluido. Se usa para
     * validar la informacion basica exigida al publicar o modificar un
     * proyecto.
     *
     * @param idProyecto     proyecto a evaluar
     * @param estadoExcluido estado de actividad que no debe computarse
     * @return true si existe al menos un requerimiento computable
     */
    @Query("""
        SELECT CASE WHEN COUNT(ra) > 0 THEN TRUE ELSE FALSE END
        FROM RequerimientoActividad ra
        WHERE ra.actividad.planificacion.proyecto.idProyecto = :idProyecto
          AND ra.actividad.estado <> :estadoExcluido
          AND ra.fechaArchivado IS NULL
    """)
    boolean existsByProyectoExcluyendoEstado(
            @Param("idProyecto") Long idProyecto,
            @Param("estadoExcluido") EstadoActividad estadoExcluido);

    /**
     * Recupera un requerimiento de actividad junto con su profesión, la
     * actividad, la planificación y el proyecto. Se usa al dar de alta una
     * postulación (UC-26) para validar profesión, estado del proyecto y
     * disponibilidad sin disparar consultas perezosas adicionales.
     *
     * @param idRequerimiento identificador del requerimiento
     * @return el requerimiento con sus relaciones principales inicializadas
     */
    @Query("""
        SELECT r FROM RequerimientoActividad r
        JOIN FETCH r.profesion
        JOIN FETCH r.actividad a
        JOIN FETCH a.planificacion p
        JOIN FETCH p.proyecto
        WHERE r.idRequerimiento = :idRequerimiento
          AND r.fechaArchivado IS NULL
    """)
    Optional<RequerimientoActividad> findByIdConActividadYProyecto(
            @Param("idRequerimiento") Long idRequerimiento);

    /**
     * Recupera requerimientos con actividad, proyecto, profesión,
     * características y habilidades inicializadas en lote. Se usa en UC-73.
     */
    @Query("""
        SELECT DISTINCT r FROM RequerimientoActividad r
        JOIN FETCH r.profesion
        JOIN FETCH r.actividad a
        JOIN FETCH a.planificacion p
        JOIN FETCH p.proyecto
        LEFT JOIN FETCH r.caracteristicas c
        LEFT JOIN FETCH c.caracteristica
        LEFT JOIN FETCH r.habilidades
        WHERE r.idRequerimiento IN :ids
          AND r.fechaArchivado IS NULL
        ORDER BY r.idRequerimiento ASC
    """)
    List<RequerimientoActividad> findConDetalleByIds(
            @Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM RequerimientoActividad r
        JOIN FETCH r.profesion
        JOIN FETCH r.actividad actividad
        JOIN FETCH actividad.planificacion planificacion
        JOIN FETCH planificacion.proyecto
        WHERE r.idRequerimiento = :idRequerimiento
          AND r.fechaArchivado IS NULL
    """)
    Optional<RequerimientoActividad> findByIdParaGestion(
            @Param("idRequerimiento") Long idRequerimiento);

    /**
     * Último snapshot de un requerimiento activo. Se usa para reutilizarlo
     * cuando el activo no cambió.
     */
    @EntityGraph(attributePaths = {
            "actividad",
            "profesion",
            "habilidades",
            "caracteristicas",
            "caracteristicas.caracteristica"
    })
    Optional<RequerimientoActividad> findFirstByIdRequerimientoOrigenOrderByFechaArchivadoDesc(
            @Param("idRequerimientoOrigen") Long idRequerimientoOrigen);
}
