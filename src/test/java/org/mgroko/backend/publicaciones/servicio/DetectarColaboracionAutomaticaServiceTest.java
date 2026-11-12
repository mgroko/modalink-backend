package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.ColaboracionAutomaticaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.proyeccion.CoincidenciaImagenProyeccion;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DetectarColaboracionAutomaticaServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ColaboracionAutomaticaRepository colaboracionRepository;

    @Mock
    private NotificacionService notificacionService;

    private DetectarColaboracionAutomaticaService servicio;
    private Publicacion actual;
    private Publicacion otra;

    @BeforeEach
    void setUp() {
        servicio = new DetectarColaboracionAutomaticaService(
                publicacionRepository,
                colaboracionRepository,
                notificacionService,
                RELOJ,
                true,
                8,
                10);

        actual = publicacion(10L, 7L, 1L, "Editorial Otoño");
        otra = publicacion(20L, 8L, 2L, "Campaña Verano");

        when(publicacionRepository.findByIdPublicacion(20L))
                .thenReturn(Optional.of(otra));
        when(publicacionRepository.existsColaborador(anyLong(), anyLong()))
                .thenReturn(false);
        when(colaboracionRepository.existsByParYEstados(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(colaboracionRepository.saveAndFlush(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(notificacionService.crear(
                any(Usuario.class), any(Perfil.class), anyString(), anyString(),
                anyString(), anyLong()))
                .thenAnswer(invocacion -> new Notificacion());
    }

    @Test
    void detectar_coincidenciaExacta_creaPropuestaYNotifica() {
        Imagen imagen = Imagen.builder()
                .idImagen(99L)
                .sha256Contenido("abc")
                .build();

        CoincidenciaImagenProyeccion coincidencia =
                mock(CoincidenciaImagenProyeccion.class);
        when(coincidencia.getIdPublicacion()).thenReturn(20L);
        when(coincidencia.getIdImagen()).thenReturn(100L);
        when(coincidencia.getSha256Contenido()).thenReturn("abc");
        when(coincidencia.getDistancia()).thenReturn(0);

        when(publicacionRepository.buscarCoincidenciasExactas(
                eq("abc"), eq(10L), eq(7L), eq(1L), eq(10)))
                .thenReturn(List.of(coincidencia));

        servicio.detectar(actual, List.of(imagen));

        ArgumentCaptor<ColaboracionAutomatica> captor =
                ArgumentCaptor.forClass(ColaboracionAutomatica.class);
        verify(colaboracionRepository).saveAndFlush(captor.capture());
        ColaboracionAutomatica creada = captor.getValue();

        assertEquals(EstadoColaboracionAutomatica.PENDIENTE, creada.getEstado());
        assertEquals(10L, creada.getPublicacionA().getIdPublicacion());
        assertEquals(20L, creada.getPublicacionB().getIdPublicacion());
        assertEquals(7L, creada.getPerfilA().getIdPerfil());
        assertEquals(8L, creada.getPerfilB().getIdPerfil());
        assertEquals("abc", creada.getSha256Evidencia());
        verify(notificacionService).registrarTodas(any());
    }

    @Test
    void detectar_siYaSonColaboradores_noCreaPropuesta() {
        Imagen imagen = Imagen.builder()
                .idImagen(99L)
                .sha256Contenido("abc")
                .build();

        CoincidenciaImagenProyeccion coincidencia =
                mock(CoincidenciaImagenProyeccion.class);
        when(coincidencia.getIdPublicacion()).thenReturn(20L);
        when(publicacionRepository.buscarCoincidenciasExactas(
                eq("abc"), eq(10L), eq(7L), eq(1L), eq(10)))
                .thenReturn(List.of(coincidencia));
        when(publicacionRepository.existsColaborador(10L, 8L))
                .thenReturn(true);

        servicio.detectar(actual, List.of(imagen));

        verify(colaboracionRepository, org.mockito.Mockito.never())
                .saveAndFlush(any());
    }

    private Publicacion publicacion(
            Long id,
            Long idPerfil,
            Long idUsuario,
            String titulo) {

        Perfil perfil = Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico("Perfil " + idPerfil)
                .estado(EstadoPerfil.Activo)
                .profesion(org.mgroko.backend.modelo.Profesion.builder()
                        .idProfesion(idPerfil)
                        .nombre("Profesión " + idPerfil)
                        .build())
                .usuario(Usuario.builder()
                        .idUsuario(idUsuario)
                        .estado(EstadoUsuario.Activo)
                        .build())
                .build();

        return Publicacion.builder()
                .idPublicacion(id)
                .titulo(titulo)
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .build();
    }
}
