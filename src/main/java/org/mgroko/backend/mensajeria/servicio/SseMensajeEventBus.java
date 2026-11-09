package org.mgroko.backend.mensajeria.servicio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mgroko.backend.mensajeria.dto.ConversacionLeidaEvento;
import org.mgroko.backend.mensajeria.dto.EscribiendoEvento;
import org.mgroko.backend.mensajeria.dto.MensajeEvento;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Implementación en memoria del bus de mensajería.
 */
@Component
public class SseMensajeEventBus implements MensajeEventBus {

    private final Map<Long, Set<SseEmitter>> suscriptores = new ConcurrentHashMap<>();
    private final Map<SseEmitter, Long> perfilPorEmisor = new ConcurrentHashMap<>();

    @Override
    public void registrarSuscriptor(Long idPerfil, SseEmitter emitter) {
        perfilPorEmisor.put(emitter, idPerfil);
        suscriptores.computeIfAbsent(idPerfil, id -> ConcurrentHashMap.newKeySet())
                .add(emitter);

        emitter.onCompletion(() -> eliminarSuscriptor(emitter));
        emitter.onTimeout(() -> eliminarSuscriptor(emitter));
        emitter.onError(ex -> eliminarSuscriptor(emitter));
    }

    @Override
    public void eliminarSuscriptor(SseEmitter emitter) {
        Long idPerfil = perfilPorEmisor.remove(emitter);
        if (idPerfil == null) {
            return;
        }

        Set<SseEmitter> emisores = suscriptores.get(idPerfil);
        if (emisores == null) {
            return;
        }

        emisores.remove(emitter);
        if (emisores.isEmpty()) {
            suscriptores.remove(idPerfil, emisores);
        }
    }

    @Override
    public void publicarMensaje(MensajeEvento evento) {
        enviar(evento.idPerfilEmisor(), "mensaje", evento);
        if (!evento.idPerfilEmisor().equals(evento.idPerfilDestinatario())) {
            enviar(evento.idPerfilDestinatario(), "mensaje", evento);
        }
    }

    @Override
    public void publicarLeido(ConversacionLeidaEvento evento) {
        enviar(evento.idPerfilLector(), "conversacion_leida", evento);
        if (!evento.idPerfilLector().equals(evento.idPerfilOtro())) {
            enviar(evento.idPerfilOtro(), "conversacion_leida", evento);
        }
    }

    /**
     * El aviso de escritura sólo se envía a la contraparte: el emisor no
     * necesita mostrarse su propio indicador.
     */
    @Override
    public void publicarEscribiendo(EscribiendoEvento evento) {
        enviar(evento.idPerfilDestinatario(), "escribiendo", evento);
    }

    @Override
    public void ping() {
        for (SseEmitter emitter : new ArrayList<>(perfilPorEmisor.keySet())) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException ex) {
                eliminarSuscriptor(emitter);
            }
        }
    }

    private void enviar(Long idPerfil, String nombreEvento, Object data) {
        Set<SseEmitter> emisores = suscriptores.get(idPerfil);
        if (emisores == null || emisores.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : new ArrayList<>(emisores)) {
            try {
                emitter.send(SseEmitter.event()
                        .name(nombreEvento)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ex) {
                eliminarSuscriptor(emitter);
            }
        }
    }
}
