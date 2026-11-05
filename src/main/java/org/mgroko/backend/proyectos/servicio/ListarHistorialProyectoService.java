package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.TipoEventoHistorial;
import org.mgroko.backend.proyectos.dto.ActividadHistorialResponse;
import org.mgroko.backend.proyectos.dto.AuditoriaBajaResponse;
import org.mgroko.backend.proyectos.dto.AutorBajaResponse;
import org.mgroko.backend.proyectos.dto.HistorialProyectoEventoResponse;
import org.mgroko.backend.proyectos.exception.HistorialParametroInvalidoException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historial de membresías terminadas de un proyecto.
 *
 * <p>Reutiliza la auditoría de bajas existente. Solo expone los estados
 * {@code ELIMINADO} y {@code BAJA_VOLUNTARIA}, que son los dos únicos estados
 * terminales de una membresía.</p>
 */
@Service
public class ListarHistorialProyectoService {

    private static final int TAMANO_MAXIMO = 100;

    private final ListarAuditoriaBajasProyectoService auditoriaBajasService;

    public ListarHistorialProyectoService(
            ListarAuditoriaBajasProyectoService auditoriaBajasService) {
        this.auditoriaBajasService = auditoriaBajasService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<HistorialProyectoEventoResponse> listar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            String tipo,
            String nombre,
            int page,
            int size) {

        TipoEventoHistorial tipoEvento = validarParametros(tipo, page, size);
        String nombreNormalizado = normalizarNombre(nombre);

        PaginaResponse<AuditoriaBajaResponse> auditoria =
                auditoriaBajasService.listar(
                        idUsuario,
                        idPerfilActivo,
                        idProyecto,
                        tipoEvento != null
                                ? tipoEvento.getEstadoParticipacion().name()
                                : null,
                        null,
                        null,
                        null,
                        0,
                        size,
                        true);

        List<HistorialProyectoEventoResponse> eventos = auditoria.contenido()
                .stream()
                .filter(evento -> coincideNombre(evento, nombreNormalizado))
                .map(this::toResponse)
                .sorted(comparadorHistorial())
                .toList();

        return paginar(eventos, page, size);
    }

    private TipoEventoHistorial validarParametros(
            String tipo,
            int page,
            int size) {

        if (page < 0) {
            throw new HistorialParametroInvalidoException(
                    "El parámetro 'page' no puede ser negativo.");
        }
        if (size < 1 || size > TAMANO_MAXIMO) {
            throw new HistorialParametroInvalidoException(
                    "El parámetro 'size' debe estar entre 1 y "
                            + TAMANO_MAXIMO + ".");
        }
        try {
            return TipoEventoHistorial.desde(tipo);
        } catch (IllegalArgumentException ex) {
            throw new HistorialParametroInvalidoException(ex.getMessage());
        }
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        return nombre.trim().toLowerCase(Locale.ROOT);
    }

    private boolean coincideNombre(AuditoriaBajaResponse evento, String nombre) {
        if (nombre == null) {
            return true;
        }

        return contiene(evento.nombreArtistico(), nombre)
                || contiene(evento.nombreUsuario(), nombre)
                || contiene(evento.apellidoUsuario(), nombre);
    }

    private boolean contiene(String valor, String nombreNormalizado) {
        return valor != null
                && valor.toLowerCase(Locale.ROOT).contains(nombreNormalizado);
    }

    private Comparator<HistorialProyectoEventoResponse> comparadorHistorial() {
        Comparator<HistorialProyectoEventoResponse> porFecha =
                Comparator.comparing(
                        HistorialProyectoEventoResponse::fecha,
                        Comparator.nullsLast(Comparator.reverseOrder()));

        return porFecha.thenComparing(
                HistorialProyectoEventoResponse::idEvento,
                Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private PaginaResponse<HistorialProyectoEventoResponse> paginar(
            List<HistorialProyectoEventoResponse> eventos,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);
        int desde = (int) pageable.getOffset();

        List<HistorialProyectoEventoResponse> contenido;
        if (desde >= eventos.size()) {
            contenido = List.of();
        } else {
            int hasta = Math.min(desde + size, eventos.size());
            contenido = new ArrayList<>(eventos.subList(desde, hasta));
        }

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pageable, eventos.size()));
    }

    private HistorialProyectoEventoResponse toResponse(
            AuditoriaBajaResponse evento) {

        AutorBajaResponse autor = evento.autorBaja();
        ActividadHistorialResponse actividad = primeraActividad(evento);

        return new HistorialProyectoEventoResponse(
                evento.idMiembro(),
                tipoDesdeEstado(evento.estadoParticipacion()),
                fechaEfectiva(evento),
                evento.idPerfil(),
                evento.nombreArtistico(),
                evento.nombreUsuario(),
                evento.apellidoUsuario(),
                evento.nombreProfesion(),
                evento.rol(),
                evento.idUsuario(),
                autor != null ? autor.idPerfil() : null,
                autor != null ? autor.nombreArtistico() : null,
                autor != null ? autor.nombre() : null,
                autor != null ? autor.apellido() : null,
                autor != null ? autor.idUsuario() : null,
                evento.motivoBaja(),
                evento.estadoParticipacion(),
                actividad != null ? actividad.idActividad() : null,
                actividad != null ? actividad.nombreActividad() : null,
                evento.actividades() != null
                        ? evento.actividades()
                        : List.of());
    }

    private ActividadHistorialResponse primeraActividad(
            AuditoriaBajaResponse evento) {
        if (evento.actividades() == null || evento.actividades().isEmpty()) {
            return null;
        }
        return evento.actividades().get(0);
    }

    private String tipoDesdeEstado(String estadoParticipacion) {
        if (estadoParticipacion == null) {
            return null;
        }
        return switch (estadoParticipacion.toUpperCase(Locale.ROOT)) {
            case "ELIMINADO" -> TipoEventoHistorial.MIEMBRO_ELIMINADO.name();
            case "BAJA_VOLUNTARIA" ->
                TipoEventoHistorial.MIEMBRO_BAJA_VOLUNTARIA.name();
            default -> null;
        };
    }

    private LocalDateTime fechaEfectiva(AuditoriaBajaResponse evento) {
        return evento.fechaBaja() != null
                ? evento.fechaBaja()
                : evento.fechaAlta();
    }
}
