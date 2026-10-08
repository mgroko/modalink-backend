# Ficha Técnica Frontend: Pantalla "Crear Proyecto" (UC-24)

Esta especificación detalla la interacción con la API, validaciones, modelos de datos, manejo de errores y consideraciones de UX/UI para el desarrollo del formulario y flujo de creación de proyectos.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `POST`
* **URL:** `/proyectos`
* **Content-Type:** `application/json`
* **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`).
* **Contexto de Perfil Requerido:**
  * La solicitud debe tener un **perfil activo seleccionado** en el contexto de la sesión (`idPerfilActivo`).
  * Si el usuario no tiene un perfil activo seleccionado o el perfil se encuentra en estado de baja, el backend rechaza la petición.

---

## 2. Estructura de Datos (Payloads)

### Request Payload (`POST /proyectos`)

```json
{
  "nombre": "Colección Primavera-Verano",
  "descripcion": "Diseño y confección de prendas sostenibles para la nueva temporada.",
  "privacidad": "PUBLICO",
  "fechaInicio": "2026-10-01",
  "fechaFinEstipulada": "2026-12-15",
  "aceptaPostulacionGral": true,
  "ubicacion": {
    "localidadId": "06049010000",
    "provinciaId": "06"
  },
  "objetivos": [
    {
      "nombre": "Diseño de bocetos iniciales",
      "descripcion": "Elaborar los primeros 10 bocetos de las prendas clave."
    }
  ],
  "requerimientosGral": [
    {
      "cantidad": 2,
      "idProfesion": 2,
      "descripcion": "Modelos para pasarela",
      "caracteristicas": [
        { "idCaracteristica": 1, "valorMin": 160, "valorMax": 180 },
        { "idCaracteristica": 5, "valores": [1, 4] }
      ],
      "habilidades": [1, 4]
    }
  ],
  "moodboard": {
    "descripcion": "Paleta fría, telas de lino, inspiración nórdica."
  }
}
```

---

## 3. Matriz de Campos y Validaciones

| Campo | Tipo / Formato | Obligatorio | Reglas / Restricciones Backend | Notas UI/UX sugeridas |
| :--- | :--- | :---: | :--- | :--- |
| `nombre` | `string` | **Sí** | Máx. 50 caracteres. No blanco. Único para el perfil director activo. | Input con contador `(x/50)`. |
| `descripcion` | `string` | **Sí** | Máx. 200 caracteres. No blanco. | Textarea con contador `(x/200)`. |
| `privacidad` | `enum` | **Sí** | `PUBLICO` \| `PRIVADO` \| `OCULTO` (mayúsculas). | Segmented Control / Radio. |
| `fechaInicio` | `YYYY-MM-DD` | **Sí** | Fecha ISO. | Datepicker. |
| `fechaFinEstipulada` | `YYYY-MM-DD` | No | ≥ `fechaInicio`. | Datepicker con `min = fechaInicio`. |
| `aceptaPostulacionGral` | `boolean` | No | Default `false`. | Switch. |
| `ubicacion` | `object` | No | Si se envía, `localidadId` no puede estar en blanco. | Autocompletado Georef. |
| `objetivos[]` | `array` | No | Lista dinámica. | Sección +/- objetivos. |
| `objetivos[].nombre` | `string` | Sí (por ítem) | Máx. 100 caracteres. | Input. |
| `objetivos[].descripcion` | `string` | No | Máx. 300 caracteres. | Textarea. |
| `requerimientosGral[]` | `array` | No | Lista de requerimientos de personal. | Sección +/- requerimientos. |
| `requerimientosGral[].cantidad` | `int` | **Sí** | > 0 (`@Positive`). | Number input. |
| `requerimientosGral[].idProfesion` | `long` | **Sí** | Debe existir. | Select de profesiones. |
| `requerimientosGral[].descripcion` | `string` | No | Máx. 200 caracteres. | Input. |
| `requerimientosGral[].caracteristicas[]` | `array` | No | Sin duplicados por `idCaracteristica`. La característica debe pertenecer a la profesión del requerimiento. | Selector filtrado por profesión. |
| `requerimientosGral[].caracteristicas[].idCaracteristica` | `long` | **Sí** | Debe existir. | Select. |
| `requerimientosGral[].caracteristicas[].valorMin` / `valorMax` | `number` | Condicional | **NUMERICO**: ambos obligatorios, ≥ 0, `min ≤ max`. **No-NUMERICO**: deben ser `null`. | Rango solo visible si tipo = NUMERICO. |
| `requerimientosGral[].caracteristicas[].valores[]` | `array<long>` | Condicional | **ENUMERADO**: ≥ 1 valor existente y perteneciente a la característica. **No-ENUMERADO**: debe ser vacío/`null`. | Checklist de valores solo si tipo = ENUMERADO. |
| `requerimientosGral[].habilidades[]` | `array<long>` | No | Cada habilidad debe existir. | Multi-select de habilidades. |
| `moodboard` | `object` | No | 1:1 con el proyecto. | Sección moodboard. |
| `moodboard.descripcion` | `string` | No | Máx. 200 caracteres. | Textarea. |

---

## 4. Respuesta Exitosa (`201 Created`)

```json
{
  "idProyecto": 105,
  "nombre": "Colección Primavera-Verano",
  "descripcion": "Diseño y confección de prendas sostenibles para la nueva temporada.",
  "fechaInicio": "2026-10-01",
  "fechaFinEstipulada": "2026-12-15",
  "estado": "Borrador",
  "privacidad": "PUBLICO",
  "aceptaPostulacionGral": true,
  "ubicacion": { "idUbicacion": 12, "localidad": "Azul", "provincia": "Buenos Aires" },
  "idDirector": 4,
  "nombreDirector": "Juan Perez",
  "objetivos": [
    { "idObjetivo": 210, "nombre": "Diseño de bocetos iniciales", "descripcion": "..." }
  ],
  "requerimientosGral": [
    {
      "idRequerimientoGral": 30,
      "cantidad": 2,
      "descripcion": "Modelos para pasarela",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "caracteristicas": [
        {
          "idCaracteristica": 1,
          "codigo": "ALTURA",
          "tipoDato": "NUMERICO",
          "valorMin": 160,
          "valorMax": 180,
          "valores": []
        },
        {
          "idCaracteristica": 5,
          "codigo": "COLOR_OJOS",
          "tipoDato": "ENUMERADO",
          "valorMin": null,
          "valorMax": null,
          "valores": [1, 4]
        }
      ],
      "habilidades": [1, 4]
    }
  ],
  "moodboard": {
    "idMoodboard": 12,
    "descripcion": "Paleta fría, telas de lino, inspiración nórdica.",
    "fechaCreacion": "2026-09-01T10:00:00"
  }
}
```

---

## 5. Manejo de Errores y Excepciones

| Código HTTP | Excepción Backend | Causa / Mensaje de Error | Acción Frontend Recomendada |
| :---: | :--- | :--- | :--- |
| **400** | `MethodArgumentNotValidException` | Fallos de formulario (vacíos, largos máximos, `cantidad ≤ 0`, `idProfesion` null). Body: `{ "errores": {campo: msg} }`. | Resaltar inputs con mensaje inline. |
| **400** | `RangoFechasProyectoInvalidoException` | `fechaFinEstipulada` anterior a `fechaInicio`. | Error en campo de fecha fin. |
| **400** | `RequerimientoInvalidoException` | Reglas de requerimiento: rango invertido/negativo, NUMERICO sin rango, ENUMERADO sin valores, característica duplicada, rango en no-NUMERICO. | Mostrar `message` bajo la sección de requerimientos. |
| **400** | `DataIntegrityViolation` | Restricción de BD no pre-validada (red de seguridad). | Alerta genérica. |
| **404** | `PerfilActivoNoSeleccionadoException` | Sin perfil activo en sesión. | Modal de selección de perfil. |
| **404** | `PerfilNoEncontradoException` | Perfil inexistente o ajeno a la cuenta. | Re-sincronizar sesión. |
| **404** | `HabilidadNoEncontradaException` | Habilidad inexistente en `habilidades[]`. | Error en el multi-select de habilidades. |
| **404** | `ProfesionNoEncontradaException` | `idProfesion` inexistente. | Error en select de profesión. |
| **409** | `NombreProyectoDuplicadoException` | Nombre ya usado por el perfil director. | Error inline en `nombre`. |
| **403** | `PerfilEnBajaException` | Perfil dado de baja. | Alerta bloqueante. |

---

## 6. Consideraciones de UX

1. **Requerimientos dinámicos:** botón `+ Agregar requerimiento`; cada fila filtra características/habilidades por la profesión seleccionada.
2. **Coherencia tipo de dato:** según `tipoDato` de la característica seleccionada (la respuesta del catálogo de características lo incluye), mostrar rango (NUMERICO) o checklist de valores (ENUMERADO); ocultar el control inaplicable.
3. **Validación reactiva de fechas:** `min` del datepicker de fin = `fechaInicio`.
4. **Navegación post-creación:** usar `idProyecto` retornado para ir al dashboard del proyecto (estado inicial: `Borrador`).
