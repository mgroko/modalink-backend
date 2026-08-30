package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.perfiles.dto.CaracteristicaPerfilRequest;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.EditarPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.perfiles.servicio.EditarPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test de integración que reproduce el flujo real de edición de un perfil
 * "modelo" (con características técnicas) contra PostgreSQL real.
 *
 * Contexto del bug investigado: la edición funciona para perfiles SIN
 * características técnicas (solo biografía) pero falla para perfiles QUE
 * tienen características. Este test crea el perfil con características
 * (vía {@link CrearPerfilService}) y luego lo edita reenviando las mismas,
 * para confirmar si el error proviene del {@code clear()} + re-agregado de
 * la colección con clave compuesta en {@link EditarPerfilService#editar}.
 */
@SpringBootTest
@Transactional
class EditarPerfilServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String CORREO = "modelo.editar@example.com";
    private static final String DNI = "55550001";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    @Autowired
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Autowired
    private ValorCaracteristicaRepository valorCaracteristicaRepository;

    @Autowired
    private CrearPerfilService crearPerfilService;

    @Autowired
    private EditarPerfilService editarPerfilService;

    private Usuario guardarUsuario() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Maria")
                .apellido("Flores")
                .dni(DNI)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(CORREO)
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }

    private Long idProfesionModelo() {
        return profesionRepository.buscar("%modelo%").get(0).getIdProfesion();
    }

    private long idCaracteristica(Long idProfesion, String codigo) {
        return caracteristicaTecnicaRepository.buscar("%" + codigo + "%", "%", idProfesion).get(0)
                .getIdCaracteristica();
    }

    private long idValor(Long idCaracteristica, String codigoValor) {
        return valorCaracteristicaRepository
                .findByCaracteristicaTecnicaIdCaracteristicaOrderByCodigo(idCaracteristica).stream()
                .filter(v -> v.getCodigo().equals(codigoValor))
                .findFirst()
                .orElseThrow()
                .getIdValor();
    }

    private List<CaracteristicaPerfilRequest> caracteristicasDeModelo(Long idProfesion) {
        long altura = idCaracteristica(idProfesion, "altura");
        long medidaPecho = idCaracteristica(idProfesion, "medida_pecho");
        long medidaCintura = idCaracteristica(idProfesion, "medida_cintura");
        long medidaCadera = idCaracteristica(idProfesion, "medida_cadera");
        long colorOjos = idCaracteristica(idProfesion, "color_ojos");
        long colorCabello = idCaracteristica(idProfesion, "color_cabello");
        long colorPiel = idCaracteristica(idProfesion, "color_piel");
        long tipoCabello = idCaracteristica(idProfesion, "tipo_cabello");

        return List.of(
                new CaracteristicaPerfilRequest(altura, "176", null),
                new CaracteristicaPerfilRequest(medidaPecho, "84", null),
                new CaracteristicaPerfilRequest(medidaCintura, "60", null),
                new CaracteristicaPerfilRequest(medidaCadera, "88", null),
                new CaracteristicaPerfilRequest(colorOjos, null, idValor(colorOjos, "marron")),
                new CaracteristicaPerfilRequest(colorCabello, null, idValor(colorCabello, "negro")),
                new CaracteristicaPerfilRequest(colorPiel, null, idValor(colorPiel, "media")),
                new CaracteristicaPerfilRequest(tipoCabello, null, idValor(tipoCabello, "lacio")));
    }

    @Test
    void editar_perfilConCaracteristicas_persisteSinError() {
        Usuario usuario = guardarUsuario();
        Long idProfesion = idProfesionModelo();
        List<CaracteristicaPerfilRequest> caracteristicas = caracteristicasDeModelo(idProfesion);

        PerfilResponse creado = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest("Luna", idProfesion, "Modelo profesional.", caracteristicas));
        assertNotNull(creado.idPerfil());
        assertEquals(8, creado.caracteristicas().size());

        EditarPerfilRequest edicion = new EditarPerfilRequest(
                "Luna Nova", "Modelo profesional renovada.", null, caracteristicas);

        PerfilResponse editado = assertDoesNotThrow(
                () -> editarPerfilService.editar(usuario.getIdUsuario(), creado.idPerfil(), edicion));

        assertEquals("Luna Nova", editado.nombreArtistico());
        assertEquals("Modelo profesional renovada.", editado.biografia());
        assertEquals(8, editado.caracteristicas().size());
    }

    @Test
    void editar_perfilSinCaracteristicas_noLanzaError() {
        Usuario usuario = guardarUsuario();
        Long idProfesion = idProfesionModelo();

        PerfilResponse creado = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest("Luna", idProfesion, "Modelo.", List.of()));
        assertNotNull(creado.idPerfil());

        EditarPerfilRequest edicion = new EditarPerfilRequest(
                "Luna Nova", "Modelo sin características.", null, List.of());

        PerfilResponse editado = assertDoesNotThrow(
                () -> editarPerfilService.editar(usuario.getIdUsuario(), creado.idPerfil(), edicion));

        assertEquals("Luna Nova", editado.nombreArtistico());
    }
}
