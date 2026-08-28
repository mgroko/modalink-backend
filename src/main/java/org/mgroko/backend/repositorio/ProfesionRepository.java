package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.Profesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfesionRepository extends JpaRepository<Profesion, Long> {

    /**
     * Busca profesiones cuyo nombre contenga el patrón indicado (sin distinguir
     * mayúsculas). {@code patron} debe ser un patrón {@code LIKE} ya formado y en
     * minúsculas (ej. {@code "%modelo%"}); usar {@code "%"} para devolver todas.
     *
     * @param patron patrón LIKE en minúsculas para el nombre
     * @return profesiones que coinciden con el criterio
     */
    @Query("""
            SELECT p FROM Profesion p
            WHERE LOWER(p.nombre) LIKE :patron
            ORDER BY p.nombre
            """)
    List<Profesion> buscar(@Param("patron") String patron);
}