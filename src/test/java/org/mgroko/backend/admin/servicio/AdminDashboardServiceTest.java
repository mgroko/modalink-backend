package org.mgroko.backend.admin.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.AdminDashboardProfesionRow;
import org.mgroko.backend.admin.dto.AdminDashboardProvinciaRow;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @InjectMocks
    private AdminDashboardService service;

    @Test
    void resumen_agrupaActividadYCalculaPorcentajeSobreProyectosNoBorrador() {
        when(usuarioRepository.count()).thenReturn(10L);
        when(proyectoRepository.countByEstado(EstadoProyecto.CONFIRMADO)).thenReturn(2L);
        when(proyectoRepository.countByEstadoNot(EstadoProyecto.BORRADOR)).thenReturn(4L);
        when(usuarioRepository.contarPerfilesActivosPorProvincia()).thenReturn(
                List.of(new AdminDashboardProvinciaRow(1L, "Buenos Aires", 3L)));
        when(proyectoRepository.contarProyectosNoBorradorPorProvincia()).thenReturn(
                List.of(new AdminDashboardProvinciaRow(1L, "Buenos Aires", 2L),
                        new AdminDashboardProvinciaRow(2L, "Cordoba", 1L)));
        when(perfilRepository.contarPerfilesActivosPorProfesion(any(Pageable.class))).thenReturn(
                List.of(new AdminDashboardProfesionRow(5L, "Diseño", 3L)));
        when(proyectoRepository.findByEstadoNotOrderByFechaInicioDesc(
                eq(EstadoProyecto.BORRADOR), any(Pageable.class))).thenReturn(List.of());

        var resumen = service.obtenerResumen(10);

        assertEquals(10L, resumen.totalUsuarios());
        assertEquals(50.0, resumen.porcentajeConfirmados());
        assertEquals(2, resumen.actividadProvincia().size());
        assertEquals(5L, resumen.actividadProvincia().get(0).totalActividad());
        assertEquals(3L, resumen.actividadProvincia().get(0).totalPerfiles());
        assertEquals(2L, resumen.actividadProvincia().get(0).totalProyectos());
        assertEquals(2, resumen.topProvincias().size());
        assertEquals(5L, resumen.topProvincias().get(0).totalActividad());
        assertEquals(5L, resumen.profesionMasDemandada().idProfesion());
    }

    @Test
    void listarProyectos_sinFiltroExcluyeBorradores() {
        when(proyectoRepository.findByEstadoNot(eq(EstadoProyecto.BORRADOR), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listarProyectos(null, 0, 20);

        verify(proyectoRepository).findByEstadoNot(eq(EstadoProyecto.BORRADOR), any(Pageable.class));
    }

    @Test
    void listarProyectos_conFiltroRespetaBorradorExplicito() {
        when(proyectoRepository.findByEstadoIn(eq(List.of(EstadoProyecto.BORRADOR)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listarProyectos(new EstadoProyecto[] {EstadoProyecto.BORRADOR}, 0, 20);

        verify(proyectoRepository).findByEstadoIn(
                eq(List.of(EstadoProyecto.BORRADOR)), any(Pageable.class));
    }

    @Test
    void listarProyectos_rechazaParametrosFueraDeRango() {
        assertThrows(IllegalArgumentException.class,
                () -> service.listarProyectos(null, -1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> service.listarProyectos(null, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.obtenerResumen(101));
    }
}
