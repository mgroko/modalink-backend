package org.mgroko.backend.mensajeria.servicio;

import org.mgroko.backend.mensajeria.dto.ConversacionLeidaEvento;
import org.mgroko.backend.mensajeria.dto.EscribiendoEvento;
import org.mgroko.backend.mensajeria.dto.MensajeEvento;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Bus de eventos de mensajería. Implementación en memoria para una instancia.
 */
public interface MensajeEventBus {

    void registrarSuscriptor(Long idPerfil, SseEmitter emitter);

    void eliminarSuscriptor(SseEmitter emitter);

    void publicarMensaje(MensajeEvento evento);

    void publicarLeido(ConversacionLeidaEvento evento);

    void publicarEscribiendo(EscribiendoEvento evento);

    void ping();
}
