package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionInvalidaException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgregarColaboradorPropioServiceTest {

    @Mock
    private PerfilActivoService perfilActivoService;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ColaboradorPublicacionOrigenService colaboradorOrigenService;

    @Mock
    private ColaboracionAutomaticaLifecycleService colaboracionLifecycleService;

    private AgregarColaboradorPropioService servicio;
    private Usuario usuario;
    private Perfil perfilActivo;
    private Perfil perfilDestino;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        servicio = new AgregarColaboradorPropioService(
                perfilActivoService,
                publicacionRepository,
                perfilRepository,
                colaboradorOrigenService,
                colaboracionLifecycleService);

        usuario = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Activo)
                .build();

        perfilActivo = Perfil.builder()
                .idPerfil(7L)
                .usuario(usuario)
                .estado(EstadoPerfil.Activo)
                .build();

        perfilDestino = Perfil.builder()
                .idPerfil(8L)
                .usuario(usuario)
                .estado(EstadoPerfil.Activo)
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .perfil(perfilActivo)
                .build();

        when(perfilActivoService.resolver(1L, 7L)).thenReturn(perfilActivo);
        when(publicacionRepository.findByIdPublicacionAndPerfilIdPerfil(99L, 7L))
                .thenReturn(Optional.of(publicacion));
        when(perfilRepository.findById(8L)).thenReturn(Optional.of(perfilDestino));
        when(publicacionRepository.existsColaborador(99L, 8L)).thenReturn(false);
    }

    @Test
    void agregar_mismoUsuarioAgregaColaboradorYRegistraOrigenManual() {
        servicio.agregar(1L, 7L, 99L, 8L);

        assertTrue(publicacion.getColaboradores().contains(perfilDestino));
        verify(publicacionRepository).saveAndFlush(publicacion);
        verify(colaboradorOrigenService).registrarManual(99L, 8L);
        verify(colaboracionLifecycleService)
                .cancelarPendientesEntrePerfil(99L, 8L);
    }

    @Test
    void agregar_perfilDeOtroUsuario_lanzaInvalida() {
        perfilDestino.setUsuario(Usuario.builder()
                .idUsuario(2L)
                .estado(EstadoUsuario.Activo)
                .build());

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.agregar(1L, 7L, 99L, 8L));

        verify(publicacionRepository, never()).saveAndFlush(any());
        assertEquals("PERFIL_PROPIO_INVALIDO", ex.getCodigo());
    }

    @Test
    void agregar_mismoPerfil_lanzaInvalida() {
        when(perfilRepository.findById(7L)).thenReturn(Optional.of(perfilActivo));

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.agregar(1L, 7L, 99L, 7L));

        verify(publicacionRepository, never()).saveAndFlush(any());
        assertEquals("PERFIL_PROPIO_INVALIDO", ex.getCodigo());
    }

    @Test
    void agregar_perfilDestinoInactivo_lanzaInvalida() {
        perfilDestino.setEstado(EstadoPerfil.PendienteBaja);

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.agregar(1L, 7L, 99L, 8L));

        verify(publicacionRepository, never()).saveAndFlush(any());
        assertEquals("PERFIL_PROPIO_INVALIDO", ex.getCodigo());
    }

    @Test
    void agregar_yaEsColaborador_noDuplicaNiFalla() {
        publicacion.getColaboradores().add(perfilDestino);
        when(publicacionRepository.existsColaborador(99L, 8L)).thenReturn(true);

        servicio.agregar(1L, 7L, 99L, 8L);

        verify(publicacionRepository, never()).saveAndFlush(any());
        verify(colaboradorOrigenService).registrarManual(99L, 8L);
    }
}
