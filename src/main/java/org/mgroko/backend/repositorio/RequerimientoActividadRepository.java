package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RequerimientoActividadRepository extends JpaRepository<RequerimientoActividad, Long> {

    List<RequerimientoActividad> findByActividad_IdActividad(Long idActividad);

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
    """)
    Optional<RequerimientoActividad> findByIdConActividadYProyecto(
            @Param("idRequerimiento") Long idRequerimiento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM RequerimientoActividad r
        JOIN FETCH r.profesion
        JOIN FETCH r.actividad actividad
        JOIN FETCH actividad.planificacion planificacion
        JOIN FETCH planificacion.proyecto
        WHERE r.idRequerimiento = :idRequerimiento
    """)
    Optional<RequerimientoActividad> findByIdParaGestion(
            @Param("idRequerimiento") Long idRequerimiento);
}
