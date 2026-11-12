package org.mgroko.backend.perfiles.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.dto.HistorialProyectoPerfilResponse;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListarHistorialProyectosPerfilServiceTest {

    @Mock
    private VerPerfilService verPerfilService;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    @Test
    void devuelveProyectosFinalizadosYCanceladosConRolDelRequerimiento() {
        Perfil perfil = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Modelo")
                .profesion(Profesion.builder().codigo("MODELO").nombre("Modelo").build())
                .build();

        Profesion profesionRequerimiento = Profesion.builder()
                .codigo("MODELO")
                .nombre("Modelo")
                .build();

        Proyecto finalizado = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Editorial Otoño")
                .estado(EstadoProyecto.FINALIZADO)
                .fechaFinalizacion(LocalDateTime.of(2026, 3, 15, 18, 0))
                .build();

        Proyecto cancelado = Proyecto.builder()
                .idProyecto(26L)
                .nombre("Campaña Cancelada")
                .estado(EstadoProyecto.CANCELADO)
                .fechaCancelacion(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();

        RolProyecto rolMiembro = RolProyecto.builder().codigo("MIEMBRO").nombre("Miembro de proyecto").build();

        MiembroProyecto miembroFinalizado = MiembroProyecto.builder()
                .proyecto(finalizado)
                .perfil(perfil)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .requerimientoGral(RequerimientoGralProyecto.builder()
                        .profesion(profesionRequerimiento)
                        .build())
                .build();

        MiembroProyecto miembroCancelado = MiembroProyecto.builder()
                .proyecto(cancelado)
                .perfil(perfil)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .requerimientoGral(RequerimientoGralProyecto.builder()
                        .profesion(profesionRequerimiento)
                        .build())
                .build();

        when(verPerfilService.obtenerPerfilVisible(7L, 1L))
                .thenReturn(new VerPerfilService.PerfilVisible(perfil, false));
        when(miembroProyectoRepository.buscarHistorialProyectosDePerfil(
                eq(7L), anyCollection(), eq(EstadoParticipacion.ACTIVO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(miembroFinalizado, miembroCancelado),
                        PageRequest.of(0, 10),
                        2));
        when(proyectoVisibilidadService.puedeVer(any(Proyecto.class), anyLong()))
                .thenReturn(true);

        ListarHistorialProyectosPerfilService servicio = new ListarHistorialProyectosPerfilService(
                verPerfilService, miembroProyectoRepository, proyectoVisibilidadService);

        PaginaResponse<HistorialProyectoPerfilResponse> respuesta =
                servicio.listar(7L, 1L, 3L, 0, 10);

        assertEquals(2, respuesta.contenido().size());
        assertEquals("Editorial Otoño", respuesta.contenido().get(0).nombre());
        assertEquals("Finalizado", respuesta.contenido().get(0).estado());
        assertEquals("Modelo", respuesta.contenido().get(0).rol());
        assertEquals("MODELO", respuesta.contenido().get(0).rolCodigo());
        assertEquals(LocalDateTime.of(2026, 4, 1, 10, 0),
                respuesta.contenido().get(1).fechaCancelacion());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<EstadoProyecto>> estados =
                ArgumentCaptor.forClass(Collection.class);
        verify(miembroProyectoRepository).buscarHistorialProyectosDePerfil(
                eq(7L), estados.capture(), eq(EstadoParticipacion.ACTIVO), any(Pageable.class));
        assertTrue(estados.getValue().contains(EstadoProyecto.FINALIZADO));
        assertTrue(estados.getValue().contains(EstadoProyecto.CANCELADO));
    }
}
