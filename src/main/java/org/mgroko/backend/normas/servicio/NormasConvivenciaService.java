package org.mgroko.backend.normas.servicio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.normas.dto.NormasConvivenciaDocumento;
import org.mgroko.backend.normas.dto.NormasConvivenciaResponse;
import org.mgroko.backend.normas.dto.NormasConvivenciaVersionResumen;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publica las normas de convivencia de la plataforma.
 *
 * <p>El contenido es la fuente canónica de la página pública y del capítulo
 * del manual. Se carga una sola vez desde el classpath y se mantiene en
 * memoria; no hay tabla ni migración asociada.</p>
 */
@Service
public class NormasConvivenciaService {

    private final ObjectMapper objectMapper;
    private final Resource recurso;
    private volatile NormasConvivenciaDocumento documento;

    public NormasConvivenciaService(
            ObjectMapper objectMapper,
            @Value("classpath:normas-convivencia/normas-convivencia.json") Resource recurso) {
        this.objectMapper = objectMapper;
        this.recurso = recurso;
    }

    /**
     * Versión vigente: la última publicada sin fecha de fin.
     */
    public NormasConvivenciaResponse vigente() {
        NormasConvivenciaDocumento doc = documento();
        NormasConvivenciaDocumento.Version version = doc.versiones().stream()
                .filter(v -> v.vigenteHasta() == null || v.vigenteHasta().isBlank())
                .max(Comparator.comparing(NormasConvivenciaDocumento.Version::vigenteDesde))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No hay una versión vigente de las normas de convivencia."));

        return respuesta(doc, version);
    }

    /**
     * Historial completo, de la versión más reciente a la más antigua.
     */
    public List<NormasConvivenciaVersionResumen> versiones() {
        return documento().versiones().stream()
                .sorted(Comparator.comparing(
                        NormasConvivenciaDocumento.Version::vigenteDesde).reversed())
                .map(this::resumen)
                .toList();
    }

    /**
     * Versión puntual, para enlaces históricos.
     */
    public NormasConvivenciaResponse version(String version) {
        NormasConvivenciaDocumento doc = documento();
        NormasConvivenciaDocumento.Version encontrada = doc.versiones().stream()
                .filter(v -> v.version() != null && v.version().equalsIgnoreCase(version))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la versión " + version
                                + " de las normas de convivencia."));

        return respuesta(doc, encontrada);
    }

    private NormasConvivenciaResponse respuesta(
            NormasConvivenciaDocumento doc,
            NormasConvivenciaDocumento.Version version) {

        List<NormasConvivenciaVersionResumen> historial = doc.versiones().stream()
                .sorted(Comparator.comparing(
                        NormasConvivenciaDocumento.Version::vigenteDesde).reversed())
                .map(this::resumen)
                .toList();

        return new NormasConvivenciaResponse(
                doc.titulo(),
                version.version(),
                version.vigenteDesde(),
                version.vigenteHasta(),
                version.regla(),
                version.secciones(),
                historial);
    }

    private NormasConvivenciaVersionResumen resumen(NormasConvivenciaDocumento.Version version) {
        return new NormasConvivenciaVersionResumen(
                version.version(),
                version.vigenteDesde(),
                version.vigenteHasta(),
                version.cambios());
    }

    private NormasConvivenciaDocumento documento() {
        NormasConvivenciaDocumento actual = documento;
        if (actual != null) {
            return actual;
        }

        synchronized (this) {
            if (documento == null) {
                try (InputStream entrada = recurso.getInputStream()) {
                    documento = objectMapper.readValue(entrada, NormasConvivenciaDocumento.class);
                } catch (IOException ex) {
                    throw new IllegalStateException(
                            "No se pudieron cargar las normas de convivencia.", ex);
                }
            }
            return documento;
        }
    }
}
