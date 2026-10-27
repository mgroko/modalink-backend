package org.mgroko.backend.postulaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.postulaciones.dto.PostulacionResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-26: alta de postulación a un requerimiento general o de actividad.
 *
 * <p>El flujo de actividad verifica la disponibilidad del perfil frente al
 * rango factible calculado por CPM. Esa verificación puede ser provisional
 * mientras el proyecto no tenga fecha de inicio confirmada; en ese caso la
 * respuesta lo indica y la comprobación debe repetirse en UC-60/UC-70.</p>
 */
@Service
public class CrearPostulacionService {

    static final String ENTIDAD_POSTULACION_ACTIVIDAD = "POSTULACION_ACTIVIDAD";
    static final String ENTIDAD_POSTULACION_GRAL = "POSTULACION_GRAL";

    private final PerfilRepository perfilRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final NotificacionService notificacionService;
    private final CalendarioService calendarioService;
    private final Clock reloj;

    public CrearPostulacionService(
            PerfilRepository perfilRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            PostulacionGralRepository postulacionGralRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            CupoRequerimientoService cupoRequerimientoService,
            NotificacionService notificacionService,
            CalendarioService calendarioService,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.notificacionService = notificacionService;
        this.calendarioService = calendarioService;
        this.reloj = reloj;
    }

    /**
     * Da de alta una postulación a un requerimiento general de proyecto.
     * No evalúa disponibilidad: el perfil todavía no asume una actividad.
     */
    @Transactional
    public PostulacionResponse darDeAltaPostulacionGeneral(
            Long idUsuario,
            Long idPerfil,
            Long idProyecto,
            Long idRequerimientoGral) {

        Perfil perfil = perfilActivo(idUsuario, idPerfil);

        RequerimientoGralProyecto requerimiento = requerimientoGralProyectoRepository
                .findByIdConProyecto(idRequerimientoGral)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(
                        "El requerimiento general " + idRequerimientoGral + " no existe."));

        Proyecto proyecto = requerimiento.getProyecto();
        validarProyecto(idProyecto, proyecto);

        if (!Boolean.TRUE.equals(proyecto.getAceptaPostulacionGral())) {
            throw new PostulacionNoPermitidaException(
                    "El proyecto no acepta postulaciones a requerimientos generales.");
        }

        validarPerfilApto(
                perfil,
                proyecto,
                requerimiento.getProfesion().getIdProfesion(),
                TipoPostulacion.GENERAL);
        validarCupoGeneral(requerimiento);
        validarNoDuplicadoGeneral(perfil, requerimiento);

        PostulacionGral postulacion = PostulacionGral.builder()
                .perfil(perfil)
                .requerimientoGral(requerimiento)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaPostulacion(LocalDateTime.now(reloj))
                .build();

        try {
            postulacion = postulacionGralRepository.saveAndFlush(postulacion);
        } catch (DataIntegrityViolationException ex) {
            throw new PostulacionNoPermitidaException(
                    "Ya existe una postulación de este perfil a este requerimiento.");
        }

        notificarDirectores(
                proyecto,
                perfil,
                postulacion.getIdPostulacionGral(),
                ENTIDAD_POSTULACION_GRAL,
                "Nueva postulación de " + perfil.getNombreArtistico()
                        + " al proyecto '" + proyecto.getNombre() + "'.");

