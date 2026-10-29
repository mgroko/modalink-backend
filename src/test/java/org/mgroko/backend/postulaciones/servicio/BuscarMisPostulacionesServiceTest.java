package org.mgroko.backend.postulaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.postulaciones.dto.MiPostulacionResponse;
import org.mgroko.backend.postulaciones.dto.MisPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuscarMisPostulacionesServiceTest {

    @Mock
    private PostulacionGralRepository postulacionGralRepository;
    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;

    private BuscarMisPostulacionesService service;

    private PostulacionGral generalPendiente;
    private PostulacionGral generalRechazada;
    private PostulacionActividad actividadPendiente;

    @BeforeEach
    void setUp() {
        service = new BuscarMisPostulacionesService(
                postulacionGralRepository, postulacionActividadRepository);

        Proyecto proyectoUno = Proyecto.builder().idProyecto(5L).nombre("Desfile Uno").build();
        Proyecto proyectoDos = Proyecto.builder().idProyecto(6L).nombre("Desfile Dos").build();

        Perfil perfil = Perfil.builder().idPerfil(10L).build();

        Profesion modelo = Profesion.builder().idProfesion(2L).nombre("Modelo").build();
        Profesion fotografo = Profesion.builder().idProfesion(3L).nombre("Fotógrafo").build();

        RequerimientoGralProyecto requerimientoUno = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(30L)
                .proyecto(proyectoUno)
                .profesion(modelo)
                .descripcion("Una modelo")
                .cantidad(1)
                .build();

        RequerimientoGralProyecto requerimientoDos = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(31L)
                .proyecto(proyectoDos)
                .profesion(fotografo)
                .descripcion("Un fotógrafo")
                .cantidad(1)
                .build();

        generalPendiente = PostulacionGral.builder()
                .idPostulacionGral(77L)
                .perfil(perfil)
                .requerimientoGral(requerimientoUno)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaPostulacion(LocalDateTime.of(2026, 10, 26, 10, 0))
                .build();

        generalRechazada = PostulacionGral.builder()
                .idPostulacionGral(78L)
                .perfil(perfil)
                .requerimientoGral(requerimientoDos)
                .estado(EstadoSolicitud.RECHAZADA)
                .fechaPostulacion(LocalDateTime.of(2026, 10, 25, 9, 0))
                .build();

        Actividad actividad = Actividad.builder()
                .idActividad(40L)
                .nombre("Prueba de vestuario")
                .estado(EstadoActividad.PENDIENTE)
                .duracionMinutos(120)
                .planificacion(Planificacion.builder()
                        .idPlanificacion(20L)
                        .proyecto(proyectoUno)
                        .build())
                .build();

        RequerimientoActividad requerimientoActividad = RequerimientoActividad.builder()
                .idRequerimiento(50L)
                .actividad(actividad)
                .profesion(modelo)
                .descripcion("Una modelo para vestuario")
                .cantidad(1)
                .build();

        actividadPendiente = PostulacionActividad.builder()
                .idPostulacion(88L)
                .perfil(perfil)
                .requerimiento(requerimientoActividad)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaPostulacion(LocalDateTime.of(2026, 10, 27, 11, 0))
                .build();
    }

    @Test
    void buscar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.buscar(
                null, new MisPostulacionesFiltro(null, null, null), 0, 20, false))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void buscar_devuelveGeneralesYActividadesOrdenadasPorFechaDesc() {
        when(postulacionGralRepository.findByPerfil_IdPerfil(10L))
                .thenReturn(List.of(generalPendiente, generalRechazada));
        when(postulacionActividadRepository.findByPerfil_IdPerfil(10L))
                .thenReturn(List.of(actividadPendiente));

        PaginaResponse<MiPostulacionResponse> pagina = service.buscar(
                10L, new MisPostulacionesFiltro(null, null, null), 0, 20, false);

        assertThat(pagina.totalElementos()).isEqualTo(3);
        assertThat(pagina.contenido())
                .extracting(MiPostulacionResponse::idPostulacion)
                .containsExactly(88L, 77L, 78L);
        assertThat(pagina.contenido().get(0).tipo()).isEqualTo(TipoPostulacion.ACTIVIDAD);
        assertThat(pagina.contenido().get(0).nombreActividad()).isEqualTo("Prueba de vestuario");
        assertThat(pagina.contenido().get(0).cantidad()).isEqualTo(1);
        assertThat(pagina.contenido().get(0).nombreProfesion()).isEqualTo("Modelo");
        assertThat(pagina.contenido().get(1).tipo()).isEqualTo(TipoPostulacion.GENERAL);
        assertThat(pagina.contenido().get(1).nombreProyecto()).isEqualTo("Desfile Uno");
        assertThat(pagina.contenido().get(1).cantidad()).isEqualTo(1);
        assertThat(pagina.contenido().get(1).nombreProfesion()).isEqualTo("Modelo");
    }

    @Test
    void buscar_marcaPuedeDarseDeBajaSoloSiEstaPendiente() {
        when(postulacionGralRepository.findByPerfil_IdPerfil(10L))
                .thenReturn(List.of(generalPendiente, generalRechazada));
        when(postulacionActividadRepository.findByPerfil_IdPerfil(10L))
                .thenReturn(List.of());

        PaginaResponse<MiPostulacionResponse> pagina = service.buscar(
                10L, new MisPostulacionesFiltro(null, null, null), 0, 20, false);

        assertThat(pagina.contenido())
                .filteredOn(postulacion -> postulacion.idPostulacion().equals(77L))
                .singleElement()
                .satisfies(postulacion -> assertThat(postulacion.puedeDarseDeBaja()).isTrue());
        assertThat(pagina.contenido())
                .filteredOn(postulacion -> postulacion.idPostulacion().equals(78L))
                .singleElement()
                .satisfies(postulacion -> assertThat(postulacion.puedeDarseDeBaja()).isFalse());
    }

    @Test
    void buscar_filtraPorProyectoEstadoYTipo() {
        when(postulacionGralRepository.findByPerfil_IdPerfil(10L))
                .thenReturn(List.of(generalPendiente, generalRechazada));
        when(postulacionActividadRepository.findByPerfil_IdPerfil(10L))
                .thenReturn(List.of(actividadPendiente));

        PaginaResponse<MiPostulacionResponse> soloGeneral = service.buscar(
                10L, new MisPostulacionesFiltro(null, null, TipoPostulacion.GENERAL), 0, 20, false);
        assertThat(soloGeneral.contenido()).hasSize(2);

        PaginaResponse<MiPostulacionResponse> soloPendientes = service.buscar(
                10L, new MisPostulacionesFiltro(null, EstadoSolicitud.PENDIENTE, null), 0, 20, false);
        assertThat(soloPendientes.contenido())
                .extracting(MiPostulacionResponse::idPostulacion)
                .containsExactly(88L, 77L);

        PaginaResponse<MiPostulacionResponse> delProyecto = service.buscar(
                10L, new MisPostulacionesFiltro(6L, null, null), 0, 20, false);
        assertThat(delProyecto.contenido())
                .extracting(MiPostulacionResponse::idPostulacion)
                .containsExactly(78L);
    }

    @Test
    void buscar_paginaLosResultados() {
        when(postulacionGralRepository.findByPerfil_IdPerfil(10L))
                .thenReturn(List.of(generalPendiente, generalRechazada));
        when(postulacionActividadRepository.findByPerfil_IdPerfil(10L))
                .thenReturn(List.of(actividadPendiente));

        PaginaResponse<MiPostulacionResponse> primera = service.buscar(
                10L, new MisPostulacionesFiltro(null, null, null), 0, 2, false);
        assertThat(primera.contenido())
                .extracting(MiPostulacionResponse::idPostulacion)
                .containsExactly(88L, 77L);
        assertThat(primera.totalPaginas()).isEqualTo(2);
        assertThat(primera.primera()).isTrue();
        assertThat(primera.ultima()).isFalse();

        PaginaResponse<MiPostulacionResponse> segunda = service.buscar(
                10L, new MisPostulacionesFiltro(null, null, null), 1, 2, false);
        assertThat(segunda.contenido())
                .extracting(MiPostulacionResponse::idPostulacion)
                .containsExactly(78L);
        assertThat(segunda.ultima()).isTrue();
    }

    @Test
    void buscar_todosIgnoraLaPaginacion() {
        when(postulacionGralRepository.findByPerfil_IdPerfil(10L))
                .thenReturn(List.of(generalPendiente, generalRechazada));
        when(postulacionActividadRepository.findByPerfil_IdPerfil(10L))
                .thenReturn(List.of(actividadPendiente));

        PaginaResponse<MiPostulacionResponse> pagina = service.buscar(
                10L, new MisPostulacionesFiltro(null, null, null), 1, 1, true);

        assertThat(pagina.contenido()).hasSize(3);
        assertThat(pagina.totalElementos()).isEqualTo(3);
    }
}
