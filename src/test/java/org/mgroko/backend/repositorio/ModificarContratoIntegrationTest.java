package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.GenerarContratoRequest;
import org.mgroko.backend.contratos.dto.ModificarContratoRequest;
import org.mgroko.backend.contratos.dto.PrevisualizacionModificacionResponse;
import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.contratos.exception.ContratoNoModificableException;
import org.mgroko.backend.contratos.servicio.GenerarContratoService;
import org.mgroko.backend.contratos.servicio.ModificarContratoService;
import org.mgroko.backend.contratos.servicio.PrevisualizarModificacionContratoService;
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
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.modelo.enums.TipoEventoContrato;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class ModificarContratoIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GenerarContratoService generarContratoService;

    @Autowired
    private ModificarContratoService modificarContratoService;

    @Autowired
    private PrevisualizarModificacionContratoService previsualizarModificacionService;

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
    void modificarTextoFinal_actualizaVersionEventoYNotificaciones() {
        Datos datos = crearProyectoConContrato();

        ContratoDetalleResponse response = modificarContratoService.modificar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new ModificarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.miembroProyecto().getIdMiembro()),
                        "TEXTO FINAL MODIFICADO",
                        List.of("Clausula ignorada"),
                        "Contrato modificado",
                        "Descripcion modificada"));

        em.flush();
        em.clear();

        assertThat(response.estado()).isEqualTo("PENDIENTE_FIRMA");
        assertThat(response.versionActual()).isEqualTo(2);
        assertThat(response.texto()).isEqualTo("TEXTO FINAL MODIFICADO");
        assertThat(response.nombre()).isEqualTo("Contrato modificado");
        assertThat(response.descripcion()).isEqualTo("Descripcion modificada");
        assertThat(response.fechaModificacion()).isNotNull();

        ContratoVersion version2 = contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(datos.idContrato(), 2)
                .orElseThrow();
        assertThat(version2.getTexto()).isEqualTo("TEXTO FINAL MODIFICADO");

        ContratoEvento modificado = eventos(datos.idContrato()).stream()
                .filter(e -> e.getTipoEvento() == TipoEventoContrato.MODIFICADO)
                .findFirst()
                .orElseThrow();
        assertThat(modificado.getContratoVersion().getIdContratoVersion())
                .isEqualTo(version2.getIdContratoVersion());

        assertThat(notificacionesModificado(datos.idContrato())).hasSize(2);
    }

    @Test
    void modificarSoloPartes_conMismoTexto_actualizaFechaYNotifica() {
        Datos datos = crearProyectoConContrato();
        String textoOriginal = em.find(Contrato.class, datos.idContrato()).getTexto();

        ContratoDetalleResponse response = modificarContratoService.modificar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new ModificarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.miembroNoParteProyecto().getIdMiembro()),
                        textoOriginal,
                        null,
                        null,
                        null));

        em.flush();
        em.clear();

        assertThat(response.versionActual()).isEqualTo(1);
        assertThat(response.fechaModificacion()).isNotNull();
        assertThat(response.partes())
                .extracting(parte -> parte.idMiembro())
                .contains(datos.miembroNoParteProyecto().getIdMiembro())
                .doesNotContain(datos.miembroProyecto().getIdMiembro());

        assertThat(eventos(datos.idContrato()))
                .extracting(ContratoEvento::getTipoEvento)
                .contains(TipoEventoContrato.MODIFICADO);
        assertThat(notificacionesModificado(datos.idContrato())).hasSize(2);
    }

    @Test
    void modificarDirectorSeleccionadoYDespuesDeseleccionado_cambiaRol() {
        Datos datos = crearProyectoConContrato();
        String textoOriginal = em.find(Contrato.class, datos.idContrato()).getTexto();

        ContratoDetalleResponse conDirector = modificarContratoService.modificar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new ModificarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.emisor().getIdMiembro(),
                                datos.miembroProyecto().getIdMiembro()),
                        textoOriginal,
                        null,
                        null,
                        null));

        assertThat(conDirector.partes())
                .filteredOn(p -> p.idMiembro().equals(datos.emisor().getIdMiembro()))
                .extracting(p -> p.rolParte())
                .containsExactly("DIRECTOR_DESTINATARIO");

        ContratoDetalleResponse sinDirector = modificarContratoService.modificar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new ModificarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.miembroProyecto().getIdMiembro()),
                        textoOriginal,
                        null,
                        null,
                        null));

        assertThat(sinDirector.partes())
                .filteredOn(p -> p.idMiembro().equals(datos.emisor().getIdMiembro()))
                .extracting(p -> p.rolParte())
                .containsExactly("DIRECTOR");
    }

    @Test
    void modificarConFirmaRegistrada_bloquea() {
        Datos datos = crearProyectoConContrato();
        firmarDestinatario(datos);

        assertThatThrownBy(() -> modificarContratoService.modificar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new ModificarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.miembroProyecto().getIdMiembro()),
                        "TEXTO BLOQUEADO",
                        null,
                        null,
                        null)))
                .isInstanceOf(ContratoNoModificableException.class)
                .extracting("codigo")
                .isEqualTo("CONTRATO_CON_FIRMAS_REGISTRADAS");
    }

    @Test
    void previsualizar_noPersisteCambios() {
        Datos datos = crearProyectoConContrato();
        String textoOriginal = em.find(Contrato.class, datos.idContrato()).getTexto();

        PrevisualizacionModificacionResponse preview =
                previsualizarModificacionService.previsualizar(
                        datos.director().getIdUsuario(),
                        datos.perfilDirector().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        datos.idContrato(),
                        new ModificarContratoRequest(
                                datos.tipo().getIdTipoAcuerdo(),
                                List.of(datos.miembroNoParteProyecto().getIdMiembro()),
                                "TEXTO PREVISUALIZADO",
                                null,
                                null,
                                null));

        assertThat(preview.texto()).isEqualTo("TEXTO PREVISUALIZADO");
        assertThat(preview.puedeModificarse()).isTrue();

        em.flush();
        em.clear();

        Contrato contrato = em.find(Contrato.class, datos.idContrato());
        assertThat(contrato.getTexto()).isEqualTo(textoOriginal);
        assertThat(contrato.getVersionActual()).isEqualTo(1);
        assertThat(notificacionesModificado(datos.idContrato())).isEmpty();
    }

    @Test
    void modificarConContratoActivoDelMismoTipo_bloquea() {
        Datos datos = crearProyectoConContrato();
        crearYActivarSegundoContrato(datos);

        assertThatThrownBy(() -> modificarContratoService.modificar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new ModificarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.miembroNoParteProyecto().getIdMiembro()),
                        null,
                        List.of("Nueva clausula"),
                        null,
                        null)))
                .isInstanceOf(ContratoNoGenerableException.class)
                .extracting("codigo")
                .isEqualTo("CONTRATO_DUPLICADO_ACTIVO");
    }

    @Test
    void modificarContratoActivo_bloquea() {
        Datos datos = crearProyectoConContrato();
        activarContrato(datos.idContrato());

        assertThatThrownBy(() -> modificarContratoService.modificar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new ModificarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.miembroProyecto().getIdMiembro()),
                        "TEXTO",
                        null,
                        null,
                        null)))
                .isInstanceOf(ContratoNoModificableException.class)
                .extracting("codigo")
                .isEqualTo("CONTRATO_NO_PENDIENTE_FIRMA");
    }

    @Test
    void modificarProyectoNoApto_bloquea() {
        Datos datos = crearProyectoConContrato();

        em.createNativeQuery("""
                UPDATE proyecto
                   SET estado = 'FINALIZADO'
                 WHERE id_proyecto = :idProyecto
                """)
                .setParameter("idProyecto", datos.proyecto().getIdProyecto())
                .executeUpdate();

        em.flush();
        em.clear();

        assertThatThrownBy(() -> modificarContratoService.modificar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new ModificarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.miembroProyecto().getIdMiembro()),
                        "TEXTO",
                        null,
                        null,
                        null)))
                .isInstanceOf(ContratoNoGenerableException.class)
                .extracting("codigo")
                .isEqualTo("PROYECTO_NO_APTO_PARA_CONTRATO");
    }

    @Test
    void modificarConClausulas_regeneraTexto() {
        Datos datos = crearProyectoConContrato();

        ContratoDetalleResponse response = modificarContratoService.modificar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new ModificarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.miembroProyecto().getIdMiembro()),
                        null,
                        List.of("Nueva clausula de integracion"),
                        null,
                        null));

        assertThat(response.texto()).contains("Nueva clausula de integracion");
        assertThat(response.versionActual()).isEqualTo(2);
    }

    @Test
    void previsualizarConDuplicadoActivo_devuelveConflicto() {
        Datos datos = crearProyectoConContrato();
        crearYActivarSegundoContrato(datos);

        PrevisualizacionModificacionResponse preview =
                previsualizarModificacionService.previsualizar(
                        datos.director().getIdUsuario(),
                        datos.perfilDirector().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        datos.idContrato(),
                        new ModificarContratoRequest(
                                datos.tipo().getIdTipoAcuerdo(),
                                List.of(datos.miembroNoParteProyecto().getIdMiembro()),
                                null,
                                List.of("Nueva clausula"),
                                null,
                                null));

        assertThat(preview.puedeModificarse()).isFalse();
        assertThat(preview.conflictos())
                .anyMatch(c -> "CONTRATO_DUPLICADO_ACTIVO".equals(c.codigo()));
    }

    @Test
    void modificarSinSerDirector_bloquea() {
        Datos datos = crearProyectoConContrato();

        assertThatThrownBy(() -> modificarContratoService.modificar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new ModificarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.miembroProyecto().getIdMiembro()),
                        "TEXTO",
                        null,
                        null,
                        null)))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    private void firmarDestinatario(Datos datos) {
        ContratoVersion version = contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(datos.idContrato(), 1)
                .orElseThrow();

        FirmaContrato destinatario = firmaContratoRepository
                .findByIdContratoOrderByIdMiembroAsc(datos.idContrato())
                .stream()
                .filter(f -> f.getIdMiembro().equals(datos.miembroProyecto().getIdMiembro()))
                .findFirst()
                .orElseThrow();

        em.createNativeQuery("""
                UPDATE firma_contrato
                   SET estado_firma = 'FIRMADO',
                       id_contrato_version = :idVersion
                 WHERE id_contrato = :idContrato
                   AND id_miembro = :idMiembro
                """)
                .setParameter("idVersion", version.getIdContratoVersion())
                .setParameter("idContrato", datos.idContrato())
                .setParameter("idMiembro", destinatario.getIdMiembro())
                .executeUpdate();

        em.flush();
        em.clear();
    }

    private void crearYActivarSegundoContrato(Datos datos) {
        ContratoGeneradoResponse segundo = generarContratoService.generar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                new GenerarContratoRequest(
                        datos.tipo().getIdTipoAcuerdo(),
                        List.of(datos.miembroNoParteProyecto().getIdMiembro()),
                        List.of("Clausula del segundo contrato")));

        em.flush();
        em.clear();

        activarContrato(segundo.idContrato());
    }

    private void activarContrato(Long idContrato) {
        ContratoVersion version = contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(idContrato, 1)
                .orElseThrow();

        List<FirmaContrato> firmas = firmaContratoRepository
                .findByIdContratoOrderByIdMiembroAsc(idContrato);

        for (FirmaContrato firma : firmas) {
            em.createNativeQuery("""
                    UPDATE firma_contrato
                       SET estado_firma = 'FIRMADO',
                           id_contrato_version = :idVersion
                     WHERE id_contrato = :idContrato
                       AND id_miembro = :idMiembro
                    """)
                    .setParameter("idVersion", version.getIdContratoVersion())
                    .setParameter("idContrato", idContrato)
                    .setParameter("idMiembro", firma.getIdMiembro())
                    .executeUpdate();
        }

        em.flush();
        em.clear();
    }

    private List<ContratoEvento> eventos(Long idContrato) {
        return contratoEventoRepository
                .findByContrato_IdContratoOrderByIdEventoAsc(idContrato);
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

    private Datos crearProyectoConContrato() {
        long sufijo = System.nanoTime();

        Profesion profesion = Profesion.builder()
                .codigo("UC53_INT_" + sufijo)
                .nombre("Profesional UC53 " + sufijo)
                .build();
        em.persist(profesion);

        long baseDni = Math.abs(sufijo % 100_000_000L);
        Usuario director = guardarUsuario("uc53.int.dir." + sufijo + "@example.com",
                String.format("%08d", baseDni));
        Usuario miembro = guardarUsuario("uc53.int.miem." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 1) % 100_000_000L));
        Usuario miembroNoParte = guardarUsuario("uc53.int.miem2." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 2) % 100_000_000L));

        Perfil perfilDirector = perfil("Directora UC53 " + sufijo, director, profesion);
        Perfil perfilMiembro = perfil("Miembro UC53 " + sufijo, miembro, profesion);
        Perfil perfilMiembroNoParte = perfil("Miembro2 UC53 " + sufijo, miembroNoParte, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC53 " + sufijo)
                .descripcion("Proyecto de integración UC-53")
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .build();
        em.persist(proyecto);

        MiembroProyecto emisor = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilDirector)
                .rolProyecto(rol("DIRECTOR"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        em.persist(emisor);

        MiembroProyecto miembroProyecto = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilMiembro)
                .rolProyecto(rol("MIEMBRO"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        em.persist(miembroProyecto);

        MiembroProyecto miembroNoParteProyecto = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilMiembroNoParte)
                .rolProyecto(rol("MIEMBRO"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        em.persist(miembroNoParteProyecto);

        TipoAcuerdoContrato tipo = TipoAcuerdoContrato.builder()
                .codigo("UC53_INT_TIPO_" + sufijo)
                .nombre("Cesión de derechos de imagen")
                .descripcion("Tipo de integración UC-53")
                .plantilla("""
                        CONTRATO DE {{tipo}}

                        Proyecto: {{proyecto}}
                        Director emisor: {{director}}
                        Partes:
                        {{partes}}

                        {{clausulas_adicionales}}
                        """)
                .activo(true)
                .build();
        em.persist(tipo);

        em.flush();

        ContratoGeneradoResponse response = generarContratoService.generar(
                director.getIdUsuario(),
                perfilDirector.getIdPerfil(),
                proyecto.getIdProyecto(),
                new GenerarContratoRequest(
                        tipo.getIdTipoAcuerdo(),
                        List.of(miembroProyecto.getIdMiembro()),
                        List.of("Clausula inicial")));

        em.flush();
        em.clear();

        return new Datos(
                proyecto,
                tipo,
                director,
                miembro,
                perfilDirector,
                perfilMiembro,
                emisor,
                miembroProyecto,
                miembroNoParteProyecto,
                response.idContrato(),
                response);
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

    private record Datos(
            Proyecto proyecto,
            TipoAcuerdoContrato tipo,
            Usuario director,
            Usuario miembro,
            Perfil perfilDirector,
            Perfil perfilMiembro,
            MiembroProyecto emisor,
            MiembroProyecto miembroProyecto,
            MiembroProyecto miembroNoParteProyecto,
            Long idContrato,
            ContratoGeneradoResponse contratoInicial) {
    }
}
