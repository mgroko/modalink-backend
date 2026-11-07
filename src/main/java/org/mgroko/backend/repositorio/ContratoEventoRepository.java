package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.ContratoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoEventoRepository
        extends JpaRepository<ContratoEvento, Long> {

    List<ContratoEvento> findByContrato_IdContratoOrderByIdEventoAsc(Long idContrato);
}
