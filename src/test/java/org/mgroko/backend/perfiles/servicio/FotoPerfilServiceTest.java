package org.mgroko.backend.perfiles.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.servicio.StorageService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FotoPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private StorageService storageService;

    private FotoPerfilService fotoPerfilService;

    private Usuario usuario;
    private Perfil perfil;

    @BeforeEach
    void setUp() {
        fotoPerfilService = new FotoPerfilService(
                usuarioRepository,
                perfilRepository,
                imagenRepository,
                storageService
        );

        usuario = Usuario.builder()
                .idUsuario(1L)
                .correo("test@modalink.com")
                .estado(EstadoUsuario.Activo)
                .build();

        Profesion profesion = Profesion.builder()
                .idProfesion(10L)
                .nombre("Diseñador")
                .build();

        perfil = Perfil.builder()
                .idPerfil(2L)
                .nombreArtistico("Artista X")
                .biografia("Bio de prueba")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
    }

    @Test
    void subirFoto_subeGuardaYReemplazaFotoAnterior() {
        Imagen imagenAnterior = Imagen.builder()
                .idImagen(5L)
                .nombreArchivo("vieja.jpg")
                .url("/uploads/perfiles/vieja.jpg")
                .build();
        perfil.setImagen(imagenAnterior);

        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "nueva.jpg",
                "image/jpeg",
                "bytes".getBytes()
        );

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(2L, 1L)).thenReturn(Optional.of(perfil));

        ArchivoAlmacenado almacenado = new ArchivoAlmacenado(
                "nueva_opt.jpg",
                "/uploads/perfiles/nueva_opt.jpg",
                "image/jpeg",
                1024L
        );
        when(storageService.almacenarFotoPerfil(file)).thenReturn(almacenado);

        Imagen nuevaImagenPersistida = Imagen.builder()
                .idImagen(6L)
                .nombreArchivo("nueva_opt.jpg")
                .url("/uploads/perfiles/nueva_opt.jpg")
                .tipoImagen("image/jpeg")
                .tamanoBytes(1024)
                .estado("Activa")
                .build();

        when(imagenRepository.save(any(Imagen.class))).thenReturn(nuevaImagenPersistida);
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PerfilResponse response = fotoPerfilService.subirFoto(1L, 2L, file);

        assertNotNull(response);
        assertEquals(6L, response.idImagen());
        assertEquals("/uploads/perfiles/nueva_opt.jpg", response.fotoUrl());

        // Debe eliminar la imagen previa de bd y almacenamiento físico
        verify(imagenRepository).delete(imagenAnterior);
        verify(storageService).eliminarFotoPerfil("vieja.jpg");
    }

    @Test
    void subirFoto_perfilEnBaja_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.Baja);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(2L, 1L)).thenReturn(Optional.of(perfil));

        MockMultipartFile file = new MockMultipartFile("archivo", "test.jpg", "image/jpeg", new byte[5]);

        assertThrows(PerfilEnBajaException.class, () -> fotoPerfilService.subirFoto(1L, 2L, file));
    }

    @Test
    void eliminarFoto_eliminaAsociacionYArchivo() {
        Imagen imagenActual = Imagen.builder()
                .idImagen(8L)
                .nombreArchivo("foto_actual.jpg")
                .url("/uploads/perfiles/foto_actual.jpg")
                .build();
        perfil.setImagen(imagenActual);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(2L, 1L)).thenReturn(Optional.of(perfil));
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PerfilResponse response = fotoPerfilService.eliminarFoto(1L, 2L);

        assertNotNull(response);
        assertNull(response.idImagen());
        assertNull(response.fotoUrl());

        verify(imagenRepository).delete(imagenActual);
        verify(storageService).eliminarFotoPerfil("foto_actual.jpg");
    }
}
