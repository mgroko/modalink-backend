package org.mgroko.backend.proyectos.servicio;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mgroko.backend.actividades.dto.ActividadCronogramaResponse;
import org.mgroko.backend.actividades.dto.CronogramaResponse;
import org.mgroko.backend.actividades.dto.ResumenCronogramaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;

/**
 * Datos de entrada del informe de proyecto.
 *
 * <p>Centraliza los cálculos derivados para que el detector de alertas y el
 * cálculo de estado por área usen exactamente las mismas definiciones.</p>
 */
record ContextoInformeProyecto(
        Proyecto proyecto,
        Perfil director,
        Planificacion planificacion,
        List<Objetivo> objetivos,
        List<RequerimientoGralProyecto> requerimientos,
        Map<Long, CupoRequerimiento> cupos,
        Moodboard moodboard,
        int limiteMoodboard,
        List<MiembroProyecto> miembros,
        List<Actividad> actividades,
        CronogramaResponse cronograma,
        List<Contrato> contratos,
        Map<Long, List<FirmaContrato>> firmasPorContrato,
        List<PostulacionGral> postulacionesGral,
        List<PostulacionActividad> postulacionesActividad,
        List<InvitacionGral> invitacionesGral,
        List<InvitacionActividad> invitacionesActividad,
        List<CalificacionPerfil> calificaciones,
        LocalDateTime ahora) {

    private static final int DIAS_POR_SEMANA = 7;

    ContextoInformeProyecto {
        objetivos = objetivos == null ? List.of() : List.copyOf(objetivos);
        requerimientos = requerimientos == null ? List.of() : List.copyOf(requerimientos);
        cupos = cupos == null ? Map.of() : Map.copyOf(cupos);
        miembros = miembros == null ? List.of() : List.copyOf(miembros);
        actividades = actividades == null ? List.of() : List.copyOf(actividades);
        contratos = contratos == null ? List.of() : List.copyOf(contratos);
        firmasPorContrato = firmasPorContrato == null ? Map.of() : Map.copyOf(firmasPorContrato);
        postulacionesGral = postulacionesGral == null ? List.of() : List.copyOf(postulacionesGral);
        postulacionesActividad = postulacionesActividad == null
                ? List.of()
                : List.copyOf(postulacionesActividad);
        invitacionesGral = invitacionesGral == null ? List.of() : List.copyOf(invitacionesGral);
        invitacionesActividad = invitacionesActividad == null
                ? List.of()
                : List.copyOf(invitacionesActividad);
        calificaciones = calificaciones == null ? List.of() : List.copyOf(calificaciones);
    }

    String modo() {
        if (proyecto.getEstado() == EstadoProyecto.BORRADOR) {
            return "BORRADOR";
        }
        if (proyecto.getEstado() == EstadoProyecto.FINALIZADO
                || proyecto.getEstado() == EstadoProyecto.CANCELADO) {
            return "CIERRE";
        }
        return "OPERATIVO";
    }

    boolean esBorrador() {
        return "BORRADOR".equals(modo());
    }

    boolean esCierre() {
        return "CIERRE".equals(modo());
    }

    List<MiembroProyecto> miembrosActivos() {
        return miembros.stream()
                .filter(miembro -> miembro.getEstadoParticipacion() == EstadoParticipacion.ACTIVO)
                .toList();
    }

    List<MiembroProyecto> miembrosHistoricos() {
        return miembros.stream()
                .filter(miembro -> miembro.getEstadoParticipacion() != EstadoParticipacion.ACTIVO)
                .toList();
    }

    List<RequerimientoGralProyecto> requerimientosActivos() {
        return requerimientos.stream()
                .filter(requerimiento -> requerimiento.getFechaArchivado() == null)
                .toList();
    }

    List<Actividad> actividadesNoCanceladas() {
        return actividades.stream()
                .filter(actividad -> actividad.getEstado() != EstadoActividad.CANCELADA)
                .toList();
    }

    Double avanceActividades() {
        List<Actividad> noCanceladas = actividadesNoCanceladas();
        if (noCanceladas.isEmpty()) {
            return null;
        }
        long finalizadas = noCanceladas.stream()
                .filter(actividad -> actividad.getEstado() == EstadoActividad.FINALIZADA)
                .count();
        return (double) finalizadas / noCanceladas.size();
    }

    Double avancePonderado() {
        List<Actividad> noCanceladas = actividadesNoCanceladas();
        long duracionTotal = noCanceladas.stream()
                .map(Actividad::getDuracionMinutos)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        if (duracionTotal == 0) {
            return null;
        }
        long duracionFinalizada = noCanceladas.stream()
                .filter(actividad -> actividad.getEstado() == EstadoActividad.FINALIZADA)
                .map(Actividad::getDuracionMinutos)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        return (double) duracionFinalizada / duracionTotal;
    }

    Long margenMinutos() {
        return resumenCronograma() == null ? null : resumenCronograma().margenMinutos();
    }

    Integer margenDias() {
        Long margen = margenMinutos();
        if (margen == null) {
            return null;
        }
        return (int) Math.round(margen / 1440.0);
    }

    LocalDateTime fechaFinProyectada() {
        return resumenCronograma() == null ? null : resumenCronograma().fechaFinProyectada();
    }

    LocalDate fechaEntrega() {
        if (resumenCronograma() != null && resumenCronograma().fechaEntrega() != null) {
            return resumenCronograma().fechaEntrega();
        }
        return planificacion == null ? null : planificacion.getFechaEntrega();
    }

    Boolean cronogramaViable() {
        return resumenCronograma() == null ? null : resumenCronograma().viable();
    }

    String estadoCronograma() {
        return resumenCronograma() == null ? null : resumenCronograma().estadoCronograma();
    }

    Integer totalActividades() {
        return actividades.size();
    }

    Integer actividadesCriticas() {
        return resumenCronograma() == null ? 0 : resumenCronograma().actividadesCriticas();
    }

    Integer conflictos() {
        return cronograma == null || cronograma.conflictos() == null
                ? 0
                : cronograma.conflictos().size();
    }

    Map<String, Integer> actividadesPorEstado() {
        Map<String, Integer> porEstado = new LinkedHashMap<>();
        for (EstadoActividad estado : EstadoActividad.values()) {
            porEstado.put(estado.name(), 0);
        }
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() != null) {
                porEstado.merge(actividad.getEstado().name(), 1, Integer::sum);
            }
        }
        return porEstado;
    }

    int actividadesVencidas() {
        return (int) actividadesNoCanceladas().stream()
                .filter(this::actividadNoFinalizada)
                .filter(this::actividadVencida)
                .count();
    }

    LocalDateTime fechaActividadVencidaMasAntigua() {
        return actividadesNoCanceladas().stream()
                .filter(this::actividadNoFinalizada)
                .filter(this::actividadVencida)
                .map(Actividad::getFechaHoraFin)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    int criticasSinIniciar() {
        if (cronograma == null || cronograma.actividades() == null) {
            return 0;
        }
        return (int) cronograma.actividades().stream()
                .filter(actividad -> Boolean.TRUE.equals(actividad.enRutaCritica()))
                .filter(actividad -> EstadoActividad.PENDIENTE.name().equals(actividad.estado()))
                .count();
    }

    Double coberturaCupos() {
        List<RequerimientoGralProyecto> activos = requerimientosActivos();
        if (activos.isEmpty()) {
            return null;
        }
        long requeridos = 0;
        long cubiertos = 0;
        for (RequerimientoGralProyecto requerimiento : activos) {
            CupoRequerimiento cupo = cupos.get(requerimiento.getIdRequerimientoGral());
            if (cupo == null) {
                continue;
            }
            requeridos += Math.max(0, cupo.cantidad());
            cubiertos += Math.max(0, cupo.cubiertos());
        }
        if (requeridos == 0) {
            return null;
        }
        return (double) cubiertos / requeridos;
    }

    Double estabilidadEquipo() {
        int total = miembros.size();
        if (total == 0) {
            return null;
        }
        int bajas = miembrosHistoricos().size();
        return 1.0 - ((double) bajas / total);
    }

    Double preparacionContractual() {
        int totalFirmas = firmasTotales();
        if (totalFirmas == 0) {
            return null;
        }
        return (double) firmasFirmadas() / totalFirmas;
    }

    Double completitudBrief() {
        int completos = 0;
        if (proyecto.getDescripcion() != null && !proyecto.getDescripcion().isBlank()) {
            completos++;
        }
        if (!objetivos.isEmpty()) {
            completos++;
        }
        if (!requerimientosActivos().isEmpty()) {
            completos++;
        }
        if (moodboard != null && moodboard.getImagenes() != null
                && !moodboard.getImagenes().isEmpty()) {
            completos++;
        }
        if (proyecto.getUbicacion() != null) {
            completos++;
        }
        if (fechaEntrega() != null) {
            completos++;
        }
        return completos / 6.0;
    }

    int postulacionesPendientes() {
        return postulacionesGral.size() + postulacionesActividad.size();
    }

    int invitacionesPendientes() {
        return invitacionesGral.size() + invitacionesActividad.size();
    }

    LocalDateTime fechaPostulacionMasAntigua() {
        return fechas(
                postulacionesGral.stream().map(PostulacionGral::getFechaPostulacion).toList(),
                postulacionesActividad.stream().map(PostulacionActividad::getFechaPostulacion).toList());
    }

    LocalDateTime fechaInvitacionMasAntigua() {
        return fechas(
                invitacionesGral.stream().map(InvitacionGral::getFechaEnvio).toList(),
                invitacionesActividad.stream().map(InvitacionActividad::getFechaEnvio).toList());
    }

    List<Contrato> contratosConsiderados() {
        return contratos.stream()
                .filter(contrato -> contrato.getEstado() == EstadoContrato.PENDIENTE_FIRMA
                        || contrato.getEstado() == EstadoContrato.ACTIVO)
                .toList();
    }

    int contratosPendientesFirma() {
        return (int) contratos.stream()
                .filter(contrato -> contrato.getEstado() == EstadoContrato.PENDIENTE_FIRMA)
                .count();
    }

    LocalDateTime fechaContratoPendienteMasAntigua() {
        return contratos.stream()
                .filter(contrato -> contrato.getEstado() == EstadoContrato.PENDIENTE_FIRMA)
                .map(this::fechaReferenciaContrato)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    int firmasFirmadas() {
        return (int) firmasConsideradas().stream()
                .filter(firma -> firma.getEstadoFirma() == EstadoFirmaContrato.FIRMADO)
                .count();
    }

    int firmasPendientes() {
        return (int) firmasConsideradas().stream()
                .filter(firma -> firma.getEstadoFirma() == EstadoFirmaContrato.PENDIENTE)
                .count();
    }

    int firmasTotales() {
        return firmasFirmadas() + firmasPendientes();
    }

    int totalActivos() {
        return miembrosActivos().size();
    }

    int totalDirectores() {
        return (int) miembrosActivos().stream()
                .filter(miembro -> RolProyectoUsuario.desde(miembro.getRolProyecto())
                        == RolProyectoUsuario.DIRECTOR)
                .count();
    }

    int totalBajas() {
        return miembrosHistoricos().size();
    }

    Double velocidadAproximada() {
        if (proyecto.getFechaInicio() == null) {
            return null;
        }
        long dias = Math.max(0, Duration.between(proyecto.getFechaInicio(), ahora).toDays());
        double semanas = Math.max(1.0, (double) dias / DIAS_POR_SEMANA);
        long finalizadas = actividades.stream()
                .filter(actividad -> actividad.getEstado() == EstadoActividad.FINALIZADA)
                .count();
        return finalizadas / semanas;
    }

    Long calificacionesCantidad() {
        return (long) calificaciones.size();
    }

    Double calificacionesPromedio() {
        if (calificaciones.isEmpty()) {
            return null;
        }
        return calificaciones.stream()
                .map(CalificacionPerfil::getValoracion)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    Long calificacionesEsperadas() {
        if (proyecto.getEstado() != EstadoProyecto.FINALIZADO) {
            return 0L;
        }
        long activos = totalActivos();
        return activos <= 1 ? 0L : activos * (activos - 1);
    }

    boolean tieneDatosCronograma() {
        return !actividades.isEmpty();
    }

    boolean tieneDatosEquipo() {
        return !requerimientosActivos().isEmpty() || !miembros.isEmpty();
    }

    boolean tieneDatosContratos() {
        return !contratos.isEmpty();
    }

    boolean tieneDatosBrief() {
        return proyecto != null;
    }

    boolean tieneFechaEntrega() {
        return fechaEntrega() != null;
    }

    boolean tieneActividades() {
        return !actividades.isEmpty();
    }

    private ResumenCronogramaResponse resumenCronograma() {
        if (cronograma == null) {
            return null;
        }
        return cronograma.resumen();
    }

    private boolean actividadNoFinalizada(Actividad actividad) {
        return actividad.getEstado() == EstadoActividad.PENDIENTE
                || actividad.getEstado() == EstadoActividad.EN_CURSO;
    }

    private boolean actividadVencida(Actividad actividad) {
        LocalDateTime fin = actividad.getFechaHoraFin();
        return fin != null && fin.isBefore(ahora);
    }

    private LocalDateTime fechaReferenciaContrato(Contrato contrato) {
        return contrato.getFechaEmision() != null
                ? contrato.getFechaEmision()
                : contrato.getFechaCreacion();
    }

    private List<FirmaContrato> firmasConsideradas() {
        List<FirmaContrato> firmas = new ArrayList<>();
        for (Contrato contrato : contratosConsiderados()) {
            firmas.addAll(firmasPorContrato.getOrDefault(contrato.getIdContrato(), List.of()));
        }
        return firmas;
    }

    private LocalDateTime fechas(Collection<LocalDateTime>... listas) {
        List<LocalDateTime> todas = new ArrayList<>();
        for (Collection<LocalDateTime> lista : listas) {
            if (lista != null) {
                todas.addAll(lista);
            }
        }
        return todas.stream()
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    /**
     * Actividades del cronograma en estado pendiente o en curso, ordenadas por
     * inicio, para el bloque de próximas actividades.
     */
    List<ActividadCronogramaResponse> proximasActividades(int limite) {
        if (cronograma == null || cronograma.actividades() == null) {
            return List.of();
        }
        return cronograma.actividades().stream()
                .filter(actividad -> EstadoActividad.PENDIENTE.name().equals(actividad.estado())
                        || EstadoActividad.EN_CURSO.name().equals(actividad.estado()))
                .filter(actividad -> {
                    LocalDateTime inicio = inicioActividad(actividad);
                    return inicio == null || !inicio.isBefore(ahora);
                })
                .sorted((a, b) -> {
                    LocalDateTime inicioA = inicioActividad(a);
                    LocalDateTime inicioB = inicioActividad(b);
                    if (inicioA == null && inicioB == null) {
                        return 0;
                    }
                    if (inicioA == null) {
                        return 1;
                    }
                    if (inicioB == null) {
                        return -1;
                    }
                    return inicioA.compareTo(inicioB);
                })
                .limit(limite)
                .toList();
    }

    private LocalDateTime inicioActividad(ActividadCronogramaResponse actividad) {
        return actividad.inicioPersistido() != null
                ? actividad.inicioPersistido()
                : actividad.inicioProyectado();
    }
}
