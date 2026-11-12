package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoConfirmacionColaboracion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.repositorio.ColaboracionAutomaticaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionarColaboracionAutomaticaServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PerfilActivoService perfilActivoService;

    @Mock
    private ColaboracionAutomaticaRepository colaboracionRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private ColaboradorPublicacionOrigenService colaboradorOrigenService;

    private GestionarColaboracionAutomaticaService servicio;
    private Perfil perfilA;
    private Perfil perfilB;
    private Publicacion publicacionA;
    private Publicacion publicacionB;
    private ColaboracionAutomatica colaboracion;

    @BeforeEach
    void setUp() {
        servicio = new GestionarColaboracionAutomaticaService(
                perfilActivoService,
                colaboracionRepository,
                publicacionRepository,
                notificacionService,
                colaboradorOrigenService,
                RELOJ);

        perfilA = perfil(7L, 1L, "Luna");
        perfilB = perfil(8L, 2L, "Sol");
        publicacionA = publicacion(10L, perfilA, "Editorial Otoño");
        publicacionB = publicacion(20L, perfilB, "Campaña Verano");

        colaboracion = ColaboracionAutomatica.builder()
                .idColaboracionAutomatica(50L)
                .publicacionA(publicacionA)
                .publicacionB(publicacionB)
                .perfilA(perfilA)
                .perfilB(perfilB)
                .estado(EstadoColaboracionAutomatica.PENDIENTE)
                .estadoPerfilA(EstadoConfirmacionColaboracion.PENDIENTE)
                .estadoPerfilB(EstadoConfirmacionColaboracion.PENDIENTE)
                .build();

        when(colaboracionRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(colaboracion));
        when(colaboracionRepository.saveAndFlush(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(publicacionRepository.findByIdPublicacionForUpdate(10L))
                .thenReturn(Optional.of(publicacionA));
        when(publicacionRepository.findByIdPublicacionForUpdate(20L))
                .thenReturn(Optional.of(publicacionB));
        when(publicacionRepository.existsColaborador(anyLong(), anyLong()))
                .thenReturn(false);
        when(publicacionRepository.saveAndFlush(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(notificacionService.crear(
                any(Usuario.class), any(Perfil.class), any(), any(), any(), anyLong()))
                .thenReturn(mock(Notificacion.class));
    }

    @Test
    void confirmar_primeraParte_dejaPendienteSinAgregarColaboradores() {
        when(perfilActivoService.resolver(1L, 7L)).thenReturn(perfilA);

        servicio.confirmar(1L, 7L, 50L);

        assertEquals(EstadoConfirmacionColaboracion.CONFIRMADA,
                colaboracion.getEstadoPerfilA());
        assertEquals(EstadoColaboracionAutomatica.PENDIENTE,
                colaboracion.getEstado());
        verify(publicacionRepository, never()).saveAndFlush(any());
        verify(colaboradorOrigenService, never()).registrarAutomatica(
                anyLong(), anyLong(), anyLong());
    }

    @Test
    void confirmar_segundaParte_agregaColaboradoresYConfirma() {
        colaboracion.setEstadoPerfilA(EstadoConfirmacionColaboracion.CONFIRMADA);
        when(perfilActivoService.resolver(2L, 8L)).thenReturn(perfilB);

        servicio.confirmar(2L, 8L, 50L);

        assertEquals(EstadoConfirmacionColaboracion.CONFIRMADA,
                colaboracion.getEstadoPerfilB());
        assertEquals(EstadoColaboracionAutomatica.CONFIRMADA,
                colaboracion.getEstado());
        verify(publicacionRepository).saveAndFlush(publicacionA);
        verify(publicacionRepository).saveAndFlush(publicacionB);
        verify(colaboradorOrigenService).registrarAutomatica(10L, 8L, 50L);
        verify(colaboradorOrigenService).registrarAutomatica(20L, 7L, 50L);
    }

    @Test
    void rechazar_dejaColaboracionRechazadaSinColaboradores() {
        when(perfilActivoService.resolver(1L, 7L)).thenReturn(perfilA);

        servicio.rechazar(1L, 7L, 50L);

        assertEquals(EstadoConfirmacionColaboracion.RECHAZADA,
                colaboracion.getEstadoPerfilA());
        assertEquals(EstadoColaboracionAutomatica.RECHAZADA,
                colaboracion.getEstado());
        verify(publicacionRepository, never()).saveAndFlush(any());
    }

    private Perfil perfil(Long id, Long idUsuario, String nombre) {
        return Perfil.builder()
                .idPerfil(id)
                .nombreArtistico(nombre)
                .estado(EstadoPerfil.Activo)
                .profesion(org.mgroko.backend.modelo.Profesion.builder()
                        .idProfesion(id)
                        .nombre("Profesión " + id)
                        .build())
                .usuario(Usuario.builder()
                        .idUsuario(idUsuario)
                        .estado(EstadoUsuario.Activo)
                        .build())
                .build();
    }

    private Publicacion publicacion(Long id, Perfil perfil, String titulo) {
        return Publicacion.builder()
                .idPublicacion(id)
                .titulo(titulo)
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .build();
    }
}
