package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.repositorio.proyeccion.CubiertosPorRequerimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AsignacionActividadRepository extends JpaRepository<AsignacionActividad, Long> {

    @Query("""
        SELECT a FROM AsignacionActividad a
        JOIN FETCH a.miembro m
        JOIN FETCH m.perfil p
        JOIN FETCH p.usuario
        JOIN FETCH p.profesion
        WHERE a.actividad.idActividad = :idActividad
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    List<AsignacionActividad> findActivasByActividadId(@Param("idActividad") Long idActividad);

    @Query("""
        SELECT a FROM AsignacionActividad a
        JOIN FETCH a.miembro m
        JOIN FETCH m.perfil p
        JOIN FETCH p.usuario
        JOIN FETCH p.profesion
        WHERE a.actividad.idActividad IN :idsActividades
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    List<AsignacionActividad> findActivasByActividadIds(
            @Param("idsActividades") Collection<Long> idsActividades);

    /**
     * Actividades con al menos una asignación de un miembro activo sobre un
     * requerimiento activo o una asignación directa sin requerimiento.
     */
    @Query("""
        SELECT DISTINCT aa.actividad.idActividad
        FROM AsignacionActividad aa
        JOIN aa.miembro m
        LEFT JOIN aa.requerimientoActividad r
        WHERE aa.actividad.idActividad IN :idsActividades
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
          AND (r IS NULL OR r.fechaArchivado IS NULL)
    """)
    Set<Long> findIdsActividadesConAsignacionesActivas(
            @Param("idsActividades") Collection<Long> idsActividades);

    @Query("""
        SELECT CASE WHEN COUNT(aa) > 0 THEN TRUE ELSE FALSE END
        FROM AsignacionActividad aa
        JOIN aa.miembro m
        LEFT JOIN aa.requerimientoActividad r
        WHERE aa.actividad.idActividad = :idActividad
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
          AND (r IS NULL OR r.fechaArchivado IS NULL)
    """)
    boolean existsAsignacionActiva(@Param("idActividad") Long idActividad);

    /**
     * Perfiles del usuario indicado asignados a las actividades indicadas.
     * Se usa para identificar de qué perfil proviene cada bloqueo calculado
     * del calendario cuando el usuario tiene más de uno.
     */
    @Query("""
        SELECT a FROM AsignacionActividad a
        JOIN FETCH a.actividad act
        JOIN FETCH a.miembro m
        JOIN FETCH m.perfil p
        WHERE act.idActividad IN :idsActividades
          AND p.usuario.idUsuario = :idUsuario
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    List<AsignacionActividad> findPerfilesDeUsuarioPorActividades(
            @Param("idsActividades") Collection<Long> idsActividades,
            @Param("idUsuario") Long idUsuario);

    @Query("""
        SELECT a FROM AsignacionActividad a
        JOIN FETCH a.miembro m
        JOIN FETCH m.perfil p
        JOIN FETCH p.usuario
        JOIN FETCH p.profesion
        WHERE a.actividad.idActividad = :idActividad
    """)
    List<AsignacionActividad> findByActividadIdConMiembroPerfil(
            @Param("idActividad") Long idActividad);

    /**
     * Asignaciones de las actividades indicadas, incluyendo miembros que ya no
     * están activos. Se usa para el historial de actividades en curso o
     * cerradas, donde se debe conservar quién participó.
     */
    @Query("""
        SELECT a FROM AsignacionActividad a
        JOIN FETCH a.actividad act
        JOIN FETCH a.miembro m
        JOIN FETCH m.perfil p
        JOIN FETCH p.usuario
        JOIN FETCH p.profesion
        WHERE a.actividad.idActividad IN :idsActividades
    """)
    List<AsignacionActividad> findByActividadIdsConMiembroPerfil(
            @Param("idsActividades") Collection<Long> idsActividades);

    @Query("""
        SELECT a FROM AsignacionActividad a
        JOIN FETCH a.miembro m
        JOIN FETCH m.perfil p
        JOIN FETCH p.usuario
        JOIN FETCH p.profesion
        WHERE a.actividad.idActividad = :idActividad
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
          AND NOT EXISTS (
              SELECT 1 FROM AsignacionActividad otra
              JOIN otra.actividad otraActividad
              JOIN otraActividad.planificacion otraPlanificacion
              WHERE otra.miembro.idMiembro = a.miembro.idMiembro
                AND otraActividad.idActividad <> :idActividad
                AND otraPlanificacion.proyecto.idProyecto = :idProyecto
                AND otraActividad.estado <> org.mgroko.backend.modelo.enums.EstadoActividad.CANCELADA
          )
    """)
    List<AsignacionActividad> findExclusivasByActividadIdAndProyecto(
            @Param("idActividad") Long idActividad,
            @Param("idProyecto") Long idProyecto);

    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM AsignacionActividad a
        WHERE a.actividad.idActividad = :idActividad
          AND a.miembro.perfil.idPerfil = :idPerfil
    """)
    int deleteByActividadIdAndPerfil(
            @Param("idActividad") Long idActividad,
            @Param("idPerfil") Long idPerfil);

    boolean existsByMiembro_IdMiembroAndActividad_IdActividad(
            Long idMiembro, Long idActividad);

    /**
     * Recupera una asignación bloqueada para desasignarla. Incluye miembro,
     * perfil, usuario, actividad, planificación, proyecto y requerimiento para
     * validar pertenencia y notificar sin consultas perezosas adicionales.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM AsignacionActividad a
        JOIN FETCH a.miembro m
        JOIN FETCH m.perfil p
        LEFT JOIN FETCH p.usuario
        LEFT JOIN FETCH p.profesion
        JOIN FETCH a.actividad act
        JOIN FETCH act.planificacion pl
        JOIN FETCH pl.proyecto pr
        LEFT JOIN FETCH a.requerimientoActividad
        WHERE a.idAsignacionAct = :idAsignacion
    """)
    Optional<AsignacionActividad> findByIdParaDesasignar(
            @Param("idAsignacion") Long idAsignacion);

    @Query("""
        SELECT COUNT(aa) FROM AsignacionActividad aa
        JOIN aa.miembro mp
        WHERE aa.requerimientoActividad.idRequerimiento = :idRequerimiento
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    long countActivasPorRequerimientoActividad(
            @Param("idRequerimiento") Long idRequerimiento);

    /**
     * Cuenta todas las asignaciones de un requerimiento y de sus snapshots,
     * sin importar el estado de participación del miembro. Se usa para el cupo
     * histórico de actividades finalizadas o canceladas.
     */
    @Query("""
        SELECT COUNT(aa) FROM AsignacionActividad aa
        WHERE aa.requerimientoActividad.idRequerimiento = :idRequerimiento
           OR aa.requerimientoActividad.idRequerimientoOrigen = :idRequerimiento
    """)
    long countTodasPorRequerimientoActividad(
            @Param("idRequerimiento") Long idRequerimiento);

    boolean existsByRequerimientoActividad_IdRequerimiento(Long idRequerimiento);

    /**
     * Asignaciones de una membresía concreta, incluyendo las que ya apuntan a
     * snapshots. Se usa al dar de baja para repuntar el historial.
     */
    @Query("""
        SELECT a FROM AsignacionActividad a
        JOIN FETCH a.miembro m
        JOIN FETCH a.actividad act
        LEFT JOIN FETCH a.requerimientoActividad r
        LEFT JOIN FETCH r.profesion
        WHERE m.idMiembro = :idMiembro
        ORDER BY act.nombre ASC, a.idAsignacionAct ASC
    """)
    List<AsignacionActividad> findByMiembroConActividadYRequerimiento(
            @Param("idMiembro") Long idMiembro);

    @Query("""
        SELECT a FROM AsignacionActividad a
        JOIN FETCH a.miembro m
        LEFT JOIN FETCH a.requerimientoActividad r
        WHERE r.idRequerimiento = :idRequerimiento
    """)
    List<AsignacionActividad> findByRequerimientoActividad_IdRequerimiento(
            @Param("idRequerimiento") Long idRequerimiento);

    /**
     * Asignaciones de miembros históricos para la auditoría de bajas.
     */
    @Query("""
        SELECT a FROM AsignacionActividad a
        JOIN FETCH a.miembro m
        JOIN FETCH a.actividad act
        LEFT JOIN FETCH a.requerimientoActividad r
        LEFT JOIN FETCH r.profesion
        WHERE m.idMiembro IN :idsMiembros
        ORDER BY act.nombre ASC, a.idAsignacionAct ASC
    """)
    List<AsignacionActividad> findHistoricasPorMiembrosConActividadYRequerimiento(
            @Param("idsMiembros") Collection<Long> idsMiembros);

    /**
     * Cuenta las asignaciones activas de cada requerimiento indicado en una
     * sola consulta. Los requerimientos sin asignaciones no aparecen en el
     * resultado; el consumidor debe asumir cero.
     */
    @Query("""
        SELECT aa.requerimientoActividad.idRequerimiento AS idRequerimiento,
               COUNT(aa) AS cubiertos
        FROM AsignacionActividad aa
        JOIN aa.miembro m
        WHERE aa.requerimientoActividad.idRequerimiento IN :ids
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
        GROUP BY aa.requerimientoActividad.idRequerimiento
    """)
    List<CubiertosPorRequerimiento> countActivasPorRequerimientoIds(
            @Param("ids") Collection<Long> ids);

    /**
     * Cuenta todas las asignaciones de cada requerimiento indicado y de sus
     * snapshots, sin importar el estado del miembro. Se usa para el cupo
     * histórico de actividades finalizadas o canceladas.
     */
    @Query("""
        SELECT COALESCE(aa.requerimientoActividad.idRequerimientoOrigen,
                        aa.requerimientoActividad.idRequerimiento) AS idRequerimiento,
               COUNT(aa) AS cubiertos
        FROM AsignacionActividad aa
        WHERE COALESCE(aa.requerimientoActividad.idRequerimientoOrigen,
                       aa.requerimientoActividad.idRequerimiento) IN :ids
        GROUP BY COALESCE(aa.requerimientoActividad.idRequerimientoOrigen,
                          aa.requerimientoActividad.idRequerimiento)
    """)
    List<CubiertosPorRequerimiento> countTodasPorRequerimientoIds(
            @Param("ids") Collection<Long> ids);

    @Query("""
        SELECT aa FROM AsignacionActividad aa
        JOIN FETCH aa.miembro mp
        JOIN FETCH aa.requerimientoActividad ra
        JOIN FETCH ra.actividad a
        JOIN FETCH a.planificacion pl
        WHERE pl.proyecto.idProyecto = :idProyecto
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    List<AsignacionActividad> findActivasDeProyectoConRequerimiento(
            @Param("idProyecto") Long idProyecto);

    /**
     * Proyectos publicados o confirmados en los que el usuario tiene al menos
     * una asignación activa sobre una actividad no cancelada. Se usa para
     * detectar el impacto de un cambio de disponibilidad.
     */
    @Query("""
        SELECT DISTINCT pl.proyecto.idProyecto
        FROM AsignacionActividad aa
        JOIN aa.miembro m
        JOIN m.perfil p
        JOIN aa.actividad a
        JOIN a.planificacion pl
        WHERE p.usuario.idUsuario = :idUsuario
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
          AND pl.proyecto.estado IN (
              org.mgroko.backend.modelo.enums.EstadoProyecto.PUBLICADO,
              org.mgroko.backend.modelo.enums.EstadoProyecto.CONFIRMADO)
          AND a.estado <> org.mgroko.backend.modelo.enums.EstadoActividad.CANCELADA
          AND a.fechaHoraInicio IS NOT NULL
        ORDER BY pl.proyecto.idProyecto
    """)
    List<Long> findProyectosProgramablesDeUsuario(
            @Param("idUsuario") Long idUsuario);

    /**
     * Asignaciones activas del usuario en otros proyectos cuya actividad
     * todavía no tiene horario persistido. Se usan para advertir un posible
     * sobrecompromiso al aceptar una postulación.
     */
    @Query("""
        SELECT aa FROM AsignacionActividad aa
        JOIN FETCH aa.miembro m
        JOIN FETCH m.perfil p
        JOIN FETCH p.usuario u
        JOIN FETCH aa.actividad a
        JOIN FETCH a.planificacion pl
        JOIN FETCH pl.proyecto pr
        WHERE u.idUsuario = :idUsuario
          AND m.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
          AND a.fechaHoraInicio IS NULL
          AND a.estado IN (org.mgroko.backend.modelo.enums.EstadoActividad.PENDIENTE,
                            org.mgroko.backend.modelo.enums.EstadoActividad.EN_CURSO)
          AND pr.estado IN (org.mgroko.backend.modelo.enums.EstadoProyecto.PUBLICADO,
                             org.mgroko.backend.modelo.enums.EstadoProyecto.CONFIRMADO)
          AND pr.idProyecto <> :idProyecto
          AND a.idActividad <> :idActividad
    """)
    List<AsignacionActividad> findActivasSinHorarioDeUsuarioEnOtrosProyectos(
            @Param("idUsuario") Long idUsuario,
            @Param("idProyecto") Long idProyecto,
            @Param("idActividad") Long idActividad);
}
