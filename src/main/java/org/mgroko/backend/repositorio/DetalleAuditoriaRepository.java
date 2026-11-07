package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.DetalleAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleAuditoriaRepository extends JpaRepository<DetalleAuditoria, Long> {

    List<DetalleAuditoria> findByIdAuditoriaInOrderByIdDetalleAuditoriaAsc(Collection<Long> idsAuditoria);
}
