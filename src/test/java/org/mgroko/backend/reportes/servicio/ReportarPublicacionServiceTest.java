package org.mgroko.backend.reportes.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.reportes.dto.CrearReportePublicacionRequest;
import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;
import org.mgroko.backend.reportes.evento.ReportePublicacionCreadoEvent;
import org.mgroko.backend.reportes.exception.AutoreportePublicacionException;
import org.mgroko.backend.reportes.exception.DescripcionReporteInvalidaException;
import org.mgroko.backend.reportes.exception.DescripcionReporteObligatoriaException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.reportes.exception.ReportePendienteExistenteException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.ReporteRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ReportarPublicacionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTIVO = 10L;
    private static final Long ID_PUBLICACION = 30L;

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReportarPublicacionService service;
    private Perfil autor;
    private Perfil duenio;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-11T12:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires"));
        service = new ReportarPublicacionService(
                perfilRepository, publicacionRepository, reporteRepository, eventPublisher, reloj);

        autor = perfil(10L, "Luna Diseños",
                usuario(1L, "Luna", "Pérez", "USUARIO", EstadoUsuario.ACTIVO));
        duenio = perfil(20L, "Perfil Dueño",
                usuario(2L, "Ana", "Gómez", "USUARIO", EstadoUsuario.ACTIVO));
        publicacion = publicacion();
    }

    @Test
    void reportar_valido_guardaYPublicaEvento() {
        prepararAutorYPublicacion();
        when(reporteRepository.existsPendiente(
                ID_PERFIL_ACTIVO, EntidadReporte.PUBLICACION, ID_PUBLICACION))
                .thenReturn(false);
        when(reporteRepository.saveAndFlush(any(Reporte.class))).thenAnswer(invocacion -> {
            Reporte reporte = invocacion.getArgument(0);
            reporte.setIdReporte(99L);
            return reporte;
        });

        service.reportar(
                ID_USUARIO,
                ID_PERFIL_ACTIVO,
                ID_PUBLICACION,
                new CrearReportePublicacionRequest(
                        MotivoReportePublicacion.CONTENIDO_INAPROPIADO,
                        "  Contenido inapropiado  "));

        ArgumentCaptor<Reporte> reporteCaptor = ArgumentCaptor.forClass(Reporte.class);
        verify(reporteRepository).saveAndFlush(reporteCaptor.capture());
        Reporte guardado = reporteCaptor.getValue();
        assertThat(guardado.getEntidadTipo()).isEqualTo(EntidadReporte.PUBLICACION);
        assertThat(guardado.getEntidadId()).isEqualTo(ID_PUBLICACION);
        assertThat(guardado.getUsuarioAutor()).isSameAs(autor.getUsuario());
        assertThat(guardado.getPerfilAutor()).isSameAs(autor);
        assertThat(guardado.getPerfilReportado()).isNull();
        assertThat(guardado.getMotivo()).isEqualTo(MotivoReporte.CONTENIDO_OFENSIVO);
        assertThat(guardado.getDescripcion()).isEqualTo("Contenido inapropiado");
        assertThat(guardado.getEstado()).isEqualTo(EstadoReporte.PENDIENTE);
        assertThat(guardado.getFechaReporte())
                .isEqualTo(LocalDateTime.of(2026, 11, 11, 9, 0));

        ArgumentCaptor<ReportePublicacionCreadoEvent> eventoCaptor =
                ArgumentCaptor.forClass(ReportePublicacionCreadoEvent.class);
        verify(eventPublisher).publishEvent(eventoCaptor.capture());
        ReportePublicacionCreadoEvent evento = eventoCaptor.getValue();
        assertThat(evento.idReporte()).isEqualTo(99L);
        assertThat(evento.idPublicacion()).isEqualTo(ID_PUBLICACION);
        assertThat(evento.tituloPublicacion()).isEqualTo("Publicación de prueba");
        assertThat(evento.idPerfilPublicacion()).isEqualTo(20L);
        assertThat(evento.nombrePerfilPublicacion()).isEqualTo("Perfil Dueño");
        assertThat(evento.idUsuarioAutor()).isEqualTo(ID_USUARIO);
        assertThat(evento.nombreUsuarioAutor()).isEqualTo("Luna Pérez");
        assertThat(evento.idPerfilAutor()).isEqualTo(ID_PERFIL_ACTIVO);
        assertThat(evento.nombrePerfilAutor()).isEqualTo("Luna Diseños");
        assertThat(evento.motivo()).isEqualTo(MotivoReportePublicacion.CONTENIDO_INAPROPIADO);
        assertThat(evento.descripcion()).isEqualTo("Contenido inapropiado");
    }

    @Test
    void reportar_otroSinDescripcion_lanzaExcepcion() {
        prepararAutorYPublicacion();

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.OTRO, "   ")))
                .isInstanceOf(DescripcionReporteObligatoriaException.class);

        verify(reporteRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportar_descripcionMayorA200_lanzaExcepcion() {
        prepararAutorYPublicacion();

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(
                        MotivoReportePublicacion.SPAM, "x".repeat(201))))
                .isInstanceOf(DescripcionReporteInvalidaException.class);

        verify(reporteRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, null, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void reportar_perfilActivoDeOtroUsuario_lanzaNoEncontrado() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void reportar_perfilActivoNoActivo_lanzaExcepcion() {
        autor.setEstado(EstadoPerfil.BAJA);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void reportar_admin_lanzaNoPermitido() {
        autor.getUsuario().setRolGlobal(rol("ADMINISTRADOR", "Administrador"));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(ReporteNoPermitidoException.class);
    }

    @Test
    void reportar_publicacionInexistente_lanzaNoEncontrado() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));
        when(publicacionRepository.findByIdPublicacion(ID_PUBLICACION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(PublicacionNoEncontradaException.class);
    }

    @Test
    void reportar_publicacionNoActiva_lanzaNoEncontrado() {
        publicacion.setEstado(EstadoPublicacion.OCULTA);
        prepararAutorYPublicacion();

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(PublicacionNoEncontradaException.class);
    }

    @Test
    void reportar_publicacionConPerfilInactivo_lanzaNoEncontrado() {
        duenio.setEstado(EstadoPerfil.BAJA);
        prepararAutorYPublicacion();

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(PublicacionNoEncontradaException.class);
    }

    @Test
    void reportar_publicacionConUsuarioInactivo_lanzaNoEncontrado() {
        duenio.getUsuario().setEstado(EstadoUsuario.DESHABILITADO);
        prepararAutorYPublicacion();

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(PublicacionNoEncontradaException.class);
    }

    @Test
    void reportar_autorreporte_lanzaExcepcion() {
        duenio.setUsuario(usuario(1L, "Luna", "Pérez", "USUARIO", EstadoUsuario.ACTIVO));
        prepararAutorYPublicacion();

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(AutoreportePublicacionException.class);
    }

    @Test
    void reportar_duplicadoPendiente_lanzaExcepcion() {
        prepararAutorYPublicacion();
        when(reporteRepository.existsPendiente(
                ID_PERFIL_ACTIVO, EntidadReporte.PUBLICACION, ID_PUBLICACION))
                .thenReturn(true);

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(ReportePendienteExistenteException.class);

        verify(reporteRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportar_carreraConIndiceUnico_lanzaDuplicado() {
        prepararAutorYPublicacion();
        when(reporteRepository.existsPendiente(
                ID_PERFIL_ACTIVO, EntidadReporte.PUBLICACION, ID_PUBLICACION))
                .thenReturn(false);
        when(reporteRepository.saveAndFlush(any(Reporte.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PUBLICACION,
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(ReportePendienteExistenteException.class);
    }

    private void prepararAutorYPublicacion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));
        when(publicacionRepository.findByIdPublicacion(ID_PUBLICACION))
                .thenReturn(Optional.of(publicacion));
    }

    private Publicacion publicacion() {
        return Publicacion.builder()
                .idPublicacion(ID_PUBLICACION)
                .titulo("Publicación de prueba")
                .descripcion("Contenido")
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(duenio)
                .build();
    }

    private Perfil perfil(Long id, String nombre, Usuario usuario) {
        return Perfil.builder()
                .idPerfil(id)
                .nombreArtistico(nombre)
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .build();
    }

    private Usuario usuario(
            Long id,
            String nombre,
            String apellido,
            String codigoRol,
            EstadoUsuario estado) {
        return Usuario.builder()
                .idUsuario(id)
                .nombre(nombre)
                .apellido(apellido)
                .correo("user" + id + "@example.com")
                .dni("3000000" + id)
                .estado(estado)
                .rolGlobal(rol(codigoRol, "Usuario"))
                .build();
    }

    private RolGlobal rol(String codigo, String nombre) {
        return RolGlobal.builder().codigo(codigo).nombre(nombre).build();
    }
}
