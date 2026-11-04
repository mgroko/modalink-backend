package org.mgroko.backend.proyectos.servicio;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequerimientoGralProteccionServiceTest {

    @Mock
    private CupoRequerimientoService cupoRequerimientoService;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private PostulacionGralRepository postulacionGralRepository;

    @Mock
    private InvitacionGralRepository invitacionGralRepository;

    @InjectMocks
    private RequerimientoGralProteccionService proteccionService;

    private RequerimientoGralProyecto requerimiento;

    @BeforeEach
    void setUp() {
        requerimiento = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(10L)
                .cantidad(1)
                .proyecto(Proyecto.builder().idProyecto(100L).build())
                .build();
    }

    private CupoRequerimiento cupo(boolean satisfecho) {
        return satisfecho
                ? new CupoRequerimiento(1, 1, 0, true)
                : new CupoRequerimiento(1, 0, 1, false);
    }

    @Test
    void estaProtegido_cupoSatisfecho_esProtegido() {
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(cupo(true));

        assertThat(proteccionService.estaProtegido(
                requerimiento, EstadoProyecto.BORRADOR)).isTrue();
    }

    @Test
    void estaProtegido_sinVinculos_noEsProtegido() {
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(cupo(false));

        assertThat(proteccionService.estaProtegido(
                requerimiento, EstadoProyecto.BORRADOR)).isFalse();
    }

    @Test
    void estaProtegido_conMiembro_esProtegido() {
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(cupo(false));
        when(miembroProyectoRepository
                .existsByRequerimientoGral_IdRequerimientoGral(10L))
                .thenReturn(true);

        assertThat(proteccionService.estaProtegido(
                requerimiento, EstadoProyecto.PUBLICADO)).isTrue();
    }

    @Test
    void estaProtegido_conPostulacionAceptada_esProtegido() {
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(cupo(false));
        when(postulacionGralRepository
                .existsByRequerimientoGralIdRequerimientoGralAndEstado(
                        10L, EstadoSolicitud.ACEPTADA))
                .thenReturn(true);

        assertThat(proteccionService.estaProtegido(
                requerimiento, EstadoProyecto.PUBLICADO)).isTrue();
    }

    @Test
    void estaProtegido_conInvitacionAceptada_esProtegido() {
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(cupo(false));
        when(invitacionGralRepository
                .existsByRequerimientoGral_IdRequerimientoGralAndEstado(
                        10L, EstadoSolicitud.ACEPTADA))
                .thenReturn(true);

        assertThat(proteccionService.estaProtegido(
                requerimiento, EstadoProyecto.PUBLICADO)).isTrue();
    }

    @Test
    void estaProtegido_pendienteEnConfirmado_esProtegido() {
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(cupo(false));
        when(postulacionGralRepository
                .existsByRequerimientoGralIdRequerimientoGralAndEstado(
                        10L, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThat(proteccionService.estaProtegido(
                requerimiento, EstadoProyecto.CONFIRMADO)).isTrue();
    }

    @Test
    void estaProtegido_pendienteEnPublicado_noEsProtegido() {
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(cupo(false));
        when(postulacionGralRepository
                .existsByRequerimientoGralIdRequerimientoGralAndEstado(
                        10L, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThat(proteccionService.estaProtegido(
                requerimiento, EstadoProyecto.PUBLICADO)).isFalse();
    }

    @Test
    void estaProtegido_soloRechazadas_noEsProtegido() {
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(cupo(false));

        assertThat(proteccionService.estaProtegido(
                requerimiento, EstadoProyecto.CONFIRMADO)).isFalse();
    }

    @Test
    void calcularProtegidos_combinaCupoMiembrosYPendientes() {
        RequerimientoGralProyecto satisfecho = requerimiento(10L);
        RequerimientoGralProyecto conMiembro = requerimiento(20L);
        RequerimientoGralProyecto conPendiente = requerimiento(30L);
        RequerimientoGralProyecto libre = requerimiento(40L);
        List<RequerimientoGralProyecto> requerimientos =
                List.of(satisfecho, conMiembro, conPendiente, libre);

        when(cupoRequerimientoService.calcularGenerales(requerimientos))
                .thenReturn(Map.of(
                        10L, cupo(true),
                        20L, cupo(false),
                        30L, cupo(false),
                        40L, cupo(false)));
        when(miembroProyectoRepository.findIdsRequerimientoGralConMiembros(
                List.of(10L, 20L, 30L, 40L)))
                .thenReturn(List.of(20L));
        when(postulacionGralRepository.findIdsRequerimientoGralPorEstado(
                List.of(10L, 20L, 30L, 40L), EstadoSolicitud.PENDIENTE))
                .thenReturn(List.of(30L));

        Map<Long, Boolean> resultado = proteccionService.calcularProtegidos(
                requerimientos, EstadoProyecto.CONFIRMADO);

        assertThat(resultado)
                .containsEntry(10L, true)
                .containsEntry(20L, true)
                .containsEntry(30L, true)
                .containsEntry(40L, false);
    }

    private RequerimientoGralProyecto requerimiento(Long id) {
        return RequerimientoGralProyecto.builder()
                .idRequerimientoGral(id)
                .cantidad(1)
                .proyecto(Proyecto.builder().idProyecto(100L).build())
                .build();
    }
}
