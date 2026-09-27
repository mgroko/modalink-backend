package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CiudadRepository extends JpaRepository<Ciudad, Long> {

    Optional<Ciudad> findByNombreAndProvincia_IdProvincia(String nombre, Long idProvincia);

    Optional<Ciudad> findByIdExterno(String idExterno);
}
