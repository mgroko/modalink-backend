package org.mgroko.backend.reemplazos.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ReemplazoCandidatoRepository extends JpaRepository<ReemplazoCandidato, Long> {

    @EntityGraph(attributePaths = {
            "reemplazo",
            "reemplazo.proyecto",
            "reemplazo.actividad",
            "reemplazo.requerimientoActividad",
            "reemplazo.requerimientoActividad.profesion",
            "reemplazo.requerimientoGral",
            "reemplazo.requerimientoGral.profesion",
            "reemplazo.perfilBaja",
            "perfil",
            "perfil.usuario",
            "perfil.usuario.ubicacion",
            "perfil.usuario.ubicacion.ciudad",
            "perfil.usuario.ubicacion.ciudad.provincia",
            "perfil.profesion",
            "perfil.imagen",
            "perfil.habilidades",
            "perfil.caracteristicas",
            "perfil.caracteristicas.caracteristicaTecnica",
            "perfil.caracteristicas.valorCaracteristica"
    })
    @Query("""
            SELECT c FROM ReemplazoCandidato c
            WHERE c.reemplazo.idReemplazo = :idReemplazo
            ORDER BY c.ordenRanking ASC, c.idCandidato ASC
            """)
    List<ReemplazoCandidato> findConDetalleByReemplazo(
            @Param("idReemplazo") Long idReemplazo);

    @EntityGraph(attributePaths = {
            "reemplazo",
            "reemplazo.proyecto",
            "reemplazo.actividad",
            "reemplazo.requerimientoActividad",
            "reemplazo.requerimientoActividad.profesion",
            "reemplazo.requerimientoGral",
            "reemplazo.requerimientoGral.profesion",
            "reemplazo.perfilBaja",
            "perfil",
            "perfil.usuario",
            "perfil.usuario.ubicacion",
            "perfil.usuario.ubicacion.ciudad",
            "perfil.usuario.ubicacion.ciudad.provincia",
            "perfil.profesion",
            "perfil.imagen",
            "perfil.habilidades",
            "perfil.caracteristicas",
            "perfil.caracteristicas.caracteristicaTecnica",
            "perfil.caracteristicas.valorCaracteristica"
    })
    @Query("SELECT c FROM ReemplazoCandidato c WHERE c.idCandidato = :idCandidato")
    Optional<ReemplazoCandidato> findByIdConDetalle(
            @Param("idCandidato") Long idCandidato);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ReemplazoCandidato c WHERE c.idCandidato = :idCandidato")
    Optional<ReemplazoCandidato> findByIdForUpdate(
            @Param("idCandidato") Long idCandidato);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM ReemplazoCandidato c
            WHERE c.reemplazo.idReemplazo = :idReemplazo
              AND c.idCandidato IN :idsCandidatos
            ORDER BY c.idCandidato ASC
            """)
    List<ReemplazoCandidato> findByIdsForUpdate(
            @Param("idReemplazo") Long idReemplazo,
            @Param("idsCandidatos") Collection<Long> idsCandidatos);

    @EntityGraph(attributePaths = {
            "reemplazo",
            "reemplazo.proyecto",
            "reemplazo.actividad",
            "reemplazo.requerimientoActividad",
            "reemplazo.requerimientoActividad.profesion",
            "reemplazo.requerimientoGral",
            "reemplazo.requerimientoGral.profesion",
            "perfil",
            "perfil.usuario",
            "perfil.usuario.ubicacion",
            "perfil.usuario.ubicacion.ciudad",
            "perfil.usuario.ubicacion.ciudad.provincia",
            "perfil.profesion",
            "perfil.imagen",
            "perfil.habilidades",
            "perfil.caracteristicas",
            "perfil.caracteristicas.caracteristicaTecnica",
            "perfil.caracteristicas.valorCaracteristica"
    })
    @Query("""
            SELECT c FROM ReemplazoCandidato c
            WHERE c.perfil.idPerfil = :idPerfil
              AND c.estado IN :estados
            ORDER BY c.fechaInvitacion DESC, c.idCandidato DESC
            """)
    List<ReemplazoCandidato> buscarInvitacionesDePerfil(
            @Param("idPerfil") Long idPerfil,
            @Param("estados") Collection<EstadoCandidatoReemplazo> estados);

    /**
     * Indica si el perfil ya tiene una invitación activa para la misma
     * actividad. Se permite que tenga invitaciones para otras actividades del
     * mismo proyecto.
     */
    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM ReemplazoCandidato c
            WHERE c.perfil.idPerfil = :idPerfil
              AND c.estado IN :estados
              AND c.reemplazo.tipo = org.mgroko.backend.reemplazos.enums.TipoReemplazo.ACTIVIDAD
              AND c.reemplazo.actividad.idActividad = :idActividad
            """)
    boolean existsInvitacionActivaParaActividad(
            @Param("idPerfil") Long idPerfil,
            @Param("idActividad") Long idActividad,
            @Param("estados") Collection<EstadoCandidatoReemplazo> estados);

    /**
     * Indica si el perfil ya tiene una invitación activa para el mismo
     * requerimiento general. Se permite que tenga invitaciones para
     * actividades u otros requerimientos generales del mismo proyecto.
     */
    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM ReemplazoCandidato c
            WHERE c.perfil.idPerfil = :idPerfil
              AND c.estado IN :estados
              AND c.reemplazo.tipo = org.mgroko.backend.reemplazos.enums.TipoReemplazo.GENERAL
              AND c.reemplazo.requerimientoGral.idRequerimientoGral = :idRequerimientoGral
            """)
    boolean existsInvitacionActivaParaGeneral(
            @Param("idPerfil") Long idPerfil,
            @Param("idRequerimientoGral") Long idRequerimientoGral,
            @Param("estados") Collection<EstadoCandidatoReemplazo> estados);

    List<ReemplazoCandidato> findByReemplazoIdReemplazoAndEstadoIn(
            Long idReemplazo,
            Collection<EstadoCandidatoReemplazo> estados);

    long countByReemplazoIdReemplazoAndEstadoIn(
            Long idReemplazo,
            Collection<EstadoCandidatoReemplazo> estados);

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM ReemplazoCandidato c
            WHERE c.reemplazo.idReemplazo = :idReemplazo
              AND c.idCandidato <> :idCandidato
            """)
    int deleteOtrosCandidatos(
            @Param("idReemplazo") Long idReemplazo,
            @Param("idCandidato") Long idCandidato);

    @Query("""
            SELECT c.idCandidato AS entidadId,
                   c.reemplazo.proyecto.idProyecto AS idProyecto
            FROM ReemplazoCandidato c
            WHERE c.idCandidato IN :ids
            """)
    List<ProyectoDeEntidad> findProyectoPorIds(@Param("ids") Collection<Long> ids);
}
