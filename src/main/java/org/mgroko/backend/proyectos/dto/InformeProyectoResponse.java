package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

/**
 * Informe ejecutivo de estado y salud de un proyecto.
 *
 * <p>Es una foto del momento, no un reemplazo de la auditoría. El estado de
 * cada área se deriva de sus alertas; no existe un puntaje agregado.</p>
 */
public record InformeProyectoResponse(
        ProyectoInforme proyecto,
        LocalDateTime generadoEn,
        LocalDateTime impresoEn,
        GeneradoPorInforme generadoPor,
        String modo,
        String versionInforme,
        ResumenInforme resumen,
        MetricasInforme metricas,
        MoodboardInforme moodboard,
        EquipoInforme equipo,
        CronogramaInforme cronograma,
        ContratosInforme contratos,
        List<AlertaInforme> alertas,
        List<HistorialInforme> historial,
        CalificacionesInforme calificaciones) {

    public InformeProyectoResponse {
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        historial = historial == null ? List.of() : List.copyOf(historial);
    }

    public record ProyectoInforme(
            Long idProyecto,
            String nombre,
            String descripcion,
            String estado,
            String privacidad,
            LocalDate fechaInicio,
            LocalDate fechaEntrega,
            LocalDateTime fechaFinalizacion,
            LocalDateTime fechaCancelacion,
            Boolean finalizacionPrematura,
            UbicacionResponse ubicacion,
            List<ObjetivoResponse> objetivos) {

        public ProyectoInforme {
            objetivos = objetivos == null ? List.of() : List.copyOf(objetivos);
        }
    }

    public record GeneradoPorInforme(
            Long idPerfil,
            String nombreArtistico,
            String rol) {
    }

    public record ResumenInforme(
            String estadoGeneral,
            List<AreaInforme> areas,
            RiesgoEntregaInforme riesgoEntrega,
            List<DecisionInforme> decisiones) {

        public ResumenInforme {
            areas = areas == null ? List.of() : List.copyOf(areas);
            decisiones = decisiones == null ? List.of() : List.copyOf(decisiones);
        }
    }

    public record AreaInforme(
            String codigo,
            EstadoAreaInforme estado,
            String motivo) {
    }

    public record RiesgoEntregaInforme(
            LocalDate fechaFinProyectada,
            LocalDate fechaEntrega,
            Integer margenDias,
            Integer criticasSinIniciar,
            Integer vencidas,
            Integer conflictos,
            Double avanceActividades,
            boolean sinDatos) {
    }

    public record DecisionInforme(
            String prioridad,
            String codigo,
            String mensaje,
            Long antiguedadDias,
            String accionSugerida) {
    }

    public record MetricasInforme(
            Double avanceActividades,
            Double avancePonderado,
            Integer margenDias,
            Long margenMinutos,
            Double coberturaCupos,
            Double estabilidadEquipo,
            Double preparacionContractual,
            Double completitudBrief,
            Integer actividadesVencidas,
            Integer postulacionesPendientes,
            Integer invitacionesPendientes,
            Integer contratosPendientesFirma,
            Double velocidadAproximada) {
    }

    public record MoodboardInforme(
            Long idMoodboard,
            String descripcion,
            LocalDateTime fechaCreacion,
            Integer totalImagenes,
            Integer limiteConfigurado,
            List<ImagenInforme> imagenes) {

        public MoodboardInforme {
            imagenes = imagenes == null ? List.of() : List.copyOf(imagenes);
        }
    }

    public record ImagenInforme(
            Long idImagen,
            String url) {
    }

    public record EquipoInforme(
            Integer totalActivos,
            Integer totalDirectores,
            Integer totalBajas,
            Double estabilidad,
            List<MiembroInforme> miembros,
            List<MiembroInforme> miembrosHistoricos,
            List<RequerimientoCoberturaInforme> requerimientos) {

        public EquipoInforme {
            miembros = miembros == null ? List.of() : List.copyOf(miembros);
            miembrosHistoricos = miembrosHistoricos == null
                    ? List.of()
                    : List.copyOf(miembrosHistoricos);
            requerimientos = requerimientos == null
                    ? List.of()
                    : List.copyOf(requerimientos);
        }
    }

    public record MiembroInforme(
            Long idMiembro,
            Long idPerfil,
            String nombreArtistico,
            String profesion,
            String rol,
            String estadoParticipacion,
            LocalDateTime fechaAlta,
            LocalDateTime fechaBaja,
            String motivoBaja,
            Boolean tycAceptado) {
    }

    public record RequerimientoCoberturaInforme(
            Long idRequerimientoGral,
            String profesion,
            String descripcion,
            Integer cantidad,
            Long cubiertos,
            Integer faltantes,
            Double porcentajeCobertura) {
    }

    public record CronogramaInforme(
            LocalDateTime fechaInicio,
            LocalDateTime fechaFinProyectada,
            LocalDate fechaEntrega,
            Long margenMinutos,
            Integer margenDias,
            Boolean viable,
            String estadoCronograma,
            Integer totalActividades,
            Integer actividadesCriticas,
            Integer actividadesVencidas,
            Integer conflictos,
            Map<String, Integer> porEstado,
            List<ActividadProximaInforme> proximasActividades) {

        public CronogramaInforme {
            porEstado = porEstado == null ? Map.of() : Map.copyOf(porEstado);
            proximasActividades = proximasActividades == null
                    ? List.of()
                    : List.copyOf(proximasActividades);
        }
    }

    public record ActividadProximaInforme(
            Long idActividad,
            String nombre,
            LocalDateTime fechaHoraInicio,
            String estado,
            Boolean enRutaCritica) {
    }

    public record ContratosInforme(
            Integer total,
            Map<String, Integer> porEstado,
            FirmasInforme firmas,
            List<ContratoInforme> contratos) {

        public ContratosInforme {
            porEstado = porEstado == null ? Map.of() : Map.copyOf(porEstado);
            contratos = contratos == null ? List.of() : List.copyOf(contratos);
        }
    }

    public record FirmasInforme(
            Integer firmadas,
            Integer pendientes,
            Integer total) {
    }

    public record ContratoInforme(
            Long idContrato,
            String nombre,
            Integer version,
            String estado,
            Integer firmadas,
            Integer totalPartes) {
    }

    public record HistorialInforme(
            LocalDateTime fecha,
            String tipo,
            String perfil,
            String motivo) {
    }

    public record CalificacionesInforme(
            Boolean disponible,
            Double promedio,
            Long cantidad,
            Long totalEsperado) {
    }
}
