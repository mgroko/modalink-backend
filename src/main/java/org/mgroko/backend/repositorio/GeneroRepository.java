package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Genero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneroRepository extends JpaRepository<Genero, Long> {

    /**
     * Busca un género por su código.
     *
     * @param codigo el código del género (ej: "mujer", "hombre", "no_binario", "no_decirlo")
     * @return un Optional con el Genero si existe, vacío en caso contrario
     */
    Optional<Genero> findByCodigo(String codigo);
}
