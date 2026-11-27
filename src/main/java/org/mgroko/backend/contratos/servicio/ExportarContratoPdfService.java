package org.mgroko.backend.contratos.servicio;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.auditoria.dto.AuditoriaDetalleRequest;
import org.mgroko.backend.auditoria.servicio.AuditoriaEventoService;
import org.mgroko.backend.contratos.exception.ContratoNoEncontradoException;
import org.mgroko.backend.contratos.mapper.ContratoMapper;
import org.mgroko.backend.contratos.pdf.ContratoPdfRenderer;
import org.mgroko.backend.contratos.pdf.ContratoPdfRenderer.DatosContratoPdf;
import org.mgroko.backend.contratos.pdf.ContratoPdfRenderer.PartePdf;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.ContratoVersionRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exportación PDF de un contrato (UC-68).
 *
 * <p>La exportación del área de usuario reutiliza
 * {@link ObtenerContratoService} para aplicar exactamente la misma regla de
 * visibilidad que el detalle: director activo del proyecto o parte firmante.
 * La exportación administrativa se apoya en el permiso {@code VER_CONTRATOS}
 * del controlador de administración.</p>
 *
 * <p>El PDF se genera en memoria antes de registrar la auditoría y antes de
 * que el controlador abra la descarga, de modo que los errores de permisos,
 * filtros o generación se respondan como JSON.</p>
 */
@Service
public class ExportarContratoPdfService {

    private static final String ORIGEN_USUARIO = "USUARIO";
    private static final String ORIGEN_ADMINISTRACION = "ADMINISTRACION";

    private final ObtenerContratoService obtenerContratoService;
    private final ContratoRepository contratoRepository;
    private final FirmaContratoRepository firmaContratoRepository;
    private final ContratoVersionRepository contratoVersionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaEventoService auditoriaEventoService;
    private final Clock reloj;
    private final ContratoPdfRenderer pdfRenderer = new ContratoPdfRenderer();

    public ExportarContratoPdfService(
            ObtenerContratoService obtenerContratoService,
            ContratoRepository contratoRepository,
            FirmaContratoRepository firmaContratoRepository,
            ContratoVersionRepository contratoVersionRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaEventoService auditoriaEventoService,
            Clock reloj) {
        this.obtenerContratoService = obtenerContratoService;
        this.contratoRepository = contratoRepository;
        this.firmaContratoRepository = firmaContratoRepository;
        this.contratoVersionRepository = contratoVersionRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaEventoService = auditoriaEventoService;
        this.reloj = reloj;
    }

