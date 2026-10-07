package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.RolGlobal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolGlobalRepository extends JpaRepository<RolGlobal, Long> {

    Optional<RolGlobal> findByNombre(String nombre);

    Optional<RolGlobal> findByCodigo(String codigo);
}
