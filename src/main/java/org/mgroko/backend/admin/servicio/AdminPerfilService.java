package org.mgroko.backend.admin.servicio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.dto.AdminPerfilDetalleResponse;
import org.mgroko.backend.admin.dto.AdminPerfilResumenResponse;
import org.mgroko.backend.admin.dto.BuscarPerfilesAdminFiltro;
import org.mgroko.backend.admin.especificacion.AdminPerfilSpecifications;
import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.admin.mapper.AdminPerfilDetalleMapper;
import org.mgroko.backend.admin.mapper.AdminPerfilMapper;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.perfiles.servicio.GestionTerminosCondicionesService;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas administrativas de perfiles. Solo lectura.
 */
@Service
@Transactional(readOnly = true)
public class AdminPerfilService {

    private static final int TAMANO_RELACIONADOS = 20;

    private final PerfilRepository perfilRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ContratoRepository contratoRepository;
    private final FirmaContratoRepository firmaContratoRepository;
    private final PublicacionRepository publicacionRepository;
    private final GestionTerminosCondicionesService gestionTerminosCondicionesService;
    private final ConfiguracionSistemaService configuracionSistemaService;

    public AdminPerfilService(
            PerfilRepository perfilRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ContratoRepository contratoRepository,
            FirmaContratoRepository firmaContratoRepository,
            PublicacionRepository publicacionRepository,
            GestionTerminosCondicionesService gestionTerminosCondicionesService,
            ConfiguracionSistemaService configuracionSistemaService) {
        this.perfilRepository = perfilRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.contratoRepository = contratoRepository;
        this.firmaContratoRepository = firmaContratoRepository;
        this.publicacionRepository = publicacionRepository;
        this.gestionTerminosCondicionesService = gestionTerminosCondicionesService;
        this.configuracionSistemaService = configuracionSistemaService;
    }

    public PaginaResponse<AdminPerfilResumenResponse> buscar(
            BuscarPerfilesAdminFiltro filtro,
            int page,
            int size,
            boolean todos) {

        Sort sort = Sort.by(
                Sort.Order.asc("usuario.apellido"),
                Sort.Order.asc("usuario.nombre"),
                Sort.Order.asc("idPerfil"));
        Pageable pageable = todos || size <= 0
                ? Pageable.unpaged(sort)
                : PageRequest.of(Math.max(0, page), size, sort);

        Page<Perfil> resultado = perfilRepository.findAll(
                AdminPerfilSpecifications.conFiltros(filtro), pageable);

        return PaginaResponse.fromPage(resultado.map(AdminPerfilMapper::toResumen));
    }

    public AdminPerfilDetalleResponse obtenerDetalle(Long idPerfil) {
        Perfil perfil = perfilRepository.findByIdConDetalleAdmin(idPerfil)
                .orElseThrow(() -> new PerfilNoEncontradoException("Perfil no encontrado."));

        List<MiembroProyecto> membresias =
                miembroProyectoRepository.findTop20ByPerfilIdPerfilOrderByFechaAltaDesc(idPerfil);

        List<AdminPerfilDetalleResponse.ProyectoRelacionado> proyectos = membresias.stream()
                .map(this::toProyectoRelacionado)
                .toList();

        List<Contrato> contratosEntidad = contratoRepository.findByIdPerfilComoParte(
                idPerfil, PageRequest.of(0, TAMANO_RELACIONADOS));

        Map<Long, List<FirmaContrato>> firmasPorContrato = contratosEntidad.isEmpty()
                ? Map.of()
                : firmaContratoRepository.findConPartesPorContratos(
                        contratosEntidad.stream().map(Contrato::getIdContrato).toList())
                        .stream()
                        .collect(Collectors.groupingBy(FirmaContrato::getIdContrato));

        List<AdminPerfilDetalleResponse.ContratoRelacionado> contratos =
                contratosEntidad.stream()
                        .map(c -> toContratoRelacionado(
                                c, firmasPorContrato.getOrDefault(c.getIdContrato(), List.of()), idPerfil))
                        .toList();

        List<Publicacion> publicacionesEntidad = publicacionRepository.findRelacionadasAdmin(
                idPerfil, PageRequest.of(0, TAMANO_RELACIONADOS));

        List<AdminPerfilDetalleResponse.PublicacionRelacionada> publicaciones =
                publicacionesEntidad.stream()
                        .map(this::toPublicacionRelacionada)
                        .toList();

        return AdminPerfilDetalleMapper.toDetalle(
                perfil,
                gestionTerminosCondicionesService.obtenerParaPerfil(perfil),
                configuracionSistemaService.obtenerDiasBaja(),
                proyectos,
                miembroProyectoRepository.countByPerfilIdPerfil(idPerfil),
                contratos,
                contratoRepository.countByIdPerfilComoParte(idPerfil),
                publicaciones,
                publicacionRepository.countRelacionadasAdmin(idPerfil));
    }

    private AdminPerfilDetalleResponse.ProyectoRelacionado toProyectoRelacionado(
            MiembroProyecto membresia) {
        return new AdminPerfilDetalleResponse.ProyectoRelacionado(
                membresia.getProyecto() != null
                        ? membresia.getProyecto().getIdProyecto()
                        : null,
                membresia.getProyecto() != null
                        ? membresia.getProyecto().getNombre()
                        : null,
                membresia.getProyecto() != null && membresia.getProyecto().getEstado() != null
                        ? membresia.getProyecto().getEstado().name()
                        : null,
                membresia.getRolProyecto() != null
                        ? membresia.getRolProyecto().getNombre()
                        : null,
                membresia.getEstadoParticipacion() != null
                        ? membresia.getEstadoParticipacion().name()
                        : null,
                membresia.getFechaAlta(),
                membresia.getFechaBaja());
    }

    private AdminPerfilDetalleResponse.ContratoRelacionado toContratoRelacionado(
            Contrato contrato,
            List<FirmaContrato> firmas,
            Long idPerfil) {

        FirmaContrato miFirma = firmas.stream()
                .filter(f -> f.getMiembro() != null
                        && f.getMiembro().getPerfil() != null
                        && idPerfil.equals(f.getMiembro().getPerfil().getIdPerfil()))
                .findFirst()
                .orElse(null);

        return new AdminPerfilDetalleResponse.ContratoRelacionado(
                contrato.getIdContrato(),
                contrato.getNombre(),
                contrato.getEstado() != null ? contrato.getEstado().name() : null,
                contrato.getProyecto() != null
                        ? contrato.getProyecto().getIdProyecto()
                        : null,
                contrato.getProyecto() != null
                        ? contrato.getProyecto().getNombre()
                        : null,
                miFirma != null && miFirma.getRolParte() != null
                        ? miFirma.getRolParte().name()
                        : null,
                miFirma != null && miFirma.getEstadoFirma() != null
                        ? miFirma.getEstadoFirma().name()
                        : null,
                contrato.getFechaEmision());
    }

    private AdminPerfilDetalleResponse.PublicacionRelacionada toPublicacionRelacionada(
            Publicacion publicacion) {
        return new AdminPerfilDetalleResponse.PublicacionRelacionada(
                publicacion.getIdPublicacion(),
                publicacion.getTitulo(),
                publicacion.getEstado() != null ? publicacion.getEstado().name() : null,
                publicacion.getFechaPublicacion(),
                publicacion.getProyecto() != null
                        ? publicacion.getProyecto().getIdProyecto()
                        : null,
                publicacion.getProyecto() != null
                        ? publicacion.getProyecto().getNombre()
                        : null);
    }
}
