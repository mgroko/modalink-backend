package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.RolProyecto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolProyectoRepository extends JpaRepository<RolProyecto, Long> {

    /**
     * Busca un rol por nombre (prefijo, sin distinguir mayúsculas) o por código
     * exacto (sin distinguir mayúsculas). {@code patron} es el prefijo ya
     * escapado (ver {@code LikePatrones}); {@code codigo} es el valor exacto
     * sin escapar (un {@code _} escapado en la igualdad dejaría de matchear).
     *
     * @param patron prefijo escapado para el nombre
     * @param codigo valor exacto para el código (case-insensitive)
     * @return el rol si coincide por nombre o por código
     */
    @org.springframework.data.jpa.repository.Query("SELECT r FROM RolProyecto r WHERE LOWER(r.nombre) LIKE LOWER(CONCAT(:patron, '%')) ESCAPE '\\' OR LOWER(r.codigo) = LOWER(:codigo)")
    Optional<RolProyecto> findByNombre(@org.springframework.data.repository.query.Param("patron") String patron,
                                       @org.springframework.data.repository.query.Param("codigo") String codigo);

    Optional<RolProyecto> findByCodigo(String codigo);
}
