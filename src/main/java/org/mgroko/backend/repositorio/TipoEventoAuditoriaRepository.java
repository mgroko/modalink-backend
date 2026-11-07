package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.TipoEventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoEventoAuditoriaRepository
        extends JpaRepository<TipoEventoAuditoria, String> {

    List<TipoEventoAuditoria> findByActivoTrueOrderByModuloAscNombreAsc();
}
