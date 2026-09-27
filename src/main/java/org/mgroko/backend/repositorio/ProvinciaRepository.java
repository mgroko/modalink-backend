package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Provincia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {

    Optional<Provincia> findByNombre(String nombre);

    Optional<Provincia> findByIdExterno(String idExterno);
}
