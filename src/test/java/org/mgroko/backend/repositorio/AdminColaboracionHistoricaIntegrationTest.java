package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.PreviewProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.PrevisualizarColaboracionesHistoricasRequest;
import org.mgroko.backend.admin.dto.ProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.servicio.AdminColaboracionHistoricaService;
import org.mgroko.backend.admin.servicio.ProcesamientoHistoricoBatchService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistorica;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración del procesamiento histórico de colaboraciones automáticas.
 *
 * <p>Los contadores de {@code previsualizar} y la paginación por clave de
 * {@code siguienteLote} se apoyan en SQL nativo que ningún mock ejercita. Esta
 * clase siembra publicaciones e imágenes reales en PostgreSQL y verifica los
 * valores exactos, incluido el umbral de distancia de pHash.</p>
 *
 * <p>Reemplaza a {@code AdminColaboracionHistoricaLocalIntegrationTest}, que
 * dependía de una base local con datos y quedaba deshabilitado por defecto.</p>
 */
@SpringBootTest
@Transactional
class AdminColaboracionHistoricaIntegrationTest extends AbstractPostgresIntegrationTest {

    /**
     * Hashes separados por un solo bit: la distancia es 1, así que con umbral 0
     * no deben formar par y con umbral 1 sí.
     */
    private static final long PHASH_UNO = 0x1234567890ABCDEFL;
    private static final long PHASH_DOS = PHASH_UNO & ~1L;

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private ProfesionRepository profesionRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private ImagenRepository imagenRepository;
    @Autowired
    private PublicacionRepository publicacionRepository;
    @Autowired
    private ProcesamientoColaboracionHistoricaRepository procesamientoRepository;
    @Autowired
    private CrearPerfilService crearPerfilService;
    @Autowired
    private AdminColaboracionHistoricaService service;
    @Autowired
    private ProcesamientoHistoricoBatchService batchService;

    @Test
    void previsualizar_sinFiltros_sumaPublicacionesEImagenesSembradas() {
        PreviewProcesamientoHistoricoResponse antes = service.previsualizar(sinFiltros());

        Sembrado sembrado = sembrar();

        PreviewProcesamientoHistoricoResponse despues = service.previsualizar(sinFiltros());

        assertThat(sembrado.idPublicacionUno()).isNotNull();
        assertThat(despues.publicaciones()).isEqualTo(antes.publicaciones() + 3);
        assertThat(despues.imagenes()).isEqualTo(antes.imagenes() + 3);
        assertThat(despues.imagenesSinSha256())
                .isEqualTo(antes.imagenesSinSha256() + 1);
        assertThat(despues.imagenesSinPhash())
                .isEqualTo(antes.imagenesSinPhash() + 1);
        assertThat(despues.paresCandidatosEstimados())
                .isGreaterThanOrEqualTo(antes.paresCandidatosEstimados() + 1);
    }

    @Test
    void previsualizar_porPerfilYPorPublicacion_acotaLosContadores() {
        Sembrado sembrado = sembrar();

        PreviewProcesamientoHistoricoResponse porPerfil = service.previsualizar(
                new PrevisualizarColaboracionesHistoricasRequest(
                        ModoProcesamientoHistorico.AMBOS,
                        null, sembrado.idPerfilUno(), null, null, null));

        assertThat(porPerfil.publicaciones()).isEqualTo(2);
        assertThat(porPerfil.imagenes()).isEqualTo(2);
        assertThat(porPerfil.imagenesSinSha256()).isZero();
        assertThat(porPerfil.imagenesSinPhash()).isEqualTo(1);

        PreviewProcesamientoHistoricoResponse porPublicacion = service.previsualizar(
                new PrevisualizarColaboracionesHistoricasRequest(
                        ModoProcesamientoHistorico.AMBOS,
                        sembrado.idPublicacionDos(), null, null, null, null));

        assertThat(porPublicacion.publicaciones()).isEqualTo(1);
        assertThat(porPublicacion.imagenes()).isEqualTo(1);
    }

    @Test
    void contarParesCandidatosHistoricos_respetaUmbralYFiltroDePerfil() {
        Sembrado sembrado = sembrar();

        long conUmbralCero = publicacionRepository.contarParesCandidatosHistoricos(
                null, sembrado.idPerfilUno(), null, null, 0);
        long conUmbralUno = publicacionRepository.contarParesCandidatosHistoricos(
                null, sembrado.idPerfilUno(), null, null, 1);

        assertThat(conUmbralCero).isZero();
        assertThat(conUmbralUno).isEqualTo(1);
    }

    @Test
    void siguienteLote_paginaPorIdYRespetaElTamanoDeLote() {
        Sembrado sembrado = sembrar();

        ProcesamientoColaboracionHistorica job = procesamientoRepository.saveAndFlush(
                job(EstadoProcesamientoHistorico.EN_PROGRESO)
                        .filtroIdPerfil(sembrado.idPerfilUno())
                        .tamanoLote(1)
                        .build());

        List<Long> primerLote = batchService.siguienteLote(job.getIdProcesamiento());
        assertThat(primerLote).containsExactly(sembrado.idPublicacionUno());

        job.setUltimoIdPublicacion(primerLote.getFirst());
        procesamientoRepository.saveAndFlush(job);

        List<Long> segundoLote = batchService.siguienteLote(job.getIdProcesamiento());
        assertThat(segundoLote).containsExactly(sembrado.idPublicacionTres());

        job.setUltimoIdPublicacion(segundoLote.getFirst());
        procesamientoRepository.saveAndFlush(job);

        assertThat(batchService.siguienteLote(job.getIdProcesamiento())).isEmpty();
    }

