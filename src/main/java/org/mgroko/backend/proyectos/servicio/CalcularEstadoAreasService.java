package org.mgroko.backend.proyectos.servicio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.mgroko.backend.proyectos.dto.AlertaInforme;
import org.mgroko.backend.proyectos.dto.EstadoAreaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.AreaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.DecisionInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ResumenInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.RiesgoEntregaInforme;
import org.mgroko.backend.proyectos.dto.SeveridadInforme;
import org.springframework.stereotype.Service;

/**
 * Calcula el estado por área del informe de proyecto.
 *
 * <p>No existe un puntaje agregado. El color de cada área surge de sus
 * alertas y el estado general es el área más grave.</p>
 */
@Service
public class CalcularEstadoAreasService {

    private static final List<String> AREAS = List.of(
            AreasInforme.CRONOGRAMA,
            AreasInforme.EQUIPO,
            AreasInforme.CONTRATOS,
            AreasInforme.BRIEF);

    public ResumenInforme calcular(
            ContextoInformeProyecto contexto,
            List<AlertaInforme> alertas) {

        List<AlertaInforme> alertasSeguras = alertas == null ? List.of() : alertas;
        List<AreaInforme> areas = new ArrayList<>();

        for (String codigo : AREAS) {
            if (contexto.esBorrador()
                    && (AreasInforme.CRONOGRAMA.equals(codigo)
                            || AreasInforme.CONTRATOS.equals(codigo))) {
                areas.add(new AreaInforme(codigo, EstadoAreaInforme.NO_APLICA,
                        "No se evalúa en un proyecto en borrador."));
                continue;
            }

            List<AlertaInforme> delArea = alertasSeguras.stream()
                    .filter(alerta -> Objects.equals(codigo, alerta.area()))
                    .sorted(Comparator
                            .comparingInt((AlertaInforme alerta) -> orden(alerta.severidad()))
                            .thenComparing(AlertaInforme::codigo))
                    .toList();

            boolean tieneDatos = tieneDatos(contexto, codigo);
            EstadoAreaInforme estado = estado(delArea, tieneDatos);
            areas.add(new AreaInforme(codigo, estado, motivo(delArea, estado)));
        }

        EstadoAreaInforme estadoGeneral = estadoGeneral(areas);
        List<DecisionInforme> decisiones = decisiones(alertasSeguras);
        RiesgoEntregaInforme riesgo = riesgo(contexto);

        return new ResumenInforme(
                estadoGeneral.name(),
                areas,
                riesgo,
                decisiones);
    }

    private EstadoAreaInforme estado(
            List<AlertaInforme> alertas,
            boolean tieneDatos) {

        if (alertas.stream().anyMatch(alerta -> alerta.severidad() == SeveridadInforme.ALTA)) {
            return EstadoAreaInforme.ROJO;
        }
        if (alertas.stream().anyMatch(alerta -> alerta.severidad() == SeveridadInforme.MEDIA)) {
            return EstadoAreaInforme.AMARILLO;
        }
        return tieneDatos ? EstadoAreaInforme.VERDE : EstadoAreaInforme.SIN_DATOS;
    }

    private boolean tieneDatos(ContextoInformeProyecto contexto, String codigo) {
        return switch (codigo) {
            case AreasInforme.CRONOGRAMA -> contexto.tieneDatosCronograma();
            case AreasInforme.EQUIPO -> contexto.tieneDatosEquipo();
            case AreasInforme.CONTRATOS -> contexto.tieneDatosContratos();
            case AreasInforme.BRIEF -> contexto.tieneDatosBrief();
            default -> false;
        };
    }

    private String motivo(List<AlertaInforme> alertas, EstadoAreaInforme estado) {
        if (estado == EstadoAreaInforme.NO_APLICA) {
            return "No se evalúa en este modo del informe.";
        }
        if (!alertas.isEmpty()) {
            return alertas.stream()
                    .limit(2)
                    .map(AlertaInforme::mensaje)
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
        }
        if (estado == EstadoAreaInforme.SIN_DATOS) {
            return "Sin datos para evaluar.";
        }
        return "Sin alertas.";
    }

    private EstadoAreaInforme estadoGeneral(List<AreaInforme> areas) {
        EstadoAreaInforme peor = null;
        for (AreaInforme area : areas) {
            if (area.estado() == EstadoAreaInforme.NO_APLICA
                    || area.estado() == EstadoAreaInforme.SIN_DATOS) {
                continue;
            }
            if (peor == null || gravedad(area.estado()) > gravedad(peor)) {
                peor = area.estado();
            }
        }
        return peor == null ? EstadoAreaInforme.SIN_DATOS : peor;
    }

    private List<DecisionInforme> decisiones(List<AlertaInforme> alertas) {
        return alertas.stream()
                .filter(alerta -> alerta.severidad() == SeveridadInforme.ALTA
                        || alerta.severidad() == SeveridadInforme.MEDIA)
                .sorted(Comparator
                        .comparingInt((AlertaInforme alerta) -> orden(alerta.severidad()))
                        .thenComparing(AlertaInforme::antiguedadDias,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AlertaInforme::codigo))
                .limit(3)
                .map(alerta -> new DecisionInforme(
                        alerta.severidad().name(),
                        alerta.codigo(),
                        alerta.mensaje(),
                        alerta.antiguedadDias(),
                        alerta.accionSugerida()))
                .toList();
    }

    private RiesgoEntregaInforme riesgo(ContextoInformeProyecto contexto) {
        boolean sinDatos = !contexto.tieneFechaEntrega() || !contexto.tieneActividades();
        return new RiesgoEntregaInforme(
                contexto.fechaFinProyectada() != null
                        ? contexto.fechaFinProyectada().toLocalDate()
                        : null,
                contexto.fechaEntrega(),
                contexto.margenDias(),
                contexto.criticasSinIniciar(),
                contexto.actividadesVencidas(),
                contexto.conflictos(),
                contexto.avanceActividades(),
                sinDatos);
    }

    private int orden(SeveridadInforme severidad) {
        return switch (severidad) {
            case ALTA -> 0;
            case MEDIA -> 1;
            case BAJA -> 2;
            case INFO -> 3;
        };
    }

    private int gravedad(EstadoAreaInforme estado) {
        return switch (estado) {
            case ROJO -> 3;
            case AMARILLO -> 2;
            case VERDE -> 1;
            case SIN_DATOS, NO_APLICA -> 0;
        };
    }
}
