package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Provincia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {

    /**
     * Busca la provincia por su clave natural en el catálogo de origen, considering
     * el país para desambiguar homónimas entre catálogos diferentes. La identidad es
     * {@code (nombre, id_pais, fuente_api)}.
     */
    Optional<Provincia> findByNombreAndPais_IdPais(String nombre, Long idPais);

    /**
     * Busca la provincia por su clave natural en el catálogo de origen.
     * El nombre no sirve como clave porque se repite entre países, e incluso
     * dentro de un mismo país. La identidad es
     * {@code (id_externo, fuente_api)}.
     */
    Optional<Provincia> findByFuenteApiAndIdExterno(String fuenteApi, String idExterno);

    Optional<Provincia> findByIdExterno(String idExterno);
}
