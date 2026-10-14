package org.mgroko.backend.proyectos.servicio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro;
import org.mgroko.backend.proyectos.dto.ProyectoBusquedaResponse;
import org.mgroko.backend.proyectos.exception.CriterioBusquedaInvalidoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BuscarProyectoServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    private BuscarProyectoService buscarProyectoService;

    @BeforeEach
    void setUp() {
        buscarProyectoService = new BuscarProyectoService(proyectoRepository, miembroProyectoRepository);
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

    private MiembroProyecto crearDirectorMock(Proyecto proyecto, Long idPerfil, String nombreArtistico) {
        return MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(Perfil.builder().idPerfil(idPerfil).nombreArtistico(nombreArtistico).build())
                .rolProyecto(RolProyecto.builder().codigo("DIRECTOR").nombre("Director de proyecto").build())
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
    }

    @Test
    void buscarProyectos_conFiltros_retornaPaginaResponseConDirector() {
        Proyecto p1 = crearProyectoMock(1L, "Desfile Primavera");
        Proyecto p2 = crearProyectoMock(2L, "Desfile Invierno");
        PageImpl<Proyecto> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 20), 2);

        when(proyectoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(anyList()))
                .thenReturn(List.of(crearDirectorMock(p1, 10L, "Luna Diseños")));

        BuscarProyectosFiltro filtro = new BuscarProyectosFiltro("Desfile", EstadoProyecto.BORRADOR, null);
        PaginaResponse<ProyectoBusquedaResponse> resultado =
                buscarProyectoService.buscarProyectos(filtro, 0, 20, false);

        assertThat(resultado).isNotNull();
        assertThat(resultado.contenido()).hasSize(2);
        assertThat(resultado.totalElementos()).isEqualTo(2);
        assertThat(resultado.tamanoPagina()).isEqualTo(20);
        assertThat(resultado.paginaActual()).isEqualTo(0);
        assertThat(resultado.contenido().get(0).nombre()).isEqualTo("Desfile Primavera");
        assertThat(resultado.contenido().get(0).nombreDirector()).isEqualTo("Luna Diseños");
        assertThat(resultado.contenido().get(0).idDirector()).isEqualTo(10L);
        assertThat(resultado.contenido().get(1).nombreDirector()).isNull();
    }

    @Test
    void buscarProyectos_conOpcionTodos_usaPageableUnpaged() {
        Proyecto p1 = crearProyectoMock(1L, "Desfile Primavera");
        PageImpl<Proyecto> page = new PageImpl<>(List.of(p1));

        when(proyectoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(anyList()))
                .thenReturn(List.of(crearDirectorMock(p1, 10L, "Luna Diseños")));

        BuscarProyectosFiltro filtro = new BuscarProyectosFiltro("Desfile", null, null);
        PaginaResponse<ProyectoBusquedaResponse> resultado =
                buscarProyectoService.buscarProyectos(filtro, 0, 0, true);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(proyectoRepository).findAll(any(Specification.class), captor.capture());

        assertThat(captor.getValue().isUnpaged()).isTrue();
        assertThat(resultado.contenido()).hasSize(1);
    }

    @Test
    void buscarProyectos_conSoloEstado_retornaResultados() {
        Proyecto p1 = crearProyectoMock(1L, "Muestra Textil");
        p1.setEstado(EstadoProyecto.PUBLICADO);
        PageImpl<Proyecto> page = new PageImpl<>(List.of(p1), PageRequest.of(0, 20), 1);

        when(proyectoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(anyList()))
                .thenReturn(List.of(crearDirectorMock(p1, 10L, "Luna Diseños")));

        BuscarProyectosFiltro filtro = new BuscarProyectosFiltro(null, EstadoProyecto.PUBLICADO, null);
        PaginaResponse<ProyectoBusquedaResponse> resultado =
                buscarProyectoService.buscarProyectos(filtro, 0, 20, false);

        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.contenido().get(0).estado()).isEqualTo("Publicado");
    }

    @Test
    void buscarProyectos_conSoloDirector_retornaResultados() {
        Proyecto p1 = crearProyectoMock(1L, "Muestra Textil");
        PageImpl<Proyecto> page = new PageImpl<>(List.of(p1), PageRequest.of(0, 20), 1);

        when(proyectoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(anyList()))
                .thenReturn(List.of(crearDirectorMock(p1, 10L, "Luna Diseños")));

        BuscarProyectosFiltro filtro = new BuscarProyectosFiltro(null, null, "Luna");
        PaginaResponse<ProyectoBusquedaResponse> resultado =
                buscarProyectoService.buscarProyectos(filtro, 0, 20, false);

        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.contenido().get(0).nombreDirector()).isEqualTo("Luna Diseños");
    }

    @Test
    void buscarProyectos_sinCoincidencias_retornaPaginaVacia() {
        PageImpl<Proyecto> page = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
        when(proyectoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BuscarProyectosFiltro filtro = new BuscarProyectosFiltro("Inexistente", null, null);
        PaginaResponse<ProyectoBusquedaResponse> resultado =
                buscarProyectoService.buscarProyectos(filtro, 0, 50, false);

        assertThat(resultado.contenido()).isEmpty();
        assertThat(resultado.totalElementos()).isZero();
        assertThat(resultado.tamanoPagina()).isEqualTo(50);
    }

    @Test
    void buscarProyectos_sinCriterio_lanzaExcepcion() {
        BuscarProyectosFiltro filtro = new BuscarProyectosFiltro(null, null, null);

        assertThatThrownBy(() -> buscarProyectoService.buscarProyectos(filtro, 0, 20, false))
                .isInstanceOf(CriterioBusquedaInvalidoException.class)
                .hasMessage("Debes indicar al menos un criterio de búsqueda: nombre, estado o director.");
    }

    @Test
    void buscarProyectos_conSoloBlancos_lanzaExcepcion() {
        BuscarProyectosFiltro filtro = new BuscarProyectosFiltro("   ", null, "  ");

        assertThatThrownBy(() -> buscarProyectoService.buscarProyectos(filtro, 0, 20, false))
                .isInstanceOf(CriterioBusquedaInvalidoException.class);
    }
}
