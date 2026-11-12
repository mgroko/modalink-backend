package org.mgroko.backend.reemplazos.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
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

    @Query("""
            SELECT r FROM Reemplazo r
            WHERE r.actividad.idActividad = :idActividad
              AND r.estado NOT IN :estadosFinales
            ORDER BY r.idReemplazo ASC
            """)
    List<Reemplazo> findActivosPorActividad(
            @Param("idActividad") Long idActividad,
            @Param("estadosFinales") Collection<EstadoReemplazo> estadosFinales);

    @Query("""
            SELECT r FROM Reemplazo r
            WHERE r.tipo = org.mgroko.backend.reemplazos.enums.TipoReemplazo.ACTIVIDAD
              AND r.requerimientoActividad.idRequerimiento = :idRequerimiento
              AND r.estado NOT IN :estadosFinales
            ORDER BY r.idReemplazo ASC
            """)
    List<Reemplazo> findActivosPorRequerimientoActividad(
            @Param("idRequerimiento") Long idRequerimiento,
            @Param("estadosFinales") Collection<EstadoReemplazo> estadosFinales);

    @Query("""
            SELECT r FROM Reemplazo r
            WHERE r.tipo = org.mgroko.backend.reemplazos.enums.TipoReemplazo.GENERAL
              AND r.requerimientoGral.idRequerimientoGral = :idRequerimientoGral
              AND r.estado NOT IN :estadosFinales
            ORDER BY r.idReemplazo ASC
            """)
    List<Reemplazo> findActivosPorRequerimientoGral(
            @Param("idRequerimientoGral") Long idRequerimientoGral,
            @Param("estadosFinales") Collection<EstadoReemplazo> estadosFinales);

    boolean existsByRequerimientoActividad_IdRequerimiento(
            Long idRequerimiento);

    boolean existsByRequerimientoGral_IdRequerimientoGral(
            Long idRequerimientoGral);

    boolean existsByActividad_IdActividad(
            Long idActividad);

    @Query("""
            SELECT DISTINCT r.requerimientoGral.idRequerimientoGral
            FROM Reemplazo r
            WHERE r.requerimientoGral.idRequerimientoGral IN :idsRequerimientos
            """)
    List<Long> findIdsRequerimientoGralConReemplazos(
            @Param("idsRequerimientos") Collection<Long> idsRequerimientos);
}
