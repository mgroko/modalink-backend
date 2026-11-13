package org.mgroko.backend.admin.servicio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.PreviewProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.PrevisualizarColaboracionesHistoricasRequest;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistorica;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;
import org.mgroko.backend.repositorio.ProcesamientoColaboracionHistoricaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class AdminColaboracionHistoricaLocalIntegrationTest {

    @Autowired
    private AdminColaboracionHistoricaService service;

    @Autowired
    private ProcesamientoHistoricoBatchService batchService;

    @Autowired
    private ProcesamientoColaboracionHistoricaRepository procesamientoRepository;

    @Test
    void previsualizar_sinFiltros_devuelveContadores() {
        PreviewProcesamientoHistoricoResponse response =
                service.previsualizar(new PrevisualizarColaboracionesHistoricasRequest(
                        ModoProcesamientoHistorico.AMBOS,
                        null, null, null, null, null));

        assertTrue(response.publicaciones() > 0);
        assertTrue(response.imagenes() > 0);
        assertTrue(response.paresCandidatosEstimados() >= 0);
    }

    @Test
    void previsualizar_porPublicacion_acotaLosContadores() {
        PreviewProcesamientoHistoricoResponse response =
                service.previsualizar(new PrevisualizarColaboracionesHistoricasRequest(
                        ModoProcesamientoHistorico.AMBOS,
                        1L, null, null, null, null));

        assertTrue(response.publicaciones() <= 1);
    }

    @Test
    void listar_sinFiltros_noFallaConParametrosNulos() {
        assertNotNull(service.listar(null, null, null, 0, 20));
    }

    @Test
    @Transactional
    void siguienteLote_devuelvePublicacionesActivas() {
        ProcesamientoColaboracionHistorica job =
                procesamientoRepository.saveAndFlush(
                        ProcesamientoColaboracionHistorica.builder()
                                .estado(EstadoProcesamientoHistorico.EN_PROGRESO)
                                .modo(ModoProcesamientoHistorico.AMBOS)
                                .notificar(false)
                                .tamanoLote(10)
                                .idUsuarioAdmin(1L)
                                .fechaInicio(LocalDateTime.now())
                                .build());

        assertFalse(batchService.siguienteLote(job.getIdProcesamiento()).isEmpty());
    }
}
