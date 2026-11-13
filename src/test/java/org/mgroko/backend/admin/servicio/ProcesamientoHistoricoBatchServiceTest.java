package org.mgroko.backend.admin.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistorica;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;
import org.mgroko.backend.publicaciones.servicio.DetectarColaboracionAutomaticaService;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.ProcesamientoColaboracionHistoricaErrorRepository;
import org.mgroko.backend.repositorio.ProcesamientoColaboracionHistoricaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.storage.servicio.HuellaImagenService;
import org.mgroko.backend.storage.servicio.StorageService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcesamientoHistoricoBatchServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ProcesamientoColaboracionHistoricaRepository procesamientoRepository;

    @Mock
    private ProcesamientoColaboracionHistoricaErrorRepository errorRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private HuellaImagenService huellaImagenService;

    @Mock
    private DetectarColaboracionAutomaticaService detectorService;

    private ProcesamientoHistoricoBatchService servicio;
    private ProcesamientoColaboracionHistorica job;
    private Publicacion publicacion;
    private Imagen imagen;

    @BeforeEach
    void setUp() {
        servicio = new ProcesamientoHistoricoBatchService(
                procesamientoRepository,
                errorRepository,
                publicacionRepository,
                imagenRepository,
                storageService,
                huellaImagenService,
                detectorService,
                RELOJ);

        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Activo)
                .build();
        Perfil perfil = Perfil.builder()
                .idPerfil(7L)
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .build();
        imagen = Imagen.builder()
                .idImagen(10L)
                .nombreArchivo("publicacion_a.webp")
                .build();
        publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .imagenes(List.of(imagen))
                .build();

        job = ProcesamientoColaboracionHistorica.builder()
                .idProcesamiento(5L)
                .estado(EstadoProcesamientoHistorico.EN_PROGRESO)
                .modo(ModoProcesamientoHistorico.AMBOS)
                .notificar(false)
                .tamanoLote(50)
                .idUsuarioAdmin(1L)
                .fechaInicio(LocalDateTime.now(RELOJ))
                .build();

        when(procesamientoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(job));
        when(procesamientoRepository.findById(5L)).thenReturn(Optional.of(job));
        when(procesamientoRepository.saveAndFlush(any(ProcesamientoColaboracionHistorica.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(publicacionRepository.findByIdPublicacionConImagenes(99L))
                .thenReturn(Optional.of(publicacion));
        when(storageService.leerArchivo("publicacion_a.webp"))
                .thenReturn(new byte[] { 1, 2, 3 });
        when(huellaImagenService.sha256(any())).thenReturn("sha-contenido");
        when(huellaImagenService.phash(any(byte[].class))).thenReturn(123L);
        when(detectorService.detectar(eq(publicacion), any(), eq(false)))
                .thenReturn(new DetectarColaboracionAutomaticaService.DeteccionResultado(
                        1, 1, 0));
    }

    @Test
    void procesarPublicacion_modoAmbos_calculaHuellasYDetecta() {
        servicio.procesarPublicacion(5L, 99L);

        assertEquals("sha-contenido", imagen.getSha256Contenido());
        assertEquals(123L, imagen.getPhash());
        assertEquals(HuellaImagenService.PHASH_VERSION, imagen.getPhashVersion());
        assertEquals(1, job.getPublicacionesProcesadas());
        assertEquals(1, job.getImagenesProcesadas());
        assertEquals(1, job.getHashesCalculados());
        assertEquals(1, job.getPropuestasCreadas());
        assertEquals(99L, job.getUltimoIdPublicacion());

        verify(imagenRepository).save(imagen);
        verify(detectorService).detectar(eq(publicacion), any(), eq(false));
    }

    @Test
    void procesarPublicacion_imagenIlegible_registraErrorYNoDetecta() {
        when(storageService.leerArchivo("publicacion_a.webp")).thenReturn(null);

        servicio.procesarPublicacion(5L, 99L);

        assertEquals(1, job.getErrores());
        assertEquals(1, job.getPublicacionesProcesadas());
        assertEquals(1, job.getImagenesProcesadas());
        assertEquals(0, job.getHashesCalculados());
        assertEquals(99L, job.getUltimoIdPublicacion());

        verify(errorRepository).save(any());
        verify(detectorService, never()).detectar(any(), any(), anyBoolean());
    }

    @Test
    void procesarPublicacion_modoHuellas_noDetecta() {
        job.setModo(ModoProcesamientoHistorico.HUELLAS);

        servicio.procesarPublicacion(5L, 99L);

        assertEquals(1, job.getHashesCalculados());
        verify(detectorService, never()).detectar(any(), any(), anyBoolean());
    }

    @Test
    void siguienteLote_respetaLimiteDePublicaciones() {
        job.setLimitePublicaciones(10);
        job.setPublicacionesProcesadas(10);

        List<Long> lote = servicio.siguienteLote(5L);

        assertTrue(lote.isEmpty());
        verify(publicacionRepository, never())
                .buscarIdsPublicacionesHistoricas(any(), any(), any(), any(), any(), any());
    }

    @Test
    void finalizar_conErrores_marcaCompletadoConErrores() {
        job.setErrores(2);

        servicio.finalizar(5L);

        assertEquals(EstadoProcesamientoHistorico.COMPLETADO_CON_ERRORES,
                job.getEstado());
        assertTrue(job.getFechaFin() != null);
    }
}
