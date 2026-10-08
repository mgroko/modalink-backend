package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequerimientoGralProyectoRepository extends JpaRepository<RequerimientoGralProyecto, Long> {

    List<RequerimientoGralProyecto> findByProyectoIdProyecto(Long idProyecto);
}
