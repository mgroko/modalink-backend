package org.mgroko.backend.contratos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.contratos.dto.ConflictoContratoResponse;
import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.GenerarContratoRequest;
import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.modelo.enums.RolParteContrato;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.mockito.Mockito;

import jakarta.persistence.EntityManager;

class GenerarContratoServiceTest {

    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 7, 12, 0);

    private ContratoContextoService contextoService;
    private GeneradorTextoContratoService generador;
    private ContratoRepository contratoRepository;
    private FirmaContratoRepository firmaContratoRepository;
    private EntityManager entityManager;
    private GenerarContratoService service;

    @BeforeEach
    void setUp() {
        contextoService = Mockito.mock(ContratoContextoService.class);
        generador = Mockito.mock(GeneradorTextoContratoService.class);
        contratoRepository = Mockito.mock(ContratoRepository.class);
        firmaContratoRepository = Mockito.mock(FirmaContratoRepository.class);
        entityManager = Mockito.mock(EntityManager.class);

        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-07T12:00:00Z"),
                ZoneOffset.UTC);

        service = new GenerarContratoService(
                contextoService,
                generador,
                contratoRepository,
                firmaContratoRepository,
                entityManager,
                reloj);
    }

    @Test
    void generar_caminoFeliz_persisteYDevuelveContratoPendienteFirma() {
        ContratoContextoService.ContextoContrato contexto = contexto(List.of());

        when(contextoService.cargar(anyLong(), anyLong(), anyLong(), anyLong(), anyList()))
                .thenReturn(contexto);
        when(generador.generar(eq(contexto), anyList())).thenReturn("TEXTO DEL CONTRATO");
        when(generador.generarNombre(contexto)).thenReturn("Cesion - Editorial");
        when(generador.generarDescripcion(contexto)).thenReturn("Contrato del proyecto");

        when(contratoRepository.saveAndFlush(any(Contrato.class)))
                .thenAnswer(invocation -> {
                    Contrato contrato = invocation.getArgument(0);
                    if (contrato.getIdContrato() == null) {
                        contrato.setIdContrato(99L);
                    }
                    return contrato;
                });

        when(firmaContratoRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Contrato actualizado = Contrato.builder()
                .idContrato(99L)
                .proyecto(contexto.proyecto())
                .tipoAcuerdo(contexto.tipoAcuerdo())
                .miembroEmisor(contexto.emisor())
                .nombre("Cesion - Editorial")
                .descripcion("Contrato del proyecto")
                .texto("TEXTO DEL CONTRATO")
                .versionActual(1)
                .fechaCreacion(AHORA)
                .fechaEmision(AHORA)
                .estado(EstadoContrato.PENDIENTE_FIRMA)
                .build();

        FirmaContrato firma = FirmaContrato.builder()
                .idMiembro(contexto.emisor().getIdMiembro())
                .idContrato(99L)
                .miembro(contexto.emisor())
                .rolParte(RolParteContrato.DIRECTOR)
                .estadoFirma(EstadoFirmaContrato.PENDIENTE)
                .build();

        when(contratoRepository.findByIdConPartes(99L))
                .thenReturn(Optional.of(actualizado));
        when(firmaContratoRepository.findConPartes(99L))
                .thenReturn(List.of(firma));

        GenerarContratoRequest request = new GenerarContratoRequest(
                1L,
                List.of(contexto.destinatarios().get(0).getIdMiembro()),
                List.of("Clausula adicional"));

        ContratoGeneradoResponse response = service.generar(
                7L, 3L, 10L, request);

        assertThat(response.idContrato()).isEqualTo(99L);
        assertThat(response.estado()).isEqualTo("PENDIENTE_FIRMA");
        assertThat(response.versionActual()).isEqualTo(1);
        assertThat(response.texto()).isEqualTo("TEXTO DEL CONTRATO");
        assertThat(response.partes()).hasSize(1);

        verify(contratoRepository, Mockito.times(2)).saveAndFlush(any(Contrato.class));
        verify(firmaContratoRepository).saveAll(anyList());
        verify(entityManager).flush();
        verify(entityManager).clear();
    }

    @Test
    void generar_conContratoActivoDuplicado_noPersisteNada() {
        ContratoContextoService.ContextoContrato contexto = contexto(List.of(
                new ConflictoContratoResponse(
                        "CONTRATO_DUPLICADO_ACTIVO",
                        8L,
                        "Beto Gómez",
                        "Beto Gómez ya tiene un contrato activo de este tipo.")));

        when(contextoService.cargar(anyLong(), anyLong(), anyLong(), anyLong(), anyList()))
                .thenReturn(contexto);

        GenerarContratoRequest request = new GenerarContratoRequest(
                1L,
                List.of(8L),
                List.of());

        assertThatThrownBy(() -> service.generar(7L, 3L, 10L, request))
                .isInstanceOf(ContratoNoGenerableException.class)
                .extracting("codigo")
                .isEqualTo("CONTRATO_DUPLICADO_ACTIVO");

        verifyNoInteractions(generador);
        verifyNoInteractions(contratoRepository);
        verifyNoInteractions(firmaContratoRepository);
    }

    private ContratoContextoService.ContextoContrato contexto(
            List<ConflictoContratoResponse> conflictos) {

        Profesion profesion = Profesion.builder()
                .idProfesion(1L)
                .nombre("Modelo")
                .build();

        Usuario usuarioDirector = Usuario.builder()
                .idUsuario(7L)
                .nombre("Ana")
                .apellido("Pérez")
                .dni("30111222")
                .build();
        Usuario usuarioParte = Usuario.builder()
                .idUsuario(8L)
                .nombre("Beto")
                .apellido("Gómez")
                .dni("33111222")
                .build();

        Perfil perfilDirector = Perfil.builder()
                .idPerfil(3L)
                .nombreArtistico("Ana")
                .usuario(usuarioDirector)
                .profesion(profesion)
                .build();
        Perfil perfilParte = Perfil.builder()
                .idPerfil(4L)
                .nombreArtistico("Beto")
                .usuario(usuarioParte)
                .profesion(profesion)
                .build();

        Proyecto proyecto = Proyecto.builder()
                .idProyecto(10L)
                .nombre("Editorial Primavera")
                .descripcion("Producción editorial")
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .build();

        TipoAcuerdoContrato tipo = TipoAcuerdoContrato.builder()
                .idTipoAcuerdo(1L)
                .codigo("CESION_DERECHOS_IMAGEN")
                .nombre("Cesión de derechos de imagen")
                .build();

        MiembroProyecto emisor = MiembroProyecto.builder()
                .idMiembro(11L)
                .perfil(perfilDirector)
                .proyecto(proyecto)
                .build();
        MiembroProyecto destinatario = MiembroProyecto.builder()
                .idMiembro(8L)
                .perfil(perfilParte)
                .proyecto(proyecto)
                .build();

        return new ContratoContextoService.ContextoContrato(
                perfilDirector,
                proyecto,
                tipo,
                emisor,
                List.of(destinatario),
                conflictos);
    }
}
