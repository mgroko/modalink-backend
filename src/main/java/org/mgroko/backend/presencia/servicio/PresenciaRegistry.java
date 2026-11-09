package org.mgroko.backend.presencia.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

/**
 * Registro en memoria del último heartbeat por perfil.
 *
 * <p>La fuente de verdad de lectura sigue siendo {@code perfil.ultima_actividad};
 * este registro solo detecta transiciones online/offline para emitir eventos en
 * tiempo real y expirar conexiones SSE.</p>
 */
@Component
public class PresenciaRegistry {

    private final ConcurrentHashMap<Long, LocalDateTime> ultimosHeartbeats =
            new ConcurrentHashMap<>();

    /**
     * Registra un heartbeat y devuelve true si el perfil pasó de offline a
     * online. Se actualiza en cada heartbeat, aunque la escritura en base se
     * haya salteado por el throttle.
     */
    public boolean registrar(Long idPerfil, LocalDateTime ahora, LocalDateTime limite) {
        AtomicBoolean transicion = new AtomicBoolean(false);

        ultimosHeartbeats.compute(idPerfil, (id, anterior) -> {
            boolean estabaEnLinea = anterior != null
                    && !anterior.isBefore(limite);
            if (!estabaEnLinea) {
                transicion.set(true);
            }
            return ahora;
        });

        return transicion.get();
    }

    /**
     * Elimina y devuelve los perfiles cuyo último heartbeat venció. La
     * eliminación es atómica para no pisar un heartbeat concurrente.
     */
    public List<Long> expirar(LocalDateTime limite) {
        List<Long> expirados = new ArrayList<>();

        ultimosHeartbeats.forEach((idPerfil, ultimo) -> {
            if (ultimo.isBefore(limite)) {
                AtomicBoolean eliminado = new AtomicBoolean(false);
                ultimosHeartbeats.computeIfPresent(idPerfil, (id, actual) -> {
                    if (actual.isBefore(limite)) {
                        eliminado.set(true);
                        return null;
                    }
                    return actual;
                });
                if (eliminado.get()) {
                    expirados.add(idPerfil);
                }
            }
        });

        return expirados;
    }

    public boolean estaRegistrado(Long idPerfil) {
        return ultimosHeartbeats.containsKey(idPerfil);
    }

    public void eliminar(Long idPerfil) {
        ultimosHeartbeats.remove(idPerfil);
    }

    public int cantidad() {
        return ultimosHeartbeats.size();
    }
}
