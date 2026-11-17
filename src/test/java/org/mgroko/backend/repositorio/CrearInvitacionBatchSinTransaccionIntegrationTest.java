package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralBatchRequest;
import org.mgroko.backend.invitaciones.servicio.CrearInvitacionBatchService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Regresión del 500 al enviar invitaciones por lote.
 *
 * <p>{@link CrearInvitacionBatchService} valida el perfil activo sin abrir
 * transacción. Con {@code spring.jpa.open-in-view=false} el {@code Perfil}
 * devuelto por el repositorio queda detached y leer su usuario perezoso
 * lanzaba {@code LazyInitializationException} antes de crear la primera
 * invitación.</p>
 *
 * <p>La clase NO lleva {@code @Transactional} a propósito: la transacción del
 * test mantendría abierta la sesión y ocultaría el defecto, que es justo lo
 * que pasaba en {@code CrearInvitacionIntegrationTest}.</p>
 */
@SpringBootTest
class CrearInvitacionBatchSinTransaccionIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Long ID_INEXISTENTE = Long.MAX_VALUE;

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private ProfesionRepository profesionRepository;
    @Autowired
    private CrearPerfilService crearPerfilService;
    @Autowired
    private CrearInvitacionBatchService crearInvitacionBatchService;

    @Test
    void invitarGeneral_sinTransaccionAbierta_noLanzaLazyInitializationException() {
        PerfilCreado director = crearPerfil(
                "uc31.sintx.dir@example.com", "88831901", "Directora Sin Tx");

        // El perfil activo se valida antes de buscar el proyecto: si esa
        // validación llega hasta acá, el acceso perezoso ya no explota.
        assertThatThrownBy(() -> crearInvitacionBatchService.invitarGeneral(
                director.idUsuario(),
                director.idPerfil(),
                ID_INEXISTENTE,
                new CrearInvitacionGralBatchRequest(
                        List.of(ID_INEXISTENTE), null)))
                .isInstanceOf(ProyectoNoEncontradoException.class);
    }

    private PerfilCreado crearPerfil(String correo, String dni, String nombreArtistico) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC31").apellido("SinTransaccion").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(26))
                .correo(correo)
                .estado(org.mgroko.backend.modelo.enums.EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesion = profesionRepository.findByCodigoIgnoreCase("modelo")
                .map(Profesion::getIdProfesion)
                .orElseGet(() -> profesionRepository.buscar("%modelo%", "modelo")
                        .getFirst().getIdProfesion());

        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesion,
                        "Perfil de prueba de regresión UC-31.", List.of()));

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private record PerfilCreado(Long idUsuario, Long idPerfil) {
    }
}
