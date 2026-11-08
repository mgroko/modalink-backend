package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionActividadRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralRequest;
import org.mgroko.backend.invitaciones.dto.DireccionInvitacion;
import org.mgroko.backend.invitaciones.dto.InvitacionBusquedaResponse;
import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.exception.InvitacionNoEncontradaException;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.invitaciones.servicio.BuscarInvitacionService;
import org.mgroko.backend.invitaciones.servicio.CrearInvitacionService;
import org.mgroko.backend.invitaciones.servicio.EliminarInvitacionService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.PublicarProyectoService;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Integración de UC-32: baja física de invitaciones pendientes del remitente.
 */
@SpringBootTest
@Transactional
class EliminarInvitacionIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private ProfesionRepository profesionRepository;
    @Autowired
    private CrearPerfilService crearPerfilService;
    @Autowired
    private CrearProyectoService crearProyectoService;
    @Autowired
    private PublicarProyectoService publicarProyectoService;
    @Autowired
    private CrearActividadService crearActividadService;
    @Autowired
    private CrearInvitacionService crearInvitacionService;
    @Autowired
    private BuscarInvitacionService buscarInvitacionService;
    @Autowired
    private EliminarInvitacionService eliminarInvitacionService;
    @Autowired
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    private record PerfilCreado(Long idUsuario, Long idPerfil) {
    }

    @Test
    void general_pendienteDelRemitente_eliminaYDesapareceDeBusqueda() {
        PerfilCreado director = crearPerfil(
                "uc32.dir1@example.com", "88832001", "Directora UC32", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc32.dest1@example.com", "88832002", "Destinataria UC32", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC32");

        InvitacionResponse invitacion = crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), null));

        assertThat(contarNotificaciones(
                "INVITACION_RECIBIDA", "INVITACION_GRAL", invitacion.idInvitacion()))
                .isEqualTo(1L);

        eliminarInvitacionService.eliminarInvitacionGeneral(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), invitacion.idInvitacion());

        Long restantes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitacion_gral WHERE id_invitacion_gral = ?
                """, Long.class, invitacion.idInvitacion());
        assertThat(restantes).isZero();
        assertThat(contarNotificaciones(
                "INVITACION_RECIBIDA", "INVITACION_GRAL", invitacion.idInvitacion()))
                .isZero();

        PaginaResponse<InvitacionBusquedaResponse> busqueda =
                buscarInvitacionService.buscar(
                        director.idPerfil(),
                        new org.mgroko.backend.invitaciones.dto.BuscarInvitacionesFiltro(
                                null, null, null, null, null,
                                DireccionInvitacion.ENVIADA, null, null),
                        0, 20, false);
        assertThat(busqueda.contenido()).isEmpty();
    }

    @Test
    void actividad_pendienteDelRemitente_elimina() {
        PerfilCreado director = crearPerfil(
                "uc32.dir2@example.com", "88832003", "Directora Act UC32", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc32.dest2@example.com", "88832004", "Destinataria Act UC32", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(
                director, "Desfile UC32 Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        InvitacionResponse invitacion = crearInvitacionService.invitarAProyectoActividad(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionActividadRequest(
                        idRequerimiento, destinatario.idPerfil(), null));

        assertThat(contarNotificaciones(
                "INVITACION_RECIBIDA", "INVITACION_ACTIVIDAD", invitacion.idInvitacion()))
                .isEqualTo(1L);

        eliminarInvitacionService.eliminarInvitacionActividad(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), invitacion.idInvitacion());

        Long restantes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitacion_actividad WHERE id_invitacion = ?
                """, Long.class, invitacion.idInvitacion());
        assertThat(restantes).isZero();
        assertThat(contarNotificaciones(
                "INVITACION_RECIBIDA", "INVITACION_ACTIVIDAD", invitacion.idInvitacion()))
                .isZero();
    }

    @Test
    void general_destinatarioNoPuedeEliminar() {
        PerfilCreado director = crearPerfil(
                "uc32.dir3@example.com", "88832005", "Directora Ajena UC32", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc32.dest3@example.com", "88832006", "Destinataria Ajena UC32", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC32 Ajeno");

        InvitacionResponse invitacion = crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), null));

        assertThatThrownBy(() -> eliminarInvitacionService.eliminarInvitacionGeneral(
                destinatario.idUsuario(), destinatario.idPerfil(),
                publicado.idProyecto(), invitacion.idInvitacion()))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        Long restantes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitacion_gral WHERE id_invitacion_gral = ?
                """, Long.class, invitacion.idInvitacion());
        assertThat(restantes).isEqualTo(1L);
    }

    @Test
    void general_noPendiente_noSeElimina() {
        PerfilCreado director = crearPerfil(
                "uc32.dir4@example.com", "88832007", "Directora Estado UC32", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc32.dest4@example.com", "88832008", "Destinataria Estado UC32", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC32 Estado");

        InvitacionResponse invitacion = crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), null));

        jdbcTemplate.update("""
                UPDATE invitacion_gral SET estado = 'ACEPTADA'
                WHERE id_invitacion_gral = ?
                """, invitacion.idInvitacion());
        entityManager.clear();

        assertThatThrownBy(() -> eliminarInvitacionService.eliminarInvitacionGeneral(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), invitacion.idInvitacion()))
                .isInstanceOf(InvitacionNoPermitidaException.class);

        Long restantes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitacion_gral WHERE id_invitacion_gral = ?
                """, Long.class, invitacion.idInvitacion());
        assertThat(restantes).isEqualTo(1L);
    }

    @Test
    void general_segundaEliminacion_lanzaNoEncontrada() {
        PerfilCreado director = crearPerfil(
                "uc32.dir5@example.com", "88832009", "Directora Doble UC32", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc32.dest5@example.com", "88832010", "Destinataria Doble UC32", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC32 Doble");

        InvitacionResponse invitacion = crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), null));

        eliminarInvitacionService.eliminarInvitacionGeneral(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), invitacion.idInvitacion());

        assertThatThrownBy(() -> eliminarInvitacionService.eliminarInvitacionGeneral(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), invitacion.idInvitacion()))
                .isInstanceOf(InvitacionNoEncontradaException.class);
    }

    @Test
    void general_deOtroProyecto_lanzaNoEncontrada() {
        PerfilCreado director = crearPerfil(
                "uc32.dir6@example.com", "88832011", "Directora Proyecto UC32", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc32.dest6@example.com", "88832012", "Destinataria Proyecto UC32", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC32 Proyecto");
        ProyectoResponse otro = publicarProyectoConRequerimientoGral(
                director, "Desfile UC32 Otro");

        InvitacionResponse invitacion = crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), null));

        assertThatThrownBy(() -> eliminarInvitacionService.eliminarInvitacionGeneral(
                director.idUsuario(), director.idPerfil(),
                otro.idProyecto(), invitacion.idInvitacion()))
                .isInstanceOf(InvitacionNoEncontradaException.class);
    }

    private Long contarNotificaciones(String tipo, String entidadTipo, Long entidadId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE tipo = ?
                  AND entidad_tipo = ?
                  AND entidad_id = ?
                """, Long.class, tipo, entidadTipo, entidadId);
    }

    private PerfilCreado crearPerfil(
            String correo,
            String dni,
            String nombreArtistico,
            String codigoProfesion) {

        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC32").apellido("Test").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(26))
                .correo(correo)
                .estado(org.mgroko.backend.modelo.enums.EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase(codigoProfesion)
                .map(Profesion::getIdProfesion)
                .orElseGet(() -> profesionRepository
                        .buscar("%" + codigoProfesion + "%", codigoProfesion)
                        .getFirst().getIdProfesion());

        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesion,
                        "Perfil de prueba UC-32.", List.of()));

        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, descripcion, fecha_inicio, fecha_fin) "
                        + "VALUES (?, 'TyC UC-32', NOW() - INTERVAL '1 day', NOW())",
                perfil.idPerfil());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private Long idProfesion(String codigo) {
        return profesionRepository.buscar("%" + codigo + "%", codigo)
                .getFirst().getIdProfesion();
    }

    private CrearProyectoRequest proyectoConRequerimientoGral(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto de prueba UC-32", Privacidad.PUBLICO,
                LocalDate.now().plusDays(30), true,
                new UbicacionRequest("0602801002"),
                null,
                List.of(new CrearRequerimientoGralRequest(
                        1, idProfesion("modelo"), "Una modelo", null, null)),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private CrearProyectoRequest proyectoConActividad(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto de prueba UC-32 con actividad", Privacidad.PUBLICO,
                LocalDate.now().plusDays(30), false,
                new UbicacionRequest("0602801002"),
                null,
                List.of(),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private ProyectoResponse publicarProyectoConRequerimientoGral(
            PerfilCreado director,
            String nombre) {

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(), proyectoConRequerimientoGral(nombre));

        return publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto());
    }

    private ProyectoResponse publicarProyectoConActividad(
            PerfilCreado director,
            String nombre) {

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(), proyectoConActividad(nombre));

        ActividadResponse actividad = crearActividadService.crear(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto(),
                new CrearActividadRequest(
                        "Prueba UC32", "Actividad de prueba UC-32", 2,
                        UnidadDuracion.HORAS, null, null,
                        List.of(new CrearRequerimientoActividadRequest(
                                1, idProfesion("modelo"), "Una modelo", null, null))));

        assertThat(actividad.idActividad()).isNotNull();

        return publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto());
    }

    private Long requerimientoDeActividad(ProyectoResponse publicado) {
        return requerimientoActividadRepository
                .findByActividad_IdActividad(idActividadDe(publicado.idProyecto()))
                .getFirst()
                .getIdRequerimiento();
    }

    private Long idActividadDe(Long idProyecto) {
        return jdbcTemplate.queryForObject("""
                SELECT a.id_actividad FROM actividad a
                JOIN planificacion p ON p.id_planificacion = a.id_planificacion
                WHERE p.id_proyecto = ?
                """, Long.class, idProyecto);
    }
}
