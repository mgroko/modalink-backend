package org.mgroko.backend.proyectos.servicio;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.dto.AsignacionRequerimientoGralResponse;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.evento.CupoRequerimientoGralModificadoEvent;
import org.mgroko.backend.proyectos.exception.RequerimientoGralNoAsignableException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * Resuelve a qué requerimiento general se vincula una membresía.
 *
 * <p>Nunca crea requerimientos: solo selecciona uno existente, activo, de la
 * misma profesión y con cupo disponible. Si no hay candidato, deja la membresía
 * con {@code requerimientoGral = null}.</p>
 */
@Service
public class RequerimientoGralAsignacionService {

    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ProyectoSecurityService proyectoSecurityService;
    private final NotificacionService notificacionService;
    private final ApplicationEventPublisher eventPublisher;

    public RequerimientoGralAsignacionService(
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ProyectoSecurityService proyectoSecurityService,
            NotificacionService notificacionService,
            ApplicationEventPublisher eventPublisher) {
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.proyectoSecurityService = proyectoSecurityService;
        this.notificacionService = notificacionService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Resuelve el requerimiento para una incorporación.
     *
     * <p>Si llega un requerimiento explícito, se respeta cuando sigue activo,
     * pertenece al proyecto, coincide la profesión y tiene cupo. Si está lleno,
     * archivado o ya no existe, se busca un candidato compatible. Si no hay,
     * devuelve {@code null}.</p>
     */
    @Transactional
    public RequerimientoGralProyecto resolverParaIncorporacion(
            Proyecto proyecto,
            Perfil perfil,
            Long idRequerimientoExplicito) {

        if (proyecto == null || proyecto.getIdProyecto() == null
                || perfil == null || perfil.getProfesion() == null
                || perfil.getProfesion().getIdProfesion() == null) {
            return null;
        }

        Long idProfesion = perfil.getProfesion().getIdProfesion();
        List<RequerimientoGralProyecto> requerimientos =
                requerimientoGralProyectoRepository.findByProyectoIdProyectoForUpdate(
                        proyecto.getIdProyecto());

        RequerimientoGralProyecto explicito = null;
        if (idRequerimientoExplicito != null) {
            explicito = requerimientos.stream()
                    .filter(requerimiento -> idRequerimientoExplicito.equals(
                            requerimiento.getIdRequerimientoGral()))
                    .findFirst()
                    .orElse(null);

            if (explicito == null) {
                // Puede estar archivado o pertenecer a otro proyecto.
                RequerimientoGralProyecto existente = requerimientoGralProyectoRepository
                        .findById(idRequerimientoExplicito)
                        .orElse(null);
                if (existente != null
                        && existente.getProyecto() != null
                        && !proyecto.getIdProyecto().equals(
                                existente.getProyecto().getIdProyecto())) {
                    throw new RequerimientoNoEncontradoException(
                            "El requerimiento no pertenece al proyecto indicado.");
                }
            } else if (explicito.getProfesion() == null
                    || !idProfesion.equals(explicito.getProfesion().getIdProfesion())) {
                throw new RequerimientoGralNoAsignableException(
                        "El perfil no tiene la profesión requerida por el requerimiento general.",
                        "PROFESION_INCOMPATIBLE");
            }
        }

        Map<Long, CupoRequerimiento> cupos =
                cupoRequerimientoService.calcularGenerales(requerimientos);

        if (explicito != null) {
            CupoRequerimiento cupo = cupos.get(explicito.getIdRequerimientoGral());
            if (cupo != null && cupo.faltantes() > 0) {
                return explicito;
            }
        }

        return requerimientos.stream()
                .filter(requerimiento -> requerimiento.getProfesion() != null
                        && idProfesion.equals(
                                requerimiento.getProfesion().getIdProfesion()))
                .filter(requerimiento -> {
                    CupoRequerimiento cupo = cupos.get(
                            requerimiento.getIdRequerimientoGral());
                    return cupo != null && cupo.faltantes() > 0;
                })
                .min(Comparator.comparing(
                        RequerimientoGralProyecto::getIdRequerimientoGral))
                .orElse(null);
    }

    /**
     * Resuelve el requerimiento explícito sin buscar alternativas. Se usa en
     * flujos donde la aceptación debe cerrar exactamente el requerimiento que
     * el director está gestionando, como el casting virtual.
     */
    @Transactional
    public RequerimientoGralProyecto resolverExactoParaIncorporacion(
            Proyecto proyecto,
            Perfil perfil,
            Long idRequerimientoGral) {

        if (proyecto == null || proyecto.getIdProyecto() == null
                || perfil == null || perfil.getProfesion() == null
                || perfil.getProfesion().getIdProfesion() == null
                || idRequerimientoGral == null) {
            throw new RequerimientoNoEncontradoException(
                    "El requerimiento general indicado no existe.");
        }

        RequerimientoGralProyecto requerimiento = requerimientoGralProyectoRepository
                .findByIdParaGestion(idRequerimientoGral)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(
                        "El requerimiento general " + idRequerimientoGral + " no existe."));

        if (!Objects.equals(
                requerimiento.getProyecto().getIdProyecto(),
                proyecto.getIdProyecto())) {
            throw new RequerimientoNoEncontradoException(
                    "El requerimiento no pertenece al proyecto indicado.");
        }

        if (requerimiento.getProfesion() == null
                || !Objects.equals(
                        requerimiento.getProfesion().getIdProfesion(),
                        perfil.getProfesion().getIdProfesion())) {
            throw new RequerimientoGralNoAsignableException(
                    "El perfil no tiene la profesión requerida por el requerimiento general.",
                    "PROFESION_INCOMPATIBLE");
        }

        CupoRequerimiento cupo = cupoRequerimientoService.calcularGeneral(requerimiento);
        if (cupo.satisfecho()) {
            throw new RequerimientoGralNoAsignableException(
                    "El requerimiento general ya está satisfecho.",
                    "REQUERIMIENTO_SATISFECHO");
        }

        return requerimiento;
    }

