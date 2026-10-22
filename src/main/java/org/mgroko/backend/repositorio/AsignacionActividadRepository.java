package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.AsignacionActividad;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
