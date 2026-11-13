package org.mgroko.backend.publicaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoConfirmacionColaboracion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.publicaciones.dto.ColaboracionAutomaticaResponse;
import org.mgroko.backend.publicaciones.exception.ColaboracionAutomaticaInvalidaException;
import org.mgroko.backend.publicaciones.exception.ColaboracionAutomaticaNoEncontradaException;
import org.mgroko.backend.publicaciones.exception.ColaboracionAutomaticaNoPendienteException;
import org.mgroko.backend.publicaciones.exception.ColaboracionAutomaticaNoPermitidaException;
import org.mgroko.backend.publicaciones.mapper.ColaboracionAutomaticaMapper;
import org.mgroko.backend.repositorio.ColaboracionAutomaticaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirmación o rechazo de una colaboración automática.
 */
@Service
public class GestionarColaboracionAutomaticaService {

    private final PerfilActivoService perfilActivoService;
    private final ColaboracionAutomaticaRepository colaboracionRepository;
    private final PublicacionRepository publicacionRepository;
    private final NotificacionService notificacionService;
    private final ColaboradorPublicacionOrigenService colaboradorOrigenService;
    private final Clock reloj;

    public GestionarColaboracionAutomaticaService(
            PerfilActivoService perfilActivoService,
            ColaboracionAutomaticaRepository colaboracionRepository,
            PublicacionRepository publicacionRepository,
            NotificacionService notificacionService,
            ColaboradorPublicacionOrigenService colaboradorOrigenService,
            Clock reloj) {
        this.perfilActivoService = perfilActivoService;
        this.colaboracionRepository = colaboracionRepository;
        this.publicacionRepository = publicacionRepository;
        this.notificacionService = notificacionService;
        this.colaboradorOrigenService = colaboradorOrigenService;
        this.reloj = reloj;
    }

