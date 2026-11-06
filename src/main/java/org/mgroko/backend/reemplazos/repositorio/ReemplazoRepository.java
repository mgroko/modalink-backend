package org.mgroko.backend.reemplazos.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ReemplazoRepository extends JpaRepository<Reemplazo, Long> {

    @EntityGraph(attributePaths = {
            "proyecto",
            "actividad",
            "requerimientoActividad",
            "requerimientoActividad.profesion",
            "requerimientoGral",
            "requerimientoGral.profesion",
            "miembroBaja",
            "perfilBaja",
            "perfilBaja.usuario"
    })
    @Query("SELECT r FROM Reemplazo r WHERE r.idReemplazo = :idReemplazo")
    Optional<Reemplazo> findByIdConDetalle(@Param("idReemplazo") Long idReemplazo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reemplazo r WHERE r.idReemplazo = :idReemplazo")
    Optional<Reemplazo> findByIdForUpdate(@Param("idReemplazo") Long idReemplazo);

    List<Reemplazo> findByProyectoIdProyectoOrderByFechaCreacionDesc(Long idProyecto);

    boolean existsByMiembroBaja_IdMiembroAndRequerimientoActividad_IdRequerimiento(
            Long idMiembro,
            Long idRequerimiento);

    boolean existsByMiembroBaja_IdMiembroAndRequerimientoGral_IdRequerimientoGral(
            Long idMiembro,
            Long idRequerimientoGral);
}
