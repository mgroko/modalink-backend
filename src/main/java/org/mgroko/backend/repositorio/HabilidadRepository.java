package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.Habilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabilidadRepository extends JpaRepository<Habilidad, Long> {

    Optional<Habilidad> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdHabilidadNot(String nombre, Long idHabilidad);

    /**
     * Busca habilidades por nombre (contiene, sin distinguir mayúsculas).
     * {@code patron} debe ser un patrón {@code LIKE} ya formado y en minúsculas
     * con los caracteres especiales escapados (ver {@code LikePatrones}).
     *
     * @param patron patrón LIKE en minúsculas para el nombre
     * @return habilidades que coinciden, ordenadas por nombre
     */
    @Query("""
            SELECT h FROM Habilidad h
            WHERE LOWER(h.nombre) LIKE :patron ESCAPE '\\'
            ORDER BY h.nombre
            """)
    List<Habilidad> buscar(@Param("patron") String patron);

    /**
     * Elimina las asociaciones de una habilidad con todos los perfiles. Se usa
     * al dar de baja una habilidad que no está referenciada por requerimientos.
     *
     * @param idHabilidad identificador de la habilidad
     * @return cantidad de asociaciones eliminadas
     */
    @Modifying
    @Query(value = "DELETE FROM habilidad_perfil WHERE id_habilidad = :idHabilidad",
            nativeQuery = true)
    int desasociarDePerfiles(@Param("idHabilidad") Long idHabilidad);
}
