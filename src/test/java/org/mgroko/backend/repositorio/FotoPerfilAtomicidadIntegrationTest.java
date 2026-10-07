package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.perfiles.servicio.FotoPerfilService;
import org.mgroko.backend.storage.exception.ErrorAlmacenamientoException;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.StorageService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Atomicidad archivo ↔ transacción al subir/reemplazar la foto de perfil:
 * el archivo viejo se borra recién después del commit y el nuevo se borra si la
 * transacción termina sin commit.
 *
 * No usa {@code @Transactional} a nivel de test: los borrados dependen de commits
 * reales, así que cada test limpia sus propias filas y archivos.
 */
@SpringBootTest
class FotoPerfilAtomicidadIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Path DIRECTORIO_SUBIDAS = Path.of("uploads", "perfiles");
    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private FotoPerfilService fotoPerfilService;

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

    @MockitoSpyBean
    private StorageService storageService;

    private Long idUsuario;
    private Long idPerfil;
    private final List<Long> idsImagen = new ArrayList<>();
    private final List<Path> archivos = new ArrayList<>();

    @BeforeEach
    void preparar() {
        int correlativo = SECUENCIA.incrementAndGet();
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        idUsuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Atomicidad")
                .apellido("Foto")
                .dni("77" + String.format("%08d", correlativo))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("atomicidad." + correlativo + "@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build()).getIdUsuario();

        Long idProfesion = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
        idPerfil = crearPerfilService.crear(idUsuario,
                new CrearPerfilRequest("Luna", idProfesion, "Modelo profesional.", List.of())).idPerfil();
    }

    @AfterEach
    void limpiar() throws Exception {
        for (Path archivo : archivos) {
            Files.deleteIfExists(archivo);
        }
        archivos.clear();

        if (idPerfil != null) {
            perfilRepository.deleteById(idPerfil);
        }
        for (Long idImagen : idsImagen) {
            imagenRepository.findById(idImagen).ifPresent(imagenRepository::delete);
        }
        idsImagen.clear();
        // El usuario no se borra: el trigger de auditoría impide eliminarlo y la fila
        // no interfiere con otros tests (dni/correo propios y sin datos dependientes)
        if (idUsuario != null) {
            agendaRepository.findByUsuario_IdUsuario(idUsuario).ifPresent(agendaRepository::delete);
        }
    }

    private String nombreArchivoAsociadoAlPerfil() {
        return new TransactionTemplate(transactionManager).execute(estado -> {
            Perfil perfil = perfilRepository.findById(idPerfil).orElseThrow();
            return perfil.getImagen() != null ? perfil.getImagen().getNombreArchivo() : null;
        });
    }

    @Test
    void commitExitoso_borraElArchivoViejoYConservaElNuevo() throws Exception {
        String archivoViejo = subirFoto();
        String archivoNuevo = subirFoto();

        assertTrue(Files.exists(DIRECTORIO_SUBIDAS.resolve(archivoNuevo)), "El archivo nuevo debe quedar");
        assertFalse(Files.exists(DIRECTORIO_SUBIDAS.resolve(archivoViejo)),
                "El archivo viejo debe borrarse después del commit");

        assertEquals(archivoNuevo, nombreArchivoAsociadoAlPerfil(),
                "La BD debe apuntar al archivo que quedó en disco");
        assertFalse(imagenRepository.findById(idsImagen.get(0)).isPresent(),
                "La fila de la imagen reemplazada debe haberse borrado");
    }

    @Test
    void rollbackForzado_borraElArchivoNuevoYConservaElViejoApuntadoPorLaBd() throws Exception {
        String archivoViejo = subirFoto();
        Long idImagenVieja = idsImagen.get(0);

        TransactionTemplate plantilla = new TransactionTemplate(transactionManager);
        SubidaRollback subida = plantilla.execute(estado -> {
            PerfilResponse respuesta = fotoPerfilService.subirFoto(idUsuario, idPerfil, archivo());
            estado.setRollbackOnly();
            String nombreArchivo = nombreDe(respuesta.fotoUrl());
            archivos.add(DIRECTORIO_SUBIDAS.resolve(nombreArchivo));
            return new SubidaRollback(nombreArchivo, respuesta.idImagen());
        });

        assertNotNull(subida);
        assertTrue(Files.exists(DIRECTORIO_SUBIDAS.resolve(archivoViejo)),
                "El archivo viejo debe conservarse si no hay commit");
        assertFalse(Files.exists(DIRECTORIO_SUBIDAS.resolve(subida.nombreArchivo())),
                "El archivo nuevo debe borrarse al hacer rollback");

        assertEquals(archivoViejo, nombreArchivoAsociadoAlPerfil(),
                "La BD debe seguir apuntando al archivo viejo");
        assertTrue(imagenRepository.findById(idImagenVieja).isPresent(), "La fila vieja se conserva");
        assertFalse(imagenRepository.findById(subida.idImagen).isPresent(),
                "La fila nueva no debe persistir tras el rollback");
    }

    @Test
    void falloAlBorrarElArchivoViejo_noRompeLaOperacionYQuedaRegistrado() throws Exception {
        String archivoViejo = subirFoto();
        doThrow(new ErrorAlmacenamientoException("Archivo bloqueado", new java.io.IOException("Acceso denegado")))
                .when(storageService).eliminarFotoPerfil(archivoViejo);

        Logger logger = (Logger) LoggerFactory.getLogger(BorradoArchivosDiferido.class);
        ListAppender<ILoggingEvent> registro = new ListAppender<>();
        registro.start();
        logger.addAppender(registro);
        try {
            String archivoNuevo = subirFoto();

            assertTrue(Files.exists(DIRECTORIO_SUBIDAS.resolve(archivoNuevo)),
                    "La operación debe completarse igualmente");
            assertTrue(Files.exists(DIRECTORIO_SUBIDAS.resolve(archivoViejo)),
                    "El archivo viejo queda en disco para el job de la Fase 5");
            assertTrue(registro.list.stream()
                            .anyMatch(evento -> evento.getFormattedMessage().contains(archivoViejo)),
                    "El fallo de borrado debe quedar registrado en el log: " + archivoViejo);
        } finally {
            logger.detachAppender(registro);
        }
    }

    @Test
    void eliminarFotoTrasCommit_borraElArchivoYLaFilaDeLaBd() throws Exception {
        String archivo = subirFoto();

        fotoPerfilService.eliminarFoto(idUsuario, idPerfil);

        assertFalse(Files.exists(DIRECTORIO_SUBIDAS.resolve(archivo)),
                "Eliminar la foto tras el commit debe borrar el archivo de disco");
        assertNull(nombreArchivoAsociadoAlPerfil(), "La BD no debe conservar la foto");
        assertFalse(imagenRepository.findById(idsImagen.get(0)).isPresent(),
                "La fila de la imagen debe borrarse");
    }

    @Test
    void eliminarFotoSinCommit_conservaElArchivoYLaFilaDeLaBd() throws Exception {
        String archivo = subirFoto();

        new TransactionTemplate(transactionManager).execute(estado -> {
            fotoPerfilService.eliminarFoto(idUsuario, idPerfil);
            estado.setRollbackOnly();
            return null;
        });

        assertTrue(Files.exists(DIRECTORIO_SUBIDAS.resolve(archivo)),
                "Sin commit, el archivo debe conservarse");
        assertEquals(archivo, nombreArchivoAsociadoAlPerfil(),
                "La BD debe seguir apuntando al archivo");
        assertTrue(imagenRepository.findById(idsImagen.get(0)).isPresent(), "La fila se conserva");
    }

    private record SubidaRollback(String nombreArchivo, Long idImagen) {}
    private String subirFoto() throws Exception {
        PerfilResponse respuesta = fotoPerfilService.subirFoto(idUsuario, idPerfil, archivo());
        assertNotNull(respuesta.idImagen());
        String nombreArchivo = nombreDe(respuesta.fotoUrl());
        idsImagen.add(respuesta.idImagen());
        archivos.add(DIRECTORIO_SUBIDAS.resolve(nombreArchivo));
        return nombreArchivo;
    }

    private static String nombreDe(String fotoUrl) {
        return fotoUrl.substring(fotoUrl.lastIndexOf('/') + 1);
    }

    private static MultipartFile archivo() {
        try {
            BufferedImage imagen = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = imagen.createGraphics();
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, 1200, 800);
            g.dispose();

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            assertTrue(ImageIO.write(imagen, "jpg", salida), "No hay escritor JPEG disponible");
            return new MockMultipartFile("archivo", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, salida.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el JPEG de prueba", e);
        }
    }
}
