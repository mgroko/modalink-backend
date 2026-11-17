package org.mgroko.backend.admin.servicio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.dto.AdminContratoDetalleResponse;
import org.mgroko.backend.admin.dto.AdminContratoResumenResponse;
import org.mgroko.backend.admin.dto.BuscarContratosAdminFiltro;
import org.mgroko.backend.admin.especificacion.AdminContratoSpecifications;
import org.mgroko.backend.admin.mapper.AdminContratoDetalleMapper;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.ContratoEventoResponse;
import org.mgroko.backend.contratos.exception.ContratoNoEncontradoException;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.repositorio.ContratoEventoRepository;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas administrativas de contratos. Solo lectura.
 */
@Service
@Transactional(readOnly = true)
public class AdminContratoService {

    private final ContratoRepository contratoRepository;
    private final FirmaContratoRepository firmaContratoRepository;
    private final ContratoEventoRepository contratoEventoRepository;

    public AdminContratoService(
            ContratoRepository contratoRepository,
            FirmaContratoRepository firmaContratoRepository,
            ContratoEventoRepository contratoEventoRepository) {
        this.contratoRepository = contratoRepository;
        this.firmaContratoRepository = firmaContratoRepository;
        this.contratoEventoRepository = contratoEventoRepository;
    }

    public PaginaResponse<AdminContratoResumenResponse> buscar(
            BuscarContratosAdminFiltro filtro,
            int page,
            int size,
            boolean todos) {

        Sort sort = Sort.by(
                Sort.Order.desc("fechaCreacion"),
                Sort.Order.desc("idContrato"));
        Pageable pageable = todos || size <= 0
                ? Pageable.unpaged(sort)
                : PageRequest.of(Math.max(0, page), size, sort);

        Page<Contrato> resultado = contratoRepository.findAll(
                AdminContratoSpecifications.conFiltros(filtro), pageable);

        Map<Long, List<FirmaContrato>> firmasPorContrato = cargarFirmas(resultado.getContent());

        return PaginaResponse.fromPage(resultado.map(c -> AdminContratoDetalleMapper.toResumen(
                c, firmasPorContrato.getOrDefault(c.getIdContrato(), List.of()))));
    }

    public AdminContratoDetalleResponse obtenerDetalle(Long idContrato) {
        Contrato contrato = contratoRepository.findByIdConPartes(idContrato)
                .orElseThrow(() -> new ContratoNoEncontradoException(idContrato));

        List<FirmaContrato> firmas = firmaContratoRepository.findConPartes(idContrato);

        return AdminContratoDetalleMapper.toDetalle(contrato, firmas);
    }

    public List<ContratoEventoResponse> listarEventos(Long idContrato) {
        if (!contratoRepository.existsById(idContrato)) {
            throw new ContratoNoEncontradoException(idContrato);
        }
        return contratoEventoRepository.findByContratoConDetalle(idContrato).stream()
                .map(evento -> new ContratoEventoResponse(
                        evento.getIdEvento(),
                        evento.getTipoEvento() != null
                                ? evento.getTipoEvento().name()
                                : null,
                        evento.getFechaHora(),
                        evento.getMiembro() != null ? evento.getMiembro().getIdMiembro() : null,
                        evento.getMiembro() != null && evento.getMiembro().getPerfil() != null
                                ? evento.getMiembro().getPerfil().getIdPerfil()
                                : null,
                        evento.getMiembro() != null && evento.getMiembro().getPerfil() != null
                                ? AdminContratoDetalleMapper.nombreCompleto(
                                        evento.getMiembro().getPerfil())
                                : null,
                        evento.getUsuario() != null ? evento.getUsuario().getIdUsuario() : null,
                        evento.getUsuario() != null
                                ? nombreCompletoUsuario(evento.getUsuario())
                                : null,
                        evento.getContratoVersion() != null
                                ? evento.getContratoVersion().getVersion()
                                : null,
                        evento.getMotivo()))
                .toList();
    }

    private Map<Long, List<FirmaContrato>> cargarFirmas(List<Contrato> contratos) {
        if (contratos.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = contratos.stream().map(Contrato::getIdContrato).toList();
        return firmaContratoRepository.findConPartesPorContratos(ids).stream()
                .collect(Collectors.groupingBy(FirmaContrato::getIdContrato));
    }

    private String nombreCompletoUsuario(org.mgroko.backend.modelo.Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
        return (nombre + " " + apellido).trim();
    }
}
