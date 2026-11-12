package org.mgroko.backend.reemplazos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.reemplazos.dto.ConfirmarReemplazoRequest;
import org.mgroko.backend.reemplazos.dto.ReemplazoDetalleResponse;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;
import org.mgroko.backend.reemplazos.exception.CandidatoReemplazoNoEncontradoException;
import org.mgroko.backend.reemplazos.exception.ReemplazoNoEncontradoException;
import org.mgroko.backend.reemplazos.exception.ReemplazoNoGestionableException;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * Confirma los candidatos elegidos y envía las invitaciones masivas.
 */
@Service
public class ConfirmarReemplazoService {

    private static final List<EstadoCandidatoReemplazo> ESTADOS_INVITACION_ACTIVA = List.of(
            EstadoCandidatoReemplazo.INVITADO,
            EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);

    private static final List<EstadoReemplazo> ESTADOS_CON_INVITACION_POSIBLE = List.of(
            EstadoReemplazo.PENDIENTE_CONFIRMACION_DIRECTOR,
            EstadoReemplazo.INVITACIONES_ENVIADAS,
            EstadoReemplazo.PENDIENTE_APROBACION);

    private final ReemplazoRepository reemplazoRepository;
    private final ReemplazoCandidatoRepository candidatoRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final ReemplazoQueryService queryService;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final CandidatoDisponibilidadService disponibilidadService;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ReemplazoNotificacionService notificacionService;
    private final GenerarReemplazosService generarReemplazosService;
    private final EstadoReemplazoCalculator estadoCalculator;
    private final Clock reloj;

    public ConfirmarReemplazoService(
            ReemplazoRepository reemplazoRepository,
            ReemplazoCandidatoRepository candidatoRepository,
            ProyectoSecurityService proyectoSecurityService,
            ReemplazoQueryService queryService,
            CupoRequerimientoService cupoRequerimientoService,
            CandidatoDisponibilidadService disponibilidadService,
            MiembroProyectoRepository miembroProyectoRepository,
            ReemplazoNotificacionService notificacionService,
            GenerarReemplazosService generarReemplazosService,
            EstadoReemplazoCalculator estadoCalculator,
            Clock reloj) {
        this.reemplazoRepository = reemplazoRepository;
        this.candidatoRepository = candidatoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.queryService = queryService;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.disponibilidadService = disponibilidadService;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.notificacionService = notificacionService;
        this.generarReemplazosService = generarReemplazosService;
        this.estadoCalculator = estadoCalculator;
        this.reloj = reloj;
    }

