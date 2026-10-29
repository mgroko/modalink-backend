package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RequerimientoGralProyectoRepository extends JpaRepository<RequerimientoGralProyecto, Long> {

    @EntityGraph(attributePaths = {
            "profesion",
            "habilidades",
            "caracteristicas",
            "caracteristicas.caracteristica"
    })
    List<RequerimientoGralProyecto> findByProyectoIdProyecto(Long idProyecto);

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
    """)
    Optional<RequerimientoGralProyecto> findByIdConProyecto(
            @Param("idRequerimientoGral") Long idRequerimientoGral);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM RequerimientoGralProyecto r
        JOIN FETCH r.profesion
        JOIN FETCH r.proyecto
        WHERE r.idRequerimientoGral = :idRequerimientoGral
    """)
    Optional<RequerimientoGralProyecto> findByIdParaGestion(
            @Param("idRequerimientoGral") Long idRequerimientoGral);
}
