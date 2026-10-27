package org.mgroko.backend.notificaciones.servicio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;
import org.mgroko.backend.notificaciones.dto.NotificacionResumenResponse;
import org.mgroko.backend.notificaciones.exception.NotificacionNoEncontradaException;
import org.mgroko.backend.notificaciones.mapper.NotificacionMapper;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {

    public static final String TIPO_PROYECTO_CANCELADO = "PROYECTO_CANCELADO";
    public static final String TIPO_ACTIVIDAD_MODIFICADA = "ACTIVIDAD_MODIFICADA";
    public static final String TIPO_ACTIVIDAD_ELIMINADA = "ACTIVIDAD_ELIMINADA";
    public static final String TIPO_POSTULACION_RECIBIDA = "POSTULACION_RECIBIDA";
    public static final String TIPO_POSTULACION_ACEPTADA = "POSTULACION_ACEPTADA";
    public static final String TIPO_POSTULACION_RECHAZADA = "POSTULACION_RECHAZADA";
    public static final String TIPO_INVITACION_RECHAZADA = "INVITACION_RECHAZADA";
    public static final String ORIGEN_SERVICIO = "SERVICIO";

    private static final int TAMANO_PAGINA_POR_DEFECTO = 20;
    private static final int TAMANO_PAGINA_MAXIMO = 100;

    private final NotificacionRepository notificacionRepository;
    private final PerfilRepository perfilRepository;

    public NotificacionService(
            NotificacionRepository notificacionRepository,
            PerfilRepository perfilRepository) {
        this.notificacionRepository = notificacionRepository;
        this.perfilRepository = perfilRepository;
    }

    public Notificacion crear(
            Usuario usuarioDestino,
            String tipo,
            String mensaje,
            String entidadTipo,
            Long entidadId) {
        return crear(usuarioDestino, null, tipo, mensaje, entidadTipo, entidadId);
    }

    public Notificacion crear(
            Usuario usuarioDestino,
            Perfil perfilContexto,
            String tipo,
            String mensaje,
            String entidadTipo,
            Long entidadId) {

        return Notificacion.builder()
                .usuarioDestino(usuarioDestino)
                .perfilContexto(perfilContexto)
                .tipo(tipo)
                .mensaje(mensaje)
                .entidadTipo(entidadTipo)
                .entidadId(entidadId)
                .leida(false)
                .fechaLeida(null)
                .origen(ORIGEN_SERVICIO)
                .build();
    }

    @Transactional(readOnly = true)
    public PaginaResponse<NotificacionResponse> listar(
            Long idUsuario,
            Long idPerfilContexto,
            int page,
            int size) {

        validarPerfilPropio(idUsuario, idPerfilContexto);
        Pageable pageable = crearPageable(page, size);

        Page<Notificacion> resultado = idPerfilContexto == null
                ? notificacionRepository.findByUsuarioDestino_IdUsuario(idUsuario, pageable)
                : notificacionRepository.findByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(
                        idUsuario, idPerfilContexto, pageable);

        return PaginaResponse.fromPage(resultado.map(NotificacionMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public NotificacionResumenResponse resumen(Long idUsuario, Long idPerfilContexto) {
        validarPerfilPropio(idUsuario, idPerfilContexto);

        long total;
        long noLeidas;

        if (idPerfilContexto == null) {
            total = notificacionRepository.countByUsuarioDestino_IdUsuario(idUsuario);
            noLeidas = notificacionRepository.countByUsuarioDestino_IdUsuarioAndLeidaFalse(idUsuario);
        } else {
            total = notificacionRepository.countByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(
                    idUsuario, idPerfilContexto);
            noLeidas = notificacionRepository
                    .countByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfilAndLeidaFalse(
                            idUsuario, idPerfilContexto);
        }

        return new NotificacionResumenResponse(total, noLeidas);
    }

    @Transactional
    public NotificacionResponse marcarLeida(Long idUsuario, Long idNotificacion) {
        Notificacion notificacion = notificacionRepository
                .findByIdNotificacionAndUsuarioDestino_IdUsuario(idNotificacion, idUsuario)
                .orElseThrow(NotificacionNoEncontradaException::new);

        if (!Boolean.TRUE.equals(notificacion.getLeida())) {
            notificacion.setLeida(true);
            notificacion.setFechaLeida(LocalDateTime.now());
            notificacion = notificacionRepository.save(notificacion);
        }

        return NotificacionMapper.toResponse(notificacion);
    }

    @Transactional
    public int marcarTodasLeidas(Long idUsuario, Long idPerfilContexto) {
        validarPerfilPropio(idUsuario, idPerfilContexto);

        if (idPerfilContexto == null) {
            return notificacionRepository.marcarTodasLeidas(idUsuario, LocalDateTime.now());
        }

        return notificacionRepository.marcarTodasLeidasDePerfil(
                idUsuario, idPerfilContexto, LocalDateTime.now());
    }

    @Transactional
    public List<Notificacion> registrarTodas(Collection<Notificacion> notificaciones) {
        if (notificaciones == null || notificaciones.isEmpty()) {
            return List.of();
        }
        return notificacionRepository.saveAll(notificaciones);
    }

    private void validarPerfilPropio(Long idUsuario, Long idPerfilContexto) {
        if (idPerfilContexto != null) {
            perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilContexto, idUsuario)
                    .orElseThrow(PerfilNoEncontradoException::new);
        }
    }

    private Pageable crearPageable(int page, int size) {
        int pagina = Math.max(0, page);
        int tamano = size <= 0
                ? TAMANO_PAGINA_POR_DEFECTO
                : Math.min(size, TAMANO_PAGINA_MAXIMO);

        Sort sort = Sort.by(
                Sort.Order.desc("fechaGenerada"),
                Sort.Order.desc("idNotificacion"));

        return PageRequest.of(pagina, tamano, sort);
    }
}
