package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.ActividadCronogramaResponse;
import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.CronogramaResponse;
import org.mgroko.backend.actividades.dto.NodoCpm;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoCronograma;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObtenerCronogramaServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @Mock
    private CronogramaService cronogramaService;

    @InjectMocks
    private ObtenerCronogramaService service;

    private Planificacion planificacion;
    private Actividad critica;
    private Actividad noCritica;
    private Actividad cancelada;
    private LocalDateTime inicio;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().idUsuario(1L).build();
        Perfil perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                .build();

        Proyecto proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile Primavera")
                .fechaInicio(LocalDate.of(2026, 10, 14))
                .build();

        planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2027, 3, 15))
                .build();

        inicio = LocalDateTime.of(2026, 10, 14, 0, 0);

        critica = Actividad.builder()
                .idActividad(1L)
                .nombre("Bocetos")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();

        cancelada = Actividad.builder()
                .idActividad(3L)
                .nombre("Cancelada")
                .duracionMinutos(60)
                .estado(EstadoActividad.CANCELADA)
                .planificacion(planificacion)
                .build();

        noCritica = Actividad.builder()
                .idActividad(2L)
                .nombre("Fitting")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .predecesoras(new HashSet<>(Set.of(critica, cancelada)))
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(critica, noCritica, cancelada));

        CalculoCpm calculo = new CalculoCpm(
                Map.of(
                        1L, nodo(1L, 0, 60, 60, 0, true, true),
                        2L, nodo(2L, 0, 60, 120, 60, false, false)),
                120L,
                300L,
                true,
                List.of());

        when(cronogramaService.calcularProyeccion(planificacion,
                List.of(critica, noCritica, cancelada)))
                .thenReturn(calculo);
        when(cronogramaService.inicioEfectivo(planificacion)).thenReturn(inicio);
        when(cronogramaService.esTentativa(planificacion)).thenReturn(false);
        when(cronogramaService.estadoDe(calculo)).thenReturn(EstadoCronograma.NORMAL);
    }

    @Test
    void obtener_devuelveResumenYExcluyeCanceladas() {
        CronogramaResponse response = service.obtener(1L, 10L, 100L);

        assertThat(response.resumen().totalActividades()).isEqualTo(2);
        assertThat(response.resumen().actividadesCriticas()).isEqualTo(1);
        assertThat(response.resumen().rutaCritica()).containsExactly(1L);
        assertThat(response.resumen().makespanMinutos()).isEqualTo(120L);
        assertThat(response.resumen().margenMinutos()).isEqualTo(180L);
        assertThat(response.resumen().estadoCronograma()).isEqualTo("NORMAL");
        assertThat(response.resumen().fechaFinProyectada())
                .isEqualTo(inicio.plusMinutes(120));

        assertThat(response.actividades())
                .extracting(ActividadCronogramaResponse::idActividad)
                .containsExactly(1L, 2L);
    }

    @Test
    void obtener_noIncluyePredecesorasCanceladas() {
        CronogramaResponse response = service.obtener(1L, 10L, 100L);

        ActividadCronogramaResponse fila = response.actividades().stream()
                .filter(actividad -> actividad.idActividad().equals(2L))
                .findFirst()
                .orElseThrow();

        assertThat(fila.predecesoras())
                .extracting(predecesora -> predecesora.idActividad())
                .containsExactly(1L);
    }

    @Test
    void obtener_calculaFinConHolguraConMakespan() {
        CronogramaResponse response = service.obtener(1L, 10L, 100L);

        ActividadCronogramaResponse fila = response.actividades().stream()
                .filter(actividad -> actividad.idActividad().equals(2L))
                .findFirst()
                .orElseThrow();

        assertThat(fila.holguraMakespan()).isEqualTo(60L);
        assertThat(fila.finProyectado()).isEqualTo(inicio.plusMinutes(60));
        assertThat(fila.finConHolgura()).isEqualTo(inicio.plusMinutes(120));
        assertThat(fila.enRutaCritica()).isFalse();
    }

    @Test
    void obtener_validaPermisoDeLectura() {
        service.obtener(1L, 10L, 100L);

        verify(proyectoSecurityService).validarPermiso(
                100L, 10L, ProyectoSecurityService.PERMISO_VER_PROYECTO);
    }

    @Test
    void obtener_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        assertThatThrownBy(() -> service.obtener(1L, null, 100L))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    private NodoCpm nodo(
            long id,
            long inicioTemprano,
            long finTemprano,
            long finTardioMakespan,
            long holgura,
            boolean critica,
            boolean enRutaCritica) {

        return new NodoCpm(
                id,
                (int) (finTemprano - inicioTemprano),
                false,
                null,
                inicioTemprano,
                finTemprano,
                inicioTemprano + holgura,
                finTardioMakespan,
                holgura,
                critica,
                inicioTemprano + holgura,
                finTardioMakespan,
                holgura,
                false,
                null,
                enRutaCritica);
    }
}
