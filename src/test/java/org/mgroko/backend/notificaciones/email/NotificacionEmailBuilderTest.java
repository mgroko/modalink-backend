package org.mgroko.backend.notificaciones.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;

class NotificacionEmailBuilderTest {

    private final NotificacionEmailBuilder builder =
            new NotificacionEmailBuilder(new NotificacionLinkResolver());

    @Test
    void construyeAsuntoHtmlYTextoConLinkSeguro() {
        Notificacion notificacion = Notificacion.builder()
                .idNotificacion(1L)
                .tipo(NotificacionService.TIPO_PROYECTO_CONFIRMADO)
                .mensaje("Mensaje con <script>alert('x')</script>")
                .idProyecto(105L)
                .build();

        NotificacionEmailBuilder.EmailContent contenido = builder.construir(
                notificacion,
                105L,
                "raw-token",
                "http://localhost:5173",
                "persona@modalink.com");

        assertThat(contenido.asunto()).isEqualTo("[ModaLink] Proyecto confirmado");
        assertThat(contenido.html()).contains("&lt;script&gt;");
        assertThat(contenido.html()).doesNotContain("<script>");
        assertThat(contenido.html()).contains(
                "http://localhost:5173/auth/email#token=raw-token&amp;redirect=%2Fproyectos%2F105");
        assertThat(contenido.texto()).contains("Mensaje con <script>");
        assertThat(contenido.texto()).contains("persona@modalink.com");
    }

    @Test
    void sinDestinoUsaFallback() {
        Notificacion notificacion = Notificacion.builder()
                .idNotificacion(2L)
                .tipo(NotificacionService.TIPO_PROYECTO_CONFIRMADO)
                .mensaje("Mensaje")
                .build();

        NotificacionEmailBuilder.EmailContent contenido = builder.construir(
                notificacion,
                null,
                "token",
                "http://localhost:5173/",
                "persona@modalink.com");

        assertThat(contenido.html()).contains("redirect=%2Fnotificaciones");
    }
}
