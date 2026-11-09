package org.mgroko.backend.conexiones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mgroko.backend.conexiones.dto.BuscarSolicitudesConexionFiltro;
import org.mgroko.backend.conexiones.dto.DireccionSolicitudConexion;
import org.mgroko.backend.conexiones.dto.SolicitudConexionBusquedaResponse;
import org.mgroko.backend.conexiones.exception.RangoFechasSolicitudConexionInvalidoException;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BuscarSolicitudConexionServiceTest {

    private static final Long ID_PERFIL_ACTIVO = 10L;

    @Mock
    private SolicitudConexionRepository solicitudConexionRepository;

    private BuscarSolicitudConexionService service;

    private Perfil perfilActivo;
    private Perfil perfilContraparte;

    @BeforeEach
    void setUp() {
        service = new BuscarSolicitudConexionService(solicitudConexionRepository);

        Usuario usuarioActivo = Usuario.builder()
                .idUsuario(1L)
                .nombre("Ana")
                .apellido("Perez")
                .estado(EstadoUsuario.Activo)
                .build();

        Usuario usuarioContraparte = Usuario.builder()
                .idUsuario(2L)
                .nombre("José")
                .apellido("Gómez")
                .estado(EstadoUsuario.Activo)
                .build();

        perfilActivo = Perfil.builder()
                .idPerfil(ID_PERFIL_ACTIVO)
                .nombreArtistico("Ana Moda")
                .biografia("Perfil activo")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuarioActivo)
                .profesion(Profesion.builder()
                        .idProfesion(1L).codigo("MODELO").nombre("Modelo").build())
                .imagen(Imagen.builder().idImagen(1L).url("https://foto-ana").build())
                .build();

        perfilContraparte = Perfil.builder()
                .idPerfil(20L)
                .nombreArtistico("Diseño José")
                .biografia("Perfil contraparte")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuarioContraparte)
                .profesion(Profesion.builder()
                        .idProfesion(2L).codigo("DISENADOR").nombre("Diseñador").build())
                .imagen(Imagen.builder().idImagen(2L).url("https://foto-jose").build())
                .build();
    }

    @Test
    void buscar_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        assertThatThrownBy(() -> service.buscar(null, null, 0, 20))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);

        verify(solicitudConexionRepository, never())
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void buscar_rangoFechasInvalido_lanzaExcepcion() {
        BuscarSolicitudesConexionFiltro filtro = new BuscarSolicitudesConexionFiltro(
                null, null, null, null, null, null,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9));

        assertThatThrownBy(() -> service.buscar(ID_PERFIL_ACTIVO, filtro, 0, 20))
                .isInstanceOf(RangoFechasSolicitudConexionInvalidoException.class);
    }

    @Test
    void buscar_solicitudRecibidaPendiente_marcaAccionesDeGestion() {
        SolicitudConexion solicitud = solicitud(EstadoSolicitud.PENDIENTE, false);
        mockPagina(solicitud);

        PaginaResponse<SolicitudConexionBusquedaResponse> respuesta =
                service.buscar(ID_PERFIL_ACTIVO, null, 0, 20);

        assertThat(respuesta.contenido()).hasSize(1);
        SolicitudConexionBusquedaResponse item = respuesta.contenido().get(0);
        assertThat(item.direccion()).isEqualTo(DireccionSolicitudConexion.RECIBIDA);
        assertThat(item.puedeAceptar()).isTrue();
        assertThat(item.puedeRechazar()).isTrue();
        assertThat(item.puedeCancelar()).isFalse();
        assertThat(item.perfil().idPerfil()).isEqualTo(20L);
        assertThat(item.perfil().fotoUrl()).isEqualTo("https://foto-jose");
    }

    @Test
    void buscar_solicitudEnviadaPendiente_marcaPuedeCancelar() {
        SolicitudConexion solicitud = solicitud(EstadoSolicitud.PENDIENTE, true);
        mockPagina(solicitud);

        PaginaResponse<SolicitudConexionBusquedaResponse> respuesta =
                service.buscar(ID_PERFIL_ACTIVO, null, 0, 20);

        SolicitudConexionBusquedaResponse item = respuesta.contenido().get(0);
        assertThat(item.direccion()).isEqualTo(DireccionSolicitudConexion.ENVIADA);
        assertThat(item.puedeAceptar()).isFalse();
        assertThat(item.puedeRechazar()).isFalse();
        assertThat(item.puedeCancelar()).isTrue();
        assertThat(item.perfil().idPerfil()).isEqualTo(20L);
    }

    @Test
    void buscar_solicitudAceptada_noMarcaAcciones() {
        SolicitudConexion solicitud = solicitud(EstadoSolicitud.ACEPTADA, false);
        mockPagina(solicitud);

        PaginaResponse<SolicitudConexionBusquedaResponse> respuesta =
                service.buscar(ID_PERFIL_ACTIVO, null, 0, 20);

        SolicitudConexionBusquedaResponse item = respuesta.contenido().get(0);
        assertThat(item.puedeAceptar()).isFalse();
        assertThat(item.puedeRechazar()).isFalse();
        assertThat(item.puedeCancelar()).isFalse();
    }

    @Test
    void buscar_sinResultados_devuelvePaginaVacia() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(solicitudConexionRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        PaginaResponse<SolicitudConexionBusquedaResponse> respuesta =
                service.buscar(ID_PERFIL_ACTIVO, null, 0, 20);

        assertThat(respuesta.contenido()).isEmpty();
        assertThat(respuesta.totalElementos()).isZero();
        verify(solicitudConexionRepository, never())
                .findAllByIdInWithPerfiles(any());
    }

    @Test
    void buscar_tamanoMayorAlMaximo_seAcotaA100() {
        mockPagina(solicitud(EstadoSolicitud.PENDIENTE, false));

        service.buscar(ID_PERFIL_ACTIVO, null, 0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudConexionRepository)
                .findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().getSort().getOrderFor("fechaEnvio").isDescending())
                .isTrue();
    }

    @Test
    void buscar_tamanoInvalido_usa20() {
        mockPagina(solicitud(EstadoSolicitud.PENDIENTE, false));

        service.buscar(ID_PERFIL_ACTIVO, null, -1, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudConexionRepository)
                .findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    private void mockPagina(SolicitudConexion solicitud) {
        PageRequest pageable = PageRequest.of(0, 20);
        when(solicitudConexionRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(solicitud), pageable, 1));
        when(solicitudConexionRepository.findAllByIdInWithPerfiles(
                eq(List.of(solicitud.getIdSolicitudConexion()))))
                .thenReturn(List.of(solicitud));
    }

    private SolicitudConexion solicitud(EstadoSolicitud estado, boolean enviada) {
        return SolicitudConexion.builder()
                .idSolicitudConexion(100L)
                .perfilRemitente(enviada ? perfilActivo : perfilContraparte)
                .perfilDestinatario(enviada ? perfilContraparte : perfilActivo)
                .estado(estado)
                .fechaEnvio(LocalDateTime.of(2026, 11, 9, 10, 0))
                .build();
    }
}
