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
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PermisoProyecto;
import org.mgroko.backend.modelo.Planificacion;
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
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.dto.ModificarProyectoRequest;
import org.mgroko.backend.proyectos.dto.ModificarRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.EstadoProyectoNoModificableException;
import org.mgroko.backend.proyectos.exception.FechaEntregaInvalidaException;
import org.mgroko.backend.proyectos.exception.InformacionBasicaIncompletaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.proyectos.exception.RequerimientoSatisfechoException;
import org.mgroko.backend.proyectos.exception.UbicacionObligatoriaException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModificarProyectoServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private ObjetivoRepository objetivoRepository;

    @Mock
    private MoodboardRepository moodboardRepository;

    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;

    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;

    @Mock
    private RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;

    @Mock
    private CupoRequerimientoService cupoRequerimientoService;

    @Mock
    private UbicacionService ubicacionService;

    @Mock
    private RequerimientoGralService requerimientoGralService;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private PlanificadorRecursosService planificadorRecursosService;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private ModificarProyectoService modificarProyectoService;

    private Perfil perfil;
    private Proyecto proyecto;
    private Planificacion planificacion;
    private Moodboard moodboard;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().idUsuario(1L).build();
        perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .build();

        RolProyecto rolDirector = RolProyecto.builder()
                .idRolProyecto(1L)
                .codigo("DIRECTOR")
                .nombre("Director")
                .permisos(Set.of(
                        PermisoProyecto.builder().idPermisoProyecto(2L).nombre("MODIFICAR_PROYECTO").build()))
                .build();

        proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile 2026")
                .descripcion("Vieja")
                .estado(EstadoProyecto.BORRADOR)
                .privacidad(Privacidad.PUBLICO)
                .aceptaPostulacionGral(false)
                .ubicacion(Ubicacion.builder().idUbicacion(50L)
                        .ciudad(Ciudad.builder().nombre("La Plata").build()).build())
                .build();

        planificacion = Planificacion.builder()
                .idPlanificacion(1L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(10))
                .build();

        moodboard = Moodboard.builder()
                .idMoodboard(7L)
                .proyecto(proyecto)
                .descripcion("Vieja paleta")
                .build();

        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));
        when(proyectoRepository.findByIdParaGestion(100L)).thenReturn(Optional.of(proyecto));
        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L)).thenReturn(Optional.of(miembro));
        when(miembroProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of(miembro));
        when(miembroProyectoRepository.buscarMiembrosActivosDeProyecto(100L)).thenReturn(List.of(miembro));
        when(planificacionRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.of(planificacion));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(1L)).thenReturn(List.of());
        when(planificadorRecursosService.planificar(any(), any(), any(), eq(false)))
                .thenReturn(new PlanificadorRecursosService.Resultado(
                        java.util.Map.of(), java.util.Map.of(), null));
        when(notificacionService.crear(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new org.mgroko.backend.modelo.Notificacion());
        when(objetivoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of());
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.of(moodboard));
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of());
        when(requerimientoActividadRepository.existsByProyectoExcluyendoEstado(
                100L, EstadoActividad.CANCELADA)).thenReturn(true);
        when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ubicacionService.obtenerOCrear("12345", "06")).thenReturn(proyecto.getUbicacion());
    }

    private ModificarProyectoRequest requestBasico() {
        return new ModificarProyectoRequest(
                "Nueva descripción",
                Privacidad.PRIVADO,
                true,
                LocalDate.now().plusDays(30),
                null,
                null,
                null,
                null);
    }

    private ModificarProyectoRequest requestPublicado() {
        return new ModificarProyectoRequest(
                "Nueva descripción",
                Privacidad.PUBLICO,
                true,
                LocalDate.now().plusDays(30),
                new UbicacionRequest("12345", "06"),
                null,
                null,
                null);
    }

    @Test
    void modificar_borrador_actualizaDatosYDevuelveResponse() {
        ProyectoResponse response = modificarProyectoService.modificar(1L, 10L, 100L, requestBasico());

        assertThat(response.idProyecto()).isEqualTo(100L);
        assertThat(response.nombre()).isEqualTo("Desfile 2026");
        assertThat(response.descripcion()).isEqualTo("Nueva descripción");
        assertThat(response.privacidad()).isEqualTo("PRIVADO");
        assertThat(response.aceptaPostulacionGral()).isTrue();
        assertThat(response.fechaEntregaEstipulada()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(response.rolUsuario()).isEqualTo(RolProyectoUsuario.DIRECTOR);

        verify(proyectoSecurityService).validarPermiso(100L, 10L, "MODIFICAR_PROYECTO");
        verify(proyectoRepository).save(proyecto);
        verify(planificacionRepository).save(planificacion);
    }

    @Test
    void modificar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> modificarProyectoService.modificar(1L, null, 100L, requestBasico()))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void modificar_sinPermiso_lanzaAccesoDenegado() {
        doThrow(new AccesoDenegadoProyectoException("No tienes el permiso 'MODIFICAR_PROYECTO'."))
                .when(proyectoSecurityService).validarPermiso(100L, 10L, "MODIFICAR_PROYECTO");

        assertThatThrownBy(() -> modificarProyectoService.modificar(1L, 10L, 100L, requestBasico()))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }

    @Test
    void modificar_proyectoFinalizado_lanzaEstadoNoModificable() {
        proyecto.setEstado(EstadoProyecto.FINALIZADO);

        assertThatThrownBy(() -> modificarProyectoService.modificar(1L, 10L, 100L, requestBasico()))
                .isInstanceOf(EstadoProyectoNoModificableException.class)
                .hasMessageContaining("Finalizado");

        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }

    @Test
    void modificar_borradorQuitaUbicacion_laBorra() {
        ModificarProyectoRequest request = new ModificarProyectoRequest(
                "Sin ubicación", Privacidad.PUBLICO, false, null, null, null, null, null);

        modificarProyectoService.modificar(1L, 10L, 100L, request);

        assertThat(proyecto.getUbicacion()).isNull();
    }

    @Test
    void modificar_publicadoQuitaUbicacion_lanzaUbicacionObligatoria() {
        proyecto.setEstado(EstadoProyecto.PUBLICADO);
        ModificarProyectoRequest request = new ModificarProyectoRequest(
                "Sin ubicación", Privacidad.PUBLICO, false, null, null, null, null, null);

        assertThatThrownBy(() -> modificarProyectoService.modificar(1L, 10L, 100L, request))
                .isInstanceOf(UbicacionObligatoriaException.class);
    }

    @Test
    void modificar_fechaEntregaAnteriorAHoy_lanzaExcepcion() {
        ModificarProyectoRequest request = new ModificarProyectoRequest(
                "Desc", Privacidad.PUBLICO, false, LocalDate.now().minusDays(1), null, null, null, null);

        assertThatThrownBy(() -> modificarProyectoService.modificar(1L, 10L, 100L, request))
                .isInstanceOf(FechaEntregaInvalidaException.class);
    }

    @Test
    void modificar_publicadoSinMoodboard_lanzaInformacionIncompleta() {
        proyecto.setEstado(EstadoProyecto.PUBLICADO);
        when(moodboardRepository.findByProyectoIdProyecto(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modificarProyectoService.modificar(1L, 10L, 100L, requestPublicado()))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("moodboard");
    }

    @Test
    void modificar_publicadoSinRequerimientosGralNiDeActividad_lanzaInformacionIncompleta() {
        proyecto.setEstado(EstadoProyecto.PUBLICADO);
        when(requerimientoActividadRepository.existsByProyectoExcluyendoEstado(
                100L, EstadoActividad.CANCELADA)).thenReturn(false);

        assertThatThrownBy(() -> modificarProyectoService.modificar(1L, 10L, 100L, requestPublicado()))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("requerimiento")
                .hasMessageContaining("general o de actividad");
    }

    @Test
    void modificar_publicadoConRequerimientoDeActividad_permiteModificar() {
        proyecto.setEstado(EstadoProyecto.PUBLICADO);
        when(requerimientoActividadRepository.existsByProyectoExcluyendoEstado(
                100L, EstadoActividad.CANCELADA)).thenReturn(true);

        ProyectoResponse response = modificarProyectoService.modificar(1L, 10L, 100L, requestPublicado());

        assertThat(response.estado()).isEqualTo("Publicado");
        assertThat(response.requerimientosGral()).isEmpty();
        verify(proyectoRepository).save(proyecto);
    }

    @Test
    void modificar_requerimientoSatisfechoEnRequest_lanzaExcepcion() {
        RequerimientoGralProyecto satisfecho = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(20L).proyecto(proyecto).cantidad(1).build();
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of(satisfecho));
        when(cupoRequerimientoService.calcularGeneral(satisfecho))
                .thenReturn(new CupoRequerimiento(1, 1, 0, true));

        ModificarProyectoRequest request = new ModificarProyectoRequest(
                "Desc", Privacidad.PUBLICO, false, null, null, null,
                List.of(new ModificarRequerimientoGralRequest(20L, 2, 2L, "editado", null, null)),
                null);

        assertThatThrownBy(() -> modificarProyectoService.modificar(1L, 10L, 100L, request))
                .isInstanceOf(RequerimientoSatisfechoException.class)
                .hasMessageContaining("20");

        verify(requerimientoGralService, never()).eliminar(any());
        verify(requerimientoGralService, never()).persistir(any(), anyLong(), any(), any(), any(), any());
    }

    @Test
    void modificar_proyectoConfirmadoConCambioDeFechaEntrega_reprogramaYNotifica() {
        proyecto.setEstado(EstadoProyecto.CONFIRMADO);
        proyecto.setFechaInicio(java.time.LocalDateTime.now().minusDays(1));
        LocalDate nuevaFecha = LocalDate.now().plusDays(30);
        ModificarProyectoRequest request = new ModificarProyectoRequest(
                "Nueva descripción",
                Privacidad.PRIVADO,
                true,
                nuevaFecha,
                new UbicacionRequest("12345", "06"),
                null,
                List.of(),
                null,
                "Ajuste solicitado por el cliente");

        ProyectoResponse response = modificarProyectoService.modificar(1L, 10L, 100L, request);

        assertThat(response.fechaEntregaEstipulada()).isEqualTo(nuevaFecha);
        assertThat(planificacion.getFechaEntrega()).isEqualTo(nuevaFecha);
        verify(planificadorRecursosService).planificar(
                eq(planificacion), any(), eq(proyecto.getFechaInicio()), eq(false));
        verify(actividadRepository, org.mockito.Mockito.atLeastOnce()).saveAll(any());
        verify(actividadRepository, org.mockito.Mockito.atLeastOnce()).flush();
        verify(notificacionService).registrarTodas(any());
    }

    @Test
    void modificar_proyectoConfirmadoConCambioInviable_noGuardaNiNotifica() {
        proyecto.setEstado(EstadoProyecto.CONFIRMADO);
        proyecto.setFechaInicio(java.time.LocalDateTime.now().minusDays(1));
        when(planificadorRecursosService.planificar(any(), any(), any(), eq(false)))
                .thenThrow(new ProyectoNoConfirmableException(
                        "SIN_DISPONIBILIDAD",
                        "La actividad no entra en la nueva entrega."));

        ModificarProyectoRequest request = new ModificarProyectoRequest(
                "Nueva descripción",
                Privacidad.PRIVADO,
                true,
                LocalDate.now().plusDays(5),
                new UbicacionRequest("12345", "06"),
                null,
                List.of(),
                null,
                null);

        assertThatThrownBy(() -> modificarProyectoService.modificar(1L, 10L, 100L, request))
                .isInstanceOf(ProyectoNoConfirmableException.class);

        verify(proyectoRepository, never()).save(any(Proyecto.class));
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void modificar_requerimientos_reemplazaNoSatisfechosYConservaSatisfechos() {
        RequerimientoGralProyecto noSatisfecho = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(10L).proyecto(proyecto).cantidad(1).build();
        RequerimientoGralProyecto satisfecho = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(20L).proyecto(proyecto).cantidad(1).build();
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L))
                .thenReturn(List.of(noSatisfecho, satisfecho));
        when(cupoRequerimientoService.calcularGeneral(noSatisfecho))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(cupoRequerimientoService.calcularGeneral(satisfecho))
                .thenReturn(new CupoRequerimiento(1, 1, 0, true));

        ModificarProyectoRequest request = new ModificarProyectoRequest(
                "Desc", Privacidad.PUBLICO, false, null, null, null,
                List.of(new ModificarRequerimientoGralRequest(10L, 3, 2L, "recreado", null, null)),
                null);

        ProyectoResponse response = modificarProyectoService.modificar(1L, 10L, 100L, request);

        assertThat(response.requerimientosGral())
                .allSatisfy(requerimiento -> assertThat(requerimiento.puedePostularse()).isFalse());

        verify(requerimientoGralService).eliminar(noSatisfecho);
        verify(requerimientoGralService, never()).eliminar(satisfecho);
        verify(requerimientoGralService).persistir(
                eq(proyecto), eq(2L), eq(3), eq("recreado"), any(), any());
    }

    @Test
    void modificar_requerimientosNull_noTocaLosExistentes() {
        RequerimientoGralProyecto existente = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(10L).proyecto(proyecto).cantidad(1).build();
        when(requerimientoGralProyectoRepository.findByProyectoIdProyecto(100L)).thenReturn(List.of(existente));
        when(cupoRequerimientoService.calcularGeneral(existente))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));

        modificarProyectoService.modificar(1L, 10L, 100L, requestBasico());

        verify(requerimientoGralService, never()).eliminar(any());
        verify(requerimientoGralService, never()).persistir(any(), anyLong(), any(), any(), any(), any());
    }

    @Test
    void modificar_moodboard_actualizaDescripcion() {
        ModificarProyectoRequest request = new ModificarProyectoRequest(
                "Desc", Privacidad.PUBLICO, false, null, null, null, null,
                new CrearMoodboardRequest("Nueva paleta"));

        modificarProyectoService.modificar(1L, 10L, 100L, request);

        assertThat(moodboard.getDescripcion()).isEqualTo("Nueva paleta");
        verify(moodboardRepository).save(moodboard);
    }
}
