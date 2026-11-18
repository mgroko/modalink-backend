package org.mgroko.backend.admin.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.admin.dto.ErrorProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.PreviewProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.ProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.ProcesarColaboracionesHistoricasRequest;
import org.mgroko.backend.admin.dto.PrevisualizarColaboracionesHistoricasRequest;
import org.mgroko.backend.admin.evento.ProcesamientoHistoricoSolicitadoEvent;
import org.mgroko.backend.admin.exception.ProcesamientoHistoricoEnCursoException;
import org.mgroko.backend.admin.exception.ProcesamientoHistoricoInvalidoException;
import org.mgroko.backend.admin.exception.ProcesamientoHistoricoNoCancelableException;
import org.mgroko.backend.admin.exception.ProcesamientoHistoricoNoEncontradoException;
import org.mgroko.backend.admin.exception.ProcesamientoHistoricoSinDatosException;
import org.mgroko.backend.admin.mapper.ProcesamientoHistoricoMapper;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleRequest;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.auditoria.servicio.AuditoriaEventoService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistorica;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistoricaError;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;
import org.mgroko.backend.repositorio.ProcesamientoColaboracionHistoricaErrorRepository;
import org.mgroko.backend.repositorio.ProcesamientoColaboracionHistoricaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración del procesamiento histórico de colaboraciones automáticas.
 */
@Service
public class AdminColaboracionHistoricaService {

    private static final List<EstadoProcesamientoHistorico> ESTADOS_ACTIVOS =
            List.of(EstadoProcesamientoHistorico.PENDIENTE,
                    EstadoProcesamientoHistorico.EN_PROGRESO);

    private final ProcesamientoColaboracionHistoricaRepository procesamientoRepository;
    private final ProcesamientoColaboracionHistoricaErrorRepository errorRepository;
    private final PublicacionRepository publicacionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditoriaEventoService auditoriaEventoService;
    private final Clock reloj;
    private final int umbralPhashCandidato;

    public AdminColaboracionHistoricaService(
            ProcesamientoColaboracionHistoricaRepository procesamientoRepository,
            ProcesamientoColaboracionHistoricaErrorRepository errorRepository,
            PublicacionRepository publicacionRepository,
            ApplicationEventPublisher eventPublisher,
            AuditoriaEventoService auditoriaEventoService,
            Clock reloj,
            @Value("${app.colaboracion-automatica.phash-distancia-candidato:24}")
            int umbralPhashCandidato) {
        this.procesamientoRepository = procesamientoRepository;
        this.errorRepository = errorRepository;
        this.publicacionRepository = publicacionRepository;
        this.eventPublisher = eventPublisher;
        this.auditoriaEventoService = auditoriaEventoService;
        this.reloj = reloj;
        this.umbralPhashCandidato = umbralPhashCandidato;
    }

    @Transactional(readOnly = true)
    public PreviewProcesamientoHistoricoResponse previsualizar(
            PrevisualizarColaboracionesHistoricasRequest request) {

        Filtros filtros = normalizarFiltros(
                request.modo(),
                request.idPublicacion(),
                request.idPerfil(),
                request.fechaDesde(),
                request.fechaHasta());

        long publicaciones = publicacionRepository.contarPublicacionesHistoricas(
                filtros.idPublicacion(), filtros.idPerfil(),
                filtros.desde(), filtros.hasta());

        long imagenes = publicacionRepository.contarImagenesHistoricas(
                filtros.idPublicacion(), filtros.idPerfil(),
                filtros.desde(), filtros.hasta());

        long sinSha256 = publicacionRepository.contarImagenesSinSha256Historicas(
                filtros.idPublicacion(), filtros.idPerfil(),
                filtros.desde(), filtros.hasta());

        long sinPhash = publicacionRepository.contarImagenesSinPhashHistoricas(
                filtros.idPublicacion(), filtros.idPerfil(),
                filtros.desde(), filtros.hasta());

        long pares = publicacionRepository.contarParesCandidatosHistoricos(
                filtros.idPublicacion(), filtros.idPerfil(),
                filtros.desde(), filtros.hasta(), umbralPhashCandidato);

        return new PreviewProcesamientoHistoricoResponse(
                publicaciones, imagenes, sinSha256, sinPhash, 0, pares);
    }

