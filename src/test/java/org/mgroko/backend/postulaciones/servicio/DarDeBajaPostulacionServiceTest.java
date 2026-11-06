package org.mgroko.backend.postulaciones.servicio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.postulaciones.exception.PostulacionNoEncontradaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DarDeBajaPostulacionServiceTest {

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private PostulacionGralRepository postulacionGralRepository;
    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;

    private DarDeBajaPostulacionService service;

    private Perfil perfil;
    private Proyecto proyecto;
    private PostulacionGral postulacionGral;
    private PostulacionActividad postulacionActividad;

    @BeforeEach
    void setUp() {
        service = new DarDeBajaPostulacionService(
                perfilRepository, postulacionGralRepository, postulacionActividadRepository);

        perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Postulante")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder().idUsuario(1L).build())
                .build();

        proyecto = Proyecto.builder()
                .idProyecto(5L)
                .nombre("Desfile")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        RequerimientoGralProyecto requerimientoGral = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(30L)
                .proyecto(proyecto)
                .cantidad(1)
                .build();

        postulacionGral = PostulacionGral.builder()
                .idPostulacionGral(77L)
                .perfil(perfil)
                .requerimientoGral(requerimientoGral)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaPostulacion(LocalDateTime.of(2026, 10, 26, 10, 0))
                .build();

        Actividad actividad = Actividad.builder()
                .idActividad(40L)
                .nombre("Prueba de vestuario")
                .estado(EstadoActividad.PENDIENTE)
                .duracionMinutos(120)
                .planificacion(Planificacion.builder()
                        .idPlanificacion(20L)
                        .proyecto(proyecto)
                        .build())
                .build();

        RequerimientoActividad requerimientoActividad = RequerimientoActividad.builder()
                .idRequerimiento(50L)
                .actividad(actividad)
                .cantidad(1)
                .build();

        postulacionActividad = PostulacionActividad.builder()
                .idPostulacion(88L)
                .perfil(perfil)
                .requerimiento(requerimientoActividad)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaPostulacion(LocalDateTime.of(2026, 10, 26, 10, 0))
                .build();
    }

    @Test
    void bajaGeneral_pendiente_eliminaYHaceFlush() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(postulacionGralRepository.findByIdParaBaja(77L))
                .thenReturn(Optional.of(postulacionGral));

        service.darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L);

        verify(postulacionGralRepository).delete(postulacionGral);
        verify(postulacionGralRepository).flush();
    }

    @Test
    void bajaActividad_pendiente_eliminaYHaceFlush() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(postulacionActividadRepository.findByIdParaBaja(88L))
                .thenReturn(Optional.of(postulacionActividad));

        service.darDeBajaPostulacionActividad(1L, 10L, 5L, 88L);

        verify(postulacionActividadRepository).delete(postulacionActividad);
        verify(postulacionActividadRepository).flush();
    }

    @Test
    void bajaGeneral_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.darDeBajaPostulacionGeneral(1L, null, 5L, 77L))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void bajaGeneral_perfilDeOtroUsuario_lanzaNoEncontrado() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PerfilNoEncontradoException.class);

        verify(postulacionGralRepository, never()).delete(any(PostulacionGral.class));
    }

    @Test
    void bajaGeneral_perfilEnBaja_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.BAJA);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> service.darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PerfilEnBajaException.class);

        verify(postulacionGralRepository, never()).delete(any(PostulacionGral.class));
    }

    @Test
    void bajaGeneral_postulacionInexistente_lanzaNoEncontrada() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(postulacionGralRepository.findByIdParaBaja(77L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionNoEncontradaException.class);
    }

    @Test
    void bajaGeneral_postulacionDeOtroProyecto_lanzaNoEncontrada() {
        prepararPerfilYPostulacionGral();
        postulacionGral.getRequerimientoGral().setProyecto(
                Proyecto.builder().idProyecto(99L).build());

        assertThatThrownBy(() -> service.darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionNoEncontradaException.class);

        verify(postulacionGralRepository, never()).delete(any(PostulacionGral.class));
    }

    @Test
    void bajaGeneral_postulacionDeOtroPerfil_lanzaNoEncontrada() {
        prepararPerfilYPostulacionGral();
        postulacionGral.setPerfil(Perfil.builder().idPerfil(99L).build());

        assertThatThrownBy(() -> service.darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionNoEncontradaException.class);

        verify(postulacionGralRepository, never()).delete(any(PostulacionGral.class));
    }

    @Test
    void bajaGeneral_aceptada_lanzaYaGestionadaYNoElimina() {
        prepararPerfilYPostulacionGral();
        postulacionGral.setEstado(EstadoSolicitud.ACEPTADA);

        assertThatThrownBy(() -> service.darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionYaGestionadaException.class);

        verify(postulacionGralRepository, never()).delete(any(PostulacionGral.class));
    }

    @Test
    void bajaGeneral_rechazada_lanzaYaGestionadaYNoElimina() {
        prepararPerfilYPostulacionGral();
        postulacionGral.setEstado(EstadoSolicitud.RECHAZADA);

        assertThatThrownBy(() -> service.darDeBajaPostulacionGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionYaGestionadaException.class);

        verify(postulacionGralRepository, never()).delete(any(PostulacionGral.class));
    }

    @Test
    void bajaActividad_postulacionDeOtroProyecto_lanzaNoEncontrada() {
        prepararPerfilYPostulacionActividad();
        postulacionActividad.getRequerimiento().getActividad().getPlanificacion()
                .setProyecto(Proyecto.builder().idProyecto(99L).build());

        assertThatThrownBy(() -> service.darDeBajaPostulacionActividad(1L, 10L, 5L, 88L))
                .isInstanceOf(PostulacionNoEncontradaException.class);

        verify(postulacionActividadRepository, never()).delete(any(PostulacionActividad.class));
    }

    @Test
    void bajaActividad_yaGestionada_lanzaExcepcionYNoElimina() {
        prepararPerfilYPostulacionActividad();
        postulacionActividad.setEstado(EstadoSolicitud.ACEPTADA);

        assertThatThrownBy(() -> service.darDeBajaPostulacionActividad(1L, 10L, 5L, 88L))
                .isInstanceOf(PostulacionYaGestionadaException.class);

        verify(postulacionActividadRepository, never()).delete(any(PostulacionActividad.class));
    }

    private void prepararPerfilYPostulacionGral() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(postulacionGralRepository.findByIdParaBaja(77L))
                .thenReturn(Optional.of(postulacionGral));
    }

    private void prepararPerfilYPostulacionActividad() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(postulacionActividadRepository.findByIdParaBaja(88L))
                .thenReturn(Optional.of(postulacionActividad));
    }
}
