# Instrucciones de testing por módulo — ModaLink Backend

> Documento reconstruido a partir del código existente en `src/test/java/org/mgroko/backend/`. Describe las normas de testing aplicadas a cada módulo y los archivos de test que lo verifican.

## Stack de testing

| Componente | Tecnología |
|---|---|
| Framework | JUnit 5 (Jupiter) + Spring Boot Test |
| Mocking | Mockito (`@Mock`, `@InjectMocks`, `MockitoExtension`) |
| Controllers | `@WebMvcTest` + MockMvc (`spring-boot-starter-webmvc-test`) |
| Validación de DTOs | `jakarta.validation.Validator` (API pura, sin contexto Spring) |
| Integración con BD | Testcontainers (PostgreSQL 17 Alpine) |
| AssertJ | Usado en el módulo `proyectos` (aserciones encadenadas) |
| Cobertura | JaCoCo (`mvn test` → `target/site/jacoco/index.html`) |

## Convenciones generales (aplican a todos los módulos)

1. **Idioma**: métodos y nombres de test en español.
2. **Nombre de test**: patrón `metodo_condicion_resultadoEsperado`, ej. `crear_usuarioNoExiste_lanzaExcepcion`, `obtener_perfilExistente_devuelve200`.
3. **Cobertura doble**: cada caso de éxito tiene su caso de error (excepción o código HTTP 4xx).
4. **En servicios**: se verifica con `verify(...)` que **no** se persista nada en los flujos de error (`verify(repo, never()).save(any())`).
5. **Captura de datos guardados**: se usa `ArgumentCaptor` para validar que el objeto persistido tiene los campos correctos (no solo el response).
6. **Entidades de prueba**: se construyen con builders (Lombok `@Builder`), con helpers privados `usuarioActivo()`, `profesionModelo()`, `requestValido()`, etc.
7. **Validaciones de negocio**: deben verificarse en el servicio (unitario), y las validaciones Bean Validation en el DTO (`...DTOValidationTest`) y/o en el controlador (HTTP 400).
8. **Casos de uso (UC)**: los tests de funcionalidad referenciada a un caso de uso se agrupan con comentarios de sección tipo `// UC-12` o `// configurarJornada`.
9. **Excepciones de dominio**: cada error de negocio lanza una excepción específica (paquete `exception` del módulo) y el `GlobalExceptionHandler` la mapea a un código HTTP. El test del controlador valida **status + `$.message`**.

---

## Capas de testing

### 1. Validación de DTOs (`dto/*DTOValidationTest` o `dto/*ValidationTest`)
- Se instancia el `Validator` de Jakarta **directamente**: `Validation.buildDefaultValidatorFactory().getValidator()`.
- No participa Spring ni Mockito.
- Tests: request válido sin violaciones + cada violación esperada (`validator.validate(request)`).
- Se verifica el campo violado con `violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("campo"))`.

### 2. Servicios (unitarios) (`servicio/*ServiceTest`)
- `@ExtendWith(MockitoExtension.class)`.
- Repositorios y servicios dependientes como `@Mock`; la clase bajo test como `@InjectMocks`.
- Se `when(...)`-ean las dependencias por test (flujo feliz completo + casos de error).
- Se validan: guardado correcto (`ArgumentCaptor`), excepciones (`assertThrows`), no-guardado en error (`verify(..., never())`).

### 3. Mappers (`mapper/*MapperTest`)
- JUnit puro, sin mocks.
- Entidad de dominio (con builders) → DTO de respuesta.
- Casos: mapeo completo (todos los campos) y caso vacío/edge (ej. `sinCaracteristicas`).

### 4. Controladores (`controlador/*ControllerTest`)
- `@WebMvcTest(controllers = ClaseController.class)` + `@AutoConfigureMockMvc(addFilters = false)`.
- Servicios como `@MockitoBean`.
- Petición con `MockMvc`, autenticación simulada vía `request.principal(new UsernamePasswordAuthenticationToken("idUsuario", null, List.of()))`.
- Se valida: status HTTP correcto, `jsonPath` del body, y `verify(servicio, never())` cuando Bean Validation rechaza la petición antes de llegar al servicio.
- Excepciones de dominio lanzadas por el servicio mockeado → se espera el status mapeado por `GlobalExceptionHandler` + `$.message`.

