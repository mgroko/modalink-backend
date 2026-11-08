package org.mgroko.backend.reemplazos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.reemplazos.dto.ReemplazoDetalleResponse;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.exception.ReemplazoNoEncontradoException;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * Cancela un proceso de reemplazo que ya no debe continuar.
 */
@Service
public class CancelarReemplazoService {

    private final ReemplazoRepository reemplazoRepository;
    private final ReemplazoCandidatoRepository candidatoRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final ReemplazoQueryService queryService;
    private final ReemplazoNotificacionService notificacionService;
    private final Clock reloj;

    public CancelarReemplazoService(
            ReemplazoRepository reemplazoRepository,
            ReemplazoCandidatoRepository candidatoRepository,
            ProyectoSecurityService proyectoSecurityService,
            ReemplazoQueryService queryService,
            ReemplazoNotificacionService notificacionService,
            Clock reloj) {
        this.reemplazoRepository = reemplazoRepository;
        this.candidatoRepository = candidatoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.queryService = queryService;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    @Transactional
    @AuditarOperacion
    public ReemplazoDetalleResponse cancelar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idReemplazo,
            String motivo) {

        queryService.validarPerfilActivo(idUsuario, idPerfilActivo);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        Reemplazo reemplazo = reemplazoRepository.findByIdForUpdate(idReemplazo)
                .filter(r -> r.getProyecto().getIdProyecto().equals(idProyecto))
                .orElseThrow(() -> new ReemplazoNoEncontradoException(idReemplazo));

        if (reemplazo.getEstado().esFinal()) {
            return queryService.detalle(idUsuario, idPerfilActivo, idProyecto, idReemplazo);
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        reemplazo.setEstado(EstadoReemplazo.CANCELADO);
        reemplazo.setFechaCierre(ahora);
        reemplazoRepository.saveAndFlush(reemplazo);

        var candidatos = candidatoRepository.findConDetalleByReemplazo(idReemplazo);
        for (var candidato : candidatos) {
            if (candidato.getEstado() != null && candidato.getEstado().estaActivo()) {
                candidato.setEstado(EstadoCandidatoReemplazo.CANCELADO);
                candidato.setFechaDecision(ahora);
                candidato.setMotivoDecision(motivo);
            }
        }
        candidatoRepository.saveAll(candidatos);

        notificacionService.notificarDirectores(
                reemplazo,
                NotificacionService.TIPO_REEMPLAZO_CANCELADO,
                "Se canceló el proceso de reemplazo de la vacante del proyecto '"
                        + reemplazo.getProyecto().getNombre() + "'."
                        + (motivo != null && !motivo.isBlank()
                                ? " Motivo: " + motivo
                                : ""));

        return queryService.detalle(idUsuario, idPerfilActivo, idProyecto, idReemplazo);
    }
}
