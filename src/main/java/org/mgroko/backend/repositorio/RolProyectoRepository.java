package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.RolProyecto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolProyectoRepository extends JpaRepository<RolProyecto, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT r FROM RolProyecto r WHERE LOWER(r.nombre) LIKE LOWER(CONCAT(:nombre, '%')) OR LOWER(r.codigo) = LOWER(:nombre)")
    Optional<RolProyecto> findByNombre(@org.springframework.data.repository.query.Param("nombre") String nombre);

    Optional<RolProyecto> findByCodigo(String codigo);
}
