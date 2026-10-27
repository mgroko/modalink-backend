package org.mgroko.backend.calendario.servicio;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aritmética pura de intervalos para verificar disponibilidad.
 *
 * <p>No conoce agendas, actividades ni el reloj: recibe ventanas de jornada y
 * bloqueos ya calculados, y responde si existe un hueco continuo de al menos
 * la duración pedida. Así la regla de UC-26/UC-70 se puede probar sin mocks.</p>
 */
public final class DisponibilidadCalculator {

    /**
     * Intervalo semiabierto {@code [inicio, fin)}.
     */
    public record Intervalo(LocalDateTime inicio, LocalDateTime fin) {

        public Intervalo {
            if (inicio == null || fin == null) {
                throw new IllegalArgumentException("Un intervalo no puede tener extremos nulos.");
            }
            if (!fin.isAfter(inicio)) {
                throw new IllegalArgumentException("El fin del intervalo debe ser posterior al inicio.");
            }
        }

        public long minutos() {
            return Duration.between(inicio, fin).toMinutes();
        }

        public boolean solapa(Intervalo otro) {
            return inicio.isBefore(otro.fin) && fin.isAfter(otro.inicio);
        }
    }

    /**
     * Indica si alguna de las ventanas contiene un hueco libre continuo de al
     * menos {@code duracionMinutos} después de descontar los bloqueos.
     *
     * @param ventanas         ventanas de jornada ya recortadas al rango
     * @param bloqueos         bloqueos manuales y por actividades
     * @param duracionMinutos  duración mínima del hueco buscado
     * @return true si existe un hueco suficiente
     */
    public boolean hayHueco(
            Collection<Intervalo> ventanas,
            Collection<Intervalo> bloqueos,
            long duracionMinutos) {
        if (duracionMinutos <= 0) {
            return true;
        }
        return primerHueco(ventanas, bloqueos, duracionMinutos, 0)
                .isPresent();
    }

    /**
     * Devuelve los intervalos de jornada que quedan libres después de
     * descontar los bloqueos. Los bloqueos se fusionan por ventana para que
     * los solapamientos no generen huecos falsos.
     */
    public List<Intervalo> huecosLibres(
            Collection<Intervalo> ventanas,
            Collection<Intervalo> bloqueos) {

        if (ventanas == null || ventanas.isEmpty()) {
            return List.of();
        }

        List<Intervalo> ordenados = bloqueos == null
                ? List.of()
                : bloqueos.stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(Intervalo::inicio))
                        .toList();
        List<Intervalo> resultado = new ArrayList<>();

        for (Intervalo ventana : ventanas) {
            if (ventana == null) {
                continue;
            }
            LocalDateTime cursor = ventana.inicio();
            for (Intervalo bloqueo : ordenados) {
                if (!bloqueo.solapa(ventana)) {
                    continue;
                }
                LocalDateTime inicioBloqueo = max(bloqueo.inicio(), ventana.inicio());
                LocalDateTime finBloqueo = min(bloqueo.fin(), ventana.fin());
                if (inicioBloqueo.isAfter(cursor)) {
                    resultado.add(new Intervalo(cursor, inicioBloqueo));
                }
                if (finBloqueo.isAfter(cursor)) {
                    cursor = finBloqueo;
                }
                if (!cursor.isBefore(ventana.fin())) {
                    break;
                }
            }
            if (cursor.isBefore(ventana.fin())) {
                resultado.add(new Intervalo(cursor, ventana.fin()));
            }
        }
        return resultado;
    }

    /**
     * Busca el primer hueco que admite la duración solicitada y un margen
     * simétrico a ambos lados de la actividad nueva.
     */
    public java.util.Optional<Intervalo> primerHueco(
            Collection<Intervalo> ventanas,
            Collection<Intervalo> bloqueos,
            long duracionMinutos,
            long margenMinutos) {

        if (duracionMinutos <= 0) {
            return java.util.Optional.empty();
        }
        if (margenMinutos < 0) {
            throw new IllegalArgumentException("El margen no puede ser negativo.");
        }

        long duracionReservada = duracionMinutos + (2 * margenMinutos);
        return primerHuecoEn(huecosLibres(ventanas, bloqueos), duracionReservada, margenMinutos);
    }

    /**
     * Variante para una colección que ya representa huecos libres, útil al
     * intersectar la disponibilidad de varios miembros.
     */
    public java.util.Optional<Intervalo> primerHuecoEnHuecos(
            Collection<Intervalo> huecos,
            long duracionMinutos,
            long margenMinutos) {
        if (duracionMinutos <= 0) {
            return java.util.Optional.empty();
        }
        if (margenMinutos < 0) {
            throw new IllegalArgumentException("El margen no puede ser negativo.");
        }
        return primerHuecoEn(
                huecos,
                duracionMinutos + (2 * margenMinutos),
                margenMinutos);
    }

    private java.util.Optional<Intervalo> primerHuecoEn(
            Collection<Intervalo> huecos,
            long duracionReservada,
            long margenMinutos) {
        if (huecos == null) {
            return java.util.Optional.empty();
        }
        for (Intervalo hueco : huecos) {
            if (hueco.minutos() < duracionReservada) {
                continue;
            }
            LocalDateTime inicio = hueco.inicio().plusMinutes(margenMinutos);
            return java.util.Optional.of(new Intervalo(
                    inicio, inicio.plusMinutes(
                            duracionReservada - (2 * margenMinutos))));
        }
        return java.util.Optional.empty();
    }

    /**
     * Intersecta huecos libres de varios miembros. Un intervalo resultante
     * solo existe cuando todos los miembros pueden trabajar simultáneamente.
     */
    public List<Intervalo> intersecarHuecos(
            Collection<? extends Collection<Intervalo>> huecosPorMiembro) {

        if (huecosPorMiembro == null || huecosPorMiembro.isEmpty()) {
            return List.of();
        }
        var iterador = huecosPorMiembro.iterator();
        Collection<Intervalo> primeros = iterador.next();
        if (primeros == null) {
            return List.of();
        }
        List<Intervalo> interseccion = new ArrayList<>(primeros);
        while (iterador.hasNext()) {
            Collection<Intervalo> huecos = iterador.next();
            if (huecos == null) {
                return List.of();
            }
            List<Intervalo> siguiente = new ArrayList<>();
            for (Intervalo primero : interseccion) {
                for (Intervalo segundo : huecos) {
                    LocalDateTime inicio = max(primero.inicio(), segundo.inicio());
                    LocalDateTime fin = min(primero.fin(), segundo.fin());
                    if (fin.isAfter(inicio)) {
                        siguiente.add(new Intervalo(inicio, fin));
                    }
                }
            }
            interseccion = siguiente;
            if (interseccion.isEmpty()) {
                return List.of();
            }
        }
        return interseccion.stream()
                .sorted(Comparator.comparing(Intervalo::inicio))
                .toList();
    }

    private static LocalDateTime max(LocalDateTime primero, LocalDateTime segundo) {
        return primero.isAfter(segundo) ? primero : segundo;
    }

    private static LocalDateTime min(LocalDateTime primero, LocalDateTime segundo) {
        return primero.isBefore(segundo) ? primero : segundo;
    }
}
