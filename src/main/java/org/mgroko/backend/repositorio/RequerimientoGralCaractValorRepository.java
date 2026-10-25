package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralCaractValorId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequerimientoGralCaractValorRepository
        extends JpaRepository<RequerimientoGralCaractValor, RequerimientoGralCaractValorId> {

    List<RequerimientoGralCaractValor> findByIdReqGralCaract(Long idReqGralCaract);
}
