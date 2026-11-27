package org.mgroko.backend.usuario.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.auditoria.servicio.AuditoriaSesionService;
import org.mgroko.backend.conexiones.servicio.CancelarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.EliminarConexionService;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.servicio.EliminarImagenesPerfilService;
import org.mgroko.backend.proyectos.servicio.BajaMembresiaProyectoService;
import org.mgroko.backend.proyectos.servicio.CancelarProyectoService;
import org.mgroko.backend.publicaciones.servicio.PublicacionBajaSistemaService;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.TokenAccesoEmailRepository;
import org.mgroko.backend.repositorio.UsuarioIdentidadRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Ejecuta la baja definitiva de una cuenta (UC-07) o de un perfil (UC-12).
 *
 * <p>El usuario y el perfil no se borran fisicamente: se anonimizan y quedan
 * como tombstone para preservar FKs, historiales y nombres artisticos. Las
 * publicaciones independientes se eliminan y las conexiones se dan de baja
 * logicamente.</p>
 */
@Service
public class BajaDefinitivaService {

    public static final String MOTIVO_BAJA_PERFIL = "Eliminación de perfil";
    private static final String BIOGRAFIA_ELIMINADA = "Perfil eliminado";

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final CancelarProyectoService cancelarProyectoService;
    private final BajaMembresiaProyectoService bajaMembresiaProyectoService;
    private final PublicacionBajaSistemaService publicacionBajaSistemaService;
    private final EliminarConexionService eliminarConexionService;
    private final CancelarSolicitudConexionService cancelarSolicitudConexionService;
    private final EliminarImagenesPerfilService eliminarImagenesPerfilService;
    private final UsuarioIdentidadRepository usuarioIdentidadRepository;
    private final TokenAccesoEmailRepository tokenAccesoEmailRepository;
    private final NotificacionRepository notificacionRepository;
    private final GeneroRepository generoRepository;
    private final AuditoriaSesionService auditoriaSesionService;
    private final EntityManager entityManager;
    private final Clock reloj;

