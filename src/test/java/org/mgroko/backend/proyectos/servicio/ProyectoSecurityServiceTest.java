package org.mgroko.backend.proyectos.servicio;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PermisoProyecto;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProyectoSecurityServiceTest {

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @InjectMocks
    private ProyectoSecurityService proyectoSecurityService;

    private RolProyecto rolDirector;
    private RolProyecto rolMiembro;
    private Proyecto proyecto;
    private Perfil perfil;

    @BeforeEach
    void setUp() {
        proyecto = Proyecto.builder().idProyecto(100L).build();
        perfil = Perfil.builder().idPerfil(10L).build();

        PermisoProyecto permisoVer = PermisoProyecto.builder().idPermisoProyecto(1L).nombre("VER_PROYECTO").build();
        PermisoProyecto permisoModificar = PermisoProyecto.builder().idPermisoProyecto(2L).nombre("MODIFICAR_PROYECTO").build();
        PermisoProyecto permisoCancelar = PermisoProyecto.builder().idPermisoProyecto(3L).nombre("CANCELAR_PROYECTO").build();

        rolDirector = RolProyecto.builder()
                .idRolProyecto(1L)
                .nombre("Director")
                .permisos(Set.of(permisoVer, permisoModificar, permisoCancelar))
                .build();

        rolMiembro = RolProyecto.builder()
                .idRolProyecto(2L)
                .nombre("Miembro")
                .permisos(Set.of(permisoVer))
                .build();
    }

    @Test
    void tienePermiso_directorConPermiso_devuelveTrue() {
        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();

        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThat(proyectoSecurityService.tienePermiso(100L, 10L, "MODIFICAR_PROYECTO")).isTrue();
        assertThat(proyectoSecurityService.tienePermiso(100L, 10L, "CANCELAR_PROYECTO")).isTrue();
        assertThat(proyectoSecurityService.tienePermiso(100L, 10L, "VER_PROYECTO")).isTrue();
    }

    @Test
    void tienePermiso_miembroSinPermiso_devuelveFalse() {
        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();

        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThat(proyectoSecurityService.tienePermiso(100L, 10L, "VER_PROYECTO")).isTrue();
        assertThat(proyectoSecurityService.tienePermiso(100L, 10L, "MODIFICAR_PROYECTO")).isFalse();
        assertThat(proyectoSecurityService.tienePermiso(100L, 10L, "CANCELAR_PROYECTO")).isFalse();
    }

    @Test
    void tienePermiso_noEsMiembroActivo_devuelveFalse() {
        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 99L))
                .thenReturn(Optional.empty());

        assertThat(proyectoSecurityService.tienePermiso(100L, 99L, "VER_PROYECTO")).isFalse();
    }

    @Test
    void esDirector_directorActivo_devuelveTrue() {
        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();

        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThat(proyectoSecurityService.esDirector(100L, 10L)).isTrue();
    }

    @Test
    void esDirector_miembroOrdinario_devuelveFalse() {
        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();

        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThat(proyectoSecurityService.esDirector(100L, 10L)).isFalse();
    }

    @Test
    void validarPermiso_sinPermiso_lanzaAccesoDenegadoProyectoException() {
        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> proyectoSecurityService.validarPermiso(100L, 10L, "MODIFICAR_PROYECTO"))
                .isInstanceOf(AccesoDenegadoProyectoException.class)
                .hasMessageContaining("No tienes el permiso 'MODIFICAR_PROYECTO'");
    }

    @Test
    void validarDirector_directorValido_noLanzaExcepcion() {
        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();

        when(miembroProyectoRepository.findMiembroActivoConPermisos(100L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThatCode(() -> proyectoSecurityService.validarDirector(100L, 10L))
                .doesNotThrowAnyException();
    }
}
