package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    /**
     * Busca la ubicación que corresponde a una localidad dentro de una
     * provincia. Al ser la ubicación una tabla compartida (usuario, y más
     * adelante proyecto y actividad), se reutiliza la fila existente cuando
     * ya se creó esa misma localidad en la provincia.
     *
     * @param localidad nombre de la localidad
     * @param provincia nombre de la provincia
     * @return un Optional con la Ubicacion si existe, vacío en caso contrario
     */
    Optional<Ubicacion> findByLocalidadAndProvincia(String localidad, String provincia);
}
