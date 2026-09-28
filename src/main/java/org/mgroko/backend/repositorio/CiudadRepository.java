package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CiudadRepository extends JpaRepository<Ciudad, Long> {

    Optional<Ciudad> findByNombreAndProvincia_IdProvincia(String nombre, Long idProvincia);

    /**
     * Busca la ciudad por su clave natural en el catálogo de origen. El nombre
     * no sirve como clave porque se repite entre provincias y países, e incluso
     * dentro de una misma provincia. El índice único
     * {@code UQ_ciudad_id_externo_y_api} sobre {@code (id_externo, fuente_api)}
     * garantiza que esa clave identifique una sola fila.
     */
    Optional<Ciudad> findByFuenteApiAndIdExterno(String fuenteApi, String idExterno);

    Optional<Ciudad> findByIdExterno(String idExterno);
}
