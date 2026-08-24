package org.mgroko.backend.repositorio;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para tests de integración contra PostgreSQL real (Testcontainers),
 * usando el patrón "contenedor singleton".
 *
 * El contenedor es estático y se levanta una única vez por JVM (al cargar
 * la primera clase que herede de esta base); todas las clases de test lo
 * comparten y Ryuk lo detiene al finalizar la ejecución. Como además las
 * subclases quedan con configuración de contexto idéntica, Spring cachea
 * un único ApplicationContext: Flyway corre una sola vez en todo el build.
 *
 * Contrapartida del patrón: al ser la misma BD para todos los tests, las
 * filas que persista un test son visibles para los demás. Los tests que
 * insertan datos deben anotarse con @Transactional para revertirlos al
 * terminar cada método.
 *
 * Requiere Docker: si no está disponible, los tests fallan.
 */
abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registrarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
