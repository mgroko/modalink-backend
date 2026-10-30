package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.AsignacionActividadResponse;
import org.mgroko.backend.actividades.dto.AsignarActividadRequest;
import org.mgroko.backend.actividades.exception.AsignacionActividadNoPermitidaException;
import org.mgroko.backend.actividades.exception.AsignacionNoDisponibleException;
import org.mgroko.backend.actividades.exception.FechaEntregaNoDefinidaException;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.dto.ResultadoPreferencia;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsignarActividadServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTOR = 10L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_ACTIVIDAD = 55L;
    private static final Long ID_PERFIL_DESTINO = 12L;
    private static final Long ID_USUARIO_DESTINO = 2L;
    private static final Long ID_MIEMBRO = 900L;

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private CupoRequerimientoService cupoRequerimientoService;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;
    @Mock
    private CalendarioService calendarioService;

    private AsignarActividadService service;

    private Perfil actor;
    private Perfil destino;
    private Proyecto proyecto;
    private Planificacion planificacion;
    private Actividad actividad;
    private MiembroProyecto miembro;
    private RequerimientoActividad requerimiento;
    private RolProyecto rolMiembro;

    @BeforeEach
    void setUp() {
        service = new AsignarActividadService(
                perfilRepository,
                planificacionRepository,
                actividadRepository,
                miembroProyectoRepository,
                asignacionActividadRepository,
                requerimientoActividadRepository,
                cupoRequerimientoService,
                proyectoSecurityService,
                calendarioService);

        actor = Perfil.builder()
                .idPerfil(ID_PERFIL_ACTOR)
                .nombreArtistico("Directora")
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder().idUsuario(ID_USUARIO).build())
                .build();

        destino = Perfil.builder()
                .idPerfil(ID_PERFIL_DESTINO)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder()
                        .idUsuario(ID_USUARIO_DESTINO)
                        .estado(EstadoUsuario.Activo)
                        .build())
                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                .build();

        proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Desfile")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        planificacion = Planificacion.builder()
                .idPlanificacion(7L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2026, 12, 31))
                .build();

        actividad = Actividad.builder()
                .idActividad(ID_ACTIVIDAD)
                .nombre("Montaje")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();

        rolMiembro = RolProyecto.builder()
                .idRolProyecto(2L)
                .codigo("MIEMBRO")
                .nombre("Miembro de proyecto")
                .build();

        miembro = MiembroProyecto.builder()
                .idMiembro(ID_MIEMBRO)
                .proyecto(proyecto)
                .perfil(destino)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();

        requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(123L)
                .actividad(actividad)
                .profesion(destino.getProfesion())
                .cantidad(1)
                .build();
    }

    @Test
    void asignaConRequerimientoCompatible() {
        prepararEscenarioBase();
        when(requerimientoActividadRepository.findConProfesionByActividadId(ID_ACTIVIDAD))
                .thenReturn(List.of(requerimiento));
        when(cupoRequerimientoService.calcularActividades(List.of(requerimiento)))
                .thenReturn(Map.of(123L, new CupoRequerimiento(1, 0, 1, false)));
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 1, 0, true));
        when(asignacionActividadRepository.saveAndFlush(any(AsignacionActividad.class)))
                .thenAnswer(invocation -> {
                    AsignacionActividad asignacion = invocation.getArgument(0);
                    asignacion.setIdAsignacionAct(1200L);
                    return asignacion;
                });

        AsignacionActividadResponse response = service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO));

        assertThat(response.idAsignacionActividad()).isEqualTo(1200L);
        assertThat(response.idRequerimientoActividad()).isEqualTo(123L);
        assertThat(response.requerimientoSatisfecho()).isTrue();
        assertThat(response.idMiembro()).isEqualTo(ID_MIEMBRO);
        assertThat(response.idPerfil()).isEqualTo(ID_PERFIL_DESTINO);
        verify(calendarioService).verificarDisponibilidad(ID_USUARIO_DESTINO, actividad);
    }

    @Test
    void asignaSinRequerimientoSiNoHayCoincidencia() {
        prepararEscenarioBase();
        when(requerimientoActividadRepository.findConProfesionByActividadId(ID_ACTIVIDAD))
                .thenReturn(List.of());
        when(asignacionActividadRepository.saveAndFlush(any(AsignacionActividad.class)))
                .thenAnswer(invocation -> {
                    AsignacionActividad asignacion = invocation.getArgument(0);
                    asignacion.setIdAsignacionAct(1201L);
                    return asignacion;
                });

        AsignacionActividadResponse response = service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO));

        assertThat(response.idRequerimientoActividad()).isNull();
        assertThat(response.requerimientoSatisfecho()).isFalse();
    }

    @Test
    void directorSeAsignaSinConsumirCupo() {
        rolMiembro.setCodigo("DIRECTOR");
        prepararEscenarioBase();
        when(asignacionActividadRepository.saveAndFlush(any(AsignacionActividad.class)))
                .thenAnswer(invocation -> {
                    AsignacionActividad asignacion = invocation.getArgument(0);
                    asignacion.setIdAsignacionAct(1202L);
                    return asignacion;
                });

        AsignacionActividadResponse response = service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO));

        assertThat(response.idRequerimientoActividad()).isNull();
        verify(requerimientoActividadRepository, never())
                .findConProfesionByActividadId(any());
    }

    @Test
    void rechazaSinDisponibilidad() {
        prepararEscenarioBase();
        when(calendarioService.verificarDisponibilidad(ID_USUARIO_DESTINO, actividad))
                .thenReturn(resultado(false, "SIN_HUECO"));

        assertThatThrownBy(() -> service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO)))
                .isInstanceOf(AsignacionNoDisponibleException.class);

        verify(asignacionActividadRepository, never())
                .saveAndFlush(any(AsignacionActividad.class));
    }

    @Test
    void rechazaDuplicado() {
        prepararEscenarioBase();
        when(asignacionActividadRepository
                .existsByMiembro_IdMiembroAndActividad_IdActividad(ID_MIEMBRO, ID_ACTIVIDAD))
                .thenReturn(true);

        assertThatThrownBy(() -> service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO)))
                .isInstanceOf(AsignacionActividadNoPermitidaException.class)
                .hasMessageContaining("ya está asignado");
    }

    @Test
    void rechazaPerfilQueNoEsMiembroActivo() {
        prepararEscenarioBase();
        when(miembroProyectoRepository.findMiembroActivoConRol(ID_PROYECTO, ID_PERFIL_DESTINO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO)))
                .isInstanceOf(AsignacionActividadNoPermitidaException.class)
                .hasMessageContaining("no es miembro activo");
    }

    @Test
    void rechazaProyectoSinFechaEntrega() {
        planificacion.setFechaEntrega(null);
        prepararEscenarioBase();

        assertThatThrownBy(() -> service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO)))
                .isInstanceOf(FechaEntregaNoDefinidaException.class);
    }

    @Test
    void rechazaActividadFinalizada() {
        actividad.setEstado(EstadoActividad.FINALIZADA);
        prepararEscenarioBase();

        assertThatThrownBy(() -> service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO)))
                .isInstanceOf(AsignacionActividadNoPermitidaException.class)
                .hasMessageContaining("no admite asignaciones");
    }

    @Test
    void rechazaProyectoFinalizado() {
        proyecto.setEstado(EstadoProyecto.FINALIZADO);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTOR, ID_USUARIO))
                .thenReturn(Optional.of(actor));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(ID_PROYECTO))
                .thenReturn(Optional.of(planificacion));

        assertThatThrownBy(() -> service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO)))
                .isInstanceOf(AsignacionActividadNoPermitidaException.class)
                .hasMessageContaining("no admite asignaciones");
    }

    @Test
    void rechazaSinPermiso() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTOR, ID_USUARIO))
                .thenReturn(Optional.of(actor));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(ID_PROYECTO))
                .thenReturn(Optional.of(planificacion));
        doThrow(new AccesoDenegadoProyectoException("Sin permiso"))
                .when(proyectoSecurityService)
                .validarPermiso(ID_PROYECTO, ID_PERFIL_ACTOR,
                        AsignarActividadService.PERMISO_ASIGNAR_ACTIVIDAD);

        assertThatThrownBy(() -> service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO)))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void rechazaPerfilDestinoInactivo() {
        destino.setEstado(EstadoPerfil.Baja);
        prepararEscenarioBase();

        assertThatThrownBy(() -> service.asignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD,
                new AsignarActividadRequest(ID_PERFIL_DESTINO)))
                .isInstanceOf(org.mgroko.backend.perfiles.exception.PerfilEnBajaException.class);
    }

    private void prepararEscenarioBase() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTOR, ID_USUARIO))
                .thenReturn(Optional.of(actor));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(ID_PROYECTO))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findByIdActividadAndPlanificacionIdPlanificacion(
                ID_ACTIVIDAD, planificacion.getIdPlanificacion()))
                .thenReturn(Optional.of(actividad));
        when(perfilRepository.findByIdConProfesion(ID_PERFIL_DESTINO))
                .thenReturn(Optional.of(destino));
        when(miembroProyectoRepository.findMiembroActivoConRol(ID_PROYECTO, ID_PERFIL_DESTINO))
                .thenReturn(Optional.of(miembro));
        when(calendarioService.verificarDisponibilidad(ID_USUARIO_DESTINO, actividad))
                .thenReturn(resultado(true, null));
    }

    private ResultadoDisponibilidad resultado(boolean disponible, String motivo) {
        return new ResultadoDisponibilidad(
                disponible,
                false,
                LocalDateTime.of(2026, 11, 2, 9, 0),
                LocalDateTime.of(2026, 11, 2, 18, 0),
                disponible ? LocalDateTime.of(2026, 11, 2, 10, 0) : null,
                disponible ? LocalDateTime.of(2026, 11, 2, 11, 0) : null,
                motivo,
                List.of(),
                ResultadoPreferencia.SIN_PREFERENCIA,
                null,
                null,
                List.of());
    }
}
