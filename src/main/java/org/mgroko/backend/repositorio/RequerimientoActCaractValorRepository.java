package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.RequerimientoActCaractValor;
import org.mgroko.backend.modelo.RequerimientoActCaractValorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequerimientoActCaractValorRepository
        extends JpaRepository<RequerimientoActCaractValor, RequerimientoActCaractValorId> {

    List<RequerimientoActCaractValor> findByIdReqActCaract(Long idReqActCaract);

    List<RequerimientoActCaractValor> findByIdReqActCaractIn(Collection<Long> idsReqActCaract);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM RequerimientoActCaractValor v WHERE v.idReqActCaract = :idReqActCaract")
    int deleteByIdReqActCaract(@Param("idReqActCaract") Long idReqActCaract);
}
