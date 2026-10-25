# Dashboard Admin - Ficha Técnica Frontend

**Estado:** implementado y validado con la suite completa `mvn test`.

Los endpoints requieren la autoridad `VER_USUARIOS`.

## Endpoints

### `GET /admin/dashboard/resumen`

Obtiene el resumen del dashboard administrativo.

#### Parámetros

| Parámetro | Tipo | Default | Restricciones |
|---|---|---:|---|
| `size` | `int` | `10` | Entre 1 y 100 |

`size` controla el máximo de elementos de `topProvincias` y
`listaNuevosProyectos`.

#### Respuesta `200 OK`

```json
{
  "totalUsuarios": 1500,
  "proyectosConfirmados": 126,
  "porcentajeConfirmados": 8.4,
  "actividadProvincia": [
    {
      "idProvincia": 1,
      "nombreProvincia": "Buenos Aires",
      "totalPerfiles": 45,
      "totalProyectos": 30,
      "totalActividad": 75
    }
  ],
  "profesionMasDemandada": {
    "idProfesion": 5,
    "nombreProfesion": "Lic. Diseño",
    "totalPerfiles": 67
  },
  "topProvincias": [
    {
      "idProvincia": 1,
      "nombreProvincia": "Buenos Aires",
      "totalActividad": 75
    }
  ],
  "listaNuevosProyectos": [
    {
      "idProyecto": 101,
      "nombre": "Proyecto Eco",
      "fechaInicio": "2024-03-15T10:30:00",
      "estado": "CONFIRMADO"
    }
  ]
}
```

`profesionMasDemandada` puede ser `null` si no existen perfiles activos
agrupables por profesión.

### `GET /admin/proyectos`

Lista proyectos administrativos paginados.

#### Parámetros

| Parámetro | Tipo | Default | Restricciones |
|---|---|---:|---|
| `estado` | `EstadoProyecto[]` | — | Valores exactos del enum |
| `page` | `int` | `0` | Mayor o igual a 0 |
| `size` | `int` | `20` | Entre 1 y 100 |

Estados permitidos:

- `BORRADOR`
- `PUBLICADO`
- `CONFIRMADO`
- `FINALIZADO`
- `CANCELADO`

Si no se envía `estado`, el endpoint excluye `BORRADOR`.
Si se envía `estado`, respeta exactamente los valores enviados, incluido
`BORRADOR`.

Ejemplos:

```text
/admin/proyectos?page=0&size=20
/admin/proyectos?estado=CONFIRMADO&estado=PUBLICADO&page=0&size=20
/admin/proyectos?estado=BORRADOR&page=0&size=20
```

#### Respuesta `200 OK`

```json
{
  "contenido": [
    {
      "idProyecto": 1,
      "nombre": "Proyecto Test",
      "fechaInicio": "2024-01-15T00:00:00",
      "estado": "PUBLICADO",
      "privacidad": "PRIVADO"
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 150,
  "totalPaginas": 8,
  "primera": true,
  "ultima": false
}
```

Los valores de `estado` y `privacidad` se serializan en mayúsculas:
`PUBLICO`, `PRIVADO`, `PUBLICADO`, etc.

### `GET /notificaciones/resumen`

Este endpoint pertenece al módulo de notificaciones y devuelve el resumen del
usuario autenticado, incluido un usuario administrador. No devuelve
notificaciones globales del panel.

```json
{
  "total": 5,
  "noLeidas": 2
}
```

### Reportes administrativos

Los siguientes endpoints aún no tienen un caso de uso ni un modelo de datos
implementado:

- `GET /admin/reportes`
- `GET /admin/reportes/{reporteTipo}`

Ambos responden `501 Not Implemented` con un mensaje explícito. El frontend no
debe tratarlos como respuestas exitosas ni asumir que devuelven una lista vacía.

## DTOs del dashboard

### `AdminDashboardResumen`

| Campo | Tipo | Descripción |
|---|---|---|
| `totalUsuarios` | `Long` | Total de usuarios registrados |
| `proyectosConfirmados` | `Long` | Proyectos con estado `CONFIRMADO` |
| `porcentajeConfirmados` | `Double` | Confirmados sobre proyectos cuyo estado no es `BORRADOR` |
| `actividadProvincia` | `List<AdminResumenProvincia>` | Perfiles activos y proyectos no borrador por provincia |
| `profesionMasDemandada` | `AdminProfesionDemandada` | Profesión con más perfiles activos |
| `topProvincias` | `List<AdminTopProvincia>` | Provincias con mayor actividad |
| `listaNuevosProyectos` | `List<AdminUltimoProyecto>` | Últimos proyectos no borrador |

