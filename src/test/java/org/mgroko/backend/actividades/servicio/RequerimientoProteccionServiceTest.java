package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequerimientoProteccionServiceTest {

    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;
    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;

    @InjectMocks
    private RequerimientoProteccionService service;

    @Test
    void sinVinculos_noEstaProtegido() {
        assertThat(service.estaProtegido(7L)).isFalse();
    }

    @Test
    void asignacionActiva_protege() {
        when(asignacionActividadRepository
                .countActivasPorRequerimientoActividad(7L))
                .thenReturn(1L);

        assertThat(service.estaProtegido(7L)).isTrue();
    }

    @Test
    void asignacionHistorica_noProtege() {
        when(asignacionActividadRepository
                .countActivasPorRequerimientoActividad(7L))
                .thenReturn(0L);

        assertThat(service.estaProtegido(7L)).isFalse();
    }

    @Test
    void postulacionPendiente_protege() {
        when(postulacionActividadRepository
                .existsByRequerimiento_IdRequerimientoAndEstado(
                        7L, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThat(service.estaProtegido(7L)).isTrue();
    }

    @Test
    void invitacionPendiente_protege() {
        when(invitacionActividadRepository
                .existsByRequerimiento_IdRequerimientoAndEstado(
                        7L, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThat(service.estaProtegido(7L)).isTrue();
    }

    @Test
    void postulacionAceptadaDeInactivo_noProtege() {
        assertThat(service.estaProtegido(7L)).isFalse();
    }
}
