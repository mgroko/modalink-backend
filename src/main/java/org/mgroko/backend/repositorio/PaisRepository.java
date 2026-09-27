package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Pais;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaisRepository extends JpaRepository<Pais, Long> {

    Optional<Pais> findByCodigoIso(String codigoIso);

    Optional<Pais> findByNombre(String nombre);
}
