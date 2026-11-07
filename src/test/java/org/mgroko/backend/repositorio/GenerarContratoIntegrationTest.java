package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.GenerarContratoRequest;
import org.mgroko.backend.contratos.dto.OpcionesGeneracionContratoResponse;
import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.contratos.servicio.GenerarContratoService;
import org.mgroko.backend.contratos.servicio.ObtenerOpcionesContratoService;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.ContratoEvento;
import org.mgroko.backend.modelo.ContratoVersion;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.modelo.enums.RolParteContrato;
import org.mgroko.backend.modelo.enums.TipoEventoContrato;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integracion de UC-39 contra PostgreSQL real.
 *
 * <p>Verifica la secuencia BORRADOR -> firmas -> PENDIENTE_FIRMA, la version 1,
 * los eventos con id_contrato_version correcto, las firmas de las partes y las
 * notificaciones con id_proyecto.</p>
 */
@SpringBootTest
@Transactional
class GenerarContratoIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GenerarContratoService generarContratoService;

    @Autowired
    private ObtenerOpcionesContratoService obtenerOpcionesContratoService;

    @Autowired
    private ContratoVersionRepository contratoVersionRepository;

    @Autowired
    private ContratoEventoRepository contratoEventoRepository;

    @Autowired
    private FirmaContratoRepository firmaContratoRepository;

    @Autowired
    private NotificacionDestinoResolver notificacionDestinoResolver;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void generar_persisteContratoVersionFirmasEventosYNotificaciones() {
        Datos datos = crearProyecto(EstadoProyecto.PUBLICADO);

        OpcionesGeneracionContratoResponse opciones = obtenerOpcionesContratoService.obtener(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto());

        assertThat(opciones.proyecto().idProyecto())
                .isEqualTo(datos.proyecto().getIdProyecto());
        assertThat(opciones.tiposAcuerdo()).isNotEmpty();
        assertThat(opciones.miembros()).hasSize(2);

        GenerarContratoRequest request = new GenerarContratoRequest(
                datos.tipo().getIdTipoAcuerdo(),
                List.of(datos.miembroProyecto().getIdMiembro()),
                List.of("La parte cedente conserva el derecho de aprobar usos adicionales."));

        ContratoGeneradoResponse response = generarContratoService.generar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                request);

        em.flush();
        em.clear();

        assertThat(response.idContrato()).isNotNull();
        assertThat(response.estado()).isEqualTo("PENDIENTE_FIRMA");
        assertThat(response.versionActual()).isEqualTo(1);
        assertThat(response.texto())
                .contains(datos.proyecto().getNombre())
                .contains(datos.perfilDirector().getNombreArtistico())
                .contains(datos.perfilMiembro().getNombreArtistico())
                .contains("La parte cedente conserva el derecho");

        Contrato contrato = em.find(Contrato.class, response.idContrato());
        assertThat(contrato.getEstado()).isEqualTo(EstadoContrato.PENDIENTE_FIRMA);
        assertThat(contrato.getFechaEmision()).isNotNull();
        assertThat(contrato.getVersionActual()).isEqualTo(1);

        ContratoVersion version = contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(contrato.getIdContrato(), 1)
                .orElseThrow();

        List<FirmaContrato> firmas = firmaContratoRepository
                .findByIdContratoOrderByIdMiembroAsc(contrato.getIdContrato());

        assertThat(firmas).hasSize(2);
        assertThat(firmas)
                .extracting(FirmaContrato::getRolParte)
                .containsExactlyInAnyOrder(
                        RolParteContrato.DIRECTOR,
                        RolParteContrato.DESTINATARIO);
        assertThat(firmas)
                .allMatch(f -> f.getEstadoFirma() == EstadoFirmaContrato.PENDIENTE);

        List<ContratoEvento> eventos = contratoEventoRepository
                .findByContrato_IdContratoOrderByIdEventoAsc(contrato.getIdContrato());

        assertThat(eventos)
                .extracting(ContratoEvento::getTipoEvento)
                .containsExactly(TipoEventoContrato.CREADO, TipoEventoContrato.EMITIDO);
        assertThat(eventos)
                .allMatch(e -> e.getContratoVersion() != null
                        && version.getIdContratoVersion()
                                .equals(e.getContratoVersion().getIdContratoVersion()));

        List<Notificacion> notificaciones = em.createQuery("""
                        SELECT n FROM Notificacion n
                        WHERE n.entidadTipo = 'CONTRATO'
                          AND n.entidadId = :idContrato
                        ORDER BY n.idNotificacion
                        """, Notificacion.class)
                .setParameter("idContrato", contrato.getIdContrato())
                .getResultList();

        assertThat(notificaciones).hasSize(2);
        assertThat(notificaciones)
                .allMatch(n -> NotificacionService.TIPO_CONTRATO_PENDIENTE_FIRMA
                        .equals(n.getTipo()));
        assertThat(notificaciones)
                .allMatch(n -> datos.proyecto().getIdProyecto().equals(n.getIdProyecto()));
        assertThat(notificaciones)
                .allMatch(n -> "TRIGGER".equals(n.getOrigen()));
        assertThat(notificacionDestinoResolver.resolverProyecto(notificaciones.get(0)))
                .isEqualTo(datos.proyecto().getIdProyecto());

        // Fallback del resolver para notificaciones historicas sin id_proyecto.
        Notificacion sinProyecto = notificaciones.get(0);
        sinProyecto.setIdProyecto(null);
        em.flush();
        em.clear();

        assertThat(notificacionDestinoResolver.resolverProyecto(sinProyecto))
                .isEqualTo(datos.proyecto().getIdProyecto());
    }

    @Test
    void generar_conDirectorComoDestinatario_loMarcaDirectorDestinatario() {
        Datos datos = crearProyecto(EstadoProyecto.PUBLICADO);

        GenerarContratoRequest request = new GenerarContratoRequest(
                datos.tipo().getIdTipoAcuerdo(),
                List.of(datos.emisor().getIdMiembro()),
                List.of());

        ContratoGeneradoResponse response = generarContratoService.generar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                request);

        em.flush();
        em.clear();

        List<FirmaContrato> firmas = firmaContratoRepository
                .findByIdContratoOrderByIdMiembroAsc(response.idContrato());

        assertThat(firmas).hasSize(1);
        assertThat(firmas.get(0).getRolParte())
                .isEqualTo(RolParteContrato.DIRECTOR_DESTINATARIO);
    }

    @Test
    void generar_conContratoActivoDelMismoTipoYDestinatario_devuelveDuplicado() {
        Datos datos = crearProyecto(EstadoProyecto.PUBLICADO);

        GenerarContratoRequest request = new GenerarContratoRequest(
                datos.tipo().getIdTipoAcuerdo(),
                List.of(datos.miembroProyecto().getIdMiembro()),
                List.of());

        ContratoGeneradoResponse primero = generarContratoService.generar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                request);

        ContratoVersion version = contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(primero.idContrato(), 1)
                .orElseThrow();

        List<FirmaContrato> firmas = firmaContratoRepository
                .findByIdContratoOrderByIdMiembroAsc(primero.idContrato());

        for (FirmaContrato firma : firmas) {
            em.createNativeQuery("""
                    UPDATE firma_contrato
                       SET estado_firma = 'FIRMADO',
                           id_contrato_version = :idVersion
                     WHERE id_contrato = :idContrato
                       AND id_miembro = :idMiembro
                    """)
                    .setParameter("idVersion", version.getIdContratoVersion())
                    .setParameter("idContrato", primero.idContrato())
                    .setParameter("idMiembro", firma.getIdMiembro())
                    .executeUpdate();
        }

        em.flush();
        em.clear();

        Contrato activo = em.find(Contrato.class, primero.idContrato());
        assertThat(activo.getEstado()).isEqualTo(EstadoContrato.ACTIVO);

        OpcionesGeneracionContratoResponse opciones = obtenerOpcionesContratoService.obtener(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto());

        assertThat(opciones.contratosActivos())
                .anyMatch(a -> primero.idContrato().equals(a.idContrato()));

        assertThatThrownBy(() -> generarContratoService.generar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                request))
                .isInstanceOf(ContratoNoGenerableException.class)
                .extracting("codigo")
                .isEqualTo("CONTRATO_DUPLICADO_ACTIVO");
    }

    @Test
    void generar_conProyectoNoApto_devuelveErrorDeNegocio() {
        Datos datos = crearProyecto(EstadoProyecto.BORRADOR);

        GenerarContratoRequest request = new GenerarContratoRequest(
                datos.tipo().getIdTipoAcuerdo(),
                List.of(datos.miembroProyecto().getIdMiembro()),
                List.of());

        assertThatThrownBy(() -> generarContratoService.generar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                request))
                .isInstanceOf(ContratoNoGenerableException.class)
                .extracting("codigo")
                .isEqualTo("PROYECTO_NO_APTO_PARA_CONTRATO");
    }

    @Test
    void generar_conDestinatarioAjeno_devuelveErrorDeNegocio() {
        Datos datos = crearProyecto(EstadoProyecto.PUBLICADO);

        GenerarContratoRequest request = new GenerarContratoRequest(
                datos.tipo().getIdTipoAcuerdo(),
                List.of(999_999L),
                List.of());

        assertThatThrownBy(() -> generarContratoService.generar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                request))
                .isInstanceOf(ContratoNoGenerableException.class)
                .extracting("codigo")
                .isEqualTo("DESTINATARIO_INVALIDO");
    }

    private Datos crearProyecto(EstadoProyecto estado) {
        long sufijo = System.nanoTime();

        Profesion profesion = Profesion.builder()
                .codigo("UC39_" + sufijo)
                .nombre("Profesional UC39 " + sufijo)
                .build();
        em.persist(profesion);

        long baseDni = Math.abs(sufijo % 100_000_000L);
        Usuario director = guardarUsuario("uc39.dir." + sufijo + "@example.com",
                String.format("%08d", baseDni));
        Usuario miembro = guardarUsuario("uc39.miem." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 1) % 100_000_000L));

        Perfil perfilDirector = perfil("Directora UC39 " + sufijo, director, profesion);
        Perfil perfilMiembro = perfil("Miembro UC39 " + sufijo, miembro, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC39 " + sufijo)
                .descripcion("Proyecto de integraciÃ³n UC-39")
                .estado(estado)
                .privacidad(Privacidad.PUBLICO)
                .build();
        em.persist(proyecto);

        MiembroProyecto emisor = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilDirector)
                .rolProyecto(rol("DIRECTOR"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        em.persist(emisor);

        MiembroProyecto miembroProyecto = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilMiembro)
                .rolProyecto(rol("MIEMBRO"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        em.persist(miembroProyecto);

        TipoAcuerdoContrato tipo = TipoAcuerdoContrato.builder()
                .codigo("UC39_TIPO_" + sufijo)
                .nombre("CesiÃ³n de derechos de imagen")
                .descripcion("Tipo de integraciÃ³n UC-39")
                .plantilla("""
                        CONTRATO DE {{tipo}}

                        Proyecto: {{proyecto}}
                        Director emisor: {{director}}
                        Partes:
                        {{partes}}

                        Las partes acuerdan las condiciones del presente contrato.

                        {{clausulas_adicionales}}
                        """)
                .activo(true)
                .build();
        em.persist(tipo);

        em.flush();

        return new Datos(
                proyecto,
                tipo,
                director,
                miembro,
                perfilDirector,
                perfilMiembro,
                emisor,
                miembroProyecto);
    }

    private Usuario guardarUsuario(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana")
                .apellido("GÃ³mez")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }

    private Perfil perfil(String nombre, Usuario usuario, Profesion profesion) {
        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombre)
                .biografia("Perfil de integraciÃ³n UC-39")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private RolProyecto rol(String codigo) {
        return em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = :codigo",
                RolProyecto.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
    }

    private record Datos(
            Proyecto proyecto,
            TipoAcuerdoContrato tipo,
            Usuario director,
            Usuario miembro,
            Perfil perfilDirector,
            Perfil perfilMiembro,
            MiembroProyecto emisor,
            MiembroProyecto miembroProyecto) {
    }
}


