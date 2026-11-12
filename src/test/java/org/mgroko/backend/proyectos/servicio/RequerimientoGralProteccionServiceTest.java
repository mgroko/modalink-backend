package org.mgroko.backend.proyectos.servicio;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
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
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private PostulacionGralRepository postulacionGralRepository;

    @Mock
    private InvitacionGralRepository invitacionGralRepository;

    @Mock
    private ReemplazoRepository reemplazoRepository;

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

    @Test
    void sinVinculos_noEsProtegido() {
        assertThat(proteccionService.estaProtegido(requerimiento)).isFalse();
    }

    @Test
    void conMiembroActivo_esProtegido() {
        when(miembroProyectoRepository
                .existsByRequerimientoGral_IdRequerimientoGralAndEstadoParticipacion(
                        10L, EstadoParticipacion.ACTIVO))
                .thenReturn(true);

        assertThat(proteccionService.estaProtegido(requerimiento)).isTrue();
    }

    @Test
    void conMiembroHistorico_noEsProtegido() {
        assertThat(proteccionService.estaProtegido(requerimiento)).isFalse();
    }

    @Test
    void conPostulacionPendiente_esProtegido() {
        when(postulacionGralRepository
                .existsByRequerimientoGralIdRequerimientoGralAndEstado(
                        10L, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThat(proteccionService.estaProtegido(requerimiento)).isTrue();
    }

    @Test
    void conInvitacionPendiente_esProtegido() {
        when(invitacionGralRepository
                .existsByRequerimientoGral_IdRequerimientoGralAndEstado(
                        10L, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThat(proteccionService.estaProtegido(requerimiento)).isTrue();
    }

    @Test
    void conReemplazoAsociado_esProtegido() {
        when(reemplazoRepository
                .existsByRequerimientoGral_IdRequerimientoGral(10L))
                .thenReturn(true);

        assertThat(proteccionService.estaProtegido(requerimiento)).isTrue();
    }

    @Test
    void conAceptadaDeInactivo_noEsProtegido() {
        assertThat(proteccionService.estaProtegido(requerimiento)).isFalse();
    }

    @Test
    void calcularProtegidos_combinaMiembrosActivosYPendientes() {
        RequerimientoGralProyecto conMiembro = requerimiento(20L);
        RequerimientoGralProyecto conPendiente = requerimiento(30L);
        RequerimientoGralProyecto libre = requerimiento(40L);
        List<RequerimientoGralProyecto> requerimientos =
                List.of(conMiembro, conPendiente, libre);

        when(miembroProyectoRepository.findIdsRequerimientoGralConMiembrosActivos(
                List.of(20L, 30L, 40L)))
                .thenReturn(List.of(20L));
        when(postulacionGralRepository.findIdsRequerimientoGralPorEstado(
                List.of(20L, 30L, 40L), EstadoSolicitud.PENDIENTE))
                .thenReturn(List.of(30L));
        when(reemplazoRepository.findIdsRequerimientoGralConReemplazos(
                List.of(20L, 30L, 40L)))
                .thenReturn(List.of());

        Map<Long, Boolean> resultado = proteccionService.calcularProtegidos(
                requerimientos);

        assertThat(resultado)
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
