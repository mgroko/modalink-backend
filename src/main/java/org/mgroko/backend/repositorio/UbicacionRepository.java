package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {
}
