package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.postulaciones.dto.MiPostulacionResponse;
import org.mgroko.backend.postulaciones.dto.MisPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.PostulacionResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.PostulacionNoEncontradaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.postulaciones.servicio.AceptarPostulacionService;
import org.mgroko.backend.postulaciones.servicio.BuscarMisPostulacionesService;
import org.mgroko.backend.postulaciones.servicio.CrearPostulacionService;
import org.mgroko.backend.postulaciones.servicio.DarDeBajaPostulacionService;
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
 * Integración de UC-27 contra PostgreSQL real: baja física de postulaciones
 * generales y de actividad, precondición de estado pendiente, pertenencia al
 * perfil activo y listado "mis postulaciones".
 */
@SpringBootTest
@Transactional
class BajaPostulacionIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private CrearPostulacionService crearPostulacionService;
    @Autowired
    private DarDeBajaPostulacionService darDeBajaPostulacionService;
    @Autowired
    private BuscarMisPostulacionesService buscarMisPostulacionesService;
    @Autowired
    private AceptarPostulacionService aceptarPostulacionService;
    @Autowired
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record PerfilCreado(Long idUsuario, Long idPerfil) {}

    @Test
    void bajaGeneral_pendiente_eliminaFilaYPermiteVolverAPostular() {
        PerfilCreado director = crearPerfil(
                "uc27.dir1@example.com", "88827001", "Directora UC27", "modelo");
        PerfilCreado postulante = crearPerfil(
                "uc27.pos1@example.com", "88827002", "Postulante UC27", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(director, "Desfile UC27");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        darDeBajaPostulacionService.darDeBajaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion());

        Long filas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM postulacion_gral WHERE id_postulacion_gral = ?
                """, Long.class, postulacion.idPostulacion());
        assertThat(filas).isZero();

        PostulacionResponse nueva = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);
        assertThat(nueva.idPostulacion()).isNotNull();
        assertThat(nueva.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void bajaActividad_pendiente_eliminaFila() {
        PerfilCreado director = crearPerfil(
                "uc27.dir2@example.com", "88827003", "Directora Actividad UC27", "modelo");
        PerfilCreado postulante = crearPerfil(
                "uc27.pos2@example.com", "88827004", "Postulante Actividad UC27", "modelo");

        ProyectoResponse publicado = publicarProyectoConActividad(director, "Desfile UC27 Actividad");
        Long idRequerimiento = requerimientoDeActividad(publicado);

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionActividad(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        darDeBajaPostulacionService.darDeBajaPostulacionActividad(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion());

        Long filas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM postulacion_actividad WHERE id_postulacion = ?
                """, Long.class, postulacion.idPostulacion());
        assertThat(filas).isZero();
    }

    @Test
    void bajaGeneral_aceptada_lanzaYaGestionadaYConservaFila() {
        PerfilCreado director = crearPerfil(
                "uc27.dir3@example.com", "88827005", "Directora Acepta UC27", "modelo");
        PerfilCreado postulante = crearPerfil(
                "uc27.pos3@example.com", "88827006", "Postulante Acepta UC27", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC27 Aceptada");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        aceptarPostulacionService.aceptarGeneral(
                director.idUsuario(), director.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion());

        assertThatThrownBy(() -> darDeBajaPostulacionService.darDeBajaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion()))
                .isInstanceOf(PostulacionYaGestionadaException.class);

        Long filas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM postulacion_gral WHERE id_postulacion_gral = ?
                """, Long.class, postulacion.idPostulacion());
        assertThat(filas).isEqualTo(1L);
    }

    @Test
    void bajaGeneral_deOtroPerfil_lanzaNoEncontradaYConservaFila() {
        PerfilCreado director = crearPerfil(
                "uc27.dir4@example.com", "88827007", "Directora Ajena UC27", "modelo");
        PerfilCreado postulante = crearPerfil(
                "uc27.pos4@example.com", "88827008", "Postulante Ajena UC27", "modelo");
        PerfilCreado otroPerfil = crearPerfil(
                "uc27.otro4@example.com", "88827009", "Otro Perfil UC27", "modelo");

        ProyectoResponse publicado = publicarProyectoConRequerimientoGral(
                director, "Desfile UC27 Ajena");
        Long idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                publicado.idProyecto(), idRequerimiento);

        assertThatThrownBy(() -> darDeBajaPostulacionService.darDeBajaPostulacionGeneral(
                otroPerfil.idUsuario(), otroPerfil.idPerfil(),
                publicado.idProyecto(), postulacion.idPostulacion()))
                .isInstanceOf(PostulacionNoEncontradaException.class);

        Long filas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM postulacion_gral WHERE id_postulacion_gral = ?
                """, Long.class, postulacion.idPostulacion());
        assertThat(filas).isEqualTo(1L);
    }

    @Test
    void misPostulaciones_devuelveGeneralYActividadDelPerfil() {
        PerfilCreado director = crearPerfil(
                "uc27.dir5@example.com", "88827010", "Directora Listado UC27", "modelo");
        PerfilCreado postulante = crearPerfil(
                "uc27.pos5@example.com", "88827011", "Postulante Listado UC27", "modelo");

        ProyectoResponse conGeneral = publicarProyectoConRequerimientoGral(
                director, "Desfile UC27 Listado");
        Long idRequerimientoGral = conGeneral.requerimientosGral().get(0).idRequerimientoGral();
        crearPostulacionService.darDeAltaPostulacionGeneral(
                postulante.idUsuario(), postulante.idPerfil(),
                conGeneral.idProyecto(), idRequerimientoGral);

        ProyectoResponse conActividad = publicarProyectoConActividad(
                director, "Desfile UC27 Listado Actividad");
        Long idRequerimientoActividad = requerimientoDeActividad(conActividad);
        crearPostulacionService.darDeAltaPostulacionActividad(
                postulante.idUsuario(), postulante.idPerfil(),
                conActividad.idProyecto(), idRequerimientoActividad);

        PaginaResponse<MiPostulacionResponse> pagina = buscarMisPostulacionesService.buscar(
                postulante.idPerfil(),
                new MisPostulacionesFiltro(null, EstadoSolicitud.PENDIENTE, null),
                0, 20, false);

        assertThat(pagina.contenido()).hasSize(2);
        assertThat(pagina.contenido())
                .extracting(MiPostulacionResponse::tipo)
                .containsExactlyInAnyOrder(TipoPostulacion.GENERAL, TipoPostulacion.ACTIVIDAD);
        assertThat(pagina.contenido())
                .allSatisfy(postulacion -> {
                    assertThat(postulacion.puedeDarseDeBaja()).isTrue();
                    assertThat(postulacion.nombreProyecto()).startsWith("Desfile UC27 Listado");
                });
    }

    // ---------------------------------------------------------------
    // Datos de prueba
    // ---------------------------------------------------------------

    private PerfilCreado crearPerfil(
            String correo,
            String dni,
            String nombreArtistico,
            String codigoProfesion) {

        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC27").apellido("Test").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(27))
                .correo(correo)
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase(codigoProfesion)
                .map(Profesion::getIdProfesion)
                .orElseGet(() -> profesionRepository.buscar("%" + codigoProfesion + "%", codigoProfesion)
                        .get(0).getIdProfesion());

        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesion,
                        "Perfil de prueba UC-27.", List.of()));

        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, id_tyc, descripcion, fecha_inicio) "
                        + "VALUES (?, 1, 'TyC UC-27', NOW())",
                perfil.idPerfil());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private Long idProfesion(String codigo) {
        return profesionRepository.buscar("%" + codigo + "%", codigo).get(0).getIdProfesion();
    }

    private CrearProyectoRequest proyectoConRequerimientoGral(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto de prueba UC-27", Privacidad.PUBLICO,
                LocalDate.now().plusDays(30), true,
                new UbicacionRequest("0602801002"),
                null,
                List.of(new CrearRequerimientoGralRequest(
                        1, idProfesion("modelo"), "Una modelo", null, null)),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private CrearProyectoRequest proyectoConActividad(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto de prueba UC-27 con actividad", Privacidad.PUBLICO,
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
                        "Prueba UC27", "Actividad de prueba UC-27", 2, UnidadDuracion.HORAS,
                        null, null,
                        List.of(new CrearRequerimientoActividadRequest(
                                1, idProfesion("modelo"), "Una modelo", null, null))));

        assertThat(actividad.idActividad()).isNotNull();

        return publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto());
    }

    private Long requerimientoDeActividad(ProyectoResponse publicado) {
        Long idActividad = jdbcTemplate.queryForObject("""
                SELECT a.id_actividad FROM actividad a
                JOIN planificacion p ON p.id_planificacion = a.id_planificacion
                WHERE p.id_proyecto = ?
                """, Long.class, publicado.idProyecto());

        return requerimientoActividadRepository
                .findByActividad_IdActividad(idActividad)
                .get(0)
                .getIdRequerimiento();
    }
}
