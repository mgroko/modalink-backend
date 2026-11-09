package org.mgroko.backend.auditoria.servicio;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaFiltroResuelto;
import org.mgroko.backend.auditoria.dto.TipoEventoAuditoriaResponse;
import org.mgroko.backend.auditoria.especificacion.AuditoriaSpecifications;
import org.mgroko.backend.auditoria.mapper.AuditoriaMapper;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Auditoria;
import org.mgroko.backend.modelo.DetalleAuditoria;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.repositorio.AuditoriaRepository;
import org.mgroko.backend.repositorio.DetalleAuditoriaRepository;
import org.mgroko.backend.repositorio.TipoEventoAuditoriaRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-67: consulta paginada del registro de auditoría.
 */
@Service
public class ListarAuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final DetalleAuditoriaRepository detalleAuditoriaRepository;
    private final TipoEventoAuditoriaRepository tipoEventoAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaFiltroService auditoriaFiltroService;
    private final AuditoriaMapper auditoriaMapper;

    public ListarAuditoriaService(
            AuditoriaRepository auditoriaRepository,
            DetalleAuditoriaRepository detalleAuditoriaRepository,
            TipoEventoAuditoriaRepository tipoEventoAuditoriaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaFiltroService auditoriaFiltroService,
            AuditoriaMapper auditoriaMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.detalleAuditoriaRepository = detalleAuditoriaRepository;
        this.tipoEventoAuditoriaRepository = tipoEventoAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaFiltroService = auditoriaFiltroService;
        this.auditoriaMapper = auditoriaMapper;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaEventoResponse> listar(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            List<String> tiposEvento,
            int page,
            int size) {

        AuditoriaFiltroResuelto filtro =
                auditoriaFiltroService.resolver(fechaDesde, fechaHasta, tiposEvento);
        auditoriaFiltroService.validarPaginacion(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("fechaHora"),
                        Sort.Order.desc("idAuditoria")));

        Page<Auditoria> pagina = auditoriaRepository.findAll(
                AuditoriaSpecifications.conFiltros(
                        filtro.fechaDesde(), filtro.fechaHasta(), filtro.tipos()),
                pageable);

        if (pagina.isEmpty()) {
            return PaginaResponse.fromPage(pagina.map(auditoria -> auditoriaMapper.toResponse(
                    auditoria,
                    filtro.catalogo(),
                    Collections.emptyMap(),
                    Collections.emptyMap())));
        }

        List<Long> idsAuditoria = pagina.getContent().stream()
                .map(Auditoria::getIdAuditoria)
                .toList();

        Map<Long, List<DetalleAuditoria>> detallesPorAuditoria =
                detalleAuditoriaRepository
                        .findByIdAuditoriaInOrderByIdDetalleAuditoriaAsc(idsAuditoria)
                        .stream()
                        .collect(Collectors.groupingBy(DetalleAuditoria::getIdAuditoria));

        Set<Long> idsUsuarios = pagina.getContent().stream()
                .map(Auditoria::getIdUsuario)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Usuario> usuarios = idsUsuarios.isEmpty()
                ? Collections.emptyMap()
                : usuarioRepository.findAllById(idsUsuarios).stream()
                        .collect(Collectors.toMap(Usuario::getIdUsuario, Function.identity()));

        return PaginaResponse.fromPage(pagina.map(auditoria -> auditoriaMapper.toResponse(
                auditoria,
                filtro.catalogo(),
                detallesPorAuditoria,
                usuarios)));
    }

    @Transactional(readOnly = true)
    public List<TipoEventoAuditoriaResponse> listarTipos() {
        return tipoEventoAuditoriaRepository.findByActivoTrueOrderByModuloAscNombreAsc()
                .stream()
                .map(tipo -> new TipoEventoAuditoriaResponse(
                        tipo.getCodigo(),
                        tipo.getNombre(),
                        tipo.getDescripcion(),
                        tipo.getModulo()))
                .toList();
    }
}
