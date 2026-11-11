package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicacionInteraccionNotificacionServiceTest {

    @Mock
    private NotificacionService notificacionService;

    private PublicacionInteraccionNotificacionService servicio;
    private Perfil actor;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        servicio = new PublicacionInteraccionNotificacionService(notificacionService);

        when(notificacionService.crear(any(), any(), any(), any(), any(), any()))
                .thenReturn(Notificacion.builder().build());

        actor = perfil(7L, "Actor", 1L, EstadoPerfil.Activo);
        Perfil duenio = perfil(8L, "Dueño", 2L, EstadoPerfil.Activo);
        Perfil colaborador = perfil(9L, "Colaborador", 3L, EstadoPerfil.Activo);
        Perfil colaboradorInactivo = perfil(10L, "Inactivo", 4L, EstadoPerfil.PendienteBaja);
        Perfil mismoActor = perfil(7L, "Actor", 1L, EstadoPerfil.Activo);

        publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .perfil(duenio)
                .colaboradores(new LinkedHashSet<>(
                        List.of(colaborador, colaboradorInactivo, mismoActor)))
                .build();
    }

    @Test
    void notificarComentario_avisaAlDuenioYColaboradoresActivosSinIncluirAlActor() {
        servicio.notificarComentario(publicacion, actor);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Notificacion>> captor =
                ArgumentCaptor.forClass(Collection.class);

        verify(notificacionService).registrarTodas(captor.capture());
        assertEquals(2, captor.getValue().size());

        verify(notificacionService).crear(
                eq(publicacion.getPerfil().getUsuario()),
                eq(publicacion.getPerfil()),
                eq(NotificacionService.TIPO_PUBLICACION_COMENTARIO),
                any(String.class),
                eq(NotificacionService.ENTIDAD_PUBLICACION),
                eq(99L));

        verify(notificacionService).crear(
                eq(publicacion.getColaboradores().iterator().next().getUsuario()),
                eq(publicacion.getColaboradores().iterator().next()),
                eq(NotificacionService.TIPO_PUBLICACION_COMENTARIO),
                any(String.class),
                eq(NotificacionService.ENTIDAD_PUBLICACION),
                eq(99L));
    }

    @Test
    void notificarMeGusta_usaElTipoYElMensajeDeMeGusta() {
        servicio.notificarMeGusta(publicacion, actor);

        verify(notificacionService).crear(
                eq(publicacion.getPerfil().getUsuario()),
                eq(publicacion.getPerfil()),
                eq(NotificacionService.TIPO_PUBLICACION_MEGUSTA),
                eq("El perfil 'Actor' le dio Me gusta a la publicación 'Editorial Otoño'."),
                eq(NotificacionService.ENTIDAD_PUBLICACION),
                eq(99L));
    }

    @Test
    void publicacionSinDestinatarios_noRegistraNotificaciones() {
        Publicacion propia = Publicacion.builder()
                .idPublicacion(100L)
                .titulo("Propia")
                .perfil(actor)
                .colaboradores(new LinkedHashSet<>())
                .build();

        servicio.notificarComentario(propia, actor);

        verify(notificacionService, org.mockito.Mockito.never())
                .registrarTodas(any());
    }

    private Perfil perfil(Long idPerfil, String nombre, Long idUsuario, EstadoPerfil estado) {
        Usuario usuario = Usuario.builder()
                .idUsuario(idUsuario)
                .estado(EstadoUsuario.Activo)
                .build();

        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombre)
                .estado(estado)
                .usuario(usuario)
                .build();
    }
}
