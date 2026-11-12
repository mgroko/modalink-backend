package org.mgroko.backend.notificaciones.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;

class NotificacionLinkResolverTest {

    private final NotificacionLinkResolver resolver = new NotificacionLinkResolver();

    @Test
    void proyecto_usaRutaDeProyecto() {
        Notificacion notificacion = notificacion(
                NotificacionService.TIPO_PROYECTO_CONFIRMADO, "PROYECTO", 105L, null);

        assertThat(resolver.resolver(notificacion, 105L)).isEqualTo("/proyectos/105");
    }

    @Test
    void actividadAsignada_usaRutaDeActividad() {
        Notificacion notificacion = notificacion(
                NotificacionService.TIPO_ACTIVIDAD_ASIGNADA, "ACTIVIDAD", 66L, null);

        assertThat(resolver.resolver(notificacion, 105L))
                .isEqualTo("/proyectos/105/actividades/66");
    }

    @Test
    void contrato_usaRutaDeContrato() {
        Notificacion notificacion = notificacion(
                NotificacionService.TIPO_CONTRATO_PENDIENTE_FIRMA, "CONTRATO", 77L, null);

        assertThat(resolver.resolver(notificacion, 105L))
                .isEqualTo("/proyectos/105/contratos/77");
    }

    @Test
    void solicitudConexion_usaRutaDeMiRed() {
        Notificacion notificacion = notificacion(
                NotificacionService.TIPO_SOLICITUD_CONEXION_RECIBIDA, "SOLICITUD_CONEXION", 1L, null);

        assertThat(resolver.resolver(notificacion, null))
                .isEqualTo("/mi-red?tab=recibidas");
    }

    @Test
    void sinProyecto_usaFallback() {
        Notificacion notificacion = notificacion(
                NotificacionService.TIPO_PROYECTO_CONFIRMADO, "PROYECTO", 105L, null);

        assertThat(resolver.resolver(notificacion, null))
                .isEqualTo(NotificacionLinkResolver.FALLBACK);
    }

    private Notificacion notificacion(String tipo, String entidadTipo, Long entidadId, Long idProyecto) {
        return Notificacion.builder()
                .idNotificacion(1L)
                .tipo(tipo)
                .entidadTipo(entidadTipo)
                .entidadId(entidadId)
                .idProyecto(idProyecto)
                .mensaje("Mensaje")
                .build();
    }
}
