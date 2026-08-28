package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.ValorCaracteristica;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValorCaracteristicaRepository extends JpaRepository<ValorCaracteristica, Long> {

    List<ValorCaracteristica> findByCaracteristicaTecnicaIdCaracteristicaOrderByCodigo(Long idCaracteristica);

    boolean existsByCaracteristicaTecnicaIdCaracteristicaAndCodigo(Long idCaracteristica, String codigo);
}