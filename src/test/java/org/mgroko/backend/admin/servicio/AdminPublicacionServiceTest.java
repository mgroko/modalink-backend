package org.mgroko.backend.admin.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.AdminPublicacionResponse;
import org.mgroko.backend.admin.dto.ModerarPublicacionRequest;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.auditoria.servicio.AuditoriaEventoService;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.publicaciones.evento.PublicacionModeradaEvent;
import org.mgroko.backend.publicaciones.exception.EstadoPublicacionInvalidoException;
import org.mgroko.backend.publicaciones.exception.PublicacionInvalidaException;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.repositorio.ComentarioRepository;
import org.mgroko.backend.repositorio.MeGustaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.ReporteRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminPublicacionServiceTest {

    private static final Long ID_PUBLICACION = 30L;
    private static final Long ID_ADMIN = 99L;
    private static final Long ID_PERFIL = 7L;
    private static final Long ID_DUENIO = 2L;

    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MeGustaRepository meGustaRepository;
    @Mock
    private ComentarioRepository comentarioRepository;
    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private AuditoriaEventoService auditoriaEventoService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AdminPublicacionService service;
    private Usuario admin;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        service = new AdminPublicacionService(
                publicacionRepository,
                usuarioRepository,
                meGustaRepository,
                comentarioRepository,
                reporteRepository,
                auditoriaEventoService,
                eventPublisher);

        admin = usuario(1L, "ADMINISTRADOR", "Administrador");
        Usuario duenio = usuario(ID_DUENIO, "USUARIO", "Usuario");
        Perfil perfil = Perfil.builder()
                .idPerfil(ID_PERFIL)
                .nombreArtistico("Luna Diseños")
                .usuario(duenio)
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(ID_PUBLICACION)
                .titulo("Editorial Otoño")
                .descripcion("Producción realizada en Buenos Aires.")
                .perfil(perfil)
                .imagenes(new ArrayList<>())
                .colaboradores(new LinkedHashSet<>())
                .build();
    }

    @Test
    void ocultar_cambiaEstadoAuditaYPublicaEvento() {
        prepararAdminYPublicacion();

        AdminPublicacionResponse response = service.moderar(
                ID_PUBLICACION,
                ID_ADMIN,
                new ModerarPublicacionRequest(
                        EstadoPublicacion.OCULTA,
                        "  Contenido inapropiado  "));

        assertThat(publicacion.getEstado()).isEqualTo(EstadoPublicacion.OCULTA);
        assertThat(response.estado()).isEqualTo(EstadoPublicacion.OCULTA);
        assertThat(response.puedeOcultar()).isFalse();
        assertThat(response.puedeReactivar()).isTrue();

        verify(publicacionRepository).saveAndFlush(publicacion);
        verify(auditoriaEventoService).registrar(
                eq("publicacion"),
                eq(ID_PUBLICACION),
                eq("PUBLICACION_OCULTADA"),
                eq("UPDATE"),
                anyList());
        verify(eventPublisher).publishEvent(any(PublicacionModeradaEvent.class));
    }

    @Test
    void reactivar_cambiaEstadoAuditaYPublicaEvento() {
        publicacion.setEstado(EstadoPublicacion.OCULTA);
        prepararAdminYPublicacion();

        AdminPublicacionResponse response = service.moderar(
                ID_PUBLICACION,
                ID_ADMIN,
                new ModerarPublicacionRequest(EstadoPublicacion.ACTIVA, null));

        assertThat(publicacion.getEstado()).isEqualTo(EstadoPublicacion.ACTIVA);
        assertThat(response.estado()).isEqualTo(EstadoPublicacion.ACTIVA);
        assertThat(response.puedeOcultar()).isTrue();
        assertThat(response.puedeReactivar()).isFalse();

        verify(auditoriaEventoService).registrar(
                eq("publicacion"),
                eq(ID_PUBLICACION),
                eq("PUBLICACION_REACTIVADA"),
                eq("UPDATE"),
                anyList());
        verify(eventPublisher).publishEvent(any(PublicacionModeradaEvent.class));
    }

    @Test
    void ocultar_sinMotivo_lanzaExcepcionYNoBloqueaLaPublicacion() {
        when(usuarioRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.moderar(
                ID_PUBLICACION,
                ID_ADMIN,
                new ModerarPublicacionRequest(EstadoPublicacion.OCULTA, "   ")))
                .isInstanceOf(PublicacionInvalidaException.class);

        verify(publicacionRepository, never()).findByIdPublicacionForUpdate(any());
    }

    @Test
    void ocultar_publicacionYaOculta_lanzaConflicto() {
        publicacion.setEstado(EstadoPublicacion.OCULTA);
        prepararAdminYPublicacion();

        assertThatThrownBy(() -> service.moderar(
                ID_PUBLICACION,
                ID_ADMIN,
                new ModerarPublicacionRequest(
                        EstadoPublicacion.OCULTA, "Contenido inapropiado")))
                .isInstanceOf(EstadoPublicacionInvalidoException.class);

        verify(publicacionRepository, never()).saveAndFlush(any(Publicacion.class));
    }

    @Test
    void moderar_publicacionInexistente_lanzaNoEncontrada() {
        when(usuarioRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
        when(publicacionRepository.findByIdPublicacionForUpdate(ID_PUBLICACION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moderar(
                ID_PUBLICACION,
                ID_ADMIN,
                new ModerarPublicacionRequest(
                        EstadoPublicacion.OCULTA, "Contenido inapropiado")))
                .isInstanceOf(PublicacionNoEncontradaException.class);
    }

    @Test
    void moderar_usuarioNoAdministrador_lanzaNoPermitido() {
        when(usuarioRepository.findById(ID_ADMIN))
                .thenReturn(Optional.of(usuario(ID_ADMIN, "USUARIO", "Usuario")));

        assertThatThrownBy(() -> service.moderar(
                ID_PUBLICACION,
                ID_ADMIN,
                new ModerarPublicacionRequest(
                        EstadoPublicacion.OCULTA, "Contenido inapropiado")))
                .isInstanceOf(ReporteNoPermitidoException.class);
    }

    @Test
    void moderar_adminInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(ID_ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moderar(
                ID_PUBLICACION,
                ID_ADMIN,
                new ModerarPublicacionRequest(
                        EstadoPublicacion.OCULTA, "Contenido inapropiado")))
                .isInstanceOf(UsuarioAdminNoEncontradoException.class);
    }

    private void prepararAdminYPublicacion() {
        when(usuarioRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
        when(publicacionRepository.findByIdPublicacionForUpdate(ID_PUBLICACION))
                .thenReturn(Optional.of(publicacion));
        when(publicacionRepository.saveAndFlush(publicacion)).thenReturn(publicacion);
    }

    private Usuario usuario(Long id, String codigoRol, String nombreRol) {
        return Usuario.builder()
                .idUsuario(id)
                .nombre("Admin")
                .apellido("Sistema")
                .estado(EstadoUsuario.ACTIVO)
                .rolGlobal(RolGlobal.builder()
                        .codigo(codigoRol)
                        .nombre(nombreRol)
                        .build())
                .build();
    }
}