    @Transactional
    @AuditarOperacion
    public ProcesamientoHistoricoResponse procesar(
            ProcesarColaboracionesHistoricasRequest request,
            Long idUsuarioAdmin) {

        Filtros filtros = normalizarFiltros(
                request.modo(),
                request.idPublicacion(),
                request.idPerfil(),
                request.fechaDesde(),
                request.fechaHasta());

        int tamanoLote = request.tamanoLote() != null
                ? request.tamanoLote()
                : 50;
        if (tamanoLote < 1 || tamanoLote > 200) {
            throw new ProcesamientoHistoricoInvalidoException(
                    "El tamaño de lote debe estar entre 1 y 200.");
        }

        Integer limite = request.limitePublicaciones();
        if (limite != null && limite < 1) {
            throw new ProcesamientoHistoricoInvalidoException(
                    "El límite de publicaciones debe ser mayor a 0.");
        }

        if (procesamientoRepository.existsByEstadoIn(ESTADOS_ACTIVOS)) {
            throw new ProcesamientoHistoricoEnCursoException();
        }

        long publicaciones = publicacionRepository.contarPublicacionesHistoricas(
                filtros.idPublicacion(), filtros.idPerfil(),
                filtros.desde(), filtros.hasta());
        if (publicaciones == 0) {
            throw new ProcesamientoHistoricoSinDatosException();
        }

        long imagenes = publicacionRepository.contarImagenesHistoricas(
                filtros.idPublicacion(), filtros.idPerfil(),
                filtros.desde(), filtros.hasta());

        ProcesamientoColaboracionHistorica job =
                ProcesamientoColaboracionHistorica.builder()
                        .estado(EstadoProcesamientoHistorico.PENDIENTE)
                        .modo(filtros.modo())
                        .filtroIdPublicacion(filtros.idPublicacion())
                        .filtroIdPerfil(filtros.idPerfil())
                        .filtroFechaDesde(filtros.desde())
                        .filtroFechaHasta(filtros.hasta())
                        .notificar(Boolean.TRUE.equals(request.notificar()))
                        .tamanoLote(tamanoLote)
                        .limitePublicaciones(limite)
                        .totalPublicaciones((int) Math.min(publicaciones,
                                limite != null ? limite : Integer.MAX_VALUE))
                        .totalImagenes((int) imagenes)
                        .idUsuarioAdmin(idUsuarioAdmin)
                        .fechaInicio(LocalDateTime.now(reloj))
                        .build();

        job = procesamientoRepository.saveAndFlush(job);
        auditoriaEventoService.registrar(
                "procesamiento_colaboracion_historica",
                job.getIdProcesamiento(),
                "PROCESAMIENTO_HISTORICO",
                "INSERT",
                List.of(new AuditoriaDetalleRequest(
                        "estado", null, job.getEstado().name())));

        eventPublisher.publishEvent(
                new ProcesamientoHistoricoSolicitadoEvent(
                        job.getIdProcesamiento()));
        return ProcesamientoHistoricoMapper.toResponse(job);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProcesamientoHistoricoResponse> listar(
            EstadoProcesamientoHistorico estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int page,
            int size) {

        PaginaResponse<ProcesamientoHistoricoResponse> respuesta =
                paginaProcesamientos(estado, fechaDesde, fechaHasta, page, size);
        return respuesta;
    }

    @Transactional(readOnly = true)
    public ProcesamientoHistoricoResponse obtener(Long idProcesamiento) {
        return ProcesamientoHistoricoMapper.toResponse(buscar(idProcesamiento));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ErrorProcesamientoHistoricoResponse> listarErrores(
            Long idProcesamiento,
            int page,
            int size) {

        buscar(idProcesamiento);
        Page<ProcesamientoColaboracionHistoricaError> errores =
                errorRepository.findByIdProcesamientoOrderByFechaRegistroDesc(
                        idProcesamiento,
                        PageRequest.of(Math.max(0, page), normalizarTamano(size)));

        return PaginaResponse.fromPage(
                errores.map(ProcesamientoHistoricoMapper::toErrorResponse));
    }

    @Transactional
    @AuditarOperacion
    public ProcesamientoHistoricoResponse cancelar(
            Long idProcesamiento,
            Long idUsuarioAdmin) {

        ProcesamientoColaboracionHistorica job = procesamientoRepository
                .findByIdForUpdate(idProcesamiento)
                .orElseThrow(ProcesamientoHistoricoNoEncontradoException::new);

        if (job.getEstado() != EstadoProcesamientoHistorico.PENDIENTE
                && job.getEstado() != EstadoProcesamientoHistorico.EN_PROGRESO) {
            throw new ProcesamientoHistoricoNoCancelableException();
        }

        job.setEstado(EstadoProcesamientoHistorico.CANCELADO);
        job.setFechaFin(LocalDateTime.now(reloj));
        job = procesamientoRepository.saveAndFlush(job);

        auditoriaEventoService.registrar(
                "procesamiento_colaboracion_historica",
                job.getIdProcesamiento(),
                "PROCESAMIENTO_HISTORICO",
                "UPDATE",
                List.of(new AuditoriaDetalleRequest(
                        "estado", null, job.getEstado().name())));

        return ProcesamientoHistoricoMapper.toResponse(job);
    }

    @Transactional
    @AuditarOperacion
    public ProcesamientoHistoricoResponse reanudar(
            Long idProcesamiento,
            Long idUsuarioAdmin) {

        ProcesamientoColaboracionHistorica original = buscar(idProcesamiento);

        if (original.getEstado() != EstadoProcesamientoHistorico.INTERRUMPIDO
                && original.getEstado() != EstadoProcesamientoHistorico.FALLIDO) {
            throw new ProcesamientoHistoricoInvalidoException(
                    "Solo se pueden reanudar procesamientos interrumpidos o fallidos.");
        }

        if (procesamientoRepository.existsByEstadoIn(ESTADOS_ACTIVOS)) {
            throw new ProcesamientoHistoricoEnCursoException();
        }

        ProcesamientoColaboracionHistorica job =
                ProcesamientoColaboracionHistorica.builder()
                        .estado(EstadoProcesamientoHistorico.PENDIENTE)
                        .modo(original.getModo())
                        .filtroIdPublicacion(original.getFiltroIdPublicacion())
                        .filtroIdPerfil(original.getFiltroIdPerfil())
                        .filtroFechaDesde(original.getFiltroFechaDesde())
                        .filtroFechaHasta(original.getFiltroFechaHasta())
                        .notificar(original.isNotificar())
                        .tamanoLote(original.getTamanoLote())
                        .limitePublicaciones(original.getLimitePublicaciones())
                        .totalPublicaciones(original.getTotalPublicaciones())
                        .publicacionesProcesadas(original.getPublicacionesProcesadas())
                        .totalImagenes(original.getTotalImagenes())
                        .imagenesProcesadas(original.getImagenesProcesadas())
                        .hashesCalculados(original.getHashesCalculados())
                        .propuestasCreadas(original.getPropuestasCreadas())
                        .propuestasOmitidas(original.getPropuestasOmitidas())
                        .errores(original.getErrores())
                        .ultimoIdPublicacion(original.getUltimoIdPublicacion())
                        .idUsuarioAdmin(idUsuarioAdmin)
                        .fechaInicio(LocalDateTime.now(reloj))
                        .build();

        job = procesamientoRepository.saveAndFlush(job);
        auditoriaEventoService.registrar(
                "procesamiento_colaboracion_historica",
                job.getIdProcesamiento(),
                "PROCESAMIENTO_HISTORICO",
                "INSERT",
                List.of(new AuditoriaDetalleRequest(
                        "estado", null, job.getEstado().name())));

        eventPublisher.publishEvent(
                new ProcesamientoHistoricoSolicitadoEvent(
                        job.getIdProcesamiento()));
        return ProcesamientoHistoricoMapper.toResponse(job);
    }

    private PaginaResponse<ProcesamientoHistoricoResponse> paginaProcesamientos(
            EstadoProcesamientoHistorico estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int page,
            int size) {

        LocalDateTime desde = fechaDesde != null
                ? fechaDesde.atStartOfDay()
                : null;
        LocalDateTime hasta = fechaHasta != null
                ? fechaHasta.atTime(23, 59, 59)
                : null;

        Page<ProcesamientoColaboracionHistorica> pagina =
                procesamientoRepository.buscar(
                        estado, desde, hasta,
                        PageRequest.of(Math.max(0, page), normalizarTamano(size)));

        return PaginaResponse.fromPage(
                pagina.map(ProcesamientoHistoricoMapper::toResponse));
    }

    private ProcesamientoColaboracionHistorica buscar(Long idProcesamiento) {
        return procesamientoRepository.findById(idProcesamiento)
                .orElseThrow(ProcesamientoHistoricoNoEncontradoException::new);
    }

    private Filtros normalizarFiltros(
            ModoProcesamientoHistorico modo,
            Long idPublicacion,
            Long idPerfil,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        LocalDateTime desde = fechaDesde != null
                ? fechaDesde.atStartOfDay()
                : null;
        LocalDateTime hasta = fechaHasta != null
                ? fechaHasta.atTime(23, 59, 59)
                : null;

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ProcesamientoHistoricoInvalidoException(
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }

        return new Filtros(
                modo != null ? modo : ModoProcesamientoHistorico.AMBOS,
                idPublicacion,
                idPerfil,
                desde,
                hasta);
    }

    private int normalizarTamano(int size) {
        if (size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private record Filtros(
            ModoProcesamientoHistorico modo,
            Long idPublicacion,
            Long idPerfil,
            LocalDateTime desde,
            LocalDateTime hasta) {
    }
}
