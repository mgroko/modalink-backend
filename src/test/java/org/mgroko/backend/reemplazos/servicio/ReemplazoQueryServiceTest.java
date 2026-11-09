package org.mgroko.backend.reemplazos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.reemplazos.dto.InvitacionReemplazoResponse;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;

@ExtendWith(MockitoExtension.class)
class ReemplazoQueryServiceTest {

    private static final Long ID_USUARIO = 10L;
    private static final Long ID_PERFIL = 20L;

    @Mock
    private ReemplazoRepository reemplazoRepository;
    @Mock
    private ReemplazoCandidatoRepository candidatoRepository;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @InjectMocks
    private ReemplazoQueryService servicio;

    @Test
    void listarInvitacionesRechazadas_soloConsultaRechazadoYExponeFechaYMotivo() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil()));

        LocalDateTime fechaRespuesta = LocalDateTime.of(2026, 11, 6, 18, 30);
        ReemplazoCandidato candidato = candidato(fechaRespuesta);

        when(candidatoRepository.buscarInvitacionesDePerfil(
                ID_PERFIL, List.of(EstadoCandidatoReemplazo.RECHAZADO)))
                .thenReturn(List.of(candidato));

        List<InvitacionReemplazoResponse> resultado =
                servicio.listarInvitacionesRechazadas(ID_USUARIO, ID_PERFIL);

        assertThat(resultado).hasSize(1);
        InvitacionReemplazoResponse invitacion = resultado.getFirst();
        assertThat(invitacion.estado())
                .isEqualTo(EstadoCandidatoReemplazo.RECHAZADO);
        assertThat(invitacion.fechaRespuesta()).isEqualTo(fechaRespuesta);
        assertThat(invitacion.motivoRespuesta()).isEqualTo("Sin disponibilidad");
        assertThat(invitacion.nombreProyecto()).isEqualTo("Proyecto test");

        verify(candidatoRepository).buscarInvitacionesDePerfil(
                ID_PERFIL, List.of(EstadoCandidatoReemplazo.RECHAZADO));
    }

    @Test
    void listarInvitaciones_porDefectoMantieneSoloEstadosActivos() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil()));
        when(candidatoRepository.buscarInvitacionesDePerfil(
                ID_PERFIL,
                List.of(EstadoCandidatoReemplazo.INVITADO,
                        EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION)))
                .thenReturn(List.of());

        assertThat(servicio.listarInvitaciones(ID_USUARIO, ID_PERFIL)).isEmpty();

        verify(candidatoRepository).buscarInvitacionesDePerfil(
                ID_PERFIL,
                List.of(EstadoCandidatoReemplazo.INVITADO,
                        EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION));
    }

    private Perfil perfil() {
        return Perfil.builder()
                .idPerfil(ID_PERFIL)
                .estado(EstadoPerfil.ACTIVO)
                .build();
    }

    private ReemplazoCandidato candidato(LocalDateTime fechaRespuesta) {
        Profesion profesion = Profesion.builder().nombre("Disenador").build();
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(30L)
                .nombre("Proyecto test")
                .build();
        RequerimientoGralProyecto requerimiento = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(40L)
                .proyecto(proyecto)
                .profesion(profesion)
                .build();
        Reemplazo reemplazo = Reemplazo.builder()
                .idReemplazo(50L)
                .tipo(TipoReemplazo.GENERAL)
                .proyecto(proyecto)
                .requerimientoGral(requerimiento)
                .build();

        return ReemplazoCandidato.builder()
                .idCandidato(60L)
                .reemplazo(reemplazo)
                .estado(EstadoCandidatoReemplazo.RECHAZADO)
                .fechaInvitacion(LocalDateTime.of(2026, 11, 5, 9, 0))
                .fechaRespuesta(fechaRespuesta)
                .motivoRespuesta("Sin disponibilidad")
                .build();
    }
}
