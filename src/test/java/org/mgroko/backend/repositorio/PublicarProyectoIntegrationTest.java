package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.especificacion.ProyectoSpecifications;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.EstadoProyectoInvalidoException;
import org.mgroko.backend.proyectos.exception.InformacionBasicaIncompletaException;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.PublicarProyectoService;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración del UC-25 Publicar proyecto contra PostgreSQL real:
 * transición BORRADOR -> PUBLICADO, visibilidad resultante para no-miembros,
 * validación de información básica, autorización PUBLICAR_PROYECTO y la
 * auditoría que dispara el trigger trg_auditar_cambio_proyecto.
 */
@SpringBootTest
@Transactional
class PublicarProyectoIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private ProyectoRepository proyectoRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record PerfilCreado(Long idUsuario, Long idPerfil) {}

    private PerfilCreado crearPerfil(String correo, String dni, String nombreArtistico) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Publicar").apellido("Test").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(26))
                .correo(correo)
                .estado(org.mgroko.backend.modelo.enums.EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesionModelo = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();

        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesionModelo, "Perfil de prueba UC-25.", List.of()));

        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, id_tyc, descripcion, fecha_inicio) VALUES (?, 1, 'TyC UC-25', NOW())",
                perfil.idPerfil());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private Long idProfesionModelo() {
        return profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
    }

    private CrearProyectoRequest proyectoCompleto(String nombre, Privacidad privacidad) {
        return new CrearProyectoRequest(
                nombre, "Descripción completa del proyecto", privacidad,
                LocalDate.now().plusDays(30), true,
                new UbicacionRequest("0602801002"), // Burzaco, prov. Buenos Aires ("06"): regresión del bug de AdminSeeder
                null,
                List.of(new CrearRequerimientoGralRequest(2, idProfesionModelo(), "Dos modelos", null, null)),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private CrearProyectoRequest proyectoSinInfoBasica(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Sin ubicación, moodboard ni requerimientos", Privacidad.PUBLICO,
                null, false,
                null, null, null, null);
    }

    private CrearProyectoRequest proyectoCompletoSinRequerimientoGral(String nombre) {
        return new CrearProyectoRequest(
                nombre, "Proyecto completo sin requerimientos generales", Privacidad.PUBLICO,
                LocalDate.now().plusDays(30), true,
                new UbicacionRequest("0602801002"),
                null,
                List.of(),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private Long crearActividadConRequerimiento(PerfilCreado director, Long idProyecto, String nombre) {
        return crearActividadService.crear(
                director.idUsuario(), director.idPerfil(), idProyecto,
                new CrearActividadRequest(
                        nombre, "Actividad de prueba UC-25", 1, UnidadDuracion.DIAS,
                        null, null,
                        List.of(new CrearRequerimientoActividadRequest(
                                2, idProfesionModelo(), "Dos modelos", null, null))))
                .idActividad();
    }

    private List<String> nombresVisiblesPara(Long idPerfil, BuscarProyectosFiltro filtro) {
        Page<org.mgroko.backend.modelo.Proyecto> resultados = proyectoRepository.findAll(
                ProyectoSpecifications.conFiltros(filtro, idPerfil),
                PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "nombre")));
        return resultados.map(org.mgroko.backend.modelo.Proyecto::getNombre).getContent();
    }

    @Test
    void publicar_borradorCompletoPublico_cambiaEstadoYEsVisibleParaNoMiembros() {
        PerfilCreado director = crearPerfil("pub25.dir1@example.com", "77725001", "Director UC25");
        PerfilCreado ajeno = crearPerfil("pub25.aju1@example.com", "77725002", "Observador UC25");

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                proyectoCompleto("Desfile Publicable", Privacidad.PUBLICO));

        assertThat(borrador.estado()).isEqualTo("Borrador");
        assertThat(nombresVisiblesPara(ajeno.idPerfil(), null)).doesNotContain("Desfile Publicable");

        ProyectoResponse publicado = publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto());

        assertThat(publicado.estado()).isEqualTo("Publicado");
        assertThat(publicado.idDirector()).isEqualTo(director.idPerfil());
        assertThat(publicado.requerimientosGral()).hasSize(1);
        assertThat(publicado.moodboard()).isNotNull();

        org.mgroko.backend.modelo.Proyecto recargado = proyectoRepository
                .findById(borrador.idProyecto()).orElseThrow();
        assertThat(recargado.getEstado()).isEqualTo(EstadoProyecto.PUBLICADO);

        assertThat(nombresVisiblesPara(ajeno.idPerfil(), null)).contains("Desfile Publicable");

        Long auditorias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM detalle_auditoria da
                JOIN auditoria a ON a.id_auditoria = da.id_auditoria
                WHERE a.entidad_afectada = 'proyecto'
                  AND a.id_afectado = ?
                  AND da.campo = 'estado'
                  AND da.valor_anterior = 'BORRADOR'
                  AND da.valor_nuevo = 'PUBLICADO'
                """, Long.class, borrador.idProyecto());
        assertThat(auditorias).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void publicar_borradorCompletoPrivado_noEsVisibleParaNoMiembros() {
        PerfilCreado director = crearPerfil("pub25.dir2@example.com", "77725003", "Director Privado");
        PerfilCreado ajeno = crearPerfil("pub25.aju2@example.com", "77725004", "Observador Privado");

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                proyectoCompleto("Backstage Privado", Privacidad.PRIVADO));

        ProyectoResponse publicado = publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto());

        assertThat(publicado.estado()).isEqualTo("Publicado");
        assertThat(nombresVisiblesPara(ajeno.idPerfil(), null)).doesNotContain("Backstage Privado");
        assertThat(nombresVisiblesPara(director.idPerfil(), null)).contains("Backstage Privado");
    }

    @Test
    void publicar_informacionBasicaIncompleta_lanzaExcepcionYNoCambiaEstado() {
        PerfilCreado director = crearPerfil("pub25.dir3@example.com", "77725005", "Director Incompleto");

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                proyectoSinInfoBasica("Proyecto Incompleto"));

        Long idProyecto = borrador.idProyecto();

        assertThatThrownBy(() -> publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), idProyecto))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("ubicación")
                .hasMessageContaining("fecha de entrega")
                .hasMessageContaining("moodboard")
                .hasMessageContaining("al menos un requerimiento (general o de actividad)");

        assertThat(proyectoRepository.findById(idProyecto).orElseThrow().getEstado())
                .isEqualTo(EstadoProyecto.BORRADOR);
    }

    @Test
    void publicar_soloConRequerimientoDeActividad_publica() {
        PerfilCreado director = crearPerfil("pub25.dir7@example.com", "77725010", "Director Actividad");

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                proyectoCompletoSinRequerimientoGral("Solo Actividad"));

        crearActividadConRequerimiento(director, borrador.idProyecto(), "Prueba modelos");

        ProyectoResponse publicado = publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto());

        assertThat(publicado.estado()).isEqualTo("Publicado");
        assertThat(proyectoRepository.findById(borrador.idProyecto()).orElseThrow().getEstado())
                .isEqualTo(EstadoProyecto.PUBLICADO);
    }

    @Test
    void publicar_soloConRequerimientoDeActividadCancelado_lanzaExcepcionYNoCambiaEstado() {
        PerfilCreado director = crearPerfil("pub25.dir8@example.com", "77725011", "Director Cancelado");

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                proyectoCompletoSinRequerimientoGral("Actividad Cancelada"));

        Long idActividad = crearActividadConRequerimiento(
                director, borrador.idProyecto(), "Prueba cancelada");
        Actividad actividad = actividadRepository.findById(idActividad).orElseThrow();
        actividad.setEstado(EstadoActividad.CANCELADA);
        actividadRepository.saveAndFlush(actividad);

        assertThatThrownBy(() -> publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto()))
                .isInstanceOf(InformacionBasicaIncompletaException.class)
                .hasMessageContaining("al menos un requerimiento (general o de actividad)");

        assertThat(proyectoRepository.findById(borrador.idProyecto()).orElseThrow().getEstado())
                .isEqualTo(EstadoProyecto.BORRADOR);
    }

    @Test
    void publicar_comoPerfilSinPermiso_lanzaAccesoDenegado() {
        PerfilCreado director = crearPerfil("pub25.dir4@example.com", "77725006", "Director Dueño");
        PerfilCreado ajeno = crearPerfil("pub25.aju4@example.com", "77725007", "Intruso UC25");

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                proyectoCompleto("Desfile Reservado", Privacidad.PUBLICO));

        assertThatThrownBy(() -> publicarProyectoService.publicar(
                ajeno.idUsuario(), ajeno.idPerfil(), borrador.idProyecto()))
                .isInstanceOf(AccesoDenegadoProyectoException.class)
                .hasMessageContaining("PUBLICAR_PROYECTO");

        assertThat(proyectoRepository.findById(borrador.idProyecto()).orElseThrow().getEstado())
                .isEqualTo(EstadoProyecto.BORRADOR);
    }

    @Test
    void publicar_yaPublicado_lanzaEstadoInvalido() {
        PerfilCreado director = crearPerfil("pub25.dir5@example.com", "77725008", "Director Repetido");

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                proyectoCompleto("Desfile Doble", Privacidad.PUBLICO));

        publicarProyectoService.publicar(director.idUsuario(), director.idPerfil(), borrador.idProyecto());

        assertThatThrownBy(() -> publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto()))
                .isInstanceOf(EstadoProyectoInvalidoException.class)
                .hasMessageContaining("Publicado");
    }

    @Test
    void publicar_proyectoYaConfirmado_lanzaEstadoInvalido() {
        PerfilCreado director = crearPerfil("pub25.dir6@example.com", "77725009", "Director Confirmado");

        ProyectoResponse borrador = crearProyectoService.crear(
                director.idUsuario(), director.idPerfil(),
                proyectoCompleto("Desfile Confirmado", Privacidad.PUBLICO));

        var proyecto = proyectoRepository.findById(borrador.idProyecto()).orElseThrow();
        proyecto.setEstado(EstadoProyecto.CONFIRMADO);
        proyectoRepository.saveAndFlush(proyecto);

        assertThatThrownBy(() -> publicarProyectoService.publicar(
                director.idUsuario(), director.idPerfil(), borrador.idProyecto()))
                .isInstanceOf(EstadoProyectoInvalidoException.class)
                .hasMessageContaining("Confirmado");
    }
}
