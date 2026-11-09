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
                eliminarSuscriptor(emitter);
            }
        }
    }

    @Override
    public void registrarSuscriptor(Collection<Long> idsPerfil, SseEmitter emitter) {
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

    @Override
    public void ping() {
        for (SseEmitter emitter : new ArrayList<>(idsPorEmisor.keySet())) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException ex) {
                eliminarSuscriptor(emitter);
            }
        }
    }
}
