package org.mgroko.backend.publicaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.ColaboracionAutomaticaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.proyeccion.CoincidenciaImagenProyeccion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detecta imágenes compartidas entre publicaciones y genera propuestas de
 * colaboración automática.
 */
@Service
public class DetectarColaboracionAutomaticaService {

    private static final List<EstadoColaboracionAutomatica> ESTADOS_QUE_BLOQUEAN =
            List.of(EstadoColaboracionAutomatica.PENDIENTE,
                    EstadoColaboracionAutomatica.CONFIRMADA);

    private final PublicacionRepository publicacionRepository;
    private final ColaboracionAutomaticaRepository colaboracionRepository;
    private final NotificacionService notificacionService;
    private final Clock reloj;
    private final boolean habilitada;
    private final int umbralPhash;
    private final int maxPropuestasPorImagen;

    public DetectarColaboracionAutomaticaService(
            PublicacionRepository publicacionRepository,
            ColaboracionAutomaticaRepository colaboracionRepository,
            NotificacionService notificacionService,
            Clock reloj,
            @Value("${app.colaboracion-automatica.habilitada:true}") boolean habilitada,
            @Value("${app.colaboracion-automatica.phash-distancia-maxima:8}") int umbralPhash,
            @Value("${app.colaboracion-automatica.max-propuestas-por-imagen:10}") int maxPropuestasPorImagen) {
        this.publicacionRepository = publicacionRepository;
        this.colaboracionRepository = colaboracionRepository;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
        this.habilitada = habilitada;
        this.umbralPhash = umbralPhash;
        this.maxPropuestasPorImagen = maxPropuestasPorImagen;
    }

    @Transactional
    public void detectar(Publicacion publicacion, List<Imagen> imagenesNuevas) {
        if (!habilitada || publicacion == null || imagenesNuevas == null
                || imagenesNuevas.isEmpty() || publicacion.getPerfil() == null
                || publicacion.getPerfil().getUsuario() == null) {
            return;
        }

        Perfil perfilActual = publicacion.getPerfil();
        Long idUsuarioActual = perfilActual.getUsuario().getIdUsuario();
        Set<Long> publicacionesYaPropuestas = new HashSet<>();

        for (Imagen imagen : imagenesNuevas) {
            if (imagen == null) {
                continue;
            }

            List<CoincidenciaImagenProyeccion> coincidencias = new ArrayList<>();

            if (imagen.getSha256Contenido() != null) {
                coincidencias.addAll(publicacionRepository.buscarCoincidenciasExactas(
                        imagen.getSha256Contenido(),
                        publicacion.getIdPublicacion(),
                        perfilActual.getIdPerfil(),
                        idUsuarioActual,
                        maxPropuestasPorImagen));
            }

            if (imagen.getPhash() != null) {
                coincidencias.addAll(publicacionRepository.buscarCoincidenciasPerceptuales(
                        imagen.getPhash(),
                        umbralPhash,
                        publicacion.getIdPublicacion(),
                        perfilActual.getIdPerfil(),
                        idUsuarioActual,
                        maxPropuestasPorImagen));
            }

            for (CoincidenciaImagenProyeccion coincidencia : coincidencias) {
                if (coincidencia.getIdPublicacion() == null
                        || !publicacionesYaPropuestas.add(coincidencia.getIdPublicacion())) {
                    continue;
                }

                procesarCoincidencia(
                        publicacion,
                        perfilActual,
                        idUsuarioActual,
                        imagen,
                        coincidencia);
            }
        }
    }

