package org.mgroko.backend.proyectos.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.proyectos.dto.AlertaInforme;
import org.mgroko.backend.proyectos.dto.EstadoAreaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.AreaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.CalificacionesInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ContratosInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.CronogramaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.DecisionInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.EquipoInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.FirmasInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.GeneradoPorInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.MetricasInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.MoodboardInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ProyectoInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ResumenInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.RiesgoEntregaInforme;
import org.mgroko.backend.proyectos.dto.SeveridadInforme;

/**
 * Fixture compartido por los tests del informe de proyecto.
 */
public final class InformeProyectoFixture {

    public static final LocalDateTime GENERADO_EN =
            LocalDateTime.of(2026, 11, 25, 10, 0);

    private InformeProyectoFixture() {
    }

    public static InformeProyectoResponse minimo() {
        ProyectoInforme proyecto = new ProyectoInforme(
                1L,
                "Campaña Primavera",
                "Producción editorial de moda",
                "CONFIRMADO",
                "PUBLICO",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 12, 15),
                null,
                null,
                false,
                null,
                List.of());

        ResumenInforme resumen = new ResumenInforme(
                "ROJO",
                List.of(
                        new AreaInforme("CRONOGRAMA", EstadoAreaInforme.ROJO,
                                "2 actividades vencidas y 1 conflicto. Margen de 3 días."),
                        new AreaInforme("EQUIPO", EstadoAreaInforme.ROJO,
                                "Falta 1 fotógrafo."),
                        new AreaInforme("CONTRATOS", EstadoAreaInforme.AMARILLO,
                                "1 contrato pendiente de firma. Firmas: 4 de 6."),
                        new AreaInforme("BRIEF", EstadoAreaInforme.VERDE,
                                "5 de 6 ítems completos.")),
                new RiesgoEntregaInforme(
                        LocalDate.of(2026, 12, 12),
                        LocalDate.of(2026, 12, 15),
                        3,
                        2,
                        2,
                        1,
                        0.62,
                        false),
                List.of(new DecisionInforme(
                        "ALTA",
                        "POSTULACIONES_PENDIENTES",
                        "Falta 1 fotógrafo; 5 postulaciones sin responder",
                        9L,
                        "Responder postulaciones")));

        MetricasInforme metricas = new MetricasInforme(
                0.62,
                0.58,
                3,
                4320L,
                0.83,
                0.9,
                0.75,
                0.83,
                2,
                5,
                0,
                1,
                4.2);

        MoodboardInforme moodboard = new MoodboardInforme(
                null,
                null,
                null,
                0,
                10,
                List.of());

        EquipoInforme equipo = new EquipoInforme(
                7,
                1,
                2,
                0.9,
                List.of(),
                List.of(),
                List.of());

        CronogramaInforme cronograma = new CronogramaInforme(
                LocalDateTime.of(2026, 11, 1, 9, 0),
                LocalDateTime.of(2026, 12, 12, 18, 0),
                LocalDate.of(2026, 12, 15),
                4320L,
                3,
                true,
                "AJUSTADO",
                20,
                5,
                2,
                1,
                Map.of("PENDIENTE", 8, "EN_CURSO", 2, "FINALIZADA", 10, "CANCELADA", 0),
                List.of());

        ContratosInforme contratos = new ContratosInforme(
                0,
                Map.of(),
                new FirmasInforme(0, 0, 0),
                List.of());

        List<AlertaInforme> alertas = List.of(new AlertaInforme(
                "CUPOS_SIN_CUBRIR",
                "EQUIPO",
                SeveridadInforme.ALTA,
                "Falta 1 fotógrafo.",
                9L,
                "Reclutar o reasignar"));

        return new InformeProyectoResponse(
                proyecto,
                GENERADO_EN,
                GENERADO_EN,
                new GeneradoPorInforme(8L, "Ana Dir", "DIRECTOR"),
                "OPERATIVO",
                "1.0",
                resumen,
                metricas,
                moodboard,
                equipo,
                cronograma,
                contratos,
                alertas,
                List.of(),
                new CalificacionesInforme(false, null, 0L, 0L));
    }
}
