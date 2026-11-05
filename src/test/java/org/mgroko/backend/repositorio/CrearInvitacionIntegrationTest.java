package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.BuscarRequerimientosActividadFiltro;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.dto.RequerimientoActividadBusquedaResponse;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.servicio.BuscarRequerimientoActividadService;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionActividadBatchRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionActividadRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralBatchRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralRequest;
import org.mgroko.backend.invitaciones.dto.InvitacionBatchResponse;
import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.invitaciones.servicio.CrearInvitacionBatchService;
import org.mgroko.backend.invitaciones.servicio.CrearInvitacionService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.CancelarProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.servicio.CancelarProyectoService;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.PublicarProyectoService;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración de UC-31 y UC-73 contra PostgreSQL real.
 */
@SpringBootTest
@Transactional
class CrearInvitacionIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private CrearInvitacionBatchService crearInvitacionBatchService;
    @Autowired
    private BuscarRequerimientoActividadService buscarRequerimientoActividadService;
    @Autowired
    private CancelarProyectoService cancelarProyectoService;
    @Autowired
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record PerfilCreado(Long idUsuario, Long idPerfil) {
    }

    @Test
    void general_publicado_persisteInvitacionYNotificaAlDestinatario() {
        PerfilCreado director = crearPerfil(
                "uc31.dir1@example.com", "88831001", "Directora UC31", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc31.dest1@example.com", "88831002", "Destinataria UC31", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC31 General");

        InvitacionResponse respuesta = crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), "Te invito"));

        assertThat(respuesta.tipo()).isEqualTo(TipoInvitacion.GENERAL);
        assertThat(respuesta.estado().name()).isEqualTo("PENDIENTE");
        assertThat(respuesta.idProyecto()).isEqualTo(publicado.idProyecto());
        assertThat(respuesta.idRequerimientoGral()).isNull();
        assertThat(respuesta.idRequerimientoActividad()).isNull();
        assertThat(respuesta.mensaje()).isEqualTo("Te invito");

        Long invitaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitacion_gral
                WHERE id_proyecto = ? AND id_perfil_destinatario = ?
                  AND id_requerimiento_gral IS NULL AND estado = 'PENDIENTE'
                """, Long.class, publicado.idProyecto(), destinatario.idPerfil());
        assertThat(invitaciones).isEqualTo(1L);

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'INVITACION_RECIBIDA'
                  AND entidad_tipo = 'INVITACION_GRAL' AND entidad_id = ?
                """, Long.class, destinatario.idUsuario(), respuesta.idInvitacion());
        assertThat(notificaciones).isEqualTo(1L);
    }

    @Test
    void batchGeneral_dosPerfiles_creaDosInvitacionesYNotificaciones() {
        PerfilCreado director = crearPerfil(
                "uc31.batch.dir1@example.com", "88831020", "Directora Batch UC31", "modelo");
        PerfilCreado destinatarioA = crearPerfil(
                "uc31.batch.dest1@example.com", "88831021", "Destinataria A Batch", "modelo");
        PerfilCreado destinatarioB = crearPerfil(
                "uc31.batch.dest2@example.com", "88831022", "Destinataria B Batch", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC31 Batch");

        InvitacionBatchResponse respuesta = crearInvitacionBatchService.invitarGeneral(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralBatchRequest(
                        List.of(destinatarioA.idPerfil(), destinatarioB.idPerfil()),
                        "Invitación multiple"));

        assertThat(respuesta.creadas()).hasSize(2);
        assertThat(respuesta.errores()).isEmpty();

        Long invitaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitacion_gral WHERE id_proyecto = ?
                """, Long.class, publicado.idProyecto());
        assertThat(invitaciones).isEqualTo(2L);

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE tipo = 'INVITACION_RECIBIDA' AND id_proyecto = ?
                """, Long.class, publicado.idProyecto());
        assertThat(notificaciones).isEqualTo(2L);
    }

    @Test
    void batchGeneral_unPerfilDuplicado_creaElRestoYReportaError() {
        PerfilCreado director = crearPerfil(
                "uc31.batch.dir2@example.com", "88831023", "Directora Batch Dup", "modelo");
        PerfilCreado destinatarioA = crearPerfil(
                "uc31.batch.dest3@example.com", "88831024", "Destinataria A Dup", "modelo");
        PerfilCreado destinatarioB = crearPerfil(
                "uc31.batch.dest4@example.com", "88831025", "Destinataria B Dup", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC31 Batch Dup");

        crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatarioA.idPerfil(), null));

        InvitacionBatchResponse respuesta = crearInvitacionBatchService.invitarGeneral(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralBatchRequest(
                        List.of(destinatarioA.idPerfil(), destinatarioB.idPerfil()), null));

        assertThat(respuesta.creadas()).hasSize(1);
        assertThat(respuesta.errores()).hasSize(1);
        assertThat(respuesta.errores().getFirst().idPerfilDestinatario())
                .isEqualTo(destinatarioA.idPerfil());
        assertThat(respuesta.errores().getFirst().codigo())
                .isEqualTo(CrearInvitacionService.CODIGO_INVITACION_DUPLICADA);

        Long invitaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitacion_gral WHERE id_proyecto = ?
                """, Long.class, publicado.idProyecto());
        assertThat(invitaciones).isEqualTo(2L);
    }

    @Test
    void batchActividad_dosPerfiles_creaDosInvitaciones() {
        PerfilCreado director = crearPerfil(
                "uc31.batch.dir3@example.com", "88831026", "Directora Batch Act", "modelo");
        PerfilCreado destinatarioA = crearPerfil(
                "uc31.batch.dest5@example.com", "88831027", "Destinataria A Act", "modelo");
        PerfilCreado destinatarioB = crearPerfil(
                "uc31.batch.dest6@example.com", "88831028", "Destinataria B Act", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(
                director, "Desfile UC31 Batch Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        InvitacionBatchResponse respuesta = crearInvitacionBatchService.invitarActividad(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionActividadBatchRequest(
                        idRequerimiento,
                        List.of(destinatarioA.idPerfil(), destinatarioB.idPerfil()),
                        null));

        assertThat(respuesta.creadas()).hasSize(2);
        assertThat(respuesta.errores()).isEmpty();

        Long invitaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitacion_actividad
                WHERE id_requerimiento_act = ?
                """, Long.class, idRequerimiento);
        assertThat(invitaciones).isEqualTo(2L);
    }

    @Test
    void general_conInvitacionPendiente_lanzaConflictoYNoDuplica() {
        PerfilCreado director = crearPerfil(
                "uc31.dir2@example.com", "88831003", "Directora Dup UC31", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc31.dest2@example.com", "88831004", "Destinataria Dup UC31", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC31 Dup");

        crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), null));

        assertThatThrownBy(() -> crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), null)))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("invitación activa");

        Long invitaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitacion_gral
                WHERE id_proyecto = ? AND id_perfil_destinatario = ?
                """, Long.class, publicado.idProyecto(), destinatario.idPerfil());
        assertThat(invitaciones).isEqualTo(1L);
    }

    @Test
    void actividad_persisteInvitacionAsociadaAlRequerimientoYNotifica() {
        PerfilCreado director = crearPerfil(
                "uc31.dir3@example.com", "88831005", "Directora Act UC31", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc31.dest3@example.com", "88831006", "Destinataria Act UC31", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(
                director, "Desfile UC31 Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        InvitacionResponse respuesta = crearInvitacionService.invitarAProyectoActividad(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionActividadRequest(
                        idRequerimiento, destinatario.idPerfil(), null));

        assertThat(respuesta.tipo()).isEqualTo(TipoInvitacion.ACTIVIDAD);
        assertThat(respuesta.idRequerimientoActividad()).isEqualTo(idRequerimiento);
        assertThat(respuesta.idProyecto()).isEqualTo(publicado.idProyecto());

        Long invitaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM invitacion_actividad
                WHERE id_requerimiento_act = ? AND id_perfil_destinatario = ?
                  AND estado = 'PENDIENTE'
                """, Long.class, idRequerimiento, destinatario.idPerfil());
        assertThat(invitaciones).isEqualTo(1L);

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'INVITACION_RECIBIDA'
                  AND entidad_tipo = 'INVITACION_ACTIVIDAD' AND entidad_id = ?
                """, Long.class, destinatario.idUsuario(), respuesta.idInvitacion());
        assertThat(notificaciones).isEqualTo(1L);
    }

    @Test
    void cancelarProyecto_rechazaInvitacionGeneralAsociadaAlProyecto() {
        PerfilCreado director = crearPerfil(
                "uc31.dir4@example.com", "88831007", "Directora Cancela UC31", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc31.dest4@example.com", "88831008", "Destinataria Cancela UC31", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC31 Cancelar");

        crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), null));

        cancelarProyectoService.cancelar(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CancelarProyectoRequest("Cancelación UC31"));

        String estado = jdbcTemplate.queryForObject("""
                SELECT estado FROM invitacion_gral
                WHERE id_proyecto = ? AND id_perfil_destinatario = ?
                """, String.class, publicado.idProyecto(), destinatario.idPerfil());
        assertThat(estado).isEqualTo("RECHAZADA");

        Long notificaciones = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificacion
                WHERE id_usuario_destino = ? AND tipo = 'INVITACION_RECHAZADA'
                  AND id_proyecto = ?
                """, Long.class, destinatario.idUsuario(), publicado.idProyecto());
        assertThat(notificaciones).isEqualTo(1L);
    }

    @Test
    void buscarRequerimientosActividad_devuelveLosDelProyecto() {
        PerfilCreado director = crearPerfil(
                "uc73.dir1@example.com", "88873001", "Directora UC73", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(
                director, "Desfile UC73");

        PaginaResponse<RequerimientoActividadBusquedaResponse> pagina =
                buscarRequerimientoActividadService.buscar(
                        director.idPerfil(),
                        publicado.idProyecto(),
                        new BuscarRequerimientosActividadFiltro(
                                null, null, null, null, null),
                        0, 20, false);

        assertThat(pagina.contenido()).hasSize(1);
        assertThat(pagina.contenido().getFirst().idProyecto())
                .isEqualTo(publicado.idProyecto());
        assertThat(pagina.contenido().getFirst().nombreActividad())
                .isEqualTo("Prueba UC31");

        PaginaResponse<RequerimientoActividadBusquedaResponse> filtrada =
                buscarRequerimientoActividadService.buscar(
                        director.idPerfil(),
                        publicado.idProyecto(),
                        new BuscarRequerimientosActividadFiltro(
                                pagina.contenido().getFirst().idActividad(),
                                pagina.contenido().getFirst().idProfesion(),
                                null, null, null),
                        0, 20, false);

        assertThat(filtrada.contenido()).hasSize(1);
        assertThat(filtrada.contenido().getFirst().idRequerimiento())
                .isEqualTo(pagina.contenido().getFirst().idRequerimiento());

        PaginaResponse<RequerimientoActividadBusquedaResponse> porCaracteristica =
                buscarRequerimientoActividadService.buscar(
                        director.idPerfil(),
                        publicado.idProyecto(),
                        new BuscarRequerimientosActividadFiltro(
                                null, null, 1L, 1L, null),
                        0, 20, false);

        assertThat(porCaracteristica.contenido()).isEmpty();
    }

    private PerfilCreado crearPerfil(
            String correo,
            String dni,
            String nombreArtistico,
            String codigoProfesion) {

        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC31").apellido("Test").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(26))
                .correo(correo)
                .estado(org.mgroko.backend.modelo.enums.EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase(codigoProfesion)
                .map(Profesion::getIdProfesion)
                .orElseGet(() -> profesionRepository
                        .buscar("%" + codigoProfesion + "%", codigoProfesion)
                        .get(0).getIdProfesion());

        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesion,
                        "Perfil de prueba UC-31.", List.of()));

        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, id_tyc, descripcion, fecha_inicio) "
                        + "VALUES (?, 1, 'TyC UC-31', NOW())",
                perfil.idPerfil());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private Long idProfesion(String codigo) {
        return profesionRepository.buscar("%" + codigo + "%", codigo)
                .getFirst().getIdProfesion();
    }

    private CrearProyectoRequest proyectoConRequerimientoGral(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto de prueba UC-31", Privacidad.PUBLICO,
                LocalDate.now().plusDays(30), true,
                new UbicacionRequest("0602801002"),
                null,
                List.of(new CrearRequerimientoGralRequest(
                        1, idProfesion("modelo"), "Una modelo", null, null)),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private CrearProyectoRequest proyectoConActividad(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto de prueba UC-31 con actividad", Privacidad.PUBLICO,
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
                        "Prueba UC31", "Actividad de prueba UC-31", 2,
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
