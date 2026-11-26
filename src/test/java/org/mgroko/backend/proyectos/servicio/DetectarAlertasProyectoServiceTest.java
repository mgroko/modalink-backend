package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.AlertaInforme;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.dto.SeveridadInforme;

class DetectarAlertasProyectoServiceTest {

    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 25, 10, 0);

    private final DetectarAlertasProyectoService service =
            new DetectarAlertasProyectoService(7, 7, 7);

    @Test
    void proyectoPublicadoSinFechaGeneraAlertaDeCronograma() {
        Proyecto proyecto = proyecto(EstadoProyecto.PUBLICADO);
        var contexto = contexto(proyecto, null, List.of(), Map.of(),
                List.of(), List.of(), List.of(), List.of());

        assertThat(service.detectar(contexto))
                .extracting(AlertaInforme::codigo)
                .contains("SIN_FECHA_ENTREGA");
    }

    @Test
    void cuposFaltantesEnConfirmadoSonAlertaAlta() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO);
        Planificacion planificacion = planificacion(LocalDate.of(2026, 12, 15));
        RequerimientoGralProyecto requerimiento = requerimiento(1L, 2);
        var contexto = contexto(
                proyecto,
                planificacion,
                List.of(requerimiento),
                Map.of(1L, new CupoRequerimiento(2, 0, 2, false)),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        AlertaInforme alerta = service.detectar(contexto).stream()
                .filter(a -> "CUPOS_SIN_CUBRIR".equals(a.codigo()))
                .findFirst()
                .orElseThrow();

        assertThat(alerta.severidad()).isEqualTo(SeveridadInforme.ALTA);
        assertThat(alerta.area()).isEqualTo(AreasInforme.EQUIPO);
    }

    @Test
    void actividadVencidaIncluyeAntiguedad() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO);
        Planificacion planificacion = planificacion(LocalDate.of(2026, 12, 15));
        Actividad actividad = Actividad.builder()
                .idActividad(1L)
                .nombre("Actividad vencida")
                .estado(EstadoActividad.PENDIENTE)
                .fechaHoraInicio(AHORA.minusDays(2))
                .duracionMinutos(60)
                .build();
        var contexto = contexto(
                proyecto,
                planificacion,
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(actividad));

        AlertaInforme alerta = service.detectar(contexto).stream()
                .filter(a -> "ACTIVIDADES_VENCIDAS".equals(a.codigo()))
                .findFirst()
                .orElseThrow();

        assertThat(alerta.severidad()).isEqualTo(SeveridadInforme.ALTA);
        assertThat(alerta.antiguedadDias()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void contratoPendienteAntiguoEsAlertaAlta() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO);
        Contrato contrato = Contrato.builder()
                .idContrato(3L)
                .nombre("Contrato")
                .estado(EstadoContrato.PENDIENTE_FIRMA)
                .fechaCreacion(AHORA.minusDays(10))
                .fechaEmision(AHORA.minusDays(10))
                .build();
        var contexto = contexto(
                proyecto,
                planificacion(LocalDate.of(2026, 12, 15)),
                List.of(),
                Map.of(),
                List.of(contrato),
                List.of(),
                List.of(),
                List.of());

        AlertaInforme alerta = service.detectar(contexto).stream()
                .filter(a -> "CONTRATOS_PENDIENTES_FIRMA".equals(a.codigo()))
                .findFirst()
                .orElseThrow();

        assertThat(alerta.severidad()).isEqualTo(SeveridadInforme.ALTA);
        assertThat(alerta.area()).isEqualTo(AreasInforme.CONTRATOS);
        assertThat(alerta.antiguedadDias()).isEqualTo(10);
    }

    @Test
    void postulacionPendienteAntiguaEsAlertaAlta() {
        Proyecto proyecto = proyecto(EstadoProyecto.PUBLICADO);
        PostulacionGral postulacion = PostulacionGral.builder()
                .fechaPostulacion(AHORA.minusDays(9))
                .build();
        var contexto = contexto(
                proyecto,
                planificacion(LocalDate.of(2026, 12, 15)),
                List.of(),
                Map.of(),
                List.of(),
                List.of(postulacion),
                List.of(),
                List.of());

        AlertaInforme alerta = service.detectar(contexto).stream()
                .filter(a -> "POSTULACIONES_PENDIENTES".equals(a.codigo()))
                .findFirst()
                .orElseThrow();

        assertThat(alerta.severidad()).isEqualTo(SeveridadInforme.ALTA);
        assertThat(alerta.antiguedadDias()).isEqualTo(9);
    }

    @Test
    void modoCierreNoGeneraAlertasOperativas() {
        Proyecto proyecto = proyecto(EstadoProyecto.FINALIZADO);
        var contexto = contexto(proyecto, null, List.of(), Map.of(),
                List.of(), List.of(), List.of(), List.of());

        assertThat(service.detectar(contexto))
                .extracting(AlertaInforme::codigo)
                .doesNotContain(
                        "SIN_FECHA_ENTREGA",
                        "CUPOS_SIN_CUBRIR",
                        "CONTRATOS_PENDIENTES_FIRMA",
                        "ACTIVIDADES_VENCIDAS");
    }

    @Test
    void borradorNoGeneraAlertasOperativasDeCronograma() {
        Proyecto proyecto = proyecto(EstadoProyecto.BORRADOR);
        var contexto = contexto(proyecto, null, List.of(), Map.of(),
                List.of(), List.of(), List.of(), List.of());

        assertThat(service.detectar(contexto))
                .extracting(AlertaInforme::codigo)
                .doesNotContain("SIN_FECHA_ENTREGA", "CONTRATOS_PENDIENTES_FIRMA");
    }

    @Test
    void invitacionPendienteSeReportaEnEquipo() {
        Proyecto proyecto = proyecto(EstadoProyecto.PUBLICADO);
        InvitacionGral invitacion = InvitacionGral.builder()
                .fechaEnvio(AHORA.minusDays(2))
                .build();
        var contexto = contexto(
                proyecto,
                planificacion(LocalDate.of(2026, 12, 15)),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(invitacion),
                List.of());

        assertThat(service.detectar(contexto))
                .extracting(AlertaInforme::codigo)
                .contains("INVITACIONES_PENDIENTES");
    }

    private ContextoInformeProyecto contexto(
            Proyecto proyecto,
            Planificacion planificacion,
            List<RequerimientoGralProyecto> requerimientos,
            Map<Long, CupoRequerimiento> cupos,
            List<Contrato> contratos,
            List<PostulacionGral> postulaciones,
            List<InvitacionGral> invitaciones,
            List<Actividad> actividades) {
        return new ContextoInformeProyecto(
                proyecto,
                null,
                planificacion,
                List.of(),
                requerimientos,
                cupos,
                null,
                10,
                List.of(),
                actividades,
                null,
                contratos,
                Map.of(),
                postulaciones,
                List.of(),
                invitaciones,
                List.of(),
                List.of(),
                AHORA);
    }

    private Proyecto proyecto(EstadoProyecto estado) {
        return Proyecto.builder()
                .idProyecto(1L)
                .nombre("Proyecto")
                .descripcion("Descripción")
                .estado(estado)
                .fechaInicio(AHORA.minusDays(20))
                .build();
    }

    private Planificacion planificacion(LocalDate entrega) {
        return Planificacion.builder()
                .fechaEntrega(entrega)
                .build();
    }

    private RequerimientoGralProyecto requerimiento(Long id, int cantidad) {
        return RequerimientoGralProyecto.builder()
                .idRequerimientoGral(id)
                .cantidad(cantidad)
                .build();
    }
}
