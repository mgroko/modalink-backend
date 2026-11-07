package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.BuscarContratosFiltro;
import org.mgroko.backend.contratos.dto.ContratoBusquedaResponse;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.GenerarContratoRequest;
import org.mgroko.backend.contratos.exception.ContratoFiltroInvalidoException;
import org.mgroko.backend.contratos.servicio.BuscarContratoService;
import org.mgroko.backend.contratos.servicio.GenerarContratoService;
import org.mgroko.backend.contratos.servicio.ObtenerContratoService;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.ContratoVersion;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class BuscarContratoIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GenerarContratoService generarContratoService;

    @Autowired
    private BuscarContratoService buscarContratoService;

    @Autowired
    private ObtenerContratoService obtenerContratoService;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private ContratoVersionRepository contratoVersionRepository;

    @Autowired
    private FirmaContratoRepository firmaContratoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void directorVeElContratoYElMiembroSoloComoParte() {
        Datos datos = crearProyectoConContrato();

        PaginaResponse<ContratoBusquedaResponse> comoDirector =
                buscarContratoService.buscar(
                        datos.director().getIdUsuario(),
                        datos.perfilDirector().getIdPerfil(),
                        filtroVacio(),
                        0,
                        20);

        assertThat(comoDirector.totalElementos()).isEqualTo(1);
        ContratoBusquedaResponse fila = comoDirector.contenido().get(0);
        assertThat(fila.idContrato()).isEqualTo(datos.idContrato());
        assertThat(fila.estado()).isEqualTo("PENDIENTE_FIRMA");
        assertThat(fila.miEstadoFirma()).isEqualTo("PENDIENTE");
        assertThat(fila.puedeModificar()).isTrue();
        assertThat(fila.partes()).hasSize(2);

        PaginaResponse<ContratoBusquedaResponse> comoMiembro =
                buscarContratoService.buscar(
                        datos.miembro().getIdUsuario(),
                        datos.perfilMiembro().getIdPerfil(),
                        filtroVacio(),
                        0,
                        20);

        assertThat(comoMiembro.totalElementos()).isEqualTo(1);
        assertThat(comoMiembro.contenido().get(0).miEstadoFirma())
                .isEqualTo("PENDIENTE");
        assertThat(comoMiembro.contenido().get(0).puedeModificar()).isFalse();

        PaginaResponse<ContratoBusquedaResponse> comoSegundoDirector =
                buscarContratoService.buscar(
                        datos.segundoDirector().getIdUsuario(),
                        datos.perfilSegundoDirector().getIdPerfil(),
                        filtroVacio(),
                        0,
                        20);

        assertThat(comoSegundoDirector.totalElementos()).isEqualTo(1);
        assertThat(comoSegundoDirector.contenido().get(0).miEstadoFirma()).isNull();
        assertThat(comoSegundoDirector.contenido().get(0).puedeModificar()).isTrue();
    }

    @Test
    void miembroActivoNoFirmanteYUsuarioAjenoNoVenElContrato() {
        Datos datos = crearProyectoConContrato();

        PaginaResponse<ContratoBusquedaResponse> comoMiembroNoParte =
                buscarContratoService.buscar(
                        datos.miembroNoParte().getIdUsuario(),
                        datos.perfilMiembroNoParte().getIdPerfil(),
                        filtroVacio(),
                        0,
                        20);

        assertThat(comoMiembroNoParte.totalElementos()).isZero();

        PaginaResponse<ContratoBusquedaResponse> comoAjeno =
                buscarContratoService.buscar(
                        datos.ajeno().getIdUsuario(),
                        datos.perfilAjeno().getIdPerfil(),
                        filtroVacio(),
                        0,
                        20);

        assertThat(comoAjeno.totalElementos()).isZero();
    }

    @Test
    void filtrosPorProyectoParteEstadoFechasTipoYNombre() {
        Datos datos = crearProyectoConContrato();

        assertThat(buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        datos.proyecto().getIdProyecto(),
                        null, null, null, null, null, null, null, null))
                .totalElementos()).isEqualTo(1);

        assertThat(buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null, List.of(EstadoContrato.PENDIENTE_FIRMA),
                        datos.perfilMiembro().getNombreArtistico(),
                        null, null, null, null, null, null))
                .totalElementos()).isEqualTo(1);

        assertThat(buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null, null, "NO EXISTE",
                        null, null, null, null, null, null))
                .totalElementos()).isZero();

        assertThat(buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null, null, null,
                        LocalDate.now(), LocalDate.now(),
                        null, null, null, null))
                .totalElementos()).isEqualTo(1);

        assertThat(buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null, null, null,
                        LocalDate.now().minusDays(2), LocalDate.now().minusDays(1),
                        null, null, null, null))
                .totalElementos()).isZero();

        assertThat(buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null, null, null, null, null, null, null,
                        datos.tipo().getIdTipoAcuerdo(), null))
                .totalElementos()).isEqualTo(1);

        assertThat(buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null, null, null, null, null, null, null,
                        null, "NO EXISTE"))
                .totalElementos()).isZero();
    }

    @Test
    void rangoDeFechasInvertidoDevuelveErrorDeFiltro() {
        Datos datos = crearProyectoConContrato();

        assertThatThrownBy(() -> buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null, null, null,
                        LocalDate.now(), LocalDate.now().minusDays(1),
                        null, null, null, null)))
                .isInstanceOf(ContratoFiltroInvalidoException.class)
                .extracting("codigo")
                .isEqualTo("RANGO_FECHAS_CONTRATO_INVALIDO");
    }

    @Test
    void detallePermiteDirectorYPartePeroNoAjeno() {
        Datos datos = crearProyectoConContrato();

        ContratoDetalleResponse comoDirector = obtenerContratoService.obtener(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        assertThat(comoDirector.texto()).isNotBlank();
        assertThat(comoDirector.puedeModificarse()).isTrue();
        assertThat(comoDirector.partes()).hasSize(2);

        ContratoDetalleResponse comoMiembro = obtenerContratoService.obtener(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        assertThat(comoMiembro.puedeModificarse()).isFalse();
        assertThat(comoMiembro.miEstadoFirma()).isEqualTo("PENDIENTE");

        assertThatThrownBy(() -> obtenerContratoService.obtener(
                datos.ajeno().getIdUsuario(),
                datos.perfilAjeno().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato()))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void estadosPorDefectoExcluyenBorradorYSePuedenPedirExplicitamente() {
        Datos datos = crearProyectoConContrato();

        Contrato borrador = Contrato.builder()
                .proyecto(datos.proyecto())
                .tipoAcuerdo(datos.tipo())
                .miembroEmisor(datos.emisor())
                .nombre("Borrador UC68")
                .descripcion("Borrador")
                .texto("TEXTO BORRADOR")
                .versionActual(1)
                .fechaCreacion(java.time.LocalDateTime.now())
                .estado(EstadoContrato.BORRADOR)
                .build();
        contratoRepository.saveAndFlush(borrador);

        em.flush();
        em.clear();

        PaginaResponse<ContratoBusquedaResponse> porDefecto = buscar(
                datos.director(),
                datos.perfilDirector(),
                filtroVacio());

        assertThat(porDefecto.totalElementos()).isEqualTo(1);
        assertThat(porDefecto.contenido())
                .extracting(ContratoBusquedaResponse::estado)
                .doesNotContain("BORRADOR");

        PaginaResponse<ContratoBusquedaResponse> explicitos = buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null,
                        List.of(EstadoContrato.BORRADOR),
                        null, null, null, null, null, null, null));

        assertThat(explicitos.totalElementos()).isEqualTo(1);
        assertThat(explicitos.contenido().get(0).estado()).isEqualTo("BORRADOR");
    }

    @Test
    void contratoEliminadoSeExcluyePorDefectoYSePuedePedirExplicitamente() {
        Datos datos = crearProyectoConContrato();

        em.createNativeQuery("""
                UPDATE contrato
                   SET estado = 'ELIMINADO'
                 WHERE id_contrato = :idContrato
                """)
                .setParameter("idContrato", datos.idContrato())
                .executeUpdate();

        em.flush();
        em.clear();

        assertThat(buscar(
                datos.director(),
                datos.perfilDirector(),
                filtroVacio()).totalElementos()).isZero();

        assertThat(buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null,
                        List.of(EstadoContrato.ELIMINADO),
                        null, null, null, null, null, null, null))
                .totalElementos()).isEqualTo(1);
    }

    @Test
    void filtroPorFechaDeFirmaEncuentraContratosFirmados() {
        Datos datos = crearProyectoConContrato();
        activarContrato(datos);

        PaginaResponse<ContratoBusquedaResponse> hoy = buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null, null, null, null, null,
                        LocalDate.now(), LocalDate.now(), null, null));

        assertThat(hoy.totalElementos()).isEqualTo(1);
        assertThat(hoy.contenido().get(0).estado()).isEqualTo("ACTIVO");
        assertThat(hoy.contenido().get(0).puedeModificar()).isFalse();

        PaginaResponse<ContratoBusquedaResponse> pasado = buscar(
                datos.director(),
                datos.perfilDirector(),
                new BuscarContratosFiltro(
                        null, null, null, null, null,
                        LocalDate.now().minusDays(2),
                        LocalDate.now().minusDays(1),
                        null, null));

        assertThat(pasado.totalElementos()).isZero();
    }

    @Test
    void contratoConFirmaParcial_noEsModificable() {
        Datos datos = crearProyectoConContrato();
        firmarDestinatario(datos);

        PaginaResponse<ContratoBusquedaResponse> pagina = buscar(
                datos.director(),
                datos.perfilDirector(),
                filtroVacio());

        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.contenido().get(0).estado()).isEqualTo("PENDIENTE_FIRMA");
        assertThat(pagina.contenido().get(0).puedeModificar()).isFalse();
    }

    private void firmarDestinatario(Datos datos) {
        ContratoVersion version = contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(datos.idContrato(), 1)
                .orElseThrow();

        FirmaContrato destinatario = firmaContratoRepository
                .findByIdContratoOrderByIdMiembroAsc(datos.idContrato())
                .stream()
                .filter(f -> f.getIdMiembro().equals(datos.miembroProyecto().getIdMiembro()))
                .findFirst()
                .orElseThrow();

        em.createNativeQuery("""
                UPDATE firma_contrato
                   SET estado_firma = 'FIRMADO',
                       id_contrato_version = :idVersion
                 WHERE id_contrato = :idContrato
                   AND id_miembro = :idMiembro
                """)
                .setParameter("idVersion", version.getIdContratoVersion())
                .setParameter("idContrato", datos.idContrato())
                .setParameter("idMiembro", destinatario.getIdMiembro())
                .executeUpdate();

        em.flush();
        em.clear();
    }

    private void activarContrato(Datos datos) {
        ContratoVersion version = contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(datos.idContrato(), 1)
                .orElseThrow();

        List<FirmaContrato> firmas = firmaContratoRepository
                .findByIdContratoOrderByIdMiembroAsc(datos.idContrato());

        for (FirmaContrato firma : firmas) {
            em.createNativeQuery("""
                    UPDATE firma_contrato
                       SET estado_firma = 'FIRMADO',
                           id_contrato_version = :idVersion
                     WHERE id_contrato = :idContrato
                       AND id_miembro = :idMiembro
                    """)
                    .setParameter("idVersion", version.getIdContratoVersion())
                    .setParameter("idContrato", datos.idContrato())
                    .setParameter("idMiembro", firma.getIdMiembro())
                    .executeUpdate();
        }

        em.flush();
        em.clear();
    }

    private PaginaResponse<ContratoBusquedaResponse> buscar(
            Usuario usuario,
            Perfil perfil,
            BuscarContratosFiltro filtro) {
        return buscarContratoService.buscar(
                usuario.getIdUsuario(),
                perfil.getIdPerfil(),
                filtro,
                0,
                20);
    }

    private BuscarContratosFiltro filtroVacio() {
        return new BuscarContratosFiltro(
                null, null, null, null, null, null, null, null, null);
    }

    private Datos crearProyectoConContrato() {
        long sufijo = System.nanoTime();

        Profesion profesion = Profesion.builder()
                .codigo("UC68_" + sufijo)
                .nombre("Profesional UC68 " + sufijo)
                .build();
        em.persist(profesion);

        long baseDni = Math.abs(sufijo % 100_000_000L);
        Usuario director = guardarUsuario("uc68.dir." + sufijo + "@example.com",
                String.format("%08d", baseDni));
        Usuario miembro = guardarUsuario("uc68.miem." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 1) % 100_000_000L));
        Usuario segundoDirector = guardarUsuario("uc68.dir2." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 2) % 100_000_000L));
        Usuario miembroNoParte = guardarUsuario("uc68.miem2." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 3) % 100_000_000L));
        Usuario ajeno = guardarUsuario("uc68.ajeno." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 4) % 100_000_000L));

        Perfil perfilDirector = perfil("Directora UC68 " + sufijo, director, profesion);
        Perfil perfilMiembro = perfil("Miembro UC68 " + sufijo, miembro, profesion);
        Perfil perfilSegundoDirector = perfil("Director2 UC68 " + sufijo, segundoDirector, profesion);
        Perfil perfilMiembroNoParte = perfil("Miembro2 UC68 " + sufijo, miembroNoParte, profesion);
        Perfil perfilAjeno = perfil("Ajeno UC68 " + sufijo, ajeno, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC68 " + sufijo)
                .descripcion("Proyecto de integración UC-68")
                .estado(EstadoProyecto.PUBLICADO)
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

        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilSegundoDirector)
                .rolProyecto(rol("DIRECTOR"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());

        MiembroProyecto miembroProyecto = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilMiembro)
                .rolProyecto(rol("MIEMBRO"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        em.persist(miembroProyecto);

        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilMiembroNoParte)
                .rolProyecto(rol("MIEMBRO"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());

        TipoAcuerdoContrato tipo = TipoAcuerdoContrato.builder()
                .codigo("UC68_TIPO_" + sufijo)
                .nombre("Cesión de derechos de imagen")
                .descripcion("Tipo de integración UC-68")
                .plantilla("""
                        CONTRATO DE {{tipo}}

                        Proyecto: {{proyecto}}
                        Director emisor: {{director}}
                        Partes:
                        {{partes}}

                        {{clausulas_adicionales}}
                        """)
                .activo(true)
                .build();
        em.persist(tipo);

        em.flush();

        GenerarContratoRequest request = new GenerarContratoRequest(
                tipo.getIdTipoAcuerdo(),
                List.of(miembroProyecto.getIdMiembro()),
                List.of("Cláusula UC-68"));

        ContratoGeneradoResponse response = generarContratoService.generar(
                director.getIdUsuario(),
                perfilDirector.getIdPerfil(),
                proyecto.getIdProyecto(),
                request);

        em.flush();
        em.clear();

        return new Datos(
                proyecto,
                tipo,
                director,
                miembro,
                segundoDirector,
                miembroNoParte,
                ajeno,
                perfilDirector,
                perfilMiembro,
                perfilSegundoDirector,
                perfilMiembroNoParte,
                perfilAjeno,
                emisor,
                miembroProyecto,
                response.idContrato());
    }

    private Usuario guardarUsuario(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana")
                .apellido("Gómez")
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
                .biografia("Perfil de integración UC-68")
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
            Usuario segundoDirector,
            Usuario miembroNoParte,
            Usuario ajeno,
            Perfil perfilDirector,
            Perfil perfilMiembro,
            Perfil perfilSegundoDirector,
            Perfil perfilMiembroNoParte,
            Perfil perfilAjeno,
            MiembroProyecto emisor,
            MiembroProyecto miembroProyecto,
            Long idContrato) {
    }
}
