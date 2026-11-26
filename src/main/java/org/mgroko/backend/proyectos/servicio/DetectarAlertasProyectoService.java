package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.AlertaInforme;
import org.mgroko.backend.proyectos.dto.SeveridadInforme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Detecta las alertas accionables del informe de proyecto.
 *
 * <p>Las alertas son la única fuente del estado por área. Cada alerta
 * operativa declara su área; las alertas informativas pueden no tenerla.</p>
 */
@Service
public class DetectarAlertasProyectoService {

    private final int margenAjustadoDias;
    private final int contratoPendienteDias;
    private final int postulacionPendienteDias;

    public DetectarAlertasProyectoService(
            @Value("${app.informe-proyecto.margen-ajustado-dias:7}") int margenAjustadoDias,
            @Value("${app.informe-proyecto.contrato-pendiente-dias:7}") int contratoPendienteDias,
            @Value("${app.informe-proyecto.postulacion-pendiente-dias:7}") int postulacionPendienteDias) {
        this.margenAjustadoDias = Math.max(1, margenAjustadoDias);
        this.contratoPendienteDias = Math.max(1, contratoPendienteDias);
        this.postulacionPendienteDias = Math.max(1, postulacionPendienteDias);
    }

    public List<AlertaInforme> detectar(ContextoInformeProyecto contexto) {
        List<AlertaInforme> alertas = new ArrayList<>();

        if (contexto.proyecto().getEstado() == EstadoProyecto.CANCELADO) {
            alertas.add(alerta(
                    "PROYECTO_CANCELADO",
                    null,
                    SeveridadInforme.INFO,
                    "El proyecto está cancelado"
                            + (contexto.proyecto().getMotivoCancelacion() != null
                                    ? ": " + contexto.proyecto().getMotivoCancelacion()
                                    : "."),
                    null,
                    "Consultar el motivo y el historial del proyecto"));
        }

        if (contexto.esCierre()) {
            agregarAlertaCalificaciones(contexto, alertas);
            return alertas;
        }

        if (!contexto.esBorrador()) {
            agregarAlertasCronograma(contexto, alertas);
            agregarAlertasEquipo(contexto, alertas);
            agregarAlertasContratos(contexto, alertas);
        } else {
            agregarAlertasBorrador(contexto, alertas);
        }

        agregarAlertaBrief(contexto, alertas);
        return alertas;
    }

    private void agregarAlertasCronograma(
            ContextoInformeProyecto contexto,
            List<AlertaInforme> alertas) {

        if (!contexto.tieneFechaEntrega()) {
            alertas.add(alerta(
                    "SIN_FECHA_ENTREGA",
                    AreasInforme.CRONOGRAMA,
                    SeveridadInforme.ALTA,
                    "El proyecto no tiene fecha de entrega definida.",
                    null,
                    "Completar la planificación y la fecha de entrega"));
        }

        if (contexto.proyecto().getEstado() == EstadoProyecto.CONFIRMADO
                && !contexto.tieneActividades()) {
            alertas.add(alerta(
                    "SIN_ACTIVIDADES",
                    AreasInforme.CRONOGRAMA,
                    SeveridadInforme.MEDIA,
                    "El proyecto está confirmado pero no tiene actividades planificadas.",
                    null,
                    "Planificar las actividades del proyecto"));
        }

        if (Boolean.FALSE.equals(contexto.cronogramaViable())) {
            alertas.add(alerta(
                    "CRONOGRAMA_INVIABLE",
                    AreasInforme.CRONOGRAMA,
                    SeveridadInforme.ALTA,
                    "El cronograma no es viable con la fecha de entrega actual.",
                    null,
                    "Reprogramar o ajustar el alcance"));
        }

        Integer margenDias = contexto.margenDias();
        if (margenDias != null && margenDias < 0) {
            alertas.add(alerta(
                    "MARGEN_NEGATIVO",
                    AreasInforme.CRONOGRAMA,
                    SeveridadInforme.ALTA,
                    "El fin proyectado supera la entrega por " + Math.abs(margenDias) + " días.",
                    null,
                    "Acortar el alcance o reprogramar"));
        } else if (margenDias != null && margenDias < margenAjustadoDias) {
            alertas.add(alerta(
                    "MARGEN_AJUSTADO",
                    AreasInforme.CRONOGRAMA,
                    SeveridadInforme.MEDIA,
                    "El margen de entrega es de " + margenDias + " días.",
                    null,
                    "Revisar el plan y las actividades críticas"));
        }

        if (contexto.conflictos() > 0) {
            alertas.add(alerta(
                    "CONFLICTOS_CRONOGRAMA",
                    AreasInforme.CRONOGRAMA,
                    SeveridadInforme.ALTA,
                    "Hay " + contexto.conflictos() + " conflicto(s) en el cronograma.",
                    null,
                    "Resolver los conflictos de dependencias o fechas"));
        }

        if (contexto.actividadesVencidas() > 0) {
            alertas.add(alerta(
                    "ACTIVIDADES_VENCIDAS",
                    AreasInforme.CRONOGRAMA,
                    SeveridadInforme.ALTA,
                    "Hay " + contexto.actividadesVencidas()
                            + " actividad(es) con fin calculado vencido.",
                    antiguedad(contexto.fechaActividadVencidaMasAntigua(), contexto.ahora()),
                    "Reprogramar las actividades vencidas"));
        }

        if (contexto.criticasSinIniciar() > 0) {
            alertas.add(alerta(
                    "ACTIVIDADES_CRITICAS_SIN_INICIAR",
                    AreasInforme.CRONOGRAMA,
                    SeveridadInforme.ALTA,
                    "Hay " + contexto.criticasSinIniciar()
                            + " actividad(es) críticas sin iniciar.",
                    null,
                    "Priorizar el inicio de las actividades críticas"));
        }
    }

