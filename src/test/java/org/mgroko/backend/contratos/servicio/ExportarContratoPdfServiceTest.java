package org.mgroko.backend.contratos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleRequest;
import org.mgroko.backend.auditoria.servicio.AuditoriaEventoService;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.ContratoVersion;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.RolParteContrato;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.ContratoVersionRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportarContratoPdfServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-18T17:06:28Z"), ZoneOffset.UTC);

    @Mock
    private ObtenerContratoService obtenerContratoService;
    @Mock
    private ContratoRepository contratoRepository;
    @Mock
    private FirmaContratoRepository firmaContratoRepository;
    @Mock
    private ContratoVersionRepository contratoVersionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditoriaEventoService auditoriaEventoService;

    private ExportarContratoPdfService service;

    @BeforeEach
    void setUp() {
        service = new ExportarContratoPdfService(
                obtenerContratoService,
                contratoRepository,
                firmaContratoRepository,
                contratoVersionRepository,
                usuarioRepository,
                auditoriaEventoService,
                RELOJ);

        when(contratoRepository.findByIdConPartes(5L))
                .thenReturn(Optional.of(contrato()));
        when(firmaContratoRepository.findConPartes(5L))
                .thenReturn(List.of(
                        firma(1L, "Luna", "Gómez", "30111222",
                                RolParteContrato.DIRECTOR,
                                EstadoFirmaContrato.FIRMADO),
                        firma(2L, "Tomás", "Díaz", "28999888",
                                RolParteContrato.DESTINATARIO,
                                EstadoFirmaContrato.PENDIENTE)));
        when(contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(5L, 2))
                .thenReturn(Optional.of(ContratoVersion.builder()
                        .version(2)
                        .texto("TEXTO DE LA VERSIÓN 2")
                        .build()));
        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(Usuario.builder()
                        .idUsuario(1L)
                        .nombre("Ana")
                        .apellido("Pérez")
                        .dni("27666555")
                        .build()));
    }

    @Test
    void exportarGeneraPdfYRegistraAuditoriaDeUsuario() {
        byte[] pdf = service.exportar(1L, 10L, 100L, 5L);

        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

        verify(obtenerContratoService).obtener(1L, 10L, 100L, 5L);

        ArgumentCaptor<List<AuditoriaDetalleRequest>> detalle =
                ArgumentCaptor.forClass(List.class);
        verify(auditoriaEventoService).registrar(
                eq("contrato"),
                eq(5L),
                eq("CONTRATO_EXPORTADO"),
                eq("UPDATE"),
                detalle.capture());

        assertThat(detalle.getValue())
                .contains(
                        new AuditoriaDetalleRequest("formato", null, "PDF"),
                        new AuditoriaDetalleRequest("idProyecto", null, "100"),
                        new AuditoriaDetalleRequest("version", null, "2"),
                        new AuditoriaDetalleRequest("origen", null, "USUARIO"));
    }

    @Test
    void exportarAdministrativoNoRevalidaVisibilidadDePerfil() {
        byte[] pdf = service.exportarAdministrativo(1L, 5L);

        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

        verify(obtenerContratoService, never())
                .obtener(eq(1L), eq(10L), eq(100L), eq(5L));

        ArgumentCaptor<List<AuditoriaDetalleRequest>> detalle =
                ArgumentCaptor.forClass(List.class);
        verify(auditoriaEventoService).registrar(
                eq("contrato"),
                eq(5L),
                eq("CONTRATO_EXPORTADO"),
                eq("UPDATE"),
                detalle.capture());
        assertThat(detalle.getValue())
                .contains(new AuditoriaDetalleRequest(
                        "origen", null, "ADMINISTRACION"));
    }

    private Contrato contrato() {
        Usuario usuarioEmisor = Usuario.builder()
                .idUsuario(1L)
                .nombre("Luna")
                .apellido("Gómez")
                .dni("30111222")
                .build();
        Perfil perfilEmisor = Perfil.builder()
                .idPerfil(1L)
                .nombreArtistico("luna")
                .usuario(usuarioEmisor)
                .build();
        MiembroProyecto emisor = MiembroProyecto.builder()
                .idMiembro(1L)
                .perfil(perfilEmisor)
                .build();

        return Contrato.builder()
                .idContrato(5L)
                .proyecto(Proyecto.builder()
                        .idProyecto(100L)
                        .nombre("Proyecto Luna")
                        .build())
                .tipoAcuerdo(TipoAcuerdoContrato.builder()
                        .nombre("Cesión de derechos")
                        .build())
                .miembroEmisor(emisor)
                .nombre("Cesión de derechos - Proyecto Luna")
                .texto("TEXTO BASE")
                .versionActual(2)
                .fechaCreacion(LocalDateTime.of(2026, 11, 10, 9, 15))
                .fechaEmision(LocalDateTime.of(2026, 11, 12, 14, 30))
                .estado(EstadoContrato.ACTIVO)
                .build();
    }

    private FirmaContrato firma(
            Long idMiembro,
            String nombre,
            String apellido,
            String dni,
            RolParteContrato rol,
            EstadoFirmaContrato estado) {

        Usuario usuario = Usuario.builder()
                .idUsuario(idMiembro + 10)
                .nombre(nombre)
                .apellido(apellido)
                .dni(dni)
                .build();
        Perfil perfil = Perfil.builder()
                .idPerfil(idMiembro + 10)
                .nombreArtistico(nombre.toLowerCase())
                .usuario(usuario)
                .build();
        MiembroProyecto miembro = MiembroProyecto.builder()
                .idMiembro(idMiembro)
                .perfil(perfil)
                .build();

        return FirmaContrato.builder()
                .idMiembro(idMiembro)
                .idContrato(5L)
                .miembro(miembro)
                .rolParte(rol)
                .estadoFirma(estado)
                .build();
    }
}
