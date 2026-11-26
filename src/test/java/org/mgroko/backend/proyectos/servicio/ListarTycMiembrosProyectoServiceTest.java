package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.MiembroTycProyectoResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.proyeccion.MiembroTycProyeccion;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarTycMiembrosProyectoServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @InjectMocks
    private ListarTycMiembrosProyectoService service;

    private Proyecto proyecto;
    private Perfil perfilActor;
    private MiembroProyecto actor;

    @BeforeEach
    void setUp() {
        proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        perfilActor = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder()
                        .idUsuario(1L)
                        .nombre("Luna")
                        .apellido("Perez")
                        .build())
                .build();

        actor = MiembroProyecto.builder()
                .idMiembro(1L)
                .proyecto(proyecto)
                .perfil(perfilActor)
                .rolProyecto(RolProyecto.builder()
                        .idRolProyecto(1L)
                        .codigo("DIRECTOR")
                        .nombre("Director de proyecto")
                        .build())
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfilActor));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));
        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L))
                .thenReturn(Optional.of(actor));
    }

    @Test
    void listar_devuelveElTycAceptadoDeCadaMiembro() {
        MiembroTycProyeccion proyeccion = mock(MiembroTycProyeccion.class);
        when(proyeccion.getIdMiembro()).thenReturn(2L);
        when(proyeccion.getIdPerfil()).thenReturn(20L);
        when(proyeccion.getNombreArtistico()).thenReturn("Sol");
        when(proyeccion.getNombreProfesion()).thenReturn("Modelo");
        when(proyeccion.getIdTycAceptado()).thenReturn(7L);
        when(proyeccion.getNumeroVersion()).thenReturn(2);
        when(proyeccion.getDescripcion()).thenReturn("Texto completo del TyC aceptado.");
        when(proyeccion.getFechaInicio())
                .thenReturn(LocalDateTime.of(2026, 11, 1, 10, 0));
        when(proyeccion.getFechaFin()).thenReturn(null);

        when(miembroProyectoRepository.listarTycAceptadosDeMiembros(100L))
                .thenReturn(List.of(proyeccion));

        List<MiembroTycProyectoResponse> resultado =
                service.listar(1L, 10L, 100L);

        assertThat(resultado).hasSize(1);
        MiembroTycProyectoResponse fila = resultado.getFirst();
        assertThat(fila.idMiembro()).isEqualTo(2L);
        assertThat(fila.idPerfil()).isEqualTo(20L);
        assertThat(fila.nombreArtistico()).isEqualTo("Sol");
        assertThat(fila.nombreProfesion()).isEqualTo("Modelo");
        assertThat(fila.idTycAceptado()).isEqualTo(7L);
        assertThat(fila.numeroVersionAceptado()).isEqualTo(2);
        assertThat(fila.descripcion()).isEqualTo("Texto completo del TyC aceptado.");
        assertThat(fila.fechaInicio())
                .isEqualTo(LocalDateTime.of(2026, 11, 1, 10, 0));
        assertThat(fila.fechaFin()).isNull();

        verify(proyectoSecurityService).validarPermiso(
                100L,
                10L,
                ProyectoSecurityService.PERMISO_VER_PROYECTO);
    }

    @Test
    void listar_sinMembresiaActiva_lanzaAccesoDenegado() {
        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(1L, 10L, 100L))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }
}
