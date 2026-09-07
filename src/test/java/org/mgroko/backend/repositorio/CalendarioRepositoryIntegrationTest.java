package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mgroko.backend.calendario.dto.ConfigJornadaRequest;
import org.mgroko.backend.calendario.dto.ConfigJornadaResponse;
import org.mgroko.backend.calendario.dto.JornadaDiaRequest;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.BloqueoAgenda;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.JornadaAgenda;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración del calendario contra PostgreSQL real (Testcontainers).
 * Valida los triggers de la migración V14, los checks de jornada partida
 * de la migración V19 y las consultas de los repositorios de
 * agenda/jornada/bloqueo, además del índice de rango.
 */
@SpringBootTest
@Transactional
class CalendarioRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String CORREO = "calendario@example.com";
    private static final String DNI = "77770001";

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
    private BloqueoAgendaRepository bloqueoAgendaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CalendarioService calendarioService;

    private Usuario construirUsuario(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        return Usuario.builder()
                .nombre("Ana").apellido("Gomez").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(22))
                .correo(correo).rolGlobal(rol).genero(genero).build();
    }

    private Usuario guardarUsuario() {
        return usuarioRepository.saveAndFlush(construirUsuario(CORREO, DNI));
    }

    private Agenda agendaDe(Usuario usuario) {
        return agendaRepository.findByUsuario_IdUsuario(usuario.getIdUsuario()).orElseThrow();
    }

    private BloqueoAgenda guardarBloqueo(Agenda agenda, LocalDateTime inicio, LocalDateTime fin) {
        return bloqueoAgendaRepository.saveAndFlush(BloqueoAgenda.builder()
                .agenda(agenda).fechaHoraInicio(inicio).fechaHoraFin(fin).build());
    }

    // ------------------------------------------------------------------
    // Trigger: agenda + jornada auto-creadas al insertar usuario
    // ------------------------------------------------------------------

    @Test
    void insertarUsuario_creaAgendaConJornadaPorDefecto() {
        Usuario usuario = guardarUsuario();

        Agenda agenda = agendaDe(usuario);

        assertNotNull(agenda.getIdAgenda());
        assertEquals(60, agenda.getMargenActividadMinutos());

        List<JornadaAgenda> dias = jornadaAgendaRepository
                .findByAgenda_IdAgendaOrderByDiaSemana(agenda.getIdAgenda());
        assertEquals(5, dias.size());
        assertTrue(dias.stream().allMatch(d -> d.getHorarioInicioManiana().equals(LocalTime.of(9, 0))));
        assertTrue(dias.stream().allMatch(d -> d.getHorarioFinTarde().equals(LocalTime.of(18, 0))));
        // La jornada por defecto es de corrido (par del mediodía ausente).
        assertTrue(dias.stream().allMatch(d -> d.getHorarioFinManiana() == null));
        assertTrue(dias.stream().allMatch(d -> d.getHorarioInicioTarde() == null));
        assertTrue(dias.stream().noneMatch(JornadaAgenda::esPartida));
    }

    // ------------------------------------------------------------------
    // Consultas de BloqueoAgendaRepository
    // ------------------------------------------------------------------

    @Test
    void findByAgenda_ordenaPorInicio() {
        Agenda agenda = agendaDe(guardarUsuario());
        guardarBloqueo(agenda, LocalDateTime.of(2026, 9, 16, 10, 0), LocalDateTime.of(2026, 9, 16, 12, 0));
        guardarBloqueo(agenda, LocalDateTime.of(2026, 9, 15, 10, 0), LocalDateTime.of(2026, 9, 15, 12, 0));

        List<BloqueoAgenda> bloqueos = bloqueoAgendaRepository
                .findByAgenda_IdAgendaOrderByFechaHoraInicio(agenda.getIdAgenda());

        assertEquals(2, bloqueos.size());
        assertEquals(LocalDateTime.of(2026, 9, 15, 10, 0), bloqueos.get(0).getFechaHoraInicio());
        assertEquals(LocalDateTime.of(2026, 9, 16, 10, 0), bloqueos.get(1).getFechaHoraInicio());
    }

    @Test
    void existsByAgenda_solape_devuelveTrue() {
        Agenda agenda = agendaDe(guardarUsuario());
        guardarBloqueo(agenda, LocalDateTime.of(2026, 9, 15, 10, 0), LocalDateTime.of(2026, 9, 15, 14, 0));

        boolean solapa = bloqueoAgendaRepository
                .existsByAgenda_IdAgendaAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                        agenda.getIdAgenda(),
                        LocalDateTime.of(2026, 9, 15, 16, 0),
                        LocalDateTime.of(2026, 9, 15, 12, 0));

        assertTrue(solapa);
    }

    @Test
    void existsByAgenda_toqueExacto_devuelveFalse() {
        Agenda agenda = agendaDe(guardarUsuario());
        guardarBloqueo(agenda, LocalDateTime.of(2026, 9, 15, 10, 0), LocalDateTime.of(2026, 9, 15, 14, 0));

        // Nuevo bloqueo [14:00, 16:00]: toca exactamente en 14:00, no solapa.
        boolean solapa = bloqueoAgendaRepository
                .existsByAgenda_IdAgendaAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                        agenda.getIdAgenda(),
                        LocalDateTime.of(2026, 9, 15, 16, 0),
                        LocalDateTime.of(2026, 9, 15, 14, 0));

        assertFalse(solapa);
    }

    @Test
    void findByIdBloqueoAndAgenda_otraAgenda_devuelveVacio() {
        Agenda agenda1 = agendaDe(guardarUsuario());
        Agenda agenda2 = agendaDe(usuarioRepository
                .saveAndFlush(construirUsuario("otro.cal@example.com", "77770002")));

        BloqueoAgenda bloqueo = guardarBloqueo(
                agenda1, LocalDateTime.of(2026, 9, 15, 10, 0), LocalDateTime.of(2026, 9, 15, 12, 0));

        assertTrue(bloqueoAgendaRepository.findByIdBloqueoAndAgenda_IdAgenda(
                bloqueo.getIdBloqueo(), agenda1.getIdAgenda()).isPresent());
        assertTrue(bloqueoAgendaRepository.findByIdBloqueoAndAgenda_IdAgenda(
                bloqueo.getIdBloqueo(), agenda2.getIdAgenda()).isEmpty());
    }

    // ------------------------------------------------------------------
    // Trigger: no solapar bloqueos manuales
    // ------------------------------------------------------------------

    @Test
    void insertarBloqueoSolapado_lanzaExcepcionDeIntegridad() {
        Agenda agenda = agendaDe(guardarUsuario());
        guardarBloqueo(agenda, LocalDateTime.of(2026, 9, 15, 10, 0), LocalDateTime.of(2026, 9, 15, 14, 0));

        assertExcepcionConMensaje(
                () -> guardarBloqueo(agenda, LocalDateTime.of(2026, 9, 15, 13, 0), LocalDateTime.of(2026, 9, 15, 16, 0)),
                "El bloqueo se superpone con otro bloqueo existente de la agenda");
    }

    @Test
    void insertarBloqueosNoSolapados_persiste() {
        Agenda agenda = agendaDe(guardarUsuario());
        guardarBloqueo(agenda, LocalDateTime.of(2026, 9, 15, 10, 0), LocalDateTime.of(2026, 9, 15, 14, 0));

        assertDoesNotThrow(() -> guardarBloqueo(
                agenda, LocalDateTime.of(2026, 9, 15, 14, 0), LocalDateTime.of(2026, 9, 15, 16, 0)));
    }

    // ------------------------------------------------------------------
    // Índice de rango
    // ------------------------------------------------------------------

    @Test
    void indiceRangoExiste() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = 'idx_bloqueo_agenda_rango'",
                Integer.class);
        assertEquals(1, count);
    }

    // ------------------------------------------------------------------
    // Servicio: editar la jornada existente (regresión del 500)
    // ------------------------------------------------------------------

    @Test
    void editarJornada_conMismosDias_noLanzaViolacionDeIntegridad() {
        Usuario usuario = guardarUsuario();
        ConfigJornadaRequest lunVie = new ConfigJornadaRequest(45, List.of(
                new JornadaDiaRequest(1, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                new JornadaDiaRequest(2, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                new JornadaDiaRequest(3, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                new JornadaDiaRequest(4, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                new JornadaDiaRequest(5, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0))));

        // Primera edición: cambia el margen manteniendo L-V por defecto.
        ConfigJornadaResponse primera =
                calendarioService.configurarJornada(usuario.getIdUsuario(), lunVie);
        assertEquals(45, primera.margenActividadMinutos());
        assertEquals(5, primera.dias().size());

        // Segunda edición: se reenvían los mismos días. Este escenario
        // rompía con delete + reinsert sin flush por la unique
        // uq_jornada_agenda_dia (id_agenda, dia_semana).
        ConfigJornadaResponse segunda =
                calendarioService.configurarJornada(usuario.getIdUsuario(), lunVie);
        assertEquals(5, segunda.dias().size());

        // Edición con cambios: actualiza un horario y agrega el sábado.
        ConfigJornadaResponse tercera = calendarioService.configurarJornada(
                usuario.getIdUsuario(),
                new ConfigJornadaRequest(45, List.of(
                        new JornadaDiaRequest(1, LocalTime.of(8, 0), null, null, LocalTime.of(17, 0)),
                        new JornadaDiaRequest(2, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                        new JornadaDiaRequest(3, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                        new JornadaDiaRequest(4, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                        new JornadaDiaRequest(5, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                        new JornadaDiaRequest(6, LocalTime.of(10, 0), null, null, LocalTime.of(16, 0)))));
        assertEquals(6, tercera.dias().size());
        assertEquals(LocalTime.of(8, 0), tercera.dias().get(0).horarioInicioManiana());

        // Y una que quita días: vuelve a eliminar el sábado.
        ConfigJornadaResponse cuarta = calendarioService.configurarJornada(
                usuario.getIdUsuario(),
                new ConfigJornadaRequest(45, List.of(
                        new JornadaDiaRequest(1, LocalTime.of(8, 0), null, null, LocalTime.of(17, 0)),
                        new JornadaDiaRequest(2, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                        new JornadaDiaRequest(3, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                        new JornadaDiaRequest(4, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                        new JornadaDiaRequest(5, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)))));
        assertEquals(5, cuarta.dias().size());
    }

    // ------------------------------------------------------------------
    // Jornada partida (checks de la migración V19)
    // ------------------------------------------------------------------

    @Test
    void configurarJornada_partida_persisteLosCuatroHorarios() {
        Usuario usuario = guardarUsuario();

        ConfigJornadaResponse response = calendarioService.configurarJornada(
                usuario.getIdUsuario(),
                new ConfigJornadaRequest(45, List.of(
                        new JornadaDiaRequest(1, LocalTime.of(9, 0), LocalTime.of(13, 0),
                                LocalTime.of(15, 0), LocalTime.of(19, 0)))));

        assertEquals(1, response.dias().size());
        assertEquals(LocalTime.of(9, 0), response.dias().get(0).horarioInicioManiana());
        assertEquals(LocalTime.of(13, 0), response.dias().get(0).horarioFinManiana());
        assertEquals(LocalTime.of(15, 0), response.dias().get(0).horarioInicioTarde());
        assertEquals(LocalTime.of(19, 0), response.dias().get(0).horarioFinTarde());

        // Y puede volver a corrido (el par del mediodía queda en NULL).
        ConfigJornadaResponse corrido = calendarioService.configurarJornada(
                usuario.getIdUsuario(),
                new ConfigJornadaRequest(45, List.of(
                        new JornadaDiaRequest(1, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)))));
        assertNull(corrido.dias().get(0).horarioFinManiana());
        assertNull(corrido.dias().get(0).horarioInicioTarde());
    }

    @Test
    void insertarJornada_mediodiaIncompleto_violaCheck() {
        Agenda agenda = agendaDe(guardarUsuario());

        // Solo fin de mañana, sin inicio de tarde: chk_jornada_mediodia_completo.
        assertExcepcionConMensaje(
                () -> jornadaAgendaRepository.saveAndFlush(JornadaAgenda.builder()
                        .agenda(agenda).diaSemana(6)
                        .horarioInicioManiana(LocalTime.of(9, 0))
                        .horarioFinManiana(LocalTime.of(13, 0))
                        .horarioFinTarde(LocalTime.of(18, 0))
                        .build()),
                "chk_jornada_mediodia_completo");
    }

    @Test
    void insertarJornada_tardeAntesQueManiana_violaCheck() {
        Agenda agenda = agendaDe(guardarUsuario());

        // inicio_tarde <= fin_maniana: chk_jornada_orden_bloques.
        assertExcepcionConMensaje(
                () -> jornadaAgendaRepository.saveAndFlush(JornadaAgenda.builder()
                        .agenda(agenda).diaSemana(6)
                        .horarioInicioManiana(LocalTime.of(9, 0))
                        .horarioFinManiana(LocalTime.of(13, 0))
                        .horarioInicioTarde(LocalTime.of(13, 0))
                        .horarioFinTarde(LocalTime.of(18, 0))
                        .build()),
                "chk_jornada_orden_bloques");
    }

    @Test
    void insertarJornada_finTardeAntesDeInicioManiana_violaCheck() {
        Agenda agenda = agendaDe(guardarUsuario());

        // Jornada corrida con fin <= inicio: chk_jornada_rango_total.
        assertExcepcionConMensaje(
                () -> jornadaAgendaRepository.saveAndFlush(JornadaAgenda.builder()
                        .agenda(agenda).diaSemana(6)
                        .horarioInicioManiana(LocalTime.of(18, 0))
                        .horarioFinTarde(LocalTime.of(9, 0))
                        .build()),
                "chk_jornada_rango_total");
    }

    /**
     * Afirma que ejecutar {@code op} lanza una excepción cuya cadena de
     * causas contiene el fragmento de mensaje esperado. Los triggers usan
     * {@code RAISE EXCEPTION}, que Hibernate envuelve en distintos tipos
     * de excepción según la vía de ejecución, por lo que se valida el
     * mensaje raíz en lugar del tipo exacto.
     */
    private void assertExcepcionConMensaje(Executable op, String fragmento) {
        Exception ex = assertThrows(Exception.class, op);
        assertTrue(contieneMensaje(ex, fragmento),
                "Se esperaba el mensaje '" + fragmento + "' pero fue: " + ex.getMessage());
    }

    private boolean contieneMensaje(Throwable t, String fragmento) {
        if (t == null) {
            return false;
        }
        if (t.getMessage() != null && t.getMessage().contains(fragmento)) {
            return true;
        }
        return contieneMensaje(t.getCause(), fragmento);
    }
}