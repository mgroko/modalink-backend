package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.ContratoVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoVersionRepository
        extends JpaRepository<ContratoVersion, Long> {

    Optional<ContratoVersion> findFirstByContrato_IdContratoAndVersion(
            Long idContrato, Integer version);

    long countByContrato_IdContrato(Long idContrato);
}
