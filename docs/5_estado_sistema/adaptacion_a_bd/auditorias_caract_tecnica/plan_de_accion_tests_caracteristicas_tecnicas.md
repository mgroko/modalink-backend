# Plan de Acción — Adaptación de Tests del Módulo de Características Técnicas

**Fecha:** 2026-09-28  
**Alcance:** Exclusivamente las clases de test vinculadas al módulo de características técnicas y sus adaptaciones a la BD.  
**Estado:** ✅ Completado con éxito  

---

## 1. Diagnóstico de Tests Afectados

Tras adaptar el código de producción a la BD en las Fases 1, 2, 3 y 4, las firmas de métodos, DTOs y constructores cambiaron:
1. `AdminCaracteristicaTecnicaRequest` cambió de `(codigo, unidad, idProfesion, tipoDato, valores)` a `(codigo, nombre, idUnidad, idProfesion, tipoDato, valores)`.
2. `CaracteristicaTecnicaResponse` ahora incluye `String nombre` y `UnidadMedidaResponse unidad` (en lugar de `String unidad`).
3. `ValorCaracteristica` usa clave primaria compuesta `@EmbeddedId ValorCaracteristicaId(idValor, idCaracteristica)` y el campo `etiqueta`. No tiene `idValor(Long)` directo en builder ni alias `codigo`.
4. `AdminCaracteristicaTecnicaService.actualizarValor` y `eliminarValor` reciben `(idCaracteristica, idValor, ...)`.
5. `AdminCaracteristicaTecnicaService` ahora inyecta `UnidadMedidaRepository`.
6. En `AdminCaracteristicaTecnicaController`, las rutas para valores pasaron a ser `PUT/DELETE /{id}/valores/{idValor}`.
7. `CaracteristicaPerfilId` fue eliminado de producción (la tabla en BD usa PK simple IDENTITY).

---

## 2. Inventario de Tests Afectados y Tareas de Adaptación

### Tarea T-01: Limpiar import obsoleto en `EditarPerfilServiceTest`
* **Archivo:** `src/test/java/org/mgroko/backend/perfiles/servicio/EditarPerfilServiceTest.java`
* **Acción:** Eliminar `import org.mgroko.backend.modelo.CaracteristicaPerfilId;` (el test no lo usa en el cuerpo).

---

### Tarea T-02: Adaptar `AdminCaracteristicaTecnicaRequestValidationTest`
* **Archivo:** `src/test/java/org/mgroko/backend/admin/dto/AdminCaracteristicaTecnicaRequestValidationTest.java`
* **Acción:**
  * Actualizar la construcción de `AdminCaracteristicaTecnicaRequest` para usar la nueva firma del record: `(codigo, nombre, idUnidad, idProfesion, tipoDato, valores)`.
  * Mantener tests de validación para `codigo`, `idProfesion`, `tipoDato` y valores en cascada.
  * Añadir test para validar `@Size(max = 100)` en el campo `nombre`.

---

### Tarea T-03: Adaptar `CaracteristicaTecnicaMapperTest`
* **Archivo:** `src/test/java/org/mgroko/backend/perfiles/mapper/CaracteristicaTecnicaMapperTest.java`
* **Acción:**
  * En la construcción de `CaracteristicaTecnica`, asignar `UnidadMedida` mediante `.unidadMedida(UnidadMedida.builder().idUnidad(1L).simbolo("cm").nombre("Centímetro").tipoDatoPermitido("NUMERICO").build())`.
  * Asignar `nombre("Altura")`.
  * Actualizar assertions de `CaracteristicaTecnicaResponse`:
    * `assertEquals("Altura", response.nombre())`
    * `assertEquals("cm", response.unidad().simbolo())` (en lugar de `response.unidad()`)
    * `assertEquals(1L, response.unidad().idUnidad())`

---

### Tarea T-04: Adaptar `CaracteristicaTecnicaServiceTest` y `CaracteristicaTecnicaControllerTest`
* **Archivos:**
  * `src/test/java/org/mgroko/backend/perfiles/servicio/CaracteristicaTecnicaServiceTest.java`
  * `src/test/java/org/mgroko/backend/perfiles/controlador/CaracteristicaTecnicaControllerTest.java`
