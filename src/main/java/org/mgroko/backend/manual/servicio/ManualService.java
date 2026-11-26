package org.mgroko.backend.manual.servicio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import org.mgroko.backend.manual.dto.ManualCapituloResponse;
import org.mgroko.backend.manual.dto.ManualCapituloResumen;
import org.mgroko.backend.manual.dto.ManualError;
import org.mgroko.backend.manual.dto.ManualIndiceDocumento;
import org.mgroko.backend.manual.dto.ManualIndiceResponse;
import org.mgroko.backend.manual.dto.ManualResultado;
import org.mgroko.backend.manual.dto.ManualSeccion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Sirve el manual de usuario desde los Markdown de {@code docs/manual}.
 *
 * <p>El índice declara qué capítulos ve cada rol. El contenido se renderiza a
 * HTML con CommonMark y se escapa cualquier HTML crudo del Markdown. La
 * búsqueda es simple, sobre el texto de cada sección, con límite de resultados
 * para mantener la respuesta acotada.</p>
 */
@Service
public class ManualService {

    private static final String ROL_USUARIO = "USUARIO";
    private static final String ROL_ADMIN = "ADMINISTRADOR";
    private static final int MAX_RESULTADOS_BUSQUEDA = 25;
    private static final Pattern TITULO_HTML = Pattern.compile(
            "<h([23]) id=\"([^\"]+)\">(.*?)</h\\1>", Pattern.DOTALL);

    private final ObjectMapper objectMapper;
    private final Resource recursoIndice;
    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Map<String, String> cacheMarkdown = new ConcurrentHashMap<>();
    private final Map<Boolean, List<ManualError>> cacheErrores = new ConcurrentHashMap<>();

    private volatile ManualIndiceDocumento indice;