    @Test
    void siguienteLote_conLimiteAlcanzadoOJobCerrado_devuelveVacio() {
        sembrar();

        ProcesamientoColaboracionHistorica limitado = procesamientoRepository.saveAndFlush(
                job(EstadoProcesamientoHistorico.EN_PROGRESO)
                        .limitePublicaciones(1)
                        .publicacionesProcesadas(1)
                        .build());

        ProcesamientoColaboracionHistorica cerrado = procesamientoRepository.saveAndFlush(
                job(EstadoProcesamientoHistorico.COMPLETADO).build());

        assertThat(batchService.siguienteLote(limitado.getIdProcesamiento())).isEmpty();
        assertThat(batchService.siguienteLote(cerrado.getIdProcesamiento())).isEmpty();
    }

    @Test
    void listar_sinFiltros_devuelveElJobCreado() {
        ProcesamientoColaboracionHistorica job = procesamientoRepository.saveAndFlush(
                job(EstadoProcesamientoHistorico.PENDIENTE).build());

        PaginaResponse<ProcesamientoHistoricoResponse> pagina =
                service.listar(null, null, null, 0, 100);

        assertThat(pagina.contenido())
                .extracting(ProcesamientoHistoricoResponse::idProcesamiento)
                .contains(job.getIdProcesamiento());
    }

    private PrevisualizarColaboracionesHistoricasRequest sinFiltros() {
        return new PrevisualizarColaboracionesHistoricasRequest(
                ModoProcesamientoHistorico.AMBOS, null, null, null, null, null);
    }

    private ProcesamientoColaboracionHistorica.ProcesamientoColaboracionHistoricaBuilder job(
            EstadoProcesamientoHistorico estado) {

        return ProcesamientoColaboracionHistorica.builder()
                .estado(estado)
                .modo(ModoProcesamientoHistorico.AMBOS)
                .notificar(false)
                .tamanoLote(10)
                .idUsuarioAdmin(1L)
                .fechaInicio(LocalDateTime.now());
    }

    /**
     * Dos perfiles y tres publicaciones: el perfil uno tiene dos (una con imagen
     * con pHash, otra con imagen sin pHash) y el perfil dos una con imagen sin
     * sha256 pero con pHash cercano al del perfil uno.
     */
    private Sembrado sembrar() {
        PerfilCreado uno = crearPerfil("88993001", "Directora Histórica");
        PerfilCreado dos = crearPerfil("88993002", "Modelo Histórica");

        Imagen conShaYPhash = imagen("hist-a.webp", "hash-a", PHASH_UNO);
        Imagen sinShaConPhash = imagen("hist-b.webp", null, PHASH_DOS);
        Imagen conShaSinPhash = imagen("hist-c.webp", "hash-c", null);

        Publicacion publicacionUno = publicacion(
                uno.idPerfil(), "Histórica uno", List.of(conShaYPhash, conShaSinPhash));
        Publicacion publicacionDos = publicacion(
                dos.idPerfil(), "Histórica dos", List.of(sinShaConPhash));
        Publicacion publicacionTres = publicacion(
                uno.idPerfil(), "Histórica tres", List.of());

        return new Sembrado(
                uno.idPerfil(),
                dos.idPerfil(),
                publicacionUno.getIdPublicacion(),
                publicacionDos.getIdPublicacion(),
                publicacionTres.getIdPublicacion());
    }

    private Imagen imagen(String nombreArchivo, String sha256, Long phash) {
        return imagenRepository.saveAndFlush(Imagen.builder()
                .url("/uploads/publicaciones/" + nombreArchivo)
                .estado("ACTIVA")
                .nombreArchivo(nombreArchivo)
                .tipoImagen("PUBLICACION")
                .tamanoBytes(1024)
                .sha256Contenido(sha256)
                .phash(phash)
                .build());
    }

    private Publicacion publicacion(Long idPerfil, String titulo, List<Imagen> imagenes) {
        Perfil perfil = perfilRepository.findById(idPerfil).orElseThrow();

        return publicacionRepository.saveAndFlush(Publicacion.builder()
                .titulo(titulo)
                .descripcion("Publicación de prueba del procesamiento histórico.")
                .fechaPublicacion(LocalDateTime.now())
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .imagenes(new ArrayList<>(imagenes))
                .build());
    }

    private PerfilCreado crearPerfil(String dni, String nombreArtistico) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Histórico").apellido("Test").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(30))
                .correo("uc66.hist." + dni + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase("modelo")
                .map(Profesion::getIdProfesion)
                .orElseGet(() -> profesionRepository.buscar("%modelo%", "modelo")
                        .getFirst().getIdProfesion());

        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesion,
                        "Perfil de prueba del procesamiento histórico.", List.of()));

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private record PerfilCreado(Long idUsuario, Long idPerfil) {
    }

    private record Sembrado(
            Long idPerfilUno,
            Long idPerfilDos,
            Long idPublicacionUno,
            Long idPublicacionDos,
            Long idPublicacionTres) {
    }
}
