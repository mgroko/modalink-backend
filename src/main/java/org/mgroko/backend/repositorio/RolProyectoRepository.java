package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.RolProyecto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolProyectoRepository extends JpaRepository<RolProyecto, Long> {

    Optional<RolProyecto> findByNombre(String nombre);
}
