package org.mgroko.backend.conexiones.servicio;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.conexiones.dto.BuscarConexionesFiltro;
import org.mgroko.backend.conexiones.dto.ConexionResponse;
import org.mgroko.backend.conexiones.especificacion.ConexionSpecifications;
import org.mgroko.backend.conexiones.mapper.SolicitudConexionMapper;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-44 bis - Listar la red de contactos activa del perfil activo.
 */
@Service
public class BuscarConexionService {

    private static final int TAMANO_PAGINA_POR_DEFECTO = 20;
    private static final int TAMANO_PAGINA_MAXIMO = 100;

    private final PerfilActivoService perfilActivoService;
    private final ConexionRepository conexionRepository;

    public BuscarConexionService(
            PerfilActivoService perfilActivoService,
            ConexionRepository conexionRepository) {
        this.perfilActivoService = perfilActivoService;
        this.conexionRepository = conexionRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ConexionResponse> buscar(
            Long idUsuario,
            Long idPerfilActivo,
            BuscarConexionesFiltro filtro,
            int page,
            int size) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizarTamano(size),
                Sort.by(
                        Sort.Order.desc("fechaInicioConexion"),
                        Sort.Order.desc("idConexion")));

        Page<Conexion> pagina = conexionRepository.findAll(
                ConexionSpecifications.conFiltros(
                        perfilActivo.getIdPerfil(), filtro),
                pageable);

        List<Long> ids = pagina.getContent().stream()
                .map(Conexion::getIdConexion)
                .toList();

        if (ids.isEmpty()) {
            return PaginaResponse.fromPage(
                    new PageImpl<>(List.of(), pageable, pagina.getTotalElements()));
        }

        Map<Long, Conexion> porId = conexionRepository
                .findAllByIdInWithPerfiles(ids)
                .stream()
                .collect(Collectors.toMap(
                        Conexion::getIdConexion,
                        conexion -> conexion));

        List<ConexionResponse> contenido = ids.stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .map(conexion -> toResponse(conexion, perfilActivo.getIdPerfil()))
                .toList();

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pageable, pagina.getTotalElements()));
    }

    private ConexionResponse toResponse(Conexion conexion, Long idPerfilActivo) {
        boolean activoEsPerfil1 = conexion.getPerfil1().getIdPerfil()
                .equals(idPerfilActivo);
        Perfil contraparte = activoEsPerfil1
                ? conexion.getPerfil2()
                : conexion.getPerfil1();

        return new ConexionResponse(
                conexion.getIdConexion(),
                conexion.getFechaInicioConexion(),
                conexion.getPerfil1().getIdPerfil(),
                conexion.getPerfil2().getIdPerfil(),
                SolicitudConexionMapper.toPerfilResponse(contraparte));
    }

    private int normalizarTamano(int size) {
        if (size <= 0) {
            return TAMANO_PAGINA_POR_DEFECTO;
        }
        return Math.min(size, TAMANO_PAGINA_MAXIMO);
    }
}
