package org.mgroko.backend.reemplazos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancela automáticamente los reemplazos cuya vacante dejó de existir porque
 * el requerimiento quedó cubierto.
 *
 * <p>La decisión se toma siempre con {@link CupoRequerimientoService}: sólo se
 * cierra un proceso cuando el requerimiento está satisfecho. Los procesos y
 * candidatos se conservan para trazabilidad, pero pasan a estado
 * {@code CANCELADO}.</p>
 */
@Service
public class CancelarReemplazosCumplidosService {

    public static final String MOTIVO_REQUERIMIENTO_CUBIERTO =
            "Requerimiento cubierto";

    private static final List<EstadoReemplazo> ESTADOS_FINALES = List.of(
            EstadoReemplazo.CUBIERTO,
            EstadoReemplazo.CANCELADO,
            EstadoReemplazo.EXPIRADO);

    private static final List<EstadoCandidatoReemplazo> ESTADOS_CANDIDATO_ACTIVOS =
            List.of(
                    EstadoCandidatoReemplazo.PROPUESTO,
                    EstadoCandidatoReemplazo.INVITADO,
                    EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);

    private static final List<EstadoCandidatoReemplazo> ESTADOS_CON_INVITACION =
            List.of(
                    EstadoCandidatoReemplazo.INVITADO,
                    EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);

    private final ReemplazoRepository reemplazoRepository;
    private final ReemplazoCandidatoRepository candidatoRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ReemplazoNotificacionService notificacionService;
    private final ReemplazoSnapshotService reemplazoSnapshotService;
    private final Clock reloj;

    public CancelarReemplazosCumplidosService(
            ReemplazoRepository reemplazoRepository,
            ReemplazoCandidatoRepository candidatoRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ReemplazoNotificacionService notificacionService,
            ReemplazoSnapshotService reemplazoSnapshotService,
            Clock reloj) {
        this.reemplazoRepository = reemplazoRepository;
        this.candidatoRepository = candidatoRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.notificacionService = notificacionService;
        this.reemplazoSnapshotService = reemplazoSnapshotService;
        this.reloj = reloj;
    }

    @Transactional
    public int cancelarPorRequerimientoActividad(Long idRequerimiento) {
        return cancelarPorRequerimientoActividad(
                idRequerimiento, MOTIVO_REQUERIMIENTO_CUBIERTO);
    }

    @Transactional
    public int cancelarPorRequerimientoActividad(
            Long idRequerimiento,
            String motivo) {

        if (idRequerimiento == null) {
            return 0;
        }

        List<Reemplazo> activos = reemplazoRepository
                .findActivosPorRequerimientoActividad(idRequerimiento, ESTADOS_FINALES);

        int cancelados = 0;
        for (Reemplazo candidato : activos) {
            Reemplazo reemplazo = reemplazoRepository
                    .findByIdForUpdate(candidato.getIdReemplazo())
                    .orElse(null);
            if (reemplazo == null
                    || reemplazo.getEstado() == null
                    || reemplazo.getEstado().esFinal()
                    || reemplazo.getTipo() != TipoReemplazo.ACTIVIDAD
                    || reemplazo.getRequerimientoActividad() == null) {
                continue;
            }

            if (!cupoRequerimientoService
                    .calcularActividad(reemplazo.getRequerimientoActividad())
                    .satisfecho()) {
                continue;
            }

            cancelar(reemplazo, motivo);
            cancelados++;
        }
        return cancelados;
    }

    @Transactional
    public int cancelarPorRequerimientoGeneral(Long idRequerimientoGral) {
        return cancelarPorRequerimientoGeneral(
                idRequerimientoGral, MOTIVO_REQUERIMIENTO_CUBIERTO);
    }

    @Transactional
    public int cancelarPorRequerimientoGeneral(
            Long idRequerimientoGral,
            String motivo) {

        if (idRequerimientoGral == null) {
            return 0;
        }

        List<Reemplazo> activos = reemplazoRepository
                .findActivosPorRequerimientoGral(idRequerimientoGral, ESTADOS_FINALES);

        int cancelados = 0;
        for (Reemplazo candidato : activos) {
            Reemplazo reemplazo = reemplazoRepository
                    .findByIdForUpdate(candidato.getIdReemplazo())
                    .orElse(null);
            if (reemplazo == null
                    || reemplazo.getEstado() == null
                    || reemplazo.getEstado().esFinal()
                    || reemplazo.getTipo() != TipoReemplazo.GENERAL
                    || reemplazo.getRequerimientoGral() == null) {
                continue;
            }

            if (!cupoRequerimientoService
                    .calcularGeneral(reemplazo.getRequerimientoGral())
                    .satisfecho()) {
                continue;
            }

            cancelar(reemplazo, motivo);
            cancelados++;
        }
        return cancelados;
    }

    private void cancelar(Reemplazo reemplazo, String motivo) {
        String motivoCierre = normalizarMotivo(motivo);
        LocalDateTime ahora = LocalDateTime.now(reloj);

        List<ReemplazoCandidato> candidatos = candidatoRepository
                .findByReemplazoIdReemplazoAndEstadoIn(
                        reemplazo.getIdReemplazo(),
                        ESTADOS_CANDIDATO_ACTIVOS);

        List<ReemplazoCandidato> notificables = new ArrayList<>();
        for (ReemplazoCandidato candidato : candidatos) {
            boolean teniaInvitacion = ESTADOS_CON_INVITACION.contains(
                    candidato.getEstado());
            if (teniaInvitacion) {
                notificacionService.eliminarInvitacionRecibida(candidato);
                notificables.add(candidato);
            }
            candidato.setEstado(EstadoCandidatoReemplazo.CANCELADO);
            candidato.setFechaDecision(ahora);
            candidato.setMotivoDecision(motivoCierre);
        }
        if (!candidatos.isEmpty()) {
            candidatoRepository.saveAllAndFlush(candidatos);
        }

        reemplazo.setEstado(EstadoReemplazo.CANCELADO);
        reemplazo.setFechaCierre(ahora);
        reemplazo.setMotivoCierre(motivoCierre);
        reemplazoSnapshotService.repuntarAlCerrar(reemplazo, ahora);
        reemplazoRepository.saveAndFlush(reemplazo);

        for (ReemplazoCandidato candidato : notificables) {
            notificacionService.notificarCandidato(
                    candidato,
                    NotificacionService.TIPO_REEMPLAZO_CANCELADO,
                    "La vacante de " + descripcionVacante(reemplazo)
                            + " en el proyecto '"
                            + reemplazo.getProyecto().getNombre()
                            + "' fue cubierta. Tu invitación de reemplazo quedó sin efecto.");
        }

        notificacionService.notificarDirectores(
                reemplazo,
                NotificacionService.TIPO_REEMPLAZO_CANCELADO,
                "Se canceló automáticamente el reemplazo de "
                        + descripcionVacante(reemplazo)
                        + " en el proyecto '"
                        + reemplazo.getProyecto().getNombre()
                        + "' porque el requerimiento ya está cubierto.");
    }

    private String descripcionVacante(Reemplazo reemplazo) {
        if (reemplazo.getTipo() == TipoReemplazo.GENERAL) {
            return "la profesión '"
                    + reemplazo.getRequerimientoGral().getProfesion().getNombre()
                    + "'";
        }
        return "la actividad '" + reemplazo.getActividad().getNombre() + "'";
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return MOTIVO_REQUERIMIENTO_CUBIERTO;
        }
        String normalizado = motivo.trim();
        return normalizado.length() > 200
                ? normalizado.substring(0, 200)
                : normalizado;
    }
}
