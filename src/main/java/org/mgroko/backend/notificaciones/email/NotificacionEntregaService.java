package org.mgroko.backend.notificaciones.email;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.NotificacionEntrega;
import org.mgroko.backend.repositorio.NotificacionEntregaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordina el ciclo de vida de las entregas de email.
 */
@Service
public class NotificacionEntregaService {

    private static final int MAX_ERROR = 500;
    private static final long MAX_BACKOFF_MINUTOS = 24 * 60;

    private final NotificacionEntregaRepository entregaRepository;
    private final ConfiguracionSistemaService configuracionSistemaService;
    private final Clock reloj;

    public NotificacionEntregaService(
            NotificacionEntregaRepository entregaRepository,
            ConfiguracionSistemaService configuracionSistemaService,
            Clock reloj) {
        this.entregaRepository = entregaRepository;
        this.configuracionSistemaService = configuracionSistemaService;
        this.reloj = reloj;
    }

    @Transactional
    public List<Long> reclamarLote() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int lote = configuracionSistemaService.obtenerLoteEmail();
        return entregaRepository.reclamarLote(ahora, lote);
    }

    @Transactional
    public int recuperarEnviandoVencidas() {
        return entregaRepository.recuperarEnviandoVencidas(
                LocalDateTime.now(reloj).minusMinutes(10));
    }

    @Transactional(readOnly = true)
    public Optional<NotificacionEntrega> cargar(Long idEntrega) {
        return entregaRepository.findByIdConNotificacion(idEntrega);
    }

    @Transactional
    public void marcarEnviado(Long idEntrega) {
        entregaRepository.actualizarResultado(
                idEntrega,
                NotificacionEntrega.ESTADO_ENVIADO,
                null,
                null,
                LocalDateTime.now(reloj));
    }

    @Transactional
    public void marcarFallido(Long idEntrega, int intentos, Throwable error) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int maximo = configuracionSistemaService.obtenerReintentosMaximosEmail();
        String detalle = truncar(mensaje(error));

        if (intentos >= maximo) {
            entregaRepository.actualizarResultado(
                    idEntrega,
                    NotificacionEntrega.ESTADO_CANCELADO,
                    truncar("REINTENTOS_AGOTADOS: " + detalle),
                    null,
                    null);
            return;
        }

        int backoff = configuracionSistemaService.obtenerBackoffMinutosEmail();
        long minutos = Math.min(
                MAX_BACKOFF_MINUTOS,
                (long) backoff * (1L << Math.min(Math.max(0, intentos - 1), 10)));

        entregaRepository.actualizarResultado(
                idEntrega,
                NotificacionEntrega.ESTADO_FALLIDO,
                detalle,
                ahora.plusMinutes(minutos),
                null);
    }

    @Transactional
    public void marcarCancelado(Long idEntrega, String motivo) {
        entregaRepository.actualizarResultado(
                idEntrega,
                NotificacionEntrega.ESTADO_CANCELADO,
                truncar(motivo),
                null,
                null);
    }

    private String mensaje(Throwable error) {
        if (error == null) {
            return "ERROR_DESCONOCIDO";
        }
        String mensaje = error.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            mensaje = error.getClass().getSimpleName();
        }
        Throwable causa = error.getCause();
        if (causa != null && causa.getMessage() != null && !causa.getMessage().isBlank()) {
            mensaje = mensaje + " | " + causa.getMessage();
        }
        return mensaje;
    }

    private String truncar(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= MAX_ERROR ? valor : valor.substring(0, MAX_ERROR);
    }
}
