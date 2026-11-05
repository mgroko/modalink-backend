package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.BloqueoFinalizacionResponse;
import org.mgroko.backend.proyectos.dto.CalificacionRequest;
import org.mgroko.backend.proyectos.dto.FinalizarProyectoRequest;
import org.mgroko.backend.proyectos.dto.FinalizarProyectoResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.evento.ProyectoFinalizadoEvent;
import org.mgroko.backend.proyectos.exception.CalificacionIncompletaException;
import org.mgroko.backend.proyectos.exception.CalificacionInvalidaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoFinalizableException;
import org.mgroko.backend.repositorio.CalificacionPerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinalizarProyectoServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTOR = 10L;
    private static final Long ID_PERFIL_MIEMBRO = 20L;
    private static final Long ID_PERFIL_OTRO_DIRECTOR = 30L;
    private static final Long ID_PROYECTO = 100L;
    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 1, 12, 0);

    @Mock
    private ContextoFinalizacionProyectoService contextoService;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private CalificacionPerfilRepository calificacionPerfilRepository;
    @Mock
    private ObtenerProyectoService obtenerProyectoService;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private FinalizarProyectoService service;

    private Proyecto proyecto;
    private Perfil perfilActor;
    private MiembroProyecto miembroActor;
    private MiembroProyecto miembroMiembro;
    private MiembroProyecto miembroOtroDirector;
    private ProyectoResponse proyectoResponse;

    @BeforeEach
    void setUp() {
        service = new FinalizarProyectoService(
                contextoService,
                proyectoRepository,
                calificacionPerfilRepository,
                obtenerProyectoService,
                eventPublisher);

        proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Desfile")
                .estado(EstadoProyecto.CONFIRMADO)
                .build();

        perfilActor = perfil(ID_PERFIL_ACTOR, ID_USUARIO, "Dirección");
        miembroActor = miembro(perfilActor, "DIRECTOR");
        miembroMiembro = miembro(
                perfil(ID_PERFIL_MIEMBRO, 2L, "Miembro"), "MIEMBRO");
        miembroOtroDirector = miembro(
                perfil(ID_PERFIL_OTRO_DIRECTOR, 3L, "Otro director"), "DIRECTOR");

        proyectoResponse = new ProyectoResponse(
                ID_PROYECTO,
                "Desfile",
                "Descripción",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1),
                "Finalizado",
                "PUBLICO",
                true,
                null,
                ID_PERFIL_ACTOR,
                "Dirección",
                List.of(),
                List.of(),
                null,
                org.mgroko.backend.modelo.enums.RolProyectoUsuario.DIRECTOR,
                true,
                List.of("FINALIZAR_PROYECTO", "VER_PROYECTO"),
                false,
                null,
                null,
                true,
                AHORA);

        when(obtenerProyectoService.obtener(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO))
                .thenReturn(proyectoResponse);
    }

    @Test
    void finalizar_happyPath_guardaCalificacionesCambiaEstadoYPublicaEvento() {
        contexto(List.of(miembroActor, miembroMiembro, miembroOtroDirector), List.of());

        FinalizarProyectoResponse response = service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(
                        calificacion(ID_PERFIL_MIEMBRO, 5, "Excelente"),
                        calificacion(ID_PERFIL_OTRO_DIRECTOR, 4, "Muy bien")));

        assertThat(response.proyecto()).isSameAs(proyectoResponse);
        assertThat(response.fechaFinalizacion()).isEqualTo(AHORA);
        assertThat(response.calificacionesRegistradas()).isEqualTo(2);

        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.FINALIZADO);
        assertThat(proyecto.getFechaFinalizacion()).isEqualTo(AHORA);

        ArgumentCaptor<List<CalificacionPerfil>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(calificacionPerfilRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .extracting(CalificacionPerfil::getPerfilReceptor)
                .extracting(Perfil::getIdPerfil)
                .containsExactly(ID_PERFIL_MIEMBRO, ID_PERFIL_OTRO_DIRECTOR);
        assertThat(captor.getValue())
                .extracting(CalificacionPerfil::getValoracion)
                .containsExactly(5, 4);
        assertThat(captor.getValue())
                .extracting(CalificacionPerfil::getResenia)
                .containsExactly("Excelente", "Muy bien");
        assertThat(captor.getValue())
                .allSatisfy(calificacion -> {
                    assertThat(calificacion.getProyecto()).isSameAs(proyecto);
                    assertThat(calificacion.getPerfilEmisor()).isSameAs(perfilActor);
                    assertThat(calificacion.getFechaRegistro()).isEqualTo(AHORA);
                });

        verify(proyectoRepository).saveAndFlush(proyecto);
        verify(eventPublisher).publishEvent(any(ProyectoFinalizadoEvent.class));
    }

    @Test
    void finalizar_conBloqueos_lanzaProyectoNoFinalizableYNoPersiste() {
        contextoConBloqueos(
                List.of(miembroActor, miembroMiembro),
                List.of(new BloqueoFinalizacionResponse(
                        "ACTIVIDAD_NO_FINALIZADA",
                        "Hay una actividad pendiente.",
                        "ACTIVIDAD",
                        55L)));

        assertThatThrownBy(() -> service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 5, "Excelente"))))
                .isInstanceOf(ProyectoNoFinalizableException.class);

        verify(calificacionPerfilRepository, never()).saveAll(anyList());
        verify(proyectoRepository, never()).saveAndFlush(any(Proyecto.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void finalizar_calificacionesIncompletas_lanzaExcepcionConFaltantes() {
        contexto(List.of(miembroActor, miembroMiembro, miembroOtroDirector), List.of());

        assertThatThrownBy(() -> service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 5, "Excelente"))))
                .isInstanceOfSatisfying(
                        CalificacionIncompletaException.class,
                        ex -> {
                            assertThat(ex.getIdsPerfilFaltantes())
                                    .containsExactly(ID_PERFIL_OTRO_DIRECTOR);
                            assertThat(ex.getMessage()).contains("Otro director");
                        });

        verify(calificacionPerfilRepository, never()).saveAll(anyList());
    }

    @Test
    void finalizar_sinCalificacionesConMiembros_lanzaIncompleta() {
        contexto(List.of(miembroActor, miembroMiembro), List.of());

        assertThatThrownBy(() -> service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request()))
                .isInstanceOf(CalificacionIncompletaException.class);
    }

    @Test
    void finalizar_sinOtrosMiembros_permiteListaVacia() {
        contexto(List.of(miembroActor), List.of());

        FinalizarProyectoResponse response = service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request());

        assertThat(response.calificacionesRegistradas()).isZero();
        verify(calificacionPerfilRepository).saveAll(List.of());
        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.FINALIZADO);
    }

    @Test
    void finalizar_autoCalificacion_lanzaInvalida() {
        contexto(List.of(miembroActor, miembroMiembro), List.of());

        assertThatThrownBy(() -> service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_ACTOR, 5, "Excelente"))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("vos mismo");
    }

    @Test
    void finalizar_perfilAjeno_lanzaInvalida() {
        contexto(List.of(miembroActor, miembroMiembro), List.of());

        assertThatThrownBy(() -> service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(999L, 5, "Excelente"))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("no participa");
    }

    @Test
    void finalizar_calificacionDuplicada_lanzaInvalida() {
        contexto(List.of(miembroActor, miembroMiembro), List.of());

        assertThatThrownBy(() -> service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(
                        calificacion(ID_PERFIL_MIEMBRO, 5, "Excelente"),
                        calificacion(ID_PERFIL_MIEMBRO, 4, "Otra"))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("duplicadas");
    }

    @Test
    void finalizar_valoracionFueraDeRango_lanzaInvalida() {
        contexto(List.of(miembroActor, miembroMiembro), List.of());

        assertThatThrownBy(() -> service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 6, "Excelente"))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("entre 1 y 5");
    }

    @Test
    void finalizar_reseniaVacia_lanzaInvalida() {
        contexto(List.of(miembroActor, miembroMiembro), List.of());

        assertThatThrownBy(() -> service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 5, "   "))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("reseña es obligatoria");
    }

    @Test
    void finalizar_reseniaMuyLarga_lanzaInvalida() {
        contexto(List.of(miembroActor, miembroMiembro), List.of());

        assertThatThrownBy(() -> service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 5, "x".repeat(201)))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("200 caracteres");
    }

    @Test
    void finalizar_miembroYaCalificado_noSeExigeDeNuevo() {
        CalificacionPerfil existente = CalificacionPerfil.builder()
                .proyecto(proyecto)
                .perfilEmisor(perfilActor)
                .perfilReceptor(miembroMiembro.getPerfil())
                .valoracion(5)
                .resenia("Ya calificado")
                .build();
        contexto(
                List.of(miembroActor, miembroMiembro, miembroOtroDirector),
                List.of(existente));

        FinalizarProyectoResponse response = service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_OTRO_DIRECTOR, 4, "Muy bien")));

        assertThat(response.calificacionesRegistradas()).isEqualTo(1);
        ArgumentCaptor<List<CalificacionPerfil>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(calificacionPerfilRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(calificacion -> calificacion.getPerfilReceptor()
                        .getIdPerfil())
                .containsExactly(ID_PERFIL_OTRO_DIRECTOR);
    }

    @Test
    void finalizar_guardaReseniaSinEspaciosExternos() {
        contexto(List.of(miembroActor, miembroMiembro), List.of());

        service.finalizar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 5, "  Excelente  ")));

        ArgumentCaptor<List<CalificacionPerfil>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(calificacionPerfilRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getResenia()).isEqualTo("Excelente");
    }

    private void contexto(
            List<MiembroProyecto> miembros,
            List<CalificacionPerfil> calificacionesExistentes) {
        contextoConBloqueos(miembros, calificacionesExistentes, List.of());
    }

    private void contextoConBloqueos(
            List<MiembroProyecto> miembros,
            List<BloqueoFinalizacionResponse> bloqueos) {
        contextoConBloqueos(miembros, List.of(), bloqueos);
    }

    private void contextoConBloqueos(
            List<MiembroProyecto> miembros,
            List<CalificacionPerfil> calificacionesExistentes,
            List<BloqueoFinalizacionResponse> bloqueos) {

        when(contextoService.cargar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, true))
                .thenReturn(new ContextoFinalizacionProyectoService.Contexto(
                        proyecto,
                        perfilActor,
                        null,
                        AHORA,
                        List.of(),
                        miembros,
                        calificacionesExistentes,
                        List.of(),
                        bloqueos));
    }

    private FinalizarProyectoRequest request(CalificacionRequest... calificaciones) {
        return new FinalizarProyectoRequest(List.of(calificaciones));
    }

    private CalificacionRequest calificacion(
            Long idPerfil,
            Integer valoracion,
            String resenia) {
        return new CalificacionRequest(idPerfil, valoracion, resenia);
    }

    private Perfil perfil(Long idPerfil, Long idUsuario, String nombreArtistico) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombreArtistico)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder()
                        .idUsuario(idUsuario)
                        .nombre("Nombre " + idUsuario)
                        .apellido("Apellido " + idUsuario)
                        .build())
                .build();
    }

    private MiembroProyecto miembro(Perfil perfil, String codigoRol) {
        return MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(RolProyecto.builder()
                        .codigo(codigoRol)
                        .nombre("DIRECTOR".equals(codigoRol)
                                ? "Director de proyecto"
                                : "Miembro de proyecto")
                        .build())
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
    }
}