    private void agregarAlertasEquipo(
            ContextoInformeProyecto contexto,
            List<AlertaInforme> alertas) {

        if (contexto.requerimientosActivos().isEmpty()) {
            if (contexto.proyecto().getEstado() == EstadoProyecto.PUBLICADO
                    || contexto.proyecto().getEstado() == EstadoProyecto.CONFIRMADO) {
                alertas.add(alerta(
                        "SIN_REQUERIMIENTOS",
                        AreasInforme.EQUIPO,
                        SeveridadInforme.ALTA,
                        "El proyecto no tiene requerimientos de personal definidos.",
                        null,
                        "Definir los perfiles y cupos necesarios"));
            }
        } else {
            int faltantes = contexto.requerimientosActivos().stream()
                    .map(requerimiento -> contexto.cupos().get(requerimiento.getIdRequerimientoGral()))
                    .filter(cupo -> cupo != null)
                    .mapToInt(cupo -> Math.max(0, cupo.faltantes()))
                    .sum();
            if (faltantes > 0) {
                boolean confirmado = contexto.proyecto().getEstado() == EstadoProyecto.CONFIRMADO;
                alertas.add(alerta(
                        "CUPOS_SIN_CUBRIR",
                        AreasInforme.EQUIPO,
                        confirmado ? SeveridadInforme.ALTA : SeveridadInforme.MEDIA,
                        "Faltan " + faltantes + " cupo(s) por cubrir en el equipo.",
                        null,
                        confirmado ? "Reclutar o reasignar miembros" : "Revisar postulaciones"));
            }
        }

        if (contexto.miembrosSinTyc() > 0) {
            alertas.add(alerta(
                    "MIEMBROS_SIN_TYC",
                    AreasInforme.EQUIPO,
                    SeveridadInforme.MEDIA,
                    "Hay " + contexto.miembrosSinTyc()
                            + " miembro(s) activo(s) sin aceptar los T&C.",
                    antiguedad(contexto.fechaMiembroSinTycMasAntigua(), contexto.ahora()),
                    "Solicitar la aceptación de los T&C"));
        }

        if (contexto.postulacionesPendientes() > 0) {
            Long antiguedad = antiguedad(contexto.fechaPostulacionMasAntigua(), contexto.ahora());
            alertas.add(alerta(
                    "POSTULACIONES_PENDIENTES",
                    AreasInforme.EQUIPO,
                    antiguedad != null && antiguedad > postulacionPendienteDias
                            ? SeveridadInforme.ALTA
                            : SeveridadInforme.MEDIA,
                    "Hay " + contexto.postulacionesPendientes()
                            + " postulación(es) pendiente(s) de respuesta.",
                    antiguedad,
                    "Responder las postulaciones"));
        }

        if (contexto.invitacionesPendientes() > 0) {
            Long antiguedad = antiguedad(contexto.fechaInvitacionMasAntigua(), contexto.ahora());
            alertas.add(alerta(
                    "INVITACIONES_PENDIENTES",
                    AreasInforme.EQUIPO,
                    antiguedad != null && antiguedad > postulacionPendienteDias
                            ? SeveridadInforme.ALTA
                            : SeveridadInforme.MEDIA,
                    "Hay " + contexto.invitacionesPendientes()
                            + " invitación(es) pendiente(s) de respuesta.",
                    antiguedad,
                    "Hacer seguimiento de las invitaciones"));
        }
    }

