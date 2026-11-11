package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.TipoPolaroid;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class PolaroidPerfilIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    @Autowired
    private RolProyectoRepository rolProyectoRepository;

    @Autowired
    private MiembroProyectoRepository miembroProyectoRepository;

    @Autowired
    private PolaroidPerfilRepository polaroidPerfilRepository;

    @Autowired
    private TipoPolaroidRepository tipoPolaroidRepository;

    @Test
    void historialIncluyeFinalizadosYCanceladosSoloConMembresiaActiva() {
        String sufijo = nuevoSufijo();
        Perfil perfil = crearPerfil(sufijo);

        Proyecto finalizado = crearProyecto("Finalizado " + sufijo,
                EstadoProyecto.FINALIZADO, null, LocalDateTime.now().minusDays(2));
        Proyecto cancelado = crearProyecto("Cancelado " + sufijo,
                EstadoProyecto.CANCELADO, LocalDateTime.now().minusDays(1), null);
        Proyecto canceladoConBaja = crearProyecto("Cancelado baja " + sufijo,
                EstadoProyecto.CANCELADO, LocalDateTime.now().minusHours(1), null);

        crearMiembro(perfil, finalizado, EstadoParticipacion.ACTIVO);
        crearMiembro(perfil, cancelado, EstadoParticipacion.ACTIVO);
        crearMiembro(perfil, canceladoConBaja, EstadoParticipacion.BAJA_VOLUNTARIA);
        em.flush();
        em.clear();

        Page<MiembroProyecto> historial = miembroProyectoRepository
                .buscarHistorialProyectosDePerfil(
                        perfil.getIdPerfil(),
                        List.of(EstadoProyecto.FINALIZADO, EstadoProyecto.CANCELADO),
                        EstadoParticipacion.ACTIVO,
                        PageRequest.of(0, 10));

        assertThat(historial.getTotalElements()).isEqualTo(2);
        assertThat(historial.getContent())
                .extracting(m -> m.getProyecto().getNombre())
                .containsExactly("Cancelado " + sufijo, "Finalizado " + sufijo);
    }

    @Test
    void polaroidRepositoryOrdenaPorOrdenYCalculaElMaximo() {
        String sufijo = nuevoSufijo();
        Perfil perfil = crearPerfil(sufijo);
        TipoPolaroid tipo = tipoPolaroidRepository.findAllByOrderByIdTipoPolaroidAsc().get(0);

        crearPolaroid(perfil, tipo, 2, "dos_" + sufijo);
        crearPolaroid(perfil, tipo, 1, "uno_" + sufijo);
        em.flush();
        em.clear();

        List<PolaroidPerfil> polaroids = polaroidPerfilRepository
                .findByPerfilIdPerfilOrderByOrdenAsc(perfil.getIdPerfil());

        assertThat(polaroids).hasSize(2);
        assertThat(polaroids).extracting(PolaroidPerfil::getOrden)
                .containsExactly(1, 2);
        assertThat(polaroidPerfilRepository.obtenerMaximoOrden(perfil.getIdPerfil()))
                .isEqualTo(2);
    }

    private String nuevoSufijo() {
        return String.format("%010d", Math.abs(System.nanoTime() % 10_000_000_000L));
    }

    private Perfil crearPerfil(String sufijo) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        Profesion modelo = profesionRepository.findByCodigoIgnoreCase("MODELO").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Modelo").apellido("Polaroid")
                .dni(sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("polaroid" + sufijo + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Modelo " + sufijo)
                .biografia("Perfil de prueba")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(modelo)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private Proyecto crearProyecto(
            String nombre,
            EstadoProyecto estado,
            LocalDateTime fechaCancelacion,
            LocalDateTime fechaFinalizacion) {
        Proyecto proyecto = Proyecto.builder()
                .nombre(nombre)
                .descripcion("Proyecto de prueba")
                .estado(estado)
                .privacidad(Privacidad.PUBLICO)
                .aceptaPostulacionGral(false)
                .fechaCancelacion(fechaCancelacion)
                .fechaFinalizacion(fechaFinalizacion)
                .build();
        em.persist(proyecto);
        return proyecto;
    }

    private void crearMiembro(
            Perfil perfil,
            Proyecto proyecto,
            EstadoParticipacion estado) {
        RolProyecto rol = rolProyectoRepository.findByCodigo("MIEMBRO").orElseThrow();
        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rol)
                .estadoParticipacion(estado)
                .build());
    }

    private void crearPolaroid(
            Perfil perfil,
            TipoPolaroid tipo,
            int orden,
            String nombreArchivo) {
        Imagen imagen = Imagen.builder()
                .nombreArchivo(nombreArchivo + ".webp")
                .url("/uploads/polaroids/" + nombreArchivo + ".webp")
                .tipoImagen("image/webp")
                .tamanoBytes(100)
                .estado("Activa")
                .build();
        em.persist(imagen);
        em.persist(PolaroidPerfil.builder()
                .perfil(perfil)
                .imagen(imagen)
                .tipoPolaroid(tipo)
                .orden(orden)
                .build());
    }
}
