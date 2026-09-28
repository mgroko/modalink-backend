package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Pais;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaisRepository extends JpaRepository<Pais, Long> {

    Optional<Pais> findByCodigoIso(String codigoIso);

    /**
     * Busca un país por su código ISO que esté activo. Un país desactivado no
     * debe recibir provincias ni ciudades nuevas, por eso la búsqueda de
     * catálogo exige {@code activo = true}.
     */
    Optional<Pais> findByCodigoIsoAndActivoTrue(String codigoIso);

    Optional<Pais> findByNombre(String nombre);
}
