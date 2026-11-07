package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.GenerarContratoRequest;
import org.mgroko.backend.contratos.servicio.GenerarContratoService;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.ContratoEvento;
import org.mgroko.backend.modelo.ContratoVersion;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.modelo.enums.RolParteContrato;
import org.mgroko.backend.modelo.enums.TipoEventoContrato;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Valida los triggers de la Fase 1 de UC-53:
 *
 * <ul>
 *   <li>{@code fecha_modificacion} se actualiza ante cualquier cambio;</li>
 *   <li>el evento MODIFICADO referencia la version correcta;</li>
 *   <li>se notifica CONTRATO_MODIFICADO a las partes actuales;</li>
 *   <li>dos modificaciones generan dos notificaciones por parte;</li>
 *   <li>una firma registrada bloquea la modificacion.</li>
 * </ul>
 */
@SpringBootTest
@Transactional
class ContratoModificacionTriggerIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GenerarContratoService generarContratoService;

    @Autowired
    private ContratoVersionRepository contratoVersionRepository;

    @Autowired
    private ContratoEventoRepository contratoEventoRepository;

    @Autowired
    private FirmaContratoRepository firmaContratoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void modificarTexto_registraVersionEventoYNotificacionSinDeduplicar() {
        Datos datos = crearContrato();
        Long idContrato = datos.idContrato();

        actualizarTexto(idContrato, "TEXTO MODIFICADO 1");
        em.flush();
        em.clear();

        Contrato contrato = em.find(Contrato.class, idContrato);
        assertThat(contrato.getVersionActual()).isEqualTo(2);
        assertThat(contrato.getFechaModificacion()).isNotNull();

        ContratoVersion version2 = contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(idContrato, 2)
                .orElseThrow();
        assertThat(version2.getTexto()).isEqualTo("TEXTO MODIFICADO 1");

        List<ContratoEvento> eventos = contratoEventoRepository
                .findByContrato_IdContratoOrderByIdEventoAsc(idContrato);
        ContratoEvento modificado = eventos.stream()
                .filter(e -> e.getTipoEvento() == TipoEventoContrato.MODIFICADO)
                .findFirst()
                .orElseThrow();

        assertThat(modificado.getContratoVersion()).isNotNull();
        assertThat(modificado.getContratoVersion().getIdContratoVersion())
                .isEqualTo(version2.getIdContratoVersion());

        List<Notificacion> notificaciones = notificacionesModificado(idContrato);
        assertThat(notificaciones).hasSize(2);
        assertThat(notificaciones).allMatch(n ->
                datos.idProyecto().equals(n.getIdProyecto()));
        assertThat(notificaciones).allMatch(n ->
                NotificacionService.TIPO_CONTRATO_MODIFICADO.equals(n.getTipo()));

        actualizarTexto(idContrato, "TEXTO MODIFICADO 2");
        em.flush();
        em.clear();

        assertThat(notificacionesModificado(idContrato)).hasSize(4);
    }

    @Test
    void modificarNombreSinCambiarTexto_actualizaFechaYNotifica() {
        Datos datos = crearContrato();
        Long idContrato = datos.idContrato();

        em.createNativeQuery("""
                UPDATE contrato
                   SET nombre = :nombre
                 WHERE id_contrato = :idContrato
                """)
                .setParameter("nombre", "Contrato modificado")
                .setParameter("idContrato", idContrato)
                .executeUpdate();

        em.flush();
        em.clear();

        Contrato contrato = em.find(Contrato.class, idContrato);
        assertThat(contrato.getVersionActual()).isEqualTo(1);
        assertThat(contrato.getFechaModificacion()).isNotNull();

        assertThat(contratoEventoRepository
                .findByContrato_IdContratoOrderByIdEventoAsc(idContrato))
                .extracting(ContratoEvento::getTipoEvento)
                .contains(TipoEventoContrato.MODIFICADO);

        assertThat(notificacionesModificado(idContrato)).hasSize(2);
    }

    @Test
    void modificarConFirmaRegistrada_bloquea() {
        Datos datos = crearContrato();
        Long idContrato = datos.idContrato();

        ContratoVersion version1 = contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(idContrato, 1)
                .orElseThrow();

        FirmaContrato destinatario = firmaContratoRepository
                .findByIdContratoOrderByIdMiembroAsc(idContrato)
                .stream()
                .filter(f -> f.getRolParte() == RolParteContrato.DESTINATARIO)
                .findFirst()
                .orElseThrow();

        em.createNativeQuery("""
                UPDATE firma_contrato
                   SET estado_firma = 'FIRMADO',
                       id_contrato_version = :idVersion
                 WHERE id_contrato = :idContrato
                   AND id_miembro = :idMiembro
                """)
                .setParameter("idVersion", version1.getIdContratoVersion())
                .setParameter("idContrato", idContrato)
                .setParameter("idMiembro", destinatario.getIdMiembro())
                .executeUpdate();

        em.flush();

        assertThatThrownBy(() -> actualizarTexto(idContrato, "TEXTO BLOQUEADO"))
                .satisfies(ex -> assertThat(mensajeProfundo(ex))
                        .contains("firmas registradas"));
    }

    private void actualizarTexto(Long idContrato, String texto) {
        em.createNativeQuery("""
                UPDATE contrato
                   SET texto = :texto
                 WHERE id_contrato = :idContrato
                """)
                .setParameter("texto", texto)
                .setParameter("idContrato", idContrato)
                .executeUpdate();
    }

    private List<Notificacion> notificacionesModificado(Long idContrato) {
        return em.createQuery("""
                        SELECT n FROM Notificacion n
                        WHERE n.entidadTipo = 'CONTRATO'
                          AND n.entidadId = :idContrato
                          AND n.tipo = :tipo
                        ORDER BY n.idNotificacion
                        """, Notificacion.class)
                .setParameter("idContrato", idContrato)
                .setParameter("tipo", NotificacionService.TIPO_CONTRATO_MODIFICADO)
                .getResultList();
    }

    private String mensajeProfundo(Throwable ex) {
        String mensaje = "";
        Throwable actual = ex;
        while (actual != null) {
            if (actual.getMessage() != null) {
                mensaje = actual.getMessage();
            }
            actual = actual.getCause();
        }
        return mensaje;
    }

    private Datos crearContrato() {
        long sufijo = System.nanoTime();

        Profesion profesion = Profesion.builder()
                .codigo("UC53_" + sufijo)
                .nombre("Profesional UC53 " + sufijo)
                .build();
        em.persist(profesion);

        long baseDni = Math.abs(sufijo % 100_000_000L);
        Usuario director = guardarUsuario("uc53.dir." + sufijo + "@example.com",
                String.format("%08d", baseDni));
        Usuario miembro = guardarUsuario("uc53.miem." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 1) % 100_000_000L));

        Perfil perfilDirector = perfil("Directora UC53 " + sufijo, director, profesion);
        Perfil perfilMiembro = perfil("Miembro UC53 " + sufijo, miembro, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC53 " + sufijo)
                .descripcion("Proyecto de integración UC-53")
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .build();
        em.persist(proyecto);

        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilDirector)
                .rolProyecto(rol("DIRECTOR"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());

        MiembroProyecto miembroProyecto = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilMiembro)
                .rolProyecto(rol("MIEMBRO"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        em.persist(miembroProyecto);

        TipoAcuerdoContrato tipo = TipoAcuerdoContrato.builder()
                .codigo("UC53_TIPO_" + sufijo)
                .nombre("Cesión de derechos de imagen")
                .descripcion("Tipo de integración UC-53")
                .plantilla("""
                        CONTRATO DE {{tipo}}

                        Proyecto: {{proyecto}}
                        Director emisor: {{director}}
                        Partes:
                        {{partes}}

                        Las partes acuerdan las condiciones del presente contrato.

                        {{clausulas_adicionales}}
                        """)
                .activo(true)
                .build();
        em.persist(tipo);

        em.flush();

        GenerarContratoRequest request = new GenerarContratoRequest(
                tipo.getIdTipoAcuerdo(),
                List.of(miembroProyecto.getIdMiembro()),
                List.of("Cláusula de integración"));

        ContratoGeneradoResponse response = generarContratoService.generar(
                director.getIdUsuario(),
                perfilDirector.getIdPerfil(),
                proyecto.getIdProyecto(),
                request);

        em.flush();
        em.clear();

        return new Datos(proyecto.getIdProyecto(), response.idContrato());
    }

    private Usuario guardarUsuario(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana")
                .apellido("Gómez")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }

    private Perfil perfil(String nombre, Usuario usuario, Profesion profesion) {
        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombre)
                .biografia("Perfil de integración UC-53")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private RolProyecto rol(String codigo) {
        return em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = :codigo",
                RolProyecto.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
    }

    private record Datos(Long idProyecto, Long idContrato) {
    }
}
