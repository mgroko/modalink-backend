package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.mgroko.backend.modelo.ProcesamientoColaboracionHistorica;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ProcesamientoColaboracionHistoricaRepository
        extends JpaRepository<ProcesamientoColaboracionHistorica, Long> {

    boolean existsByEstadoIn(Collection<EstadoProcesamientoHistorico> estados);

    Optional<ProcesamientoColaboracionHistorica> findFirstByEstadoInOrderByFechaInicioDesc(
            Collection<EstadoProcesamientoHistorico> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM ProcesamientoColaboracionHistorica p
            WHERE p.idProcesamiento = :id
            """)
    Optional<ProcesamientoColaboracionHistorica> findByIdForUpdate(
            @Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ProcesamientoColaboracionHistorica p
            SET p.estado = :interrumpido,
                p.fechaFin = :ahora
            WHERE p.estado IN :estados
            """)
    int marcarInterrumpidos(
            @Param("estados") Collection<EstadoProcesamientoHistorico> estados,
            @Param("interrumpido") EstadoProcesamientoHistorico interrumpido,
            @Param("ahora") LocalDateTime ahora);

    @Query("""
            SELECT p
            FROM ProcesamientoColaboracionHistorica p
            WHERE p.estado = COALESCE(:estado, p.estado)
              AND p.fechaInicio >= COALESCE(:desde, p.fechaInicio)
              AND p.fechaInicio <= COALESCE(:hasta, p.fechaInicio)
            ORDER BY p.fechaInicio DESC, p.idProcesamiento DESC
            """)
    Page<ProcesamientoColaboracionHistorica> buscar(
            @Param("estado") EstadoProcesamientoHistorico estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);
}
