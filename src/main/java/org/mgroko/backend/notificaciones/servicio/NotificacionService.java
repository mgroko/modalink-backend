package org.mgroko.backend.notificaciones.servicio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    public static final String TIPO_PROYECTO_CONFIRMADO = "PROYECTO_CONFIRMADO";
    public static final String TIPO_PROYECTO_FINALIZADO = "PROYECTO_FINALIZADO";
    public static final String TIPO_PROYECTO_PUBLICADO = "PROYECTO_PUBLICADO";
    public static final String TIPO_ACTIVIDAD_ASIGNADA = "ACTIVIDAD_ASIGNADA";
    public static final String TIPO_CALIFICACION_SOLICITADA = "CALIFICACION_SOLICITADA";
    public static final String TIPO_ACTIVIDAD_MODIFICADA = "ACTIVIDAD_MODIFICADA";
    public static final String TIPO_ACTIVIDAD_ELIMINADA = "ACTIVIDAD_ELIMINADA";
    public static final String TIPO_ACTIVIDAD_DESASIGNADA = "ACTIVIDAD_DESASIGNADA";
    public static final String TIPO_POSTULACION_RECIBIDA = "POSTULACION_RECIBIDA";
    public static final String TIPO_POSTULACION_ACEPTADA = "POSTULACION_ACEPTADA";
    public static final String TIPO_POSTULACION_RECHAZADA = "POSTULACION_RECHAZADA";
    public static final String TIPO_AJUSTE_HORARIO = "AJUSTE_HORARIO";
    public static final String TIPO_SOBRECOMPROMISO = "SOBRECOMPROMISO";
    public static final String TIPO_PROYECTO_FECHA_ENTREGA_MODIFICADA =
            "PROYECTO_FECHA_ENTREGA_MODIFICADA";
    public static final String TIPO_INVITACION_RECIBIDA = "INVITACION_RECIBIDA";
    public static final String TIPO_INVITACION_ACEPTADA = "INVITACION_ACEPTADA";
    public static final String TIPO_INVITACION_RECHAZADA = "INVITACION_RECHAZADA";
    public static final String TIPO_HITO_CUMPLIDO = "HITO_CUMPLIDO";
    public static final String TIPO_PERFIL_REPORTADO = "PERFIL_REPORTADO";
    public static final String TIPO_PUBLICACION_REPORTADA = "PUBLICACION_REPORTADA";
    public static final String TIPO_PUBLICACION_OCULTADA = "PUBLICACION_OCULTADA";
    public static final String TIPO_PUBLICACION_REACTIVADA = "PUBLICACION_REACTIVADA";
    public static final String TIPO_HABILIDAD_MODIFICADA = "HABILIDAD_MODIFICADA";
    public static final String TIPO_HABILIDAD_ELIMINADA = "HABILIDAD_ELIMINADA";
    public static final String TIPO_INTEGRANTE_ELIMINADO = "INTEGRANTE_ELIMINADO";
    public static final String TIPO_INTEGRANTE_BAJA_VOLUNTARIA =
            "INTEGRANTE_BAJA_VOLUNTARIA";
    public static final String TIPO_CONTRATO_PENDIENTE_FIRMA =
            "CONTRATO_PENDIENTE_FIRMA";
    public static final String TIPO_CONTRATO_MODIFICADO =
            "CONTRATO_MODIFICADO";
    public static final String TIPO_CONTRATO_FIRMADO =
            "CONTRATO_FIRMADO";
    public static final String TIPO_CONTRATO_ACTIVO =
            "CONTRATO_ACTIVO";
    public static final String TIPO_CONTRATO_RECHAZADO =
            "CONTRATO_RECHAZADO";
    public static final String TIPO_CONTRATO_CANCELADO =
            "CONTRATO_CANCELADO";
    public static final String TIPO_CONTRATO_ELIMINADO =
            "CONTRATO_ELIMINADO";
    public static final String TIPO_REEMPLAZO_CANDIDATOS_DISPONIBLES =
            "REEMPLAZO_CANDIDATOS_DISPONIBLES";
    public static final String TIPO_REEMPLAZO_SIN_CANDIDATOS =
            "REEMPLAZO_SIN_CANDIDATOS";
    public static final String TIPO_REEMPLAZO_INVITACION_RECIBIDA =
            "REEMPLAZO_INVITACION_RECIBIDA";
    public static final String TIPO_REEMPLAZO_CANDIDATO_RECHAZO =
            "REEMPLAZO_CANDIDATO_RECHAZO";
    public static final String TIPO_REEMPLAZO_ACEPTADO_PENDIENTE_APROBACION =
            "REEMPLAZO_ACEPTADO_PENDIENTE_APROBACION";
    public static final String TIPO_REEMPLAZO_INCORPORACION_APROBADA =
            "REEMPLAZO_INCORPORACION_APROBADA";
    public static final String TIPO_REEMPLAZO_INCORPORACION_RECHAZADA =
            "REEMPLAZO_INCORPORACION_RECHAZADA";
    public static final String TIPO_REEMPLAZO_CANCELADO =
            "REEMPLAZO_CANCELADO";
    public static final String TIPO_SOLICITUD_CONEXION_RECIBIDA =
            "SOLICITUD_CONEXION_RECIBIDA";
    public static final String TIPO_SOLICITUD_CONEXION_ACEPTADA =
            "SOLICITUD_CONEXION_ACEPTADA";
    public static final String TIPO_SOLICITUD_CONEXION_RECHAZADA =
            "SOLICITUD_CONEXION_RECHAZADA";
    public static final String TIPO_CONEXION_ELIMINADA =
            "CONEXION_ELIMINADA";
    public static final String TIPO_MENSAJE_PRIVADO_RECIBIDO =
            "MENSAJE_PRIVADO_RECIBIDO";
    public static final String TIPO_MENSAJE_GRUPAL_RECIBIDO =
            "MENSAJE_GRUPAL_RECIBIDO";
    public static final String TIPO_SOLICITUD_COLABORACION_RECIBIDA =
            "SOLICITUD_COLABORACION_RECIBIDA";
    public static final String TIPO_SOLICITUD_COLABORACION_ACEPTADA =
            "SOLICITUD_COLABORACION_ACEPTADA";
    public static final String TIPO_SOLICITUD_COLABORACION_RECHAZADA =
            "SOLICITUD_COLABORACION_RECHAZADA";
    public static final String TIPO_PUBLICACION_MEGUSTA =
            "PUBLICACION_MEGUSTA";
    public static final String TIPO_PUBLICACION_COMENTARIO =
            "PUBLICACION_COMENTARIO";
    public static final String ENTIDAD_SOLICITUD_CONEXION =
            "SOLICITUD_CONEXION";
    public static final String ENTIDAD_CONEXION = "CONEXION";
    public static final String ENTIDAD_CONVERSACION = "CONVERSACION";
    public static final String ENTIDAD_PROYECTO = "PROYECTO";
    public static final String ENTIDAD_PUBLICACION = "PUBLICACION";
    public static final String ORIGEN_SERVICIO = "SERVICIO";

    private static final int TAMANO_PAGINA_POR_DEFECTO = 20;
    private static final int TAMANO_PAGINA_MAXIMO = 100;

    private final NotificacionRepository notificacionRepository;
    private final PerfilRepository perfilRepository;
    private final NotificacionDestinoResolver notificacionDestinoResolver;

    public NotificacionService(
            NotificacionRepository notificacionRepository,
            PerfilRepository perfilRepository,
            NotificacionDestinoResolver notificacionDestinoResolver) {
        this.notificacionRepository = notificacionRepository;
        this.perfilRepository = perfilRepository;
        this.notificacionDestinoResolver = notificacionDestinoResolver;
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

        return crear(
                usuarioDestino,
                perfilContexto,
                tipo,
                mensaje,
                entidadTipo,
                entidadId,
                null);
    }

    /**
     * Crea una notificación capturando el proyecto asociado. Para
     * notificaciones históricas o tipos globales puede ser {@code null}; en ese
     * caso el proyecto se intenta derivar en lectura.
     */
    public Notificacion crear(
            Usuario usuarioDestino,
            Perfil perfilContexto,
            String tipo,
            String mensaje,
            String entidadTipo,
            Long entidadId,
            Long idProyecto) {

        return Notificacion.builder()
                .usuarioDestino(usuarioDestino)
                .perfilContexto(perfilContexto)
                .tipo(tipo)
                .mensaje(mensaje)
                .entidadTipo(entidadTipo)
                .entidadId(entidadId)
                .idProyecto(idProyecto)
                .leida(false)
                .fechaLeida(null)
                .origen(ORIGEN_SERVICIO)
                .build();
    }

    /**
     * Elimina las notificaciones asociadas a una entidad concreta.
     *
     * <p>Se usa cuando la entidad referenciada deja de existir, por ejemplo una
     * invitación eliminada, para que el destinatario no conserve una
     * notificación que apunta a algo inexistente.</p>
     *
     * @return cantidad de notificaciones eliminadas
     */
    @Transactional
    public int eliminarPorEntidad(
            String tipo,
            String entidadTipo,
            Long entidadId) {

        if (tipo == null || entidadTipo == null || entidadId == null) {
            return 0;
        }

        return notificacionRepository.deleteByTipoAndEntidadTipoAndEntidadId(
                tipo, entidadTipo, entidadId);
    }

    /**
     * Elimina todas las notificaciones de una entidad concreta,
     * independientemente de su tipo. UC-20 lo usa al dar de baja físicamente
     * una publicación.
     *
     * @return cantidad de notificaciones eliminadas
     */
    @Transactional
    public int eliminarPorEntidad(String entidadTipo, Long entidadId) {
        if (entidadTipo == null || entidadId == null) {
            return 0;
        }

        return notificacionRepository.deleteByEntidadTipoAndEntidadId(
                entidadTipo, entidadId);
    }

    /**
     * Elimina las notificaciones de una entidad concreta para un usuario y
     * perfil de destino.
     *
     * <p>UC-64 lo usa para retirar únicamente la notificación del destinatario
     * de una solicitud de colaboración eliminada, sin afectar a otros
     * destinatarios de la misma publicación.</p>
     *
     * @return cantidad de notificaciones eliminadas
     */
    @Transactional
    public int eliminarPorEntidadYPerfil(
            String tipo,
            String entidadTipo,
            Long entidadId,
            Long idUsuarioDestino,
            Long idPerfilContexto) {

        if (tipo == null || entidadTipo == null || entidadId == null
                || idUsuarioDestino == null || idPerfilContexto == null) {
            return 0;
        }

        return notificacionRepository
                .deleteByTipoAndEntidadTipoAndEntidadIdYPerfil(
                        tipo,
                        entidadTipo,
                        entidadId,
                        idUsuarioDestino,
                        idPerfilContexto);
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

        Map<Long, Long> proyectoPorNotificacion =
                notificacionDestinoResolver.resolverProyectos(resultado.getContent());

        return PaginaResponse.fromPage(resultado.map(notificacion ->
                NotificacionMapper.toResponse(
                        notificacion,
                        proyectoPorNotificacion.get(notificacion.getIdNotificacion()))));
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

        return NotificacionMapper.toResponse(
                notificacion,
                notificacionDestinoResolver.resolverProyecto(notificacion));
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
