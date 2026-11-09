package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.invitaciones.dto.BuscarInvitacionesFiltro;
import org.mgroko.backend.invitaciones.dto.DireccionInvitacion;
import org.mgroko.backend.invitaciones.dto.InvitacionBusquedaResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.invitaciones.servicio.BuscarInvitacionService;
import org.mgroko.backend.invitaciones.servicio.CrearInvitacionService;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionActividadRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralRequest;
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
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.PublicarProyectoService;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración de UC-30 contra PostgreSQL real: búsqueda de invitaciones
 * recibidas y enviadas, tipos, filtros y aislamiento entre perfiles.
 */
@SpringBootTest
@Transactional
class BuscarInvitacionIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record PerfilCreado(Long idUsuario, Long idPerfil) {
    }

    @Test
    void buscar_invitacionGeneral_devuelveEnviadaYRecibida() {
        PerfilCreado director = crearPerfil(
                "uc30.dir1@example.com", "88830001", "Directora UC30", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc30.dest1@example.com", "88830002", "Destinataria UC30", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC30");

        crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), "Mensaje"));

        PaginaResponse<InvitacionBusquedaResponse> enviadas =
                buscarInvitacionService.buscar(
                        director.idPerfil(),
                        new BuscarInvitacionesFiltro(
                                null, null, null, null, null,
                                DireccionInvitacion.ENVIADA, null, null),
                        0, 20, false);

        assertThat(enviadas.contenido()).hasSize(1);
        InvitacionBusquedaResponse enviada = enviadas.contenido().getFirst();
        assertThat(enviada.tipo()).isEqualTo(TipoInvitacion.GENERAL);
        assertThat(enviada.direccion()).isEqualTo(DireccionInvitacion.ENVIADA);
        assertThat(enviada.idProyecto()).isEqualTo(publicado.idProyecto());
        assertThat(enviada.nombreProyecto()).isEqualTo("Desfile UC30");
        assertThat(enviada.destinatario().idPerfil()).isEqualTo(destinatario.idPerfil());
        assertThat(enviada.puedeEliminar()).isTrue();

        PaginaResponse<InvitacionBusquedaResponse> recibidas =
                buscarInvitacionService.buscar(
                        destinatario.idPerfil(),
                        new BuscarInvitacionesFiltro(
                                null, null, null, null, null,
                                DireccionInvitacion.RECIBIDA, null, null),
                        0, 20, false);

        assertThat(recibidas.contenido()).hasSize(1);
        InvitacionBusquedaResponse recibida = recibidas.contenido().getFirst();
        assertThat(recibida.direccion()).isEqualTo(DireccionInvitacion.RECIBIDA);
        assertThat(recibida.remitente().idPerfil()).isEqualTo(director.idPerfil());
        assertThat(recibida.puedeEliminar()).isFalse();
    }

    @Test
    void buscar_invitacionDeActividad_devuelveActividadYRequerimiento() {
        PerfilCreado director = crearPerfil(
                "uc30.dir2@example.com", "88830003", "Directora Act UC30", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc30.dest2@example.com", "88830004", "Destinataria Act UC30", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(
                director, "Desfile UC30 Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        crearInvitacionService.invitarAProyectoActividad(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionActividadRequest(
                        idRequerimiento, destinatario.idPerfil(), null));

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                buscarInvitacionService.buscar(
                        director.idPerfil(),
                        new BuscarInvitacionesFiltro(
                                null, "desfile", null, null, null, null, null,
                                TipoInvitacion.ACTIVIDAD),
                        0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        InvitacionBusquedaResponse invitacion = respuesta.contenido().getFirst();
        assertThat(invitacion.tipo()).isEqualTo(TipoInvitacion.ACTIVIDAD);
        assertThat(invitacion.idRequerimientoActividad()).isEqualTo(idRequerimiento);
        assertThat(invitacion.idActividad()).isNotNull();
        assertThat(invitacion.nombreActividad()).isEqualTo("Prueba UC30");
    }

    @Test
    void buscar_noDevuelveInvitacionesDeOtrosPerfiles() {
        PerfilCreado directorAjeno = crearPerfil(
                "uc30.dir3@example.com", "88830005", "Directora Ajena UC30", "modelo");
        PerfilCreado destinatarioAjeno = crearPerfil(
                "uc30.dest3@example.com", "88830006", "Destinataria Ajena UC30", "modelo");
        PerfilCreado consultante = crearPerfil(
                "uc30.consulta@example.com", "88830007", "Consultante UC30", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                directorAjeno, "Desfile UC30 Ajeno");

        crearInvitacionService.invitarAProyecto(
                directorAjeno.idUsuario(), directorAjeno.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatarioAjeno.idPerfil(), null));

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                buscarInvitacionService.buscar(consultante.idPerfil(), null, 0, 20, false);

        assertThat(respuesta.contenido()).isEmpty();
        assertThat(respuesta.totalElementos()).isZero();
    }

    @Test
    void buscar_porNombreDePerfilYFecha_filtraCorrectamente() {
        PerfilCreado director = crearPerfil(
                "uc30.dir4@example.com", "88830008", "Directora Filtro UC30", "modelo");
        PerfilCreado destinatario = crearPerfil(
                "uc30.dest4@example.com", "88830009", "Sol Filtro UC30", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC30 Filtro");

        crearInvitacionService.invitarAProyecto(
                director.idUsuario(), director.idPerfil(), publicado.idProyecto(),
                new CrearInvitacionGralRequest(destinatario.idPerfil(), null));

        PaginaResponse<InvitacionBusquedaResponse> porNombre =
                buscarInvitacionService.buscar(
                        director.idPerfil(),
                        new BuscarInvitacionesFiltro(
                                null, null, null, null, "sol",
                                DireccionInvitacion.ENVIADA, null, null),
                        0, 20, false);

        assertThat(porNombre.contenido()).hasSize(1);
        assertThat(porNombre.contenido().getFirst().destinatario().idPerfil())
                .isEqualTo(destinatario.idPerfil());

        PaginaResponse<InvitacionBusquedaResponse> porFecha =
                buscarInvitacionService.buscar(
                        director.idPerfil(),
                        new BuscarInvitacionesFiltro(
                                null, null, LocalDate.now(), LocalDate.now(),
                                null, DireccionInvitacion.ENVIADA, null, null),
                        0, 20, false);

        assertThat(porFecha.contenido()).hasSize(1);
    }

    private PerfilCreado crearPerfil(
            String correo,
            String dni,
            String nombreArtistico,
            String codigoProfesion) {

        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC30").apellido("Test").dni(dni)
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
                        "Perfil de prueba UC-30.", List.of()));

        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, descripcion, fecha_inicio, fecha_fin) "
                        + "VALUES (?, 'TyC UC-30', NOW() - INTERVAL '1 day', NOW())",
                perfil.idPerfil());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private Long idProfesion(String codigo) {
        return profesionRepository.buscar("%" + codigo + "%", codigo)
                .getFirst().getIdProfesion();
    }

    private CrearProyectoRequest proyectoConRequerimientoGral(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto de prueba UC-30", Privacidad.PUBLICO,
                LocalDate.now().plusDays(30), true,
                new UbicacionRequest("0602801002"),
                null,
                List.of(new CrearRequerimientoGralRequest(
                        1, idProfesion("modelo"), "Una modelo", null, null)),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private CrearProyectoRequest proyectoConActividad(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto de prueba UC-30 con actividad", Privacidad.PUBLICO,
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
                        "Prueba UC30", "Actividad de prueba UC-30", 2,
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
