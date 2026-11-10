package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProyectoVisibilidadServiceTest {

    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_PERFIL = 10L;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private ConexionRepository conexionRepository;

    @InjectMocks
    private ProyectoVisibilidadService servicio;

    private Proyecto proyecto(EstadoProyecto estado, Privacidad privacidad) {
        return Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .estado(estado)
                .privacidad(privacidad)
                .build();
    }

    private void sinMembresia() {
        when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(
                        ID_PROYECTO, ID_PERFIL, EstadoParticipacion.ACTIVO))
                .thenReturn(false);
    }

    @Test
    void publicoNoBorradorNiCancelado_esVisible() {
        assertThat(servicio.esVisiblePorPrivacidad(
                proyecto(EstadoProyecto.PUBLICADO, Privacidad.PUBLICO), ID_PERFIL))
                .isTrue();
    }

    @Test
    void publicoBorrador_noEsVisible() {
        assertThat(servicio.esVisiblePorPrivacidad(
                proyecto(EstadoProyecto.BORRADOR, Privacidad.PUBLICO), ID_PERFIL))
                .isFalse();
    }

    @Test
    void publicoCancelado_noEsVisible() {
        assertThat(servicio.esVisiblePorPrivacidad(
                proyecto(EstadoProyecto.CANCELADO, Privacidad.PUBLICO), ID_PERFIL))
                .isFalse();
    }

    @Test
    void privadoPublicadoConContactoDeDirector_esVisible() {
        when(conexionRepository.existeContactoActivoConDirectorDeProyecto(
                ID_PERFIL, ID_PROYECTO)).thenReturn(true);

        assertThat(servicio.esVisiblePorPrivacidad(
                proyecto(EstadoProyecto.PUBLICADO, Privacidad.PRIVADO), ID_PERFIL))
                .isTrue();
    }

    @Test
    void privadoPublicadoSinContactoDeDirector_noEsVisible() {
        when(conexionRepository.existeContactoActivoConDirectorDeProyecto(
                ID_PERFIL, ID_PROYECTO)).thenReturn(false);

        assertThat(servicio.esVisiblePorPrivacidad(
                proyecto(EstadoProyecto.PUBLICADO, Privacidad.PRIVADO), ID_PERFIL))
                .isFalse();
    }

    @Test
    void privadoBorradorConContacto_noEsVisible() {
        assertThat(servicio.esVisiblePorPrivacidad(
                proyecto(EstadoProyecto.BORRADOR, Privacidad.PRIVADO), ID_PERFIL))
                .isFalse();
    }

    @Test
    void privadoCanceladoConContacto_noEsVisible() {
        assertThat(servicio.esVisiblePorPrivacidad(
                proyecto(EstadoProyecto.CANCELADO, Privacidad.PRIVADO), ID_PERFIL))
                .isFalse();
    }

    @Test
    void puedeVer_miembroActivo_esVisibleAunqueSeaBorrador() {
        when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(
                        ID_PROYECTO, ID_PERFIL, EstadoParticipacion.ACTIVO))
                .thenReturn(true);

        assertThat(servicio.puedeVer(
                proyecto(EstadoProyecto.BORRADOR, Privacidad.PRIVADO), ID_PERFIL))
                .isTrue();
    }

    @Test
    void puedeVer_exMiembroDeCancelado_esVisible() {
        sinMembresia();
        when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfil(ID_PROYECTO, ID_PERFIL))
                .thenReturn(true);

        assertThat(servicio.puedeVer(
                proyecto(EstadoProyecto.CANCELADO, Privacidad.PRIVADO), ID_PERFIL))
                .isTrue();
    }

    @Test
    void puedeVer_contactoDeDirectorEnProyectoPublicado_esVisible() {
        sinMembresia();
        when(conexionRepository.existeContactoActivoConDirectorDeProyecto(
                ID_PERFIL, ID_PROYECTO)).thenReturn(true);

        assertThat(servicio.puedeVer(
                proyecto(EstadoProyecto.PUBLICADO, Privacidad.PRIVADO), ID_PERFIL))
                .isTrue();
    }

    @Test
    void puedeVer_noContactoEnProyectoPublicado_esVisiblePorSerPublico() {
        sinMembresia();

        assertThat(servicio.puedeVer(
                proyecto(EstadoProyecto.PUBLICADO, Privacidad.PUBLICO), ID_PERFIL))
                .isTrue();
    }
}
