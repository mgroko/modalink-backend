package org.mgroko.backend.requerimientos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.requerimientos.dto.BuscarRequerimientosGlobalFiltro;
import org.mgroko.backend.requerimientos.dto.RequerimientoOportunidadResponse;
import org.mgroko.backend.requerimientos.dto.TipoRequerimiento;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BuscarRequerimientosGeneralesServiceTest {

    private static final Long ID_PERFIL = 10L;
    private static final Long ID_PROFESION = 2L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_REQUERIMIENTO = 30L;

    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralRepository;
    @Mock
    private RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;
    @Mock
    private PostulacionGralRepository postulacionGralRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private CupoRequerimientoService cupoRequerimientoService;

    private BuscarRequerimientosGeneralesService servicio;

    @BeforeEach
    void setUp() {
        servicio = new BuscarRequerimientosGeneralesService(
                requerimientoGralRepository,
                requerimientoGralCaractValorRepository,
                postulacionGralRepository,
                miembroProyectoRepository,
                planificacionRepository,
                perfilRepository,
                cupoRequerimientoService,
                new ElegibilidadRequerimientoService());
    }

    @Test
    void buscar_conPerfilActivo_devuelveOportunidadGeneral() {
        Perfil perfil = Perfil.builder()
                .idPerfil(ID_PERFIL)
                .estado(EstadoPerfil.ACTIVO)
                .profesion(Profesion.builder()
                        .idProfesion(ID_PROFESION)
                        .nombre("Modelo")
                        .build())
                .build();

        Proyecto proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Desfile Primavera")
                .estado(EstadoProyecto.PUBLICADO)
                .aceptaPostulacionGral(true)
                .fechaInicio(LocalDateTime.of(2026, 11, 10, 9, 0))
                .build();

        RequerimientoGralProyecto requerimiento = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(ID_REQUERIMIENTO)
                .cantidad(2)
                .descripcion("Dos modelos")
                .proyecto(proyecto)
                .profesion(perfil.getProfesion())
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        when(perfilRepository.findByIdConProfesion(ID_PERFIL))
                .thenReturn(Optional.of(perfil));
        when(requerimientoGralRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(requerimiento), pageable, 1));
        when(requerimientoGralRepository.findConDetalleByIds(List.of(ID_REQUERIMIENTO)))
                .thenReturn(List.of(requerimiento));
        when(cupoRequerimientoService.calcularGenerales(List.of(requerimiento)))
                .thenReturn(Map.of(ID_REQUERIMIENTO,
                        new CupoRequerimiento(2, 0, 2, false)));
        when(postulacionGralRepository.findEstadosPorPerfilYRequerimientos(
                any(), anyCollection()))
                .thenReturn(List.of());
        when(miembroProyectoRepository.findRolesActivosPorPerfilYProyectos(
                anyCollection(), any()))
                .thenReturn(List.of());
        when(planificacionRepository.findFechasEntregaPorProyectoIds(anyCollection()))
                .thenReturn(List.of());

        PaginaResponse<RequerimientoOportunidadResponse> respuesta = servicio.buscar(
                ID_PERFIL,
                new BuscarRequerimientosGlobalFiltro(
                        null, null, null, null, null, null, null, null, null, null),
                0, 20, false);

        assertThat(respuesta.totalElementos()).isEqualTo(1);
        RequerimientoOportunidadResponse fila = respuesta.contenido().getFirst();
        assertThat(fila.tipo()).isEqualTo(TipoRequerimiento.GENERAL);
        assertThat(fila.idRequerimiento()).isEqualTo(ID_REQUERIMIENTO);
        assertThat(fila.nombreProyecto()).isEqualTo("Desfile Primavera");
        assertThat(fila.nombreProfesion()).isEqualTo("Modelo");
        assertThat(fila.faltantes()).isEqualTo(2);
        assertThat(fila.puedePostularse()).isTrue();
        assertThat(fila.yaPostulado()).isFalse();
        assertThat(fila.requiereVerificacionDisponibilidad()).isFalse();
    }
}
