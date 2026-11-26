package org.mgroko.backend.postulaciones.casting.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.postulaciones.casting.dto.RequerimientoCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.TableroCastingResponse;
import org.mgroko.backend.postulaciones.casting.enums.OrdenCasting;
import org.mgroko.backend.postulaciones.dto.BuscarPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.ConteosPostulacionesResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionBusquedaResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionesPaginaResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.servicio.BuscarPostulacionesProyectoService;
import org.mgroko.backend.repositorio.CaracteristicaPerfilRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PolaroidPerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;

@ExtendWith(MockitoExtension.class)
class ObtenerTableroCastingServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private BuscarPostulacionesProyectoService buscarPostulacionesProyectoService;
    @Mock
    private RequerimientoCastingService requerimientoCastingService;
    @Mock
    private EvaluadorAgendaCastingService evaluadorAgendaCastingService;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private CaracteristicaPerfilRepository caracteristicaPerfilRepository;
    @Mock
    private PolaroidPerfilRepository polaroidPerfilRepository;
    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    @InjectMocks
    private ObtenerTableroCastingService service;

    @Test
    void paginaDeSeisCandidatosYTotalDeSiete() {
        preparar(7, 7);

        TableroCastingResponse respuesta = service.obtener(
                10L, 5L, TipoPostulacion.GENERAL, 50L, null,
                OrdenCasting.CUMPLIMIENTO, 0, null);

        assertThat(respuesta.contenido()).hasSize(6);
        assertThat(respuesta.totalElementos()).isEqualTo(7);
        assertThat(respuesta.totalPaginas()).isEqualTo(2);
        assertThat(respuesta.paginaActual()).isZero();
        assertThat(respuesta.primera()).isTrue();
        assertThat(respuesta.ultima()).isFalse();
        assertThat(respuesta.tamanoPagina()).isEqualTo(6);
    }

    @Test
    void idPostulacionDestacadaResuelveLaPaginaQueLaContiene() {
        preparar(7, 7);

        TableroCastingResponse respuesta = service.obtener(
                10L, 5L, TipoPostulacion.GENERAL, 50L, null,
                OrdenCasting.NOMBRE, 0, 7L);

        assertThat(respuesta.paginaActual()).isEqualTo(1);
        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().get(0).idPostulacion()).isEqualTo(7L);
        assertThat(respuesta.primera()).isFalse();
        assertThat(respuesta.ultima()).isTrue();
    }

    @Test
    void paginaFueraDeRangoSeAjustaALaUltima() {
        preparar(7, 7);

        TableroCastingResponse respuesta = service.obtener(
                10L, 5L, TipoPostulacion.GENERAL, 50L, null,
                OrdenCasting.CUMPLIMIENTO, 99, null);

        assertThat(respuesta.paginaActual()).isEqualTo(1);
        assertThat(respuesta.contenido()).hasSize(1);
    }

    private void preparar(int cantidad, long pendientes) {
        when(proyectoRepository.findById(5L)).thenReturn(Optional.of(
                Proyecto.builder().idProyecto(5L).nombre("Proyecto").build()));

        List<PostulacionBusquedaResponse> postulaciones = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            postulaciones.add(postulacion(i));
        }

        when(buscarPostulacionesProyectoService.buscar(
                eq(10L), eq(5L), any(BuscarPostulacionesFiltro.class),
                eq(0), eq(0), eq(true)))
                .thenReturn(new PostulacionesPaginaResponse(
                        postulaciones, 0, 0, cantidad, 1, true, true,
                        new ConteosPostulacionesResponse(pendientes, 0, 0, pendientes)));

        when(requerimientoCastingService.cargar(5L, TipoPostulacion.GENERAL, 50L, pendientes))
                .thenReturn(requerimiento(pendientes));
        when(perfilRepository.findByIdPerfilInConDatosCasting(anyCollection()))
                .thenReturn(List.of());
        when(caracteristicaPerfilRepository.findByPerfil_IdPerfilIn(anyCollection()))
                .thenReturn(List.of());
        when(polaroidPerfilRepository
                .findByPerfil_IdPerfilInOrderByPerfil_IdPerfilAscOrdenAsc(anyCollection()))
                .thenReturn(List.of());
        when(configuracionSistemaService.obtenerCandidatosPorPaginaCasting())
                .thenReturn(6);
    }

    private PostulacionBusquedaResponse postulacion(long id) {
        return new PostulacionBusquedaResponse(
                id,
                TipoPostulacion.GENERAL,
                EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 20, 10, 0).plusMinutes(id),
                id,
                "A" + (id - 1),
                2L,
                "Modelo",
                50L,
                "Dos modelos",
                null,
                null,
                2,
                0L,
                2,
                false,
                false,
                true,
                true);
    }

    private RequerimientoCastingResponse requerimiento(long pendientes) {
        return new RequerimientoCastingResponse(
                TipoPostulacion.GENERAL,
                50L,
                null,
                null,
                "Dos modelos",
                2L,
                "MODELO",
                "Modelo",
                2,
                0,
                2,
                false,
                false,
                pendientes,
                false,
                null,
                null,
                null,
                List.of(),
                List.of());
    }
}
