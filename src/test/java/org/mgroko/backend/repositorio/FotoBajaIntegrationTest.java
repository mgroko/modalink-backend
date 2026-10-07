package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.perfiles.servicio.ExpirarPerfilService;
import org.mgroko.backend.perfiles.servicio.FotoPerfilService;
import org.mgroko.backend.usuario.servicio.ExpirarCuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * La foto solo se libera cuando el perfil pasa efectivamente a {@code BAJA}: en
 * {@code PENDIENTE_BAJA} se conserva y, al expirar la cuenta o el plazo del
 * perfil, se borran la fila de {@code imagen} y el archivo f&iacute;sico.
 *
 * No usa {@code @Transactional} a nivel de test: el borrado del archivo depende
 * del commit real de la expiraci&oacute;n, as&iacute; que cada test limpia sus
 * propias filas y archivos.
 */
@SpringBootTest
class FotoBajaIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Path DIRECTORIO_SUBIDAS = Path.of("uploads", "perfiles");
    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private FotoPerfilService fotoPerfilService;

    @Autowired
    private CrearPerfilService crearPerfilService;

    @Autowired
    private ExpirarPerfilService expirarPerfilService;

    @Autowired
    private ExpirarCuentaService expirarCuentaService;

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
    void preparar() {
        int correlativo = SECUENCIA.incrementAndGet();
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        idUsuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Baja")
                .apellido("Foto")
                .dni("78" + String.format("%08d", correlativo))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("baja.foto." + correlativo + "@example.com")
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
            perfilRepository.findById(idPerfil).ifPresent(perfilRepository::delete);
        }
        for (Long idImagen : idsImagen) {
            imagenRepository.findById(idImagen).ifPresent(imagenRepository::delete);
        }
        idsImagen.clear();
        // El usuario no se borra: el trigger de auditoría impide eliminarlo y la fila
        // no interfiere con otros tests (dni/correo propios y sin datos dependientes)
        if (idUsuario != null) {
            agendaRepository.findByUsuario_IdUsuario(idUsuario).ifPresent(agendaRepository::delete);
            restaurarUsuario();
        }
    }

    @Test
    void perfilEnPendienteDeBaja_conservaSuFoto() throws Exception {
        String archivo = subirFoto();
        Long idImagen = idsImagen.get(0);
        ponerPerfilEnPendienteDeBaja(LocalDateTime.now().minusDays(5));

        EstadoPerfilCargado perfil = perfilCargado();

        assertEquals(EstadoPerfil.PendienteBaja, perfil.estado());
        assertEquals(archivo, perfil.nombreArchivo(), "En pendiente de baja la foto sigue asociada");
        assertTrue(imagenRepository.findById(idImagen).isPresent(), "La fila de la imagen se conserva");
        assertTrue(Files.exists(DIRECTORIO_SUBIDAS.resolve(archivo)), "El archivo se conserva en disco");
    }

    @Test
    void expirarPerfilVencido_pasaABajaYBorraLaFilaYElArchivo() throws Exception {
        String archivo = subirFoto();
        Long idImagen = idsImagen.get(0);
        ponerPerfilEnPendienteDeBaja(LocalDateTime.now().minusDays(40));

        int expirados = expirarPerfilService.expirarVencidos(30);

        assertEquals(1, expirados);
        EstadoPerfilCargado perfil = perfilCargado();
        assertEquals(EstadoPerfil.Baja, perfil.estado());
        assertNull(perfil.nombreArchivo(), "Al pasar a baja la foto debe desvincularse");
        assertFalse(imagenRepository.findById(idImagen).isPresent(), "La fila de la imagen debe borrarse");
        assertFalse(Files.exists(DIRECTORIO_SUBIDAS.resolve(archivo)),
                "El archivo físico debe borrarse después del commit");
    }

    @Test
    void expirarCuentaVencida_pasaABajaYBorraLaFilaYElArchivo() throws Exception {
        String archivo = subirFoto();
        Long idImagen = idsImagen.get(0);
        ponerPerfilEnPendienteDeBaja(LocalDateTime.now().minusDays(40));
        ponerUsuarioEnPendienteDeBaja(LocalDateTime.now().minusDays(40));

        int cuentas = expirarCuentaService.expirarVencidos(30);

        assertEquals(1, cuentas);
        assertEquals(EstadoUsuario.Baja, usuarioRepository.findById(idUsuario).orElseThrow().getEstado());
        EstadoPerfilCargado perfil = perfilCargado();
        assertEquals(EstadoPerfil.Baja, perfil.estado());
        assertNull(perfil.nombreArchivo(), "Al pasar la cuenta a baja la foto debe desvincularse");
        assertFalse(imagenRepository.findById(idImagen).isPresent(), "La fila de la imagen debe borrarse");
        assertFalse(Files.exists(DIRECTORIO_SUBIDAS.resolve(archivo)),
                "El archivo físico debe borrarse después del commit");
    }

    @Test
    void expirarPerfilSinFoto_pasaABajaSinFallas() {
        ponerPerfilEnPendienteDeBaja(LocalDateTime.now().minusDays(40));

        int expirados = expirarPerfilService.expirarVencidos(30);

        assertEquals(1, expirados);
        EstadoPerfilCargado perfil = perfilCargado();
        assertEquals(EstadoPerfil.Baja, perfil.estado());
        assertNull(perfil.nombreArchivo(), "Un perfil sin foto debe pasar a baja sin imágenes");
    }

    private record EstadoPerfilCargado(EstadoPerfil estado, String nombreArchivo) {
    }

    private EstadoPerfilCargado perfilCargado() {
        // getImagen() es lazy: se lee dentro de la transacción que carga el perfil
        return new TransactionTemplate(transactionManager).execute(estado -> {
            Perfil perfil = perfilRepository.findById(idPerfil).orElseThrow();
            var imagen = perfil.getImagen();
            return new EstadoPerfilCargado(perfil.getEstado(),
                    imagen == null ? null : imagen.getNombreArchivo());
        });
    }

    private void ponerPerfilEnPendienteDeBaja(LocalDateTime fecha) {
        new TransactionTemplate(transactionManager).execute(estado -> {
            Perfil perfil = perfilRepository.findById(idPerfil).orElseThrow();
            perfil.setEstado(EstadoPerfil.PendienteBaja);
            perfil.setFechaSolicitudBaja(fecha);
            return perfilRepository.saveAndFlush(perfil).getIdPerfil();
        });
    }

    private void ponerUsuarioEnPendienteDeBaja(LocalDateTime fecha) {
        new TransactionTemplate(transactionManager).execute(estado -> {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
            usuario.setEstado(EstadoUsuario.PendienteBaja);
            usuario.setFechaSolicitudBaja(fecha);
            return usuarioRepository.saveAndFlush(usuario).getIdUsuario();
        });
    }

    private void restaurarUsuario() {
        new TransactionTemplate(transactionManager).execute(estado -> {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
            usuario.setEstado(EstadoUsuario.Activo);
            usuario.setFechaSolicitudBaja(null);
            return usuarioRepository.saveAndFlush(usuario).getIdUsuario();
        });
    }

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
