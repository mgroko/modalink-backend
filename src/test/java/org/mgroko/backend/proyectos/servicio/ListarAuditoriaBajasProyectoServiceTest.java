package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.proyectos.dto.AuditoriaBajaResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListarAuditoriaBajasProyectoServiceTest {

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    private ListarAuditoriaBajasProyectoService service;

    @BeforeEach
    void setUp() {
        service = new ListarAuditoriaBajasProyectoService(
                perfilRepository,
                proyectoRepository,
                miembroProyectoRepository,
                asignacionActividadRepository,
                proyectoSecurityService);

        Perfil perfil = Perfil.builder()
                .idPerfil(10L)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder().idUsuario(1L).build())
                .build();
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(100L))
                .thenReturn(Optional.of(Proyecto.builder().idProyecto(100L).build()));
    }

    @Test
    void listar_devuelveBajasConAutorYActividades() {
        when(miembroProyectoRepository.findHistoricosDeProyectoParaAuditoria(100L))
                .thenReturn(List.of(miembro()));

        Actividad actividad = Actividad.builder()
                .idActividad(20L)
                .nombre("Montaje")
                .estado(EstadoActividad.EnCurso)
                .build();
        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(30L)
                .profesion(Profesion.builder().idProfesion(2L).nombre("Fotógrafo").build())
                .build();
        AsignacionActividad asignacion = AsignacionActividad.builder()
                .idAsignacionAct(40L)
                .miembro(miembro())
                .actividad(actividad)
                .requerimientoActividad(requerimiento)
                .build();
        when(asignacionActividadRepository
                .findHistoricasPorMiembrosConActividadYRequerimiento(List.of(55L)))
                .thenReturn(List.of(asignacion));

        PaginaResponse<AuditoriaBajaResponse> pagina = service.listar(
                1L, 10L, 100L, null, null, null, null, 0, 20, false);

        assertThat(pagina.contenido()).singleElement().satisfies(baja -> {
            assertThat(baja.idMiembro()).isEqualTo(55L);
            assertThat(baja.estadoParticipacion()).isEqualTo("BAJA_VOLUNTARIA");
            assertThat(baja.motivoBaja()).isEqualTo("Motivos personales");
            assertThat(baja.autorBaja()).isNotNull();
            assertThat(baja.autorBaja().idPerfil()).isEqualTo(20L);
            assertThat(baja.actividades()).singleElement().satisfies(act -> {
                assertThat(act.idActividad()).isEqualTo(20L);
                assertThat(act.nombreActividad()).isEqualTo("Montaje");
                assertThat(act.idRequerimiento()).isEqualTo(30L);
                assertThat(act.nombreProfesion()).isEqualTo("Fotógrafo");
            });
        });

        verify(proyectoSecurityService).validarDirector(100L, 10L);
    }

    @Test
    void listar_sinDirector_lanzaAccesoDenegado() {
        doThrow(new AccesoDenegadoProyectoException("No es director."))
                .when(proyectoSecurityService).validarDirector(100L, 10L);

        assertThatThrownBy(() -> service.listar(
                1L, 10L, 100L, null, null, null, null, 0, 20, false))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    private MiembroProyecto miembro() {
        Perfil perfil = Perfil.builder()
                .idPerfil(12L)
                .nombreArtistico("Luna")
                .usuario(Usuario.builder()
                        .idUsuario(7L)
                        .nombre("Ana")
                        .apellido("Gómez")
                        .build())
                .build();
        Perfil autor = Perfil.builder()
                .idPerfil(20L)
                .nombreArtistico("Director")
                .usuario(Usuario.builder()
                        .idUsuario(3L)
                        .nombre("Carlos")
                        .apellido("Pérez")
                        .build())
                .build();

        return MiembroProyecto.builder()
                .idMiembro(55L)
                .perfil(perfil)
                .perfilAutorBaja(autor)
                .rolProyecto(RolProyecto.builder()
                        .idRolProyecto(2L)
                        .codigo("MIEMBRO")
                        .nombre("Miembro")
                        .build())
                .estadoParticipacion(EstadoParticipacion.BajaVoluntaria)
                .motivoBaja("Motivos personales")
                .fechaAlta(LocalDateTime.of(2026, 9, 1, 10, 0))
                .fechaBaja(LocalDateTime.of(2026, 10, 15, 18, 30))
                .build();
    }
}
