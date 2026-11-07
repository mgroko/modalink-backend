package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.repositorio.proyeccion.ContratoActivoProyeccion;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ContratoRepository
        extends JpaRepository<Contrato, Long>, ContratoBusquedaRepository {

    @Query("""
        SELECT c FROM Contrato c
        JOIN FETCH c.proyecto
        JOIN FETCH c.tipoAcuerdo
        JOIN FETCH c.miembroEmisor me
        JOIN FETCH me.perfil p
        JOIN FETCH p.usuario
        WHERE c.idContrato = :idContrato
    """)
    Optional<Contrato> findByIdConPartes(@Param("idContrato") Long idContrato);

    /**
     * Carga el contrato con lock pesimista para serializar modificaciones
     * concurrentes de UC-53.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c FROM Contrato c
        JOIN FETCH c.proyecto
        JOIN FETCH c.tipoAcuerdo
        JOIN FETCH c.miembroEmisor me
        JOIN FETCH me.perfil p
        JOIN FETCH p.usuario
        WHERE c.idContrato = :idContrato
    """)
    Optional<Contrato> findByIdForUpdate(@Param("idContrato") Long idContrato);

    /**
     * Destinatarios que ya tienen un contrato ACTIVO del mismo tipo en el
     * proyecto. Refleja la regla de fn_contrato_tiene_duplicado_activo.
     */
    @Query(value = """
        SELECT DISTINCT f.id_miembro
        FROM firma_contrato f
        JOIN contrato c ON c.id_contrato = f.id_contrato
        WHERE f.id_miembro IN (:idsMiembro)
          AND f.rol_parte IN ('DESTINATARIO', 'DIRECTOR_DESTINATARIO')
          AND c.estado = 'ACTIVO'
          AND c.id_proyecto = :idProyecto
          AND c.id_tipo_acuerdo = :idTipoAcuerdo
    """, nativeQuery = true)
    List<Long> findMiembrosConContratoActivo(
            @Param("idProyecto") Long idProyecto,
            @Param("idTipoAcuerdo") Long idTipoAcuerdo,
            @Param("idsMiembro") Collection<Long> idsMiembro);

    /**
     * Contratos ACTIVOS del proyecto con sus destinatarios. Se usa para que
     * el frontend pueda deshabilitar combinaciones conflictivas.
     */
    @Query(value = """
        SELECT f.id_miembro AS "idMiembro",
               c.id_tipo_acuerdo AS "idTipoAcuerdo",
               c.id_contrato AS "idContrato",
               c.nombre AS "nombre"
        FROM contrato c
        JOIN firma_contrato f ON f.id_contrato = c.id_contrato
        JOIN miembros_proyecto mp ON mp.id_miembro = f.id_miembro
        WHERE c.id_proyecto = :idProyecto
          AND c.estado = 'ACTIVO'
          AND mp.estado_participacion = 'ACTIVO'
          AND f.rol_parte IN ('DESTINATARIO', 'DIRECTOR_DESTINATARIO')
        ORDER BY f.id_miembro, c.id_contrato
    """, nativeQuery = true)
    List<ContratoActivoProyeccion> findActivosPorMiembroDeProyecto(
            @Param("idProyecto") Long idProyecto);

    /**
     * Resuelve el proyecto de cada contrato indicado para las notificaciones.
     */
    @Query("""
        SELECT c.idContrato AS entidadId,
               c.proyecto.idProyecto AS idProyecto
        FROM Contrato c
        WHERE c.idContrato IN :ids
    """)
    List<ProyectoDeEntidad> findProyectoPorIds(@Param("ids") Collection<Long> ids);
}
