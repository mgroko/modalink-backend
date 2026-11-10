package org.mgroko.backend.presencia.servicio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mgroko.backend.presencia.dto.PresenciaEvento;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Implementación en memoria de {@link PresenciaEventBus}.
 */
@Component
public class SsePresenciaEventBus implements PresenciaEventBus {

    private final Map<Long, Set<SseEmitter>> suscriptores = new ConcurrentHashMap<>();
    private final Map<SseEmitter, Set<Long>> idsPorEmisor = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emisoresPorCliente = new ConcurrentHashMap<>();

    @Override
    public void publicar(PresenciaEvento evento) {
        Set<SseEmitter> emisores = suscriptores.get(evento.idPerfil());
        if (emisores == null || emisores.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : new ArrayList<>(emisores)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("presencia")
                        .data(evento, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ex) {
                cerrarSuscriptor(emitter, ex);
            }
        }
    }

    @Override
    public void registrarSuscriptor(Collection<Long> idsPerfil, SseEmitter emitter) {
        registrarSuscriptor(idsPerfil, emitter, null);
    }

    @Override
    public void registrarSuscriptor(
            Collection<Long> idsPerfil,
            SseEmitter emitter,
            String clientId) {

        // Si la misma pestaña reconecta, el stream anterior se cierra antes de
        // registrar el nuevo. Así no se acumulan emisores ni conexiones JDBC
        // cuando el navegador reemplaza un EventSource.
        if (clientId != null && !clientId.isBlank()) {
            SseEmitter anterior = emisoresPorCliente.put(clientId, emitter);
            if (anterior != null && anterior != emitter) {
                cerrarSuscriptor(anterior, null);
            }
        }

        Set<Long> ids = Set.copyOf(idsPerfil);
        idsPorEmisor.put(emitter, ids);

        for (Long idPerfil : ids) {
            suscriptores.computeIfAbsent(idPerfil, id -> ConcurrentHashMap.newKeySet())
                    .add(emitter);
        }

        emitter.onCompletion(() -> eliminarSuscriptor(emitter));
        emitter.onTimeout(() -> eliminarSuscriptor(emitter));
        emitter.onError(ex -> eliminarSuscriptor(emitter));
    }

    @Override
    public void eliminarSuscriptor(SseEmitter emitter) {
        Set<Long> ids = idsPorEmisor.remove(emitter);
        emisoresPorCliente.entrySet().removeIf(entry -> entry.getValue() == emitter);

        if (ids == null) {
            return;
        }

        for (Long idPerfil : ids) {
            Set<SseEmitter> emisores = suscriptores.get(idPerfil);
            if (emisores == null) {
                continue;
            }
            emisores.remove(emitter);
            if (emisores.isEmpty()) {
                suscriptores.remove(idPerfil, emisores);
            }
        }
    }

    /**
     * Quita el emisor del bus y completa el request async. Sin este cierre, el
     * navegador puede haber cerrado el EventSource pero el servidor mantiene
     * vivo el request SSE (y su conexión JDBC) hasta el timeout del emitter.
     */
    private void cerrarSuscriptor(SseEmitter emitter, Throwable error) {
        eliminarSuscriptor(emitter);
        try {
            if (error != null) {
                emitter.completeWithError(error);
            } else {
                emitter.complete();
            }
        } catch (Exception ignored) {
            // El contenedor o el cliente ya habían cerrado el request.
        }
    }

    @Override
    public void ping() {
        for (SseEmitter emitter : new ArrayList<>(idsPorEmisor.keySet())) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException ex) {
                cerrarSuscriptor(emitter, ex);
            }
        }
    }
}
