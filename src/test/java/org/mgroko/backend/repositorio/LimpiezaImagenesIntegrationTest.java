package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.perfiles.servicio.EliminarImagenesPerfilService;
import org.mgroko.backend.storage.servicio.LimpiezaImagenesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Job de reconciliaci&oacute;n de im&aacute;genes (Fase 5): borra archivos sin fila,
 * filas sin ninguna referencia y las im&aacute;genes de perfiles ya en baja,
 * respetando la gracia de {@code IMAGEN_LIMPIEZA_GRACIA_MINUTOS} y sin abortar
 * ante un fallo individual.
 *
 * Usa un directorio de subida propio ({@code target/...}) para que el job no
 * toque las fotos de los otros tests ni el archivo commiteado en
 * {@code uploads/perfiles}.
 */
@SpringBootTest(properties = "app.storage.upload-dir=target/uploads-limpieza-fase5")
class LimpiezaImagenesIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Path DIRECTORIO_SUBIDAS = Path.of("target", "uploads-limpieza-fase5", "perfiles");
    private static final Path ARCHIVO_COMMITEADO = Path.of("uploads", "perfiles",
            "perfil_1df0847a-dd47-40ee-b875-d7fb5bd227c8.jpg");
    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private LimpiezaImagenesService limpiezaImagenesService;

    @Autowired
    private EliminarImagenesPerfilService eliminarImagenesPerfilService;

    @Autowired
    private CrearPerfilService crearPerfilService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private ImagenRepository imagenRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long idUsuario;
    private Long idPerfil;
    private final List<Long> idsImagen = new ArrayList<>();
    private final List<Path> archivos = new ArrayList<>();

    @BeforeEach
    void preparar() throws Exception {
        vaciarDirectorioDeFotos();
        borrarImagenesSinReferencia();
        liberarFotosDePerfilesEnBaja();

        int correlativo = SECUENCIA.incrementAndGet();
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        idUsuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Limpieza")
                .apellido("Imagenes")
                .dni("79" + String.format("%08d", correlativo))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("limpieza.imagenes." + correlativo + "@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build()).getIdUsuario();

        Long idProfesion = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
        idPerfil = crearPerfilService.crear(idUsuario,
                new CrearPerfilRequest("Luna", idProfesion, "Modelo profesional.", List.of())).idPerfil();
    }

    @AfterEach
    void limpiar() throws Exception {
        // El perfil se borra primero: su columna foto_perfil apunta a la imagen
        if (idPerfil != null) {
            perfilRepository.findById(idPerfil).ifPresent(perfilRepository::delete);
        }
        for (Long idImagen : idsImagen) {
            imagenRepository.findById(idImagen).ifPresent(imagenRepository::delete);
        }
        idsImagen.clear();
        if (idUsuario != null) {
            agendaRepository.findByUsuario_IdUsuario(idUsuario).ifPresent(agendaRepository::delete);
        }
        vaciarDirectorioDeFotos();
    }

    @Test
    void archivosSinFila_fueraDeLaGracia_seBorran_yLosRecientesSeConservan() throws Exception {
        Path huerfanoViejo = crearArchivo("huerfano-viejo.webp", 180);
        Path huerfanoReciente = crearArchivo("huerfano-reciente.webp", 0);

        LimpiezaImagenesService.Resultado resultado = limpiezaImagenesService.ejecutar();

        assertEquals(1, resultado.archivosHuerfanos());
        assertEquals(0, resultado.errores());
        assertFalse(Files.exists(huerfanoViejo), "El archivo huÃ©rfano fuera de la gracia debe borrarse");
        assertTrue(Files.exists(huerfanoReciente), "El archivo reciente estÃ¡ en gracia y se conserva");
        assertTrue(Files.exists(ARCHIVO_COMMITEADO),
                "El job no debe tocar el directorio de subidas por defecto");
    }

    @Test
    void imagenSinReferenciaAntigua_seBorraFilaYArchivo_yLaRecienteNo() throws Exception {
        Path archivoViejo = crearArchivo("huerfana-vieja.webp", 180);
        Long idVieja = crearImagen("huerfana-vieja.webp", LocalDateTime.now().minusHours(3));

        Path archivoReciente = crearArchivo("huerfana-reciente.webp", 0);
        Long idReciente = crearImagen("huerfana-reciente.webp", LocalDateTime.now());

        Path archivoReferenciado = crearArchivo("referenciada-vieja.webp", 180);
        Long idReferenciada = crearImagen("referenciada-vieja.webp", LocalDateTime.now().minusHours(3));
        asociarFotoAlPerfil(idReferenciada);

        LimpiezaImagenesService.Resultado resultado = limpiezaImagenesService.ejecutar();

        assertEquals(1, resultado.imagenesHuerfanas());
        assertEquals(0, resultado.archivosHuerfanos());
        assertEquals(0, resultado.errores());
        assertFalse(imagenRepository.findById(idVieja).isPresent(), "La fila sin referencia y antigua se borra");
        assertFalse(Files.exists(archivoViejo), "Su archivo tambiÃ©n se borra");
        assertTrue(imagenRepository.findById(idReciente).isPresent(), "La fila reciente sigue en gracia");
        assertTrue(Files.exists(archivoReciente));
        assertTrue(imagenRepository.findById(idReferenciada).isPresent(), "Una fila referenciada nunca es huÃ©rfana");
        assertTrue(Files.exists(archivoReferenciado));
    }

    @Test
    void imagenDePerfilEnBaja_seEliminaLaFilaYElArchivo() throws Exception {
        Path archivo = crearArchivo("perfil-baja.webp", 180);
        Long idImagen = crearImagen("perfil-baja.webp", LocalDateTime.now().minusHours(3));
        asociarFotoAlPerfil(idImagen);
        ponerPerfilEnBaja();

        LimpiezaImagenesService.Resultado resultado = limpiezaImagenesService.ejecutar();

        assertTrue(resultado.imagenesPerfilesBaja() >= 1,
                "Debe reintentarse la limpieza de las imÃ¡genes de perfiles en baja");
        assertEquals(0, resultado.errores());
        assertNull(perfilCargado().nombreArchivo(), "La foto debe quedar desvinculada");
        assertFalse(imagenRepository.findById(idImagen).isPresent(), "La fila debe borrarse");
        assertFalse(Files.exists(archivo), "El archivo debe borrarse");
    }

    @Test
    void falloAlBorrarUnaImagen_noAbortaElResto() throws Exception {
        Path directorioBloqueado = DIRECTORIO_SUBIDAS.resolve("bloqueada.webp");
        Files.createDirectories(directorioBloqueado);
        Files.write(directorioBloqueado.resolve("interno.bin"), "dummy".getBytes());
        archivos.add(directorioBloqueado.resolve("interno.bin"));
        archivos.add(directorioBloqueado);
        Long idBloqueada = crearImagen("bloqueada.webp", LocalDateTime.now().minusHours(3));

        Path archivoLibre = crearArchivo("libre.webp", 180);
        Long idLibre = crearImagen("libre.webp", LocalDateTime.now().minusHours(3));

        Path archivoSuelto = crearArchivo("suelto.webp", 180);

        LimpiezaImagenesService.Resultado resultado = limpiezaImagenesService.ejecutar();

        assertEquals(1, resultado.imagenesHuerfanas(), "La imagen que sÃ­ se puede borrar no se ve afectada");
        assertEquals(1, resultado.archivosHuerfanos(), "El archivo suelto se borra pese al fallo anterior");
        assertEquals(1, resultado.errores(), "Solo se registra el fallo del elemento bloqueado");
        assertFalse(imagenRepository.findById(idBloqueada).isPresent(),
                "La fila se borra aunque el archivo no se pueda eliminar");
        assertTrue(Files.exists(directorioBloqueado), "El archivo bloqueado queda para el prÃ³ximo ciclo");
        assertFalse(imagenRepository.findById(idLibre).isPresent());
        assertFalse(Files.exists(archivoLibre));
        assertFalse(Files.exists(archivoSuelto));
    }

    private Path crearArchivo(String nombre, long minutosDeAntiguedad) throws IOException {
        Files.createDirectories(DIRECTORIO_SUBIDAS);
        Path ruta = DIRECTORIO_SUBIDAS.resolve(nombre);
        Files.write(ruta, "dummy".getBytes());
        Files.setLastModifiedTime(ruta, FileTime.from(Instant.now().minusSeconds(minutosDeAntiguedad * 60)));
        archivos.add(ruta);
        return ruta;
    }

    private Long crearImagen(String nombreArchivo, LocalDateTime fechaSubida) {
        Imagen imagen = imagenRepository.saveAndFlush(Imagen.builder()
                .url("/uploads/perfiles/" + nombreArchivo)
                .estado("Activa")
                .nombreArchivo(nombreArchivo)
                .tipoImagen("image/webp")
                .tamanoBytes(32)
                .fechaSubida(fechaSubida)
                .build());
        idsImagen.add(imagen.getIdImagen());
        return imagen.getIdImagen();
    }

    private void asociarFotoAlPerfil(Long idImagen) {
        new TransactionTemplate(transactionManager).execute(estado -> {
            Perfil perfil = perfilRepository.findById(idPerfil).orElseThrow();
            perfil.setImagen(imagenRepository.findById(idImagen).orElseThrow());
            return perfilRepository.saveAndFlush(perfil).getIdPerfil();
        });
    }

    private void ponerPerfilEnBaja() {
        new TransactionTemplate(transactionManager).execute(estado -> {
            Perfil perfil = perfilRepository.findById(idPerfil).orElseThrow();
            perfil.setEstado(EstadoPerfil.Baja);
            return perfilRepository.saveAndFlush(perfil).getIdPerfil();
        });
    }

    private EstadoPerfilCargado perfilCargado() {
        return new TransactionTemplate(transactionManager).execute(estado -> {
            Perfil perfil = perfilRepository.findById(idPerfil).orElseThrow();
            var imagen = perfil.getImagen();
            return new EstadoPerfilCargado(perfil.getEstado(),
                    imagen == null ? null : imagen.getNombreArchivo());
        });
    }

    private record EstadoPerfilCargado(EstadoPerfil estado, String nombreArchivo) {
    }

    private void vaciarDirectorioDeFotos() throws IOException {
        if (!Files.isDirectory(DIRECTORIO_SUBIDAS)) {
            return;
        }
        try (Stream<Path> entradas = Files.walk(DIRECTORIO_SUBIDAS)) {
            for (Path ruta : entradas.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(ruta);
            }
        }
    }

    private void borrarImagenesSinReferencia() {
        for (Imagen imagen : imagenRepository.buscarHuerfanas(LocalDateTime.now())) {
            imagenRepository.delete(imagen);
        }
    }

    private void liberarFotosDePerfilesEnBaja() {
        for (Long idPerfilEnBaja : perfilRepository.buscarIdsConFoto(EstadoPerfil.Baja)) {
            eliminarImagenesPerfilService.eliminarImagenesDePerfil(idPerfilEnBaja);
        }
    }
}