        return new PostulacionResponse(
                postulacion.getIdPostulacionGral(),
                TipoPostulacion.GENERAL,
                postulacion.getEstado(),
                postulacion.getFechaPostulacion(),
                proyecto.getIdProyecto(),
                requerimiento.getIdRequerimientoGral(),
                false);
    }

    /**
     * Da de alta una postulación a un requerimiento de actividad. Antes de
     * persistir verifica que el perfil tenga un hueco libre suficiente dentro
     * del rango factible de la actividad.
     */
    @Transactional
    public PostulacionResponse darDeAltaPostulacionActividad(
            Long idUsuario,
            Long idPerfil,
            Long idProyecto,
            Long idRequerimientoActividad) {

        Perfil perfil = perfilActivo(idUsuario, idPerfil);

        RequerimientoActividad requerimiento = requerimientoActividadRepository
                .findByIdConActividadYProyecto(idRequerimientoActividad)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(
                        "El requerimiento de actividad " + idRequerimientoActividad + " no existe."));

        Actividad actividad = requerimiento.getActividad();
        Proyecto proyecto = actividad.getPlanificacion().getProyecto();
        validarProyecto(idProyecto, proyecto);

        if (actividad.getEstado() != EstadoActividad.PENDIENTE) {
            throw new PostulacionNoPermitidaException(
                    "Sólo se puede postular a actividades pendientes.");
        }

        validarPerfilApto(
                perfil,
                proyecto,
                requerimiento.getProfesion().getIdProfesion(),
                TipoPostulacion.ACTIVIDAD);
        validarCupoActividad(requerimiento);
        validarNoDuplicadoActividad(perfil, requerimiento);

        ResultadoDisponibilidad disponibilidad =
                calendarioService.verificarDisponibilidad(idUsuario, actividad);
        if (!disponibilidad.disponible()) {
            throw new PostulacionNoPermitidaException(
                    "No tenés disponibilidad suficiente dentro del rango factible de la actividad.");
        }

        PostulacionActividad postulacion = PostulacionActividad.builder()
                .perfil(perfil)
                .requerimiento(requerimiento)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaPostulacion(LocalDateTime.now(reloj))
                .build();

        try {
            postulacion = postulacionActividadRepository.saveAndFlush(postulacion);
        } catch (DataIntegrityViolationException ex) {
            throw new PostulacionNoPermitidaException(
                    "Ya existe una postulación de este perfil a este requerimiento.");
        }

        notificarDirectores(
                proyecto,
                perfil,
                postulacion.getIdPostulacion(),
                ENTIDAD_POSTULACION_ACTIVIDAD,
                "Nueva postulación de " + perfil.getNombreArtistico()
                        + " a la actividad '" + actividad.getNombre()
                        + "' del proyecto '" + proyecto.getNombre() + "'.");

        return new PostulacionResponse(
                postulacion.getIdPostulacion(),
                TipoPostulacion.ACTIVIDAD,
                postulacion.getEstado(),
                postulacion.getFechaPostulacion(),
                proyecto.getIdProyecto(),
                requerimiento.getIdRequerimiento(),
                disponibilidad.provisional());
    }

    private Perfil perfilActivo(Long idUsuario, Long idPerfil) {
        if (idPerfil == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfil, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }
        return perfil;
    }

    private void validarProyecto(Long idProyecto, Proyecto proyecto) {
        if (!proyecto.getIdProyecto().equals(idProyecto)) {
            throw new RequerimientoNoEncontradoException(
                    "El requerimiento no pertenece al proyecto indicado.");
        }
        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new PostulacionNoPermitidaException(
                    "El proyecto ya no acepta postulaciones.");
        }
    }

    private void validarPerfilApto(
            Perfil perfil,
            Proyecto proyecto,
            Long idProfesionRequerimiento,
            TipoPostulacion tipo) {

        var miembroActivo = miembroProyectoRepository
                .findMiembroActivoConRol(proyecto.getIdProyecto(), perfil.getIdPerfil());

        if (miembroActivo.isPresent()) {
            if (tipo == TipoPostulacion.GENERAL) {
                throw new PostulacionNoPermitidaException(
                        "Ya sos miembro activo de este proyecto.");
            }

            if (RolProyectoUsuario.desde(miembroActivo.get().getRolProyecto())
                    == RolProyectoUsuario.DIRECTOR) {
                throw new PostulacionNoPermitidaException(
                        "Un director no puede postularse a una actividad.");
            }
        }

        if (perfil.getProfesion() == null
                || !perfil.getProfesion().getIdProfesion().equals(idProfesionRequerimiento)) {
            throw new PostulacionNoPermitidaException(
                    "Tu perfil no tiene la profesión solicitada por el requerimiento.");
        }
    }

    private void validarCupoActividad(RequerimientoActividad requerimiento) {
        CupoRequerimiento cupo = cupoRequerimientoService.calcularActividad(requerimiento);

        if (cupo.satisfecho()) {
            throw new PostulacionNoPermitidaException(
                    "El requerimiento de actividad ya está satisfecho.");
        }
    }

    private void validarCupoGeneral(RequerimientoGralProyecto requerimiento) {
        CupoRequerimiento cupo = cupoRequerimientoService.calcularGeneral(requerimiento);

        if (cupo.satisfecho()) {
            throw new PostulacionNoPermitidaException(
                    "El requerimiento general ya está satisfecho.");
        }
    }

    private void validarNoDuplicadoActividad(
            Perfil perfil,
            RequerimientoActividad requerimiento) {

        if (postulacionActividadRepository
                .existsByPerfil_IdPerfilAndRequerimiento_IdRequerimiento(
                        perfil.getIdPerfil(), requerimiento.getIdRequerimiento())) {
            throw new PostulacionNoPermitidaException(
                    "Ya existe una postulación de este perfil a este requerimiento.");
        }
    }

    private void validarNoDuplicadoGeneral(
            Perfil perfil,
            RequerimientoGralProyecto requerimiento) {

        if (postulacionGralRepository
                .existsByPerfil_IdPerfilAndRequerimientoGral_IdRequerimientoGral(
                        perfil.getIdPerfil(), requerimiento.getIdRequerimientoGral())) {
            throw new PostulacionNoPermitidaException(
                    "Ya existe una postulación de este perfil a este requerimiento.");
        }
    }

    private void notificarDirectores(
            Proyecto proyecto,
            Perfil postulante,
            Long idPostulacion,
            String entidadTipo,
            String mensaje) {

        List<MiembroProyecto> directores = miembroProyectoRepository
                .buscarDirectoresDeProyectos(List.of(proyecto.getIdProyecto()));

        if (directores.isEmpty()) {
            return;
        }

        List<Notificacion> notificaciones = new ArrayList<>();
        for (MiembroProyecto director : directores) {
            Perfil perfilDirector = director.getPerfil();
            if (perfilDirector == null || perfilDirector.getUsuario() == null) {
                continue;
            }
            notificaciones.add(notificacionService.crear(
                    perfilDirector.getUsuario(),
                    perfilDirector,
                    NotificacionService.TIPO_POSTULACION_RECIBIDA,
                    mensaje,
                    entidadTipo,
                    idPostulacion));
        }

        notificacionService.registrarTodas(notificaciones);
    }
}
