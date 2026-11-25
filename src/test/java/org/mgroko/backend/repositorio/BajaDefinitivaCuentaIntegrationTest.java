package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.mensajeria.dto.ConversacionResponse;
import org.mgroko.backend.mensajeria.dto.MensajeResponse;
import org.mgroko.backend.mensajeria.exception.MensajeNoPermitidoException;
import org.mgroko.backend.mensajeria.servicio.MensajeriaService;
import org.mgroko.backend.modelo.Comentario;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.Conversacion;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MeGusta;
import org.mgroko.backend.modelo.MeGustaId;
import org.mgroko.backend.modelo.Mensaje;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoConexion;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.usuario.servicio.BajaDefinitivaService;
import org.mgroko.backend.usuario.servicio.ResumenBajaDefinitiva;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class BajaDefinitivaCuentaIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private BajaDefinitivaService bajaDefinitivaService;
    @Autowired
    private CrearPerfilService crearPerfilService;
    @Autowired
    private MensajeriaService mensajeriaService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private ProyectoRepository proyectoRepository;
    @Autowired
    private MiembroProyectoRepository miembroProyectoRepository;
    @Autowired
    private PublicacionRepository publicacionRepository;
    @Autowired
    private ComentarioRepository comentarioRepository;
    @Autowired
    private MeGustaRepository meGustaRepository;
    @Autowired
    private ConexionRepository conexionRepository;
    @Autowired
    private ConversacionRepository conversacionRepository;
    @Autowired
    private MensajeRepository mensajeRepository;
    @Autowired
    private SolicitudConexionRepository solicitudConexionRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private ProfesionRepository profesionRepository;
    @Autowired
    private RolProyectoRepository rolProyectoRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void expirarCuenta_ejecutaLaBajaDefinitivaCompleta() {
        int n = SECUENCIA.incrementAndGet();
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigoIgnoreCase("MUJER").orElseThrow();
        List<Profesion> profesiones = profesionRepository.findAll();
        assertThat(profesiones).hasSizeGreaterThanOrEqualTo(2);
        Profesion profesionA = profesiones.get(0);
        Profesion profesionB = profesiones.get(1);
        RolProyecto director = rolProyectoRepository.findByCodigo("DIRECTOR").orElseThrow();
        RolProyecto miembro = rolProyectoRepository.findByCodigo("MIEMBRO").orElseThrow();

        Usuario usuario = crearUsuario(rol, genero, n, "Ana", "Baja");
        Perfil perfilA = crearPerfil(usuario, profesionA, "Perfil A " + n);
        Perfil perfilB = crearPerfil(usuario, profesionB, "Perfil B " + n);

        Usuario usuarioC = crearUsuario(rol, genero, n + 1000, "Carla", "Tercero");
        Perfil perfilC = crearPerfil(usuarioC, profesionA, "Perfil C " + n);

        Usuario usuarioD = crearUsuario(rol, genero, n + 2000, "Dora", "Cuarto");
        Perfil perfilD = crearPerfil(usuarioD, profesionA, "Perfil D " + n);

        Proyecto aBorrador = crearProyecto("A Borrador " + n, EstadoProyecto.BORRADOR);
        Proyecto aPublicado = crearProyecto("A Publicado " + n, EstadoProyecto.PUBLICADO);
        Proyecto aConfirmado = crearProyecto("A Confirmado " + n, EstadoProyecto.CONFIRMADO);
        Proyecto aFinalizado = crearProyecto("A Finalizado " + n, EstadoProyecto.FINALIZADO);
        Proyecto aCancelado = crearProyecto("A Cancelado " + n, EstadoProyecto.CANCELADO);

        crearMiembro(aBorrador, perfilA, director);
        crearMiembro(aPublicado, perfilA, director);
        crearMiembro(aConfirmado, perfilA, director);
        crearMiembro(aFinalizado, perfilA, director);
        crearMiembro(aCancelado, perfilA, director);

        Proyecto bPublicado = crearProyecto("B Publicado " + n, EstadoProyecto.PUBLICADO);
        Proyecto bConfirmado = crearProyecto("B Confirmado " + n, EstadoProyecto.CONFIRMADO);
        Proyecto bFinalizado = crearProyecto("B Finalizado " + n, EstadoProyecto.FINALIZADO);
        Proyecto bCancelado = crearProyecto("B Cancelado " + n, EstadoProyecto.CANCELADO);

        crearMiembro(bPublicado, perfilC, director);
        crearMiembro(bPublicado, perfilB, miembro);
        crearMiembro(bConfirmado, perfilC, director);
        crearMiembro(bConfirmado, perfilB, miembro);
        crearMiembro(bFinalizado, perfilC, director);
        crearMiembro(bFinalizado, perfilB, miembro);
        crearMiembro(bCancelado, perfilC, director);
        crearMiembro(bCancelado, perfilB, miembro);

        Publicacion independiente = crearPublicacion(perfilA, "Independiente " + n);
        Publicacion colaborativaDeA = crearPublicacion(perfilA, "Colaborativa A " + n);
        colaborativaDeA.getColaboradores().add(perfilC);
        publicacionRepository.saveAndFlush(colaborativaDeA);

        Publicacion colaborativaDeC = crearPublicacion(perfilC, "Colaborativa C " + n);
        colaborativaDeC.getColaboradores().add(perfilA);
        publicacionRepository.saveAndFlush(colaborativaDeC);

        Comentario comentario = comentarioRepository.saveAndFlush(Comentario.builder()
                .texto("Comentario historico")
                .fechaComentario(LocalDateTime.now().minusDays(1))
                .publicacion(colaborativaDeC)
                .perfil(perfilA)
                .build());

        MeGustaId meGustaId = new MeGustaId(colaborativaDeC.getIdPublicacion(),
                perfilA.getIdPerfil());
        meGustaRepository.saveAndFlush(MeGusta.builder()
                .id(meGustaId)
                .fecha(LocalDateTime.now().minusHours(1))
                .build());

        SolicitudConexion aceptada = solicitudConexionRepository.saveAndFlush(
                SolicitudConexion.builder()
                        .perfilRemitente(perfilA)
                        .perfilDestinatario(perfilC)
                        .estado(EstadoSolicitud.PENDIENTE)
                        .fechaEnvio(LocalDateTime.now().minusDays(2))
                        .build());
        aceptada.setEstado(EstadoSolicitud.ACEPTADA);
        aceptada.setFechaRespuesta(LocalDateTime.now().minusDays(1));
        aceptada = solicitudConexionRepository.saveAndFlush(aceptada);

        Conexion conexion = conexionRepository.saveAndFlush(Conexion.builder()
                .perfil1(perfilA)
                .perfil2(perfilC)
                .solicitudOrigen(aceptada)
                .estado(EstadoConexion.ACTIVA)
                .fechaInicioConexion(LocalDateTime.now().minusDays(1))
                .build());

        Conversacion conversacion = conversacionRepository.saveAndFlush(
                Conversacion.builder()
                        .perfil1(perfilA)
                        .perfil2(perfilC)
                        .fechaCreacion(LocalDateTime.now().minusDays(1))
                        .fechaUltimoMensaje(LocalDateTime.now().minusHours(2))
                        .build());

        mensajeRepository.saveAndFlush(Mensaje.builder()
                .conversacion(conversacion)
                .perfilEmisor(perfilA)
                .contenido("Hola desde A")
                .fechaEnvio(LocalDateTime.now().minusHours(2))
                .leido(false)
                .build());
        mensajeRepository.saveAndFlush(Mensaje.builder()
                .conversacion(conversacion)
                .perfilEmisor(perfilC)
                .contenido("Respuesta desde C")
                .fechaEnvio(LocalDateTime.now().minusMinutes(90))
                .leido(false)
                .build());

        SolicitudConexion pendiente = solicitudConexionRepository.saveAndFlush(
                SolicitudConexion.builder()
                        .perfilRemitente(perfilA)
                        .perfilDestinatario(perfilD)
                        .estado(EstadoSolicitud.PENDIENTE)
                        .fechaEnvio(LocalDateTime.now().minusHours(3))
                        .build());

        usuario.setEstado(EstadoUsuario.PendienteBaja);
        usuario.setFechaSolicitudBaja(LocalDateTime.now().minusDays(31));
        usuarioRepository.saveAndFlush(usuario);
        perfilA.setEstado(EstadoPerfil.PendienteBaja);
        perfilA.setFechaSolicitudBaja(usuario.getFechaSolicitudBaja());
        perfilB.setEstado(EstadoPerfil.PendienteBaja);
        perfilB.setFechaSolicitudBaja(usuario.getFechaSolicitudBaja());
        perfilRepository.saveAllAndFlush(List.of(perfilA, perfilB));

        String correoOriginal = usuario.getCorreo();
        String dniOriginal = usuario.getDni();

        ResumenBajaDefinitiva resumen =
                bajaDefinitivaService.expirarCuentasVencidas(30);
        em.flush();
        em.clear();

        assertThat(resumen).isEqualTo(new ResumenBajaDefinitiva(1, 2, 3, 5, 1, 1));

        Usuario usuarioEliminado = usuarioRepository.findById(usuario.getIdUsuario())
                .orElseThrow();
        assertThat(usuarioEliminado.getEstado()).isEqualTo(EstadoUsuario.Baja);
        assertThat(usuarioEliminado.getNombre()).isEqualTo("Usuario");
        assertThat(usuarioEliminado.getApellido()).isEqualTo("Eliminado");
        assertThat(usuarioEliminado.getCorreo()).startsWith("eliminado+");
        assertThat(usuarioEliminado.getDni()).startsWith("9");
        assertThat(usuarioEliminado.getPasswordHash()).isNull();
        assertThat(usuarioEliminado.getUbicacion()).isNull();
        assertThat(usuarioRepository.findByCorreo(correoOriginal)).isEmpty();
        assertThat(usuarioRepository.existsByDni(dniOriginal)).isFalse();

        Perfil perfilATombstone = perfilRepository.findById(perfilA.getIdPerfil())
                .orElseThrow();
        assertThat(perfilATombstone.getEstado()).isEqualTo(EstadoPerfil.Baja);
        assertThat(perfilATombstone.getNombreArtistico()).isEqualTo(perfilA.getNombreArtistico());
        assertThat(perfilATombstone.getBiografia()).isEqualTo("Perfil eliminado");

        assertThat(proyectoRepository.findById(aBorrador.getIdProyecto())
                .orElseThrow().getEstado()).isEqualTo(EstadoProyecto.CANCELADO);
        assertThat(proyectoRepository.findById(aPublicado.getIdProyecto())
                .orElseThrow().getEstado()).isEqualTo(EstadoProyecto.CANCELADO);
        assertThat(proyectoRepository.findById(aConfirmado.getIdProyecto())
                .orElseThrow().getEstado()).isEqualTo(EstadoProyecto.CANCELADO);
        assertThat(proyectoRepository.findById(aFinalizado.getIdProyecto())
                .orElseThrow().getEstado()).isEqualTo(EstadoProyecto.FINALIZADO);
        assertThat(proyectoRepository.findById(aCancelado.getIdProyecto())
                .orElseThrow().getEstado()).isEqualTo(EstadoProyecto.CANCELADO);

        assertThat(estadoMembresia(aBorrador.getIdProyecto(), perfilA.getIdPerfil()))
                .isEqualTo(EstadoParticipacion.BAJA_VOLUNTARIA);
        assertThat(estadoMembresia(aPublicado.getIdProyecto(), perfilA.getIdPerfil()))
                .isEqualTo(EstadoParticipacion.BAJA_VOLUNTARIA);
        assertThat(estadoMembresia(aConfirmado.getIdProyecto(), perfilA.getIdPerfil()))
                .isEqualTo(EstadoParticipacion.BAJA_VOLUNTARIA);
        assertThat(estadoMembresia(aFinalizado.getIdProyecto(), perfilA.getIdPerfil()))
                .isEqualTo(EstadoParticipacion.ACTIVO);
        assertThat(estadoMembresia(aCancelado.getIdProyecto(), perfilA.getIdPerfil()))
                .isEqualTo(EstadoParticipacion.ACTIVO);

        assertThat(estadoMembresia(bPublicado.getIdProyecto(), perfilB.getIdPerfil()))
                .isEqualTo(EstadoParticipacion.BAJA_VOLUNTARIA);
        assertThat(estadoMembresia(bConfirmado.getIdProyecto(), perfilB.getIdPerfil()))
                .isEqualTo(EstadoParticipacion.BAJA_VOLUNTARIA);
        assertThat(estadoMembresia(bFinalizado.getIdProyecto(), perfilB.getIdPerfil()))
                .isEqualTo(EstadoParticipacion.ACTIVO);
        assertThat(estadoMembresia(bCancelado.getIdProyecto(), perfilB.getIdPerfil()))
                .isEqualTo(EstadoParticipacion.ACTIVO);

        assertThat(publicacionRepository.existsById(independiente.getIdPublicacion())).isFalse();
        assertThat(publicacionRepository.existsById(colaborativaDeA.getIdPublicacion())).isTrue();
        assertThat(publicacionRepository.existsById(colaborativaDeC.getIdPublicacion())).isTrue();

        assertThat(publicacionRepository
                .findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
                        perfilC.getIdPerfil(),
                        EstadoPublicacion.ACTIVA,
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent())
                .extracting(Publicacion::getIdPublicacion)
                .contains(colaborativaDeA.getIdPublicacion());

        Integer etiquetasA = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM colaborador_publicacion
                WHERE id_publicacion = ? AND id_perfil = ?
                """, Integer.class, colaborativaDeC.getIdPublicacion(), perfilA.getIdPerfil());
        assertThat(etiquetasA).isZero();

        Comentario comentarioPersistido = comentarioRepository.findById(comentario.getIdComentario())
                .orElseThrow();
        assertThat(comentarioPersistido.getNombreArtisticoAutor())
                .isEqualTo(perfilA.getNombreArtistico());

        assertThat(meGustaRepository.findById(meGustaId)).isEmpty();

        Conexion conexionEliminada = conexionRepository.findById(conexion.getIdConexion())
                .orElseThrow();
        assertThat(conexionEliminada.getEstado()).isEqualTo(EstadoConexion.ELIMINADA);
        assertThat(conexionEliminada.getFechaFinConexion()).isNotNull();
        assertThat(solicitudConexionRepository.findById(pendiente.getIdSolicitudConexion()))
                .isEmpty();

        assertThat(conversacionRepository.findById(conversacion.getIdConversacion())).isPresent();
        assertThat(mensajeRepository.buscarDeConversacion(
                conversacion.getIdConversacion(),
                org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(2);

        ConversacionResponse respuestaConversacion = mensajeriaService
                .obtenerConversacion(usuarioC.getIdUsuario(), perfilC.getIdPerfil(),
                        conversacion.getIdConversacion());
        assertThat(respuestaConversacion.puedeEnviar()).isFalse();

        List<MensajeResponse> mensajes = mensajeriaService
                .listarMensajes(usuarioC.getIdUsuario(), perfilC.getIdPerfil(),
                        conversacion.getIdConversacion(), 0, 10)
                .contenido();
        assertThat(mensajes).hasSize(2);

        assertThatThrownBy(() -> mensajeriaService.enviarMensaje(
                usuarioC.getIdUsuario(), perfilC.getIdPerfil(),
                conversacion.getIdConversacion(), "No deberia enviarse"))
                .isInstanceOf(MensajeNoPermitidoException.class);

        String operador = jdbcTemplate.queryForObject("""
                SELECT operador_nombre
                FROM auditoria
                WHERE entidad_afectada = 'usuario'
                  AND id_afectado = ?
                  AND tipo_evento = 'USUARIO_BAJA'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, String.class, usuario.getIdUsuario());
        assertThat(operador).isEqualTo("Ana Baja");

        assertThat(bajaDefinitivaService.expirarCuentasVencidas(30)
                .totalCuentasPerfiles()).isZero();
    }

    @Test
    void expirarPerfil_permiteRecrearOtroPerfilConLaMismaProfesion() {
        int n = SECUENCIA.incrementAndGet();
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigoIgnoreCase("MUJER").orElseThrow();
        Profesion profesion = profesionRepository.findAll().stream().findFirst().orElseThrow();

        Usuario usuario = crearUsuario(rol, genero, n + 3000, "Elena", "Perfil");
        Perfil perfil = crearPerfil(usuario, profesion, "Perfil viejo " + n);

        perfil.setEstado(EstadoPerfil.PendienteBaja);
        perfil.setFechaSolicitudBaja(LocalDateTime.now().minusDays(31));
        perfilRepository.saveAndFlush(perfil);

        bajaDefinitivaService.expirarPerfilesVencidos(30);
        em.flush();
        em.clear();

        assertThat(perfilRepository.findById(perfil.getIdPerfil()).orElseThrow().getEstado())
                .isEqualTo(EstadoPerfil.Baja);

        var nuevo = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest("Perfil nuevo " + n,
                        profesion.getIdProfesion(), "Biografia nueva", List.of()));

        assertThat(nuevo.idPerfil()).isNotNull();
        assertThat(perfilRepository.findById(nuevo.idPerfil()).orElseThrow().getEstado())
                .isEqualTo(EstadoPerfil.Activo);
    }

    private Usuario crearUsuario(
            RolGlobal rol,
            Genero genero,
            int n,
            String nombre,
            String apellido) {

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(String.format("%08d", 80000000 + n))
                .fechaNacimiento(LocalDate.now().minusYears(30))
                .correo("baja.def." + n + "@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .estado(EstadoUsuario.Activo)
                .build());
    }

    private Perfil crearPerfil(Usuario usuario, Profesion profesion, String nombreArtistico) {
        return perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Biografia de prueba")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build());
    }

    private Proyecto crearProyecto(String nombre, EstadoProyecto estado) {
        Proyecto.ProyectoBuilder builder = Proyecto.builder()
                .nombre(nombre)
                .descripcion("Proyecto de prueba")
                .estado(estado)
                .privacidad(Privacidad.Publico);

        if (estado == EstadoProyecto.CONFIRMADO) {
            builder.fechaInicio(LocalDateTime.now());
        }

        return proyectoRepository.saveAndFlush(builder.build());
    }

    private MiembroProyecto crearMiembro(
            Proyecto proyecto,
            Perfil perfil,
            RolProyecto rol) {

        return miembroProyectoRepository.saveAndFlush(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rol)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());
    }

    private Publicacion crearPublicacion(Perfil perfil, String titulo) {
        return publicacionRepository.saveAndFlush(Publicacion.builder()
                .titulo(titulo)
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .build());
    }

    private EstadoParticipacion estadoMembresia(Long idProyecto, Long idPerfil) {
        em.flush();
        em.clear();
        return miembroProyectoRepository
                .findFirstByProyectoIdProyectoAndPerfilIdPerfilOrderByFechaAltaDesc(
                        idProyecto, idPerfil)
                .orElseThrow()
                .getEstadoParticipacion();
    }
}