    private void procesarCoincidencia(
            Publicacion publicacionActual,
            Perfil perfilActual,
            Long idUsuarioActual,
            Imagen imagen,
            CoincidenciaImagenProyeccion coincidencia) {

        Publicacion otra = publicacionRepository
                .findByIdPublicacion(coincidencia.getIdPublicacion())
                .orElse(null);

        if (otra == null
                || otra.getEstado() != EstadoPublicacion.ACTIVA
                || otra.getPerfil() == null
                || otra.getPerfil().getEstado() != EstadoPerfil.ACTIVO
                || otra.getPerfil().getUsuario() == null
                || otra.getPerfil().getUsuario().getEstado() != EstadoUsuario.ACTIVO
                || otra.getPerfil().getUsuario().getIdUsuario().equals(idUsuarioActual)) {
            return;
        }

        Perfil otroPerfil = otra.getPerfil();

        if (publicacionRepository.existsColaborador(
                publicacionActual.getIdPublicacion(), otroPerfil.getIdPerfil())
                || publicacionRepository.existsColaborador(
                        otra.getIdPublicacion(), perfilActual.getIdPerfil())) {
            return;
        }

        Publicacion publicacionA = publicacionActual.getIdPublicacion()
                < otra.getIdPublicacion() ? publicacionActual : otra;
        Publicacion publicacionB = publicacionActual.getIdPublicacion()
                < otra.getIdPublicacion() ? otra : publicacionActual;
        Perfil perfilA = publicacionA.getPerfil();
        Perfil perfilB = publicacionB.getPerfil();

        if (colaboracionRepository.existsByParYEstados(
                publicacionA.getIdPublicacion(),
                publicacionB.getIdPublicacion(),
                ESTADOS_QUE_BLOQUEAN)) {
            return;
        }

        ColaboracionAutomatica colaboracion = ColaboracionAutomatica.builder()
                .imagen(imagen)
                .sha256Evidencia(imagen.getSha256Contenido())
                .phashVersion(imagen.getPhashVersion())
                .publicacionA(publicacionA)
                .publicacionB(publicacionB)
                .perfilA(perfilA)
                .perfilB(perfilB)
                .estado(EstadoColaboracionAutomatica.PENDIENTE)
                .fechaDeteccion(LocalDateTime.now(reloj))
                .build();

        try {
            colaboracion = colaboracionRepository.saveAndFlush(colaboracion);
        } catch (DataIntegrityViolationException ex) {
            // Carrera contra el índice único de propuestas pendientes.
            return;
        }

        notificarDeteccion(colaboracion, publicacionA, publicacionB, perfilA, perfilB);
    }

    private void notificarDeteccion(
            ColaboracionAutomatica colaboracion,
            Publicacion publicacionA,
            Publicacion publicacionB,
            Perfil perfilA,
            Perfil perfilB) {

        String mensaje = "Detectamos una posible colaboración entre las publicaciones '"
                + publicacionA.getTitulo() + "' y '" + publicacionB.getTitulo()
                + "'. Si ambos confirman, aparecerán como colaboradores en la publicación del otro.";

        List<org.mgroko.backend.modelo.Notificacion> notificaciones = new ArrayList<>();
        agregarNotificacion(notificaciones, notificacionService.crear(
                perfilA.getUsuario(),
                perfilA,
                NotificacionService.TIPO_COLABORACION_AUTOMATICA_DETECTADA,
                mensaje,
                NotificacionService.ENTIDAD_COLABORACION_AUTOMATICA,
                colaboracion.getIdColaboracionAutomatica()));
        agregarNotificacion(notificaciones, notificacionService.crear(
                perfilB.getUsuario(),
                perfilB,
                NotificacionService.TIPO_COLABORACION_AUTOMATICA_DETECTADA,
                mensaje,
                NotificacionService.ENTIDAD_COLABORACION_AUTOMATICA,
                colaboracion.getIdColaboracionAutomatica()));

        notificacionService.registrarTodas(notificaciones);
    }

    private void agregarNotificacion(
            List<org.mgroko.backend.modelo.Notificacion> notificaciones,
            org.mgroko.backend.modelo.Notificacion notificacion) {
        if (notificacion != null) {
            notificaciones.add(notificacion);
        }
    }
}
