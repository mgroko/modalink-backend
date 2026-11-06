package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.repositorio.proyeccion.PlanificacionEntregaProyeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PlanificacionRepository extends JpaRepository<Planificacion, Long> {

    Optional<Planificacion> findByProyectoIdProyecto(Long idProyecto);

    /**
     * Fecha de entrega de la planificación de varios proyectos en una sola
     * consulta.
     */
    @Query("""
        SELECT p.proyecto.idProyecto AS idProyecto,
               p.fechaEntrega AS fechaEntrega
        FROM Planificacion p
        WHERE p.proyecto.idProyecto IN :idsProyectos
    """)
    List<PlanificacionEntregaProyeccion> findFechasEntregaPorProyectoIds(
            @Param("idsProyectos") Collection<Long> idsProyectos);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM Planificacion p
        WHERE p.proyecto.idProyecto = :idProyecto
    """)
    Optional<Planificacion> findByProyectoIdProyectoForUpdate(
            @Param("idProyecto") Long idProyecto);
}
