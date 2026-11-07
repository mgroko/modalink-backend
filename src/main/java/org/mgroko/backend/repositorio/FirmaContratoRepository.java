package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.FirmaContratoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FirmaContratoRepository
        extends JpaRepository<FirmaContrato, FirmaContratoId> {

    List<FirmaContrato> findByIdContratoOrderByIdMiembroAsc(Long idContrato);

    @Query("""
        SELECT f FROM FirmaContrato f
        JOIN FETCH f.miembro m
        JOIN FETCH m.perfil p
        JOIN FETCH p.usuario
        WHERE f.idContrato = :idContrato
        ORDER BY f.idMiembro
    """)
    List<FirmaContrato> findConPartes(@Param("idContrato") Long idContrato);

    @Query("""
        SELECT f FROM FirmaContrato f
        JOIN FETCH f.miembro m
        JOIN FETCH m.perfil p
        JOIN FETCH p.usuario
        WHERE f.idContrato IN :idsContrato
        ORDER BY f.idContrato, f.idMiembro
    """)
    List<FirmaContrato> findConPartesPorContratos(
            @Param("idsContrato") Collection<Long> idsContrato);
}
