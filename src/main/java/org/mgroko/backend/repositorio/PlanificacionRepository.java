package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Planificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PlanificacionRepository extends JpaRepository<Planificacion, Long> {

    Optional<Planificacion> findByProyectoIdProyecto(Long idProyecto);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM Planificacion p
        WHERE p.proyecto.idProyecto = :idProyecto
    """)
    Optional<Planificacion> findByProyectoIdProyectoForUpdate(
            @Param("idProyecto") Long idProyecto);
}
