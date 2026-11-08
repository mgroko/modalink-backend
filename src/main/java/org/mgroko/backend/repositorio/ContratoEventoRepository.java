package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.ContratoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContratoEventoRepository
        extends JpaRepository<ContratoEvento, Long> {

    List<ContratoEvento> findByContrato_IdContratoOrderByIdEventoAsc(Long idContrato);

    /**
     * Historial con las relaciones necesarias para mostrar nombres de parte y
     * operador en una sola consulta.
     */
    @Query("""
            SELECT e FROM ContratoEvento e
            LEFT JOIN FETCH e.miembro m
            LEFT JOIN FETCH m.perfil p
            LEFT JOIN FETCH p.usuario
            LEFT JOIN FETCH e.usuario
            LEFT JOIN FETCH e.contratoVersion
            WHERE e.contrato.idContrato = :idContrato
            ORDER BY e.idEvento ASC
            """)
    List<ContratoEvento> findByContratoConDetalle(
            @Param("idContrato") Long idContrato);
}
