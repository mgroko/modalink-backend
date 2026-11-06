package org.mgroko.backend.reemplazos.servicio;

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
import org.mgroko.backend.reemplazos.exception.ReemplazoNoEncontradoException;
import org.mgroko.backend.reemplazos.mapper.ReemplazoMapper;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
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

    private final ReemplazoRepository reemplazoRepository;
    private final ReemplazoCandidatoRepository reemplazoCandidatoRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;

    public ReemplazoQueryService(
            ReemplazoRepository reemplazoRepository,
            ReemplazoCandidatoRepository reemplazoCandidatoRepository,
            PerfilRepository perfilRepository,
            ProyectoSecurityService proyectoSecurityService) {
        this.reemplazoRepository = reemplazoRepository;
        this.reemplazoCandidatoRepository = reemplazoCandidatoRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
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

        List<CandidatoReemplazoResponse> candidatos = reemplazoCandidatoRepository
                .findConDetalleByReemplazo(idReemplazo)
                .stream()
                .map(ReemplazoMapper::toCandidato)
                .toList();

        return new ReemplazoDetalleResponse(toResumen(reemplazo), candidatos);
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
