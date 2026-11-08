package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoAcuerdoContratoRepository
        extends JpaRepository<TipoAcuerdoContrato, Long> {

    List<TipoAcuerdoContrato> findByActivoTrueOrderByNombreAsc();

    List<TipoAcuerdoContrato> findAllByOrderByNombreAsc();

    Optional<TipoAcuerdoContrato> findByIdTipoAcuerdoAndActivoTrue(Long idTipoAcuerdo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdTipoAcuerdoNot(String nombre, Long idTipoAcuerdo);

    boolean existsByNombreIgnoreCaseAndActivoTrueAndIdTipoAcuerdoNot(
            String nombre, Long idTipoAcuerdo);

    long countByActivoTrue();
}
