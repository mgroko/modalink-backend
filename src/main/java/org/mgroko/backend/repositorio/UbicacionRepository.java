package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    /**
     * Devuelve la ubicación asociada a una ciudad, si ya existe.
     *
     * <p>La ciudad se localiza por su clave natural {@code (id_externo, fuente_api)}
     * en {@code CiudadRepository}, nunca por nombre: el nombre de una ciudad se
     * repite entre países y entre catálogos, y buscar por nombre podía devolver
     * la fila de otra provincia, de otro país o de otra fuente.</p>
     *
     * @param idCiudad id de la ciudad
     * @return la Ubicacion de esa ciudad, o vacío si la ciudad todavía no tiene una
     */
    Optional<Ubicacion> findByCiudad_IdCiudad(Long idCiudad);
}
