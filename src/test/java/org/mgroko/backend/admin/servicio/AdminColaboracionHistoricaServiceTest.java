package org.mgroko.backend.admin.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.ProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.ProcesarColaboracionesHistoricasRequest;
import org.mgroko.backend.admin.dto.PrevisualizarColaboracionesHistoricasRequest;
import org.mgroko.backend.admin.evento.ProcesamientoHistoricoSolicitadoEvent;
import org.mgroko.backend.admin.exception.ProcesamientoHistoricoEnCursoException;
import org.mgroko.backend.admin.exception.ProcesamientoHistoricoNoCancelableException;
import org.mgroko.backend.admin.exception.ProcesamientoHistoricoNoEncontradoException;
import org.mgroko.backend.admin.exception.ProcesamientoHistoricoSinDatosException;
import org.mgroko.backend.auditoria.servicio.AuditoriaEventoService;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistorica;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;
import org.mgroko.backend.repositorio.ProcesamientoColaboracionHistoricaErrorRepository;
import org.mgroko.backend.repositorio.ProcesamientoColaboracionHistoricaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminColaboracionHistoricaServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ProcesamientoColaboracionHistoricaRepository procesamientoRepository;

    @Mock
    private ProcesamientoColaboracionHistoricaErrorRepository errorRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditoriaEventoService auditoriaEventoService;

    private AdminColaboracionHistoricaService servicio;

    @BeforeEach
    void setUp() {
        servicio = new AdminColaboracionHistoricaService(
                procesamientoRepository,
                errorRepository,
                publicacionRepository,
                eventPublisher,
                auditoriaEventoService,
                RELOJ,
                24);
    }

    @Test
    void previsualizar_devuelveContadores() {
        when(publicacionRepository.contarPublicacionesHistoricas(any(), any(), any(), any()))
                .thenReturn(10L);
        when(publicacionRepository.contarImagenesHistoricas(any(), any(), any(), any()))
                .thenReturn(25L);
        when(publicacionRepository.contarImagenesSinSha256Historicas(any(), any(), any(), any()))
                .thenReturn(3L);
        when(publicacionRepository.contarImagenesSinPhashHistoricas(any(), any(), any(), any()))
                .thenReturn(4L);
        when(publicacionRepository.contarParesCandidatosHistoricos(any(), any(), any(), any(), eq(24)))
                .thenReturn(7L);

        var response = servicio.previsualizar(new PrevisualizarColaboracionesHistoricasRequest(
                ModoProcesamientoHistorico.AMBOS, null, null, null, null, null));

        assertEquals(10L, response.publicaciones());
        assertEquals(25L, response.imagenes());
        assertEquals(3L, response.imagenesSinSha256());
        assertEquals(4L, response.imagenesSinPhash());
        assertEquals(7L, response.paresCandidatosEstimados());
    }

    @Test
    void procesar_creaJobYDisparaWorker() {
        when(procesamientoRepository.existsByEstadoIn(any())).thenReturn(false);
        when(publicacionRepository.contarPublicacionesHistoricas(any(), any(), any(), any()))
                .thenReturn(10L);
        when(publicacionRepository.contarImagenesHistoricas(any(), any(), any(), any()))
                .thenReturn(20L);
        when(procesamientoRepository.saveAndFlush(any(ProcesamientoColaboracionHistorica.class)))
                .thenAnswer(invocation -> {
                    ProcesamientoColaboracionHistorica job = invocation.getArgument(0);
                    job.setIdProcesamiento(5L);
                    return job;
                });

        ProcesamientoHistoricoResponse response = servicio.procesar(
                new ProcesarColaboracionesHistoricasRequest(
                        ModoProcesamientoHistorico.AMBOS, null, null, null, null,
                        null, 50, null),
                1L);

        assertEquals(5L, response.idProcesamiento());
        assertEquals(EstadoProcesamientoHistorico.PENDIENTE, response.estado());
        assertEquals(10, response.totalPublicaciones());
        assertEquals(20, response.totalImagenes());
        assertEquals(false, response.notificar());
        verify(eventPublisher).publishEvent(
                any(ProcesamientoHistoricoSolicitadoEvent.class));
    }

    @Test
    void procesar_conJobActivo_lanzaEnCurso() {
        when(procesamientoRepository.existsByEstadoIn(any())).thenReturn(true);

        assertThrows(ProcesamientoHistoricoEnCursoException.class,
                () -> servicio.procesar(requestProcesar(), 1L));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void procesar_sinPublicaciones_lanzaSinDatos() {
        when(procesamientoRepository.existsByEstadoIn(any())).thenReturn(false);
        when(publicacionRepository.contarPublicacionesHistoricas(any(), any(), any(), any()))
                .thenReturn(0L);

        assertThrows(ProcesamientoHistoricoSinDatosException.class,
                () -> servicio.procesar(requestProcesar(), 1L));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void procesar_tamanoDeLoteInvalido_lanzaInvalido() {
        assertThrows(org.mgroko.backend.admin.exception.ProcesamientoHistoricoInvalidoException.class,
                () -> servicio.procesar(new ProcesarColaboracionesHistoricasRequest(
                        ModoProcesamientoHistorico.AMBOS, null, null, null, null,
                        false, 500, null), 1L));
    }

    @Test
    void listar_mapeaPagina() {
        ProcesamientoColaboracionHistorica job = job(
                5L, EstadoProcesamientoHistorico.COMPLETADO);
        when(procesamientoRepository.buscar(any(), any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(job)));

        var response = servicio.listar(null, null, null, 0, 20);

        assertEquals(1, response.contenido().size());
        assertEquals(5L, response.contenido().get(0).idProcesamiento());
    }

    @Test
    void obtener_inexistente_lanzaNoEncontrado() {
        when(procesamientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProcesamientoHistoricoNoEncontradoException.class,
                () -> servicio.obtener(99L));
    }

    @Test
    void cancelar_jobPendiente_marcaCancelado() {
        ProcesamientoColaboracionHistorica job = job(
                5L, EstadoProcesamientoHistorico.PENDIENTE);
        when(procesamientoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(job));
        when(procesamientoRepository.saveAndFlush(job)).thenReturn(job);

        var response = servicio.cancelar(5L, 1L);

        assertEquals(EstadoProcesamientoHistorico.CANCELADO, response.estado());
    }

    @Test
    void cancelar_jobCompletado_lanzaNoCancelable() {
        ProcesamientoColaboracionHistorica job = job(
                5L, EstadoProcesamientoHistorico.COMPLETADO);
        when(procesamientoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(job));

        assertThrows(ProcesamientoHistoricoNoCancelableException.class,
                () -> servicio.cancelar(5L, 1L));
    }

    @Test
    void reanudar_interrumpido_creaNuevoJobConCursor() {
        ProcesamientoColaboracionHistorica original = job(
                5L, EstadoProcesamientoHistorico.INTERRUMPIDO);
        original.setUltimoIdPublicacion(42L);
        original.setPublicacionesProcesadas(3);

        when(procesamientoRepository.findById(5L)).thenReturn(Optional.of(original));
        when(procesamientoRepository.existsByEstadoIn(any())).thenReturn(false);
        when(procesamientoRepository.saveAndFlush(any(ProcesamientoColaboracionHistorica.class)))
                .thenAnswer(invocation -> {
                    ProcesamientoColaboracionHistorica job = invocation.getArgument(0);
                    job.setIdProcesamiento(6L);
                    return job;
                });

        var response = servicio.reanudar(5L, 1L);

        assertEquals(6L, response.idProcesamiento());
        assertEquals(42L, response.ultimoIdPublicacion());
        assertEquals(EstadoProcesamientoHistorico.PENDIENTE, response.estado());
        verify(eventPublisher).publishEvent(
                any(ProcesamientoHistoricoSolicitadoEvent.class));
    }

    private ProcesarColaboracionesHistoricasRequest requestProcesar() {
        return new ProcesarColaboracionesHistoricasRequest(
                ModoProcesamientoHistorico.AMBOS, null, null, null, null,
                false, 50, null);
    }

    private ProcesamientoColaboracionHistorica job(
            Long id,
            EstadoProcesamientoHistorico estado) {

        return ProcesamientoColaboracionHistorica.builder()
                .idProcesamiento(id)
                .estado(estado)
                .modo(ModoProcesamientoHistorico.AMBOS)
                .notificar(false)
                .tamanoLote(50)
                .idUsuarioAdmin(1L)
                .fechaInicio(LocalDateTime.now(RELOJ))
                .build();
    }
}
