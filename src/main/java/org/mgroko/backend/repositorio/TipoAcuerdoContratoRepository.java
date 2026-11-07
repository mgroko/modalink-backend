package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoAcuerdoContratoRepository
        extends JpaRepository<TipoAcuerdoContrato, Long> {

    List<TipoAcuerdoContrato> findByActivoTrueOrderByNombreAsc();

    Optional<TipoAcuerdoContrato> findByIdTipoAcuerdoAndActivoTrue(Long idTipoAcuerdo);
}
