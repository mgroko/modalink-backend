package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaRepository
        extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {
}
