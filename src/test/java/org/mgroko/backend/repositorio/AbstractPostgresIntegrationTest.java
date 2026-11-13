package org.mgroko.backend.repositorio;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Base para tests de integración contra PostgreSQL real (Testcontainers),
 * usando el patrón "contenedor singleton".
 *
 * El contenedor es estático y se levanta una única vez por JVM (al cargar
 * la primera clase que herede de esta base); todas las clases de test lo
 * comparten y Ryuk lo detiene al finalizar la ejecución. Como además las
 * subclases quedan con configuración de contexto idéntica, Spring cachea
 * un único ApplicationContext: Modalink.sql.
 *
 * Contrapartida del patrón: al ser la misma BD para todos los tests, las
 * filas que persista un test son visibles para los demás. Los tests que
 * insertan datos deben anotarse con @Transactional para revertirlos al
 * terminar cada método.
 *
 * Requiere Docker: si no está disponible, los tests fallan.
 */
abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(prepararSchemaLf()),
                    "/docker-entrypoint-initdb.d/01-schema.sql");

    static {
        POSTGRES.start();
    }

    /**
     * Normaliza el script a LF y sin BOM en un archivo temporal. Evita
     * problemas de Testcontainers con CRLF en Windows/OneDrive.
     */
    private static Path prepararSchemaLf() {
        try (InputStream entrada = AbstractPostgresIntegrationTest.class
                .getClassLoader().getResourceAsStream("ModaLinkBD.sql")) {

            if (entrada == null) {
                throw new IllegalStateException(
                        "No se encontró ModaLinkBD.sql en el classpath.");
            }

            String sql = new String(entrada.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\uFEFF", "")
                    .replace("\r\n", "\n")
                    .replace("\r", "\n");

            Path archivo = Files.createTempFile("modalink-schema-", ".sql");
            Files.writeString(archivo, sql, StandardCharsets.UTF_8);
            archivo.toFile().deleteOnExit();
            return archivo;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @DynamicPropertySource
    static void registrarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
