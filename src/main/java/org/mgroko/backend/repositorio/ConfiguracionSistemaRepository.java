package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistema, String> {
    Optional<ConfiguracionSistema> findByClave(String clave);
}
