package org.mgroko.backend.publicaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.ColaboracionAutomaticaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancelación y disolución de colaboraciones automáticas por cambios en las
 * publicaciones o imágenes.
 */
@Service
public class ColaboracionAutomaticaLifecycleService {

    private final ColaboracionAutomaticaRepository colaboracionRepository;
    private final PublicacionRepository publicacionRepository;
    private final ColaboradorPublicacionOrigenService colaboradorOrigenService;
    private final NotificacionService notificacionService;
    private final Clock reloj;

    public ColaboracionAutomaticaLifecycleService(
            ColaboracionAutomaticaRepository colaboracionRepository,
            PublicacionRepository publicacionRepository,
            ColaboradorPublicacionOrigenService colaboradorOrigenService,
            NotificacionService notificacionService,
            Clock reloj) {
        this.colaboracionRepository = colaboracionRepository;
        this.publicacionRepository = publicacionRepository;
        this.colaboradorOrigenService = colaboradorOrigenService;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    @Transactional
    public void cancelarPendientesPorPublicacion(Long idPublicacion) {
        if (idPublicacion == null) {
            return;
        }

        List<ColaboracionAutomatica> pendientes = colaboracionRepository
                .findByPublicacionYEstado(
                        idPublicacion, EstadoColaboracionAutomatica.PENDIENTE);

        for (ColaboracionAutomatica colaboracion : pendientes) {
            cancelar(colaboracion);
        }
    }

    @Transactional
    public void cancelarPendientesPorImagen(Long idImagen) {
        if (idImagen == null) {
            return;
        }

        List<ColaboracionAutomatica> pendientes = colaboracionRepository
                .findByImagenYEstado(idImagen, EstadoColaboracionAutomatica.PENDIENTE);

        for (ColaboracionAutomatica colaboracion : pendientes) {
            cancelar(colaboracion);
        }
    }

    /**
     * Disuelve las colaboraciones confirmadas cuando un perfil se quita la
     * etiqueta de colaborador de una publicación.
     */
    @Transactional
    public void disolverPorEtiqueta(Long idPublicacion, Long idPerfil) {
        if (idPublicacion == null || idPerfil == null) {
            return;
        }

        List<ColaboracionAutomatica> confirmadas = colaboracionRepository
                .findConfirmadaPorPublicacionYPerfil(
                        idPublicacion,
                        idPerfil,
                        EstadoColaboracionAutomatica.CONFIRMADA);

        for (ColaboracionAutomatica colaboracion : confirmadas) {
            disolver(colaboracion, idPublicacion, idPerfil);
        }
    }

    private void cancelar(ColaboracionAutomatica colaboracion) {
        colaboracion.setEstado(EstadoColaboracionAutomatica.CANCELADA);
        colaboracionRepository.saveAndFlush(colaboracion);
        notificacionService.eliminarPorEntidad(
                NotificacionService.ENTIDAD_COLABORACION_AUTOMATICA,
                colaboracion.getIdColaboracionAutomatica());
    }

    private void disolver(
            ColaboracionAutomatica colaboracion,
            Long idPublicacion,
            Long idPerfil) {

        Publicacion otraPublicacion;
        Perfil otroPerfil;

        if (colaboracion.getPublicacionA() != null
                && colaboracion.getPublicacionA().getIdPublicacion().equals(idPublicacion)) {
            otraPublicacion = colaboracion.getPublicacionB();
            otroPerfil = colaboracion.getPerfilA();
        } else {
            otraPublicacion = colaboracion.getPublicacionA();
            otroPerfil = colaboracion.getPerfilB();
        }

        if (otraPublicacion != null && otroPerfil != null) {
            publicacionRepository.eliminarColaboradorPublicacion(
                    otraPublicacion.getIdPublicacion(), otroPerfil.getIdPerfil());
            colaboradorOrigenService.eliminar(
                    otraPublicacion.getIdPublicacion(), otroPerfil.getIdPerfil());
        }

        colaboracion.setEstado(EstadoColaboracionAutomatica.DISUELTA);
        colaboracion.setFechaDisolucion(LocalDateTime.now(reloj));
        colaboracionRepository.saveAndFlush(colaboracion);

        notificarDisolucion(colaboracion);
    }

    private void notificarDisolucion(ColaboracionAutomatica colaboracion) {
        List<Notificacion> notificaciones = new ArrayList<>();
        for (Perfil perfil : List.of(colaboracion.getPerfilA(), colaboracion.getPerfilB())) {
            if (perfil.getUsuario() != null
                    && perfil.getUsuario().getEstado() == EstadoUsuario.ACTIVO) {
                notificaciones.add(notificacionService.crear(
                        perfil.getUsuario(),
                        perfil,
                        NotificacionService.TIPO_COLABORACION_AUTOMATICA_DISUELTA,
                        "La colaboración automática fue disuelta.",
                        NotificacionService.ENTIDAD_COLABORACION_AUTOMATICA,
                        colaboracion.getIdColaboracionAutomatica()));
            }
        }
        notificacionService.registrarTodas(notificaciones);
    }
}