    @Transactional
    @AuditarOperacion
    public ReemplazoDetalleResponse regenerar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idReemplazo) {

        queryService.validarPerfilActivo(idUsuario, idPerfilActivo);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        Reemplazo reemplazo = reemplazoRepository.findByIdForUpdate(idReemplazo)
                .filter(r -> r.getProyecto().getIdProyecto().equals(idProyecto))
                .orElseThrow(() -> new ReemplazoNoEncontradoException(idReemplazo));

        if (reemplazo.getEstado() != EstadoReemplazo.SIN_CANDIDATOS) {
            throw new ReemplazoNoGestionableException(
                    "Sólo se pueden regenerar candidatos de un reemplazo sin candidatos.",
                    "REEMPLAZO_CON_CANDIDATOS");
        }

        if (reemplazo.getProyecto().getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new ReemplazoNoGestionableException(
                    "El proyecto ya no está confirmado.",
                    "PROYECTO_NO_CONFIRMADO");
        }

        validarCupo(reemplazo);
        generarReemplazosService.regenerar(reemplazo);
        return queryService.detalle(idUsuario, idPerfilActivo, idProyecto, idReemplazo);
    }

    @Transactional
    @AuditarOperacion
    public ReemplazoDetalleResponse confirmar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idReemplazo,
            ConfirmarReemplazoRequest request) {

        queryService.validarPerfilActivo(idUsuario, idPerfilActivo);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        Reemplazo reemplazo = reemplazoRepository.findByIdForUpdate(idReemplazo)
                .filter(r -> r.getProyecto().getIdProyecto().equals(idProyecto))
                .orElseThrow(() -> new ReemplazoNoEncontradoException(idReemplazo));

        if (!ESTADOS_CON_INVITACION_POSIBLE.contains(reemplazo.getEstado())) {
            throw new ReemplazoNoGestionableException(
                    "El reemplazo no admite nuevas invitaciones.",
                    "REEMPLAZO_NO_PENDIENTE_CONFIRMACION");
        }

        if (reemplazo.getProyecto().getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new ReemplazoNoGestionableException(
                    "El proyecto ya no está confirmado.",
                    "PROYECTO_NO_CONFIRMADO");
        }

        validarCupo(reemplazo);

        List<ReemplazoCandidato> seleccionados = candidatoRepository
                .findByIdsForUpdate(idReemplazo, request.idsCandidatos());

        if (seleccionados.size() != request.idsCandidatos().stream().distinct().count()) {
            throw new CandidatoReemplazoNoEncontradoException(
                    request.idsCandidatos().stream()
                            .filter(id -> seleccionados.stream()
                                    .noneMatch(c -> c.getIdCandidato().equals(id)))
                            .findFirst()
                            .orElse(null));
        }

        String mensaje = normalizarMensaje(request.mensaje());
        LocalDateTime ahora = LocalDateTime.now(reloj);

        for (ReemplazoCandidato candidato : seleccionados) {
            validarCandidatoInvitables(reemplazo, candidato);
            candidato.setEstado(EstadoCandidatoReemplazo.INVITADO);
            candidato.setFechaInvitacion(ahora);
            candidato.setMensaje(mensaje);
        }
        candidatoRepository.saveAllAndFlush(seleccionados);

        EstadoReemplazo nuevoEstado = estadoCalculator.calcular(idReemplazo);
        if (reemplazo.getEstado() != nuevoEstado) {
            reemplazo.setEstado(nuevoEstado);
            reemplazoRepository.saveAndFlush(reemplazo);
        }

        for (ReemplazoCandidato candidato : seleccionados) {
            notificacionService.notificarCandidato(
                    candidato,
                    NotificacionService.TIPO_REEMPLAZO_INVITACION_RECIBIDA,
                    "Recibiste una invitación para cubrir la vacante de "
                            + descripcionVacante(reemplazo)
                            + " en el proyecto '" + reemplazo.getProyecto().getNombre()
                            + "'.");
        }

        return queryService.detalle(idUsuario, idPerfilActivo, idProyecto, idReemplazo);
    }

    private void validarCupo(Reemplazo reemplazo) {
        if (reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD) {
            var actividad = reemplazo.getActividad();
            if (actividad == null
                    || actividad.getEstado() == EstadoActividad.FINALIZADA
                    || actividad.getEstado() == EstadoActividad.CANCELADA) {
                throw new ReemplazoNoGestionableException(
                        "La actividad ya no admite incorporaciones.",
                        "ACTIVIDAD_NO_GESTIONABLE");
            }
            if (reemplazo.getRequerimientoActividad() == null
                    || reemplazo.getRequerimientoActividad().getFechaArchivado() != null) {
                throw new ReemplazoNoGestionableException(
                        "El requerimiento de actividad ya no está activo.",
                        "REQUERIMIENTO_NO_ACTIVO");
            }
        } else if (reemplazo.getRequerimientoGral() == null
                || reemplazo.getRequerimientoGral().getFechaArchivado() != null) {
            throw new ReemplazoNoGestionableException(
                    "El requerimiento general ya no está activo.",
                    "REQUERIMIENTO_NO_ACTIVO");
        }

        boolean satisfecho = reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD
                ? cupoRequerimientoService
                        .calcularActividad(reemplazo.getRequerimientoActividad())
                        .satisfecho()
                : cupoRequerimientoService
                        .calcularGeneral(reemplazo.getRequerimientoGral())
                        .satisfecho();
        if (satisfecho) {
            throw new ReemplazoNoGestionableException(
                    "La vacante ya está cubierta.",
                    "VACANTE_YA_CUBIERTA");
        }
    }

    private void validarCandidatoInvitables(
            Reemplazo reemplazo,
            ReemplazoCandidato candidato) {

        if (candidato.getEstado() != EstadoCandidatoReemplazo.PROPUESTO) {
            throw new ReemplazoNoGestionableException(
                    "El candidato " + candidato.getIdCandidato()
                            + " ya no está disponible para invitar.",
                    "CANDIDATO_NO_DISPONIBLE_PARA_INVITAR");
        }

        var perfil = candidato.getPerfil();
        var usuario = candidato.getUsuario();
        if (perfil == null || usuario == null
                || perfil.getEstado() != EstadoPerfil.ACTIVO
                || usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ReemplazoNoGestionableException(
                    "El perfil del candidato " + candidato.getIdCandidato()
                            + " ya no está activo.",
                    "CANDIDATO_INACTIVO");
        }

        if (miembroProyectoRepository
                .findMiembroActivoConRol(
                        reemplazo.getProyecto().getIdProyecto(),
                        perfil.getIdPerfil())
                .isPresent()) {
            throw new ReemplazoNoGestionableException(
                    "El perfil del candidato ya es miembro activo del proyecto.",
                    "CANDIDATO_YA_ES_MIEMBRO");
        }

        boolean invitacionActiva = reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD
                ? candidatoRepository.existsInvitacionActivaParaActividad(
                        perfil.getIdPerfil(),
                        reemplazo.getActividad().getIdActividad(),
                        ESTADOS_INVITACION_ACTIVA)
                : candidatoRepository.existsInvitacionActivaParaGeneral(
                        perfil.getIdPerfil(),
                        reemplazo.getRequerimientoGral().getIdRequerimientoGral(),
                        ESTADOS_INVITACION_ACTIVA);

        if (invitacionActiva) {
            String destino = reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD
                    ? "esta actividad"
                    : "este requerimiento general";
            throw new ReemplazoNoGestionableException(
                    "El candidato ya tiene otra invitación activa para " + destino + ".",
                    "CANDIDATO_CON_INVITACION_ACTIVA");
        }

        if (reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD) {
            var disponibilidad = disponibilidadService.evaluar(
                    reemplazo.getProyecto(),
                    reemplazo.getActividad(),
                    usuario.getIdUsuario());
            if (!disponibilidad.disponible()) {
                throw new ReemplazoNoGestionableException(
                        "El candidato ya no tiene disponibilidad para la actividad.",
                        "CANDIDATO_SIN_DISPONIBILIDAD");
            }
        }
    }

    private String descripcionVacante(Reemplazo reemplazo) {
        if (reemplazo.getTipo() == TipoReemplazo.GENERAL) {
            return "la profesión '"
                    + reemplazo.getRequerimientoGral().getProfesion().getNombre()
                    + "'";
        }
        return "la actividad '" + reemplazo.getActividad().getNombre() + "'";
    }

    private String normalizarMensaje(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        String normalizado = mensaje.trim();
        return normalizado.isEmpty()
                ? null
                : normalizado.length() > 200
                        ? normalizado.substring(0, 200)
                        : normalizado;
    }
}
