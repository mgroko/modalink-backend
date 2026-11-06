package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.ConflictoCpm;
import org.mgroko.backend.actividades.dto.NodoCpm;
import org.mgroko.backend.actividades.exception.CronogramaInviableException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;

/**
 * Matriz de pruebas del motor CPM puro de dos pasadas (plan v3, sección 8.1).
 * Todos los valores están en minutos y {@code t0 = 0}.
 */
class CpmMotorTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-10-25T12:00:00Z"),
            ZoneId.of("UTC"));

    private final CronogramaService cronogramaService = new CronogramaService(RELOJ);

    // ------------------------------------------------------------------
    // Sección 8.1: casos de la tabla
    // ------------------------------------------------------------------

    @Test
    void cadenaSimple_calculaAmbasPasadasYElMargen() {
        Actividad a = actividad(1L, 60);
        Actividad b = actividad(2L, 120);
        Actividad c = actividad(3L, 60);
        b.setPredecesoras(Set.of(a));
        c.setPredecesoras(Set.of(b));

        CalculoCpm calculo = calcular(List.of(a, b, c), Map.of(), 300L);

        assertThat(calculo.makespan()).isEqualTo(240);
        assertThat(calculo.minutosHastaEntrega()).isEqualTo(300);
        assertThat(calculo.margen()).isEqualTo(60);
        assertThat(calculo.viable()).isTrue();
        assertThat(calculo.parcial()).isFalse();
        assertThat(calculo.conflictos()).isEmpty();

        NodoCpm nodoA = calculo.nodoDe(1L);
        NodoCpm nodoB = calculo.nodoDe(2L);
        NodoCpm nodoC = calculo.nodoDe(3L);

        assertThat(nodoA.inicioTemprano()).isZero();
        assertThat(nodoA.finTemprano()).isEqualTo(60);
        assertThat(nodoA.inicioTardioMakespan()).isZero();
        assertThat(nodoA.finTardioMakespan()).isEqualTo(60);
        assertThat(nodoA.holguraMakespan()).isZero();
        assertThat(nodoA.inicioTardioEntrega()).isEqualTo(60);
        assertThat(nodoA.finTardioEntrega()).isEqualTo(120);
        assertThat(nodoA.holguraEntrega()).isEqualTo(60);

        assertThat(nodoB.inicioTemprano()).isEqualTo(60);
        assertThat(nodoB.finTemprano()).isEqualTo(180);
        assertThat(nodoB.inicioTardioMakespan()).isEqualTo(60);
        assertThat(nodoB.finTardioMakespan()).isEqualTo(180);
        assertThat(nodoB.holguraMakespan()).isZero();
        assertThat(nodoB.inicioTardioEntrega()).isEqualTo(120);
        assertThat(nodoB.finTardioEntrega()).isEqualTo(240);
        assertThat(nodoB.holguraEntrega()).isEqualTo(60);

        assertThat(nodoC.inicioTemprano()).isEqualTo(180);
        assertThat(nodoC.finTemprano()).isEqualTo(240);
        assertThat(nodoC.inicioTardioMakespan()).isEqualTo(180);
        assertThat(nodoC.holguraMakespan()).isZero();
        assertThat(nodoC.inicioTardioEntrega()).isEqualTo(240);
        assertThat(nodoC.finTardioEntrega()).isEqualTo(300);
        assertThat(nodoC.holguraEntrega()).isEqualTo(60);

        assertThat(nodoA.ventanaInicio().inicioMinutos()).isZero();
        assertThat(nodoA.ventanaInicio().finMinutos()).isEqualTo(60);
        assertThat(nodoA.ventanaEjecucion().inicioMinutos()).isZero();
        assertThat(nodoA.ventanaEjecucion().finMinutos()).isEqualTo(120);
    }

    @Test
    void paralelasConUnion_calculaHolgurasYCadenaCritica() {
        Actividad a = actividad(1L, 60);
        Actividad b = actividad(2L, 120);
        Actividad c = actividad(3L, 60);
        c.setPredecesoras(Set.of(a, b));

        CalculoCpm calculo = calcular(List.of(a, b, c), Map.of(), 300L);

        assertThat(calculo.makespan()).isEqualTo(180);

        NodoCpm nodoA = calculo.nodoDe(1L);
        NodoCpm nodoB = calculo.nodoDe(2L);
        NodoCpm nodoC = calculo.nodoDe(3L);

        assertThat(nodoA.holguraMakespan()).isEqualTo(60);
        assertThat(nodoA.critica()).isFalse();
        assertThat(nodoA.holguraEntrega()).isEqualTo(180);

        assertThat(nodoB.holguraMakespan()).isZero();
        assertThat(nodoB.critica()).isTrue();
        assertThat(nodoB.holguraEntrega()).isEqualTo(120);

        assertThat(nodoC.inicioTemprano()).isEqualTo(120);
        assertThat(nodoC.finTemprano()).isEqualTo(180);
        assertThat(nodoC.holguraMakespan()).isZero();
        assertThat(nodoC.critica()).isTrue();
        assertThat(nodoC.holguraEntrega()).isEqualTo(120);

        assertThat(nodoA.enRutaCritica()).isFalse();
        assertThat(nodoB.enRutaCritica()).isTrue();
        assertThat(nodoC.enRutaCritica()).isTrue();
    }

    @Test
    void sucesoraFija_anclaLaFijaYNoLeSumaElMargenAlFinTardio() {
        Actividad a = actividad(1L, 240);
        Actividad b = actividad(2L, 120);
        b.setPredecesoras(Set.of(a));

        CalculoCpm calculo = calcular(List.of(a, b), Map.of(2L, 240L), 540L);

        assertThat(calculo.makespan()).isEqualTo(360);
        assertThat(calculo.margen()).isEqualTo(180);
        assertThat(calculo.viable()).isTrue();

        NodoCpm nodoA = calculo.nodoDe(1L);
        NodoCpm nodoB = calculo.nodoDe(2L);

        assertThat(nodoA.inicioTemprano()).isZero();
        assertThat(nodoA.finTemprano()).isEqualTo(240);
        assertThat(nodoA.inicioTardioMakespan()).isZero();
        assertThat(nodoA.finTardioMakespan()).isEqualTo(240);
        assertThat(nodoA.holguraMakespan()).isZero();

        // Regresión explícita de P2.8: no es 240 + 180 = 420.
        assertThat(nodoA.inicioTardioEntrega()).isZero();
        assertThat(nodoA.finTardioEntrega()).isEqualTo(240);
        assertThat(nodoA.holguraEntrega()).isZero();

        assertThat(nodoB.fijo()).isTrue();
        assertThat(nodoB.inicioFijo()).isEqualTo(240);
        assertThat(nodoB.inicioTemprano()).isEqualTo(240);
        assertThat(nodoB.finTemprano()).isEqualTo(360);
        assertThat(nodoB.inicioTardioMakespan()).isEqualTo(240);
        assertThat(nodoB.finTardioMakespan()).isEqualTo(360);
        assertThat(nodoB.inicioTardioEntrega()).isEqualTo(240);
        assertThat(nodoB.finTardioEntrega()).isEqualTo(360);
    }

    @Test
    void fijaEnElMedio_propagaElTopeDeLaFijaHaciaAtras() {
        Actividad a = actividad(1L, 60);
        Actividad b = actividad(2L, 120);
        Actividad c = actividad(3L, 60);
        b.setPredecesoras(Set.of(a));
        c.setPredecesoras(Set.of(b));

        CalculoCpm calculo = calcular(List.of(a, b, c), Map.of(2L, 120L), 360L);

        assertThat(calculo.makespan()).isEqualTo(300);

        NodoCpm nodoA = calculo.nodoDe(1L);
        NodoCpm nodoB = calculo.nodoDe(2L);
        NodoCpm nodoC = calculo.nodoDe(3L);

        assertThat(nodoA.holguraMakespan()).isEqualTo(60);
        assertThat(nodoA.holguraEntrega()).isEqualTo(60);

        assertThat(nodoB.fijo()).isTrue();
        assertThat(nodoB.inicioTardioMakespan()).isEqualTo(120);
        assertThat(nodoB.finTardioMakespan()).isEqualTo(240);

        assertThat(nodoC.holguraMakespan()).isZero();
        assertThat(nodoC.holguraEntrega()).isEqualTo(60);

        assertThat(nodoA.enRutaCritica()).isFalse();
        assertThat(nodoB.enRutaCritica()).isTrue();
        assertThat(nodoC.enRutaCritica()).isTrue();
    }

    @Test
    void conflictoConFija_esInviableAunqueElMargenSeaPositivo() {
        Actividad a = actividad(1L, 300);
        Actividad b = actividad(2L, 120);
        b.setPredecesoras(Set.of(a));

        CalculoCpm calculo = calcular(List.of(a, b), Map.of(2L, 240L), 540L);

        assertThat(calculo.makespan()).isEqualTo(360);
        assertThat(calculo.margen()).isEqualTo(180);
        assertThat(calculo.viable()).isFalse();

        NodoCpm nodoA = calculo.nodoDe(1L);
        assertThat(nodoA.holguraMakespan()).isEqualTo(-60);
        assertThat(nodoA.holguraEntrega()).isEqualTo(-60);
        assertThat(nodoA.conflicto()).isTrue();

        assertThat(calculo.conflictos())
                .hasSize(1)
                .allSatisfy(conflicto -> {
                    assertThat(conflicto.tipo()).isEqualTo(ConflictoCpm.Tipo.CONFLICTO_FIJO);
                    assertThat(conflicto.idActividad()).isEqualTo(2L);
                });
    }

    @Test
    void entregaInsuficiente_marcaHolguraDeEntregaNegativa() {
        Actividad a = actividad(1L, 300);

        CalculoCpm calculo = calcular(List.of(a), Map.of(), 240L);

        assertThat(calculo.makespan()).isEqualTo(300);
        assertThat(calculo.margen()).isEqualTo(-60);
        assertThat(calculo.viable()).isFalse();

        NodoCpm nodo = calculo.nodoDe(1L);
        assertThat(nodo.holguraMakespan()).isZero();
        assertThat(nodo.critica()).isTrue();
        assertThat(nodo.inicioTardioEntrega()).isEqualTo(-60);
        assertThat(nodo.finTardioEntrega()).isEqualTo(240);
        assertThat(nodo.holguraEntrega()).isEqualTo(-60);
        assertThat(nodo.conflicto()).isTrue();
    }

    @Test
    void fijaParalela_noSeMueveYLaLibreConservaSuHolgura() {
        Actividad a = actividad(1L, 240);
        Actividad b = actividad(2L, 120);

        CalculoCpm calculo = calcular(List.of(a, b), Map.of(2L, 240L), 540L);

        assertThat(calculo.makespan()).isEqualTo(360);

        NodoCpm nodoA = calculo.nodoDe(1L);
        NodoCpm nodoB = calculo.nodoDe(2L);

        assertThat(nodoA.holguraMakespan()).isEqualTo(120);
        assertThat(nodoA.holguraEntrega()).isEqualTo(300);

        assertThat(nodoB.fijo()).isTrue();
        assertThat(nodoB.inicioTemprano()).isEqualTo(240);
        assertThat(nodoB.finTemprano()).isEqualTo(360);
        assertThat(nodoB.holguraMakespan()).isZero();
        assertThat(nodoB.holguraEntrega()).isZero();

        assertThat(nodoB.enRutaCritica()).isTrue();
        assertThat(nodoA.enRutaCritica()).isFalse();
    }

    @Test
    void fijaNoVinculante_tieneHolguraCeroPeroNoEstaEnLaRutaCritica() {
        Actividad a = actividad(1L, 300);
        Actividad b = actividad(2L, 60);

        CalculoCpm calculo = calcular(List.of(a, b), Map.of(2L, 0L), 540L);

        assertThat(calculo.makespan()).isEqualTo(300);

        NodoCpm nodoA = calculo.nodoDe(1L);
        NodoCpm nodoB = calculo.nodoDe(2L);

        assertThat(nodoA.holguraMakespan()).isZero();
        assertThat(nodoA.critica()).isTrue();
        assertThat(nodoA.enRutaCritica()).isTrue();

        assertThat(nodoB.fijo()).isTrue();
        assertThat(nodoB.holguraMakespan()).isZero();
        assertThat(nodoB.critica()).isTrue();
        assertThat(nodoB.enRutaCritica()).isFalse();
    }

    @Test
    void fijaDespuesDeLaEntrega_haceInviableElCronograma() {
        Actividad a = actividad(1L, 60);

        CalculoCpm calculo = calcular(List.of(a), Map.of(1L, 600L), 540L);

        assertThat(calculo.makespan()).isEqualTo(660);
        assertThat(calculo.margen()).isEqualTo(-120);
        assertThat(calculo.viable()).isFalse();
        assertThat(calculo.conflictos()).isEmpty();
    }

    @Test
    void sinEntrega_devuelveResultadoParcialYViableNulo() {
        Actividad a = actividad(1L, 60);
        Actividad b = actividad(2L, 120);
        b.setPredecesoras(Set.of(a));

        CalculoCpm calculo = calcular(List.of(a, b), Map.of(), null);

        assertThat(calculo.makespan()).isEqualTo(180);
        assertThat(calculo.minutosHastaEntrega()).isNull();
        assertThat(calculo.margen()).isNull();
        assertThat(calculo.viable()).isNull();
        assertThat(calculo.parcial()).isTrue();

        NodoCpm nodo = calculo.nodoDe(1L);
        assertThat(nodo.inicioTardioEntrega()).isNull();
        assertThat(nodo.finTardioEntrega()).isNull();
        assertThat(nodo.holguraEntrega()).isNull();
        assertThat(nodo.ventanaInicio().finMinutos())
                .isEqualTo(nodo.inicioTardioMakespan());
        assertThat(nodo.ventanaEjecucion().finMinutos())
                .isEqualTo(nodo.finTardioMakespan());
    }

    @Test
    void fijaFueraDelProyecto_reportaConflictoYEsInviable() {
        Actividad a = actividad(1L, 120);

        CalculoCpm calculo = calcular(List.of(a), Map.of(1L, -60L), 540L);

        assertThat(calculo.viable()).isFalse();
        assertThat(calculo.conflictos())
                .hasSize(1)
                .allSatisfy(conflicto ->
                        assertThat(conflicto.tipo()).isEqualTo(ConflictoCpm.Tipo.FUERA_DE_PROYECTO));
        assertThat(calculo.nodoDe(1L).conflicto()).isTrue();
        assertThat(calculo.nodoDe(1L).motivoConflicto()).contains("anterior");
    }

    @Test
    void todasFijas_noHayNodosLibresYLaRutaCriticaSePropaga() {
        Actividad a = actividad(1L, 60);
        Actividad b = actividad(2L, 120);
        b.setPredecesoras(Set.of(a));

        CalculoCpm calculo = calcular(List.of(a, b), Map.of(1L, 0L, 2L, 60L), 300L);

        assertThat(calculo.makespan()).isEqualTo(180);
        assertThat(calculo.margen()).isEqualTo(120);
        assertThat(calculo.viable()).isTrue();
        assertThat(calculo.conflictos()).isEmpty();

        assertThat(calculo.nodoDe(1L).fijo()).isTrue();
        assertThat(calculo.nodoDe(2L).fijo()).isTrue();
        assertThat(calculo.nodoDe(1L).holguraMakespan()).isZero();
        assertThat(calculo.nodoDe(2L).holguraMakespan()).isZero();
        assertThat(calculo.nodoDe(1L).enRutaCritica()).isTrue();
        assertThat(calculo.nodoDe(2L).enRutaCritica()).isTrue();
    }

    @Test
    void grafoVacio_esViableYElMargenEsLaEntregaCompleta() {
        CalculoCpm calculo = calcular(List.of(), Map.of(), 300L);

        assertThat(calculo.makespan()).isZero();
        assertThat(calculo.margen()).isEqualTo(300);
        assertThat(calculo.viable()).isTrue();
        assertThat(calculo.parcial()).isFalse();
        assertThat(calculo.nodos()).isEmpty();
        assertThat(calculo.conflictos()).isEmpty();
    }

    @Test
    void ciclo_lanzaCronogramaInviable() {
        Actividad a = actividad(1L, 60);
        Actividad b = actividad(2L, 60);
        a.setPredecesoras(Set.of(b));
        b.setPredecesoras(Set.of(a));

        assertThatThrownBy(() -> calcular(List.of(a, b), Map.of(), 300L))
                .isInstanceOf(CronogramaInviableException.class)
                .hasMessageContaining("ciclo");
    }

    @Test
    void cancelada_seExcluyeDelGrafoYNoInfluyeEnElMakespan() {
        Actividad cancelada = Actividad.builder()
                .idActividad(1L)
                .nombre("Cancelada")
                .duracionMinutos(600)
                .estado(EstadoActividad.CANCELADA)
                .build();
        Actividad b = actividad(2L, 60);
        b.setPredecesoras(Set.of(cancelada));

        CalculoCpm calculo = calcular(List.of(cancelada, b), Map.of(), 300L);

        assertThat(calculo.makespan()).isEqualTo(60);
        assertThat(calculo.nodos()).containsOnlyKeys(2L);
        assertThat(calculo.viable()).isTrue();
        assertThatThrownBy(() -> calculo.nodoDe(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------
    // Casos adicionales de la sección 8.1
    // ------------------------------------------------------------------

    @Test
    void critica_usaSoloLaHolguraDeMakespan() {
        Actividad a = actividad(1L, 60);
        Actividad b = actividad(2L, 120);
        b.setPredecesoras(Set.of(a));

        CalculoCpm calculo = calcular(List.of(a, b), Map.of(), 600L);

        NodoCpm nodoA = calculo.nodoDe(1L);
        NodoCpm nodoB = calculo.nodoDe(2L);

        assertThat(nodoA.holguraMakespan()).isZero();
        assertThat(nodoA.holguraEntrega()).isEqualTo(420);
        assertThat(nodoA.critica()).isTrue();

        assertThat(nodoB.holguraMakespan()).isZero();
        assertThat(nodoB.holguraEntrega()).isEqualTo(420);
        assertThat(nodoB.critica()).isTrue();
    }

    @Test
    void cambiarLaEntregaSoloAlteraLaPasadaDeEntrega() {
        Actividad a = actividad(1L, 60);
        Actividad b = actividad(2L, 120);
        b.setPredecesoras(Set.of(a));

        CalculoCpm conEntrega300 = calcular(List.of(a, b), Map.of(), 300L);
        CalculoCpm conEntrega400 = calcular(List.of(a, b), Map.of(), 400L);

        assertThat(conEntrega400.makespan()).isEqualTo(conEntrega300.makespan());
        assertThat(conEntrega400.nodoDe(1L).holguraMakespan())
                .isEqualTo(conEntrega300.nodoDe(1L).holguraMakespan());
        assertThat(conEntrega400.nodoDe(1L).critica())
                .isEqualTo(conEntrega300.nodoDe(1L).critica());

        assertThat(conEntrega300.nodoDe(1L).holguraEntrega()).isEqualTo(120);
        assertThat(conEntrega400.nodoDe(1L).holguraEntrega()).isEqualTo(220);
        assertThat(conEntrega300.margen()).isEqualTo(120);
        assertThat(conEntrega400.margen()).isEqualTo(220);
    }

    @Test
    void releaseTime_desplazaLasRaicesLibresSinMoverLasFijasNiCambiarT0() {
        Actividad a = actividad(1L, 60);
        Actividad b = actividad(2L, 60);
        b.setPredecesoras(Set.of(a));
        Actividad fija = actividad(3L, 30);

        CalculoCpm calculo = cronogramaService.calcular(
                List.of(a, b, fija), Map.of(3L, 10L), 600L, 100L);

        assertThat(calculo.nodoDe(1L).inicioTemprano()).isEqualTo(100);
        assertThat(calculo.nodoDe(1L).finTemprano()).isEqualTo(160);
        assertThat(calculo.nodoDe(2L).inicioTemprano()).isEqualTo(160);
        assertThat(calculo.nodoDe(2L).finTemprano()).isEqualTo(220);
        assertThat(calculo.nodoDe(3L).inicioTemprano()).isEqualTo(10);
        assertThat(calculo.nodoDe(3L).finTemprano()).isEqualTo(40);
    }

    @Test
    void actividadEnCursoSinInicio_reportaDatoInconsistente() {
        Actividad enCurso = Actividad.builder()
                .idActividad(1L)
                .nombre("En curso")
                .duracionMinutos(60)
                .estado(EstadoActividad.EN_CURSO)
                .build();

        CalculoCpm calculo = calcular(List.of(enCurso), Map.of(), 300L);

        assertThat(calculo.viable()).isFalse();
        assertThat(calculo.conflictos())
                .hasSize(1)
                .allSatisfy(conflicto ->
                        assertThat(conflicto.tipo()).isEqualTo(ConflictoCpm.Tipo.DATO_INCONSISTENTE));
        assertThat(calculo.nodoDe(1L).conflicto()).isTrue();
    }

    @Test
    void calcular_esPuroYNoModificaLasActividades() {
        Actividad a = actividad(1L, 60);
        Actividad b = actividad(2L, 120);
        b.setPredecesoras(Set.of(a));

        calcular(List.of(a, b), Map.of(), 300L);

        assertThat(a.getFechaHoraInicio()).isNull();
        assertThat(b.getFechaHoraInicio()).isNull();
        assertThat(a.getEstado()).isEqualTo(EstadoActividad.PENDIENTE);
    }

    @Test
    void anexoEjemploCompleto_reproduceLasTablasDelPlan() {
        Actividad a = actividad(1L, 240);
        Actividad b = actividad(2L, 120);
        Actividad c = actividad(3L, 60);
        Actividad d = actividad(4L, 120);
        c.setPredecesoras(Set.of(a, b));
        d.setPredecesoras(Set.of(c));

        CalculoCpm conEntregaJusta = calcular(
                List.of(a, b, c, d), Map.of(2L, 240L), 540L);

        assertThat(conEntregaJusta.makespan()).isEqualTo(540);
        assertThat(conEntregaJusta.margen()).isZero();
        assertThat(conEntregaJusta.nodoDe(1L).holguraMakespan()).isEqualTo(120);
        assertThat(conEntregaJusta.nodoDe(1L).holguraEntrega()).isEqualTo(120);
        assertThat(conEntregaJusta.nodoDe(2L).fijo()).isTrue();
        assertThat(conEntregaJusta.nodoDe(2L).inicioTardioEntrega()).isEqualTo(240);
        assertThat(conEntregaJusta.nodoDe(3L).inicioTemprano()).isEqualTo(360);
        assertThat(conEntregaJusta.nodoDe(4L).finTemprano()).isEqualTo(540);

        CalculoCpm conMargen = calcular(
                List.of(a, b, c, d), Map.of(2L, 240L), 660L);

        assertThat(conMargen.makespan()).isEqualTo(540);
        assertThat(conMargen.margen()).isEqualTo(120);
        assertThat(conMargen.nodoDe(1L).inicioTardioMakespan()).isEqualTo(120);
        assertThat(conMargen.nodoDe(1L).inicioTardioEntrega()).isEqualTo(240);
        assertThat(conMargen.nodoDe(1L).finTardioEntrega()).isEqualTo(480);
        assertThat(conMargen.nodoDe(1L).holguraEntrega()).isEqualTo(240);
        assertThat(conMargen.nodoDe(2L).inicioTardioEntrega()).isEqualTo(240);
        assertThat(conMargen.nodoDe(2L).finTardioEntrega()).isEqualTo(360);
        assertThat(conMargen.nodoDe(3L).inicioTardioEntrega()).isEqualTo(480);
        assertThat(conMargen.nodoDe(3L).finTardioEntrega()).isEqualTo(540);
        assertThat(conMargen.nodoDe(3L).holguraEntrega()).isEqualTo(120);
        assertThat(conMargen.nodoDe(4L).inicioTardioEntrega()).isEqualTo(540);
        assertThat(conMargen.nodoDe(4L).finTardioEntrega()).isEqualTo(660);
        assertThat(conMargen.nodoDe(4L).holguraEntrega()).isEqualTo(120);

        assertThat(conMargen.nodoDe(1L).enRutaCritica()).isFalse();
        assertThat(conMargen.nodoDe(2L).enRutaCritica()).isTrue();
        assertThat(conMargen.nodoDe(3L).enRutaCritica()).isTrue();
        assertThat(conMargen.nodoDe(4L).enRutaCritica()).isTrue();
    }

    private CalculoCpm calcular(
            List<Actividad> actividades,
            Map<Long, Long> iniciosFijos,
            Long minutosHastaEntrega) {
        return cronogramaService.calcular(actividades, iniciosFijos, minutosHastaEntrega, 0L);
    }

    private Actividad actividad(Long id, int duracionMinutos) {
        return Actividad.builder()
                .idActividad(id)
                .nombre("A" + id)
                .duracionMinutos(duracionMinutos)
                .estado(EstadoActividad.PENDIENTE)
                .build();
    }
}