    public BajaDefinitivaService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            CancelarProyectoService cancelarProyectoService,
            BajaMembresiaProyectoService bajaMembresiaProyectoService,
            PublicacionBajaSistemaService publicacionBajaSistemaService,
            EliminarConexionService eliminarConexionService,
            CancelarSolicitudConexionService cancelarSolicitudConexionService,
            EliminarImagenesPerfilService eliminarImagenesPerfilService,
            UsuarioIdentidadRepository usuarioIdentidadRepository,
            TokenAccesoEmailRepository tokenAccesoEmailRepository,
            NotificacionRepository notificacionRepository,
            GeneroRepository generoRepository,
            AuditoriaSesionService auditoriaSesionService,
            EntityManager entityManager,
            Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.cancelarProyectoService = cancelarProyectoService;
        this.bajaMembresiaProyectoService = bajaMembresiaProyectoService;
        this.publicacionBajaSistemaService = publicacionBajaSistemaService;
        this.eliminarConexionService = eliminarConexionService;
        this.cancelarSolicitudConexionService = cancelarSolicitudConexionService;
        this.eliminarImagenesPerfilService = eliminarImagenesPerfilService;
        this.usuarioIdentidadRepository = usuarioIdentidadRepository;
        this.tokenAccesoEmailRepository = tokenAccesoEmailRepository;
        this.notificacionRepository = notificacionRepository;
        this.generoRepository = generoRepository;
        this.auditoriaSesionService = auditoriaSesionService;
        this.entityManager = entityManager;
        this.reloj = reloj;
    }

    @Transactional
    public ResumenBajaDefinitiva expirarCuentasVencidas(int diasBaja) {
        LocalDateTime fechaLimite = LocalDateTime.now(reloj).minusDays(diasBaja);
        List<Usuario> vencidos = usuarioRepository
                .findByEstadoAndFechaSolicitudBajaBefore(
                        EstadoUsuario.PendienteBaja, fechaLimite);

        ResumenBajaDefinitiva resumen = ResumenBajaDefinitiva.vacio();
        for (Usuario usuario : vencidos) {
            resumen = resumen.sumar(procesarUsuario(usuario));
        }

        return resumen;
    }

    @Transactional
    public ResumenBajaDefinitiva expirarPerfilesVencidos(int diasBaja) {
        LocalDateTime fechaLimite = LocalDateTime.now(reloj).minusDays(diasBaja);
        List<Perfil> vencidos = perfilRepository
                .findByEstadoAndFechaSolicitudBajaBefore(
                        EstadoPerfil.PendienteBaja, fechaLimite);

        ResumenBajaDefinitiva resumen = ResumenBajaDefinitiva.vacio();
        for (Perfil perfil : vencidos) {
            resumen = resumen.sumar(
                    procesarPerfil(perfil, true, new LinkedHashSet<>()));
        }

        return resumen;
    }

    @Transactional
    public ResumenBajaDefinitiva expirarTodasLasBajas(int diasBaja) {
        return expirarCuentasVencidas(diasBaja)
                .sumar(expirarPerfilesVencidos(diasBaja));
    }

    private ResumenBajaDefinitiva procesarUsuario(Usuario usuario) {
        Long idUsuario = usuario.getIdUsuario();
        auditoriaSesionService.marcarOperador(idUsuario);

        List<Perfil> perfiles = perfilRepository.findByUsuarioIdUsuario(idUsuario);

        // Primero se registra la baja con el nombre original del operador.
        // La anonimizacion se hace al final, en una actualizacion separada.
        usuario.setEstado(EstadoUsuario.Baja);
        usuarioRepository.saveAndFlush(usuario);

        Set<Long> proyectosCancelados = new LinkedHashSet<>();
        ResumenBajaDefinitiva resumen = ResumenBajaDefinitiva.vacio();
        for (Perfil perfil : perfiles) {
            resumen = resumen.sumar(
                    procesarPerfil(perfil, false, proyectosCancelados));
        }

        limpiarDatosPersonalesUsuario(usuario);
        anonimizarUsuario(usuario);

        return new ResumenBajaDefinitiva(
                1,
                resumen.perfiles(),
                resumen.proyectosCancelados(),
                resumen.membresiasBaja(),
                resumen.publicacionesEliminadas(),
                resumen.conexionesEliminadas());
    }

    private ResumenBajaDefinitiva procesarPerfil(
            Perfil perfil,
            boolean marcarOperador,
            Set<Long> proyectosCanceladosEnLaBaja) {
        if (perfil == null || perfil.getIdPerfil() == null) {
            return ResumenBajaDefinitiva.vacio();
        }

        if (marcarOperador
                && perfil.getUsuario() != null
                && perfil.getUsuario().getIdUsuario() != null) {
            auditoriaSesionService.marcarOperador(
                    perfil.getUsuario().getIdUsuario());
        }

        Long idPerfil = perfil.getIdPerfil();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        List<MiembroProyecto> membresias = miembroProyectoRepository
                .findActivosDePerfilConProyectoYRol(idPerfil);

        Set<Long> proyectosACancelar = new LinkedHashSet<>();
        Set<Long> proyectosConBaja = new LinkedHashSet<>();

        for (MiembroProyecto membresia : membresias) {
            EstadoProyecto estado = membresia.getProyecto().getEstado();
            boolean esDirector = RolProyectoUsuario.desde(
                    membresia.getRolProyecto()) == RolProyectoUsuario.DIRECTOR;

            if (esDirector && esCancelable(estado)) {
                proyectosACancelar.add(membresia.getProyecto().getIdProyecto());
            }

            if (estado == EstadoProyecto.PUBLICADO
                    || estado == EstadoProyecto.CONFIRMADO
                    || proyectosCanceladosEnLaBaja.contains(
                            membresia.getProyecto().getIdProyecto())) {
                proyectosConBaja.add(membresia.getProyecto().getIdProyecto());
            }
        }

        int proyectosCancelados = 0;
        for (Long idProyecto : proyectosACancelar) {
            if (cancelarProyectoService.cancelarPorBajaPerfil(
                    idProyecto, MOTIVO_BAJA_PERFIL, idPerfil)) {
                proyectosCancelados++;
            }
        }

        // Los proyectos cancelados tambien deben cerrar la participacion del
        // perfil eliminado. Los proyectos finalizados/cancelados previamente
        // se dejan intactos para conservar el historial.
        proyectosConBaja.addAll(proyectosACancelar);
        proyectosCanceladosEnLaBaja.addAll(proyectosACancelar);

        int membresiasBaja = 0;
        for (Long idProyecto : proyectosConBaja) {
            if (bajaMembresiaProyectoService.darDeBajaPorSistema(
                    idProyecto, idPerfil, MOTIVO_BAJA_PERFIL, ahora)) {
                membresiasBaja++;
            }
        }

        int publicacionesEliminadas =
                publicacionBajaSistemaService.procesarPerfil(perfil);
        int conexionesEliminadas = eliminarConexionService
                .eliminarPorBajaPerfil(idPerfil);
        cancelarSolicitudConexionService.cancelarPorBajaPerfil(idPerfil);

        perfil.getHabilidades().clear();
        perfil.getCaracteristicas().clear();

        eliminarImagenesPerfilService.eliminarImagenesDePerfil(perfil);

        perfil.setEstado(EstadoPerfil.Baja);
        perfil.setBiografia(BIOGRAFIA_ELIMINADA);
        perfil.setUltimaActividad(null);
        perfilRepository.saveAndFlush(perfil);

        return new ResumenBajaDefinitiva(
                0,
                1,
                proyectosCancelados,
                membresiasBaja,
                publicacionesEliminadas,
                conexionesEliminadas);
    }

    private boolean esCancelable(EstadoProyecto estado) {
        return estado == EstadoProyecto.BORRADOR
                || estado == EstadoProyecto.PUBLICADO
                || estado == EstadoProyecto.CONFIRMADO;
    }

    private void limpiarDatosPersonalesUsuario(Usuario usuario) {
        Long idUsuario = usuario.getIdUsuario();

        usuarioIdentidadRepository.deleteByUsuario_IdUsuario(idUsuario);
        tokenAccesoEmailRepository.deleteByUsuario_IdUsuario(idUsuario);
        notificacionRepository.deleteByUsuarioDestino_IdUsuario(idUsuario);

        entityManager.createNativeQuery(
                "DELETE FROM notificacion_preferencia WHERE id_usuario = :id")
                .setParameter("id", idUsuario)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM bloqueo_agenda
                WHERE id_agenda IN (
                    SELECT id_agenda FROM agenda WHERE id_usuario = :id
                )
                """)
                .setParameter("id", idUsuario)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM jornada_agenda
                WHERE id_agenda IN (
                    SELECT id_agenda FROM agenda WHERE id_usuario = :id
                )
                """)
                .setParameter("id", idUsuario)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM agenda WHERE id_usuario = :id")
                .setParameter("id", idUsuario)
                .executeUpdate();
    }

    private void anonimizarUsuario(Usuario usuario) {
        Long idUsuario = usuario.getIdUsuario();

        usuario.setNombre("Usuario");
        usuario.setApellido("Eliminado");
        // El DNI anonimizado debe seguir siendo numerico y caber en 15 digitos
        // por los CHECK de la BD.
        long sufijoDni = Math.abs(idUsuario) % 100_000_000_000_000L;
        usuario.setDni("9" + String.format("%014d", sufijoDni));
        usuario.setCorreo("eliminado+" + idUsuario + "@modalink.invalid");
        usuario.setFechaNacimiento(LocalDate.of(1900, 1, 1));
        usuario.setPasswordHash(null);
        usuario.setUbicacion(null);
        usuario.setMotivoDeshabilitacion(null);
        usuario.setFechaHastaDeshabilitacion(null);
        usuario.setEstado(EstadoUsuario.Baja);

        generoRepository.findByCodigoIgnoreCase("NO_DECIRLO")
                .ifPresent(usuario::setGenero);

        usuarioRepository.saveAndFlush(usuario);
    }
}
