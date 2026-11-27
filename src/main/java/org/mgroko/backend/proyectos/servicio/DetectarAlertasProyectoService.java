package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.AlertaInforme;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
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
            boolean confirmado = contexto.proyecto().getEstado() == EstadoProyecto.CONFIRMADO;
            Map<String, Integer> faltantesPorProfesion = faltantesPorProfesion(contexto);
            if (!faltantesPorProfesion.isEmpty()) {
                alertas.add(alerta(
                        "CUPOS_SIN_CUBRIR",
                        AreasInforme.EQUIPO,
                        confirmado ? SeveridadInforme.ALTA : SeveridadInforme.MEDIA,
                        mensajeCuposFaltantes(faltantesPorProfesion),
                        null,
                        confirmado ? "Reclutar o reasignar miembros" : "Revisar postulaciones"));
            }
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

    /**
     * Agrupa los cupos faltantes por profesión para poder nombrar qué falta
     * cubrir en el equipo. Los requerimientos sin profesión se agrupan juntos
     * bajo una clave vacía.
     */
    private Map<String, Integer> faltantesPorProfesion(ContextoInformeProyecto contexto) {
        Map<String, Integer> faltantes = new LinkedHashMap<>();
        for (RequerimientoGralProyecto requerimiento : contexto.requerimientosActivos()) {
            CupoRequerimiento cupo = contexto.cupos().get(requerimiento.getIdRequerimientoGral());
            if (cupo == null) {
                continue;
            }
            int pendientes = Math.max(0, cupo.faltantes());
            if (pendientes == 0) {
                continue;
            }
            faltantes.merge(nombreProfesion(requerimiento), pendientes, Integer::sum);
        }
        return faltantes;
    }

    private String mensajeCuposFaltantes(Map<String, Integer> faltantesPorProfesion) {
        List<String> frases = new ArrayList<>();
        for (Map.Entry<String, Integer> entrada : faltantesPorProfesion.entrySet()) {
            frases.add(fraseFaltante(entrada.getValue(), entrada.getKey()));
        }
        return String.join(" ", frases);
    }

    /**
     * "Falta 1 maquillador.", "Faltan 2 modelos." Las profesiones compuestas
     * ("Diseñador de Moda") usan el fallback "N personas · Profesión" para no
     * arriesgar una pluralización incorrecta.
     */
    private String fraseFaltante(int faltantes, String profesion) {
        boolean singular = faltantes == 1;
        String verbo = singular ? "Falta" : "Faltan";
        String nombre = profesion == null ? "" : profesion.trim();
        if (nombre.isEmpty()) {
            return verbo + " " + faltantes + (singular ? " cupo" : " cupos") + " en el equipo.";
        }
        if (!esProfesionSimple(nombre)) {
            return verbo + " " + faltantes
                    + (singular ? " persona · " : " personas · ") + nombre + ".";
        }
        String etiqueta = singular ? nombre.toLowerCase() : pluralizar(nombre);
        return verbo + " " + faltantes + " " + etiqueta + ".";
    }

    private boolean esProfesionSimple(String nombre) {
        return nombre.codePoints().allMatch(Character::isLetter);
    }

    private String pluralizar(String nombre) {
        String palabra = nombre.toLowerCase();
        if (palabra.isEmpty()) {
            return palabra;
        }
        char ultimo = palabra.charAt(palabra.length() - 1);
        if ("aeiouáéíóúü".indexOf(ultimo) >= 0) {
            return palabra + "s";
        }
        if (ultimo == 'z') {
            return palabra.substring(0, palabra.length() - 1) + "ces";
        }
        if ("lrnj".indexOf(ultimo) >= 0) {
            return palabra + "es";
        }
        return palabra + "s";
    }

    private String nombreProfesion(RequerimientoGralProyecto requerimiento) {
        if (requerimiento.getProfesion() == null
                || requerimiento.getProfesion().getNombre() == null) {
            return "";
        }
        return requerimiento.getProfesion().getNombre().trim();
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
