package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequerimientoSnapshotServiceTest {

    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    @Mock
    private RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;
    @Mock
    private RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;
    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;
    @Mock
    private PostulacionGralRepository postulacionGralRepository;
    @Mock
    private InvitacionGralRepository invitacionGralRepository;

    @Test
    void asegurarHistorialActividad_soloRepuntaAsignacionesDeMiembrosInactivos() {
        RequerimientoSnapshotService service = new RequerimientoSnapshotService(
                requerimientoActividadRepository,
                requerimientoGralProyectoRepository,
                requerimientoActCaractValorRepository,
                requerimientoGralCaractValorRepository,
                asignacionActividadRepository,
                miembroProyectoRepository,
                postulacionActividadRepository,
                invitacionActividadRepository,
                postulacionGralRepository,
                invitacionGralRepository);

        RequerimientoActividad activo = requerimiento();
        AsignacionActividad activa = asignacion(1L, EstadoParticipacion.ACTIVO, activo);
        AsignacionActividad inactiva = asignacion(2L, EstadoParticipacion.BajaVoluntaria, activo);

        when(asignacionActividadRepository
                .existsByRequerimientoActividad_IdRequerimiento(30L))
                .thenReturn(true);
        when(postulacionActividadRepository
                .existsByRequerimiento_IdRequerimiento(30L))
                .thenReturn(false);
        when(invitacionActividadRepository
                .existsByRequerimiento_IdRequerimiento(30L))
                .thenReturn(false);
        when(asignacionActividadRepository
                .findByRequerimientoActividad_IdRequerimiento(30L))
                .thenReturn(List.of(activa, inactiva));
        when(requerimientoActividadRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> {
                    RequerimientoActividad snapshot = invocation.getArgument(0);
                    snapshot.setIdRequerimiento(99L);
                    return snapshot;
                });
        when(postulacionActividadRepository
                .findHistoricasPorRequerimiento(30L))
                .thenReturn(List.of());
        when(invitacionActividadRepository
                .findHistoricasPorRequerimiento(30L))
                .thenReturn(List.of());

        service.asegurarHistorialActividad(activo, LocalDateTime.now());

        verify(asignacionActividadRepository).saveAll(argThat(asignaciones -> {
            List<AsignacionActividad> lista = new ArrayList<>();
            asignaciones.forEach(lista::add);
            return lista.size() == 1 && lista.get(0) == inactiva;
        }));
        assertThat(activa.getRequerimientoActividad()).isSameAs(activo);
        assertThat(inactiva.getRequerimientoActividad().getFechaArchivado()).isNotNull();
    }

    private RequerimientoActividad requerimiento() {
        Actividad actividad = Actividad.builder()
                .idActividad(10L)
                .nombre("Montaje")
                .estado(EstadoActividad.Pendiente)
                .build();
        return RequerimientoActividad.builder()
                .idRequerimiento(30L)
                .cantidad(1)
                .actividad(actividad)
                .profesion(Profesion.builder().idProfesion(2L).nombre("Fotógrafo").build())
                .caracteristicas(new ArrayList<>())
                .habilidades(new HashSet<>())
                .build();
    }

    private AsignacionActividad asignacion(
            Long idMiembro,
            EstadoParticipacion estado,
            RequerimientoActividad requerimiento) {

        MiembroProyecto miembro = MiembroProyecto.builder()
                .idMiembro(idMiembro)
                .perfil(Perfil.builder().idPerfil(idMiembro).build())
                .estadoParticipacion(estado)
                .build();
        return AsignacionActividad.builder()
                .idAsignacionAct(idMiembro)
                .miembro(miembro)
                .actividad(requerimiento.getActividad())
                .requerimientoActividad(requerimiento)
                .build();
    }
}
