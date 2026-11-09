package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.GenerarContratoRequest;
import org.mgroko.backend.contratos.servicio.GenerarContratoService;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.ContratoEvento;
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
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Fixture compartido de UC-55: proyecto PUBLICADO con contrato PENDIENTE_FIRMA
 * entre el director emisor y un miembro destinatario.
 */
abstract class AbstractFirmaContratoIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    protected EntityManager em;

    @Autowired
    protected GenerarContratoService generarContratoService;

    @Autowired
    protected ContratoVersionRepository contratoVersionRepository;

    @Autowired
    protected ContratoEventoRepository contratoEventoRepository;

    @Autowired
    protected FirmaContratoRepository firmaContratoRepository;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected RolGlobalRepository rolGlobalRepository;

    @Autowired
    protected GeneroRepository generoRepository;

    protected Datos crearProyectoConContrato() {
        long sufijo = System.nanoTime();

        Profesion profesion = Profesion.builder()
                .codigo("UC55_" + sufijo)
                .nombre("Profesional UC55 " + sufijo)
                .build();
        em.persist(profesion);

        long baseDni = Math.abs(sufijo % 100_000_000L);
        Usuario director = guardarUsuario("uc55.dir." + sufijo + "@example.com",
                String.format("%08d", baseDni));
        Usuario miembro = guardarUsuario("uc55.miem." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 1) % 100_000_000L));
        Usuario noParte = guardarUsuario("uc55.noparte." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 2) % 100_000_000L));
        Usuario ajeno = guardarUsuario("uc55.ajeno." + sufijo + "@example.com",
                String.format("%08d", (baseDni + 3) % 100_000_000L));

        Perfil perfilDirector = perfil(
                "Directora UC55 " + sufijo, director, profesion);
        Perfil perfilMiembro = perfil(
                "Miembro UC55 " + sufijo, miembro, profesion);
        Perfil perfilNoParte = perfil(
                "No parte UC55 " + sufijo, noParte, profesion);
        Perfil perfilAjeno = perfil(
                "Ajeno UC55 " + sufijo, ajeno, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC55 " + sufijo)
                .descripcion("Proyecto de integración UC-55")
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

        MiembroProyecto noParteProyecto = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilNoParte)
                .rolProyecto(rol("MIEMBRO"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        em.persist(noParteProyecto);

        TipoAcuerdoContrato tipo = TipoAcuerdoContrato.builder()
                .codigo("UC55_TIPO_" + sufijo)
                .nombre("Cesión de derechos de imagen")
                .descripcion("Tipo de integración UC-55")
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

        ContratoGeneradoResponse response = generarContratoService.generar(
                director.getIdUsuario(),
                perfilDirector.getIdPerfil(),
                proyecto.getIdProyecto(),
                new GenerarContratoRequest(
                        tipo.getIdTipoAcuerdo(),
                        List.of(miembroProyecto.getIdMiembro()),
                        List.of("Cláusula UC-55")));

        em.flush();
        em.clear();

        return new Datos(
                proyecto,
                tipo,
                director,
                miembro,
                noParte,
                ajeno,
                perfilDirector,
                perfilMiembro,
                perfilNoParte,
                perfilAjeno,
                emisor,
                miembroProyecto,
                noParteProyecto,
                response.idContrato());
    }

    protected Contrato contrato(Long idContrato) {
        return em.find(Contrato.class, idContrato);
    }

    protected FirmaContrato firma(Long idContrato, Long idMiembro) {
        return firmaContratoRepository
                .findByIdContratoAndIdMiembro(idContrato, idMiembro)
                .orElseThrow();
    }

    protected List<FirmaContrato> firmas(Long idContrato) {
        return firmaContratoRepository
                .findByIdContratoOrderByIdMiembroAsc(idContrato);
    }

    protected List<ContratoEvento> eventos(Long idContrato) {
        return contratoEventoRepository
                .findByContrato_IdContratoOrderByIdEventoAsc(idContrato);
    }

    protected List<Notificacion> notificaciones(Long idContrato) {
        return em.createQuery("""
                        SELECT n FROM Notificacion n
                        WHERE n.entidadTipo = 'CONTRATO'
                          AND n.entidadId = :idContrato
                        ORDER BY n.idNotificacion
                        """, Notificacion.class)
                .setParameter("idContrato", idContrato)
                .getResultList();
    }

    protected void cancelarFirma(Long idContrato, Long idMiembro) {
        em.createNativeQuery("""
                UPDATE firma_contrato
                   SET estado_firma = 'CANCELADA'
                 WHERE id_contrato = :idContrato
                   AND id_miembro = :idMiembro
                """)
                .setParameter("idContrato", idContrato)
                .setParameter("idMiembro", idMiembro)
                .executeUpdate();

        em.flush();
        em.clear();
    }

    protected void cambiarEstadoProyecto(Datos datos, EstadoProyecto estado) {
        em.createNativeQuery("""
                UPDATE proyecto
                   SET estado = :estado
                 WHERE id_proyecto = :idProyecto
                """)
                .setParameter("estado", estado.name())
                .setParameter("idProyecto", datos.proyecto().getIdProyecto())
                .executeUpdate();

        em.flush();
        em.clear();
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
                .biografia("Perfil de integración UC-55")
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

    protected record Datos(
            Proyecto proyecto,
            TipoAcuerdoContrato tipo,
            Usuario director,
            Usuario miembro,
            Usuario noParte,
            Usuario ajeno,
            Perfil perfilDirector,
            Perfil perfilMiembro,
            Perfil perfilNoParte,
            Perfil perfilAjeno,
            MiembroProyecto emisor,
            MiembroProyecto miembroProyecto,
            MiembroProyecto noParteProyecto,
            Long idContrato) {
    }
}
