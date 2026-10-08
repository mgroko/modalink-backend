package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Planificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanificacionRepository extends JpaRepository<Planificacion, Long> {

    Optional<Planificacion> findByProyectoIdProyecto(Long idProyecto);
}
