package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.perfiles.dto.ResumenResenasRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalificacionPerfilRepository extends JpaRepository<CalificacionPerfil, Long> {

    List<CalificacionPerfil> findByProyectoIdProyecto(Long idProyecto);

    List<CalificacionPerfil> findByProyectoIdProyectoAndPerfilEmisorIdPerfil(
            Long idProyecto,
            Long idPerfilEmisor);

    List<CalificacionPerfil> findByProyectoIdProyectoAndPerfilReceptorIdPerfil(
            Long idProyecto,
            Long idPerfilReceptor);

    boolean existsByProyectoIdProyectoAndPerfilEmisorIdPerfilAndPerfilReceptorIdPerfil(
            Long idProyecto,
            Long idPerfilEmisor,
            Long idPerfilReceptor);

    long countByProyectoIdProyectoAndPerfilEmisorIdPerfil(
            Long idProyecto,
            Long idPerfilEmisor);

    /**
     * Reseñas recibidas por un perfil, con los datos públicos del emisor y del
     * proyecto ya inicializados para evitar consultas perezosas al mapear.
     */
    @EntityGraph(attributePaths = {
            "perfilEmisor",
            "perfilEmisor.profesion",
            "perfilEmisor.imagen",
            "proyecto"
    })
    Page<CalificacionPerfil> findByPerfilReceptorIdPerfil(
            Long idPerfilReceptor,
            Pageable pageable);

    /**
     * Detalle de una reseña validando que pertenezca al perfil consultado.
     */
    @EntityGraph(attributePaths = {
            "perfilEmisor",
            "perfilEmisor.profesion",
            "perfilEmisor.imagen",
            "proyecto"
    })
    Optional<CalificacionPerfil> findByIdCalificacionAndPerfilReceptorIdPerfil(
            Long idCalificacion,
            Long idPerfilReceptor);

    /**
     * Resumen agregado de las reseñas recibidas por un perfil en una sola
     * consulta.
     */
    @Query("""
            SELECT new org.mgroko.backend.perfiles.dto.ResumenResenasRow(
                COUNT(c),
                AVG(c.valoracion),
                COALESCE(SUM(CASE WHEN c.valoracion = 5 THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN c.valoracion = 4 THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN c.valoracion = 3 THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN c.valoracion = 2 THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN c.valoracion = 1 THEN 1L ELSE 0L END), 0L)
            )
            FROM CalificacionPerfil c
            WHERE c.perfilReceptor.idPerfil = :idPerfil
            """)
    ResumenResenasRow resumirPorPerfilReceptor(@Param("idPerfil") Long idPerfil);
}
