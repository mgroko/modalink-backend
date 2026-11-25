package org.mgroko.backend.proyectos.servicio;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.evento.CupoRequerimientoGralModificadoEvent;
import org.mgroko.backend.proyectos.exception.RequerimientoGralNoAsignableException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequerimientoGralAsignacionServiceTest {

    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private CupoRequerimientoService cupoRequerimientoService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RequerimientoGralAsignacionService service;

    private final Profesion modelo = Profesion.builder()
            .idProfesion(2L)
            .nombre("Modelo")
            .build();

    private final Proyecto proyecto = Proyecto.builder()
            .idProyecto(1L)
            .nombre("Proyecto")
            .build();

    private final Perfil perfil = Perfil.builder()
            .idPerfil(5L)
            .nombreArtistico("Modelo")
            .profesion(modelo)
            .build();

    @Test
    void resolver_priorizaElPrimerRequerimientoConCupo() {
        RequerimientoGralProyecto primero = requerimiento(10L, modelo);
        RequerimientoGralProyecto segundo = requerimiento(20L, modelo);

        when(requerimientoGralProyectoRepository.findByProyectoIdProyectoForUpdate(1L))
                .thenReturn(List.of(primero, segundo));
        when(cupoRequerimientoService.calcularGenerales(List.of(primero, segundo)))
                .thenReturn(Map.of(
                        10L, new CupoRequerimiento(1, 1, 0, true),
                        20L, new CupoRequerimiento(1, 0, 1, false)));

        RequerimientoGralProyecto resultado =
                service.resolverParaIncorporacion(proyecto, perfil, null);

        assertThat(resultado).isSameAs(segundo);
    }

    @Test
    void resolver_sinCandidatoConCupo_devuelveNull() {
        RequerimientoGralProyecto primero = requerimiento(10L, modelo);

        when(requerimientoGralProyectoRepository.findByProyectoIdProyectoForUpdate(1L))
                .thenReturn(List.of(primero));
        when(cupoRequerimientoService.calcularGenerales(List.of(primero)))
                .thenReturn(Map.of(10L, new CupoRequerimiento(1, 1, 0, true)));

        assertThat(service.resolverParaIncorporacion(proyecto, perfil, null)).isNull();
    }

    @Test
    void resolver_explicitoConProfesionDistinta_lanzaExcepcion() {
        Profesion otra = Profesion.builder().idProfesion(3L).nombre("Fotógrafo").build();
        RequerimientoGralProyecto requerimiento = requerimiento(10L, otra);

        when(requerimientoGralProyectoRepository.findByProyectoIdProyectoForUpdate(1L))
                .thenReturn(List.of(requerimiento));

        assertThatThrownBy(() -> service.resolverParaIncorporacion(
                proyecto, perfil, 10L))
                .isInstanceOf(RequerimientoGralNoAsignableException.class)
                .extracting("codigo")
                .isEqualTo("PROFESION_INCOMPATIBLE");
    }

    @Test
    void vincularSiCorresponde_director_noVincula() {
        MiembroProyecto director = miembro(
                RolProyecto.builder()
                        .codigo("DIRECTOR")
                        .nombre("Director")
                        .build());

        service.vincularSiCorresponde(director);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void vincularSiCorresponde_miembroSinRequerimiento_vinculaYPublicaEvento() {
        MiembroProyecto miembro = miembro(
                RolProyecto.builder()
                        .codigo("MIEMBRO")
                        .nombre("Miembro")
                        .build());
        RequerimientoGralProyecto requerimiento = requerimiento(10L, modelo);

        when(requerimientoGralProyectoRepository.findByProyectoIdProyectoForUpdate(1L))
                .thenReturn(List.of(requerimiento));
        when(cupoRequerimientoService.calcularGenerales(List.of(requerimiento)))
                .thenReturn(Map.of(10L, new CupoRequerimiento(1, 0, 1, false)));
        when(miembroProyectoRepository.saveAndFlush(miembro)).thenReturn(miembro);

        service.vincularSiCorresponde(miembro);

        assertThat(miembro.getRequerimientoGral()).isSameAs(requerimiento);
        verify(miembroProyectoRepository).saveAndFlush(miembro);

        ArgumentCaptor<CupoRequerimientoGralModificadoEvent> captor =
                ArgumentCaptor.forClass(CupoRequerimientoGralModificadoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().idRequerimientoGral()).isEqualTo(10L);
    }

    private RequerimientoGralProyecto requerimiento(Long id, Profesion profesion) {
        return RequerimientoGralProyecto.builder()
                .idRequerimientoGral(id)
                .proyecto(proyecto)
                .profesion(profesion)
                .cantidad(1)
                .build();
    }

    private MiembroProyecto miembro(RolProyecto rol) {
        return MiembroProyecto.builder()
                .idMiembro(50L)
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rol)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
    }
}