* **Acción:**
  * En `CaracteristicaTecnicaServiceTest`: en `caracteristicaAltura()`, usar `.unidadMedida(UnidadMedida.builder().simbolo("cm").build())`. En assertions, verificar `response.get(0).unidad().simbolo()`.
  * En `CaracteristicaTecnicaControllerTest`: actualizar la construcción del mock `CaracteristicaTecnicaResponse` con el nuevo orden de parámetros `(id, codigo, nombre, unidadResponse, idProfesion, profesion, tipoDato, valores)` y actualizar el `jsonPath("$.unidad.simbolo")` (o `$.unidad` según corresponda).

---

### Tarea T-05: Adaptar `AdminCaracteristicaTecnicaServiceTest`
* **Archivo:** `src/test/java/org/mgroko/backend/admin/servicio/AdminCaracteristicaTecnicaServiceTest.java`
* **Acción:**
  * Añadir mock `@Mock private UnidadMedidaRepository unidadMedidaRepository;` para satisfacer la nueva inyección en el servicio.
  * En métodos de prueba para `crear` y `actualizar`:
    * Mockear `unidadMedidaRepository.findById(1L)` cuando el request provea `idUnidad`.
    * Actualizar requests con la nueva firma `(codigo, nombre, idUnidad, idProfesion, tipoDato, valores)`.
  * En pruebas de valores:
    * En `actualizarValor`: invocar `service.actualizarValor(10L, 50L, req)` (pasando `idCaracteristica` e `idValor`).
    * En `eliminarValor`: invocar `service.eliminarValor(10L, 50L)`.
    * En construcción de `ValorCaracteristica`: usar `id(new ValorCaracteristicaId(50L, 10L))` y `etiqueta(...)`.
  * Añadir casos de prueba para:
    * Unidad no encontrada: lanza `UnidadMedidaNoEncontradaException`.
    * Incompatibilidad de tipo de dato entre unidad y característica: lanza `TipoDatoInvalidoException`.

---

### Tarea T-06: Adaptar `AdminCaracteristicaTecnicaControllerTest`
* **Archivo:** `src/test/java/org/mgroko/backend/admin/controlador/AdminCaracteristicaTecnicaControllerTest.java`
* **Acción:**
  * En `requestCaracteristica`: construir `AdminCaracteristicaTecnicaRequest` con `(codigo, "Nombre " + codigo, 1L, 1L, tipoDato, List.of())`.
  * En endpoints de `actualizarValor` y `eliminarValor`:
    * Actualizar URLs de llamada MockMvc a `/admin/caracteristicas-tecnicas/10/valores/20` (coincidiendo con `@PutMapping("/{id}/valores/{idValor}")` y `@DeleteMapping("/{id}/valores/{idValor}")`).
    * Actualizar mocks de servicio a `actualizarValor(eq(10L), eq(20L), any())` y `eliminarValor(10L, 20L)`.
  * En assertions de `CaracteristicaTecnicaResponse`: adaptar chequeos de unidad a objeto `jsonPath("$.unidad.simbolo")`.

---

### Tarea T-07: Agregar tests unitarios para `AdminUnidadMedidaController`
* **Archivo a crear:** `src/test/java/org/mgroko/backend/admin/controlador/AdminUnidadMedidaControllerTest.java`
* **Acción:**
  * Probar endpoint `GET /admin/unidades-medida` listando todas las unidades.
  * Probar filtrado por `?tipoDato=NUMERICO`.

---

## 3. Criterio de Aceptación del Plan de Tests
1. `mvn test-compile` finaliza con `BUILD SUCCESS` (0 errores de compilación en tests).
2. Todos los tests del módulo de características técnicas (`AdminCaracteristicaTecnica*`, `CaracteristicaTecnica*`, `CaracteristicaPerfil*`, `ValorCaracteristica*`, `UnidadMedida*`) ejecutan y pasan exitosamente (`BUILD SUCCESS`).
3. No se tocan tests ajenos a este módulo.