    /**
     * Vincula una membresía activa que todavía no tiene requerimiento.
     * Los Directores y los miembros inactivos no consumen cupo.
     */
    @Transactional
    public void vincularSiCorresponde(MiembroProyecto miembro) {
        if (miembro == null || miembro.getIdMiembro() == null
                || miembro.getEstadoParticipacion() != EstadoParticipacion.ACTIVO
                || miembro.getRequerimientoGral() != null
                || miembro.getProyecto() == null
                || miembro.getPerfil() == null) {
            return;
        }

        if (RolProyectoUsuario.desde(miembro.getRolProyecto())
                == RolProyectoUsuario.DIRECTOR) {
            return;
        }

        RequerimientoGralProyecto candidato = resolverParaIncorporacion(
                miembro.getProyecto(), miembro.getPerfil(), null);
        if (candidato == null) {
            return;
        }

        miembro.setRequerimientoGral(candidato);
        miembroProyectoRepository.saveAndFlush(miembro);
        publicarCupoModificado(candidato.getIdRequerimientoGral());
    }

    /**
     * Asigna, reasigna o desasigna un miembro a un requerimiento general.
     * {@code idRequerimientoGral = null} desasigna.
     */
    @Transactional
    @AuditarOperacion
    public AsignacionRequerimientoGralResponse asignar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idMiembro,
            Long idRequerimientoGral) {

        validarPerfilActivo(idUsuario, idPerfilActivo);

        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto,
                idPerfilActivo,
                ProyectoSecurityService.PERMISO_ASIGNAR_REQUERIMIENTO);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);
        validarEstadoProyecto(proyecto);

        RequerimientoGralProyecto destino = null;
        if (idRequerimientoGral != null) {
            destino = requerimientoGralProyectoRepository
                    .findByIdParaGestion(idRequerimientoGral)
                    .orElseThrow(() -> new RequerimientoNoEncontradoException(
                            "El requerimiento general " + idRequerimientoGral
                                    + " no existe."));

            if (!destino.getProyecto().getIdProyecto().equals(idProyecto)) {
                throw new RequerimientoNoEncontradoException(
                        "El requerimiento no pertenece al proyecto indicado.");
            }
        }

        MiembroProyecto miembro = miembroProyectoRepository
                .findByIdMiembroAndProyectoForUpdate(idMiembro, idProyecto)
                .orElseThrow(() -> new RequerimientoGralNoAsignableException(
                        "El miembro no pertenece al proyecto.",
                        "MIEMBRO_NO_PERTENECE"));

        validarMiembroAsignable(miembro);

        if (destino == null) {
            return desasignarInterno(miembro);
        }

        Long anteriorId = miembro.getRequerimientoGral() != null
                ? miembro.getRequerimientoGral().getIdRequerimientoGral()
                : null;

        if (Objects.equals(anteriorId, destino.getIdRequerimientoGral())) {
            return respuesta(miembro, destino);
        }

        validarProfesion(miembro.getPerfil(), destino);

        CupoRequerimiento cupo = cupoRequerimientoService.calcularGeneral(destino);
        if (cupo.satisfecho()) {
            throw new RequerimientoGralNoAsignableException(
                    "El requerimiento general ya está satisfecho.",
                    "REQUERIMIENTO_SATISFECHO");
        }

        miembro.setRequerimientoGral(destino);
        miembroProyectoRepository.saveAndFlush(miembro);
        publicarCupoModificado(destino.getIdRequerimientoGral());

        notificar(
                miembro,
                anteriorId == null
                        ? NotificacionService.TIPO_REQUERIMIENTO_ASIGNADO
                        : NotificacionService.TIPO_REQUERIMIENTO_REASIGNADO,
                destino);

        return respuesta(miembro, destino);
    }

    private AsignacionRequerimientoGralResponse desasignarInterno(
            MiembroProyecto miembro) {

        RequerimientoGralProyecto anterior = miembro.getRequerimientoGral();
        if (anterior == null) {
            return respuesta(miembro, null);
        }

        miembro.setRequerimientoGral(null);
        miembroProyectoRepository.saveAndFlush(miembro);

        CupoRequerimiento cupoAnterior =
                cupoRequerimientoService.calcularGeneral(anterior);
        publicarCupoModificado(anterior.getIdRequerimientoGral());

        notificar(
                miembro,
                NotificacionService.TIPO_REQUERIMIENTO_DESASIGNADO,
                anterior);

        return new AsignacionRequerimientoGralResponse(
                miembro.getIdMiembro(),
                miembro.getPerfil().getIdPerfil(),
                anterior.getIdRequerimientoGral(),
                cupoAnterior,
                cupoAnterior.satisfecho(),
                cupoAnterior.faltantes());
    }

    private AsignacionRequerimientoGralResponse respuesta(
            MiembroProyecto miembro,
            RequerimientoGralProyecto requerimiento) {

        if (requerimiento == null) {
            return new AsignacionRequerimientoGralResponse(
                    miembro.getIdMiembro(),
                    miembro.getPerfil().getIdPerfil(),
                    null,
                    null,
                    Boolean.TRUE,
                    0);
        }

        CupoRequerimiento cupo =
                cupoRequerimientoService.calcularGeneral(requerimiento);
        return new AsignacionRequerimientoGralResponse(
                miembro.getIdMiembro(),
                miembro.getPerfil().getIdPerfil(),
                requerimiento.getIdRequerimientoGral(),
                cupo,
                cupo.satisfecho(),
                cupo.faltantes());
    }

    private void validarPerfilActivo(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }
    }

    private void validarEstadoProyecto(Proyecto proyecto) {
        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new RequerimientoGralNoAsignableException(
                    "Solo se puede asignar un requerimiento general en proyectos Publicados o Confirmados.",
                    "PROYECTO_ESTADO_INVALIDO");
        }
    }

    private void validarMiembroAsignable(MiembroProyecto miembro) {
        if (miembro.getEstadoParticipacion() != EstadoParticipacion.ACTIVO) {
            throw new RequerimientoGralNoAsignableException(
                    "El miembro no está activo en el proyecto.",
                    "MIEMBRO_NO_ACTIVO");
        }
        if (RolProyectoUsuario.desde(miembro.getRolProyecto())
                == RolProyectoUsuario.DIRECTOR) {
            throw new RequerimientoGralNoAsignableException(
                    "Un Director no cubre requerimientos generales.",
                    "MIEMBRO_DIRECTOR");
        }
        if (miembro.getPerfil() == null || miembro.getPerfil().getProfesion() == null) {
            throw new RequerimientoGralNoAsignableException(
                    "El miembro no tiene una profesión configurada.",
                    "PROFESION_INCOMPATIBLE");
        }
    }

    private void validarProfesion(
            Perfil perfil,
            RequerimientoGralProyecto requerimiento) {

        if (perfil == null || perfil.getProfesion() == null
                || requerimiento.getProfesion() == null
                || !perfil.getProfesion().getIdProfesion()
                        .equals(requerimiento.getProfesion().getIdProfesion())) {
            throw new RequerimientoGralNoAsignableException(
                    "El miembro no tiene la profesión requerida.",
                    "PROFESION_INCOMPATIBLE");
        }
    }

    private void notificar(
            MiembroProyecto miembro,
            String tipo,
            RequerimientoGralProyecto requerimiento) {

        if (miembro.getPerfil() == null
                || miembro.getPerfil().getUsuario() == null
                || miembro.getProyecto() == null) {
            return;
        }

        String profesion = requerimiento.getProfesion() != null
                ? requerimiento.getProfesion().getNombre()
                : "profesión no informada";

        String mensaje = switch (tipo) {
            case NotificacionService.TIPO_REQUERIMIENTO_ASIGNADO ->
                    "Fuiste asignado al requerimiento general de "
                            + profesion + ".";
            case NotificacionService.TIPO_REQUERIMIENTO_REASIGNADO ->
                    "Fuiste reasignado al requerimiento general de "
                            + profesion + ".";
            case NotificacionService.TIPO_REQUERIMIENTO_DESASIGNADO ->
                    "Fuiste desasignado del requerimiento general de "
                            + profesion + ".";
            default -> "Se modificó tu requerimiento general.";
        };

        notificacionService.registrarTodas(List.of(
                notificacionService.crear(
                        miembro.getPerfil().getUsuario(),
                        miembro.getPerfil(),
                        tipo,
                        mensaje,
                        NotificacionDestinoResolver.ENTIDAD_PROYECTO,
                        miembro.getProyecto().getIdProyecto(),
                        miembro.getProyecto().getIdProyecto())));
    }

    public void publicarCupoModificado(Long idRequerimientoGral) {
        if (idRequerimientoGral != null) {
            eventPublisher.publishEvent(new CupoRequerimientoGralModificadoEvent(
                    idRequerimientoGral));
        }
    }
}
