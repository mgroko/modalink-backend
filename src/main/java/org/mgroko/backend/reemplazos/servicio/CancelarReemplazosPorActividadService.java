package org.mgroko.backend.reemplazos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancela lógicamente los reemplazos de una actividad que fue cancelada.
 *
 * <p>Los procesos y candidatos se conservan para trazabilidad, pero pasan a
 * estado CANCELADO y dejan de aparecer en la bandeja activa del profesional.</p>
 */
@Service
public class CancelarReemplazosPorActividadService {

    private static final List<EstadoReemplazo> ESTADOS_FINALES = List.of(
            EstadoReemplazo.CUBIERTO,
            EstadoReemplazo.CANCELADO,
            EstadoReemplazo.EXPIRADO);

    private static final List<EstadoCandidatoReemplazo> ESTADOS_CANDIDATO_ACTIVOS =
            List.of(
                    EstadoCandidatoReemplazo.PROPUESTO,
                    EstadoCandidatoReemplazo.INVITADO,
                    EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);

    private static final String MOTIVO_CIERRE = "Actividad cancelada";

    private final ReemplazoRepository reemplazoRepository;
    private final ReemplazoCandidatoRepository candidatoRepository;
    private final ReemplazoNotificacionService notificacionService;
    private final ReemplazoSnapshotService reemplazoSnapshotService;
    private final Clock reloj;

    public CancelarReemplazosPorActividadService(
            ReemplazoRepository reemplazoRepository,
            ReemplazoCandidatoRepository candidatoRepository,
            ReemplazoNotificacionService notificacionService,
            ReemplazoSnapshotService reemplazoSnapshotService,
            Clock reloj) {
        this.reemplazoRepository = reemplazoRepository;
        this.candidatoRepository = candidatoRepository;
        this.notificacionService = notificacionService;
        this.reemplazoSnapshotService = reemplazoSnapshotService;
        this.reloj = reloj;
    }

    @Transactional
    public void cancelarPorActividad(
            Long idActividad,
            String nombreActividad,
            String nombreProyecto) {

        if (idActividad == null) {
            return;
        }

        List<Reemplazo> reemplazos = reemplazoRepository
                .findActivosPorActividad(idActividad, ESTADOS_FINALES);

        if (reemplazos.isEmpty()) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        for (Reemplazo reemplazo : reemplazos) {
            List<ReemplazoCandidato> candidatos = candidatoRepository
                    .findByReemplazoIdReemplazoAndEstadoIn(
                            reemplazo.getIdReemplazo(),
                            ESTADOS_CANDIDATO_ACTIVOS);

            for (ReemplazoCandidato candidato : candidatos) {
                notificacionService.eliminarInvitacionRecibida(candidato);
                candidato.setEstado(EstadoCandidatoReemplazo.CANCELADO);
                candidato.setFechaDecision(ahora);
                candidato.setMotivoDecision(MOTIVO_CIERRE);
            }
            if (!candidatos.isEmpty()) {
                candidatoRepository.saveAll(candidatos);
            }

            reemplazo.setEstado(EstadoReemplazo.CANCELADO);
            reemplazo.setFechaCierre(ahora);
            reemplazo.setMotivoCierre(MOTIVO_CIERRE);
            reemplazoSnapshotService.repuntarAlCerrar(reemplazo, ahora);
            reemplazoRepository.saveAndFlush(reemplazo);

            for (ReemplazoCandidato candidato : candidatos) {
                notificacionService.notificarCandidato(
                        candidato,
                        NotificacionService.TIPO_REEMPLAZO_CANCELADO,
                        "La actividad '" + nombreActividad
                                + "' del proyecto '" + nombreProyecto
                                + "' fue cancelada. La invitación de reemplazo quedó sin efecto.");
            }

            notificacionService.notificarDirectores(
                    reemplazo,
                    NotificacionService.TIPO_REEMPLAZO_CANCELADO,
                    "El proceso de reemplazo de la actividad '" + nombreActividad
                            + "' del proyecto '" + nombreProyecto
                            + "' se canceló automáticamente porque la actividad fue cancelada.");
        }
    }
}
