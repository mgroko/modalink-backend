package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.modelo.ValorCaracteristicaId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValorCaracteristicaRepository extends JpaRepository<ValorCaracteristica, ValorCaracteristicaId> {

    List<ValorCaracteristica> findByCaracteristicaTecnica_IdCaracteristicaOrderByEtiqueta(Long idCaracteristica);

    List<ValorCaracteristica> findByCaracteristicaTecnica_IdCaracteristicaIn(
            Collection<Long> idsCaracteristica);

    boolean existsByCaracteristicaTecnica_IdCaracteristicaAndEtiqueta(Long idCaracteristica, String etiqueta);
}