    private void agregarAlertasContratos(
            ContextoInformeProyecto contexto,
            List<AlertaInforme> alertas) {

        if (contexto.contratosPendientesFirma() == 0) {
            return;
        }

        Long antiguedad = antiguedad(
                contexto.fechaContratoPendienteMasAntigua(),
                contexto.ahora());
        boolean vencido = antiguedad != null && antiguedad > contratoPendienteDias;

        alertas.add(alerta(
                "CONTRATOS_PENDIENTES_FIRMA",
                AreasInforme.CONTRATOS,
                vencido ? SeveridadInforme.ALTA : SeveridadInforme.MEDIA,
                "Hay " + contexto.contratosPendientesFirma()
                        + " contrato(s) pendiente(s) de firma.",
                antiguedad,
                "Escalar la firma de los contratos"));
    }

    private void agregarAlertasBorrador(
            ContextoInformeProyecto contexto,
            List<AlertaInforme> alertas) {

        if (contexto.miembrosSinTyc() > 0) {
            alertas.add(alerta(
                    "MIEMBROS_SIN_TYC",
                    AreasInforme.EQUIPO,
                    SeveridadInforme.MEDIA,
                    "Hay " + contexto.miembrosSinTyc()
                            + " miembro(s) activo(s) sin aceptar los T&C.",
                    antiguedad(contexto.fechaMiembroSinTycMasAntigua(), contexto.ahora()),
                    "Solicitar la aceptación de los T&C"));
        }
    }

    private void agregarAlertaBrief(
            ContextoInformeProyecto contexto,
            List<AlertaInforme> alertas) {

        boolean tieneMoodboard = contexto.moodboard() != null
                && contexto.moodboard().getImagenes() != null
                && !contexto.moodboard().getImagenes().isEmpty();

        if (!tieneMoodboard
                && (contexto.proyecto().getEstado() == EstadoProyecto.PUBLICADO
                        || contexto.proyecto().getEstado() == EstadoProyecto.CONFIRMADO)) {
            alertas.add(alerta(
                    "SIN_MOODBOARD",
                    AreasInforme.BRIEF,
                    SeveridadInforme.MEDIA,
                    "El proyecto no tiene imágenes de moodboard.",
                    null,
                    "Cargar las referencias visuales"));
        } else if (tieneMoodboard
                && (contexto.moodboard().getDescripcion() == null
                        || contexto.moodboard().getDescripcion().isBlank())) {
            alertas.add(alerta(
                    "MOODBOARD_INCOMPLETO",
                    AreasInforme.BRIEF,
                    SeveridadInforme.BAJA,
                    "El moodboard no tiene una descripción conceptual.",
                    null,
                    "Describir la dirección visual del proyecto"));
        }

        Double completitud = contexto.completitudBrief();
        if (completitud != null && completitud < 1.0 && alertas.stream()
                .noneMatch(alerta -> AreasInforme.BRIEF.equals(alerta.area()))) {
            alertas.add(alerta(
                    "BRIEF_INCOMPLETO",
                    AreasInforme.BRIEF,
                    SeveridadInforme.MEDIA,
                    "El brief tiene " + Math.round(completitud * 6)
                            + " de 6 ítems completos.",
                    null,
                    "Completar la información del proyecto"));
        }
    }

    private void agregarAlertaCalificaciones(
            ContextoInformeProyecto contexto,
            List<AlertaInforme> alertas) {

        if (contexto.proyecto().getEstado() != EstadoProyecto.FINALIZADO) {
            return;
        }
        long esperadas = contexto.calificacionesEsperadas();
        long registradas = contexto.calificacionesCantidad();
        if (esperadas > 0 && registradas < esperadas) {
            alertas.add(alerta(
                    "SIN_CALIFICACIONES",
                    null,
                    SeveridadInforme.BAJA,
                    "El equipo registró " + registradas + " de " + esperadas
                            + " calificaciones esperadas.",
                    null,
                    "Promover la calificación del equipo"));
        }
    }

    private AlertaInforme alerta(
            String codigo,
            String area,
            SeveridadInforme severidad,
            String mensaje,
            Long antiguedadDias,
            String accionSugerida) {
        return new AlertaInforme(
                codigo,
                area,
                severidad,
                mensaje,
                antiguedadDias,
                accionSugerida);
    }

    private Long antiguedad(LocalDateTime referencia, LocalDateTime ahora) {
        if (referencia == null || ahora == null) {
            return null;
        }
        long dias = ChronoUnit.DAYS.between(referencia.toLocalDate(), ahora.toLocalDate());
        return Math.max(0, dias);
    }
}
