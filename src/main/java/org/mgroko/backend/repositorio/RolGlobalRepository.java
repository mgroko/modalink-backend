package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.RolGlobal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolGlobalRepository extends JpaRepository<RolGlobal, Long> {

    /**
     * Busca un rol global por su nombre.
     *
     * @param nombre el nombre del rol (ej: "Usuario", "Administrador")
     * @return un Optional con el RolGlobal si existe, vacío en caso contrario
     */
    Optional<RolGlobal> findByNombre(String nombre);
}