### 5. Integración con base de datos (`repositorio/*IntegrationTest`)
- `@SpringBootTest` + `@Transactional` (rollback por método) sobre la clase base `AbstractPostgresIntegrationTest`.
- `AbstractPostgresIntegrationTest`: contenedor PostgreSQL `postgres:17-alpine` tipo **singleton estático** (una sola vez por JVM), datasource inyectado con `@DynamicPropertySource`. Flyway corre una sola vez. **Requiere Docker**.
- Se validan consultas derivadas, case-sensitivity y constraints únicas/referenciales del esquema.
- Norma: los tests que persisten filas **deben** anotarse `@Transactional` porque la BD es compartida entre todas las clases.

### 6. Seguridad (`security/*`)
- `JwtService` se instancia **directo** con secret de test (≥ 32 caracteres para HS256). Se prueban: generación/validación, expiración (`ExpiredJwtException`), firma manipulada (`SignatureException`), formato inválido (`MalformedJwtException`).
- `JwtAuthenticationFilterTest`: vive en el mismo paquete `security` para acceder a `doFilterInternal` (protected). Mocks de `HttpServletRequest/Response` y `FilterChain`. Después de cada test se limpia con `SecurityContextHolder.clearContext()`.
- Tests de logout con anotación personalizada `@WithMockJwtUser` (factory de `WithSecurityContext`).

---

## Tests por módulo

### Módulo `auth` (autenticación)
| Archivo | Capa | Cubre |
|---|---|---|
| `auth/AuthControllerTest` | Controller | `POST /auth/registro`, `POST /auth/login`, `GET /auth/me`. Cookie JWT en respuesta, validaciones 400, credenciales inválidas 401, perfil activo en contexto. |
| `auth/AuthServiceTest` | Servicio | Lógica de registro/login, hashing BCrypt, duplicados (correo/DNI), edad mínima, generación de token y claims. |
| `auth/UsuarioMapperTest` | Mapper | Mapeo `Usuario` → `UsuarioResponse` (incluido perfil activo). |
| `auth/dto/AuthDTOValidationTest` | Validación DTO | `RegistroRequest` y `LoginRequest`: campos obligatorios, formato de correo, longitud de contraseña, etc. |
| `auth/LogoutIntegrationTest` | Integración (WebMvcTest) | `POST /auth/logout`: 401 sin cookie/usuario inexistente/deshabilitado; 200 con cookie expirada (`Max-Age=0`). |

### Módulo `security` (seguridad / JWT)
| Archivo | Capa | Cubre |
|---|---|---|
| `security/JwtServiceTest` | Unitario | Generar/validar token, claims, subject, token expirado, firma manipulada, formato inválido. |
| `security/JwtAuthenticationFilterTest` | Unitario (mismo paquete) | Filtro sin cookie, token válido (autentica), token inválido/usuario inexistente/deshabilitado (limpia contexto y continúa cadena), perfil activo en `details`. |
| `security/SecurityConfigTest` | Config | Cadena de seguridad: endpoints públicos vs. protegidos, cookies, CSRF. |

