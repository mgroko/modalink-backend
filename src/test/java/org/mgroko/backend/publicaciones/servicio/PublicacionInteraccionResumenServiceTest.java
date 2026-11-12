package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.publicaciones.dto.ResumenInteraccionPublicacion;
import org.mgroko.backend.repositorio.ComentarioRepository;
import org.mgroko.backend.repositorio.MeGustaRepository;
import org.mgroko.backend.repositorio.proyeccion.ConteoPorPublicacion;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicacionInteraccionResumenServiceTest {

    @Mock
    private MeGustaRepository meGustaRepository;

    @Mock
    private ComentarioRepository comentarioRepository;

    private PublicacionInteraccionResumenService servicio;

    @BeforeEach
    void setUp() {
        servicio = new PublicacionInteraccionResumenService(
                meGustaRepository, comentarioRepository);
    }

    @Test
    void resumir_devuelveConteosYEstadoDeMeGusta() {
        ConteoPorPublicacion meGusta = conteo(99L, 3L);
        ConteoPorPublicacion comentarios = conteo(99L, 2L);

        when(meGustaRepository.contarPorPublicaciones(any()))
                .thenReturn(List.of(meGusta));
        when(comentarioRepository.contarPorPublicaciones(any()))
                .thenReturn(List.of(comentarios));
        when(meGustaRepository.findIdsPublicacionesConMeGusta(any(), any()))
                .thenReturn(List.of(99L));

        Map<Long, ResumenInteraccionPublicacion> resumenes =
                servicio.resumir(List.of(99L), 7L);

        assertEquals(1, resumenes.size());
        ResumenInteraccionPublicacion resumen = resumenes.get(99L);
        assertEquals(3, resumen.cantidadMeGusta());
        assertEquals(2, resumen.cantidadComentarios());
        assertTrue(resumen.meGusta());
    }

    @Test
    void resumir_sinPerfilActivo_devuelveMeGustaFalseYNoConsultaEstados() {
        ConteoPorPublicacion meGusta = conteo(99L, 3L);
        ConteoPorPublicacion comentarios = conteo(99L, 2L);

        when(meGustaRepository.contarPorPublicaciones(any()))
                .thenReturn(List.of(meGusta));
        when(comentarioRepository.contarPorPublicaciones(any()))
                .thenReturn(List.of(comentarios));

        Map<Long, ResumenInteraccionPublicacion> resumenes =
                servicio.resumir(List.of(99L), null);

        assertFalse(resumenes.get(99L).meGusta());
        verify(meGustaRepository, never())
                .findIdsPublicacionesConMeGusta(any(), any());
    }

    @Test
    void resumir_listaVacia_noConsultaRepositorios() {
        Map<Long, ResumenInteraccionPublicacion> resumenes =
                servicio.resumir(List.of(), 7L);

        assertTrue(resumenes.isEmpty());
        verifyNoInteractions(meGustaRepository, comentarioRepository);
    }

    @Test
    void resumirUna_publicacionSinInteracciones_devuelveCeros() {
        when(meGustaRepository.contarPorPublicaciones(any())).thenReturn(List.of());
        when(comentarioRepository.contarPorPublicaciones(any())).thenReturn(List.of());
        when(meGustaRepository.findIdsPublicacionesConMeGusta(any(), any()))
                .thenReturn(List.of());

        ResumenInteraccionPublicacion resumen = servicio.resumirUna(99L, 7L);

        assertEquals(0, resumen.cantidadMeGusta());
        assertEquals(0, resumen.cantidadComentarios());
        assertFalse(resumen.meGusta());
    }

    private ConteoPorPublicacion conteo(Long idPublicacion, Long total) {
        ConteoPorPublicacion conteo = mock(ConteoPorPublicacion.class);
        when(conteo.getIdPublicacion()).thenReturn(idPublicacion);
        when(conteo.getTotal()).thenReturn(total);
        return conteo;
    }
}
