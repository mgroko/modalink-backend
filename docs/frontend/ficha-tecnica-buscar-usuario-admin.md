# Ficha Técnica Frontend: Búsqueda de Usuarios - Panel Administrador (UC-06)

Esta especificación técnica detalla la integración con la API, parámetros de consulta, modelos de datos, paginación parametrizable y consideraciones de UX/UI para la pantalla de **Búsqueda y Gestión de Usuarios** en el panel de Administración.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `GET`
* **URL:** `/admin/usuarios/buscar`
* **Content-Type:** `application/json`
* **Autenticación requerida:** Sí (Bearer Token JWT o Cookie de sesión autenticada).
* **Autorización Requerida:** Permiso `VER_USUARIOS` (Rol Administrador). Peticiones de usuarios sin este permiso recibirán `403 Forbidden`.
* **Regla de Negocio sobre Estados:**
  * El Administrador tiene visibilidad de usuarios en **cualquier estado**: `Activo`, `Deshabilitado`, `PendienteBaja` y `Baja`.
  * Si no se envía el filtro de estado, se listan todos.

---

## 2. Parámetros de Consulta (Query Params)

Todos los parámetros se envían mediante query string en la URL (`GET /admin/usuarios/buscar?...`). Son **opcionales y combinables**.

### 2.1. Paginación y Control de Visualización

| Parámetro | Tipo | Default | Opciones / Descripción |
| :--- | :---: | :---: | :--- |
| `page` | `integer` | `0` | Número de página (0-indexed: `0` para la primera, `1` para la segunda, etc.). |
| `size` | `integer` | `20` | Cantidad de resultados por página seleccionada: `20`, `50`, etc. |
| `todos` | `boolean` | `false` | Si se envía `true` (o `size=0`), la API desactiva la paginación y retorna la totalidad de usuarios sin truncar. |

> **Selector en la UI:**
> Proveer selector de visualización:
> - **20 por página** (`size=20&todos=false`)
> - **50 por página** (`size=50&todos=false`)
> - **Ver todos** (`todos=true` o `size=0`)

---

### 2.2. Criterios de Filtrado

| Parámetro | Tipo | Descripción / Formato |
| :--- | :---: | :--- |
| `nombre` | `string` | Búsqueda parcial e insensible a mayúsculas sobre el nombre de pila. |
| `apellido` | `string` | Búsqueda parcial e insensible a mayúsculas sobre el apellido. |
| `correo` | `string` | Búsqueda parcial e insensible a mayúsculas sobre el correo electrónico. |
| `estado` | `string` | Estado exacto del usuario: `"Activo"`, `"Deshabilitado"`, `"PendienteBaja"`, `"Baja"`. |
| `idProfesion` | `integer` (Long) | ID de profesión de al menos uno de los perfiles asociados al usuario. |
| `nombreProfesion` | `string` | Búsqueda parcial sobre el nombre de la profesión de sus perfiles asociados. |
| `nombreArtisticoPerfil` | `string` | Búsqueda parcial sobre el nombre artístico de cualquiera de los perfiles del usuario. |

---

## 3. Estructura de la Respuesta (`200 OK`)

La API devuelve un objeto estructurado `PaginaResponse<AdminUsuarioResponse>`:

### Ejemplo de Respuesta JSON

```json
{
  "contenido": [
    {
      "idUsuario": 4,
      "nombre": "Carlos",
      "apellido": "Gómez",
      "correo": "carlos.gomez@example.com",
      "estado": "Activo",
      "rolGlobal": "USUARIO",
      "fechaNacimiento": "1994-06-20",
      "dni": "38123456",
      "fechaSolicitudBaja": null,
      "motivoDeshabilitacion": null,
      "fechaHastaDeshabilitacion": null,
      "genero": {
        "idGenero": 1,
        "codigo": "MASC"
      }
    },
    {
      "idUsuario": 9,
      "nombre": "Laura",
      "apellido": "Martínez",
      "correo": "laura.m@example.com",
      "estado": "Deshabilitado",
      "rolGlobal": "USUARIO",
      "fechaNacimiento": "1990-11-12",
      "dni": "35987654",
      "fechaSolicitudBaja": null,
      "motivoDeshabilitacion": "Comportamiento inadecuado en casting",
      "fechaHastaDeshabilitacion": "2026-10-15T00:00:00",
      "genero": {
        "idGenero": 2,
        "codigo": "FEM"
      }
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 2,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

---

## 4. Códigos de Respuesta HTTP

| Código HTTP | Escenario | Comportamiento Frontend |
| :---: | :--- | :--- |
| `200 OK` | Búsqueda procesada con éxito (con o sin resultados coincidentes). | Renderizar tabla/grilla de usuarios o estado vacío si `totalElementos === 0`. |
| `401 Unauthorized` | Sesión caducada o sin token de autenticación. | Redirigir al login administrativo. |
| `403 Forbidden` | Usuario autenticado pero sin rol/permiso de administrador (`VER_USUARIOS`). | Mostrar pantalla de acceso denegado. |
| `500 Internal Server Error` | Excepción no controlada en el servidor. | Mostrar notificación Toast de error y permitir reintentar. |

---

## 5. Recomendaciones de UX/UI para el Panel de Administración

### 5.1. Badges de Estado del Usuario
* **`Activo`**: Verde (`#2e7d32` o clase `badge-success`).
* **`Deshabilitado`**: Rojo/Ámbar (`#c62828` o clase `badge-danger`), mostrando tooltip o popover con `motivoDeshabilitacion` y `fechaHastaDeshabilitacion` si aplica.
* **`PendienteBaja`**: Naranja (`#ef6c00` o clase `badge-warning`), indicando fecha de solicitud de baja (`fechaSolicitudBaja`).
* **`Baja`**: Gris (`#757575` o clase `badge-secondary`).

### 5.2. Tabla de Resultados y Acciones Rápidas
Por cada fila de usuario, proveer acceso directo a las operaciones del administrador:
* **Ver Perfiles Asociados**: Navegar a o desplegar modal con `GET /admin/usuarios/{id}/perfiles`.
* **Ver Detalle Completo**: `GET /admin/usuarios/{id}`.
* **Habilitar**: Si el usuario está `Deshabilitado`, botón de acción rápida `PATCH /admin/usuarios/{id}/habilitar`.
* **Deshabilitar**: Si el usuario está `Activo`, botón que abre modal solicitando motivo y duración opcional (`PATCH /admin/usuarios/{id}/deshabilitar`).

### 5.3. Filtros y Debounce
* Aplicar debounce de 300ms en los campos de texto (`nombre`, `apellido`, `correo`, `nombreArtisticoPerfil`).
* Selector desplegable para el filtro `estado`: *"Todos los estados"*, *"Activo"*, *"Deshabilitado"*, *"Pendiente de baja"*, *"Baja"*.
* Botón para limpiar todos los filtros aplicados.
