package org.mgroko.backend.notificaciones.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionEmailPolicyTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private NotificacionEmailPolicy policy;

    @Test
    void usuarioActivoConCorreoYTipoAlta_esElegible() {
        Notificacion notificacion = notificacion("PROYECTO_CONFIRMADO");
        Usuario usuario = usuario(EstadoUsuario.ACTIVO, "persona@modalink.com");
        when(notificacionRepository.esTipoEmailPrioridadAlta("PROYECTO_CONFIRMADO"))
                .thenReturn(true);

        assertThat(policy.esElegible(notificacion, usuario)).isTrue();
    }

    @Test
    void usuarioSinCorreo_noEsElegible() {
        Notificacion notificacion = notificacion("PROYECTO_CONFIRMADO");
        Usuario usuario = usuario(EstadoUsuario.ACTIVO, " ");

        assertThat(policy.motivoNoElegible(notificacion, usuario))
                .isEqualTo("USUARIO_SIN_CORREO");
    }

    @Test
    void usuarioDadoDeBaja_noEsElegible() {
        Notificacion notificacion = notificacion("PROYECTO_CONFIRMADO");
        Usuario usuario = usuario(EstadoUsuario.BAJA, "persona@modalink.com");

        assertThat(policy.motivoNoElegible(notificacion, usuario))
                .isEqualTo("USUARIO_NO_ACTIVO");
    }

    @Test
    void tipoNoAlta_noEsElegible() {
        Notificacion notificacion = notificacion("PUBLICACION_MEGUSTA");
        Usuario usuario = usuario(EstadoUsuario.ACTIVO, "persona@modalink.com");
        when(notificacionRepository.esTipoEmailPrioridadAlta("PUBLICACION_MEGUSTA"))
                .thenReturn(false);

        assertThat(policy.motivoNoElegible(notificacion, usuario))
                .isEqualTo("TIPO_NO_ELEGIBLE");
    }

    private Notificacion notificacion(String tipo) {
        return Notificacion.builder()
                .idNotificacion(1L)
                .tipo(tipo)
                .mensaje("Mensaje")
                .build();
    }

    private Usuario usuario(EstadoUsuario estado, String correo) {
        return Usuario.builder()
                .idUsuario(1L)
                .estado(estado)
                .correo(correo)
                .build();
    }
}
