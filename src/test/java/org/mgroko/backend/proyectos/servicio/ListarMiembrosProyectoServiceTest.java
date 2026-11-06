package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PermisoProyecto;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.MiembroProyectoResponse;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarMiembrosProyectoServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @InjectMocks
    private ListarMiembrosProyectoService service;

    private Proyecto proyecto;
    private Perfil perfilActor;
    private MiembroProyecto actor;
    private MiembroProyecto miembro;

    @BeforeEach
    void setUp() {
        proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        perfilActor = perfil(10L, 1L, "Luna");
        actor = miembro(1L, perfilActor, rol(1L, "DIRECTOR", "Director de proyecto"),
                EstadoParticipacion.ACTIVO);

        Perfil perfilMiembro = perfil(20L, 2L, "Sol");
        miembro = miembro(2L, perfilMiembro, rol(2L, "MIEMBRO", "Miembro de proyecto"),
                EstadoParticipacion.ACTIVO);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfilActor));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));
        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L))
                .thenReturn(Optional.of(actor));
        when(miembroProyectoRepository.buscarActivosDeProyectoConPerfilProfesionYRol(100L))
                .thenReturn(List.of(actor, miembro));
    }

    @Test
    void listar_calculaEliminableParaDirectorYMiembro() {
        PaginaResponse<MiembroProyectoResponse> pagina = service.listar(
                1L, 10L, 100L, null, null, null, 0, 20, true);

        MiembroProyectoResponse filaActor = pagina.contenido().get(0);
        MiembroProyectoResponse filaMiembro = pagina.contenido().get(1);

        assertThat(filaActor.eliminable()).isFalse();
        assertThat(filaActor.motivoNoEliminable())
                .isEqualTo(ListarMiembrosProyectoService.MOTIVO_AUTOELIMINACION);

        assertThat(filaMiembro.eliminable()).isTrue();
        assertThat(filaMiembro.motivoNoEliminable()).isNull();
    }

    @Test
    void listar_otroDirectorConDirectorActivo_esEliminable() {
        miembro.setRolProyecto(rol(3L, "DIRECTOR", "Director de proyecto"));
        when(miembroProyectoRepository.buscarActivosDeProyectoConPerfilProfesionYRol(100L))
                .thenReturn(List.of(actor, miembro));

        PaginaResponse<MiembroProyectoResponse> pagina = service.listar(
                1L, 10L, 100L, null, null, null, 0, 20, true);

        assertThat(pagina.contenido().get(1).eliminable()).isTrue();
        assertThat(pagina.contenido().get(1).motivoNoEliminable()).isNull();
    }

    @Test
    void listar_proyectoEnBorrador_marcaProyectoNoPermiteBaja() {
        proyecto.setEstado(EstadoProyecto.BORRADOR);

        PaginaResponse<MiembroProyectoResponse> pagina = service.listar(
                1L, 10L, 100L, null, null, null, 0, 20, true);

        assertThat(pagina.contenido())
                .allSatisfy(fila -> {
                    assertThat(fila.eliminable()).isFalse();
                    assertThat(fila.motivoNoEliminable())
                            .isEqualTo(ListarMiembrosProyectoService.MOTIVO_PROYECTO_NO_PERMITE_BAJA);
                });
    }

    @Test
    void listar_actorSinPermisoDeEliminar_marcaSinPermiso() {
        actor.getRolProyecto().setPermisos(Set.of(
                PermisoProyecto.builder()
                        .idPermisoProyecto(1L)
                        .nombre(ProyectoSecurityService.PERMISO_VER_PROYECTO)
                        .build()));

        PaginaResponse<MiembroProyectoResponse> pagina = service.listar(
                1L, 10L, 100L, null, null, null, 0, 20, true);

        assertThat(pagina.contenido())
                .allSatisfy(fila -> {
                    assertThat(fila.eliminable()).isFalse();
                    assertThat(fila.motivoNoEliminable())
                            .isEqualTo(ListarMiembrosProyectoService.MOTIVO_SIN_PERMISO);
                });
    }

    private Perfil perfil(Long idPerfil, Long idUsuario, String nombre) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombre)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder()
                        .idUsuario(idUsuario)
                        .nombre(nombre)
                        .apellido("Perez")
                        .build())
                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                .build();
    }

    private MiembroProyecto miembro(
            Long idMiembro,
            Perfil perfil,
            RolProyecto rol,
            EstadoParticipacion estado) {
        return MiembroProyecto.builder()
                .idMiembro(idMiembro)
                .perfil(perfil)
                .rolProyecto(rol)
                .estadoParticipacion(estado)
                .proyecto(proyecto)
                .build();
    }

    private RolProyecto rol(Long idRol, String codigo, String nombre) {
        return RolProyecto.builder()
                .idRolProyecto(idRol)
                .codigo(codigo)
                .nombre(nombre)
                .permisos(Set.of(
                        PermisoProyecto.builder()
                                .idPermisoProyecto(1L)
                                .nombre(ProyectoSecurityService.PERMISO_VER_PROYECTO)
                                .build(),
                        PermisoProyecto.builder()
                                .idPermisoProyecto(2L)
                                .nombre(ProyectoSecurityService.PERMISO_ELIMINAR_INTEGRANTE)
                                .build()))
                .build();
    }
}