### Módulo `perfiles` (gestión de perfiles)
| Archivo | Capa | Cubre |
|---|---|---|
| `perfiles/controlador/PerfilControllerTest` | Controller | Crear (201/400/409), listar y obtener propios (200/401/404), editar (200/400/409), eliminar (200/409), reactivar (200), activar (200 + cookie JWT/404/409), perfil activo en sesión (200/404). |
| `perfiles/controlador/ProfesionControllerTest` | Controller | Búsqueda de profesiones y características por profesión. |
| `perfiles/controlador/CaracteristicaTecnicaControllerTest` | Controller | Catálogo de características técnicas. |
| `perfiles/servicio/CrearPerfilServiceTest` | Servicio | Creación con/sin características, valores texto/enumerado/numérico, validaciones por tipo de dato y excepciones (`PerfilDuplicado`, `ProfesionNoEncontrada`, `Caracteristica*`, `Valor*`). |
| `perfiles/servicio/EditarPerfilServiceTest` | Servicio | Edición de datos y características (UC-11). |
| `perfiles/servicio/EliminarPerfilServiceTest` | Servicio | Solicitud de baja → `PendienteBaja` y cuenta regresiva 30 días (UC-12). |
| `perfiles/servicio/ReactivarPerfilServiceTest` | Servicio | Reactivación dentro del plazo (UC-12). |
| `perfiles/servicio/ActivarPerfilServiceTest` | Servicio | Perfil activo seleccionado + regeneración de JWT. |
| `perfiles/servicio/ExpirarPerfilServiceTest` | Servicio | Baja definitiva de perfiles no reactivados a tiempo. |
| `perfiles/servicio/UsuarioPerfilServiceTest` | Servicio | Listar/obtener perfiles propios, perfil activo del contexto. |
| `perfiles/servicio/CaracteristicaTecnicaServiceTest` | Servicio | Catálogo de características (búsqueda por código/unidad). |
| `perfiles/servicio/CaracteristicaPerfilHelperTest` | Servicio (helper) | Validación/construcción de `CaracteristicaPerfil` por tipo de dato. |
| `perfiles/servicio/ProfesionServiceTest` | Servicio | Búsqueda de profesiones. |
| `perfiles/mapper/PerfilMapperTest` | Mapper | `Perfil` → `PerfilResponse` (orden alfabético de características, lista vacía). |
| `perfiles/mapper/CaracteristicaTecnicaMapperTest` | Mapper | Característica → response. |
| `perfiles/mapper/ProfesionMapperTest` | Mapper | Profesión → response. |
| `perfiles/dto/CrearPerfilDTOValidationTest` | Validación DTO | Nombre artístico (vacío/corto/largo), profesión, biografía, características sin id. |
| `perfiles/dto/EditarPerfilDTOValidationTest` | Validación DTO | Reglas de validación de la edición. |

### Módulo `usuario` (datos personales y cuenta)
| Archivo | Capa | Cubre |
|---|---|---|
| `usuario/controlador/DatosPersonalesControllerTest` | Controller | `PUT /usuario/datos-personales` (UC-08): 200, 400, 401/404. |
| `usuario/servicio/DatosPersonalesServiceTest` | Servicio | Actualización de datos personales (UC-08), excepciones de dominio. |
| `usuario/servicio/ExpirarCuentaServiceTest` | Servicio | Baja definitiva de cuentas pendientes (scheduler). |
| `usuario/mapper/DatosPersonalesMapperTest` | Mapper | Mapeo de datos personales. |
| `usuario/dto/DatosPersonalesDTOValidationTest` | Validación DTO | Reglas de validación de `DatosPersonalesRequest`. |

### Módulo `calendario` (disponibilidad y jornada)
| Archivo | Capa | Cubre |
|---|---|---|
| `calendario/controlador/CalendarioControllerTest` | Controller | Endpoints de jornada, bloqueos y disponibilidad. |
| `calendario/servicio/CalendarioServiceTest` | Servicio | `obtener`, `configurarJornada` (corrida/partida, actualización, validaciones `JornadaInvalida`), `marcarNoDisponible` (UC-18: solapamientos con actividad/margen/bloqueo), `marcarDisponible` (UC-17). |
| `calendario/mapper/CalendarioMapperTest` | Mapper | Mapeo de jornada/bloqueos a responses. |
| `calendario/dto/ConfigJornadaRequestValidationTest` | Validación DTO | Reglas de la configuración de jornada. |
| `calendario/dto/MarcarNoDisponibleRequestValidationTest` | Validación DTO | Reglas del bloqueo manual. |

### Módulo `admin` (administración)
| Archivo | Capa | Cubre |
|---|---|---|
| `admin/controlador/AdminUsuarioControllerTest` | Controller | Habilitar/deshabilitar usuarios (`PATCH /admin/usuarios/{id}/…`): 200, 400 (motivo vacío, duración negativa), 404, 409. |
| `admin/servicio/AdminUsuarioServiceTest` | Servicio | Habilitar/deshabilitar (UC), auto-deshabilitación prohibida, fechas de duración, limpieza de motivo al habilitar. |
| `admin/servicio/ConfiguracionSistemaServiceTest` | Servicio | Configuración del scheduler. |
| `admin/servicio/ExpirarDeshabilitacionServiceTest` | Servicio | Reactivación automática de usuarios con deshabilitación vencida. |
| `admin/mapper/AdminUsuarioMapperTest` | Mapper | `Usuario` → `AdminUsuarioResponse`. |
| `admin/dto/DeshabilitarUsuarioRequestValidationTest` | Validación DTO | Motivo obligatorio, duración positiva. |

