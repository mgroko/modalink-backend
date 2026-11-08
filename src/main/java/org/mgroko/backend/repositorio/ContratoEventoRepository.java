package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.ContratoEvento;
import org.mgroko.backend.modelo.enums.TipoEventoContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Historial de eventos de todos los contratos de un proyecto, con los
     * datos necesarios para la tabla del frontend.
     *
     * <p>{@code soloVisiblesParaPerfil} permite que un miembro vea unicamente
     * los contratos donde su perfil figura como parte firmante. El Director
     * consulta sin esa restriccion. No se filtra por estado del contrato para
     * que los eliminados y los historicos sigan apareciendo.</p>
     */
    @Query("""
            SELECT e FROM ContratoEvento e
            JOIN FETCH e.contrato c
            JOIN FETCH c.proyecto p
            JOIN FETCH c.tipoAcuerdo ta
            LEFT JOIN FETCH e.miembro m
            LEFT JOIN FETCH m.perfil mp
            LEFT JOIN FETCH mp.usuario
            LEFT JOIN FETCH e.usuario
            LEFT JOIN FETCH e.contratoVersion
            WHERE p.idProyecto = :idProyecto
              AND (:soloVisiblesParaPerfil = false OR EXISTS (
                    SELECT 1 FROM FirmaContrato f
                    JOIN f.miembro fm
                    JOIN fm.perfil fp
                    WHERE f.idContrato = c.idContrato
                      AND fp.idPerfil = :idPerfilActivo
              ))
              AND (:tipo IS NULL OR e.tipoEvento = :tipo)
              AND (:nombre IS NULL OR LOWER(c.nombre) LIKE :nombre ESCAPE '\\')
            ORDER BY e.fechaHora DESC, e.idEvento DESC
            """)
    Page<ContratoEvento> findByProyectoConDetalle(
            @Param("idProyecto") Long idProyecto,
            @Param("idPerfilActivo") Long idPerfilActivo,
            @Param("soloVisiblesParaPerfil") boolean soloVisiblesParaPerfil,
            @Param("tipo") TipoEventoContrato tipo,
            @Param("nombre") String nombre,
            Pageable pageable);
}
