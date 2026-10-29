package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralCaractValorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequerimientoGralCaractValorRepository
        extends JpaRepository<RequerimientoGralCaractValor, RequerimientoGralCaractValorId> {

    List<RequerimientoGralCaractValor> findByIdReqGralCaract(Long idReqGralCaract);

    /**
     * Carga en una sola consulta los valores de todas las características
     * indicadas. Evita un SELECT por característica al armar el detalle del
     * proyecto.
     */
    @Query("""
        SELECT v FROM RequerimientoGralCaractValor v
        WHERE v.idReqGralCaract IN :idsReqGralCaract
    """)
    List<RequerimientoGralCaractValor> findByIdReqGralCaractIn(
            @Param("idsReqGralCaract") Collection<Long> idsReqGralCaract);
}
