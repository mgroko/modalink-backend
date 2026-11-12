package org.mgroko.backend.publicaciones.servicio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EliminarPublicacionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_DUENIO = 7L;
    private static final Long ID_PERFIL_COLABORADOR = 8L;
    private static final Long ID_PERFIL_OTRO = 9L;
    private static final Long ID_PUBLICACION = 99L;

    @Mock
    private PublicacionDatosService publicacionDatosService;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private SolicitudColaboracionRepository solicitudColaboracionRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private BorradoArchivosDiferido borradoArchivosDiferido;

    @Mock
    private ColaboracionAutomaticaLifecycleService colaboracionLifecycleService;

    @Mock
    private ColaboradorPublicacionOrigenService colaboradorOrigenService;

    @Mock
    private EntityManager entityManager;

    private EliminarPublicacionService servicio;
    private Perfil duenio;
    private Perfil colaborador;
    private Perfil otro;
    private Publicacion publicacion;
    private Imagen imagenUno;
    private Imagen imagenDos;

    @BeforeEach
    void setUp() {
        servicio = new EliminarPublicacionService(
                publicacionDatosService,
                publicacionRepository,
                imagenRepository,
                solicitudColaboracionRepository,
                notificacionService,
                borradoArchivosDiferido,
                colaboracionLifecycleService,
                colaboradorOrigenService,
                entityManager);

        duenio = crearPerfil(ID_PERFIL_DUENIO, 1L, "Luna Diseños");
        colaborador = crearPerfil(ID_PERFIL_COLABORADOR, 2L, "Sol");
        otro = crearPerfil(ID_PERFIL_OTRO, 3L, "Otro");

        imagenUno = Imagen.builder()
                .idImagen(10L)
                .nombreArchivo("publicacion-1.webp")
                .url("/uploads/publicaciones/publicacion-1.webp")
                .tipoImagen("image/webp")
                .tamanoBytes(100)
                .build();
        imagenDos = Imagen.builder()
                .idImagen(11L)
                .nombreArchivo("publicacion-2.webp")
                .url("/uploads/publicaciones/publicacion-2.webp")
                .tipoImagen("image/webp")
                .tamanoBytes(100)
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(ID_PUBLICACION)
                .titulo("Editorial Otoño")
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(duenio)
                .imagenes(new ArrayList<>(List.of(imagenUno, imagenDos)))
                .colaboradores(new LinkedHashSet<>(List.of(colaborador)))
                .build();

        when(publicacionDatosService.validarPerfilActivo(ID_USUARIO, ID_PERFIL_DUENIO))
                .thenReturn(duenio);
        when(publicacionRepository.findByIdPublicacionForUpdate(ID_PUBLICACION))
                .thenReturn(Optional.of(publicacion));
    }

    @Test
    void eliminar_comoDuenio_eliminaPublicacionImagenesArchivosSolicitudesYNotificaciones() {
        servicio.eliminar(ID_USUARIO, ID_PERFIL_DUENIO, ID_PUBLICACION);

        verify(publicacionRepository).findByIdPublicacionForUpdate(ID_PUBLICACION);
        verify(publicacionRepository).eliminarImagenesPublicacion(ID_PUBLICACION);
        verify(imagenRepository).eliminarFisicamente(imagenUno.getIdImagen());
        verify(imagenRepository).eliminarFisicamente(imagenDos.getIdImagen());
        verify(borradoArchivosDiferido)
                .registrarBorrados(null, imagenUno.getNombreArchivo());
        verify(borradoArchivosDiferido)
                .registrarBorrados(null, imagenDos.getNombreArchivo());
        verify(publicacionRepository).eliminarColaboradoresPublicacion(ID_PUBLICACION);
        verify(solicitudColaboracionRepository).eliminarPorPublicacion(ID_PUBLICACION);
        verify(publicacionRepository).eliminarMeGustaDePublicacion(ID_PUBLICACION);
        verify(publicacionRepository).eliminarComentariosDePublicacion(ID_PUBLICACION);
        verify(notificacionService).eliminarPorEntidad(
                NotificacionService.ENTIDAD_PUBLICACION, ID_PUBLICACION);
        verify(publicacionRepository).eliminarFisicamente(ID_PUBLICACION);
        verify(entityManager).clear();
    }

    @Test
    void eliminar_comoColaborador_soloQuitaLaEtiqueta() {
        when(publicacionDatosService.validarPerfilActivo(
                ID_USUARIO, ID_PERFIL_COLABORADOR)).thenReturn(colaborador);

        servicio.eliminar(ID_USUARIO, ID_PERFIL_COLABORADOR, ID_PUBLICACION);

        verify(publicacionRepository).eliminarColaboradorPublicacion(
                ID_PUBLICACION, ID_PERFIL_COLABORADOR);
        verify(publicacionRepository, never()).eliminarImagenesPublicacion(any());
        verify(imagenRepository, never()).eliminarFisicamente(any());
        verify(borradoArchivosDiferido, never()).registrarBorrados(any(), any());
        verify(solicitudColaboracionRepository, never()).eliminarPorPublicacion(any());
        verify(notificacionService, never()).eliminarPorEntidad(any(), any());
        verify(publicacionRepository, never()).eliminarFisicamente(any());
    }

    @Test
    void eliminar_publicacionOculta_comoDuenio_eliminaFisicamente() {
        publicacion.setEstado(EstadoPublicacion.OCULTA);

        servicio.eliminar(ID_USUARIO, ID_PERFIL_DUENIO, ID_PUBLICACION);

        verify(publicacionRepository).eliminarFisicamente(ID_PUBLICACION);
    }

    @Test
    void eliminar_publicacionOculta_comoColaborador_soloQuitaLaEtiqueta() {
        publicacion.setEstado(EstadoPublicacion.OCULTA);
        when(publicacionDatosService.validarPerfilActivo(
                ID_USUARIO, ID_PERFIL_COLABORADOR)).thenReturn(colaborador);

        servicio.eliminar(ID_USUARIO, ID_PERFIL_COLABORADOR, ID_PUBLICACION);

        verify(publicacionRepository).eliminarColaboradorPublicacion(
                ID_PUBLICACION, ID_PERFIL_COLABORADOR);
        verify(publicacionRepository, never()).eliminarFisicamente(any());
    }

    @Test
    void eliminar_perfilAjeno_lanzaNoEncontrada() {
        when(publicacionDatosService.validarPerfilActivo(
                ID_USUARIO, ID_PERFIL_OTRO)).thenReturn(otro);

        assertThatThrownBy(() -> servicio.eliminar(
                ID_USUARIO, ID_PERFIL_OTRO, ID_PUBLICACION))
                .isInstanceOf(PublicacionNoEncontradaException.class);

        verify(publicacionRepository, never()).eliminarColaboradorPublicacion(any(), any());
        verify(publicacionRepository, never()).eliminarFisicamente(any());
    }

    @Test
    void eliminar_publicacionInexistente_lanzaNoEncontrada() {
        when(publicacionRepository.findByIdPublicacionForUpdate(ID_PUBLICACION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(
                ID_USUARIO, ID_PERFIL_DUENIO, ID_PUBLICACION))
                .isInstanceOf(PublicacionNoEncontradaException.class);
    }

    @Test
    void eliminar_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        when(publicacionDatosService.validarPerfilActivo(ID_USUARIO, null))
                .thenThrow(new PerfilActivoNoSeleccionadoException());

        assertThatThrownBy(() -> servicio.eliminar(ID_USUARIO, null, ID_PUBLICACION))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void eliminar_perfilEnBaja_lanzaPerfilEnBaja() {
        when(publicacionDatosService.validarPerfilActivo(
                ID_USUARIO, ID_PERFIL_DUENIO))
                .thenThrow(new PerfilEnBajaException("El perfil no se encuentra activo."));

        assertThatThrownBy(() -> servicio.eliminar(
                ID_USUARIO, ID_PERFIL_DUENIO, ID_PUBLICACION))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    private Perfil crearPerfil(Long idPerfil, Long idUsuario, String nombreArtistico) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombreArtistico)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder()
                        .idUsuario(idUsuario)
                        .estado(EstadoUsuario.Activo)
                        .build())
                .profesion(Profesion.builder()
                        .idProfesion(1L)
                        .nombre("Profesión")
                        .build())
                .build();
    }
}
