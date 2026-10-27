package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.AsignacionActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
        WHERE a.actividad.idActividad = :idActividad
    """)
    List<AsignacionActividad> findByActividadIdConMiembroPerfil(
            @Param("idActividad") Long idActividad);

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
              WHERE otra.miembro.idMiembro = a.miembro.idMiembro
                AND otra.actividad.idActividad <> :idActividad
          )
    """)
    List<AsignacionActividad> findExclusivasByActividadId(
            @Param("idActividad") Long idActividad);

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

    @Query("""
        SELECT COUNT(aa) FROM AsignacionActividad aa
        JOIN aa.miembro mp
        WHERE aa.requerimientoActividad.idRequerimiento = :idRequerimiento
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    long countActivasPorRequerimientoActividad(
            @Param("idRequerimiento") Long idRequerimiento);

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
}