    @Transactional
    public ColaboracionAutomaticaResponse confirmar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idColaboracionAutomatica) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);
        ColaboracionAutomatica colaboracion = bloquearYValidar(
                idColaboracionAutomatica, perfilActivo);

        validarPublicacionesDisponibles(colaboracion);

        boolean esPerfilA = colaboracion.getPerfilA().getIdPerfil()
                .equals(perfilActivo.getIdPerfil());
        EstadoConfirmacionColaboracion estadoActual = esPerfilA
                ? colaboracion.getEstadoPerfilA()
                : colaboracion.getEstadoPerfilB();

        if (estadoActual != EstadoConfirmacionColaboracion.PENDIENTE) {
            throw new ColaboracionAutomaticaNoPendienteException(
                    "La colaboración automática ya fue respondida por este perfil.");
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (esMismoUsuario(colaboracion)) {
            colaboracion.setEstadoPerfilA(EstadoConfirmacionColaboracion.CONFIRMADA);
            colaboracion.setEstadoPerfilB(EstadoConfirmacionColaboracion.CONFIRMADA);
            colaboracion.setFechaRespuestaA(ahora);
            colaboracion.setFechaRespuestaB(ahora);
        } else if (esPerfilA) {
            colaboracion.setEstadoPerfilA(EstadoConfirmacionColaboracion.CONFIRMADA);
            colaboracion.setFechaRespuestaA(ahora);
        } else {
            colaboracion.setEstadoPerfilB(EstadoConfirmacionColaboracion.CONFIRMADA);
            colaboracion.setFechaRespuestaB(ahora);
        }

        boolean ambasConfirmadas = colaboracion.getEstadoPerfilA()
                == EstadoConfirmacionColaboracion.CONFIRMADA
                && colaboracion.getEstadoPerfilB()
                == EstadoConfirmacionColaboracion.CONFIRMADA;

        if (ambasConfirmadas) {
            agregarColaborador(
                    colaboracion.getPublicacionA(),
                    colaboracion.getPerfilB(),
                    colaboracion.getIdColaboracionAutomatica());
            agregarColaborador(
                    colaboracion.getPublicacionB(),
                    colaboracion.getPerfilA(),
                    colaboracion.getIdColaboracionAutomatica());

            colaboracion.setEstado(EstadoColaboracionAutomatica.CONFIRMADA);
            colaboracion.setFechaConfirmacion(ahora);
        }

        colaboracion = colaboracionRepository.saveAndFlush(colaboracion);

        if (ambasConfirmadas) {
            notificar(
                    colaboracion,
                    NotificacionService.TIPO_COLABORACION_AUTOMATICA_CONFIRMADA,
                    "La colaboración automática fue confirmada por ambos perfiles.",
                    perfilActivo);
        }

        return ColaboracionAutomaticaMapper.toResponse(
                colaboracion, perfilActivo.getIdPerfil());
    }

    @Transactional
    public ColaboracionAutomaticaResponse rechazar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idColaboracionAutomatica) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);
        ColaboracionAutomatica colaboracion = bloquearYValidar(
                idColaboracionAutomatica, perfilActivo);

        boolean esPerfilA = colaboracion.getPerfilA().getIdPerfil()
                .equals(perfilActivo.getIdPerfil());
        EstadoConfirmacionColaboracion estadoActual = esPerfilA
                ? colaboracion.getEstadoPerfilA()
                : colaboracion.getEstadoPerfilB();

        if (estadoActual != EstadoConfirmacionColaboracion.PENDIENTE) {
            throw new ColaboracionAutomaticaNoPendienteException(
                    "La colaboración automática ya fue respondida por este perfil.");
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (esMismoUsuario(colaboracion)) {
            colaboracion.setEstadoPerfilA(EstadoConfirmacionColaboracion.RECHAZADA);
            colaboracion.setEstadoPerfilB(EstadoConfirmacionColaboracion.RECHAZADA);
            colaboracion.setFechaRespuestaA(ahora);
            colaboracion.setFechaRespuestaB(ahora);
        } else if (esPerfilA) {
            colaboracion.setEstadoPerfilA(EstadoConfirmacionColaboracion.RECHAZADA);
            colaboracion.setFechaRespuestaA(ahora);
        } else {
            colaboracion.setEstadoPerfilB(EstadoConfirmacionColaboracion.RECHAZADA);
            colaboracion.setFechaRespuestaB(ahora);
        }

        colaboracion.setEstado(EstadoColaboracionAutomatica.RECHAZADA);
        colaboracion = colaboracionRepository.saveAndFlush(colaboracion);

        notificar(
                colaboracion,
                NotificacionService.TIPO_COLABORACION_AUTOMATICA_RECHAZADA,
                "La colaboración automática fue rechazada.",
                perfilActivo);

        return ColaboracionAutomaticaMapper.toResponse(
                colaboracion, perfilActivo.getIdPerfil());
    }

    private ColaboracionAutomatica bloquearYValidar(
            Long idColaboracionAutomatica,
            Perfil perfilActivo) {

        ColaboracionAutomatica colaboracion = colaboracionRepository
                .findByIdForUpdate(idColaboracionAutomatica)
                .orElseThrow(ColaboracionAutomaticaNoEncontradaException::new);

        boolean participa = colaboracion.getPerfilA().getIdPerfil()
                .equals(perfilActivo.getIdPerfil())
                || colaboracion.getPerfilB().getIdPerfil()
                        .equals(perfilActivo.getIdPerfil());

        if (!participa) {
            throw new ColaboracionAutomaticaNoPermitidaException(
                    "La colaboración automática no pertenece al perfil activo.");
        }

        if (colaboracion.getEstado() != EstadoColaboracionAutomatica.PENDIENTE) {
            throw new ColaboracionAutomaticaNoPendienteException(
                    "La colaboración automática ya no está pendiente.");
        }

        return colaboracion;
    }

    private void validarPublicacionesDisponibles(ColaboracionAutomatica colaboracion) {
        if (colaboracion.getPublicacionA() == null
                || colaboracion.getPublicacionB() == null) {
            throw new ColaboracionAutomaticaInvalidaException(
                    "Una de las publicaciones ya no está disponible.");
        }

        Publicacion publicacionA = publicacionRepository
                .findByIdPublicacionForUpdate(colaboracion.getPublicacionA().getIdPublicacion())
                .orElseThrow(() -> new ColaboracionAutomaticaInvalidaException(
                        "Una de las publicaciones ya no está disponible."));
        Publicacion publicacionB = publicacionRepository
                .findByIdPublicacionForUpdate(colaboracion.getPublicacionB().getIdPublicacion())
                .orElseThrow(() -> new ColaboracionAutomaticaInvalidaException(
                        "Una de las publicaciones ya no está disponible."));

        validarPublicacionDisponible(publicacionA);
        validarPublicacionDisponible(publicacionB);

        colaboracion.setPublicacionA(publicacionA);
        colaboracion.setPublicacionB(publicacionB);
    }

    private void validarPublicacionDisponible(Publicacion publicacion) {
        Perfil perfil = publicacion.getPerfil();
        if (publicacion.getEstado() != EstadoPublicacion.ACTIVA
                || perfil == null
                || perfil.getEstado() != EstadoPerfil.ACTIVO
                || perfil.getUsuario() == null
                || perfil.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new ColaboracionAutomaticaInvalidaException(
                    "Una de las publicaciones ya no está disponible.");
        }
    }

    private void agregarColaborador(
            Publicacion publicacion,
            Perfil perfil,
            Long idColaboracionAutomatica) {

        if (publicacion == null || perfil == null) {
            return;
        }

        if (!publicacionRepository.existsColaborador(
                publicacion.getIdPublicacion(), perfil.getIdPerfil())) {
            if (publicacion.getColaboradores() == null) {
                publicacion.setColaboradores(new LinkedHashSet<>());
            }
            publicacion.getColaboradores().add(perfil);
            try {
                publicacionRepository.saveAndFlush(publicacion);
            } catch (DataIntegrityViolationException ex) {
                if (!mensajeContiene(ex, "PK_colaborador_publicacion")) {
                    throw ex;
                }
            }
        }

        colaboradorOrigenService.registrarAutomatica(
                publicacion.getIdPublicacion(),
                perfil.getIdPerfil(),
                idColaboracionAutomatica);
    }

    private void notificar(
            ColaboracionAutomatica colaboracion,
            String tipo,
            String mensaje,
            Perfil perfilActivo) {

        List<org.mgroko.backend.modelo.Notificacion> notificaciones = new ArrayList<>();

        if (esMismoUsuario(colaboracion)) {
            if (perfilActivo.getUsuario() != null
                    && perfilActivo.getUsuario().getEstado() == EstadoUsuario.ACTIVO) {
                notificaciones.add(notificacionService.crear(
                        perfilActivo.getUsuario(),
                        perfilActivo,
                        tipo,
                        mensaje,
                        NotificacionService.ENTIDAD_COLABORACION_AUTOMATICA,
                        colaboracion.getIdColaboracionAutomatica()));
            }
        } else {
            for (Perfil perfil : List.of(colaboracion.getPerfilA(), colaboracion.getPerfilB())) {
                if (perfil.getUsuario() != null
                        && perfil.getUsuario().getEstado() == EstadoUsuario.ACTIVO) {
                    notificaciones.add(notificacionService.crear(
                            perfil.getUsuario(),
                            perfil,
                            tipo,
                            mensaje,
                            NotificacionService.ENTIDAD_COLABORACION_AUTOMATICA,
                            colaboracion.getIdColaboracionAutomatica()));
                }
            }
        }

        notificacionService.registrarTodas(notificaciones);
    }

    private boolean esMismoUsuario(ColaboracionAutomatica colaboracion) {
        return colaboracion.getPerfilA() != null
                && colaboracion.getPerfilA().getUsuario() != null
                && colaboracion.getPerfilB() != null
                && colaboracion.getPerfilB().getUsuario() != null
                && colaboracion.getPerfilA().getUsuario().getIdUsuario()
                        .equals(colaboracion.getPerfilB().getUsuario().getIdUsuario());
    }

    private boolean mensajeContiene(DataIntegrityViolationException ex, String texto) {
        Throwable causa = ex.getMostSpecificCause();
        return causa != null && causa.getMessage() != null
                && causa.getMessage().contains(texto);
    }
}
