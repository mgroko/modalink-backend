package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.ProcesamientoColaboracionHistoricaError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcesamientoColaboracionHistoricaErrorRepository
        extends JpaRepository<ProcesamientoColaboracionHistoricaError, Long> {

    Page<ProcesamientoColaboracionHistoricaError>
            findByIdProcesamientoOrderByFechaRegistroDesc(
                    Long idProcesamiento, Pageable pageable);

    long countByIdProcesamiento(Long idProcesamiento);
}
