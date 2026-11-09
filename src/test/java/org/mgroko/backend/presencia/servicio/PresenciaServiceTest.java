package org.mgroko.backend.presencia.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.presencia.config.PresenciaProperties;
import org.mgroko.backend.presencia.dto.PresenciaPerfilResponse;
import org.mgroko.backend.presencia.dto.PresenciaPerfilRow;
import org.mgroko.backend.presencia.evento.PresenciaActividadRegistrada;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresenciaServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL = 10L;
    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 9, 12, 0);

    @Mock
    private PerfilActivoService perfilActivoService;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PresenciaProperties properties;
    private PresenciaService service;

    @BeforeEach
    void setUp() {
        properties = new PresenciaProperties();
        properties.setUmbralEnLinea(Duration.ofSeconds(120));

        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-09T12:00:00Z"), ZoneOffset.UTC);
        service = new PresenciaService(
                perfilActivoService, perfilRepository, properties, eventPublisher, reloj);

        when(perfilActivoService.resolver(ID_USUARIO, ID_PERFIL))
                .thenReturn(perfil(EstadoPerfil.ACTIVO, EstadoUsuario.ACTIVO, AHORA));
    }

    @Test
    void registrarActividad_actualizaPerfilYPublicaEvento() {
        service.registrarActividad(ID_USUARIO, ID_PERFIL);

        verify(perfilRepository).actualizarUltimaActividad(
                eq(ID_PERFIL), eq(ID_USUARIO), eq(AHORA), eq(AHORA.minusSeconds(120)));

        ArgumentCaptor<PresenciaActividadRegistrada> captor =
                ArgumentCaptor.forClass(PresenciaActividadRegistrada.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().idPerfil()).isEqualTo(ID_PERFIL);
        assertThat(captor.getValue().ahora()).isEqualTo(AHORA);
    }

    @Test
    void estaEnLinea_perfilYUsuarioActivosConActividadReciente_esTrue() {
        Perfil perfil = perfil(EstadoPerfil.ACTIVO, EstadoUsuario.ACTIVO, AHORA.minusSeconds(60));

        assertThat(service.estaEnLinea(perfil, AHORA)).isTrue();
    }

    @Test
    void estaEnLinea_sinActividad_esFalse() {
        Perfil perfil = perfil(EstadoPerfil.ACTIVO, EstadoUsuario.ACTIVO, null);

        assertThat(service.estaEnLinea(perfil, AHORA)).isFalse();
    }

    @Test
    void estaEnLinea_fueraDelUmbral_esFalse() {
        Perfil perfil = perfil(
                EstadoPerfil.ACTIVO, EstadoUsuario.ACTIVO, AHORA.minusSeconds(121));

        assertThat(service.estaEnLinea(perfil, AHORA)).isFalse();
    }

    @Test
    void estaEnLinea_usuarioInactivo_esFalse() {
        Perfil perfil = perfil(
                EstadoPerfil.ACTIVO, EstadoUsuario.PendienteBaja, AHORA);

        assertThat(service.estaEnLinea(perfil, AHORA)).isFalse();
    }

    @Test
    void estaEnLinea_perfilInactivo_esFalse() {
        Perfil perfil = perfil(
                EstadoPerfil.PendienteBaja, EstadoUsuario.Activo, AHORA);

        assertThat(service.estaEnLinea(perfil, AHORA)).isFalse();
    }

    @Test
    void snapshot_preservaOrdenYDeducePerfilesInexistentes() {
        when(perfilRepository.buscarPresenciaPorIds(any()))
                .thenReturn(List.of(
                        new PresenciaPerfilRow(
                                10L, EstadoPerfil.ACTIVO, EstadoUsuario.ACTIVO, AHORA),
                        new PresenciaPerfilRow(
                                20L, EstadoPerfil.ACTIVO, EstadoUsuario.PendienteBaja, AHORA)));

        List<PresenciaPerfilResponse> respuesta = service.snapshot(
                List.of(10L, 20L, 30L));

        assertThat(respuesta).hasSize(3);
        assertThat(respuesta.get(0).idPerfil()).isEqualTo(10L);
        assertThat(respuesta.get(0).enLinea()).isTrue();
        assertThat(respuesta.get(1).enLinea()).isFalse();
        assertThat(respuesta.get(2).enLinea()).isFalse();
    }

    private Perfil perfil(
            EstadoPerfil estadoPerfil,
            EstadoUsuario estadoUsuario,
            LocalDateTime ultimaActividad) {

        Usuario usuario = Usuario.builder()
                .idUsuario(ID_USUARIO)
                .nombre("Ana")
                .estado(estadoUsuario)
                .build();

        return Perfil.builder()
                .idPerfil(ID_PERFIL)
                .nombreArtistico("Ana Moda")
                .biografia("Perfil")
                .estado(estadoPerfil)
                .usuario(usuario)
                .ultimaActividad(ultimaActividad)
                .build();
    }
}
