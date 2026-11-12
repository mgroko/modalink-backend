package org.mgroko.backend.reemplazos.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.reemplazos.servicio.modelo.CandidatoPerfil;
import org.mgroko.backend.reemplazos.servicio.modelo.CaracteristicaEvaluada;
import org.mgroko.backend.reemplazos.servicio.modelo.CaracteristicaRequerida;
import org.mgroko.backend.reemplazos.servicio.modelo.HabilidadEvaluada;
import org.mgroko.backend.reemplazos.servicio.modelo.RequisitoReemplazo;
import org.mgroko.backend.reemplazos.servicio.modelo.ResultadoConcordancia;
import org.mgroko.backend.reemplazos.servicio.modelo.ValorPerfil;

/**
 * Calcula la concordancia entre un perfil y un requisito.
 *
 * <p>Profesión y disponibilidad se validan antes y son filtros duros. Esta
 * clase solo puntúa habilidades y características: un perfil que no cumple
 * todo sigue siendo candidato, pero queda marcado como alternativo y recibe
 * menos puntaje.</p>
 */
public final class EvaluadorConcordancia {

    private static final double PESO_HABILIDADES = 0.40;
    private static final double PESO_CARACTERISTICAS = 0.60;

    private EvaluadorConcordancia() {
    }

    public static ResultadoConcordancia evaluar(
            RequisitoReemplazo requisito,
            CandidatoPerfil candidato) {

        List<String> faltantes = new ArrayList<>();
        List<HabilidadEvaluada> habilidadesEvaluadas = new ArrayList<>();

        long habilidadesCumplidas = 0;
        if (!requisito.idsHabilidades().isEmpty()) {
            List<Long> idsOrdenados = requisito.idsHabilidades().stream()
                    .sorted(java.util.Comparator.comparing(
                            id -> requisito.nombresHabilidades()
                                    .getOrDefault(id, ""),
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();

            for (Long idHabilidad : idsOrdenados) {
                String nombre = requisito.nombresHabilidades()
                        .getOrDefault(idHabilidad, "Habilidad " + idHabilidad);
                boolean cumple = candidato.idsHabilidades().contains(idHabilidad);
                habilidadesEvaluadas.add(new HabilidadEvaluada(
                        idHabilidad,
                        nombre,
                        true,
                        cumple));
                if (cumple) {
                    habilidadesCumplidas++;
                } else {
                    faltantes.add("Habilidad faltante: " + nombre);
                }
            }
        }

        double puntajeHabilidades = requisito.idsHabilidades().isEmpty()
                ? 1.0
                : habilidadesCumplidas
                        / (double) requisito.idsHabilidades().size();

        double puntajeCaracteristicas = 1.0;
        List<CaracteristicaEvaluada> caracteristicasEvaluadas = new ArrayList<>();
        long caracteristicasCumplidas = 0;
        if (!requisito.caracteristicas().isEmpty()) {
            double suma = 0.0;
            for (CaracteristicaRequerida caracteristica : requisito.caracteristicas()) {
                EvaluacionCaracteristica evaluacion =
                        evaluarCaracteristica(caracteristica, candidato);
                suma += evaluacion.puntaje();
                caracteristicasEvaluadas.add(new CaracteristicaEvaluada(
                        caracteristica.idCaracteristica(),
                        caracteristica.codigo(),
                        caracteristica.nombre(),
                        true,
                        evaluacion.cumple(),
                        caracteristica.valorPedido()));
                if (evaluacion.cumple()) {
                    caracteristicasCumplidas++;
                } else if (evaluacion.detalle() != null) {
                    faltantes.add(evaluacion.detalle());
                }
            }
            puntajeCaracteristicas = suma / requisito.caracteristicas().size();
        }

        double puntajeFinal;
        if (requisito.idsHabilidades().isEmpty()) {
            puntajeFinal = puntajeCaracteristicas;
        } else if (requisito.caracteristicas().isEmpty()) {
            puntajeFinal = puntajeHabilidades;
        } else {
            puntajeFinal = PESO_HABILIDADES * puntajeHabilidades
                    + PESO_CARACTERISTICAS * puntajeCaracteristicas;
        }

        BigDecimal puntaje = BigDecimal.valueOf(puntajeFinal * 100.0)
                .setScale(2, RoundingMode.HALF_UP);

        int requisitosTotales = habilidadesEvaluadas.size()
                + caracteristicasEvaluadas.size();
        int requisitosCumplidos = (int) (habilidadesCumplidas
                + caracteristicasCumplidas);
        boolean cumpleTodo = requisitosCumplidos >= requisitosTotales;

        return new ResultadoConcordancia(
                puntaje,
                !cumpleTodo,
                faltantes,
                requisitosCumplidos,
                requisitosTotales,
                cumpleTodo,
                habilidadesEvaluadas,
                caracteristicasEvaluadas);
    }

    private static EvaluacionCaracteristica evaluarCaracteristica(
            CaracteristicaRequerida requerida,
            CandidatoPerfil candidato) {

        ValorPerfil valor = candidato.caracteristicas()
                .get(requerida.idCaracteristica());

        if (CaracteristicaTecnica.TIPO_NUMERICO.equals(requerida.tipoDato())) {
            return evaluarNumerica(requerida, valor);
        }
        if (CaracteristicaTecnica.TIPO_ENUMERADO.equals(requerida.tipoDato())) {
            return evaluarEnumerada(requerida, valor);
        }
        return evaluarPresencia(requerida, valor);
    }

    private static EvaluacionCaracteristica evaluarNumerica(
            CaracteristicaRequerida requerida,
            ValorPerfil valor) {

        if (valor == null || valor.valor() == null || valor.valor().isBlank()) {
            return new EvaluacionCaracteristica(
                    0.0,
                    false,
                    "Sin valor para " + nombre(requerida));
        }

        BigDecimal numero;
        try {
            numero = new BigDecimal(valor.valor().trim());
        } catch (NumberFormatException ex) {
            return new EvaluacionCaracteristica(
                    0.0,
                    false,
                    "Valor inválido para " + nombre(requerida));
        }

        BigDecimal min = requerida.valorMin();
        BigDecimal max = requerida.valorMax();
        boolean sinRango = min == null && max == null;

        if (sinRango) {
            return new EvaluacionCaracteristica(1.0, true, null);
        }

        boolean dentro = (min == null || numero.compareTo(min) >= 0)
                && (max == null || numero.compareTo(max) <= 0);
        if (dentro) {
            // Dentro de un rango, el valor más alto queda primero. Se usa un
            // margen pequeño para no empatar con los valores de afuera.
            if (min != null && max != null && max.compareTo(min) > 0) {
                double proporcion = numero.subtract(min)
                        .divide(max.subtract(min), 6, RoundingMode.HALF_UP)
                        .doubleValue();
                return new EvaluacionCaracteristica(
                        0.999 + 0.001 * proporcion,
                        true,
                        null);
            }
            return new EvaluacionCaracteristica(1.0, true, null);
        }

        BigDecimal distancia;
        if (min != null && numero.compareTo(min) < 0) {
            distancia = min.subtract(numero);
        } else {
            distancia = numero.subtract(max);
        }

        BigDecimal ancho = min != null && max != null && max.compareTo(min) > 0
                ? max.subtract(min)
                : BigDecimal.ONE;
        double normalizada = distancia
                .divide(ancho, 6, RoundingMode.HALF_UP)
                .doubleValue();
        double puntaje = 1.0 / (1.0 + normalizada);

        // Para un valor exacto, en empate gana el que está por debajo:
        // 190, 189, 191, 188, 192...
        if (min != null && max != null && min.compareTo(max) == 0
                && numero.compareTo(min) < 0) {
            puntaje += 0.0001;
        }

        return new EvaluacionCaracteristica(
                puntaje,
                false,
                nombre(requerida) + ": " + numero.stripTrailingZeros().toPlainString()
                        + " fuera del rango " + rango(min, max));
    }

    private static EvaluacionCaracteristica evaluarEnumerada(
            CaracteristicaRequerida requerida,
            ValorPerfil valor) {

        if (valor == null || valor.idValor() == null) {
            return new EvaluacionCaracteristica(
                    0.0,
                    false,
                    "Sin valor para " + nombre(requerida));
        }

        if (requerida.idsValores().contains(valor.idValor())) {
            return new EvaluacionCaracteristica(1.0, true, null);
        }

        return new EvaluacionCaracteristica(
                0.0,
                false,
                nombre(requerida) + " no coincide con los valores pedidos");
    }

    private static EvaluacionCaracteristica evaluarPresencia(
            CaracteristicaRequerida requerida,
            ValorPerfil valor) {

        if (valor == null
                || (valor.valor() == null || valor.valor().isBlank())
                        && valor.idValor() == null) {
            return new EvaluacionCaracteristica(
                    0.0,
                    false,
                    "Sin valor para " + nombre(requerida));
        }

        return new EvaluacionCaracteristica(1.0, true, null);
    }

    private static String nombre(CaracteristicaRequerida requerida) {
        if (requerida.nombre() != null && !requerida.nombre().isBlank()) {
            return requerida.nombre();
        }
        return requerida.codigo() != null
                ? requerida.codigo()
                : "Característica " + requerida.idCaracteristica();
    }

    private static String rango(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            return min.stripTrailingZeros().toPlainString()
                    + " - "
                    + max.stripTrailingZeros().toPlainString();
        }
        if (min != null) {
            return "mínimo " + min.stripTrailingZeros().toPlainString();
        }
        return "máximo " + max.stripTrailingZeros().toPlainString();
    }

    private record EvaluacionCaracteristica(
            double puntaje,
            boolean cumple,
            String detalle) {
    }
}
