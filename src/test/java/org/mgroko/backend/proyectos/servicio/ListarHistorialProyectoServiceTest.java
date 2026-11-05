package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.proyectos.dto.ActividadHistorialResponse;
import org.mgroko.backend.proyectos.dto.AuditoriaBajaResponse;
import org.mgroko.backend.proyectos.dto.AutorBajaResponse;
import org.mgroko.backend.proyectos.dto.HistorialProyectoEventoResponse;
import org.mgroko.backend.proyectos.exception.HistorialParametroInvalidoException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListarHistorialProyectoServiceTest {

    @Mock
    private ListarAuditoriaBajasProyectoService auditoriaBajasService;

    private ListarHistorialProyectoService service;

    @BeforeEach
    void setUp() {
        service = new ListarHistorialProyectoService(auditoriaBajasService);
    }

    @Test
    void listar_mapeaEliminadoConActorYActividad() {
        when(auditoriaBajasService.listar(
                anyLong(), anyLong(), anyLong(), any(), any(), any(), any(),
                anyInt(), anyInt(), anyBoolean()))
                .thenReturn(pagina(List.of(
                        evento(
                                55L,
                                "ELIMINADO",
                                LocalDateTime.of(2026, 11, 4, 18, 30),
                                null,
                                "Luna",
                                "Luna",
                                "Pérez",
                                "Modelo",
                                "MIEMBRO",
                                10L,
                                new AutorBajaResponse(
                                        456L, 11L, "Dirección", "Ana", "Gómez"),
                                "Falta de compromiso",
                                List.of(new ActividadHistorialResponse(
                                        20L, "Montaje", "PENDIENTE", 30L, "Modelo"))))));

        PaginaResponse<HistorialProyectoEventoResponse> response =
                service.listar(1L, 10L, 100L, null, null, 0, 20);

        assertThat(response.contenido()).singleElement().satisfies(evento -> {
            assertThat(evento.idEvento()).isEqualTo(55L);
            assertThat(evento.tipo()).isEqualTo("MIEMBRO_ELIMINADO");
            assertThat(evento.fecha()).isEqualTo(
                    LocalDateTime.of(2026, 11, 4, 18, 30));
            assertThat(evento.idPerfilAfectado()).isEqualTo(123L);
            assertThat(evento.nombreArtisticoAfectado()).isEqualTo("Luna");
            assertThat(evento.nombreProfesionAfectado()).isEqualTo("Modelo");
            assertThat(evento.rolAfectado()).isEqualTo("MIEMBRO");
            assertThat(evento.idUsuarioAfectado()).isEqualTo(10L);
            assertThat(evento.idPerfilActor()).isEqualTo(456L);
            assertThat(evento.nombreArtisticoActor()).isEqualTo("Dirección");
            assertThat(evento.idUsuarioActor()).isEqualTo(11L);
            assertThat(evento.motivo()).isEqualTo("Falta de compromiso");
            assertThat(evento.estadoParticipacion()).isEqualTo("ELIMINADO");
            assertThat(evento.idActividad()).isEqualTo(20L);
            assertThat(evento.nombreActividad()).isEqualTo("Montaje");
        });
    }

    @Test
    void listar_mapeaBajaVoluntariaYUsaFechaAltaSiNoHayFechaBaja() {
        LocalDateTime fechaAlta = LocalDateTime.of(2026, 10, 1, 9, 0);
        when(auditoriaBajasService.listar(
                anyLong(), anyLong(), anyLong(), any(), any(), any(), any(),
                anyInt(), anyInt(), anyBoolean()))
                .thenReturn(pagina(List.of(
                        evento(
                                60L,
                                "BAJA_VOLUNTARIA",
                                null,
                                fechaAlta,
                                "Sol",
                                "Sol",
                                "Díaz",
                                "Fotógrafa",
                                "DIRECTOR",
                                12L,
                                null,
                                "Motivos personales",
                                List.of()))));

        PaginaResponse<HistorialProyectoEventoResponse> response =
                service.listar(1L, 10L, 100L, null, null, 0, 20);

        assertThat(response.contenido()).singleElement().satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo("MIEMBRO_BAJA_VOLUNTARIA");
            assertThat(evento.fecha()).isEqualTo(fechaAlta);
            assertThat(evento.estadoParticipacion()).isEqualTo("BAJA_VOLUNTARIA");
            assertThat(evento.idActividad()).isNull();
            assertThat(evento.nombreActividad()).isNull();
            assertThat(evento.idPerfilActor()).isNull();
        });
    }

    @Test
    void listar_filtraPorTipoYNombreAntesDePaginar() {
        when(auditoriaBajasService.listar(
                anyLong(), anyLong(), anyLong(), any(), any(), any(), any(),
                anyInt(), anyInt(), anyBoolean()))
                .thenReturn(pagina(List.of(
                        evento(1L, "ELIMINADO", LocalDateTime.of(2026, 11, 4, 18, 0),
                                null, "Luna", "Luna", "Pérez", "Modelo", "MIEMBRO",
                                10L, null, "Motivo", List.of()),
                        evento(2L, "ELIMINADO", LocalDateTime.of(2026, 11, 3, 18, 0),
                                null, "Otra", "Otra", "Persona", "Modelo", "MIEMBRO",
                                11L, null, "Motivo", List.of()))));

        PaginaResponse<HistorialProyectoEventoResponse> response =
                service.listar(1L, 10L, 100L, "MIEMBRO_ELIMINADO", "luna", 0, 20);

        assertThat(response.contenido()).singleElement()
                .extracting(HistorialProyectoEventoResponse::idEvento)
                .isEqualTo(1L);

        ArgumentCaptor<String> estado = ArgumentCaptor.forClass(String.class);
        verify(auditoriaBajasService).listar(
                eq(1L), eq(10L), eq(100L), estado.capture(), any(), any(), any(),
                eq(0), eq(20), eq(true));
        assertThat(estado.getValue()).isEqualTo("ELIMINADO");
    }

    @Test
    void listar_paginaYDevuelveMetadataCoherente() {
        when(auditoriaBajasService.listar(
                anyLong(), anyLong(), anyLong(), any(), any(), any(), any(),
                anyInt(), anyInt(), anyBoolean()))
                .thenReturn(pagina(List.of(
                        evento(1L, "ELIMINADO", LocalDateTime.of(2026, 11, 4, 18, 0),
                                null, "Uno", "Uno", "Uno", "Modelo", "MIEMBRO",
                                10L, null, "Motivo", List.of()),
                        evento(2L, "ELIMINADO", LocalDateTime.of(2026, 11, 3, 18, 0),
                                null, "Dos", "Dos", "Dos", "Modelo", "MIEMBRO",
                                11L, null, "Motivo", List.of()),
                        evento(3L, "ELIMINADO", LocalDateTime.of(2026, 11, 2, 18, 0),
                                null, "Tres", "Tres", "Tres", "Modelo", "MIEMBRO",
                                12L, null, "Motivo", List.of()))));

        PaginaResponse<HistorialProyectoEventoResponse> response =
                service.listar(1L, 10L, 100L, null, null, 1, 2);

        assertThat(response.paginaActual()).isEqualTo(1);
        assertThat(response.tamanoPagina()).isEqualTo(2);
        assertThat(response.totalElementos()).isEqualTo(3);
        assertThat(response.totalPaginas()).isEqualTo(2);
        assertThat(response.primera()).isFalse();
        assertThat(response.ultima()).isTrue();
        assertThat(response.contenido()).extracting(
                HistorialProyectoEventoResponse::idEvento).containsExactly(3L);
    }

    @Test
    void listar_paginaFueraDeRangoDevuelveVacioConMetadata() {
        when(auditoriaBajasService.listar(
                anyLong(), anyLong(), anyLong(), any(), any(), any(), any(),
                anyInt(), anyInt(), anyBoolean()))
                .thenReturn(pagina(List.of(
                        evento(1L, "ELIMINADO", LocalDateTime.of(2026, 11, 4, 18, 0),
                                null, "Uno", "Uno", "Uno", "Modelo", "MIEMBRO",
                                10L, null, "Motivo", List.of()))));

        PaginaResponse<HistorialProyectoEventoResponse> response =
                service.listar(1L, 10L, 100L, null, null, 5, 20);

        assertThat(response.contenido()).isEmpty();
        assertThat(response.totalElementos()).isEqualTo(1);
        assertThat(response.totalPaginas()).isEqualTo(1);
        assertThat(response.ultima()).isTrue();
    }

    @Test
    void listar_parametrosInvalidosLanza400() {
        assertThatThrownBy(() -> service.listar(1L, 10L, 100L, null, null, -1, 20))
                .isInstanceOf(HistorialParametroInvalidoException.class);

        assertThatThrownBy(() -> service.listar(1L, 10L, 100L, null, null, 0, 0))
                .isInstanceOf(HistorialParametroInvalidoException.class);

        assertThatThrownBy(() -> service.listar(1L, 10L, 100L, null, null, 0, 101))
                .isInstanceOf(HistorialParametroInvalidoException.class);

        assertThatThrownBy(() -> service.listar(
                1L, 10L, 100L, "MIEMBRO_AGREGADO", null, 0, 20))
                .isInstanceOf(HistorialParametroInvalidoException.class);

        verify(auditoriaBajasService, never()).listar(
                anyLong(), anyLong(), anyLong(), any(), any(), any(), any(),
                anyInt(), anyInt(), anyBoolean());
    }

    private PaginaResponse<AuditoriaBajaResponse> pagina(
            List<AuditoriaBajaResponse> contenido) {
        return new PaginaResponse<>(
                contenido, 0, 20, contenido.size(),
                contenido.isEmpty() ? 0 : 1, true, true);
    }

    private AuditoriaBajaResponse evento(
            Long idMiembro,
            String estado,
            LocalDateTime fechaBaja,
            LocalDateTime fechaAlta,
            String nombreArtistico,
            String nombre,
            String apellido,
            String nombreProfesion,
            String rol,
            Long idUsuario,
            AutorBajaResponse autor,
            String motivo,
            List<ActividadHistorialResponse> actividades) {

        return new AuditoriaBajaResponse(
                idMiembro,
                123L,
                idUsuario,
                nombreArtistico,
                nombre,
                apellido,
                nombreProfesion,
                rol,
                estado,
                motivo,
                fechaAlta,
                fechaBaja,
                autor,
                actividades);
    }
}
