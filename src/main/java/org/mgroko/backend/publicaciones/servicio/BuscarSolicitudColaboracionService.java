package org.mgroko.backend.publicaciones.servicio;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.publicaciones.dto.BuscarSolicitudesColaboracionFiltro;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionBusquedaResponse;
import org.mgroko.backend.publicaciones.especificacion.SolicitudColaboracionSpecifications;
import org.mgroko.backend.publicaciones.exception.FiltroSolicitudColaboracionInvalidoException;
import org.mgroko.backend.publicaciones.exception.RangoFechasSolicitudColaboracionInvalidoException;
import org.mgroko.backend.publicaciones.mapper.SolicitudColaboracionMapper;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-65 - Buscar solicitudes de colaboración del perfil activo.
 */
@Service
public class BuscarSolicitudColaboracionService {

    private static final int TAMANO_PAGINA_POR_DEFECTO = 20;
    private static final int TAMANO_PAGINA_MAXIMO = 100;

    private final SolicitudColaboracionRepository solicitudColaboracionRepository;

    public BuscarSolicitudColaboracionService(
            SolicitudColaboracionRepository solicitudColaboracionRepository) {
        this.solicitudColaboracionRepository = solicitudColaboracionRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<SolicitudColaboracionBusquedaResponse> buscar(
            Long idPerfilActivo,
            BuscarSolicitudesColaboracionFiltro filtro,
            int page,
            int size) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        validar(filtro);
        BuscarSolicitudesColaboracionFiltro filtroNormalizado = normalizar(filtro);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizarTamano(size),
                Sort.by(
                        Sort.Order.desc("fechaEnvio"),
                        Sort.Order.desc("idSolicitudColaboracion")));

        Page<SolicitudColaboracion> pagina = solicitudColaboracionRepository.findAll(
                SolicitudColaboracionSpecifications.conFiltros(
                        idPerfilActivo, filtroNormalizado),
                pageable);

        List<Long> ids = pagina.getContent().stream()
                .map(SolicitudColaboracion::getIdSolicitudColaboracion)
                .toList();

        if (ids.isEmpty()) {
            return PaginaResponse.fromPage(
                    new PageImpl<>(List.of(), pageable, pagina.getTotalElements()));
        }

        Map<Long, SolicitudColaboracion> porId = solicitudColaboracionRepository
                .findAllByIdInWithDetalle(ids)
                .stream()
                .collect(Collectors.toMap(
                        SolicitudColaboracion::getIdSolicitudColaboracion,
                        solicitud -> solicitud));

        List<SolicitudColaboracionBusquedaResponse> contenido = ids.stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .map(solicitud -> SolicitudColaboracionMapper.toBusquedaResponse(
                        solicitud, idPerfilActivo))
                .toList();

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pageable, pagina.getTotalElements()));
    }

    private void validar(BuscarSolicitudesColaboracionFiltro filtro) {
        if (filtro == null) {
            return;
        }

        if (Boolean.TRUE.equals(filtro.todosLosEstados())
                && filtro.estado() != null) {
            throw new FiltroSolicitudColaboracionInvalidoException(
                    "No se puede combinar todosLosEstados con un estado específico.");
        }

        if (filtro.fechaDesde() != null && filtro.fechaHasta() != null
                && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new RangoFechasSolicitudColaboracionInvalidoException();
        }

        if ((filtro.idPerfilEmisor() != null && filtro.idPerfilEmisor() <= 0)
                || (filtro.idPerfilReceptor() != null && filtro.idPerfilReceptor() <= 0)
                || (filtro.idPublicacion() != null && filtro.idPublicacion() <= 0)) {
            throw new FiltroSolicitudColaboracionInvalidoException(
                    "Los identificadores de filtro deben ser positivos.");
        }
    }

    private BuscarSolicitudesColaboracionFiltro normalizar(
            BuscarSolicitudesColaboracionFiltro filtro) {

        if (filtro == null) {
            return new BuscarSolicitudesColaboracionFiltro(
                    null, null, null,
                    EstadoSolicitud.PENDIENTE,
                    false, null, null, null);
        }

        EstadoSolicitud estado = Boolean.TRUE.equals(filtro.todosLosEstados())
                ? null
                : (filtro.estado() != null
                        ? filtro.estado()
                        : EstadoSolicitud.PENDIENTE);

        return new BuscarSolicitudesColaboracionFiltro(
                filtro.direccion(),
                filtro.idPerfilEmisor(),
                filtro.idPerfilReceptor(),
                estado,
                filtro.todosLosEstados(),
                filtro.fechaDesde(),
                filtro.fechaHasta(),
                filtro.idPublicacion());
    }

    private int normalizarTamano(int size) {
        if (size <= 0) {
            return TAMANO_PAGINA_POR_DEFECTO;
        }
        return Math.min(size, TAMANO_PAGINA_MAXIMO);
    }
}
