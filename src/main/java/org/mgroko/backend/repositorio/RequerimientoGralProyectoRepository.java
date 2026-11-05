package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RequerimientoGralProyectoRepository
        extends JpaRepository<RequerimientoGralProyecto, Long>,
                JpaSpecificationExecutor<RequerimientoGralProyecto> {

    boolean existsByHabilidades_IdHabilidad(Long idHabilidad);

    @EntityGraph(attributePaths = {
            "profesion",
            "habilidades",
            "caracteristicas",
            "caracteristicas.caracteristica"
    })
    @Query("""
        SELECT r FROM RequerimientoGralProyecto r
        WHERE r.proyecto.idProyecto = :idProyecto
          AND r.fechaArchivado IS NULL
    """)
    List<RequerimientoGralProyecto> findByProyectoIdProyecto(
            @Param("idProyecto") Long idProyecto);

    /**
     * Recupera un requerimiento general junto con su profesión y su proyecto.
     * Se usa al dar de alta una postulación (UC-26) para validar la profesión
     * del perfil y el estado del proyecto sin consultas perezosas adicionales.
     *
     * @param idRequerimientoGral identificador del requerimiento general
     * @return el requerimiento con sus relaciones principales inicializadas
     */
    @Query("""
        SELECT r FROM RequerimientoGralProyecto r
        JOIN FETCH r.profesion
        JOIN FETCH r.proyecto
        WHERE r.idRequerimientoGral = :idRequerimientoGral
          AND r.fechaArchivado IS NULL
    """)
    Optional<RequerimientoGralProyecto> findByIdConProyecto(
            @Param("idRequerimientoGral") Long idRequerimientoGral);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM RequerimientoGralProyecto r
        JOIN FETCH r.profesion
        JOIN FETCH r.proyecto
        WHERE r.idRequerimientoGral = :idRequerimientoGral
          AND r.fechaArchivado IS NULL
    """)
    Optional<RequerimientoGralProyecto> findByIdParaGestion(
            @Param("idRequerimientoGral") Long idRequerimientoGral);

    /**
     * Recupera requerimientos generales con proyecto, profesión,
     * características y habilidades inicializadas en lote. Se usa en el
     * buscador de oportunidades (UC-72).
     */
    @Query("""
        SELECT DISTINCT r FROM RequerimientoGralProyecto r
        JOIN FETCH r.profesion
        JOIN FETCH r.proyecto
        LEFT JOIN FETCH r.caracteristicas c
        LEFT JOIN FETCH c.caracteristica
        LEFT JOIN FETCH r.habilidades
        WHERE r.idRequerimientoGral IN :ids
          AND r.fechaArchivado IS NULL
        ORDER BY r.idRequerimientoGral ASC
    """)
    List<RequerimientoGralProyecto> findConDetalleByIds(
            @Param("ids") Collection<Long> ids);

    /**
     * Carga y bloquea los requerimientos generales de un proyecto antes de
     * validar protección y aplicar cambios. Se usa en UC-36 para evitar
     * carreras entre la validación de vínculos y el borrado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM RequerimientoGralProyecto r
        WHERE r.proyecto.idProyecto = :idProyecto
          AND r.fechaArchivado IS NULL
    """)
    List<RequerimientoGralProyecto> findByProyectoIdProyectoForUpdate(
            @Param("idProyecto") Long idProyecto);

    /**
     * Último snapshot de un requerimiento general activo.
     */
    @EntityGraph(attributePaths = {
            "proyecto",
            "profesion",
            "habilidades",
            "caracteristicas",
            "caracteristicas.caracteristica"
    })
    Optional<RequerimientoGralProyecto> findFirstByIdRequerimientoOrigenOrderByFechaArchivadoDesc(
            @Param("idRequerimientoOrigen") Long idRequerimientoOrigen);
}
