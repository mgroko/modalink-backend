package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.JornadaAgenda;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.actividades.servicio.CambiarEstadoActividadService;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.AceptacionTerminosRequest;
import org.mgroko.backend.reemplazos.dto.AprobarCandidatoRequest;
import org.mgroko.backend.reemplazos.dto.ConfirmarReemplazoRequest;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.enums.NivelUbicacion;
import org.mgroko.backend.reemplazos.enums.OrigenReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoDisponibilidad;
import org.mgroko.backend.reemplazos.evento.ReemplazoNecesarioEvent;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.mgroko.backend.reemplazos.servicio.AprobacionIncorporacionService;
import org.mgroko.backend.reemplazos.servicio.ConfirmarReemplazoService;
import org.mgroko.backend.reemplazos.servicio.GenerarReemplazosService;
import org.mgroko.backend.reemplazos.servicio.InvitacionReemplazoService;
import org.mgroko.backend.reemplazos.servicio.ReemplazoQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class GenerarReemplazosIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GenerarReemplazosService generarReemplazosService;

    @Autowired
    private ReemplazoRepository reemplazoRepository;

    @Autowired
    private ReemplazoCandidatoRepository reemplazoCandidatoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private JornadaAgendaRepository jornadaAgendaRepository;

    @Autowired
    private ConfirmarReemplazoService confirmarReemplazoService;

    @Autowired
    private InvitacionReemplazoService invitacionReemplazoService;

    @Autowired
    private AprobacionIncorporacionService aprobacionIncorporacionService;

    @Autowired
    private CambiarEstadoActividadService cambiarEstadoActividadService;

    @Autowired
    private ReemplazoQueryService reemplazoQueryService;

    @Autowired
    private AsignacionActividadRepository asignacionActividadRepository;

    @Autowired
    private MiembroProyectoRepository miembroProyectoRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void generaProcesosParaActividadYRequerimientoGeneral() {
        Escenario escenario = crearEscenario();

        generarReemplazosService.generar(new ReemplazoNecesarioEvent(
                escenario.idProyecto(),
                escenario.idMiembroBaja(),
                escenario.idPerfilBaja(),
                OrigenReemplazo.BAJA_VOLUNTARIA));

        var reemplazos = reemplazoRepository
                .findByProyectoIdProyectoOrderByFechaCreacionDesc(escenario.idProyecto());

        assertThat(reemplazos).hasSize(2);
        assertThat(reemplazos)
                .extracting(r -> r.getTipo().name())
                .containsExactlyInAnyOrder("ACTIVIDAD", "GENERAL");

        for (var reemplazo : reemplazos) {
            var candidatos = reemplazoCandidatoRepository
                    .findConDetalleByReemplazo(reemplazo.getIdReemplazo());
            assertThat(candidatos)
                    .as("candidatos de %s", reemplazo.getTipo())
                    .hasSize(1);
            assertThat(candidatos).allMatch(c -> c.getPerfil().getProfesion()
                    .getIdProfesion().equals(escenario.idProfesion()));
            assertThat(candidatos)
                    .extracting(c -> c.getPerfil().getIdPerfil())
                    .containsExactly(escenario.idPerfilCandidato());
        }
    }

    @Test
    @Transactional
    void permiteInvitacionesDelMismoProyectoParaRequerimientosDistintos() {
        Escenario escenario = crearEscenario();

        generarReemplazosService.generar(new ReemplazoNecesarioEvent(
                escenario.idProyecto(),
                escenario.idMiembroBaja(),
                escenario.idPerfilBaja(),
                OrigenReemplazo.BAJA_VOLUNTARIA));

        var reemplazos = reemplazoRepository
                .findByProyectoIdProyectoOrderByFechaCreacionDesc(escenario.idProyecto());
        var reemplazoActividad = reemplazos.stream()
                .filter(r -> r.getTipo().name().equals("ACTIVIDAD"))
                .findFirst()
                .orElseThrow();
        var reemplazoGeneral = reemplazos.stream()
                .filter(r -> r.getTipo().name().equals("GENERAL"))
                .findFirst()
                .orElseThrow();

        var candidatoActividad = reemplazoCandidatoRepository
                .findConDetalleByReemplazo(reemplazoActividad.getIdReemplazo())
                .getFirst();
        var candidatoGeneral = reemplazoCandidatoRepository
                .findConDetalleByReemplazo(reemplazoGeneral.getIdReemplazo())
                .getFirst();

        confirmarReemplazoService.confirmar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazoActividad.getIdReemplazo(),
                new ConfirmarReemplazoRequest(
                        List.of(candidatoActividad.getIdCandidato()),
                        "Invitación a actividad"));

        confirmarReemplazoService.confirmar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazoGeneral.getIdReemplazo(),
                new ConfirmarReemplazoRequest(
                        List.of(candidatoGeneral.getIdCandidato()),
                        "Invitación general"));

        assertThat(reemplazoRepository.findById(reemplazoActividad.getIdReemplazo())
                .orElseThrow()
                .getEstado().name())
                .isEqualTo("INVITACIONES_ENVIADAS");
        assertThat(reemplazoRepository.findById(reemplazoGeneral.getIdReemplazo())
                .orElseThrow()
                .getEstado().name())
                .isEqualTo("INVITACIONES_ENVIADAS");
        assertThat(reemplazoCandidatoRepository.findById(candidatoActividad.getIdCandidato())
                .orElseThrow()
                .getEstado().name())
                .isEqualTo("INVITADO");
        assertThat(reemplazoCandidatoRepository.findById(candidatoGeneral.getIdCandidato())
                .orElseThrow()
                .getEstado().name())
                .isEqualTo("INVITADO");
    }

    @Test
    @Transactional
    void alCancelarActividadCancelaLogicamenteElReemplazoYLaInvitacion() {
        Escenario escenario = crearEscenario();

        generarReemplazosService.generar(new ReemplazoNecesarioEvent(
                escenario.idProyecto(),
                escenario.idMiembroBaja(),
                escenario.idPerfilBaja(),
                OrigenReemplazo.BAJA_VOLUNTARIA));

        var reemplazoActividad = reemplazoRepository
                .findByProyectoIdProyectoOrderByFechaCreacionDesc(escenario.idProyecto())
                .stream()
                .filter(r -> r.getTipo().name().equals("ACTIVIDAD"))
                .findFirst()
                .orElseThrow();

        var candidato = reemplazoCandidatoRepository
                .findConDetalleByReemplazo(reemplazoActividad.getIdReemplazo())
                .getFirst();

        confirmarReemplazoService.confirmar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazoActividad.getIdReemplazo(),
                new ConfirmarReemplazoRequest(
                        List.of(candidato.getIdCandidato()),
                        "Invitación a actividad"));

        assertThat(reemplazoCandidatoRepository.findById(candidato.getIdCandidato())
                .orElseThrow()
                .getEstado().name())
                .isEqualTo("INVITADO");

        cambiarEstadoActividadService.cambiar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                escenario.idActividad(),
                EstadoActividad.CANCELADA);

        var reemplazoCancelado = reemplazoRepository
                .findById(reemplazoActividad.getIdReemplazo())
                .orElseThrow();
        assertThat(reemplazoCancelado.getEstado().name()).isEqualTo("CANCELADO");
        assertThat(reemplazoCancelado.getMotivoCierre()).isEqualTo("Actividad cancelada");
        assertThat(reemplazoCancelado.getFechaCierre()).isNotNull();

        var candidatoCancelado = reemplazoCandidatoRepository
                .findById(candidato.getIdCandidato())
                .orElseThrow();
        assertThat(candidatoCancelado.getEstado().name()).isEqualTo("CANCELADO");
        assertThat(candidatoCancelado.getMotivoDecision()).isEqualTo("Actividad cancelada");

        assertThat(reemplazoQueryService.listarInvitaciones(
                escenario.idUsuarioCandidato(),
                escenario.idPerfilCandidato())).isEmpty();
    }

    @Test
    @Transactional
    void flujoConfirmarAceptarYAprobarIncorporaAlCandidato() {
        Escenario escenario = crearEscenario();

        generarReemplazosService.generar(new ReemplazoNecesarioEvent(
                escenario.idProyecto(),
                escenario.idMiembroBaja(),
                escenario.idPerfilBaja(),
                OrigenReemplazo.BAJA_VOLUNTARIA));

        var reemplazoActividad = reemplazoRepository
                .findByProyectoIdProyectoOrderByFechaCreacionDesc(escenario.idProyecto())
                .stream()
                .filter(r -> r.getTipo().name().equals("ACTIVIDAD"))
                .findFirst()
                .orElseThrow();

        var candidato = reemplazoCandidatoRepository
                .findConDetalleByReemplazo(reemplazoActividad.getIdReemplazo())
                .getFirst();

        confirmarReemplazoService.confirmar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazoActividad.getIdReemplazo(),
                new ConfirmarReemplazoRequest(List.of(candidato.getIdCandidato()), "Te invitamos"));

        invitacionReemplazoService.aceptar(
                escenario.idUsuarioCandidato(),
                escenario.idPerfilCandidato(),
                candidato.getIdCandidato());

        aprobacionIncorporacionService.aprobar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazoActividad.getIdReemplazo(),
                candidato.getIdCandidato());

        var reemplazoCubierto = reemplazoRepository
                .findById(reemplazoActividad.getIdReemplazo())
                .orElseThrow();
        assertThat(reemplazoCubierto.getEstado().name()).isEqualTo("CUBIERTO");
        assertThat(reemplazoCubierto.getIdAsignacionResultante()).isNotNull();

        assertThat(miembroProyectoRepository.findMiembroActivoConRol(
                escenario.idProyecto(), escenario.idPerfilCandidato())).isPresent();
        assertThat(asignacionActividadRepository
                .existsByMiembro_IdMiembroAndActividad_IdActividad(
                        reemplazoCubierto.getIdMiembroResultante(),
                        escenario.idActividad())).isTrue();
    }

    @Test
    @Transactional
    void permiteVariasAceptacionesYElDirectorEligeCualIncorporar() {
        Escenario escenario = crearEscenario();

        generarReemplazosService.generar(new ReemplazoNecesarioEvent(
                escenario.idProyecto(),
                escenario.idMiembroBaja(),
                escenario.idPerfilBaja(),
                OrigenReemplazo.BAJA_VOLUNTARIA));

        Reemplazo reemplazo = reemplazoActividad(escenario);
        ReemplazoCandidato primero = candidatosDe(reemplazo).getFirst();
        ReemplazoCandidato segundo = agregarCandidatoAdicional(reemplazo, escenario);

        confirmarReemplazoService.confirmar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazo.getIdReemplazo(),
                new ConfirmarReemplazoRequest(
                        List.of(primero.getIdCandidato(), segundo.getIdCandidato()),
                        "Invitación masiva"));

        invitacionReemplazoService.aceptar(
                escenario.idUsuarioCandidato(),
                escenario.idPerfilCandidato(),
                primero.getIdCandidato());
        invitacionReemplazoService.aceptar(
                segundo.getUsuario().getIdUsuario(),
                segundo.getPerfil().getIdPerfil(),
                segundo.getIdCandidato());

        assertThat(candidato(primero.getIdCandidato()).getEstado())
                .isEqualTo(EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);
        assertThat(candidato(segundo.getIdCandidato()).getEstado())
                .isEqualTo(EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);
        assertThat(reemplazoRepository.findById(reemplazo.getIdReemplazo())
                .orElseThrow()
                .getEstado())
                .isEqualTo(EstadoReemplazo.PENDIENTE_APROBACION);

        aprobacionIncorporacionService.aprobar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazo.getIdReemplazo(),
                segundo.getIdCandidato());

        assertThat(candidato(segundo.getIdCandidato()).getEstado())
                .isEqualTo(EstadoCandidatoReemplazo.APROBADO);

        ReemplazoCandidato cancelado = candidato(primero.getIdCandidato());
        assertThat(cancelado.getEstado())
                .isEqualTo(EstadoCandidatoReemplazo.CANCELADO);
        assertThat(cancelado.getFechaDecision()).isNotNull();
        assertThat(cancelado.getMotivoDecision())
                .contains("Vacante cubierta por otro candidato");

        assertThat(reemplazoRepository.findById(reemplazo.getIdReemplazo())
                .orElseThrow()
                .getEstado())
                .isEqualTo(EstadoReemplazo.CUBIERTO);

        assertThat(miembroProyectoRepository.findMiembroActivoConRol(
                escenario.idProyecto(),
                segundo.getPerfil().getIdPerfil())).isPresent();
        assertThat(miembroProyectoRepository.findMiembroActivoConRol(
                escenario.idProyecto(),
                escenario.idPerfilCandidato())).isEmpty();

        assertThat(reemplazoQueryService.listarInvitaciones(
                escenario.idUsuarioCandidato(),
                escenario.idPerfilCandidato())).isEmpty();
    }

    @Test
    @Transactional
    void rechazarUnaAceptacionMantieneElProcesoPendienteSiQuedanOtrosAceptados() {
        Escenario escenario = crearEscenario();

        generarReemplazosService.generar(new ReemplazoNecesarioEvent(
                escenario.idProyecto(),
                escenario.idMiembroBaja(),
                escenario.idPerfilBaja(),
                OrigenReemplazo.BAJA_VOLUNTARIA));

        Reemplazo reemplazo = reemplazoActividad(escenario);
        ReemplazoCandidato primero = candidatosDe(reemplazo).getFirst();
        ReemplazoCandidato segundo = agregarCandidatoAdicional(reemplazo, escenario);

        confirmarReemplazoService.confirmar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazo.getIdReemplazo(),
                new ConfirmarReemplazoRequest(
                        List.of(primero.getIdCandidato(), segundo.getIdCandidato()),
                        "Invitación masiva"));

        invitacionReemplazoService.aceptar(
                escenario.idUsuarioCandidato(),
                escenario.idPerfilCandidato(),
                primero.getIdCandidato());
        invitacionReemplazoService.aceptar(
                segundo.getUsuario().getIdUsuario(),
                segundo.getPerfil().getIdPerfil(),
                segundo.getIdCandidato());

        aprobacionIncorporacionService.rechazar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazo.getIdReemplazo(),
                primero.getIdCandidato(),
                "No disponible para la actividad");

        assertThat(candidato(primero.getIdCandidato()).getEstado())
                .isEqualTo(EstadoCandidatoReemplazo.RECHAZADO_ADMIN);
        assertThat(candidato(segundo.getIdCandidato()).getEstado())
                .isEqualTo(EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);
        assertThat(reemplazoRepository.findById(reemplazo.getIdReemplazo())
                .orElseThrow()
                .getEstado())
                .isEqualTo(EstadoReemplazo.PENDIENTE_APROBACION);

        aprobacionIncorporacionService.aprobar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazo.getIdReemplazo(),
                segundo.getIdCandidato());

        assertThat(candidato(segundo.getIdCandidato()).getEstado())
                .isEqualTo(EstadoCandidatoReemplazo.APROBADO);
        assertThat(reemplazoRepository.findById(reemplazo.getIdReemplazo())
                .orElseThrow()
                .getEstado())
                .isEqualTo(EstadoReemplazo.CUBIERTO);
    }

    @Test
    @Transactional
    void rechazoDelProfesionalNoCierraElProcesoSiOtroCandidatoYaAcepto() {
        Escenario escenario = crearEscenario();

        generarReemplazosService.generar(new ReemplazoNecesarioEvent(
                escenario.idProyecto(),
                escenario.idMiembroBaja(),
                escenario.idPerfilBaja(),
                OrigenReemplazo.BAJA_VOLUNTARIA));

        Reemplazo reemplazo = reemplazoActividad(escenario);
        ReemplazoCandidato primero = candidatosDe(reemplazo).getFirst();
        ReemplazoCandidato segundo = agregarCandidatoAdicional(reemplazo, escenario);

        confirmarReemplazoService.confirmar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazo.getIdReemplazo(),
                new ConfirmarReemplazoRequest(
                        List.of(primero.getIdCandidato(), segundo.getIdCandidato()),
                        "Invitación masiva"));

        invitacionReemplazoService.aceptar(
                escenario.idUsuarioCandidato(),
                escenario.idPerfilCandidato(),
                primero.getIdCandidato());

        invitacionReemplazoService.rechazar(
                segundo.getUsuario().getIdUsuario(),
                segundo.getPerfil().getIdPerfil(),
                segundo.getIdCandidato(),
                "No puedo cubrir la vacante");

        assertThat(candidato(primero.getIdCandidato()).getEstado())
                .isEqualTo(EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);
        assertThat(candidato(segundo.getIdCandidato()).getEstado())
                .isEqualTo(EstadoCandidatoReemplazo.RECHAZADO);
        assertThat(reemplazoRepository.findById(reemplazo.getIdReemplazo())
                .orElseThrow()
                .getEstado())
                .isEqualTo(EstadoReemplazo.PENDIENTE_APROBACION);

        aprobacionIncorporacionService.aprobar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazo.getIdReemplazo(),
                primero.getIdCandidato());

        assertThat(candidato(primero.getIdCandidato()).getEstado())
                .isEqualTo(EstadoCandidatoReemplazo.APROBADO);
        assertThat(reemplazoRepository.findById(reemplazo.getIdReemplazo())
                .orElseThrow()
                .getEstado())
                .isEqualTo(EstadoReemplazo.CUBIERTO);
    }

    @Test
    @Transactional
    void aprobarCandidatoConTycVigenteRegistraLaVersionAceptada() {
        Escenario escenario = crearEscenario();

        Long idTyc = jdbcTemplate.queryForObject("""
                INSERT INTO perfil_tyc (id_perfil, descripcion, fecha_inicio)
                VALUES (?, 'Términos vigentes para reemplazo', NOW())
                RETURNING id_tyc
                """, Long.class, escenario.idPerfilCandidato());

        generarReemplazosService.generar(new ReemplazoNecesarioEvent(
                escenario.idProyecto(),
                escenario.idMiembroBaja(),
                escenario.idPerfilBaja(),
                OrigenReemplazo.BAJA_VOLUNTARIA));

        var reemplazoActividad = reemplazoRepository
                .findByProyectoIdProyectoOrderByFechaCreacionDesc(escenario.idProyecto())
                .stream()
                .filter(r -> r.getTipo().name().equals("ACTIVIDAD"))
                .findFirst()
                .orElseThrow();

        var candidato = reemplazoCandidatoRepository
                .findConDetalleByReemplazo(reemplazoActividad.getIdReemplazo())
                .getFirst();

        confirmarReemplazoService.confirmar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazoActividad.getIdReemplazo(),
                new ConfirmarReemplazoRequest(List.of(candidato.getIdCandidato()), "Te invitamos"));

        invitacionReemplazoService.aceptar(
                escenario.idUsuarioCandidato(),
                escenario.idPerfilCandidato(),
                candidato.getIdCandidato());

        var respuesta = aprobacionIncorporacionService.aprobar(
                escenario.idUsuarioDirector(),
                escenario.idPerfilDirector(),
                escenario.idProyecto(),
                reemplazoActividad.getIdReemplazo(),
                candidato.getIdCandidato(),
                new AprobarCandidatoRequest(
                        new AceptacionTerminosRequest(idTyc, true)));

        MiembroProyecto miembro = miembroProyectoRepository.findMiembroActivoConRol(
                escenario.idProyecto(), escenario.idPerfilCandidato()).orElseThrow();

        assertThat(miembro.getIdTycAceptado()).isEqualTo(idTyc);
        assertThat(respuesta.idTycAceptado()).isEqualTo(idTyc);
    }

    private Reemplazo reemplazoActividad(Escenario escenario) {
        return reemplazoRepository
                .findByProyectoIdProyectoOrderByFechaCreacionDesc(escenario.idProyecto())
                .stream()
                .filter(r -> r.getTipo().name().equals("ACTIVIDAD"))
                .findFirst()
                .orElseThrow();
    }

    private List<ReemplazoCandidato> candidatosDe(Reemplazo reemplazo) {
        return reemplazoCandidatoRepository.findConDetalleByReemplazo(
                reemplazo.getIdReemplazo());
    }

    private ReemplazoCandidato candidato(Long idCandidato) {
        return reemplazoCandidatoRepository.findById(idCandidato).orElseThrow();
    }

    /**
     * Agrega un segundo perfil disponible para el mismo reemplazo, para poder
     * ejercitar el flujo de varias aceptaciones sin depender de que el motor de
     * matching devuelva más de un candidato.
     */
    private ReemplazoCandidato agregarCandidatoAdicional(
            Reemplazo reemplazo,
            Escenario escenario) {

        String sufijo = String.valueOf(Math.abs(System.nanoTime()));
        String baseDni = sufijo.substring(Math.max(0, sufijo.length() - 11));
        String baseCorreo = "reemplazo.adicional." + sufijo + ".";

        RolGlobal rolGlobal = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        Usuario usuario = crearUsuario(
                baseCorreo + "candidato@example.com", baseDni, rolGlobal, genero);
        Profesion profesion = em.find(Profesion.class, escenario.idProfesion());
        Perfil perfil = crearPerfil(
                usuario, "Candidato adicional " + sufijo, profesion);
        crearAgendaDisponible(usuario, reemplazo.getActividad().getFechaHoraInicio());

        return reemplazoCandidatoRepository.saveAndFlush(ReemplazoCandidato.builder()
                .reemplazo(reemplazo)
                .idProyecto(escenario.idProyecto())
                .perfil(perfil)
                .usuario(usuario)
                .estado(EstadoCandidatoReemplazo.PROPUESTO)
                .ordenRanking(2)
                .puntaje(BigDecimal.valueOf(80))
                .nivelUbicacion(NivelUbicacion.MISMA_CIUDAD)
                .esAlternativo(false)
                .tipoDisponibilidad(TipoDisponibilidad.EXACTA)
                .build());
    }

    private Escenario crearEscenario() {
        String sufijo = String.valueOf(Math.abs(System.nanoTime()));
        String baseDni = sufijo.substring(Math.max(0, sufijo.length() - 11));
        String baseCorreo = "reemplazo." + sufijo + ".";

        RolGlobal rolGlobal = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        Profesion profesion = Profesion.builder()
                .codigo("TEST_REEMPLAZO_" + sufijo)
                .nombre("Profesión reemplazo " + sufijo)
                .descripcion("Profesión aislada para test de reemplazos")
                .build();
        em.persist(profesion);
        em.flush();

        Usuario usuarioDirector = crearUsuario(
                baseCorreo + "director@example.com", baseDni + "1", rolGlobal, genero);
        Usuario usuarioBaja = crearUsuario(
                baseCorreo + "baja@example.com", baseDni + "2", rolGlobal, genero);
        Usuario usuarioCandidato = crearUsuario(
                baseCorreo + "candidato@example.com", baseDni + "3", rolGlobal, genero);

        Perfil perfilDirector = crearPerfil(usuarioDirector, "Director " + sufijo, profesion);
        Perfil perfilBaja = crearPerfil(usuarioBaja, "Baja " + sufijo, profesion);
        Perfil perfilCandidato = crearPerfil(
                usuarioCandidato, "Candidato " + sufijo, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto reemplazo " + sufijo)
                .descripcion("Proyecto de test de reemplazo")
                .fechaInicio(LocalDate.now())
                .estado(EstadoProyecto.CONFIRMADO)
                .privacidad(Privacidad.PRIVADO)
                .build();
        em.persist(proyecto);

        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(30))
                .build();
        em.persist(planificacion);

        RolProyecto rolDirector = em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = 'DIRECTOR'", RolProyecto.class)
                .getSingleResult();
        RolProyecto rolMiembro = em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = 'MIEMBRO'", RolProyecto.class)
                .getSingleResult();

        crearMiembro(proyecto, perfilDirector, rolDirector, null);
        MiembroProyecto miembroBaja = crearMiembro(
                proyecto, perfilBaja, rolMiembro, null);

        RequerimientoGralProyecto requerimientoGral =
                RequerimientoGralProyecto.builder()
                        .proyecto(proyecto)
                        .profesion(profesion)
                        .cantidad(1)
                        .descripcion("Requerimiento general de reemplazo")
                        .build();
        em.persist(requerimientoGral);
        miembroBaja.setRequerimientoGral(requerimientoGral);

        LocalDateTime inicioActividad = LocalDateTime.now()
                .plusDays(2)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        Actividad actividad = Actividad.builder()
                .nombre("Actividad de reemplazo")
                .descripcion("Actividad de test")
                .duracionMinutos(60)
                .fechaHoraInicio(inicioActividad)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);

        RequerimientoActividad requerimientoActividad = RequerimientoActividad.builder()
                .actividad(actividad)
                .profesion(profesion)
                .cantidad(1)
                .descripcion("Requerimiento de actividad de reemplazo")
                .build();
        em.persist(requerimientoActividad);

        em.persist(AsignacionActividad.builder()
                .miembro(miembroBaja)
                .actividad(actividad)
                .requerimientoActividad(requerimientoActividad)
                .build());

        crearAgendaDisponible(usuarioCandidato, inicioActividad);

        miembroBaja.setEstadoParticipacion(EstadoParticipacion.BAJA_VOLUNTARIA);
        miembroBaja.setFechaBaja(LocalDateTime.now());
        em.flush();

        return new Escenario(
                proyecto.getIdProyecto(),
                usuarioDirector.getIdUsuario(),
                perfilDirector.getIdPerfil(),
                usuarioCandidato.getIdUsuario(),
                perfilCandidato.getIdPerfil(),
                miembroBaja.getIdMiembro(),
                perfilBaja.getIdPerfil(),
                actividad.getIdActividad(),
                profesion.getIdProfesion());
    }

    private Usuario crearUsuario(
            String correo,
            String dni,
            RolGlobal rolGlobal,
            Genero genero) {

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Usuario")
                .apellido("Reemplazo")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo)
                .rolGlobal(rolGlobal)
                .genero(genero)
                .build());
    }

    private Perfil crearPerfil(
            Usuario usuario,
            String nombreArtistico,
            Profesion profesion) {

        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de test de reemplazo")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private MiembroProyecto crearMiembro(
            Proyecto proyecto,
            Perfil perfil,
            RolProyecto rolProyecto,
            RequerimientoGralProyecto requerimientoGral) {

        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolProyecto)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .requerimientoGral(requerimientoGral)
                .build();
        em.persist(miembro);
        return miembro;
    }

    private void crearAgendaDisponible(Usuario usuario, LocalDateTime inicioActividad) {
        Agenda agenda = agendaRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseGet(() -> {
                    Agenda nueva = Agenda.builder()
                            .usuario(usuario)
                            .margenActividadMinutos(0)
                            .build();
                    em.persist(nueva);
                    em.flush();
                    return nueva;
                });

        int dia = inicioActividad.getDayOfWeek().getValue();
        boolean existeDia = jornadaAgendaRepository
                .findByAgenda_IdAgendaOrderByDiaSemana(agenda.getIdAgenda())
                .stream()
                .anyMatch(jornada -> jornada.getDiaSemana().equals(dia));

        if (!existeDia) {
            em.persist(JornadaAgenda.builder()
                    .agenda(agenda)
                    .diaSemana(dia)
                    .horaInicioManana(LocalTime.of(8, 0))
                    .horaFinTarde(LocalTime.of(20, 0))
                    .build());
        }
    }

    private record Escenario(
            Long idProyecto,
            Long idUsuarioDirector,
            Long idPerfilDirector,
            Long idUsuarioCandidato,
            Long idPerfilCandidato,
            Long idMiembroBaja,
            Long idPerfilBaja,
            Long idActividad,
            Long idProfesion) {
    }
}
