package org.mgroko.backend.reemplazos.servicio;

import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.reemplazos.dto.CandidatoReemplazoResponse;
import org.mgroko.backend.reemplazos.dto.InvitacionReemplazoResponse;
import org.mgroko.backend.reemplazos.dto.ReemplazoDetalleResponse;
import org.mgroko.backend.reemplazos.dto.ReemplazoResumenResponse;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;
import org.mgroko.backend.reemplazos.exception.ReemplazoNoGestionableException;
import org.mgroko.backend.reemplazos.exception.ReemplazoNoEncontradoException;
import org.mgroko.backend.reemplazos.mapper.ReemplazoMapper;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.mgroko.backend.reemplazos.servicio.modelo.CandidatoPerfil;
import org.mgroko.backend.reemplazos.servicio.modelo.RequisitoReemplazo;
import org.mgroko.backend.reemplazos.servicio.modelo.ResultadoConcordancia;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de lectura del módulo de reemplazos.
 */
@Service
public class ReemplazoQueryService {

    private static final List<EstadoCandidatoReemplazo> ESTADOS_INVITACION = List.of(
            EstadoCandidatoReemplazo.INVITADO,
            EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);

    private static final List<EstadoCandidatoReemplazo> ESTADOS_CON_INVITACION =
            List.of(
                    EstadoCandidatoReemplazo.INVITADO,
                    EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION,
                    EstadoCandidatoReemplazo.APROBADO);

    private static final List<EstadoCandidatoReemplazo> ESTADOS_RECHAZADOS =
            List.of(EstadoCandidatoReemplazo.RECHAZADO);

    private final ReemplazoRepository reemplazoRepository;
    private final ReemplazoCandidatoRepository reemplazoCandidatoRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final CatalogoRequisitoService catalogoRequisitoService;
    private final CandidatoMatchingService candidatoMatchingService;

    public ReemplazoQueryService(
            ReemplazoRepository reemplazoRepository,
            ReemplazoCandidatoRepository reemplazoCandidatoRepository,
            PerfilRepository perfilRepository,
            ProyectoSecurityService proyectoSecurityService,
            CatalogoRequisitoService catalogoRequisitoService,
            CandidatoMatchingService candidatoMatchingService) {
        this.reemplazoRepository = reemplazoRepository;
        this.reemplazoCandidatoRepository = reemplazoCandidatoRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.catalogoRequisitoService = catalogoRequisitoService;
        this.candidatoMatchingService = candidatoMatchingService;
    }

