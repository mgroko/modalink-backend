package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PermisoProyecto;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.EstadoProyectoInvalidoException;
import org.mgroko.backend.proyectos.exception.InformacionBasicaIncompletaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicarProyectoServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private MoodboardRepository moodboardRepository;

    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;

    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;

    @Mock
    private RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;

    @Mock
    private ObjetivoRepository objetivoRepository;

    @Mock
    private CupoRequerimientoService cupoRequerimientoService;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @InjectMocks
    private PublicarProyectoService publicarProyectoService;

    private Perfil perfil;
    private RolProyecto rolDirector;
    private Proyecto proyectoBorrador;
    private MiembroProyecto miembroDirector;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().idUsuario(1L).build();
        perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .build();

        rolDirector = RolProyecto.builder()
                .idRolProyecto(1L)
                .codigo("DIRECTOR")
                .nombre("Director de proyecto")
                .permisos(Set.of(
                        PermisoProyecto.builder().idPermisoProyecto(1L).nombre("VER_PROYECTO").build(),
                        PermisoProyecto.builder().idPermisoProyecto(4L).nombre("PUBLICAR_PROYECTO").build()))
                .build();

        proyectoBorrador = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile 2026")
                .descripcion("Pasarela invierno")
                .estado(EstadoProyecto.BORRADOR)
                .privacidad(Privacidad.PUBLICO)
                .aceptaPostulacionGral(true)
                .ubicacion(Ubicacion.builder()
                        .idUbicacion(50L)
                        .ciudad(Ciudad.builder().nombre("La Plata").build())
                        .build())
                .build();

        miembroDirector = MiembroProyecto.builder()
                .proyecto(proyectoBorrador)
                .perfil(perfil)
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();

        lenient().when(cupoRequerimientoService.calcularGeneral(any()))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
    }

    private void prepararCaminoFeliz() {
        prepararCaminoFeliz(true);
    }

    private void prepararCaminoFeliz(boolean conRequerimientoGral) {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(Planificacion.builder()
                        .proyecto(proyectoBorrador)
                        .fechaEntrega(LocalDate.now().plusDays(30))
                        .build()));
        when(moodboardRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(Moodboard.builder()
                        .idMoodboard(7L)
                        .proyecto(proyectoBorrador)
                        .build()));
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L))
                .thenReturn(conRequerimientoGral
                        ? List.of(RequerimientoGralProyecto.builder()
                                .idRequerimientoGral(20L)
                                .proyecto(proyectoBorrador)
                                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                                .cantidad(2)
                                .build())
                        : List.of());
        when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(miembroProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of(miembroDirector));
        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L))
                .thenReturn(Optional.of(miembroDirector));
        when(objetivoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of());
    }

    @Test
    void publicar_borradorConInformacionCompleta_cambiaEstadoAPublicado() {
        prepararCaminoFeliz();

        ProyectoResponse response = publicarProyectoService.publicar(1L, 10L, 100L);

        assertThat(response).isNotNull();
        assertThat(response.idProyecto()).isEqualTo(100L);
        assertThat(response.estado()).isEqualTo("Publicado");
        assertThat(response.nombre()).isEqualTo("Desfile 2026");
        assertThat(response.idDirector()).isEqualTo(10L);
        assertThat(response.nombreDirector()).isEqualTo("Luna Diseños");
        assertThat(response.fechaEntregaEstipulada()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(response.moodboard()).isNotNull();
        assertThat(response.moodboard().idMoodboard()).isEqualTo(7L);
        assertThat(response.requerimientosGral()).hasSize(1);
        assertThat(response.rolUsuario()).isEqualTo(RolProyectoUsuario.DIRECTOR);
        assertThat(response.esMiembro()).isTrue();
        assertThat(response.permisos()).contains("PUBLICAR_PROYECTO", "VER_PROYECTO");
        assertThat(response.elegibleParaPostularse()).isFalse();

        assertThat(proyectoBorrador.getEstado()).isEqualTo(EstadoProyecto.PUBLICADO);
        verify(proyectoSecurityService).validarPermiso(100L, 10L, "PUBLICAR_PROYECTO");
        verify(proyectoRepository).save(proyectoBorrador);
    }

    @Test
    void publicar_sinRequerimientoGralConRequerimientoDeActividad_cambiaEstadoAPublicado() {
        prepararCaminoFeliz(false);
        when(requerimientoActividadRepository.existsByProyectoExcluyendoEstado(
                100L, EstadoActividad.CANCELADA)).thenReturn(true);

        ProyectoResponse response = publicarProyectoService.publicar(1L, 10L, 100L);

        assertThat(response.estado()).isEqualTo("Publicado");
        assertThat(response.requerimientosGral()).isEmpty();
        assertThat(proyectoBorrador.getEstado()).isEqualTo(EstadoProyecto.PUBLICADO);
        verify(proyectoRepository).save(proyectoBorrador);
    }

    @Test
    void publicar_soloConRequerimientoDeActividadCancelado_lanzaExcepcionDeInformacionIncompleta() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(Planificacion.builder()
                        .proyecto(proyectoBorrador)
                        .fechaEntrega(LocalDate.now().plusDays(30))
                        .build()));
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.of(
                Moodboard.builder().idMoodboard(7L).proyecto(proyectoBorrador).build()));
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of());
        when(requerimientoActividadRepository.existsByProyectoExcluyendoEstado(
                100L, EstadoActividad.CANCELADA)).thenReturn(false);

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 100L))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("requerimiento")
                .hasMessageContaining("actividad");

        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }

    @Test
    void publicar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, null, 100L))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void publicar_perfilNoExiste_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 99L, 100L))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void publicar_perfilEnBaja_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 100L))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void publicar_proyectoInexistente_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 999L))
                .isInstanceOf(ProyectoNoEncontradoException.class);
    }

    @Test
    void publicar_sinPermisoPublicar_lanzaAccesoDenegado() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        doThrow(new AccesoDenegadoProyectoException("No tienes el permiso 'PUBLICAR_PROYECTO' en este proyecto."))
                .when(proyectoSecurityService).validarPermiso(100L, 10L, "PUBLICAR_PROYECTO");

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 100L))
                .isInstanceOf(AccesoDenegadoProyectoException.class)
                .hasMessageContaining("PUBLICAR_PROYECTO");

        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }

    @Test
    void publicar_proyectoNoEnBorrador_lanzaExcepcion() {
        proyectoBorrador.setEstado(EstadoProyecto.PUBLICADO);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 100L))
                .isInstanceOf(EstadoProyectoInvalidoException.class)
                .hasMessageContaining("Borrador")
                .hasMessageContaining("Publicado");

        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }

    @Test
    void publicar_sinUbicacion_lanzaExcepcionDeInformacionIncompleta() {
        proyectoBorrador.setUbicacion(null);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(Planificacion.builder()
                        .proyecto(proyectoBorrador)
                        .fechaEntrega(LocalDate.now().plusDays(30))
                        .build()));
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.of(
                Moodboard.builder().idMoodboard(7L).proyecto(proyectoBorrador).build()));
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L))
                .thenReturn(List.of(RequerimientoGralProyecto.builder().idRequerimientoGral(20L).build()));

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 100L))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("ubicación");

        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }

    @Test
    void publicar_sinFechaDeEntrega_lanzaExcepcionDeInformacionIncompleta() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(Planificacion.builder()
                        .proyecto(proyectoBorrador)
                        .fechaEntrega(null)
                        .build()));
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.of(
                Moodboard.builder().idMoodboard(7L).proyecto(proyectoBorrador).build()));
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L))
                .thenReturn(List.of(RequerimientoGralProyecto.builder().idRequerimientoGral(20L).build()));

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 100L))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("fecha de entrega");
    }

    @Test
    void publicar_sinPlanificacion_lanzaExcepcionDeInformacionIncompleta() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(planificacionRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.empty());
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.of(
                Moodboard.builder().idMoodboard(7L).proyecto(proyectoBorrador).build()));
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L))
                .thenReturn(List.of(RequerimientoGralProyecto.builder().idRequerimientoGral(20L).build()));

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 100L))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("fecha de entrega");
    }

    @Test
    void publicar_sinMoodboard_lanzaExcepcionDeInformacionIncompleta() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(Planificacion.builder()
                        .proyecto(proyectoBorrador)
                        .fechaEntrega(LocalDate.now().plusDays(30))
                        .build()));
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.empty());
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L))
                .thenReturn(List.of(RequerimientoGralProyecto.builder().idRequerimientoGral(20L).build()));

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 100L))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("moodboard");
    }

    @Test
    void publicar_sinRequerimientosGralNiDeActividad_lanzaExcepcionDeInformacionIncompleta() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(Planificacion.builder()
                        .proyecto(proyectoBorrador)
                        .fechaEntrega(LocalDate.now().plusDays(30))
                        .build()));
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.of(
                Moodboard.builder().idMoodboard(7L).proyecto(proyectoBorrador).build()));
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of());
        when(requerimientoActividadRepository.existsByProyectoExcluyendoEstado(
                100L, EstadoActividad.CANCELADA)).thenReturn(false);

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 100L))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("requerimiento")
                .hasMessageContaining("general o de actividad");
    }

    @Test
    void publicar_variosCamposFaltantes_listaTodosEnElMensaje() {
        proyectoBorrador.setUbicacion(null);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(planificacionRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.empty());
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.empty());
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of());
        when(requerimientoActividadRepository.existsByProyectoExcluyendoEstado(
                100L, EstadoActividad.CANCELADA)).thenReturn(false);

        assertThatThrownBy(() -> publicarProyectoService.publicar(1L, 10L, 100L))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("ubicación")
                .hasMessageContaining("fecha de entrega")
                .hasMessageContaining("moodboard")
                .hasMessageContaining("requerimiento")
                .hasMessageContaining("general o de actividad");
    }
}
