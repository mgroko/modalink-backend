package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
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
import org.mgroko.backend.proyectos.dto.CrearObjetivoRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.NombreProyectoDuplicadoException;
import org.mgroko.backend.proyectos.exception.RangoFechasProyectoInvalidoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RolProyectoRepository;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrearProyectoServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private RolProyectoRepository rolProyectoRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private ObjetivoRepository objetivoRepository;

    @Mock
    private UbicacionService ubicacionService;

    @InjectMocks
    private CrearProyectoService crearProyectoService;

    private Perfil perfil;
    private RolProyecto rolDirector;

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
                .nombre("Director")
                .build();
    }

    @Test
    void crear_datosValidos_creaProyectoConDirectorYPlanificacion() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile 2026",
                "Pasarela invierno",
                Privacidad.Publico,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15),
                true,
                new UbicacionRequest("12345", "06"),
                List.of(new CrearObjetivoRequest("Conseguir sponsors", "Contactar marcas"))
        );

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.existeProyectoConNombreParaPerfil("Desfile 2026", 10L, "Director", EstadoParticipacion.Activo))
                .thenReturn(false);
        when(rolProyectoRepository.findByNombre("Director")).thenReturn(Optional.of(rolDirector));

        Ubicacion ubicacion = Ubicacion.builder().idUbicacion(50L).localidad("La Plata").build();
        when(ubicacionService.obtenerOCrear("12345", "06")).thenReturn(ubicacion);

        when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(invocation -> {
            Proyecto p = invocation.getArgument(0);
            p.setIdProyecto(100L);
            return p;
        });

        when(objetivoRepository.save(any(Objetivo.class))).thenAnswer(invocation -> {
            Objetivo obj = invocation.getArgument(0);
            obj.setIdObjetivo(1L);
            return obj;
        });

        ProyectoResponse response = crearProyectoService.crear(1L, 10L, request);

        assertThat(response).isNotNull();
        assertThat(response.idProyecto()).isEqualTo(100L);
        assertThat(response.nombre()).isEqualTo("Desfile 2026");
        assertThat(response.estado()).isEqualTo("Borrador");
        assertThat(response.idDirector()).isEqualTo(10L);
        assertThat(response.nombreDirector()).isEqualTo("Luna Diseños");
        assertThat(response.fechaFinEstipulada()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(response.objetivos()).hasSize(1);
        assertThat(response.objetivos().get(0).nombre()).isEqualTo("Conseguir sponsors");

        verify(miembroProyectoRepository).save(any(MiembroProyecto.class));
        verify(planificacionRepository).save(any(Planificacion.class));
    }

    @Test
    void crear_sinPerfilActivo_lanzaExcepcion() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null
        );

        assertThatThrownBy(() -> crearProyectoService.crear(1L, null, request))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void crear_perfilNoExiste_lanzaExcepcion() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null
        );
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 99L, request))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void crear_perfilEnBaja_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null
        );
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void crear_fechaFinMenorAFechaInicio_lanzaExcepcion() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto", "Desc", Privacidad.Publico,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 5),
                false, null, null
        );
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(RangoFechasProyectoInvalidoException.class)
                .hasMessageContaining("no puede ser anterior a la fecha de inicio");
    }

    @Test
    void crear_nombreDuplicadoParaMismoDirector_lanzaExcepcion() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto Existente", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null
        );
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.existeProyectoConNombreParaPerfil("Proyecto Existente", 10L, "Director", EstadoParticipacion.Activo))
                .thenReturn(true);

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(NombreProyectoDuplicadoException.class)
                .hasMessageContaining("Ya tienes un proyecto con el nombre 'Proyecto Existente'");
    }
}
