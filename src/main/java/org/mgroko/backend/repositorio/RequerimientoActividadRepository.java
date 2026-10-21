package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.RequerimientoActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequerimientoActividadRepository extends JpaRepository<RequerimientoActividad, Long> {

    /**
     * Recupera los requerimientos de las actividades indicadas junto con su
     * profesión en una sola consulta, para armar el resumen de UC-52 sin N+1.
     *
     * @param idsActividades actividades de la página actual
     * @return requerimientos ordenados por actividad e identificador
     */
    @Query("""
        SELECT r FROM RequerimientoActividad r
        JOIN FETCH r.profesion
        WHERE r.actividad.idActividad IN :idsActividades
        ORDER BY r.actividad.idActividad ASC, r.idRequerimiento ASC
    """)
    List<RequerimientoActividad> findConProfesionByActividadIds(
            @Param("idsActividades") Collection<Long> idsActividades);
}
