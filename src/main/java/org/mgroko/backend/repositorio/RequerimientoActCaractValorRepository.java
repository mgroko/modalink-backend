package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.RequerimientoActCaractValor;
import org.mgroko.backend.modelo.RequerimientoActCaractValorId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequerimientoActCaractValorRepository
        extends JpaRepository<RequerimientoActCaractValor, RequerimientoActCaractValorId> {

    List<RequerimientoActCaractValor> findByIdReqActCaract(Long idReqActCaract);
}
