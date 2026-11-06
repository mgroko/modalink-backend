package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BajaVoluntariaProyectoPolicyTest {

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @InjectMocks
    private BajaVoluntariaProyectoPolicy policy;

    private Proyecto proyecto;
    private RolProyecto rolDirector;
    private RolProyecto rolMiembro;

    @BeforeEach
    void setUp() {
        proyecto = Proyecto.builder()
                .idProyecto(100L)
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        rolDirector = RolProyecto.builder().idRolProyecto(1L).codigo("DIRECTOR").build();
        rolMiembro = RolProyecto.builder().idRolProyecto(2L).codigo("MIEMBRO").build();
    }

    @Test
    void miembroActivo_enProyectoPublicado_puedeDarseDeBaja() {
        MiembroProyecto miembro = miembro(rolMiembro, EstadoParticipacion.ACTIVO);

        assertThat(policy.puedeDarseDeBaja(proyecto, miembro)).isTrue();
    }

    @Test
    void directorConOtroDirector_puedeDarseDeBaja() {
        MiembroProyecto miembro = miembro(rolDirector, EstadoParticipacion.ACTIVO);
        when(miembroProyectoRepository.countOtrosDirectoresActivos(100L, 900L))
                .thenReturn(1L);

        assertThat(policy.puedeDarseDeBaja(proyecto, miembro)).isTrue();
    }

    @Test
    void directorUnico_noPuedeDarseDeBaja() {
        MiembroProyecto miembro = miembro(rolDirector, EstadoParticipacion.ACTIVO);
        when(miembroProyectoRepository.countOtrosDirectoresActivos(100L, 900L))
                .thenReturn(0L);

        assertThat(policy.puedeDarseDeBaja(proyecto, miembro)).isFalse();
    }

    @Test
    void miembroNoActivo_noPuedeDarseDeBaja() {
        MiembroProyecto miembro = miembro(rolMiembro, EstadoParticipacion.BAJA_VOLUNTARIA);

        assertThat(policy.puedeDarseDeBaja(proyecto, miembro)).isFalse();
    }

    @Test
    void proyectoNoPublicadoNiConfirmado_noPuedeDarseDeBaja() {
        proyecto.setEstado(EstadoProyecto.BORRADOR);
        MiembroProyecto miembro = miembro(rolMiembro, EstadoParticipacion.ACTIVO);

        assertThat(policy.puedeDarseDeBaja(proyecto, miembro)).isFalse();
    }

    @Test
    void sinMiembro_noPuedeDarseDeBaja() {
        assertThat(policy.puedeDarseDeBaja(proyecto, null)).isFalse();
    }

    private MiembroProyecto miembro(RolProyecto rol, EstadoParticipacion estado) {
        return MiembroProyecto.builder()
                .idMiembro(900L)
                .proyecto(proyecto)
                .rolProyecto(rol)
                .estadoParticipacion(estado)
                .build();
    }
}
