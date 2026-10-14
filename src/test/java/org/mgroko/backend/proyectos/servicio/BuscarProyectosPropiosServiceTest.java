package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro;
import org.mgroko.backend.proyectos.dto.ProyectoPropioResponse;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BuscarProyectosPropiosServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private ActividadRepository actividadRepository;

    private BuscarProyectosPropiosService service;

    @BeforeEach
    void setUp() {
        service = new BuscarProyectosPropiosService(
                proyectoRepository, miembroProyectoRepository, actividadRepository);
    }

    private Proyecto crearProyectoMock(Long id, String nombre) {
        return Proyecto.builder()
                .idProyecto(id)
                .nombre(nombre)
                .descripcion("Descripción de prueba")
                .estado(EstadoProyecto.BORRADOR)
                .privacidad(Privacidad.Publico)
                .build();
    }

    private MiembroProyecto crearMiembroMock(Proyecto proyecto, String codigoRol) {
        return MiembroProyecto.builder()
                .proyecto(proyecto)
                .rolProyecto(RolProyecto.builder().codigo(codigoRol)
                        .nombre("DIRECTOR".equals(codigoRol) ? "Director de proyecto" : "Miembro de proyecto")
                        .build())
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
    }

    private Actividad crearActividadMock(Proyecto proyecto, String nombre) {
        return Actividad.builder()
                .idActividad(500L)
                .nombre(nombre)
                .duracionMinutos(180)
                .fechaHoraInicio(LocalDateTime.now().plusDays(1).withHour(15).withMinute(0))
                .planificacion(Planificacion.builder().proyecto(proyecto).build())
                .build();
    }

    @Test
    void buscar_conProyectos_retornaRolYProximaActividad() {
        Proyecto p1 = crearProyectoMock(1L, "Desfile Primavera");
        Proyecto p2 = crearProyectoMock(2L, "Muestra Textil");
        PageImpl<Proyecto> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 20), 2);

        when(proyectoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(miembroProyectoRepository.buscarMiembrosActivosDeProyectos(anyList(), anyLong()))
                .thenReturn(List.of(crearMiembroMock(p1, "DIRECTOR"), crearMiembroMock(p2, "MIEMBRO")));
        when(actividadRepository.findProximasActividadesAsignadas(anyCollection(), anyLong(), any()))
                .thenReturn(List.of(crearActividadMock(p1, "Sesion")));

        BuscarProyectosPropiosFiltro filtro = new BuscarProyectosPropiosFiltro(null, null, null);
        PaginaResponse<ProyectoPropioResponse> resultado = service.buscar(10L, filtro, 0, 20, false);

        assertThat(resultado.contenido()).hasSize(2);
        assertThat(resultado.totalElementos()).isEqualTo(2);

        var primero = resultado.contenido().get(0);
        assertThat(primero.nombre()).isEqualTo("Desfile Primavera");
        assertThat(primero.rolUsuario()).isEqualTo(RolProyectoUsuario.DIRECTOR);
        assertThat(primero.proximaActividad()).isNotNull();
        assertThat(primero.proximaActividad().nombre()).isEqualTo("Sesion");
        assertThat(primero.proximaActividad().duracionMinutos()).isEqualTo(180);

        var segundo = resultado.contenido().get(1);
        assertThat(segundo.rolUsuario()).isEqualTo(RolProyectoUsuario.MIEMBRO);
        assertThat(segundo.proximaActividad()).isNull();
    }

    @Test
    void buscar_conPerfilActivoPasaAlScoping() {
        Proyecto p1 = crearProyectoMock(1L, "Desfile Primavera");
        PageImpl<Proyecto> page = new PageImpl<>(List.of(p1), PageRequest.of(0, 20), 1);

        when(proyectoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(miembroProyectoRepository.buscarMiembrosActivosDeProyectos(anyList(), anyLong()))
                .thenReturn(List.of(crearMiembroMock(p1, "MIEMBRO")));
        when(actividadRepository.findProximasActividadesAsignadas(anyCollection(), anyLong(), any()))
                .thenReturn(List.of());

        BuscarProyectosPropiosFiltro filtro = new BuscarProyectosPropiosFiltro(
                "Desfile", EstadoProyecto.BORRADOR, RolProyectoUsuario.DIRECTOR);
        service.buscar(10L, filtro, 0, 20, false);

        verify(miembroProyectoRepository).buscarMiembrosActivosDeProyectos(List.of(1L), 10L);
        verify(actividadRepository).findProximasActividadesAsignadas(
                org.mockito.ArgumentMatchers.eq(List.of(1L)),
                org.mockito.ArgumentMatchers.eq(10L),
                any());
    }

    @Test
    void buscar_conOpcionTodos_usaPageableUnpaged() {
        Proyecto p1 = crearProyectoMock(1L, "Desfile Primavera");
        PageImpl<Proyecto> page = new PageImpl<>(List.of(p1));

        when(proyectoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(miembroProyectoRepository.buscarMiembrosActivosDeProyectos(anyList(), anyLong()))
                .thenReturn(List.of(crearMiembroMock(p1, "DIRECTOR")));
        when(actividadRepository.findProximasActividadesAsignadas(anyCollection(), anyLong(), any()))
                .thenReturn(List.of());

        BuscarProyectosPropiosFiltro filtro = new BuscarProyectosPropiosFiltro(null, null, null);
        PaginaResponse<ProyectoPropioResponse> resultado = service.buscar(10L, filtro, 0, 0, true);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(proyectoRepository).findAll(any(Specification.class), captor.capture());

        assertThat(captor.getValue().isUnpaged()).isTrue();
        assertThat(resultado.contenido()).hasSize(1);
    }

    @Test
    void buscar_sinCoincidencias_retornaPaginaVaciaSinConsultasExtra() {
        PageImpl<Proyecto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(proyectoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BuscarProyectosPropiosFiltro filtro = new BuscarProyectosPropiosFiltro("Inexistente", null, null);
        PaginaResponse<ProyectoPropioResponse> resultado = service.buscar(10L, filtro, 0, 20, false);

        assertThat(resultado.contenido()).isEmpty();
        assertThat(resultado.totalElementos()).isZero();
        verify(miembroProyectoRepository, never()).buscarMiembrosActivosDeProyectos(anyList(), anyLong());
        verify(actividadRepository, never()).findProximasActividadesAsignadas(anyCollection(), anyLong(), any());
    }

    @Test
    void buscar_sinPerfilActivo_lanzaExcepcion() {
        BuscarProyectosPropiosFiltro filtro = new BuscarProyectosPropiosFiltro(null, null, null);

        assertThatThrownBy(() -> service.buscar(null, filtro, 0, 20, false))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }
}
