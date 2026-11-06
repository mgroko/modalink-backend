package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.BuscarRequerimientosActividadFiltro;
import org.mgroko.backend.actividades.dto.RequerimientoActividadBusquedaResponse;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuscarRequerimientoActividadServiceTest {

    private static final Long ID_PERFIL = 10L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_REQUERIMIENTO = 30L;

    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    private BuscarRequerimientoActividadService servicio;

    @BeforeEach
    void setUp() {
        servicio = new BuscarRequerimientoActividadService(
                requerimientoActividadRepository,
                requerimientoActCaractValorRepository,
                proyectoRepository,
                perfilRepository,
                proyectoVisibilidadService);
    }

    @Test
    void buscar_conProyectoVisible_devuelvePaginaMapeada() {
        Perfil perfil = Perfil.builder()
                .idPerfil(ID_PERFIL)
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder().idUsuario(1L).build())
                .build();
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Proyecto UC73")
                .build();
        RequerimientoActividad requerimiento = requerimiento(proyecto);

        when(perfilRepository.findByIdConProfesion(ID_PERFIL))
                .thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(ID_PROYECTO))
                .thenReturn(Optional.of(proyecto));

        Pageable pageable = PageRequest.of(0, 20);
        Page<RequerimientoActividad> pagina = new PageImpl<>(
                List.of(requerimiento), pageable, 1);
        when(requerimientoActividadRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);
        when(requerimientoActividadRepository.findConDetalleByIds(List.of(ID_REQUERIMIENTO)))
                .thenReturn(List.of(requerimiento));
        when(requerimientoActCaractValorRepository.findByIdReqActCaractIn(any()))
                .thenReturn(List.of());

        PaginaResponse<RequerimientoActividadBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL, ID_PROYECTO,
                        new BuscarRequerimientosActividadFiltro(
                                null, null, null, null, null),
                        0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        RequerimientoActividadBusquedaResponse fila = respuesta.contenido().getFirst();
        assertThat(fila.idRequerimiento()).isEqualTo(ID_REQUERIMIENTO);
        assertThat(fila.idActividad()).isEqualTo(40L);
        assertThat(fila.nombreActividad()).isEqualTo("Actividad UC73");
        assertThat(fila.idProyecto()).isEqualTo(ID_PROYECTO);
        assertThat(fila.nombreProfesion()).isEqualTo("Fotógrafo");
    }

    private RequerimientoActividad requerimiento(Proyecto proyecto) {
        Actividad actividad = Actividad.builder()
                .idActividad(40L)
                .nombre("Actividad UC73")
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(Planificacion.builder()
                        .idPlanificacion(50L)
                        .proyecto(proyecto)
                        .build())
                .build();

        return RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO)
                .cantidad(2)
                .descripcion("Requerimiento UC73")
                .profesion(Profesion.builder().idProfesion(1L).nombre("Fotógrafo").build())
                .actividad(actividad)
                .build();
    }
}