    /**
     * Exporta un contrato visible para el perfil activo del usuario.
     */
    @Transactional
    public byte[] exportar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato) {

        // Valida perfil activo, proyecto y visibilidad del contrato.
        obtenerContratoService.obtener(
                idUsuario, idPerfilActivo, idProyecto, idContrato);

        Contrato contrato = cargarContrato(idContrato);
        validarProyecto(contrato, idProyecto);

        return generar(contrato, idUsuario, ORIGEN_USUARIO);
    }

    /**
     * Exporta un contrato desde el área administrativa. El controlador exige
     * el permiso {@code VER_CONTRATOS}.
     */
    @Transactional
    public byte[] exportarAdministrativo(Long idUsuario, Long idContrato) {
        Contrato contrato = cargarContrato(idContrato);
        return generar(contrato, idUsuario, ORIGEN_ADMINISTRACION);
    }

    private Contrato cargarContrato(Long idContrato) {
        return contratoRepository.findByIdConPartes(idContrato)
                .orElseThrow(() -> new ContratoNoEncontradoException(idContrato));
    }

    private void validarProyecto(Contrato contrato, Long idProyecto) {
        if (contrato.getProyecto() == null
                || !idProyecto.equals(contrato.getProyecto().getIdProyecto())) {
            throw new ContratoNoEncontradoException(contrato.getIdContrato());
        }
    }

    private byte[] generar(
            Contrato contrato, Long idUsuario, String origen) {

        List<FirmaContrato> firmas = firmaContratoRepository
                .findConPartes(contrato.getIdContrato());

        int version = contrato.getVersionActual() == null
                ? 1
                : contrato.getVersionActual();

        String texto = contratoVersionRepository
                .findFirstByContrato_IdContratoAndVersion(
                        contrato.getIdContrato(), version)
                .map(versionContrato -> versionContrato.getTexto())
                .filter(valor -> valor != null && !valor.isBlank())
                .orElse(contrato.getTexto());

        LocalDateTime ahora = LocalDateTime.now(reloj);

        DatosContratoPdf datos = new DatosContratoPdf(
                contrato.getIdContrato(),
                contrato.getNombre(),
                nombreProyecto(contrato.getProyecto()),
                nombreTipoAcuerdo(contrato.getTipoAcuerdo()),
                contrato.getEstado() != null
                        ? contrato.getEstado().name()
                        : null,
                version,
                contrato.getFechaCreacion(),
                contrato.getFechaEmision(),
                contrato.getFechaModificacion(),
                contrato.getFechaBaja(),
                contrato.getFechaCancelacion(),
                contrato.getMotivoCancelacion(),
                texto,
                nombreEmisor(contrato),
                firmas.stream().map(this::toParte).toList(),
                contarFirmados(firmas),
                contarTotalPartes(firmas),
                nombreUsuario(idUsuario),
                ahora,
                ahora);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        pdfRenderer.render(datos, salida);

        registrarExportacion(contrato, origen, version);

        return salida.toByteArray();
    }

    private PartePdf toParte(FirmaContrato firma) {
        MiembroProyecto miembro = firma.getMiembro();
        Perfil perfil = miembro != null ? miembro.getPerfil() : null;
        Usuario usuario = perfil != null ? perfil.getUsuario() : null;

        return new PartePdf(
                ContratoMapper.nombreCompleto(perfil),
                usuario != null ? usuario.getDni() : null,
                firma.getRolParte() != null
                        ? firma.getRolParte().name()
                        : null,
                firma.getEstadoFirma() != null
                        ? firma.getEstadoFirma().name()
                        : null,
                firma.getFechaFirma(),
                firma.getFechaRechazo(),
                firma.getMotivoRechazo());
    }

    private void registrarExportacion(
            Contrato contrato, String origen, int version) {

        auditoriaEventoService.registrar(
                "contrato",
                contrato.getIdContrato(),
                "CONTRATO_EXPORTADO",
                "UPDATE",
                List.of(
                        new AuditoriaDetalleRequest("formato", null, "PDF"),
                        new AuditoriaDetalleRequest(
                                "idProyecto",
                                null,
                                contrato.getProyecto() != null
                                        ? String.valueOf(
                                                contrato.getProyecto().getIdProyecto())
                                        : ""),
                        new AuditoriaDetalleRequest(
                                "version",
                                null,
                                String.valueOf(version)),
                        new AuditoriaDetalleRequest("origen", null, origen)));
    }

    private String nombreEmisor(Contrato contrato) {
        MiembroProyecto emisor = contrato.getMiembroEmisor();
        Perfil perfil = emisor != null ? emisor.getPerfil() : null;
        return ContratoMapper.nombreCompleto(perfil);
    }

    private String nombreProyecto(Proyecto proyecto) {
        return proyecto != null ? proyecto.getNombre() : null;
    }

    private String nombreTipoAcuerdo(TipoAcuerdoContrato tipo) {
        return tipo != null ? tipo.getNombre() : null;
    }

    private String nombreUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return "No informado";
        }
        return usuarioRepository.findById(idUsuario)
                .map(this::nombreCompleto)
                .orElse("Usuario #" + idUsuario);
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty()
                ? "Usuario #" + usuario.getIdUsuario()
                : completo;
    }

    private int contarFirmados(List<FirmaContrato> firmas) {
        return (int) firmas.stream()
                .filter(firma -> firma.getEstadoFirma() == EstadoFirmaContrato.FIRMADO)
                .count();
    }

    private int contarTotalPartes(List<FirmaContrato> firmas) {
        return (int) firmas.stream()
                .filter(firma -> firma.getEstadoFirma() == EstadoFirmaContrato.PENDIENTE
                        || firma.getEstadoFirma() == EstadoFirmaContrato.FIRMADO)
                .count();
    }
}
