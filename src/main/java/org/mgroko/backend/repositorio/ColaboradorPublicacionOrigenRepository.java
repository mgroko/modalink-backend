package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.ColaboradorPublicacionOrigen;
import org.mgroko.backend.modelo.ColaboradorPublicacionOrigenId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColaboradorPublicacionOrigenRepository
        extends JpaRepository<ColaboradorPublicacionOrigen, ColaboradorPublicacionOrigenId> {

    Optional<ColaboradorPublicacionOrigen> findByIdPublicacionAndIdPerfil(
            Long idPublicacion, Long idPerfil);
}
