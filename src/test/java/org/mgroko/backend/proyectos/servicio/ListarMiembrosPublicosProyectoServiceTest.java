package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.MiembroPublicoTycProyectoResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.proyeccion.MiembroPublicoTycProyeccion;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarMiembrosPublicosProyectoServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    @InjectMocks
    private ListarMiembrosPublicosProyectoService service;

    private Proyecto proyecto;
    private Perfil perfilObservador;

    @BeforeEach
    void setUp() {
        proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        perfilObservador = Perfil.builder()
                .idPerfil(30L)
                .nombreArtistico("Observador")
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder()
                        .idUsuario(3L)
                        .nombre("Observador")
                        .apellido("Perez")
                        .build())
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(30L, 3L))
                .thenReturn(Optional.of(perfilObservador));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));
    }

    @Test
    void listar_devuelveMiembrosYSuTycAceptado() {
        MiembroPublicoTycProyeccion proyeccion =
                mock(MiembroPublicoTycProyeccion.class);
        when(proyeccion.getIdPerfil()).thenReturn(20L);
        when(proyeccion.getNombreArtistico()).thenReturn("Sol");
        when(proyeccion.getNombreProfesion()).thenReturn("Modelo");
        when(proyeccion.getCodigoRol()).thenReturn("MIEMBRO");
        when(proyeccion.getIdTycAceptado()).thenReturn(7L);
        when(proyeccion.getNumeroVersion()).thenReturn(2);
        when(proyeccion.getDescripcion())
                .thenReturn("Texto completo del TyC aceptado.");
        when(proyeccion.getFechaInicio())
                .thenReturn(LocalDateTime.of(2026, 11, 1, 10, 0));
        when(proyeccion.getFechaFin()).thenReturn(null);

        when(miembroProyectoRepository.listarMiembrosPublicosConTyc(100L))
                .thenReturn(List.of(proyeccion));

        List<MiembroPublicoTycProyectoResponse> resultado =
                service.listar(3L, 30L, 100L);

        assertThat(resultado).hasSize(1);
        MiembroPublicoTycProyectoResponse fila = resultado.getFirst();
        assertThat(fila.idPerfil()).isEqualTo(20L);
        assertThat(fila.nombreArtistico()).isEqualTo("Sol");
        assertThat(fila.nombreProfesion()).isEqualTo("Modelo");
        assertThat(fila.rol()).isEqualTo("MIEMBRO");
        assertThat(fila.idTycAceptado()).isEqualTo(7L);
        assertThat(fila.numeroVersionAceptado()).isEqualTo(2);
        assertThat(fila.descripcion())
                .isEqualTo("Texto completo del TyC aceptado.");

        verify(proyectoVisibilidadService).validarAcceso(proyecto, 30L);
    }

    @Test
    void listar_proyectoNoVisible_lanzaAccesoDenegado() {
        doThrow(new AccesoDenegadoProyectoException("No tienes acceso a este proyecto."))
                .when(proyectoVisibilidadService)
                .validarAcceso(proyecto, 30L);

        assertThatThrownBy(() -> service.listar(3L, 30L, 100L))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }
}
