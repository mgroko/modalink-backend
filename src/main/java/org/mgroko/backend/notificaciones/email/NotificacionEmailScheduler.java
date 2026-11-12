package org.mgroko.backend.notificaciones.email;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.NotificacionEntrega;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

/**
 * Procesa el outbox de entregas de email.
 *
 * <p>Se ejecuta cada N segundos y toma un lote de entregas pendientes o
 * fallidas listas para reintento. El envío SMTP se hace fuera de la
 * transacción que reclama la fila, para no retener locks durante la red.</p>
 */
@Component
@ConditionalOnProperty(
        name = "app.notificaciones.email.habilitado",
        havingValue = "true")
public class NotificacionEmailScheduler implements SchedulingConfigurer {

    private static final Logger LOG =
            LoggerFactory.getLogger(NotificacionEmailScheduler.class);

    private final NotificacionEntregaService entregaService;
    private final NotificacionEmailPolicy policy;
    private final NotificacionEmailBuilder emailBuilder;
    private final NotificacionEmailSender emailSender;
    private final TokenAccesoEmailService tokenService;
    private final NotificacionDestinoResolver destinoResolver;
    private final ConfiguracionSistemaService configuracionSistemaService;
    private final String frontendUrl;

    public NotificacionEmailScheduler(
            NotificacionEntregaService entregaService,
            NotificacionEmailPolicy policy,
            NotificacionEmailBuilder emailBuilder,
            NotificacionEmailSender emailSender,
            TokenAccesoEmailService tokenService,
            NotificacionDestinoResolver destinoResolver,
            ConfiguracionSistemaService configuracionSistemaService,
            @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.entregaService = entregaService;
        this.policy = policy;
        this.emailBuilder = emailBuilder;
        this.emailSender = emailSender;
        this.tokenService = tokenService;
        this.destinoResolver = destinoResolver;
        this.configuracionSistemaService = configuracionSistemaService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        int segundos = Math.max(5, configuracionSistemaService.obtenerIntervaloEmailSegundos());
        taskRegistrar.addTriggerTask(this::procesarLote, new PeriodicTrigger(Duration.ofSeconds(segundos)));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        int recuperadas = entregaService.recuperarEnviandoVencidas();
        if (recuperadas > 0) {
            LOG.info("Se recuperaron {} entregas de email que habían quedado en ENVIANDO.", recuperadas);
        }
    }

    public void procesarLote() {
        List<Long> ids = entregaService.reclamarLote();
        if (ids.isEmpty()) {
            return;
        }

        LOG.info("Procesando {} entrega(s) de email.", ids.size());
        for (Long id : ids) {
            try {
                procesar(id);
            } catch (RuntimeException ex) {
                LOG.warn("Error inesperado procesando la entrega de email {}.", id, ex);
            }
        }
    }

    private void procesar(Long idEntrega) {
        Optional<NotificacionEntrega> optional = entregaService.cargar(idEntrega);
        if (optional.isEmpty()) {
            return;
        }

        NotificacionEntrega entrega = optional.get();
        Notificacion notificacion = entrega.getNotificacion();
        if (notificacion == null) {
            entregaService.marcarCancelado(idEntrega, "NOTIFICACION_INEXISTENTE");
            return;
        }

        Usuario usuario = notificacion.getUsuarioDestino();

        try {
            String motivo = policy.motivoNoElegible(notificacion, usuario);
            if (motivo != null) {
                entregaService.marcarCancelado(idEntrega, motivo);
                return;
            }

            Long idProyecto = destinoResolver.resolverProyecto(notificacion);
            Long idPerfilContexto = notificacion.getPerfilContexto() != null
                    ? notificacion.getPerfilContexto().getIdPerfil()
                    : null;

            String token = tokenService.generarToken(
                    idEntrega,
                    usuario.getIdUsuario(),
                    idPerfilContexto);

            NotificacionEmailBuilder.EmailContent contenido = emailBuilder.construir(
                    notificacion,
                    idProyecto,
                    token,
                    frontendUrl,
                    usuario.getCorreo());

            emailSender.enviar(contenido);
            entregaService.marcarEnviado(idEntrega);
            LOG.info("Email enviado idEntrega={} tipo={} usuario={}",
                    idEntrega, notificacion.getTipo(), usuario.getIdUsuario());
        } catch (RuntimeException ex) {
            int intentos = entrega.getIntentos() == null ? 1 : entrega.getIntentos();
            entregaService.marcarFallido(idEntrega, intentos, ex);
            LOG.warn("No se pudo enviar el email idEntrega={} tipo={}.",
                    idEntrega, notificacion.getTipo(), ex);
        }
    }

}
