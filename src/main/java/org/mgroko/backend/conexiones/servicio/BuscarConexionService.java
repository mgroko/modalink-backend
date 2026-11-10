package org.mgroko.backend.conexiones.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.conexiones.dto.BuscarConexionesFiltro;
import org.mgroko.backend.conexiones.dto.ConexionResponse;
import org.mgroko.backend.conexiones.dto.EstadoPresenciaConexion;
import org.mgroko.backend.conexiones.dto.ProfesionConexionResponse;
import org.mgroko.backend.conexiones.dto.ResumenConexionesResponse;
import org.mgroko.backend.conexiones.especificacion.ConexionSpecifications;
import org.mgroko.backend.conexiones.mapper.SolicitudConexionMapper;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.presencia.servicio.PresenciaService;
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
    private final PresenciaService presenciaService;

    public BuscarConexionService(
            PerfilActivoService perfilActivoService,
            ConexionRepository conexionRepository,
            PresenciaService presenciaService) {
        this.perfilActivoService = perfilActivoService;
        this.conexionRepository = conexionRepository;
        this.presenciaService = presenciaService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ConexionResponse> buscar(
            Long idUsuario,
            Long idPerfilActivo,
            BuscarConexionesFiltro filtro,
            int page,
            int size) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        LocalDateTime ahora = presenciaService.ahora();
        LocalDateTime limite = presenciaService.limite(ahora);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizarTamano(size),
                Sort.by(
                        Sort.Order.desc("fechaInicioConexion"),
                        Sort.Order.desc("idConexion")));

        Page<Conexion> pagina = conexionRepository.findAll(
                ConexionSpecifications.conFiltros(
                        perfilActivo.getIdPerfil(), filtro, limite),
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
                .map(conexion -> toResponse(
                        conexion, perfilActivo.getIdPerfil(), ahora))
                .toList();

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pageable, pagina.getTotalElements()));
    }

    /**
     * Resumen de la red para el hero y los chips de profesión del mockup.
     */
    @Transactional(readOnly = true)
    public ResumenConexionesResponse resumen(
            Long idUsuario,
            Long idPerfilActivo) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        LocalDateTime ahora = presenciaService.ahora();
        LocalDateTime limite = presenciaService.limite(ahora);

        long totalConexiones = conexionRepository.count(
                ConexionSpecifications.conFiltros(
                        perfilActivo.getIdPerfil(), null, limite));

        long totalEnLinea = conexionRepository.count(
                ConexionSpecifications.conFiltros(
                        perfilActivo.getIdPerfil(),
                        new BuscarConexionesFiltro(
                                null, null, null, null, null,
                                EstadoPresenciaConexion.EN_LINEA),
                        limite));

        List<ProfesionConexionResponse> profesiones = conexionRepository
                .contarConexionesPorProfesion(perfilActivo.getIdPerfil())
                .stream()
                .map(fila -> new ProfesionConexionResponse(
                        fila.getIdProfesion(),
                        fila.getNombre(),
                        fila.getCantidad() != null ? fila.getCantidad() : 0L))
                .toList();

        return new ResumenConexionesResponse(
                totalConexiones, totalEnLinea, profesiones);
    }

    private ConexionResponse toResponse(
            Conexion conexion,
            Long idPerfilActivo,
            LocalDateTime ahora) {

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
                SolicitudConexionMapper.toPerfilResponse(contraparte),
                presenciaService.estaEnLinea(contraparte, ahora));
    }

    private int normalizarTamano(int size) {
        if (size <= 0) {
            return TAMANO_PAGINA_POR_DEFECTO;
        }
        return Math.min(size, TAMANO_PAGINA_MAXIMO);
    }
}
