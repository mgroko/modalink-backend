package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.Profesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfesionRepository extends JpaRepository<Profesion, Long> {

    /**
     * Busca profesiones por nombre (contiene, sin distinguir mayúsculas) o por
     * código exacto (sin distinguir mayúsculas). {@code patron} debe ser un patrón
     * {@code LIKE} ya formado y en minúsculas con los caracteres especiales
     * escapados (ver {@code LikePatrones}); usar {@code "%"} para devolver todas.
     * {@code codigo} es el valor exacto a comparar contra el código
     * (ej. {@code "MODELO"}); usar {@code ""} para no filtrar por código.
     * El resultado se ordena por código.
     *
     * @param patron patrón LIKE en minúsculas para el nombre (escapado)
     * @param codigo valor exacto para el código (case-insensitive, sin escapar)
     * @return profesiones que coinciden con el criterio
     */
    @Query("""
            SELECT p FROM Profesion p
            WHERE LOWER(p.nombre) LIKE :patron ESCAPE '\\'
               OR LOWER(p.codigo) = LOWER(:codigo)
            ORDER BY p.codigo
            """)
    List<Profesion> buscar(@Param("patron") String patron, @Param("codigo") String codigo);
    
    List<Profesion> findAllByOrderByCodigo();

    Optional<Profesion> findByCodigoIgnoreCase(String codigo);
}