### `AdminResumenProvincia`

| Campo | Tipo | Descripción |
|---|---|---|
| `idProvincia` | `Long` | Identificador de provincia |
| `nombreProvincia` | `String` | Nombre de provincia |
| `totalPerfiles` | `Long` | Perfiles en estado `ACTIVO` con ubicación provincial |
| `totalProyectos` | `Long` | Proyectos cuyo estado no es `BORRADOR` y tienen ubicación provincial |
| `totalActividad` | `Long` | `totalPerfiles + totalProyectos` |

### `AdminProfesionDemandada`

| Campo | Tipo | Descripción |
|---|---|---|
| `idProfesion` | `Long` | Identificador de profesión |
| `nombreProfesion` | `String` | Nombre de profesión |
| `totalPerfiles` | `Long` | Perfiles activos asociados |

### `AdminTopProvincia`

| Campo | Tipo | Descripción |
|---|---|---|
| `idProvincia` | `Long` | Identificador de provincia |
| `nombreProvincia` | `String` | Nombre de provincia |
| `totalActividad` | `Long` | Actividad total |

### `AdminUltimoProyecto`

| Campo | Tipo | Descripción |
|---|---|---|
| `idProyecto` | `Long` | Identificador de proyecto |
| `nombre` | `String` | Nombre del proyecto |
| `fechaInicio` | `LocalDateTime` | Fecha de inicio |
| `estado` | `EstadoProyecto` | Estado serializado en mayúsculas |

### `AdminProyectoList`

| Campo | Tipo | Descripción |
|---|---|---|
| `idProyecto` | `Long` | Identificador de proyecto |
| `nombre` | `String` | Nombre del proyecto |
| `fechaInicio` | `LocalDateTime` | Fecha de inicio |
| `estado` | `EstadoProyecto` | Estado serializado en mayúsculas |
| `privacidad` | `String` | `PUBLICO` o `PRIVADO` |

## Reglas de cálculo

### Porcentaje de confirmados

```text
porcentajeConfirmados =
    proyectosConfirmados / totalProyectosNoBorrador * 100
```

Si no hay proyectos no borrador, el backend devuelve `0.0`. El frontend no
debe hardcodear ningún porcentaje de diseño.

### Actividad por provincia

- Se cuentan perfiles en estado `ACTIVO`.
- Se cuentan proyectos cuyo estado no es `BORRADOR`.
- Sólo se agrupan registros con ubicación provincial disponible.
- No se cuentan postulaciones ni actividades individuales.
- `actividadProvincia` se ordena por `totalActividad` descendente.
- En caso de empate, se ordena por nombre de provincia.
- `topProvincias` toma como máximo `size` elementos de ese resultado.

## Errores relevantes

- `400 Bad Request`: `page` o `size` inválidos, o errores de validación.
- `401/403`: usuario no autenticado o sin `VER_USUARIOS`.
- `501 Not Implemented`: endpoints de reportes administrativos.

## Consideraciones de interfaz

- Mostrar estado de carga mientras se consulta el dashboard.
- Manejar listas vacías cuando no existan datos.
- Manejar `profesionMasDemandada: null`.
- Mostrar los estados y la privacidad con etiquetas amigables, pero enviar los
  valores del enum en mayúsculas.
- La notificación debe usar `/notificaciones/resumen` para el usuario actual.

## Ejemplo Fetch

```javascript
const obtenerDashboard = async (token, size = 10) => {
  const response = await fetch(`/admin/dashboard/resumen?size=${size}`, {
    headers: {
      // Agregar el header Authorization con el JWT del usuario autenticado.
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    throw new Error(`Error al obtener el dashboard: ${response.status}`);
  }
  return response.json();
};

const obtenerProyectos = async (token, page = 0, size = 20) => {
  const response = await fetch(`/admin/proyectos?page=${page}&size=${size}`, {
    headers: {
      // Agregar el header Authorization con el JWT del usuario autenticado.
    }
  });

  if (!response.ok) {
    throw new Error(`Error al obtener proyectos: ${response.status}`);
  }
  return response.json();
};
```
