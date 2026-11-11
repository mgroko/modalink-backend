package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.publicaciones.dto.BuscarSolicitudesColaboracionFiltro;
import org.mgroko.backend.publicaciones.dto.DireccionSolicitudColaboracion;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionBusquedaResponse;
import org.mgroko.backend.publicaciones.exception.FiltroSolicitudColaboracionInvalidoException;
import org.mgroko.backend.publicaciones.exception.RangoFechasSolicitudColaboracionInvalidoException;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuscarSolicitudColaboracionServiceTest {

    @Mock
    private SolicitudColaboracionRepository solicitudColaboracionRepository;

    private BuscarSolicitudColaboracionService servicio;
    private SolicitudColaboracion solicitud;

    @BeforeEach
    void setUp() {
        servicio = new BuscarSolicitudColaboracionService(
                solicitudColaboracionRepository);

        Perfil remitente = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .build();
        Perfil destinatario = Perfil.builder()
                .idPerfil(8L)
                .nombreArtistico("Sol")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(4L).nombre("Fotógrafa").build())
                .build();
        Publicacion publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .perfil(remitente)
                .imagenes(List.of(Imagen.builder()
                        .idImagen(10L)
                        .url("/uploads/publicaciones/portada.webp")
                        .build()))
                .build();
        solicitud = SolicitudColaboracion.builder()
                .idSolicitudColaboracion(50L)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.of(2026, 11, 10, 20, 30))
                .publicacion(publicacion)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .build();
    }

    private void stubPagina(List<SolicitudColaboracion> contenido) {
        Page<SolicitudColaboracion> pagina = new PageImpl<>(
                contenido, PageRequest.of(0, 20), contenido.size());
        when(solicitudColaboracionRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);
        when(solicitudColaboracionRepository.findAllByIdInWithDetalle(anyList()))
                .thenReturn(contenido);
    }

    @Test
    void buscar_sinPerfilActivo_lanzaExcepcion() {
        assertThrows(PerfilActivoNoSeleccionadoException.class,
                () -> servicio.buscar(null, null, 0, 20));
    }

    @Test
    void buscar_todosLosEstadosConEstado_lanzaFiltroInvalido() {
        BuscarSolicitudesColaboracionFiltro filtro = new BuscarSolicitudesColaboracionFiltro(
                null, null, null, EstadoSolicitud.PENDIENTE, true,
                null, null, null);

        assertThrows(FiltroSolicitudColaboracionInvalidoException.class,
                () -> servicio.buscar(7L, filtro, 0, 20));
    }

    @Test
    void buscar_rangoDeFechasInvalido_lanzaExcepcion() {
        BuscarSolicitudesColaboracionFiltro filtro = new BuscarSolicitudesColaboracionFiltro(
                null, null, null, null, false,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), null);

        assertThrows(RangoFechasSolicitudColaboracionInvalidoException.class,
                () -> servicio.buscar(7L, filtro, 0, 20));
    }

    @Test
    void buscar_idsDeFiltroNoPositivos_lanzaFiltroInvalido() {
        BuscarSolicitudesColaboracionFiltro filtro = new BuscarSolicitudesColaboracionFiltro(
                null, 0L, null, null, false, null, null, null);

        assertThrows(FiltroSolicitudColaboracionInvalidoException.class,
                () -> servicio.buscar(7L, filtro, 0, 20));
    }

    @Test
    void buscar_paginaVacia_devuelvePaginaVaciaSinCargarDetalle() {
        stubPagina(List.of());

        PaginaResponse<SolicitudColaboracionBusquedaResponse> response =
                servicio.buscar(7L, null, 0, 20);

        assertTrue(response.contenido().isEmpty());
        assertEquals(0, response.totalElementos());
        verify(solicitudColaboracionRepository, never())
                .findAllByIdInWithDetalle(anyList());
    }

    @Test
    void buscar_perfilActivoComoDestinatario_marcaRecibidaYPuedeAceptar() {
        stubPagina(List.of(solicitud));

        PaginaResponse<SolicitudColaboracionBusquedaResponse> response =
                servicio.buscar(8L, null, 0, 20);

        SolicitudColaboracionBusquedaResponse item = response.contenido().get(0);
        assertEquals(DireccionSolicitudColaboracion.RECIBIDA, item.direccion());
        assertTrue(item.puedeAceptar());
        assertTrue(item.puedeRechazar());
        assertFalse(item.puedeEliminar());
        assertEquals("/uploads/publicaciones/portada.webp", item.publicacion().portadaUrl());
    }

    @Test
    void buscar_perfilActivoComoRemitente_marcaEnviadaYPuedeEliminar() {
        stubPagina(List.of(solicitud));

        PaginaResponse<SolicitudColaboracionBusquedaResponse> response =
                servicio.buscar(7L, null, 0, 20);

        SolicitudColaboracionBusquedaResponse item = response.contenido().get(0);
        assertEquals(DireccionSolicitudColaboracion.ENVIADA, item.direccion());
        assertFalse(item.puedeAceptar());
        assertFalse(item.puedeRechazar());
        assertTrue(item.puedeEliminar());
    }

    @Test
    void buscar_ordenaPorFechaDescendenteYLimitaElTamanoMaximo() {
        stubPagina(List.of(solicitud));

        servicio.buscar(7L, null, 0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudColaboracionRepository).findAll(
                any(Specification.class), captor.capture());

        Pageable pageable = captor.getValue();
        assertEquals(100, pageable.getPageSize());
        assertEquals(0, pageable.getPageNumber());
        assertEquals(
                Sort.by(
                        Sort.Order.desc("fechaEnvio"),
                        Sort.Order.desc("idSolicitudColaboracion")),
                pageable.getSort());
    }

}
