package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObtenerProyectoServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private ObjetivoRepository objetivoRepository;

    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;

    @Mock
    private RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;

    @Mock
    private MoodboardRepository moodboardRepository;

    @InjectMocks
    private ObtenerProyectoService obtenerProyectoService;

    private Perfil perfil;
    private Proyecto proyectoBorrador;
    private Proyecto proyectoPublicado;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().idUsuario(1L).build();
        perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .build();

        Ubicacion ubicacion = Ubicacion.builder()
                .idUbicacion(50L)
                .ciudad(Ciudad.builder().nombre("La Plata").build())
                .build();

        proyectoBorrador = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile Borrador")
                .descripcion("Desc")
                .fechaInicio(LocalDate.of(2026, 6, 1))
                .estado(EstadoProyecto.BORRADOR)
                .privacidad(Privacidad.PUBLICO)
                .ubicacion(ubicacion)
                .build();

        proyectoPublicado = Proyecto.builder()
                .idProyecto(200L)
                .nombre("Desfile Publicado")
                .descripcion("Desc")
                .fechaInicio(LocalDate.of(2026, 6, 1))
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .ubicacion(ubicacion)
                .build();
    }

    private void prepararPerfil() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
    }

    @Test
    void obtener_miembroActivo_devuelveProyectoCompleto() {
        prepararPerfil();
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(100L, 10L, EstadoParticipacion.ACTIVO))
                .thenReturn(true);

        RolProyecto rolDirector = RolProyecto.builder().nombre("Director").build();
        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyectoBorrador)
                .perfil(perfil)
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        when(miembroProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of(miembro));

        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(Planificacion.builder()
                        .proyecto(proyectoBorrador)
                        .fechaEntrega(LocalDate.of(2026, 6, 15))
                        .build()));
        when(objetivoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of());
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of());

        Moodboard moodboard = Moodboard.builder()
                .idMoodboard(77L)
                .proyecto(proyectoBorrador)
                .descripcion("Paleta fría")
                .fechaCreacion(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.of(moodboard));

        ProyectoResponse response = obtenerProyectoService.obtener(1L, 10L, 100L);

        assertThat(response.idProyecto()).isEqualTo(100L);
        assertThat(response.estado()).isEqualTo("Borrador");
        assertThat(response.idDirector()).isEqualTo(10L);
        assertThat(response.nombreDirector()).isEqualTo("Luna Diseños");
        assertThat(response.fechaFinEstipulada()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(response.moodboard()).isNotNull();
        assertThat(response.moodboard().idMoodboard()).isEqualTo(77L);
        assertThat(response.requerimientosGral()).isEmpty();
    }

    @Test
    void obtener_noMiembroProyectoPublicadoPermitido() {
        prepararPerfil();
        when(proyectoRepository.findById(200L)).thenReturn(Optional.of(proyectoPublicado));
        when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(200L, 10L, EstadoParticipacion.ACTIVO))
                .thenReturn(false);
        when(miembroProyectoRepository.findByProyectoIdProyecto(200L)).thenReturn(List.of());
        when(planificacionRepository.findByProyectoIdProyecto(200L)).thenReturn(Optional.empty());
        when(objetivoRepository.findByProyectoIdProyecto(200L)).thenReturn(List.of());
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(200L)).thenReturn(List.of());
        when(moodboardRepository.findByProyectoIdProyecto(200L)).thenReturn(Optional.empty());

        ProyectoResponse response = obtenerProyectoService.obtener(1L, 10L, 200L);

        assertThat(response.idProyecto()).isEqualTo(200L);
        assertThat(response.idDirector()).isNull();
    }

    @Test
    void obtener_noMiembroProyectoBorrador_lanzaAccesoDenegado() {
        prepararPerfil();
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(100L, 10L, EstadoParticipacion.ACTIVO))
                .thenReturn(false);

        assertThatThrownBy(() -> obtenerProyectoService.obtener(1L, 10L, 100L))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void obtener_noMiembroProyectoPrivado_lanzaAccesoDenegado() {
        proyectoPublicado.setPrivacidad(Privacidad.PRIVADO);
        prepararPerfil();
        when(proyectoRepository.findById(200L)).thenReturn(Optional.of(proyectoPublicado));
        when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(200L, 10L, EstadoParticipacion.ACTIVO))
                .thenReturn(false);

        assertThatThrownBy(() -> obtenerProyectoService.obtener(1L, 10L, 200L))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void obtener_proyectoInexistente_lanzaExcepcion() {
        prepararPerfil();
        when(proyectoRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerProyectoService.obtener(1L, 10L, 999L))
                .isInstanceOf(ProyectoNoEncontradoException.class)
                .hasMessageContaining("999");
    }

    @Test
    void obtener_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> obtenerProyectoService.obtener(1L, null, 100L))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void obtener_perfilNoPerteneceAlUsuario_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerProyectoService.obtener(1L, 10L, 100L))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void obtener_perfilEnBaja_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> obtenerProyectoService.obtener(1L, 10L, 100L))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void obtener_requerimientoConProfesion_incluyeDatosEnResponse() {
        prepararPerfil();
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyectoBorrador));
        when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(100L, 10L, EstadoParticipacion.ACTIVO))
                .thenReturn(true);
        when(miembroProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of());
        when(planificacionRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.empty());
        when(objetivoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of());
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.empty());

        Profesion profesion = Profesion.builder().idProfesion(2L).nombre("Modelo").build();
        RequerimientoGralProyecto rg = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(10L)
                .proyecto(proyectoBorrador)
                .profesion(profesion)
                .cantidad(2)
                .descripcion("Dos modelos")
                .build();
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of(rg));

        ProyectoResponse response = obtenerProyectoService.obtener(1L, 10L, 100L);

        assertThat(response.requerimientosGral()).hasSize(1);
        assertThat(response.requerimientosGral().get(0).idProfesion()).isEqualTo(2L);
        assertThat(response.requerimientosGral().get(0).nombreProfesion()).isEqualTo("Modelo");
        assertThat(response.requerimientosGral().get(0).cantidad()).isEqualTo(2);
    }
}
