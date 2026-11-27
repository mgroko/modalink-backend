package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.AlertaInforme;
import org.mgroko.backend.proyectos.dto.EstadoAreaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.DecisionInforme;
import org.mgroko.backend.proyectos.dto.SeveridadInforme;

class CalcularEstadoAreasServiceTest {

    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 25, 10, 0);

    private final CalcularEstadoAreasService service =
            new CalcularEstadoAreasService();

    @Test
    void areaConAlertaAltaEsRojaYGeneralRojo() {
        var contexto = contexto(EstadoProyecto.CONFIRMADO, List.of(actividad()));
        var alertas = List.of(alerta(
                "ACTIVIDADES_VENCIDAS",
                AreasInforme.CRONOGRAMA,
                SeveridadInforme.ALTA,
                5L));

        var resumen = service.calcular(contexto, alertas);

        assertThat(area(resumen, AreasInforme.CRONOGRAMA).estado())
                .isEqualTo(EstadoAreaInforme.ROJO);
        assertThat(resumen.estadoGeneral()).isEqualTo("ROJO");
    }

    @Test
    void areaSoloConAlertaMediaEsAmarilla() {
        var contexto = contexto(EstadoProyecto.CONFIRMADO, List.of(actividad()));
        var alertas = List.of(alerta(
                "MARGEN_AJUSTADO",
                AreasInforme.CRONOGRAMA,
                SeveridadInforme.MEDIA,
                null));

        var resumen = service.calcular(contexto, alertas);

        assertThat(area(resumen, AreasInforme.CRONOGRAMA).estado())
                .isEqualTo(EstadoAreaInforme.AMARILLO);
        assertThat(resumen.estadoGeneral()).isEqualTo("AMARILLO");
    }

    @Test
    void areaSinDatosNoEsVerde() {
        var contexto = contexto(EstadoProyecto.CONFIRMADO, List.of());

        var resumen = service.calcular(contexto, List.of());

        assertThat(area(resumen, AreasInforme.CONTRATOS).estado())
                .isEqualTo(EstadoAreaInforme.SIN_DATOS);
        assertThat(area(resumen, AreasInforme.CRONOGRAMA).estado())
                .isEqualTo(EstadoAreaInforme.SIN_DATOS);
    }

    @Test
    void borradorNoEvaluaCronogramaNiContratos() {
        var contexto = contexto(EstadoProyecto.BORRADOR, List.of());

        var resumen = service.calcular(contexto, List.of());

        assertThat(area(resumen, AreasInforme.CRONOGRAMA).estado())
                .isEqualTo(EstadoAreaInforme.NO_APLICA);
        assertThat(area(resumen, AreasInforme.CONTRATOS).estado())
                .isEqualTo(EstadoAreaInforme.NO_APLICA);
    }

    @Test
    void estadoGeneralEsElAreaMasGrave() {
        var contexto = contexto(EstadoProyecto.CONFIRMADO, List.of(actividad()));
        var alertas = List.of(
                alerta("CUPOS_SIN_CUBRIR", AreasInforme.EQUIPO,
                        SeveridadInforme.ALTA, null),
                alerta("CONTRATOS_PENDIENTES_FIRMA", AreasInforme.CONTRATOS,
                        SeveridadInforme.MEDIA, 8L));

        var resumen = service.calcular(contexto, alertas);

        assertThat(resumen.estadoGeneral()).isEqualTo("ROJO");
    }

    @Test
    void decisionesOrdenanPorSeveridadYAntiguedad() {
        var contexto = contexto(EstadoProyecto.CONFIRMADO, List.of(actividad()));
        var alertas = List.of(
                alerta("ALTA_RECIENTE", AreasInforme.CRONOGRAMA,
                        SeveridadInforme.ALTA, 2L),
                alerta("ALTA_ANTIGUA", AreasInforme.CRONOGRAMA,
                        SeveridadInforme.ALTA, 9L),
                alerta("MEDIA", AreasInforme.EQUIPO,
                        SeveridadInforme.MEDIA, 12L));

        var resumen = service.calcular(contexto, alertas);

        assertThat(resumen.decisiones())
                .extracting(DecisionInforme::codigo)
                .containsExactly("ALTA_ANTIGUA", "ALTA_RECIENTE", "MEDIA");
    }

    @Test
    void briefConDatosSinAlertasEsVerde() {
        var contexto = contexto(EstadoProyecto.CONFIRMADO, List.of(actividad()));

        var resumen = service.calcular(contexto, List.of());

        assertThat(area(resumen, AreasInforme.BRIEF).estado())
                .isEqualTo(EstadoAreaInforme.VERDE);
    }

    private ContextoInformeProyecto contexto(
            EstadoProyecto estado,
            List<Actividad> actividades) {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(1L)
                .nombre("Proyecto")
                .descripcion("Descripción")
                .estado(estado)
                .build();

        return new ContextoInformeProyecto(
                proyecto,
                null,
                null,
                List.of(),
                List.of(),
                Map.of(),
                null,
                10,
                List.of(),
                actividades,
                null,
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                AHORA);
    }

    private Actividad actividad() {
        return Actividad.builder()
                .idActividad(1L)
                .nombre("Actividad")
                .estado(EstadoActividad.PENDIENTE)
                .duracionMinutos(60)
                .build();
    }

    private AlertaInforme alerta(
            String codigo,
            String area,
            SeveridadInforme severidad,
            Long antiguedad) {
        return new AlertaInforme(
                codigo,
                area,
                severidad,
                "Mensaje " + codigo,
                antiguedad,
                "Acción");
    }

    private org.mgroko.backend.proyectos.dto.InformeProyectoResponse.AreaInforme area(
            org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ResumenInforme resumen,
            String codigo) {
        return resumen.areas().stream()
                .filter(area -> codigo.equals(area.codigo()))
                .findFirst()
                .orElseThrow();
    }
}
