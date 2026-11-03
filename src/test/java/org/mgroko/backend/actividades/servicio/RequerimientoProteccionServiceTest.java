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
        assertThat(service.estaProtegido(7L, true)).isFalse();
    }

    @Test
    void postulacionAceptada_protegeSiempre() {
        when(postulacionActividadRepository
                .existsByRequerimiento_IdRequerimientoAndEstado(
                        7L, EstadoSolicitud.ACEPTADA))
                .thenReturn(true);

        assertThat(service.estaProtegido(7L)).isTrue();
        assertThat(service.estaProtegido(7L, false)).isTrue();
    }

    @Test
    void postulacionPendiente_soloProtegeEnConfirmado() {
        when(postulacionActividadRepository
                .existsByRequerimiento_IdRequerimientoAndEstado(
                        7L, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThat(service.estaProtegido(7L)).isFalse();
        assertThat(service.estaProtegido(7L, true)).isTrue();
    }

    @Test
    void invitacionPendiente_soloProtegeEnConfirmado() {
        when(invitacionActividadRepository
                .existsByRequerimiento_IdRequerimientoAndEstado(
                        7L, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThat(service.estaProtegido(7L)).isFalse();
        assertThat(service.estaProtegido(7L, true)).isTrue();
    }

    @Test
    void asignacion_protegeSiempre() {
        when(asignacionActividadRepository
                .countTodasPorRequerimientoActividad(7L))
                .thenReturn(1L);

        assertThat(service.estaProtegido(7L)).isTrue();
    }
}
