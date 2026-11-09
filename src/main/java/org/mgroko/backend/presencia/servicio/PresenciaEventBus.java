package org.mgroko.backend.presencia.servicio;

import java.util.Collection;

import org.mgroko.backend.presencia.dto.PresenciaEvento;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Bus de eventos de presencia. La implementación por defecto es en memoria y
 * alcanza para una única instancia. Se deja como interfaz para poder
 * reemplazarla por Redis Pub/Sub o PostgreSQL LISTEN/NOTIFY si el despliegue
 * pasa a ser multi-instancia.
 */
public interface PresenciaEventBus {

    void publicar(PresenciaEvento evento);

    void registrarSuscriptor(Collection<Long> idsPerfil, SseEmitter emitter);

    void eliminarSuscriptor(SseEmitter emitter);

    void ping();
}
