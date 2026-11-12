package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.publicaciones.dto.DireccionSolicitudColaboracion;
import org.mgroko.backend.publicaciones.dto.PublicacionSolicitudColaboracionResponse;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarPublicacionesSolicitudColaboracionServiceTest {

    @Mock
    private SolicitudColaboracionRepository solicitudColaboracionRepository;

    private ListarPublicacionesSolicitudColaboracionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new ListarPublicacionesSolicitudColaboracionService(
                solicitudColaboracionRepository);
    }

    @Test
    void listar_recibidasConsultaSoloRecibidasYDevuelveResumen() {
        Publicacion publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .estado(EstadoPublicacion.ACTIVA)
                .imagenes(List.of(Imagen.builder()
                        .idImagen(10L)
                        .url("/uploads/publicaciones/portada.webp")
                        .build()))
                .build();

        when(solicitudColaboracionRepository.findPublicacionesDistintasParaFiltro(
                7L, true, false,
                EstadoPublicacion.ACTIVA, EstadoPerfil.Activo, EstadoUsuario.Activo))
                .thenReturn(List.of(publicacion));

        List<PublicacionSolicitudColaboracionResponse> response = servicio.listar(
                7L, DireccionSolicitudColaboracion.RECIBIDA);

        assertEquals(1, response.size());
        assertEquals(99L, response.get(0).idPublicacion());
        assertEquals("Editorial Otoño", response.get(0).titulo());
        assertEquals("/uploads/publicaciones/portada.webp", response.get(0).portadaUrl());
        assertEquals("ACTIVA", response.get(0).estado());
    }

    @Test
    void listar_enviadasConsultaSoloEnviadas() {
        when(solicitudColaboracionRepository.findPublicacionesDistintasParaFiltro(
                7L, false, true,
                EstadoPublicacion.ACTIVA, EstadoPerfil.Activo, EstadoUsuario.Activo))
                .thenReturn(List.of());

        assertTrue(servicio.listar(7L, DireccionSolicitudColaboracion.ENVIADA).isEmpty());

        verify(solicitudColaboracionRepository)
                .findPublicacionesDistintasParaFiltro(
                        7L, false, true,
                        EstadoPublicacion.ACTIVA, EstadoPerfil.Activo, EstadoUsuario.Activo);
    }

    @Test
    void listar_sinDireccionConsultaAmbas() {
        when(solicitudColaboracionRepository.findPublicacionesDistintasParaFiltro(
                7L, true, true,
                EstadoPublicacion.ACTIVA, EstadoPerfil.Activo, EstadoUsuario.Activo))
                .thenReturn(List.of());

        assertTrue(servicio.listar(7L, null).isEmpty());

        verify(solicitudColaboracionRepository)
                .findPublicacionesDistintasParaFiltro(
                        7L, true, true,
                        EstadoPublicacion.ACTIVA, EstadoPerfil.Activo, EstadoUsuario.Activo);
    }

    @Test
    void listar_sinPerfilActivo_lanzaExcepcion() {
        assertThrows(PerfilActivoNoSeleccionadoException.class,
                () -> servicio.listar(null, DireccionSolicitudColaboracion.RECIBIDA));
    }
}
