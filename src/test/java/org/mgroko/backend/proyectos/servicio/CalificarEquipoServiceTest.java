package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.proyectos.dto.CalificacionRequest;
import org.mgroko.backend.proyectos.dto.CalificacionesProyectoResponse;
import org.mgroko.backend.proyectos.dto.CalificarEquipoRequest;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.CalificacionIncompletaException;
import org.mgroko.backend.proyectos.exception.CalificacionInvalidaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoCalificableException;
import org.mgroko.backend.repositorio.CalificacionPerfilRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalificarEquipoServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTOR = 10L;
    private static final Long ID_PERFIL_MIEMBRO = 20L;
    private static final Long ID_PERFIL_OTRO_DIRECTOR = 30L;
    private static final Long ID_PROYECTO = 100L;
    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 5, 15, 0);

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private CalificacionPerfilRepository calificacionPerfilRepository;

    private CalificarEquipoService service;

    private Perfil perfilActor;
    private Proyecto proyecto;
    private MiembroProyecto miembroActor;
    private MiembroProyecto miembro;
    private MiembroProyecto otroDirector;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-05T15:00:00Z"),
                ZoneOffset.UTC);
        service = new CalificarEquipoService(
                perfilRepository,
                proyectoRepository,
                miembroProyectoRepository,
                calificacionPerfilRepository,
                reloj);

        perfilActor = perfil(ID_PERFIL_ACTOR, ID_USUARIO, "Dirección");
        miembroActor = miembro(perfilActor, RolProyectoUsuario.DIRECTOR);
        miembro = miembro(
                perfil(ID_PERFIL_MIEMBRO, 2L, "Miembro"),
                RolProyectoUsuario.MIEMBRO);
        otroDirector = miembro(
                perfil(ID_PERFIL_OTRO_DIRECTOR, 3L, "Otro director"),
                RolProyectoUsuario.DIRECTOR);

        proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Desfile")
                .estado(EstadoProyecto.FINALIZADO)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(
                ID_PERFIL_ACTOR, ID_USUARIO)).thenReturn(Optional.of(perfilActor));
        when(proyectoRepository.findById(ID_PROYECTO))
                .thenReturn(Optional.of(proyecto));
        when(miembroProyectoRepository.findMiembroActivoConRol(
                ID_PROYECTO, ID_PERFIL_ACTOR))
                .thenReturn(Optional.of(miembroActor));
        when(miembroProyectoRepository
                .buscarActivosDeProyectoConPerfilProfesionYRol(ID_PROYECTO))
                .thenReturn(List.of(miembroActor, miembro, otroDirector));
        when(calificacionPerfilRepository
                .findByProyectoIdProyectoAndPerfilEmisorIdPerfil(
                        ID_PROYECTO, ID_PERFIL_ACTOR))
                .thenReturn(List.of());
        when(calificacionPerfilRepository.saveAll(anyList()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void calificar_happyPath_guardaCalificacionesYDevuelveEstadoCompleto() {
        CalificacionesProyectoResponse response = service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(
                        calificacion(ID_PERFIL_MIEMBRO, 5, "Excelente"),
                        calificacion(ID_PERFIL_OTRO_DIRECTOR, 4, "Muy bien")));

        assertThat(response.calificacionesCompletas()).isTrue();
        assertThat(response.calificacionesRequeridas()).isEqualTo(2);
        assertThat(response.calificacionesRealizadas()).isEqualTo(2);
        assertThat(response.calificacionesPendientes()).isZero();
        assertThat(response.calificacionesEmitidas()).hasSize(2);
        assertThat(response.miembrosACalificar())
                .allSatisfy(miembro -> assertThat(miembro.yaCalificado()).isTrue());

        ArgumentCaptor<List<CalificacionPerfil>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(calificacionPerfilRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .allSatisfy(calificacion -> {
                    assertThat(calificacion.getProyecto()).isSameAs(proyecto);
                    assertThat(calificacion.getPerfilEmisor()).isSameAs(perfilActor);
                    assertThat(calificacion.getFechaRegistro()).isEqualTo(AHORA);
                });
        assertThat(captor.getValue())
                .extracting(CalificacionPerfil::getResenia)
                .containsExactly("Excelente", "Muy bien");
    }

    @Test
    void obtener_sinCalificaciones_devuelveTodosLosPendientes() {
        CalificacionesProyectoResponse response = service.obtener(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO);

        assertThat(response.calificacionesCompletas()).isFalse();
        assertThat(response.calificacionesRequeridas()).isEqualTo(2);
        assertThat(response.calificacionesRealizadas()).isZero();
        assertThat(response.calificacionesPendientes()).isEqualTo(2);
        assertThat(response.calificacionesEmitidas()).isEmpty();
        assertThat(response.miembrosACalificar())
                .allSatisfy(miembro -> assertThat(miembro.yaCalificado()).isFalse());
    }

    @Test
    void obtener_conCalificacionExistente_calculaRealizadasYPendientes() {
        CalificacionPerfil existente = calificacionExistente(
                miembro.getPerfil(), 5, "Excelente");
        when(calificacionPerfilRepository
                .findByProyectoIdProyectoAndPerfilEmisorIdPerfil(
                        ID_PROYECTO, ID_PERFIL_ACTOR))
                .thenReturn(List.of(existente));

        CalificacionesProyectoResponse response = service.obtener(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO);

        assertThat(response.calificacionesCompletas()).isFalse();
        assertThat(response.calificacionesRealizadas()).isEqualTo(1);
        assertThat(response.calificacionesPendientes()).isEqualTo(1);
        assertThat(response.calificacionesEmitidas()).hasSize(1);
        assertThat(response.miembrosACalificar())
                .filteredOn(miembro -> miembro.idPerfil().equals(ID_PERFIL_MIEMBRO))
                .extracting(miembro -> miembro.yaCalificado())
                .containsExactly(true);
    }

    @Test
    void calificar_sinCalificacionesYConPendientes_lanzaIncompleta() {
        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request()))
                .isInstanceOfSatisfying(
                        CalificacionIncompletaException.class,
                        ex -> assertThat(ex.getIdsPerfilFaltantes())
                                .containsExactly(
                                        ID_PERFIL_MIEMBRO,
                                        ID_PERFIL_OTRO_DIRECTOR));

        verify(calificacionPerfilRepository, never()).saveAll(anyList());
    }

    @Test
    void calificar_parcial_lanzaIncompletaConFaltantes() {
        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 5, "Excelente"))))
                .isInstanceOfSatisfying(
                        CalificacionIncompletaException.class,
                        ex -> assertThat(ex.getIdsPerfilFaltantes())
                                .containsExactly(ID_PERFIL_OTRO_DIRECTOR));
    }

    @Test
    void calificar_sinOtrosMiembros_permiteListaVacia() {
        when(miembroProyectoRepository
                .buscarActivosDeProyectoConPerfilProfesionYRol(ID_PROYECTO))
                .thenReturn(List.of(miembroActor));

        CalificacionesProyectoResponse response = service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request());

        assertThat(response.calificacionesCompletas()).isTrue();
        assertThat(response.calificacionesRequeridas()).isZero();
        assertThat(response.calificacionesPendientes()).isZero();

        ArgumentCaptor<List<CalificacionPerfil>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(calificacionPerfilRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void calificar_autoCalificacion_lanzaInvalida() {
        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_ACTOR, 5, "Excelente"))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("vos mismo");
    }

    @Test
    void calificar_perfilAjeno_lanzaInvalida() {
        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(999L, 5, "Excelente"))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("no participa");
    }

    @Test
    void calificar_duplicado_lanzaInvalida() {
        assertThatThrownBy(() -> service.calificar(
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
    void calificar_miembroYaCalificado_lanzaInvalida() {
        when(calificacionPerfilRepository
                .findByProyectoIdProyectoAndPerfilEmisorIdPerfil(
                        ID_PROYECTO, ID_PERFIL_ACTOR))
                .thenReturn(List.of(calificacionExistente(
                        miembro.getPerfil(), 5, "Excelente")));

        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 5, "Excelente"))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("ya tiene una calificación");
    }

    @Test
    void calificar_valoracionFueraDeRango_lanzaInvalida() {
        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 6, "Excelente"))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("entre 1 y 5");
    }

    @Test
    void calificar_reseniaVacia_lanzaInvalida() {
        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 5, "   "))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("reseña es obligatoria");
    }

    @Test
    void calificar_reseniaMuyLarga_lanzaInvalida() {
        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(calificacion(ID_PERFIL_MIEMBRO, 5, "x".repeat(201)))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("200 caracteres");
    }

    @Test
    void calificar_proyectoNoFinalizado_lanzaNoCalificable() {
        proyecto.setEstado(EstadoProyecto.CONFIRMADO);

        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request()))
                .isInstanceOf(ProyectoNoCalificableException.class)
                .hasMessageContaining("Finalizado");
    }

    @Test
    void obtener_proyectoNoFinalizado_lanzaNoCalificable() {
        proyecto.setEstado(EstadoProyecto.CONFIRMADO);

        assertThatThrownBy(() -> service.obtener(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO))
                .isInstanceOf(ProyectoNoCalificableException.class);
    }

    @Test
    void calificar_actorNoMiembro_lanzaAccesoDenegado() {
        when(miembroProyectoRepository.findMiembroActivoConRol(
                ID_PROYECTO, ID_PERFIL_ACTOR))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request()))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void calificar_perfilInactivo_lanzaPerfilEnBaja() {
        perfilActor.setEstado(EstadoPerfil.Baja);

        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request()))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void calificar_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        assertThatThrownBy(() -> service.calificar(
                ID_USUARIO,
                null,
                ID_PROYECTO,
                request()))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void calificar_guardaReseniaSinEspaciosExternos() {
        service.calificar(
                ID_USUARIO,
                ID_PERFIL_ACTOR,
                ID_PROYECTO,
                request(
                        calificacion(ID_PERFIL_MIEMBRO, 5, "  Excelente  "),
                        calificacion(ID_PERFIL_OTRO_DIRECTOR, 4, "Muy bien")));

        ArgumentCaptor<List<CalificacionPerfil>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(calificacionPerfilRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getResenia()).isEqualTo("Excelente");
    }

    private CalificarEquipoRequest request(CalificacionRequest... calificaciones) {
        return new CalificarEquipoRequest(List.of(calificaciones));
    }

    private CalificacionRequest calificacion(
            Long idPerfil,
            Integer valoracion,
            String resenia) {
        return new CalificacionRequest(idPerfil, valoracion, resenia);
    }

    private CalificacionPerfil calificacionExistente(
            Perfil receptor,
            Integer valoracion,
            String resenia) {
        return CalificacionPerfil.builder()
                .proyecto(proyecto)
                .perfilEmisor(perfilActor)
                .perfilReceptor(receptor)
                .valoracion(valoracion)
                .resenia(resenia)
                .fechaRegistro(AHORA)
                .build();
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

    private MiembroProyecto miembro(
            Perfil perfil,
            RolProyectoUsuario rol) {
        return MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(RolProyecto.builder()
                        .codigo(rol.name())
                        .nombre(rol == RolProyectoUsuario.DIRECTOR
                                ? "Director de proyecto"
                                : "Miembro de proyecto")
                        .build())
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
    }
}
