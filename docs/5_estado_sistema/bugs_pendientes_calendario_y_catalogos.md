# Bugs Pendientes — Calendario y Catálogos

> **Tipo**: Registro de defectos (NO es un plan de ejecución)
> **Fecha de detección**: 27 de Septiembre de 2026
> **Origen**: ejecución de la suite completa con `mvn clean test` — 528 tests, 33 fallando
> **Fuente de Verdad de BD**: Modelo CASE — [`docs/3_diseño/ModaLinkBD.sql`](file:///c:/Users/HP/Desktop/modalink-backend/docs/3_dise%C3%B1o/ModaLinkBD.sql)
> **Esquema espejo**: [`src/test/resources/ModaLinkBD.sql`](file:///c:/Users/HP/Desktop/modalink-backend/src/test/resources/ModaLinkBD.sql)

Este documento **registra defectos pendientes de resolver en el futuro**. No describe correcciones ya aplicadas.
Los cambios de SQL los aplica a mano el desarrollador: este informe describe qué falta y por qué, no edita el script.

---

## 1. Resumen

| ID | Severidad | Tipo | Componente | Estado |
|----|-----------|------|-----------|--------|
| BUG-01 | **Crítica** | Bug de producción | `AdminCaracteristicaTecnicaService` | Abierto, **sin cobertura de tests** |
| BUG-02 | **Alta** | Hueco de esquema | `jornada_agenda` | Abierto, requiere SQL |
| BUG-03 | **Alta** | Decisión de negocio | `fn_crear_agenda()` | **Requiere confirmación humana** |
| BUG-04 | Media | Hueco de esquema | `idx_bloqueo_agenda_rango` | Abierto, requiere SQL |
| BUG-05 | Media | Contrato BD ↔ entidad | `bloqueo_agenda.motivo` | Abierto, latente |
| BUG-06 | Media | Restricción BD | `actividad.id_ubicacion` | Abierto, requiere SQL |
| BUG-07 | Baja | Deuda de tests | 5 clases de test | Abierto,tests desadaptados |
| BUG-08 | Baja | Seed incompleto | `perfil_tyc` | Abierto, requiere SQL |
| BUG-09 | — | Remisión | Auditoría de ubicación (B1–B11) | Ver sección 7 |

**Distribución de las 31 fallas distintas de las 2 conocidas de `UbicacionServiceTest`:**

- **13** → tests desadaptados, producción correcta (BUG-07)
- **18** → problemas de calendario: 13 por factories de test desadaptados, 5 por invariantes que la BD ya no valida

> **Advertencia**: `mvn clean test` **no compila en HEAD**. El árbol de trabajo contiene cambios sin commitear en
> `modelo/Ubicacion.java` que son requeridos por el resto del código. No se puede obtener una línea base
> comparable con `git stash`. Cualquier verificación de "antes/después" debe hacerse contra el árbol de trabajo.

---

## 2. BUG-01 — El control de duplicados ignora mayúsculas y la BD lo rebota

**Severidad**: Crítica
**Estado**: Abierto. **Ningún test lo detecta.**

### Ubicación

- `src/main/java/org/mgroko/backend/admin/servicio/AdminCaracteristicaTecnicaService.java:66`, `:80`, `:103`, `:104`
- `src/main/java/org/mgroko/backend/repositorio/CaracteristicaTecnicaRepository.java:37`
- `docs/3_diseño/ModaLinkBD.sql:383-384`

### Descripción

La tabla `caracteristica_tecnica` exige que el código esté en mayúsculas a nivel de motor:

```sql
codigo  varchar(50)  NOT NULL
                 CHECK (codigo = UPPER(codigo)),
```

Pero el servicio guarda el código con un simple `trim()`, sin normalizar a mayúsculas:

```java
// AdminCaracteristicaTecnicaService:80
.codigo(request.codigo().trim())
```

Y el control de duplicados usa `existsByCodigo`, que es una query derivada de Spring Data y por lo tanto
**case-sensitive** a diferencia del resto de los repositorios del proyecto:

```java
// CaracteristicaTecnicaRepository:37
boolean existsByCodigo(String codigo);
```

### Secuencia de fallo

Si el administrador escribe `altura` en minúsculas y ya existe `ALTURA`:

1. `existsByCodigo("altura")` no encuentra `'ALTURA'` → **el control de duplicados pasa cuando debería rechazarlo**.
2. El `INSERT` es rebotado por el `CHECK (codigo = UPPER(codigo))` → el administrador recibe un error crudo de
   constraint de PostgreSQL, no un mensaje de validación.

El mismo defecto aparece en la edición (líneas 103–104), con una inconsistencia interna que delata la intención:

```java
// línea 103 — case-insensitive
if (!codigo.equalsIgnoreCase(caracteristica.getCodigo())
// línea 104 — case-sensitive
    && caracteristicaTecnicaRepository.existsByCodigo(codigo)) {
```

La comparación contra sí misma es case-insensitive pero el chequeo de duplicados no. La intención del autor era
detección de duplicados case-insensitive, como en `GeneroRepository` y `ProfesionRepository`.

### Por qué está en verde

`AdminCaracteristicaTecnicaServiceTest` pasa **21/21** porque mockea `CaracteristicaTecnicaRepository`. El mock
acepta cualquier string, así que ni el `existsByCodigo` ni el `CHECK` llegan a evaluarse. El defecto es invisible
para la suite.

### Corrección propuesta

Normalizar a mayúsculas en el borde del servicio, antes de validar y persistir, y volver el chequeo de duplicados
case-insensitive:

- Agregar `existsByCodigoIgnoreCase` (o equivalente) y usarlo en las líneas 66 y 104.
- Aplicar `toUpperCase(Locale.ROOT)` sobre el código ya normalizado con `trim()` en las líneas 80, 102 y 153.

> `Locale.ROOT` y no el default: con el locale del sistema, `"i".toUpperCase()` puede devolver `"I"` o `"İ"`
> según el caso, lo que rompería la comparación con lo almacenado.

**Alternativa válida**: agregar validación `@Pattern(regexp = "[A-Z0-9_]+")` en el DTO y rechazar en el borde con un
mensaje 400. Es preferible si el código tiene un formato cerrado. La decisión requiere conocer el contrato de
`codigo` en [`casos_de_uso.md`](file:///c:/Users/HP/Desktop/modalink-backend/docs/1_requisitos/casos_de_uso.md).

---

## 3. BUG-02 — `jornada_agenda` perdió la validación de coherencia horaria

**Severidad**: Alta
**Estado**: Abierto. Requiere cambios en el SQL.

### Ubicación

- `docs/3_diseño/ModaLinkBD.sql:687-698` (definición de tabla)
- `src/test/resources/ModaLinkBD.sql` (espejo, mismos cambios)
- `src/test/java/org/mgroko/backend/repositorio/CalendarioRepositoryIntegrationTest.java` (3 tests)

### Descripción

La única restricción de la tabla es el rango del día:

```sql
CREATE TABLE jornada_agenda(
    ...
    dia_semana          int4    NOT NULL
                        CHECK (dia_semana BETWEEN 1 AND 7),
    hora_inicio_manana  time    NOT NULL,
    hora_fin_manana     time,          -- sin NOT NULL
    hora_inicio_tarde   time,          -- sin NOT NULL
    hora_fin_tarde      time    NOT NULL,
    ...
)
```

`hora_fin_manana` e `hora_inicio_tarde` son nullable y **no existe ningún `CHECK` entre columnas ni trigger de
validación** que verifique la coherencia de los cuatro horarios.

Los tests existentes documentan tres invariantes que la base ya no garantiza:

| Test | Invariante esperada |
|------|---------------------|
| `insertarJornada_finTardeAntesDeInicioManiana_violaCheck` | `hora_fin_tarde >= hora_inicio_manana` |
| `insertarJornada_tardeAntesQueManiana_violaCheck` | `hora_inicio_tarde >= hora_inicio_manana` |
| `insertarJornada_mediodiaIncompleto_violaCheck` | si `hora_fin_manana` está seteada, `hora_inicio_tarde` también |

Los tres fallan con *"Expected java.lang.Exception to be thrown, but nothing was thrown"*: la base acepta
jornadas imposibles.

### Causa probable

La migración de la arquitectura plana a la normalizada reescribió la tabla y los triggers de forma mecánica. El
script lo declara explícitamente en la línea 1567 (`-- Adaptado a columnas: ...`) y no incluye ningún trigger de
validación de jornada. Las invariantes se perdieron en el traslado.

**Impacto**: una jornada con `hora_inicio_manana = 18:00` y `hora_fin_tarde = 09:00` se persiste y se le presenta al
usuario como si fuera válida. No hay corrupción de datos ni excepción, solo basura silenciosa.

### Corrección propuesta

Agregar `CHECK`s de coherencia o un trigger `BEFORE INSERT OR UPDATE`. Conviene decidir si la mañana y la tarde son
independientes o si forman un bloque corrido, porque eso cambia la forma de los `CHECK`:

```sql
-- Esbozo, a validar contra el contrato de jornadas
CHECK (hora_fin_manana IS NULL OR hora_fin_manana >= hora_inicio_manana)
CHECK (hora_inicio_tarde IS NULL OR hora_fin_manana IS NULL
       OR hora_inicio_tarde >= hora_fin_manana)
CHECK (hora_fin_tarde >= hora_inicio_manana)
```

> Estos tres `CHECK` son un punto de partida, no una solución validada. El orden relativo entre
> `hora_fin_manana` y `hora_inicio_tarde` depende de si existe un descanso entre ambos bloques, y eso no está
> definido en el modelo. **Confirmar contra [`contratos.md`](file:///c:/Users/HP/Desktop/modalink-backend/docs/2_analisis/contratos.md)
> antes de escribir el SQL.**

---

## 4. BUG-03 — `margen_actividad_min`: la BD dice 30, el test esperaba 60

**Severidad**: Alta
**Estado**: **Requiere decisión de negocio.** No se puede resolver leyendo el código.

### Ubicación

- `docs/3_diseño/ModaLinkBD.sql:1569-1583` (`fn_crear_agenda()`), en particular la línea 1574
- `docs/3_diseño/ModaLinkBD.sql:1586-1588` (`trg_agenda_crear`)
- `src/test/java/org/mgroko/backend/repositorio/CalendarioRepositoryIntegrationTest.java:103`

### Descripción

Al registrar un usuario, un trigger inserta la agenda con un margen de 30 minutos:

```sql
CREATE OR REPLACE FUNCTION fn_crear_agenda() RETURNS trigger AS $$
BEGIN
    INSERT INTO agenda (margen_actividad_min, id_usuario)
    VALUES (30, NEW.id_usuario)
```

El test `insertarUsuario_creaAgendaConJornadaPorDefecto` afirma `expected: <60> but was: <30>`.

### Por qué no se puede decidir solo

El comentario de la línea 1567 dice `-- Adaptado a columnas: margen_actividad_min, hora_inicio_manana, hora_fin_tarde`.
Eso indica que la función fue reescrita mecánicamente durante la migración, y que el `30` es probablemente un
resto de la arquitectura anterior que nadie revisó.

Como la base es la fuente de verdad, la lectura formal daría por válido el `30` y marcaría el test como
equivocado. Pero **el trigger se ejecuta en producción en cada alta de usuario**: si la regla de negocio real es
60 minutos, hoy todos los usuarios registrados tienen la mitad del margen previsto, y eso no se manifiesta como
error sino como agenda demasiado ajustada.

**Acción requerida**: confirmar el valor correcto con negocio. Si es 60, corregir el trigger. Si es 30, corregir el
test y documentar la decisión.

---

## 5. BUG-04 — Falta el índice de rango que usa el control de solapamiento

**Severidad**: Media
**Estado**: Abierto. Requiere SQL.

### Ubicación

- `docs/3_diseño/ModaLinkBD.sql:211-223` (`fn_validar_solape_bloqueo`)
- `src/test/java/org/mgroko/backend/repositorio/CalendarioRepositoryIntegrationTest.java:206-211`

### Descripción

El trigger de solapamiento ejecuta, en **cada** insert o update de `bloqueo_agenda`, una consulta correlacionada
sobre un rango temporal:

```sql
IF EXISTS (
    SELECT 1 FROM bloqueo_agenda b
    WHERE b.id_agenda = NEW.id_agenda
      AND b.id_bloqueo IS DISTINCT FROM NEW.id_bloqueo
      AND NEW.fecha_hora_inicio < b.fecha_hora_fin
      AND NEW.fecha_hora_fin   > b.fecha_hora_inicio
) THEN
    RAISE EXCEPTION 'El bloqueo se superpone con otro bloqueo existente de la agenda.';
```

El índice que la soportaría, `idx_bloqueo_agenda_rango`, **no existe** en el script. El test
`indiceRangoExiste` lo verifica por nombre y falla con `expected: <1> but was: <0>`.

**Impacto actual**: ninguno. No hay código de producción que inserte en `bloqueo_agenda` (ver BUG-05), así que el
trigger todavía no se dispara. Es un `Seq Scan` esperando a que se implemente la funcionalidad de bloqueos.

### Corrección propuesta

```sql
CREATE INDEX idx_bloqueo_agenda_rango ON bloqueo_agenda (id_agenda, fecha_hora_inicio, fecha_hora_fin);
```

El orden de columnas sigue el predicado: primero `id_agenda` (igualdad), luego el rango temporal.

---

## 6. BUG-05 — `bloqueo_agenda.motivo`: la BD y la entidad se contradicen

**Severidad**: Media
**Estado**: Abierto. Latente — sin impacto hasta que se implemente la funcionalidad.

### Ubicación

- `docs/3_diseño/ModaLinkBD.sql:205` → `motivo varchar(200) NOT NULL`
- `src/main/java/org/mgroko/backend/modelo/BloqueoAgenda.java:26-27`

### Descripción

Las dos fuentes de verdad dicen cosas distintas sobre el mismo campo:

**La base** lo exige:

```sql
CREATE TABLE bloqueo_agenda(
    ...
    motivo  varchar(200)  NOT NULL,
    ...
)
```

**La entidad** lo declara opcional, y lo hace de forma deliberada — sus campos vecinos **sí** llevan
`nullable = false`, este no:

```java
@Column(name = "fecha_hora_inicio", nullable = false)
private LocalDateTime fechaHoraInicio;

// Opcional segun UC-18 (vacaciones, enfermedad, etc)
@Column(name = "motivo", length = 200)
private String motivo;
```

El comentario cita explícitamente **UC-18** como fundamento de que el motivo es opcional.

### Por qué nadie lo detecta

`spring.jpa.hibernate.ddl-auto=validate` **solo valida tipos y existencia de columnas, no nulabilidad**. La
discrepancia es invisible para Hibernate. Es la misma clase de problema que el `usuario.id_ubicacion NOT NULL`
que se detectó durante la migración de ubicación.

Tampoco hay cobertura: no se encontró ninguna construcción de `BloqueoAgenda` en `src/main/java`, así que hoy
nadie inserta en la tabla.

**Impacto actual**: ninguno. **Impacto futuro**: el día que se implemente "marcar no disponible" o bloqueos, el
primer insert fallará con `null value in column "motivo" violates not-null constraint`, porque el código
desarrollado contra el contrato de la entidad (opcional) va a omitir el campo.

Esta contradicción es la causa de 13 tests de calendario que fallan al no setear `motivo` en su factory: esos
tests se escribieron respetando el contrato de la entidad, no el de la base.

### Decisión requerida

Determinar cuál de las dos fuentes es la correcta y alinear la otra:

- Si el motivo **es opcional** (lo que dice UC-18 y la entidad): quitar el `NOT NULL` del SQL.
- Si el motivo **es obligatorio**: corregir la entidad con `nullable = false`, corregir el comentario que cita
  UC-18, y revisar el contrato de UC-18.

---

## 7. BUG-06 — `actividad.id_ubicacion` sigue siendo `NOT NULL`

**Severidad**: Media
**Estado**: Abierto. Requiere SQL.

### Ubicación

- `docs/3_diseño/ModaLinkBD.sql` — tabla `actividad`
- `src/test/resources/ModaLinkBD.sql` — línea 1146 y equivalente en `usuario`

### Descripción

Durante la migración de ubicación se liberaron `proyecto.id_ubicacion` y `usuario.id_ubicacion` para que puedan ser
nulos. **`actividad.id_ubicacion` no se revisó y permanece `NOT NULL`.**

Es la tercera tabla con esa columna y la única que conserva la restricción. Si una actividad puede crearse sin
ubicación —trabajo remoto, evento presencial no geolocalizado, o cualquier otro supuesto aún por definir— el
insert falla igual que fallaban los dos casos ya corregidos.

### Acción requerida

Decidir si una actividad requiere ubicación obligatoria, y en caso contrario liberar el `NOT NULL` como se hizo con
`proyecto` y `usuario`. Recordar que el cambio debe aplicarse en **ambos** scripts (diseño y espejo de test).

---

## 8. BUG-07 — Tests desadaptados tras el normalizado de enums a UPPERCASE

**Severidad**: Baja (producción correcta)
**Estado**: Abierto. 13 tests.

### Causa raíz

El commit `7c26edd` normalizó los enums y las claves de catálogo a UPPERCASE en la base. Las aserciones de los
tests de integración nunca se actualizaron.

### Patrón del error

En los tres repositorios la búsqueda pasó a ser **case-insensitive deliberadamente**, lo cual es el comportamiento
correcto. Los tests fallan porque comparan el **valor de entrada** contra el **valor almacenado**:

| Repositorio | Query de producción | Seed | Aserción del test |
|-------------|--------------------|------|--------------------|
| `GeneroRepository.findByCodigo` | `UPPER(g.codigo) = UPPER(:codigo)` | `'MUJER'` | `assertEquals("mujer", ...)` |
| `ProfesionRepository.buscar` | `LOWER(p.nombre) LIKE :patron` | `'Modelo'` | `assertEquals("modelo", ...)` |
| `CaracteristicaTecnicaRepository.buscar` | `LOWER(c.codigo) LIKE :patronCodigo` | `'MEDIDA_PECHO'` | `assertEquals("medida_pecho", ...)` |

La producción está bien. Los tests pasan un patrón en minúsculas, la query lo encuentra correctamente, y la
aserción falla al comparar contra el string de entrada en vez de contra el valor persistido.

### Tests afectados

| Clase | Tests | Causa |
|-------|-------|-------|
| `GeneroRepositoryIntegrationTest` | 5 | 4 por comparación de caso; 1 porque `findByCodigo_esCaseSensitive` exige el contrato **opuesto** al que el código implementa |
| `ProfesionRepositoryIntegrationTest` | 2 | Comparación de caso |
| `CaracteristicaTecnicaRepositoryIntegrationTest` | 5 | 2 por comparación de caso; 3 por conteos (8 vs 4, 3 vs 0) porque los filtros usan códigos en minúscula que ya no existen |
| `EditarPerfilServiceIntegrationTest` | 1 | Cascada: busca la característica por el código minúscula, recibe lista vacía, y `List.get(0)` lanza `IndexOutOfBounds` |

### Nota sobre `findByCodigo_esCaseSensitive`

`GeneroRepositoryIntegrationTest:48` verifica que `findByCodigo("MUJER")` **no** encuentre nada, asumiendo búsqueda
case-sensitive. El método es case-insensitive por diseño. El test no solo está desactualizado: **codifica el
contrato contrario al implementado**, y su nombre describe un comportamiento que el sistema no tiene. Conviene
renombrarlo o eliminarlo, no solo corregir la aserción.

### Acción requerida

Corregir las aserciones para comparar contra el valor persistido, no contra el de entrada. **No modificar las
queries de los repositorios**: el comportamiento case-insensitive es el correcto y es el que usan
`GeneroRepository` y `ProfesionRepository` de forma consistente.

---

## 9. BUG-08 — `perfil_tyc` figura como tabla base pero no está sembrada

**Severidad**: Baja
**Estado**: Abierto. Requiere decisión + SQL.

### Ubicación

- `docs/5_estado_sistema/estado_sistema_v.0.4.md:53` — listada entre las tablas base con PK `int8`
- `docs/3_diseño/ModaLinkBD.sql` — sin `INSERT` asociado

### Descripción

`perfil_tyc` aparece en el inventario de catálogos base del documento de estado v0.4, pero el script no la siembra.
Verificado sobre la base levantada: 15 tablas con datos, y `perfil_tyc` entre las vacías junto a `provincia` y
`ciudad`.

La diferencia con `provincia` y `ciudad` es intencional —esas dos se pueblan desde el catálogo de Georef—, pero
`perfil_tyc` no tiene fuente externa ni carga posterior documentada.

### Acción requerida

Confirmar si la tabla debe sembrarse. Si es un catálogo de valores por defecto, falta el `INSERT` con su
`ON CONFLICT`. Si se puebla en runtime, sacar la tabla del inventario de catálogos base en el documento de estado.

---

## 10. Remisión — Bugs de la auditoría de ubicación

**Estado**: varios abiertos. Ver el detalle en la sección correspondiente del plan de la migración.

Defectos identificados durante la migración a la arquitectura normalizada que **siguen abiertos** al cierre de la
resolución del país:

- **R1** — ~~Los 4 getters fantasma de `modelo/Ubicacion.java` (`getLocalidad`, `getProvincia`, `getPais`,`getIdGeoref`). Jackson los serializa y pueden disparar `LazyInitializationException` fuera de transacción.~~
  **[RESUELTO]** — getters eliminados.
- **R2** — ~~`UbicacionMapper` mapea el id externo de Georef a un campo llamado `localidadId`: dos espacios de ids
  mezclados en un mismo DTO.~~ **[RESUELTO]** — la clave natural `(idExterno, fuenteApi)` se expone en cada nivel.
- **R3** — ~~`UbicacionRepository.findByLocalidadAndProvincia(nombre, nombre)`: nomenclatura de la tabla plana y
  búsqueda por nombre global, que colisiona en cuanto haya más de un país.~~ **[RESUELTO]** — método y *fast path*
  eliminados. Era un bug de corrección, no solo nomenclatura: la query no filtraba por `pais`, `id_externo` ni
  `fuente_api`, y podía devolver la ubicación de otro país u otro catálogo.
- **R3-bis** — ~~**NUEVO, descubierto al verificar R3.** `UQ_ciudad_provincia_nombre` sobre
  `ciudad(nombre, id_provincia)` y `UQ_provincia_pais_nombre` sobre `provincia(nombre, id_pais)` son índices únicos
  **por nombre** que coexisten con la clave natural `(id_externo, fuente_api)`. Contradicen la decisión de que la
  identidad es la clave natural y **bloquean el diseño multi-catálogo a nivel de esquema**: el mismo nombre de
  ciudad en la misma provincia se rechaza aunque venga de otra fuente. **Requiere decisión + SQL.** Detalle y
  evidencia en la sección 5.3 de
  [`eliminacion_getters_fantasma_ubicacion.md`](file:///c:/Users/HP/Desktop/modalink-backend/docs/5_estado_sistema/eliminacion_getters_fantasma_ubicacion.md).~~ **[RESUELTO]**
- **R4** — ~~`BuscarPerfilesFiltro.idUbicacion` filtra perfiles por la clave surrogata de `ubicacion` en lugar de
  `idCiudad`.~~ **[RESUELTO]**
- **R5** — ~~`DatosPersonalesResponse.ubicacion` es un `String` compuesto a mano con `", "`, residuo del modelo plano.~~
  **[RESUELTO]** — ahora es un `UbicacionResponse` estructurado.
- **R6** — ~~`PerfilBusquedaResponse` y `PerfilDetalleResponse` exponen `localidad` y `provincia` pero no `pais`,contrato distinto del de `UbicacionResponse`.~~ **[RESUELTO]** — ambos exponen `CiudadResponse`, que ya incluye
  provincia y país.
- **R7** — ~~`UbicacionNoEncontradaException` es código muerto: importada y registrada en `GlobalExceptionHandler`,nunca lanzada.~~ **[RESUELTO]** — clase y handler eliminados. Era además un duplicado exacto de
  `LocalidadNoEncontradaException` (mismo 400 y mismo cuerpo `buildErrorResponse`), sin ningún hueco semántico que
  cubriera.
- **R8** — ~~Métodos de repositorio declarados y nunca invocados. **Parcialmente resuelto**: `findByCiudad_IdCiudad`y `findByFuenteApiAndIdExterno` (nuevos) están en uso; `findByNombre` y `findByNombreAndProvincia_IdProvincia`siguen sin uso.~~ **[RESUELTO]**
- **B4** — ~~Doble escritura de lat/long: `UbicacionService` las escribe a mano y además existe el trigger`fn_heredar_coordenadas_ciudad`, que ya las hereda. Dos fuentes de verdad.~~ **[RESUELTO]**
- **B8** — ~~El campo `activo` existe en `pais`, `provincia` y `ciudad` pero las búsquedas no lo filtran.~~ **[RESUELTO]**
- **M1** — No existe el puerto `CatalogoGeografico`: `UbicacionService` y `UbicacionCatalogoController` dependen
  de la clase concreta `GeorefCatalogoService`. Cambiar de API implica cambiar firmas.
- **M10** — ~~Los índices únicos `(id_externo, fuente_api)` ya existen, pero son uniques sobre columnas **nullable**:
  en PostgreSQL los `NULL` se consideran distintos entre sí, así que no restringen las filas con `id_externo` nulo.
  Invariante a documentar: el código nunca debe escribir `null` en esas columnas.~~ **[RESUELTO]**

**Resuelto en esta iteración**: la resolución del país (B1–B3, M4, M6) mediante `PaisCatalogoService` y la clave
`CATALOGO_GEOREF_PAIS_ISO` en `configuracion_sistema`.

**Resuelto con posterioridad — R1, R2, R3, R5, R6 y R7 cerrados.** Los 4 getters fantasma de `modelo/Ubicacion.java` fueron
eliminados y los DTO de respuesta reescritos para reflejar la cadena `pais -> provincia -> ciudad -> ubicacion`.
En la misma pasada se eliminó `UbicacionNoEncontradaException` (código muerto) y la búsqueda de ubicación por nombre
`findByLocalidadAndProvincia`, que era un bug de corrección latente. El detalle de qué se borró, qué mappers estaban
afectados, cómo quedó el contrato JSON y el nuevo hallazgo R3-bis están en
[`eliminacion_getters_fantasma_ubicacion.md`](file:///c:/Users/HP/Desktop/modalink-backend/docs/5_estado_sistema/eliminacion_getters_fantasma_ubicacion.md).
Los ítems R3-bis, R4, R8, B4, B8, M1 y M10 que se listan arriba **siguen abiertos**.

---

## 11. Checklist de verificación

Para dar por cerrado cada defecto:

### BUG-01
- [ ] Un test **sin mocks** que intente crear una característica con código en minúsculas y reciba un error 400
      controlado, no una violación de constraint.
- [ ] Un test que intente crear `altura` cuando ya existe `ALTURA` y confirme el rechazo por duplicado.
- [ ] `AdminCaracteristicaTecnicaServiceTest` sigue verde.

### BUG-02
- [ ] Los 3 tests `insertarJornada_*_violaCheck` en verde.
- [ ] Un test que inserte una jornada con los cuatro horarios en orden creciente y confirme que se acepta.

### BUG-03
- [ ] El valor de `margen_actividad_min` está confirmado por negocio y documentado.
- [ ] `insertarUsuario_creaAgendaConJornadaPorDefecto` en verde, con el valor correcto.

### BUG-04
- [ ] `indiceRangoExiste` en verde.
- [ ] `EXPLAIN` de la query de `fn_validar_solape_bloqueo` usa el índice, no `Seq Scan`.

### BUG-05
- [ ] El SQL y la entidad coinciden en la nulabilidad de `motivo`.
- [ ] El comentario de `BloqueoAgenda` y el contrato UC-18 reflejan la decisión tomada.
- [ ] `BloqueoAgenda.java` declara el `nullable` explícitamente, sea `true` o `false`.

### BUG-06
- [ ] Decisión documentada sobre si una actividad exige ubicación.
- [ ] Ambos scripts (diseño y espejo de test) alineados.

### BUG-07
- [ ] Los 13 tests en verde **sin modificar las queries de los repositorios**.
- [ ] `findByCodigo_esCaseSensitive` renombrado o eliminado: su nombre describe un comportamiento inexistente.

### BUG-08
- [ ] Decisión documentada: sembrar `perfil_tyc` o retirarla del inventario de catálogos base.

---

## 12. Cómo reproducir la detección

```bash
# Suite completa. El esquema de test se levanta con Testcontainers (postgres:17-alpine),
# que ejecuta src/test/resources/ModaLinkBD.sql
mvn clean test

# Verificar el script completo de la BD de diseño sobre una instancia limpia
docker run -d --name mlverify -e POSTGRES_PASSWORD=verify -e POSTGRES_DB=ml postgres:17-alpine
docker cp "docs/3_diseño/ModaLinkBD.sql" mlverify:/tmp/schema.sql
docker exec mlverify psql -U postgres -d ml -v ON_ERROR_STOP=1 -f /tmp/schema.sql
```

`ON_ERROR_STOP=1` es necesario: es lo que usa Testcontainers, y sin él `psql` **continúa después de un error** y
oculta fallos de DDL. En particular, un `CREATE INDEX` sobre una columna inexistente aborta el script en el punto
exacto y deja la base a medio construir sin que lo indique el código de salida.
