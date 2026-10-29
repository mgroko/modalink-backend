package org.mgroko.backend.postulaciones.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.postulaciones.dto.MiPostulacionResponse;
import org.mgroko.backend.postulaciones.dto.MisPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-27: listado de las postulaciones emitidas por el perfil activo.
 *
 * <p>El frontend lo usa para identificar el proyecto, el requerimiento y el
 * identificador de la postulación antes de confirmar la baja. Devuelve en una
 * sola página tanto postulaciones generales como de actividad.</p>
 */
@Service
public class BuscarMisPostulacionesService {

    private final PostulacionGralRepository postulacionGralRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;

    public BuscarMisPostulacionesService(
            PostulacionGralRepository postulacionGralRepository,
            PostulacionActividadRepository postulacionActividadRepository) {
        this.postulacionGralRepository = postulacionGralRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MiPostulacionResponse> buscar(
            Long idPerfilActivo,
            MisPostulacionesFiltro filtro,
            int page,
            int size,
            boolean todos) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        List<MiPostulacionResponse> postulaciones = new ArrayList<>();
        postulacionGralRepository.findByPerfil_IdPerfil(idPerfilActivo)
                .forEach(postulacion -> postulaciones.add(aRespuesta(postulacion)));
        postulacionActividadRepository.findByPerfil_IdPerfil(idPerfilActivo)
                .forEach(postulacion -> postulaciones.add(aRespuesta(postulacion)));

        List<MiPostulacionResponse> filtradas = postulaciones.stream()
                .filter(postulacion -> coincideProyecto(postulacion, filtro))
                .filter(postulacion -> coincideEstado(postulacion, filtro))
                .filter(postulacion -> coincideTipo(postulacion, filtro))
                .sorted(Comparator.comparing(
                        MiPostulacionResponse::fechaPostulacion,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .toList();

        if (todos || size <= 0) {
            return PaginaResponse.fromPage(new PageImpl<>(filtradas));
        }

        PageRequest pageable = PageRequest.of(Math.max(0, page), size);
        int desde = (int) Math.min(pageable.getOffset(), filtradas.size());
        int hasta = Math.min(desde + size, filtradas.size());

        return PaginaResponse.fromPage(new PageImpl<>(
                filtradas.subList(desde, hasta), pageable, filtradas.size()));
    }

    private boolean coincideProyecto(
            MiPostulacionResponse postulacion,
            MisPostulacionesFiltro filtro) {
        return filtro == null || filtro.idProyecto() == null
                || filtro.idProyecto().equals(postulacion.idProyecto());
    }

    private boolean coincideEstado(
            MiPostulacionResponse postulacion,
            MisPostulacionesFiltro filtro) {
        return filtro == null || filtro.estado() == null
                || filtro.estado() == postulacion.estado();
    }

    private boolean coincideTipo(
            MiPostulacionResponse postulacion,
            MisPostulacionesFiltro filtro) {
        return filtro == null || filtro.tipo() == null
                || filtro.tipo() == postulacion.tipo();
    }

    private MiPostulacionResponse aRespuesta(PostulacionGral postulacion) {
        var requerimiento = postulacion.getRequerimientoGral();
        var proyecto = requerimiento.getProyecto();

        return new MiPostulacionResponse(
                postulacion.getIdPostulacionGral(),
                TipoPostulacion.GENERAL,
                postulacion.getEstado(),
                postulacion.getFechaPostulacion(),
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                requerimiento.getIdRequerimientoGral(),
                requerimiento.getDescripcion(),
                null,
                null,
                postulacion.getEstado() == EstadoSolicitud.PENDIENTE);
    }

    private MiPostulacionResponse aRespuesta(PostulacionActividad postulacion) {
        var requerimiento = postulacion.getRequerimiento();
        var actividad = requerimiento.getActividad();
        var proyecto = actividad.getPlanificacion().getProyecto();

        return new MiPostulacionResponse(
                postulacion.getIdPostulacion(),
                TipoPostulacion.ACTIVIDAD,
                postulacion.getEstado(),
                postulacion.getFechaPostulacion(),
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                requerimiento.getIdRequerimiento(),
                requerimiento.getDescripcion(),
                actividad.getIdActividad(),
                actividad.getNombre(),
                postulacion.getEstado() == EstadoSolicitud.PENDIENTE);
    }
}
