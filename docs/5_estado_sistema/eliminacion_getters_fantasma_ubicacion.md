# Eliminación de los getters fantasma de `Ubicacion` y adaptación de los DTO al modelo real

> **Tipo**: Registro de un cambio aplicado (con el detalle de lo borrado, para trazabilidad)
> **Fecha**: 27 de Septiembre de 2026
> **Alcance**: eliminación de R1, R2, R5 y R6 de la auditoría de ubicación
> **Permiso**: eliminar los getters fantasma; adaptar los DTO al modelo actual de BD. No hay producción, el
> cambio de contrato de API es aceptable.
> **Informe relacionado**: [`bugs_pendientes_calendario_y_catalogos.md`](file:///c:/Users/HP/Desktop/modalink-backend/docs/5_estado_sistema/bugs_pendientes_calendario_y_catalogos.md) — su sección 10 daba R1, R2, R5 y R6 por abiertos; quedan cerrados por este trabajo.
> **Fuente de Verdad de BD**: Modelo CASE — [`docs/3_diseño/ModaLinkBD.sql`](file:///c:/Users/HP/Desktop/modalink-backend/docs/3_dise%C3%B1o/ModaLinkBD.sql)

---

## 1. Resumen

La entidad `Ubicacion` exponía cuatro getters que no correspondían a ninguna columna: aplanaban en `String` la
cadena `pais -> provincia -> ciudad` que la base de datos guarda normalizada en cuatro tablas. Eso obligaba a que
tres mappersaran el modelo viejo, y esos mappers a su vez publicaban contratos de API falsos.

| Métrica | Antes | Después |
|---------|-------|---------|
| Getters fantasma en `modelo/Ubicacion.java` | 4 | **0** |
| Mappers que dependían de ellos | 3 | **0** |
| Clases de excepción muertas | 1 | **0** |
| Tests de la suite | 533 | 539 |
| Fallos de la suite | 31 | **31** (los mismos, cero regresiones) |

El único cambio de comportamiento observable es el **contrato JSON de la API**, que es deliberado (sección 4).

---

## 2. QUÉ SE BORRÓ

De `src/main/java/org/mgroko/backend/modelo/Ubicacion.java`, líneas 33 a 50. Se conserva el código original
verbatim para trazabilidad:

```java
// Métodos helper para compatibilidad con código existente (DTOs, Mappers, etc.)
public String getLocalidad() {
    return ciudad != null ? ciudad.getNombre() : null;
}

public String getProvincia() {
    return (ciudad != null && ciudad.getProvincia() != null) ? ciudad.getProvincia().getNombre() : null;
}

public String getPais() {
    return (ciudad != null && ciudad.getProvincia() != null && ciudad.getProvincia().getPais() != null)
            ? ciudad.getProvincia().getPais().getNombre()
            : null;
}

public String getIdGeoref() {
    return ciudad != null ? ciudad.getIdExterno() : null;
}
```

**Por qué eran un problema y no solo código muerto:**

1. **Mentían sobre el modelo.** Devolvían `String` cuando la relación real es `Ciudad` → `Provincia` → `Pais`.
2. **Eran enganche de la arquitectura anterior.** Eran el contrato que impedía borrar la tabla plana de
   `ubicacion`.
3. **Eran una trampa de Jackson.** Al ser getters públicos, Jackson los serializaba. Eso podía disparar
   `LazyInitializationException` al serializar la entidad fuera de una transacción abierta. Ya no es posible.
4. **Perdían información.** `getIdGeoref()` exponía el id externo del catálogo bajo un nombre de proveedor
   (`GEOREF`) en una clase que no debería saber de dónde viene el dato.

La entidad `Ubicacion` conserva únicamente sus columnas reales: `idUbicacion`, `direccion`, `codigoPostal`,
`latitud`, `longitud` y la relación `ciudad`.

---

## 3. QUÉ MAPPERS TENÍAN PROBLEMAS Y CÓMO SE RESOLVIERON

### 3.1 `UbicacionMapper` — R2 (dos espacios de ids en un mismo DTO)

**Problema.** Publicaba el id externo del catálogo de Georef en un campo llamado `localidadId`. El nombre decía
"localidad" pero el valor era el id de Georef, y `fuente_api` no viajaba: el cliente no podía saber de qué
catálogo venía.

```java
// Código eliminado
return new UbicacionResponse(
        ubicacion.getIdUbicacion(),
        ubicacion.getIdGeoref(),   // <-- id de Georef publicado como "localidadId"
        ubicacion.getLocalidad(),
        ubicacion.getProvincia(),
        ubicacion.getPais(),
        ubicacion.getCodigoPostal(),
        ubicacion.getLatitud(),
        ubicacion.getLongitud());
```

**Resolución.** Se reescribió el mapper para recorrer el grafo y proyectar la cadena completa, exponiendo la clave
natural `(idExterno, fuenteApi)` en cada nivel. Todos los métodos son tolerantes a `null` en cada eslabón.

Métodos actuales: `toResponse`, `toCiudadResponse`, `toProvinciaResponse`, `toPaisResponse`. Los tres últimos son
públicos porque `PerfilMapper` los reutiliza.

### 3.2 `DatosPersonalesMapper` — R5 (string compuesto a mano)

**Problema.** Construía la ubicación con concatenación manual. Era el peor de los tres: un `String` que no se podía
parsear, que no distinguía "sin ubicación" de "con datos", y que producía basura cuando faltaba un eslabón.

```java
// Código eliminado
String ubicacion = null;
if (usuario.getUbicacion() != null) {
    ubicacion = usuario.getUbicacion().getLocalidad() + ", "
            + usuario.getUbicacion().getProvincia();
}
```

Consecuencia concreta: una ciudad sin provincia devolvía la cadena `"Rosario, null"`. Ese caso está cubierto ahora
por el test `toResponse_ciudadSinProvincia_noProduceComasSueltas`.

**Resolución.** Delega en `UbicacionMapper.toResponse(...)` y devuelve la ubicación estructurada. Se eliminó la
concatenación.

### 3.3 `PerfilMapper` — R6 (contrato distinto del de `UbicacionResponse`)

**Problema.** En los dos métodos de response (`toBusquedaResponse` y `toDetalleResponse`) publicaba `localidad` y
`provincia` pero **no** `pais`, de modo que el mismo dato tenía dos contratos distintos según el endpoint. Leía
`getLocalidad()` / `getProvincia()` en las líneas 68, 69, 107 y 108.

**Resolución.** `PerfilBusquedaResponse` y `PerfilDetalleResponse` reemplazan los dos `String` por un único
`CiudadResponse ciudad`, que arrastra provincia y país. Los dos endpoints comparten ahora el mismo tipo.

### 3.4 `ProyectoMapper` — sin defecto propio

No usaba los getters fantasma; delegaba en `UbicacionMapper.toResponse(...)`. No se modificó. Pero **heredó** la
forma plana de `UbicacionResponse`, así que su JSON cambió junto con el resto. Se incluye aquí para que quede
registrado que su cambio es consecuencia, no decisión.

### 3.5 Nota sobre `UbicacionCatalogoController` — colisión de nombres resuelta

Durante el trabajo se sobrescribió por error un DTO preexistente: `ubicacion.dto.ProvinciaResponse`, que era la
proyección del **catálogo externo** con dos campos (`String id, String nombre`) y que usa
`UbicacionCatalogoController` para `GET /ubicaciones/provincias`. El nombre choca con la proyección de la **tabla
`provincia`**, que es un concepto distinto.

Resolución: el DTO del catálogo pasó a llamarse `ProvinciaCatalogoResponse` y el controlador se actualizó. **El JSON
de `GET /ubicaciones/provincias` no cambia** (`id` y `nombre`), por lo que su test no se tocó. Queda una asimetría
menor: el hermano `LocalidadResponse` sigue sin sufijo. Anotado como deuda menor en la sección 7.

---

## 4. CONTRATO DE API: QUÉ CAMBIÓ

Este es el único cambio observable fuera de Java. **El front se rompe y hay que avisarlo.**

### 4.1 `GET /usuario/ubicacion` y `PUT /usuario/ubicacion`

```jsonc
// ANTES
{
  "idUbicacion": 10,
  "localidadId": "0208401002",   // en realidad el id externo de Georef
  "localidad": "Saavedra",
  "provincia": "Ciudad Autónoma de Buenos Aires",
  "pais": "Argentina",
  "codigoPostal": null,
  "latitud": -34.5548978526608,
  "longitud": -58.4863271154338
}

// AHORA
{
  "idUbicacion": 10,
  "direccion": null,            // NUEVO: la columna existe y no se exponía
  "codigoPostal": null,
  "latitud": -34.5548978526608,
  "longitud": -58.4863271154338,
  "ciudad": {
    "idCiudad": 100,
    "idExterno": "0208401002",  // el id del catálogo
    "fuenteApi": "GEOREF",      // NUEVO: de qué catálogo vino
    "nombre": "Saavedra",
    "provincia": {
      "idProvincia": 10,
      "idExterno": "02",
      "fuenteApi": "GEOREF",
      "nombre": "Ciudad Autónoma de Buenos Aires",
      "pais": { "idPais": 1, "codigoIso": "AR", "nombre": "Argentina" }
    }
  }
}
```

### 4.2 `PUT /usuario/datos-personales`

`ubicacion` pasa de ser un `String` a ser un objeto con la misma forma de 4.1, o `null`.

```jsonc
"ubicacion": "Saavedra, Ciudad Autónoma de Buenos Aires"   // ANTES
"ubicacion": { "idUbicacion": 10, "ciudad": { ... } }       // AHORA
```

### 4.3 `GET /perfiles/buscar` y `GET /perfiles/{idPerfil}`

```jsonc
// ANTES
"genero": "FEM", "localidad": "Rosario", "provincia": "Santa Fe"
// AHORA
"genero": "FEM", "ciudad": { "idCiudad": 100, "idExterno": "0320104", "fuenteApi": "GEOREF", "nombre": "Rosario", "provincia": { ... } }
```

### 4.4 `POST /proyectos`

`ubicacion` cambia igual que en 4.1.

### 4.5 Lo que NO se tocó

Los DTO de **entrada** se conservan: `UbicacionRequest.localidadId()`, `provinciaId()`,
`BuscarPerfilesFiltro.localidad()`. Son la entrada del selector de catálogo y no arrastran el modelo viejo.

## 5. DECISIONES TOMADAS

### 5.1 R7 — `UbicacionNoEncontradaException`: eliminada por duplicación semántica

**Qué se borró.** La clase `src/main/java/org/mgroko/backend/usuario/exception/UbicacionNoEncontradaException.java`,
su `import` en `GlobalExceptionHandler` y su handler:

```java
@ExceptionHandler(UbicacionNoEncontradaException.class)
public ResponseEntity<Map<String, Object>> handleUbicacionNoEncontrada(UbicacionNoEncontradaException ex) {
    return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
}
```

**Por qué era seguro borrarla.** Antes de eliminarla se verificó que no hubiera ningún caso de negocio que la
necesitara, para no estar tapando un hueco:

1. **Nunca se lanzaba.** Las únicas 5 referencias en todo el árbol eran la propia declaración, el `import` y el
   handler. Ningún servicio la lanzaba. No había ningún test que la produjera.
2. **Era un duplicado exacto de otra excepción.** `LocalidadNoEncontradaException` también devuelve
   `HttpStatus.BAD_REQUEST` con un cuerpo `buildErrorResponse(ex.getMessage(), ...)` estructuralmente idéntico.
   Dos excepciones distintas para el mismo código HTTP y el mismo cuerpo.
3. **No había hueco semántico.** Los dos casos que pretendería cubrir ya tienen dueño:

   | Situación | Resolución actual | Dónde |
   |---|---|---|
   | La localidad no existe en el catálogo externo | `LocalidadNoEncontradaException` → 400 | `UbicacionService.obtenerOCrear` |
   | El usuario no tiene ubicación guardada | `Optional.empty()` → 200 sin cuerpo | `UbicacionUsuarioService.obtener` |

   El segundo es un estado normal del dominio —un usuario recién registrado no tiene ubicación— y
   correctamente **no** es un error. Por eso `obtener` devuelve `Optional` y no lanza.

4. **Vía de hecho correcta.** Ningún test lo cubría, así que eliminarlo no deja un hueco de cobertura: la
   autorización de borrado la da el análisis de las 5 referencias, no la ausencia de tests.

**Señal adicional de que era residuo de otra época:** la clase vivía en `usuario/exception/`, no en
`ubicacion/exception/` junto a sus hermanas (`LocalidadNoEncontradaException`, `ProvinciaSinLocalidadException`,
`LocalidadSinProvinciaException`). Ya no queda ninguna en `usuario/exception/` salvo `SolicitudBajaException`.

**Consecuencia**: ninguna. No hay ruta de producción que pueda cambiar de comportamiento, porque no había ninguna
ruta que la lanzara. Los 31 fallos de la suite son los mismos 31 de antes.

---

### 5.2 R3 — eliminada la búsqueda de ubicación por nombre

**Qué se borró.** El método `UbicacionRepository.findByLocalidadAndProvincia(String, String)` —con su `@Query`,
sus `@Param` y su Javadoc— y el *fast path* que lo invocaba en `UbicacionService.obtenerOCrear`.

El código eliminado, en `UbicacionService.java:83-85`:

```java
LocalidadCatalogo localidad = catalogoGeoref.obtenerLocalidad(localidadId);
return ubicacionRepository
        .findByLocalidadAndProvincia(localidad.nombre(), localidad.nombreProvincia())
        .orElseGet(() -> crear(localidad));
```

Y la query, en `UbicacionRepository.java:22-23`:

```sql
SELECT u FROM Ubicacion u
WHERE LOWER(u.ciudad.nombre) = LOWER(:localidad)
  AND LOWER(u.ciudad.provincia.nombre) = LOWER(:provincia)
```

**Por qué era un bug y no solo nomenclatura.** El Javadoc de `UbicacionService` declaraba el invariante correcto
—«las altas se idempotentan por la clave natural, **no por nombre**»— y el código hacía lo contrario: consultaba por
nombre **antes** del lookup autoritativo y devolvía el resultado. La query no filtraba por `pais`, ni por
`id_externo`, ni por `fuente_api`.

Los dos modos de falla, que **no** son el obvious:

| Escenario | Resultado |
|---|---|
| Ambas filas tienen `ubicacion` | `IncorrectResultSizeDataAccessException` → **500**. Spring Data no puede desambiguar. |
| Solo una tiene `ubicacion` | Devuelve la fila del **país equivocado** y `usuario.setUbicacion(...)` la persiste. Fallo silencioso. |

No hace falta un segundo país para que colisione: la query tampoco filtra por `fuente_api`, así que dos catálogos
(GEOREF + GEONAMES) con el mismo par ciudad/provincia colisionan dentro de la misma Argentina. Eso contradecía el
Javadoc «agnóstico a la fuente API (GEOREF, GEONAMES, GOOGLEMAPS, etc)».

**Por qué no se pierde rendimiento.** Se verificó antes de borrar, porque el fast path parecía existir por eso:

- El camino por nombre envuelve ambas columnas en `LOWER()`, así que **no puede usar ningún índice**: seq scan.
- `findByCiudad_IdCiudad` **tampoco** tenía índice sobre `ubicacion.id_ciudad`: también era seq scan.

Los dos eran igual de caros. El fast path no compraba nada a cambio de un bug de corrección.

**Por qué no se pierde funcionalidad.** `crear()` ya resueldía el reuso correctamente y es ahora el único camino:
`findByFuenteApiAndIdExterno` (natural) → `findByCiudad_IdCiudad` (ubicación existente) → `save`.

**Tests afectados.**

- `UbicacionServiceTest`: el test `obtenerOCrear_localidadExistente_reutilizaSinGuardar` probaba el camino
  eliminado. Se reemplazó por `obtenerOCrear_noConsultaPorNombre_paraDecidirElReuso`, que verifica lo contrario:
  que el reuso se decide por `findByFuenteApiAndIdExterno` + `findByCiudad_IdCiudad`. También se removió un stub
  de `findByLocalidadAndProvincia` que quedaba sin uso (habría fallen `UnnecessaryStubbing`).
- `UbicacionRepositoryIntegrationTest`: tenía el método borrado como sujeto de prueba. Se reescribió completo con
  `findByCiudad_IdCiudad` como sujeto. El test más valioso es
  `mismaLocalidadEnOtraProvincia_sonUbicacionesDistintas`: dos ciudades con el mismo nombre en provincias
  distintas son filas distintas, que es exactamente la propiedad que el fast path por nombre no garantizaba.

**Nota de SQL aplicada por el desarrollador.** `ubicacion.id_ciudad` pasó a `NOT NULL` (antes era nullable).
Verificado en ambos scripts, `docs/3_diseño/ModaLinkBD.sql:1468` y `src/test/resources/ModaLinkBD.sql:1468`, que
quedaron alineados. Es coherente con `@JoinColumn(name = "id_ciudad", nullable = false)` y `optional = false` en la
entidad, y elimina la tercera fuente de verdad sobre si una ubicación puede no tener ciudad.

---

### 5.3 R3-bis — NUEVO: los índices únicos por nombre siguen contradiciendo la clave natural

**Estado**: Abierto. **Requiere SQL.** No lo introdujo este trabajo: se descubrió al verificar R3.

`ciudad` y `provincia` tienen **dos** nociones de identidad simultáneas y contradictorias:

```sql
-- Restricción por nombre (residuo de la tabla plana)
CREATE UNIQUE INDEX "UQ_ciudad_provincia_nombre" ON ciudad(nombre, id_provincia)
CREATE UNIQUE INDEX "UQ_provincia_pais_nombre"  ON provincia(nombre, id_pais)

-- Clave natural (la identidad real, según el diseño)
CREATE UNIQUE INDEX "UQ_ciudad_id_externo_y_api"    ON ciudad(id_externo, fuente_api)
CREATE UNIQUE INDEX "UQ_provincia_id_externo_y_api" ON provincia(id_externo, fuente_api)
```

Las dos primeras se aplican: son índices únicos reales, no convenciones. Evidencia verificada ejecutando un insert
de «La Plata» en la misma provincia con `fuente_api` distinto:

```
ERROR: duplicate key value violates unique constraint "UQ_ciudad_provincia_nombre"
Detail: Key (nombre, id_provincia)=(La Plata, 1) already exists.
```

La clave natural `(id_externo, fuente_api)` lo habría permitido — es otra fuente. La restricción por nombre no, y
esa manda.

**Consecuencia.** El diseño multi-catálogo que `UbicacionService` declara en su Javadoc **está bloqueado a nivel de
esquema, no solo de código**. Cuando se cargue un segundo catálogo, `crearCiudad` va a insertar y va a rebotar con
una violación de constraint → 500, sin manejo. Es un bug latente que R3 no cubría: R3 era la búsqueda por nombre en
Java; esto es la restricción por nombre en la base.

**Acción requerida.** Decidir si `UQ_ciudad_provincia_nombre` y `UQ_provincia_pais_nombre` deben caer, o si el
catálogo de una ciudad se considera único por nombre dentro de la provincia. La primera opción es coherente con la
decisión de que la identidad es la clave natural. Si se conservan, hay que documentar que **una ciudad no puede
existir dos veces en la misma provincia aunque venga de catálogos distintos**, y `crearCiudad` debería manejar el
choque con un error de dominio controlado y no con un 500.

> No se editó ningún SQL: los cambios de esquema los aplica el desarrollador a mano.

---

1. **Anidar en vez de aplanar.** Como la BD está normalizada en 4 tablas, la traducción fiel son records anidados
   que replican la cadena. La alternativa —mantener `String` planos y cambiar solo de dónde se leen— habría
   permitido borrar R1 sin romper la API, pero habría dejado el contrato mintiendo sobre el modelo.
2. **Exponer la clave natural `(idExterno, fuenteApi)`.** Es lo que permite al front recargar una ubicación sin
   depender del id interno, y cierra R2. No se expone `idCiudad` para escritura: la escritura va por
   `UbicacionRequest`.
3. **No exponer `activo`.** Existe en `pais`, `provincia` y `ciudad`, pero es una bandera de mantenimiento del
   catálogo, no información de la ubicación de un usuario. Si alguna vez hace falta, se agrega.
4. **No exponer `ciudad.codigo_postal`, `latitud_defecto` ni `longitud_defecto`.** Son datos de mantenimiento del
   catálogo, y la postal y las coordenadas propias ya viajan en `UbicacionResponse`.
5. **Agregar `direccion` a `UbicacionResponse`.** La columna existía en la tabla y el DTO la omitía. Era una
   omisión, no una decisión.
6. **Null-safety en cada eslabón.** Los mappers toleran `null` en todos los niveles para poder describir una
   ubicación a medio catalogar sin romper la serialización.

---

## 6. RIESGO NUEVO A TENER EN CUENTA

Los mappers ahora recorren el grafo **explícitamente** en vez de dejar que Jackson lo hiciera por la vía de los
getters. Consecuencias:

- **Bien**: la navegación ocurre dentro de la transacción del servicio, que es donde debe. Se eliminó el riesgo de
  `LazyInitializationException` de R1.
- **A tener en cuenta**: si alguno de estos mappers se invocara alguna vez fuera de una sesión de Hibernate, va a
  fallar. Hoy los tres se llaman desde servicios `@Transactional` de lectura. Si se agrega un mapper nuevo para
  ubicación, hay que respetar eso.
- **No se cambió** el número de consultas: la cadena es la misma que ya recorrían los getters. La pega por N+1 de
  las relaciones `LAZY` sigue pendiente y no se abordó en este trabajo.

---

## 7. Tests

### 7.1 Adaptados

| Clase | Qué cambió |
|-------|-----------|
| `UbicacionMapperTest` | Reescrito: 2 tests → **7**. Aserciones sobre la cadena anidada, más 4 casos de null-safety que no existían (`toResponse(null)`, sin ciudad, sin provincia, `idExterno`/`pais` nulos) |
| `DatosPersonalesMapperTest` | Reescrito: 3 tests → **4**. Se cambió el mock de los getters fantasma por un grafo de entidades real. Nuevo: `toResponse_ciudadSinProvincia_noProduceComasSueltas`, que fija el defecto de R5 |
| `UbicacionUsuarioServiceTest` | Aserciones `localidad()` / `localidadId()` → `ciudad().nombre()` / `ciudad().idExterno()` |
| `UbicacionUsuarioControllerTest` | Constructor del DTO y rutas JSON: `$.localidad` → `$.ciudad.nombre` |
| `VerPerfilServiceTest` | `localidad()` / `provincia()` → `ciudad().nombre()` / `ciudad().provincia().nombre()` |
| `PerfilControllerTest` | Los dos constructores de DTO y las rutas JSON |

### 7.2 Estado de la suite

```
mvn clean test  ->  539 tests, 17 failures, 14 errors  (BUILD FAILURE)
```

Los 31 fallos son **exactamente los mismos** que antes de este trabajo, todos preexistentes y ajenos a la
ubicación. Cero regresiones. El desglose por clase está en
[`bugs_pendientes_calendario_y_catalogos.md`](file:///c:/Users/HP/Desktop/modalink-backend/docs/5_estado_sistema/bugs_pendientes_calendario_y_catalogos.md).

> **Advertencia operativa**: `mvn test-compile` puede reportar `BUILD SUCCESS` sin recompilar cuando cambian clases
> de `/main` y no de `/test`. La compilación incremental de Maven no siempre invalida las clases de test ante un
> cambio en main. Para verificar de verdad hay que usar `mvn clean test-compile`. Durante este trabajo eso dio un
> falso positivo que casi se tomaría por bueno.

---

## 8. Lo que queda abierto [YA FUE TODO CERRADO]

Cerrado en este trabajo: **R1, R2, R3, R5, R6, R7**. Sigue abierto de la auditoría de ubicación:

- ~~**R3-bis** — **Nuevo, descubierto al verificar R3.** `UQ_ciudad_provincia_nombre` y
  `UQ_provincia_pais_nombre` son índices únicos **por nombre** que contradicen la clave natural y bloquean el
  diseño multi-catálogo a nivel de esquema. Ver sección 5.3. **Requiere SQL.**~~
- ~~**R4** — `BuscarPerfilesFiltro.idUbicacion` filtra perfiles por la clave surrogata de `ubicacion` en lugar de
  `idCiudad`.~~
- ~~**R7** — `UbicacionNoEncontradaException` es código muerto: sigue importada y registrada en
  `GlobalExceptionHandler`, nunca lanzada.~~ **[RESUELTO]** — clase y handler eliminados. Ver sección 5.1.
- ~~**R8** — Métodos de repositorio declarados y nunca invocados: `findByNombre` y
  `findByNombreAndProvincia_IdProvincia` siguen sin uso en producción (solo los usa
  `UbicacionRepositoryIntegrationTest` para armar datos). `findByLocalidadAndProvincia` ya no existe.~~
- ~~**B4** — Doble escritura de lat/long: `UbicacionService` las escribe a mano y además existe el trigger
  `fn_heredar_coordenadas_ciudad` (`ModaLinkBD.sql:1473-1487`), que ya las hereda. Dos fuentes de verdad.~~
- ~~**B8** — El campo `activo` existe en `pais`, `provincia` y `ciudad` pero las búsquedas no lo filtran.~~
- ~~**M1** — No existe el puerto `CatalogoGeografico`: `UbicacionService` y `UbicacionCatalogoController` dependen
  de la clase concreta `GeorefCatalogoService`.~~
- ~~**M10** — Los índices únicos `(id_externo, fuente_api)` son uniques sobre columnas nullable. Invariante a
  documentar: el código nunca debe escribir `null` en esas columnas.~~
- ~~**Índice faltante en `ubicacion`** — `ubicacion` no tiene **ningún** índice. `findByCiudad_IdCiudad`, que es el
  camino caliente de reuso, hace seq scan. Era la opción C de R3, deliberadamente diferida. **Requiere SQL.**~~

Deudas menores que dejó este trabajo:

- `LocalidadResponse` vs `ProvinciaCatalogoResponse`: asimetría de nombres entre los dos DTO del catálogo externo.
- No hay test de contrato que fije la forma JSON de `GET /ubicaciones/provincias` tras el renombre del DTO. El test
  existente sigue verde porque el JSON no cambió, así que la cobertura existe, pero no sobre el tipo nuevo.
- BUG-06 del informe anterior: `actividad.id_ubicacion` sigue siendo `NOT NULL`, decisión de negocio pendiente.

---

## 9. Cómo reproducir la verificación

```bash
# Los DTO y la cadena
mvn -o test -Dtest=UbicacionMapperTest,DatosPersonalesMapperTest,UbicacionUsuarioServiceTest,UbicacionUsuarioControllerTest,VerPerfilServiceTest,PerfilControllerTest

# La suite completa (el esquema de test se levanta con Testcontainers, postgres:17-alpine)
mvn clean test

# Confirmar que no queda ninguna referencia a los getters borrados
#   getIdGeoref | getLocalidad() | .localidad() | .localidadId()
# Solo deben aparecer coincidencias sobre DTOs de entrada (Request), nunca sobre la entidad.
```

## 10. Archivos tocados

### Entidad
- `src/main/java/org/mgroko/backend/modelo/Ubicacion.java` — 4 getters eliminados

### DTOs
- `src/main/java/org/mgroko/backend/ubicacion/dto/PaisResponse.java` — nuevo
- `src/main/java/org/mgroko/backend/ubicacion/dto/CiudadResponse.java` — nuevo
- `src/main/java/org/mgroko/backend/ubicacion/dto/ProvinciaResponse.java` — redefinido como proyección de la tabla
- `src/main/java/org/mgroko/backend/ubicacion/dto/ProvinciaCatalogoResponse.java` — nuevo, absorbe el DTO del catálogo
- `src/main/java/org/mgroko/backend/ubicacion/dto/UbicacionResponse.java` — reescrito
- `src/main/java/org/mgroko/backend/usuario/dto/DatosPersonalesResponse.java` — `String ubicacion` → `UbicacionResponse`
- `src/main/java/org/mgroko/backend/perfiles/dto/PerfilBusquedaResponse.java` — `String` × 2 → `CiudadResponse`
- `src/main/java/org/mgroko/backend/perfiles/dto/PerfilDetalleResponse.java` — `String` × 2 → `CiudadResponse`

### Mappers y controlador
- `src/main/java/org/mgroko/backend/ubicacion/mapper/UbicacionMapper.java` — reescrito
- `src/main/java/org/mgroko/backend/usuario/mapper/DatosPersonalesMapper.java` — reescrito
- `src/main/java/org/mgroko/backend/perfiles/mapper/PerfilMapper.java` — 2 métodos actualizados
- `src/main/java/org/mgroko/backend/ubicacion/controlador/UbicacionCatalogoController.java` — renombre del DTO
