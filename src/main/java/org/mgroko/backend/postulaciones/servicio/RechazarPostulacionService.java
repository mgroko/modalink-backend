package org.mgroko.backend.postulaciones.servicio;

import java.util.List;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.postulaciones.dto.PostulacionGestionadaResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.PostulacionNoEncontradaException;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-34: rechazar una postulación general o de actividad.
 */
@Service
public class RechazarPostulacionService {

    static final String PERMISO_GESTIONAR_POSTULACIONES = "GESTIONAR_POSTULACIONES";

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final NotificacionService notificacionService;

    public RechazarPostulacionService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            PostulacionGralRepository postulacionGralRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            ProyectoSecurityService proyectoSecurityService,
            NotificacionService notificacionService) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public PostulacionGestionadaResponse rechazarGeneral(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idPostulacion) {

        Proyecto proyecto = cargarProyectoAutorizado(
                idUsuario, idPerfilActivo, idProyecto);

        PostulacionGral postulacion = postulacionGralRepository
                .findByIdParaGestion(idPostulacion)
                .orElseThrow(PostulacionNoEncontradaException::new);

        if (!postulacion.getRequerimientoGral().getProyecto().getIdProyecto()
                .equals(idProyecto)) {
            throw new PostulacionNoEncontradaException();
        }

        if (postulacion.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new PostulacionYaGestionadaException();
        }

        postulacion.setEstado(EstadoSolicitud.RECHAZADA);
        postulacion = postulacionGralRepository.saveAndFlush(postulacion);

        notificarRechazo(
                postulacion.getPerfil(),
                "Tu postulación al proyecto '" + proyecto.getNombre()
                        + "' fue rechazada.",
                "POSTULACION_GRAL",
                postulacion.getIdPostulacionGral(),
                proyecto.getIdProyecto());

        return new PostulacionGestionadaResponse(
                postulacion.getIdPostulacionGral(),
                TipoPostulacion.GENERAL,
                postulacion.getEstado(),
                proyecto.getIdProyecto(),
                postulacion.getRequerimientoGral().getIdRequerimientoGral(),
                postulacion.getPerfil().getIdPerfil(),
                null,
                null);
    }

    @Transactional
    public PostulacionGestionadaResponse rechazarActividad(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idPostulacion) {

        Proyecto proyecto = cargarProyectoAutorizado(
                idUsuario, idPerfilActivo, idProyecto);

        PostulacionActividad postulacion = postulacionActividadRepository
                .findByIdParaGestion(idPostulacion)
                .orElseThrow(PostulacionNoEncontradaException::new);

        if (!postulacion.getRequerimiento().getActividad().getPlanificacion()
                .getProyecto().getIdProyecto().equals(idProyecto)) {
            throw new PostulacionNoEncontradaException();
        }

        if (postulacion.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new PostulacionYaGestionadaException();
        }

        postulacion.setEstado(EstadoSolicitud.RECHAZADA);
        postulacion = postulacionActividadRepository.saveAndFlush(postulacion);

        notificarRechazo(
                postulacion.getPerfil(),
                "Tu postulación a la actividad '"
                        + postulacion.getRequerimiento().getActividad().getNombre()
                        + "' del proyecto '" + proyecto.getNombre()
                        + "' fue rechazada.",
                "POSTULACION_ACTIVIDAD",
                postulacion.getIdPostulacion(),
                proyecto.getIdProyecto());

        return new PostulacionGestionadaResponse(
                postulacion.getIdPostulacion(),
                TipoPostulacion.ACTIVIDAD,
                postulacion.getEstado(),
                proyecto.getIdProyecto(),
                postulacion.getRequerimiento().getIdRequerimiento(),
                postulacion.getPerfil().getIdPerfil(),
                null,
                null);
    }

    private Proyecto cargarProyectoAutorizado(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfilDirector = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfilDirector.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, PERMISO_GESTIONAR_POSTULACIONES);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new PostulacionNoPermitidaException(
                    "El proyecto no permite rechazar postulaciones en estado "
                            + proyecto.getEstado().getNombre() + ".");
        }

        return proyecto;
    }

    private void notificarRechazo(
            Perfil postulante,
            String mensaje,
            String entidadTipo,
            Long entidadId,
            Long idProyecto) {

        if (postulante == null || postulante.getUsuario() == null) {
            return;
        }

        Notificacion notificacion = notificacionService.crear(
                postulante.getUsuario(),
                postulante,
                NotificacionService.TIPO_POSTULACION_RECHAZADA,
                mensaje,
                entidadTipo,
                entidadId,
                idProyecto);

        notificacionService.registrarTodas(List.of(notificacion));
    }
}
