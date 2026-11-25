package org.mgroko.backend.notificaciones.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.ColaboracionAutomaticaRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionImagenResolverTest {

    private static final String BASE = "http://localhost:8080/";

    @Mock
    private ColaboracionAutomaticaRepository colaboracionRepository;

    @Test
    void resuelveLaUrlRelativaConLaBaseConfigurada() {
        when(colaboracionRepository.findImagenUrlById(9L))
                .thenReturn(Optional.of("/uploads/publicaciones/a.webp"));

        NotificacionImagenResolver resolver =
                new NotificacionImagenResolver(colaboracionRepository, BASE);

        assertThat(resolver.resolver(notificacion(
                NotificacionService.TIPO_COLABORACION_AUTOMATICA_DETECTADA, 9L)))
                .contains("http://localhost:8080/uploads/publicaciones/a.webp");
    }

    @Test
    void respetaUnaUrlQueYaEsAbsoluta() {
        when(colaboracionRepository.findImagenUrlById(9L))
                .thenReturn(Optional.of("https://cdn.modalink.com/a.webp"));

        NotificacionImagenResolver resolver =
                new NotificacionImagenResolver(colaboracionRepository, BASE);

        assertThat(resolver.resolver(notificacion(
                NotificacionService.TIPO_COLABORACION_AUTOMATICA_CONFIRMADA, 9L)))
                .contains("https://cdn.modalink.com/a.webp");
    }

    @Test
    void devuelveVacioSiLaColaboracionNoTieneImagen() {
        when(colaboracionRepository.findImagenUrlById(9L))
                .thenReturn(Optional.empty());

        NotificacionImagenResolver resolver =
                new NotificacionImagenResolver(colaboracionRepository, BASE);

        assertThat(resolver.resolver(notificacion(
                NotificacionService.TIPO_COLABORACION_AUTOMATICA_DETECTADA, 9L)))
                .isEmpty();
    }

    @Test
    void ignoraNotificacionesQueNoSonDeColaboracion() {
        NotificacionImagenResolver resolver =
                new NotificacionImagenResolver(colaboracionRepository, BASE);

        assertThat(resolver.resolver(notificacion(
                NotificacionService.TIPO_PROYECTO_CONFIRMADO, 9L)))
                .isEmpty();
        verifyNoInteractions(colaboracionRepository);
    }

    @Test
    void ignoraNotificacionesDeOtraEntidad() {
        NotificacionImagenResolver resolver =
                new NotificacionImagenResolver(colaboracionRepository, BASE);

        Notificacion notificacion = Notificacion.builder()
                .tipo(NotificacionService.TIPO_COLABORACION_AUTOMATICA_DETECTADA)
                .entidadTipo(NotificacionService.ENTIDAD_PUBLICACION)
                .entidadId(9L)
                .build();

        assertThat(resolver.resolver(notificacion)).isEmpty();
        verifyNoInteractions(colaboracionRepository);
    }

    @Test
    void devuelveVacioSiLaNotificacionEsNula() {
        NotificacionImagenResolver resolver =
                new NotificacionImagenResolver(colaboracionRepository, BASE);

        assertThat(resolver.resolver(null)).isEmpty();
        verifyNoInteractions(colaboracionRepository);
    }

    private Notificacion notificacion(String tipo, Long entidadId) {
        return Notificacion.builder()
                .tipo(tipo)
                .entidadTipo(NotificacionService.ENTIDAD_COLABORACION_AUTOMATICA)
                .entidadId(entidadId)
                .build();
    }
}