### Módulo `ubicacion` (catálogo georef)
| Archivo | Capa | Cubre |
|---|---|---|
| `ubicacion/controlador/UbicacionUsuarioControllerTest` | Controller | Ubicación del usuario autenticado. |
| `ubicacion/controlador/UbicacionCatalogoControllerTest` | Controller | Catálogo de provincias/localidades. |
| `ubicacion/servicio/UbicacionServiceTest` | Servicio | `obtenerOCrear`: localidad nueva (guarda con id georef), existente (reutiliza), inexistente, provincia sin localidad. |
| `ubicacion/servicio/UbicacionUsuarioServiceTest` | Servicio | Asignación de ubicación al usuario. |
| `ubicacion/servicio/GeorefCatalogoServiceTest` | Servicio | Cliente del catálogo georef (provincias/localidades). |
| `ubicacion/mapper/UbicacionMapperTest` | Mapper | Mapeo de ubicación. |
| `ubicacion/dto/UbicacionRequestValidationTest` | Validación DTO | Reglas de `UbicacionRequest`. |

### Módulo `proyectos` (proyectos): usa AssertJ
| Archivo | Capa | Cubre |
|---|---|---|
| `proyectos/controlador/ProyectoControllerTest` | Controller | Endpoints de proyectos: 200/201, 400, validaciones y permisos. |
| `proyectos/servicio/CrearProyectoServiceTest` | Servicio | Creación con director, planificación y objetivos; perfil activo requerido, perfil en baja, rango de fechas inválido, nombre duplicado. |
| `proyectos/servicio/ProyectoSecurityServiceTest` | Servicio | Reglas de seguridad/permisos sobre proyectos. |
| `proyectos/dto/CrearProyectoDTOValidationTest` | Validación DTO | Reglas de `CrearProyectoRequest`. |

### Módulo `home` (dashboard / health)
| Archivo | Capa | Cubre |
|---|---|---|
| `home/controlador/HomeControllerTest` | Controller | `GET /health` (200 ok) y `GET /home/resumen` (perfil activo presente/ausente). |

### Módulo `repositorio` (integración con PostgreSQL)
| Archivo | Capa | Cubre |
|---|---|---|
| `repositorio/AbstractPostgresIntegrationTest` | Base (Testcontainers) | Contenedor singleton `postgres:17-alpine` + registro del datasource dinámico. |
| `repositorio/UsuarioRepositoryIntegrationTest` | Integración | `findByCorreo` (case-sensitive), `existsByCorreo/Dni`, unicidad de correo/DNI, usuario Google sin password, motivo/duración de deshabilitación, `reactivarVencidos` (UC-04). |
| `repositorio/GeneroRepositoryIntegrationTest` | Integración | Datos sembrados y consultas de géneros. |
| `repositorio/RolGlobalRepositoryIntegrationTest` | Integración | Datos sembrados y consultas de roles. |
| `repositorio/ProfesionRepositoryIntegrationTest` | Integración | Consultas de profesiones. |
| `repositorio/UbicacionRepositoryIntegrationTest` | Integración | Consultas de ubicaciones (`findByLocalidadAndProvincia`). |
| `repositorio/CaracteristicaTecnicaRepositoryIntegrationTest` | Integración | Consultas de características técnicas por profesión/filtros. |
| `repositorio/CalendarioRepositoryIntegrationTest` | Integración | Consultas de jornada/bloqueos. |
| `repositorio/CalendarioActividadIntegrationTest` | Integración | Solapamiento de actividades con margen. |
| `repositorio/EditarPerfilServiceIntegrationTest` | Integración (servicio+BD) | Edición real de perfil contra PostgreSQL. |

---

## Cómo ejecutar

```bash
./mvnw test
```

- Los tests de servicio, validación DTO, mappers y controllers **no requieren infraestructura**.
- Los tests de integración (`repositorio/*`) **requieren Docker** para levantar PostgreSQL con Testcontainers; sin Docker fallan.
- JaCoCo genera el reporte de cobertura en `target/site/jacoco/index.html`.

## Regla de la cátedra (reforzada en este proyecto)

> Cada módulo nuevo debe entregarse con pruebas completas antes de avanzar al siguiente: validación de DTOs, unitarios de servicio, mappers, controladores y —cuando toque persistencia/consultas— tests de integración contra PostgreSQL real.