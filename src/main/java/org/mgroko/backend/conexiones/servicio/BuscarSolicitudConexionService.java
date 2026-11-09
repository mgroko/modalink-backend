package org.mgroko.backend.conexiones.servicio;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.conexiones.dto.BuscarSolicitudesConexionFiltro;
import org.mgroko.backend.conexiones.dto.SolicitudConexionBusquedaResponse;
import org.mgroko.backend.conexiones.especificacion.SolicitudConexionSpecifications;
import org.mgroko.backend.conexiones.exception.RangoFechasSolicitudConexionInvalidoException;
import org.mgroko.backend.conexiones.mapper.SolicitudConexionMapper;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-46 - Buscar solicitudes de conexión del perfil activo.
 */
@Service
public class BuscarSolicitudConexionService {

    private static final int TAMANO_PAGINA_POR_DEFECTO = 20;
    private static final int TAMANO_PAGINA_MAXIMO = 100;

    private final SolicitudConexionRepository solicitudConexionRepository;

    public BuscarSolicitudConexionService(
            SolicitudConexionRepository solicitudConexionRepository) {
        this.solicitudConexionRepository = solicitudConexionRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<SolicitudConexionBusquedaResponse> buscar(
            Long idPerfilActivo,
            BuscarSolicitudesConexionFiltro filtro,
            int page,
            int size) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        validarFechas(filtro);
        BuscarSolicitudesConexionFiltro filtroNormalizado = normalizar(filtro);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizarTamano(size),
                Sort.by(
                        Sort.Order.desc("fechaEnvio"),
                        Sort.Order.desc("idSolicitudConexion")));

        Page<SolicitudConexion> pagina = solicitudConexionRepository.findAll(
                SolicitudConexionSpecifications.conFiltros(
                        idPerfilActivo, filtroNormalizado),
                pageable);

        List<Long> ids = pagina.getContent().stream()
                .map(SolicitudConexion::getIdSolicitudConexion)
                .toList();

        if (ids.isEmpty()) {
            return PaginaResponse.fromPage(
                    new PageImpl<>(List.of(), pageable, pagina.getTotalElements()));
        }

        Map<Long, SolicitudConexion> porId = solicitudConexionRepository
                .findAllByIdInWithPerfiles(ids)
                .stream()
                .collect(Collectors.toMap(
                        SolicitudConexion::getIdSolicitudConexion,
                        solicitud -> solicitud));

        List<SolicitudConexionBusquedaResponse> contenido = ids.stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .map(solicitud -> SolicitudConexionMapper.toBusquedaResponse(
                        solicitud, idPerfilActivo))
                .toList();

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pageable, pagina.getTotalElements()));
    }

    private void validarFechas(BuscarSolicitudesConexionFiltro filtro) {
        if (filtro == null || filtro.fechaDesde() == null || filtro.fechaHasta() == null) {
            return;
        }
        if (filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new RangoFechasSolicitudConexionInvalidoException();
        }
    }

    private BuscarSolicitudesConexionFiltro normalizar(
            BuscarSolicitudesConexionFiltro filtro) {

        if (filtro == null) {
            return new BuscarSolicitudesConexionFiltro(
                    null,
                    EstadoSolicitud.PENDIENTE,
                    null, null, null, null, null, null);
        }

        return new BuscarSolicitudesConexionFiltro(
                filtro.direccion(),
                filtro.estado() != null ? filtro.estado() : EstadoSolicitud.PENDIENTE,
                filtro.nombre(),
                filtro.apellido(),
                filtro.nombreArtistico(),
                filtro.idProfesion(),
                filtro.fechaDesde(),
                filtro.fechaHasta());
    }

    private int normalizarTamano(int size) {
        if (size <= 0) {
            return TAMANO_PAGINA_POR_DEFECTO;
        }
        return Math.min(size, TAMANO_PAGINA_MAXIMO);
    }
}
