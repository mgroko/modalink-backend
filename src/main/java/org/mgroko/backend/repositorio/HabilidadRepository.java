package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Habilidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabilidadRepository extends JpaRepository<Habilidad, Long> {

    Optional<Habilidad> findByCodigoIgnoreCase(String codigo);
}
