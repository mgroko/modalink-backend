package org.mgroko.backend.notificaciones.mapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;

class NotificacionMapperTest {

    private Notificacion notificacion(
            String tipo,
            String entidadTipo,
            Long entidadId,
            Long idProyecto) {

        return Notificacion.builder()
                .idNotificacion(30L)
                .tipo(tipo)
                .mensaje("Mensaje de prueba")
                .entidadTipo(entidadTipo)
                .entidadId(entidadId)
                .idProyecto(idProyecto)
                .leida(false)
                .origen("SERVICIO")
                .fechaGenerada(LocalDateTime.of(2026, 10, 29, 12, 0))
                .perfilContexto(Perfil.builder().idPerfil(20L).build())
                .build();
    }

    @Test
    void mapeaTituloMensajeYPerfil() {
        NotificacionResponse response = NotificacionMapper.toResponse(
                notificacion("PROYECTO_CANCELADO", "PROYECTO", 105L, 105L),
                105L);

        assertThat(response.titulo()).isEqualTo("Proyecto cancelado");
        assertThat(response.mensaje()).isEqualTo("Mensaje de prueba");
        assertThat(response.idPerfilContexto()).isEqualTo(20L);
        assertThat(response.idProyecto()).isEqualTo(105L);
    }

    @Test
    void tipoDesconocido_devuelveTituloNull() {
        NotificacionResponse response = NotificacionMapper.toResponse(
                notificacion("TIPO_NUEVO", "PROYECTO", 105L, 105L),
                105L);

        assertThat(response.titulo()).isNull();
    }

    @Test
    void actividad_derivaIdActividad() {
        NotificacionResponse response = NotificacionMapper.toResponse(
                notificacion("ACTIVIDAD_ELIMINADA", "ACTIVIDAD", 66L, 105L),
                105L);

        assertThat(response.idActividad()).isEqualTo(66L);
        assertThat(response.idHito()).isNull();
        assertThat(response.idInvitacion()).isNull();
    }

    @Test
    void hito_derivaIdHito() {
        NotificacionResponse response = NotificacionMapper.toResponse(
                notificacion("HITO_CUMPLIDO", "HITO", 99L, 105L),
                105L);

        assertThat(response.idHito()).isEqualTo(99L);
        assertThat(response.idActividad()).isNull();
        assertThat(response.idInvitacion()).isNull();
    }

    @Test
    void invitacionGral_derivaIdInvitacion() {
        NotificacionResponse response = NotificacionMapper.toResponse(
                notificacion("INVITACION_RECHAZADA", "INVITACION_GRAL", 300L, 105L),
                105L);

        assertThat(response.idInvitacion()).isEqualTo(300L);
        assertThat(response.idActividad()).isNull();
        assertThat(response.idHito()).isNull();
    }

    @Test
    void invitacionActividad_derivaIdInvitacion() {
        NotificacionResponse response = NotificacionMapper.toResponse(
                notificacion("INVITACION_RECHAZADA", "INVITACION_ACTIVIDAD", 301L, 105L),
                105L);

        assertThat(response.idInvitacion()).isEqualTo(301L);
    }

    @Test
    void postulacion_noDerivaIdsEspecificos() {
        NotificacionResponse response = NotificacionMapper.toResponse(
                notificacion("POSTULACION_RECIBIDA", "POSTULACION_GRAL", 77L, 105L),
                105L);

        assertThat(response.idActividad()).isNull();
        assertThat(response.idHito()).isNull();
        assertThat(response.idInvitacion()).isNull();
        assertThat(response.entidadId()).isEqualTo(77L);
    }

    @Test
    void titulos_conocidos_seResuelvenPorTipo() {
        assertThat(NotificacionTitulos.tituloDe("AJUSTE_HORARIO"))
                .isEqualTo("Ajuste horario");
        assertThat(NotificacionTitulos.tituloDe("SOBRECOMPROMISO"))
                .isEqualTo("Posible sobrecompromiso");
        assertThat(NotificacionTitulos.tituloDe("HITO_CUMPLIDO"))
                .isEqualTo("Hito cumplido");
        assertThat(NotificacionTitulos.tituloDe("DESCONOCIDO")).isNull();
        assertThat(NotificacionTitulos.tituloDe(null)).isNull();
    }
}