    @Transactional(readOnly = true)
    public List<ReemplazoResumenResponse> listar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        validarPerfilActivo(idUsuario, idPerfilActivo);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        return reemplazoRepository
                .findByProyectoIdProyectoOrderByFechaCreacionDesc(idProyecto)
                .stream()
                .map(this::toResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReemplazoDetalleResponse detalle(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idReemplazo) {

        validarPerfilActivo(idUsuario, idPerfilActivo);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        Reemplazo reemplazo = reemplazoRepository.findByIdConDetalle(idReemplazo)
                .filter(r -> r.getProyecto().getIdProyecto().equals(idProyecto))
                .orElseThrow(() -> new ReemplazoNoEncontradoException(idReemplazo));

        RequisitoReemplazo requisito = cargarRequisito(reemplazo);

        List<CandidatoReemplazoResponse> candidatos = reemplazoCandidatoRepository
                .findConDetalleByReemplazo(idReemplazo)
                .stream()
                .map(candidato -> ReemplazoMapper.toCandidato(
                        candidato,
                        evaluarCandidato(candidato, requisito)))
                .toList();

        return new ReemplazoDetalleResponse(toResumen(reemplazo), candidatos);
    }

    /**
     * Intenta reconstruir el requisito de la vacante para enriquecer la
     * respuesta con el detalle de cada habilidad y característica.
     *
     * <p>Si el requerimiento ya no está activo (por ejemplo, un proceso
     * cerrado con snapshot histórico), se devuelve {@code null} y el mapper
     * mantiene el contrato anterior. La UI sigue funcionando con su
     * fallback.</p>
     */
    private RequisitoReemplazo cargarRequisito(Reemplazo reemplazo) {
        try {
            if (reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD
                    && reemplazo.getRequerimientoActividad() != null) {
                Long idRequerimiento = reemplazo.getRequerimientoActividad()
                        .getFechaArchivado() != null
                        && reemplazo.getRequerimientoActividad()
                                .getIdRequerimientoOrigen() != null
                                        ? reemplazo.getRequerimientoActividad()
                                                .getIdRequerimientoOrigen()
                                        : reemplazo.getRequerimientoActividad()
                                                .getIdRequerimiento();
                if (idRequerimiento != null) {
                    return catalogoRequisitoService.cargarActividad(
                            idRequerimiento);
                }
            }
            if (reemplazo.getRequerimientoGral() != null) {
                Long idRequerimiento = reemplazo.getRequerimientoGral()
                        .getFechaArchivado() != null
                        && reemplazo.getRequerimientoGral()
                                .getIdRequerimientoOrigen() != null
                                        ? reemplazo.getRequerimientoGral()
                                                .getIdRequerimientoOrigen()
                                        : reemplazo.getRequerimientoGral()
                                                .getIdRequerimientoGral();
                if (idRequerimiento != null) {
                    return catalogoRequisitoService.cargarGeneral(
                            idRequerimiento);
                }
            }
        } catch (ReemplazoNoGestionableException ex) {
            return null;
        }
        return null;
    }

    private ResultadoConcordancia evaluarCandidato(
            ReemplazoCandidato candidato,
            RequisitoReemplazo requisito) {

        if (requisito == null
                || candidato.getPerfil() == null
                || candidato.getPerfil().getUsuario() == null) {
            return null;
        }

        CandidatoPerfil perfil = candidatoMatchingService
                .toCandidatoPerfil(candidato.getPerfil());
        return EvaluadorConcordancia.evaluar(requisito, perfil);
    }

    @Transactional(readOnly = true)
    public List<InvitacionReemplazoResponse> listarInvitaciones(
            Long idUsuario,
            Long idPerfilActivo) {

        validarPerfilActivo(idUsuario, idPerfilActivo);

        return reemplazoCandidatoRepository
                .buscarInvitacionesDePerfil(idPerfilActivo, ESTADOS_INVITACION)
                .stream()
                .map(ReemplazoMapper::toInvitacion)
                .toList();
    }

    /**
     * Invitaciones de reemplazo rechazadas por el propio profesional.
     *
     * <p>Sólo se incluye el estado {@code RECHAZADO}: las invitaciones
     * eliminadas por el director (borrado masivo al cubrir la vacante) y las
     * canceladas junto con el proceso no forman parte de este historial.</p>
     */
    @Transactional(readOnly = true)
    public List<InvitacionReemplazoResponse> listarInvitacionesRechazadas(
            Long idUsuario,
            Long idPerfilActivo) {

        validarPerfilActivo(idUsuario, idPerfilActivo);

        return reemplazoCandidatoRepository
                .buscarInvitacionesDePerfil(idPerfilActivo, ESTADOS_RECHAZADOS)
                .stream()
                .map(ReemplazoMapper::toInvitacion)
                .sorted(Comparator.comparing(
                        InvitacionReemplazoResponse::fechaRespuesta,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Perfil validarPerfilActivo(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }
        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);
        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }
        return perfil;
    }

    private ReemplazoResumenResponse toResumen(Reemplazo reemplazo) {
        List<ReemplazoCandidato> candidatos = reemplazo.getCandidatos();
        int total = candidatos.size();
        int invitados = (int) candidatos.stream()
                .filter(c -> c.getEstado() != null
                        && ESTADOS_CON_INVITACION.contains(c.getEstado()))
                .count();
        return ReemplazoMapper.toResumen(reemplazo, total, invitados);
    }
}
