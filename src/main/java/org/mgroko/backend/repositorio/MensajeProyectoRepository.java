package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.MensajeProyecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeProyectoRepository extends JpaRepository<MensajeProyecto, Long> {

    @EntityGraph(attributePaths = {
            "perfilEmisor",
            "perfilEmisor.usuario",
            "perfilEmisor.profesion",
            "perfilEmisor.imagen"
    })
    @Query("""
        SELECT m
        FROM MensajeProyecto m
        WHERE m.proyecto.idProyecto = :idProyecto
    """)
    Page<MensajeProyecto> buscarDeProyecto(
            @Param("idProyecto") Long idProyecto,
            Pageable pageable);
}
