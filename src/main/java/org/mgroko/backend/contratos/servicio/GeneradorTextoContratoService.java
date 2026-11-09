package org.mgroko.backend.contratos.servicio;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.contratos.mapper.ContratoMapper;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.springframework.stereotype.Service;

/**
 * Genera el texto legal de un contrato a partir de la plantilla del tipo de
 * acuerdo y de los datos del proyecto, el director y las partes.
 *
 * <p>El reemplazo de placeholders se hace en una sola pasada sobre la
 * plantilla. Asi, si una clausula adicional contiene texto como
 * {@code {{proyecto}}}, se conserva literalmente y no se interpreta.</p>
 */
@Service
public class GeneradorTextoContratoService {

    private static final int MAX_TEXTO = 100_000;
    private static final int MAX_NOMBRE = 50;
    private static final int MAX_DESCRIPCION = 200;

    private static final Pattern TOKEN = Pattern.compile(
            "\\{\\{(proyecto|director|partes|clausulas_adicionales|tipo)\\}\\}");

    public String generar(
            ContratoContextoService.ContextoContrato contexto,
            List<String> clausulasAdicionales) {

        return generar(
                contexto.proyecto(),
                contexto.tipoAcuerdo(),
                contexto.emisor(),
                contexto.destinatarios(),
                clausulasAdicionales);
    }

    public String generar(
            Proyecto proyecto,
            TipoAcuerdoContrato tipoAcuerdo,
            MiembroProyecto emisor,
            List<MiembroProyecto> destinatarios,
            List<String> clausulasAdicionales) {

        String plantilla = tipoAcuerdo.getPlantilla();
        boolean plantillaConClausulas = plantilla != null
                && plantilla.contains("{{clausulas_adicionales}}");

        if (plantilla == null || plantilla.isBlank()) {
            plantilla = plantillaGenerica();
            plantillaConClausulas = true;
        }

        String partes = destinatarios.stream()
                .map(this::describir)
                .collect(Collectors.joining("\n- ", "- ", ""));

        if (partes.isBlank()) {
            partes = "- Sin destinatarios.";
        }

        String clausulas = renderClausulas(clausulasAdicionales);

        String texto = render(plantilla, Map.of(
                "proyecto", describirProyecto(proyecto),
                "director", describir(emisor),
                "partes", partes,
                "clausulas_adicionales", clausulas,
                "tipo", tipoAcuerdo.getNombre()));

        if (!plantillaConClausulas) {
            texto = texto + "\n\nCLÁUSULAS ADICIONALES\n" + clausulas;
        }

        validarTexto(texto);
        return texto;
    }

    public void validarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ContratoNoGenerableException(
                    "CONTRATO_TEXTO_INVALIDO",
                    "No se pudo generar el texto del contrato.");
        }

        if (texto.codePointCount(0, texto.length()) > MAX_TEXTO) {
            throw new ContratoNoGenerableException(
                    "CONTRATO_TEXTO_INVALIDO",
                    "El texto del contrato supera el máximo permitido.");
        }
    }

    public String generarNombre(ContratoContextoService.ContextoContrato contexto) {
        return generarNombre(contexto.proyecto(), contexto.tipoAcuerdo());
    }

    public String generarNombre(Proyecto proyecto, TipoAcuerdoContrato tipoAcuerdo) {
        String nombre = tipoAcuerdo.getNombre() + " - " + proyecto.getNombre();
        return truncar(nombre, MAX_NOMBRE);
    }

    public String generarDescripcion(ContratoContextoService.ContextoContrato contexto) {
        return generarDescripcion(contexto.proyecto());
    }

    public String generarDescripcion(Proyecto proyecto) {
        return truncar(
                "Contrato generado para el proyecto " + proyecto.getNombre(),
                MAX_DESCRIPCION);
    }

    private String render(String plantilla, Map<String, String> valores) {
        Matcher matcher = TOKEN.matcher(plantilla);
        StringBuilder resultado = new StringBuilder();

        while (matcher.find()) {
            String valor = valores.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(resultado, Matcher.quoteReplacement(valor));
        }
        matcher.appendTail(resultado);

        return resultado.toString();
    }

    private String renderClausulas(List<String> clausulas) {
        List<String> limpias = clausulas == null
                ? List.of()
                : clausulas.stream()
                        .filter(c -> c != null && !c.isBlank())
                        .map(String::trim)
                        .toList();

        if (limpias.isEmpty()) {
            return "Sin cláusulas adicionales.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limpias.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(i + 1).append(". ").append(limpias.get(i));
        }
        return sb.toString();
    }

    private String describirProyecto(Proyecto proyecto) {
        if (proyecto == null) {
            return "Sin proyecto";
        }
        if (proyecto.getDescripcion() == null || proyecto.getDescripcion().isBlank()) {
            return proyecto.getNombre();
        }
        return proyecto.getNombre() + " — " + proyecto.getDescripcion();
    }

    private String describir(MiembroProyecto miembro) {
        if (miembro == null) {
            return "Sin parte";
        }

        Perfil perfil = miembro.getPerfil();
        Usuario usuario = perfil != null ? perfil.getUsuario() : null;

        String nombre = ContratoMapper.nombreCompleto(perfil);
        if (nombre == null || nombre.isBlank()) {
            nombre = "Sin nombre";
        }

        StringBuilder sb = new StringBuilder(nombre);

        if (usuario != null && usuario.getDni() != null && !usuario.getDni().isBlank()) {
            sb.append(" (DNI ").append(usuario.getDni()).append(')');
        }

        if (perfil != null && perfil.getNombreArtistico() != null
                && !perfil.getNombreArtistico().isBlank()) {
            sb.append(" — @").append(perfil.getNombreArtistico());
        }

        if (perfil != null && perfil.getProfesion() != null
                && perfil.getProfesion().getNombre() != null) {
            sb.append(" (").append(perfil.getProfesion().getNombre()).append(')');
        }

        return sb.toString();
    }

    private String plantillaGenerica() {
        return """
                CONTRATO DE {{tipo}}

                Proyecto: {{proyecto}}
                Director emisor: {{director}}
                Partes:
                {{partes}}

                Las partes acuerdan las condiciones del presente contrato.

                {{clausulas_adicionales}}
                """;
    }

    private String truncar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        if (valor.codePointCount(0, valor.length()) <= maximo) {
            return valor;
        }
        int fin = valor.offsetByCodePoints(0, maximo);
        return valor.substring(0, fin);
    }
}
