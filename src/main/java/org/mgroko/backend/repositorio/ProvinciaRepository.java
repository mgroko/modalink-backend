package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Provincia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {

    Optional<Provincia> findByNombre(String nombre);

    /**
     * Busca la provincia por su clave natural en el catálogo de origen. Es la
     * única clave que no depende del nombre, ya que el nombre de una provincia
     * se repite entre países. El índice único
     * {@code UQ_provincia_id_externo_y_api} sobre {@code (id_externo, fuente_api)}
     * garantiza que esa clave identifique una sola fila.
     */
    Optional<Provincia> findByFuenteApiAndIdExterno(String fuenteApi, String idExterno);

    Optional<Provincia> findByIdExterno(String idExterno);
}