    public ManualService(
            ObjectMapper objectMapper,
            @Value("classpath:manual/indice.json") Resource recursoIndice) {
        this.objectMapper = objectMapper;
        this.recursoIndice = recursoIndice;

        List<org.commonmark.Extension> extensiones = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensiones).build();
        this.renderer = HtmlRenderer.builder()
                .extensions(extensiones)
                .escapeHtml(true)
                .attributeProviderFactory(contexto -> new TitulosConId())
                .build();
    }

    public ManualIndiceResponse indice(boolean admin) {
        List<ManualCapituloResumen> capitulos = capitulosPermitidos(admin).stream()
                .map(this::resumen)
                .toList();
        return new ManualIndiceResponse(documento().version(), capitulos);
    }

    public ManualCapituloResponse capitulo(String id, boolean admin) {
        List<ManualIndiceDocumento.Capitulo> permitidos = capitulosPermitidos(admin);
        int posicion = -1;
        for (int i = 0; i < permitidos.size(); i++) {
            if (permitidos.get(i).id().equals(id)) {
                posicion = i;
                break;
            }
        }

        if (posicion < 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Capítulo del manual no encontrado.");
        }

        ManualIndiceDocumento.Capitulo capitulo = permitidos.get(posicion);
        String markdown = leerMarkdown(capitulo.archivo());
        String cuerpo = markdown
                .replaceFirst("(?m)^#\\s+.*\\R?", "")
                .replaceAll("(?m)^\\[CAPTURA:.*\\]\\s*$", "");
        String html = renderer.render(parser.parse(cuerpo));

        ManualCapituloResumen anterior = posicion > 0
                ? resumen(permitidos.get(posicion - 1))
                : null;
        ManualCapituloResumen siguiente = posicion < permitidos.size() - 1
                ? resumen(permitidos.get(posicion + 1))
                : null;

        return new ManualCapituloResponse(
                capitulo.id(),
                capitulo.numero(),
                capitulo.etiqueta(),
                capitulo.titulo(),
                html,
                extraerSecciones(html),
                anterior,
                siguiente);
    }

    public List<ManualResultado> buscar(String consulta, boolean admin) {
        String buscado = normalizar(consulta);
        if (buscado.length() < 2) {
            return List.of();
        }

        List<ManualResultado> resultados = new ArrayList<>();
        for (ManualIndiceDocumento.Capitulo capitulo : capitulosPermitidos(admin)) {
            String markdown = leerMarkdown(capitulo.archivo());
            for (SeccionTexto seccion : seccionesMarkdown(markdown)) {
                if (normalizar(seccion.texto()).contains(buscado)) {
                    resultados.add(new ManualResultado(
                            capitulo.id(),
                            capitulo.titulo(),
                            slug(seccion.titulo()),
                            seccion.titulo(),
                            fragmento(seccion.texto(), buscado)));
                    if (resultados.size() >= MAX_RESULTADOS_BUSQUEDA) {
                        return resultados;
                    }
                }
            }
        }
        return resultados;
    }

    /**
     * Catálogo de errores frecuentes extraído de las tablas
     * {@code Mensaje | Causa | Solución} del manual. Se deduplica por mensaje
     * normalizado y se filtra por los capítulos visibles para el rol.
     */
    public List<ManualError> errores(boolean admin) {
        return cacheErrores.computeIfAbsent(admin, this::construirErrores);
    }

    private List<ManualError> construirErrores(boolean admin) {
        Map<String, ManualError> unicos = new LinkedHashMap<>();
        int indice = 0;

        for (ManualIndiceDocumento.Capitulo capitulo : capitulosPermitidos(admin)) {
            String markdown = leerMarkdown(capitulo.archivo());
            String seccion = "Introducción";
            String[] lineas = markdown.split("\\R", -1);

            for (int i = 0; i < lineas.length; i++) {
                String linea = lineas[i].trim();

                if (linea.startsWith("## ") || linea.startsWith("### ")) {
                    seccion = linea.replaceFirst("^#{2,3}\\s+", "").trim();
                    continue;
                }

                if (!linea.startsWith("|") || i + 1 >= lineas.length) {
                    continue;
                }

                List<String> cabecera = celdas(linea);
                if (cabecera.isEmpty()
                        || !normalizar(cabecera.get(0)).contains("mensaje")
                        || !lineas[i + 1].trim().startsWith("|")
                        || !lineas[i + 1].trim().contains("---")) {
                    continue;
                }

                int j = i + 2;
                while (j < lineas.length && lineas[j].trim().startsWith("|")) {
                    List<String> fila = celdas(lineas[j]);
                    if (!fila.isEmpty()) {
                        String mensaje = limpiarMensaje(fila.get(0));
                        if (!mensaje.isBlank()) {
                            String clave = normalizar(mensaje);
                            if (!unicos.containsKey(clave)) {
                                String causa = fila.size() > 1 ? limpiarTexto(fila.get(1)) : "";
                                String solucion = fila.size() > 2 ? limpiarTexto(fila.get(2)) : "";
                                unicos.put(clave, new ManualError(
                                        capitulo.id() + "-" + indice++,
                                        capitulo.id(),
                                        capitulo.titulo(),
                                        slug(seccion),
                                        seccion,
                                        mensaje,
                                        causa,
                                        solucion));
                            }
                        }
                    }
                    j++;
                }
                i = j - 1;
            }
        }

        return List.copyOf(unicos.values());
    }

    private static List<String> celdas(String linea) {
        String limpia = linea.trim();
        if (limpia.startsWith("|")) {
            limpia = limpia.substring(1);
        }
        if (limpia.endsWith("|")) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        return Arrays.stream(limpia.split("\\|", -1))
                .map(String::trim)
                .toList();
    }

    private static String limpiarMensaje(String valor) {
        String texto = limpiarTexto(valor);
        if (texto.length() >= 2
                && ((texto.startsWith("“") && texto.endsWith("”"))
                        || (texto.startsWith("\"") && texto.endsWith("\"")))) {
            texto = texto.substring(1, texto.length() - 1).trim();
        }
        return texto;
    }

    private static String limpiarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replaceAll("\\*\\*?", "")
                .replace("`", "")
                .replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<ManualIndiceDocumento.Capitulo> capitulosPermitidos(boolean admin) {
        String rol = admin ? ROL_ADMIN : ROL_USUARIO;
        return documento().capitulos().stream()
                .filter(capitulo -> capitulo.roles().contains(rol))
                .toList();
    }

    private ManualCapituloResumen resumen(ManualIndiceDocumento.Capitulo capitulo) {
        return new ManualCapituloResumen(
                capitulo.id(),
                capitulo.numero(),
                capitulo.etiqueta(),
                capitulo.titulo());
    }

    private List<ManualSeccion> extraerSecciones(String html) {
        List<ManualSeccion> secciones = new ArrayList<>();
        Matcher matcher = TITULO_HTML.matcher(html);
        while (matcher.find()) {
            String titulo = HtmlUtils.htmlUnescape(
                    matcher.group(3).replaceAll("<[^>]+>", "")).trim();
            secciones.add(new ManualSeccion(
                    matcher.group(2),
                    titulo,
                    Integer.parseInt(matcher.group(1))));
        }
        return secciones;
    }

    private List<SeccionTexto> seccionesMarkdown(String markdown) {
        List<SeccionTexto> secciones = new ArrayList<>();
        String titulo = "Introducción";
        StringBuilder buffer = new StringBuilder();

        for (String linea : markdown.split("\\R", -1)) {
            if (linea.startsWith("## ") || linea.startsWith("### ")) {
                if (buffer.length() > 0) {
                    secciones.add(new SeccionTexto(titulo, buffer.toString()));
                }
                titulo = linea.replaceFirst("^#{2,3}\\s+", "").trim();
                buffer.setLength(0);
            } else if (!linea.startsWith("# ")) {
                buffer.append(linea).append('\n');
            }
        }

        if (buffer.length() > 0) {
            secciones.add(new SeccionTexto(titulo, buffer.toString()));
        }
        return secciones;
    }

    private String fragmento(String texto, String buscado) {
        for (String parrafo : texto.split("\\R\\s*\\R")) {
            if (normalizar(parrafo).contains(buscado)) {
                return recortar(textoPlano(parrafo));
            }
        }
        return recortar(textoPlano(texto));
    }

    private String textoPlano(String texto) {
        return texto
                .replaceAll("\\[CAPTURA:[^\\]]*\\]", " ")
                .replaceAll("[#*_`>|]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String recortar(String texto) {
        if (texto.length() <= 200) {
            return texto;
        }
        return texto.substring(0, 197).trim() + "...";
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String slug(String texto) {
        String resultado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (resultado.isEmpty()) {
            resultado = "seccion";
        }
        if (Character.isDigit(resultado.charAt(0))) {
            resultado = "seccion-" + resultado;
        }
        return resultado;
    }

    private String leerMarkdown(String archivo) {
        return cacheMarkdown.computeIfAbsent(archivo, nombre -> {
            Resource recurso = new ClassPathResource("manual/" + nombre);
            try (InputStream entrada = recurso.getInputStream()) {
                return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new IllegalStateException(
                        "No se pudo leer el capítulo del manual: " + nombre, ex);
            }
        });
    }

    private ManualIndiceDocumento documento() {
        ManualIndiceDocumento actual = indice;
        if (actual != null) {
            return actual;
        }

        synchronized (this) {
            if (indice == null) {
                try (InputStream entrada = recursoIndice.getInputStream()) {
                    indice = objectMapper.readValue(entrada, ManualIndiceDocumento.class);
                } catch (IOException ex) {
                    throw new IllegalStateException(
                            "No se pudo cargar el índice del manual.", ex);
                }
            }
            return indice;
        }
    }

    private record SeccionTexto(String titulo, String texto) {
    }

    /**
     * Agrega un id estable a cada título para que el frontend pueda enlazar
     * secciones y construir el índice interno del capítulo.
     */
    private static final class TitulosConId implements AttributeProvider {

        private final Map<String, Integer> usados = new HashMap<>();

        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            if (!(node instanceof Heading)) {
                return;
            }

            String base = slug(textoDe(node));
            int repeticiones = usados.merge(base, 1, Integer::sum);
            attributes.put("id", repeticiones == 1 ? base : base + "-" + repeticiones);
        }

        private static String textoDe(Node node) {
            StringBuilder texto = new StringBuilder();
            node.accept(new AbstractVisitor() {
                @Override
                public void visit(Text nodoTexto) {
                    texto.append(nodoTexto.getLiteral());
                }

                @Override
                public void visit(Code codigo) {
                    texto.append(codigo.getLiteral());
                }
            });
            return texto.toString().trim();
        }
    }
}
