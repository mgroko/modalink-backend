package org.mgroko.backend.perfiles.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.perfiles.dto.ResenasPerfilResponse;
import org.mgroko.backend.perfiles.dto.ResumenResenasRow;
import org.mgroko.backend.perfiles.exception.ResenaNoEncontradaException;
import org.mgroko.backend.repositorio.CalificacionPerfilRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ConsultarResenasPerfilServiceTest {

    private static final Long ID_PERFIL = 5L;
    private static final Long ID_USUARIO = 1L;

    @Mock
    private CalificacionPerfilRepository calificacionPerfilRepository;

    @Mock
    private VerPerfilService verPerfilService;

    private ConsultarResenasPerfilService service;

    @BeforeEach
    void setUp() {
        service = new ConsultarResenasPerfilService(
                calificacionPerfilRepository,
                verPerfilService);
    }

    @Test
    @DisplayName("Lista reseñas con resumen y datos públicos del emisor")
    void listar_conResenas_devuelveResumenYContenido() {
        CalificacionPerfil calificacion = calificacion();
        Page<CalificacionPerfil> pagina = new PageImpl<>(
                List.of(calificacion),
                Pageable.ofSize(10),
                1);

        when(verPerfilService.obtenerPerfilVisible(ID_PERFIL, ID_USUARIO))
                .thenReturn(new VerPerfilService.PerfilVisible(perfilReceptor(), false));
        when(calificacionPerfilRepository.findByPerfilReceptorIdPerfil(
                eq(ID_PERFIL), any(Pageable.class)))
                .thenReturn(pagina);
        when(calificacionPerfilRepository.resumirPorPerfilReceptor(ID_PERFIL))
                .thenReturn(new ResumenResenasRow(1L, 5.0, 1L, 0L, 0L, 0L, 0L));

        ResenasPerfilResponse response = service.listar(
                ID_PERFIL, ID_USUARIO, 0, 10);

        assertThat(response.idPerfil()).isEqualTo(ID_PERFIL);
        assertThat(response.resumen().total()).isEqualTo(1);
        assertThat(response.resumen().promedio()).isEqualTo(5.0);
        assertThat(response.resumen().distribucion()).hasSize(5);
        assertThat(response.resumen().distribucion().get(0).valoracion()).isEqualTo(5);
        assertThat(response.resumen().distribucion().get(0).cantidad()).isEqualTo(1);
        assertThat(response.calificaciones().contenido()).hasSize(1);

        var resena = response.calificaciones().contenido().get(0);
        assertThat(resena.idCalificacion()).isEqualTo(1L);
        assertThat(resena.valoracion()).isEqualTo(5);
        assertThat(resena.emisor().idPerfil()).isEqualTo(10L);
        assertThat(resena.emisor().nombreArtistico()).isEqualTo("Lucía Benítez");
        assertThat(resena.emisor().profesion()).isEqualTo("Diseñadora de indumentaria");
        assertThat(resena.emisor().fotoUrl()).isEqualTo("/uploads/perfiles/lucia.jpg");
        assertThat(resena.proyecto().idProyecto()).isEqualTo(100L);
        assertThat(resena.proyecto().nombre()).isEqualTo("Editorial Primavera 2026");
    }

    @Test
    @DisplayName("Sin reseñas devuelve promedio cero y distribución completa en cero")
    void listar_sinResenas_devuelveResumenVacio() {
        when(verPerfilService.obtenerPerfilVisible(ID_PERFIL, ID_USUARIO))
                .thenReturn(new VerPerfilService.PerfilVisible(perfilReceptor(), true));
        when(calificacionPerfilRepository.findByPerfilReceptorIdPerfil(
                eq(ID_PERFIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(calificacionPerfilRepository.resumirPorPerfilReceptor(ID_PERFIL))
                .thenReturn(new ResumenResenasRow(0L, null, null, null, null, null, null));

        ResenasPerfilResponse response = service.listar(
                ID_PERFIL, ID_USUARIO, 0, 10);

        assertThat(response.resumen().total()).isZero();
        assertThat(response.resumen().promedio()).isZero();
        assertThat(response.resumen().distribucion())
                .extracting(d -> d.cantidad())
                .containsOnly(0L);
        assertThat(response.calificaciones().contenido()).isEmpty();
    }

    @Test
    @DisplayName("El tamaño de página se limita al máximo permitido")
    void listar_tamanoExcesivo_seLimitaAlMaximo() {
        when(verPerfilService.obtenerPerfilVisible(ID_PERFIL, ID_USUARIO))
                .thenReturn(new VerPerfilService.PerfilVisible(perfilReceptor(), false));
        when(calificacionPerfilRepository.findByPerfilReceptorIdPerfil(
                eq(ID_PERFIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(calificacionPerfilRepository.resumirPorPerfilReceptor(ID_PERFIL))
                .thenReturn(new ResumenResenasRow(0L, null, null, null, null, null, null));

        service.listar(ID_PERFIL, ID_USUARIO, 0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(calificacionPerfilRepository)
                .findByPerfilReceptorIdPerfil(eq(ID_PERFIL), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("Un perfil no visible corta la consulta antes de ir al repositorio")
    void listar_perfilNoVisible_propagaLaExcepcion() {
        when(verPerfilService.obtenerPerfilVisible(ID_PERFIL, ID_USUARIO))
                .thenThrow(new PerfilNoEncontradoException("Perfil no encontrado."));

        assertThatThrownBy(() -> service.listar(ID_PERFIL, ID_USUARIO, 0, 10))
                .isInstanceOf(PerfilNoEncontradoException.class);

        verify(calificacionPerfilRepository, never())
                .findByPerfilReceptorIdPerfil(any(), any());
    }

    @Test
    @DisplayName("Devuelve el detalle de una reseña del perfil")
    void obtener_resenaDelPerfil_devuelveDetalle() {
        when(verPerfilService.obtenerPerfilVisible(ID_PERFIL, ID_USUARIO))
                .thenReturn(new VerPerfilService.PerfilVisible(perfilReceptor(), false));
        when(calificacionPerfilRepository
                .findByIdCalificacionAndPerfilReceptorIdPerfil(1L, ID_PERFIL))
                .thenReturn(Optional.of(calificacion()));

        var response = service.obtener(ID_PERFIL, 1L, ID_USUARIO);

        assertThat(response.idCalificacion()).isEqualTo(1L);
        assertThat(response.resenia()).isEqualTo("Excelente predisposición.");
        assertThat(response.emisor().nombreArtistico()).isEqualTo("Lucía Benítez");
    }

    @Test
    @DisplayName("Una reseña de otro perfil responde como no encontrada")
    void obtener_resenaDeOtroPerfil_lanzaNoEncontrada() {
        when(verPerfilService.obtenerPerfilVisible(ID_PERFIL, ID_USUARIO))
                .thenReturn(new VerPerfilService.PerfilVisible(perfilReceptor(), false));
        when(calificacionPerfilRepository
                .findByIdCalificacionAndPerfilReceptorIdPerfil(1L, ID_PERFIL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(ID_PERFIL, 1L, ID_USUARIO))
                .isInstanceOf(ResenaNoEncontradaException.class)
                .hasMessage("La reseña no fue encontrada.");
    }

    private CalificacionPerfil calificacion() {
        Usuario usuarioEmisor = Usuario.builder()
                .idUsuario(2L)
                .nombre("Lucía")
                .apellido("Benítez")
                .build();

        Profesion profesion = Profesion.builder()
                .idProfesion(3L)
                .codigo("DISENIADORA")
                .nombre("Diseñadora de indumentaria")
                .build();

        Imagen imagen = Imagen.builder()
                .idImagen(4L)
                .url("/uploads/perfiles/lucia.jpg")
                .build();

        Perfil emisor = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Lucía Benítez")
                .usuario(usuarioEmisor)
                .profesion(profesion)
                .imagen(imagen)
                .build();

        Proyecto proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Editorial Primavera 2026")
                .build();

        return CalificacionPerfil.builder()
                .idCalificacion(1L)
                .proyecto(proyecto)
                .perfilEmisor(emisor)
                .perfilReceptor(perfilReceptor())
                .valoracion(5)
                .resenia("Excelente predisposición.")
                .fechaRegistro(LocalDateTime.of(2026, 10, 12, 15, 30))
                .build();
    }

    private Perfil perfilReceptor() {
        return Perfil.builder()
                .idPerfil(ID_PERFIL)
                .nombreArtistico("Doja Cat")
                .build();
    }
}
