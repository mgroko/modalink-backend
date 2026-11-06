package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ReporteRepository
        extends JpaRepository<Reporte, Long>, JpaSpecificationExecutor<Reporte> {

    /**
     * Indica si el perfil autor ya tiene un reporte pendiente sobre la misma
     * entidad. La base también lo garantiza con el índice único parcial
     * {@code UQ_reporte_pendiente}.
     */
    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END
        FROM Reporte r
        WHERE r.perfilAutor.idPerfil = :idPerfilAutor
          AND r.entidadTipo = :entidadTipo
          AND r.entidadId = :entidadId
          AND r.estado = org.mgroko.backend.modelo.enums.EstadoReporte.PENDIENTE
    """)
    boolean existsPendiente(
            @Param("idPerfilAutor") Long idPerfilAutor,
            @Param("entidadTipo") EntidadReporte entidadTipo,
            @Param("entidadId") Long entidadId);

    @EntityGraph(attributePaths = {
            "usuarioAutor",
            "perfilAutor",
            "perfilReportado",
            "perfilReportado.usuario",
            "perfilReportado.profesion",
            "adminRevisor"
    })
    @Query("SELECT r FROM Reporte r WHERE r.idReporte = :idReporte")
    Optional<Reporte> findDetalleById(@Param("idReporte") Long idReporte);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reporte r WHERE r.idReporte = :idReporte")
    Optional<Reporte> findByIdParaActualizar(@Param("idReporte") Long idReporte);

    @Override
    @EntityGraph(attributePaths = {
            "usuarioAutor",
            "perfilAutor",
            "perfilReportado",
            "perfilReportado.usuario",
            "perfilReportado.profesion"
    })
    Page<Reporte> findAll(Specification<Reporte> spec, Pageable pageable);
}
