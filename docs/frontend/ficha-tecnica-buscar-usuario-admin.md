# Ficha Técnica Frontend: Búsqueda de Usuarios - Panel Administrador (UC-06)

Esta especificación técnica detalla la integración con la API, parámetros de consulta, modelos de datos, paginación parametrizable y consideraciones de UX/UI para la pantalla de **Búsqueda y Gestión de Usuarios** en el panel de Administración.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `GET`
* **URL:** `/admin/usuarios/buscar`
* **Autenticación requerida:** Sí (Bearer Token JWT o Cookie de sesión autenticada).
* **Autorización Requerida:** Permiso `VER_USUARIOS` (Rol Administrador). Peticiones de usuarios sin este permiso recibirán `403 Forbidden`.
* **Regla de Negocio sobre Estados:**
  * El Administrador tiene visibilidad de usuarios en **cualquier estado**: `Activo`, `Deshabilitado`, `PendienteBaja` y `Baja`.
  * Si no se envía el filtro de estado, se listan todos.
* **Ordenamiento fijo:** Los resultados siempre se devuelven ordenados por `apellido` ascendente y luego `nombre` ascendente. El frontend no puede modificar el orden.

---

## 2. Parámetros de Consulta (Query Params)

Todos los parámetros se envían mediante query string en la URL (`GET /admin/usuarios/buscar?...`). Son **opcionales y combinables**.

### 2.1. Paginación y Control de Visualización

| Parámetro | Tipo | Default | Opciones / Descripción |
| :--- | :---: | :---: | :--- |
| `page` | `integer` | `0` | Número de página (0-indexed: `0` para la primera, `1` para la segunda, etc.). Valores negativos se ajustan a `0`. |
| `size` | `integer` | `20` | Cantidad de resultados por página seleccionada: `20`, `50`, etc. |
| `todos` | `boolean` | `false` | Si se envía `true` (o `size<=0`), la API desactiva la paginación y retorna la totalidad de usuarios sin truncar. |

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
| `estado` | `string` (enum) | **Valor exacto del enum** (case-sensitive): `ACTIVO`, `DESHABILITADO`, `PENDIENTE_BAJA`, `BAJA`. Un valor distinto ⇒ `400 Bad Request`. Ver nota abajo. |
| `idProfesion` | `integer` (Long) | ID de profesión de al menos uno de los perfiles asociados al usuario (comparación exacta). |
| `nombreProfesion` | `string` | Búsqueda parcial sobre el nombre de la profesión de sus perfiles asociados. |
| `nombreArtisticoPerfil` | `string` | Búsqueda parcial sobre el nombre artístico de cualquiera de los perfiles del usuario. |

> **⚠️ Asimetría de `estado` (importante):**
> - **Para filtrar** (query param) se envía el **nombre del enum**: `ACTIVO`, `DESHABILITADO`, `PENDIENTE_BAJA`, `BAJA`.
> - **En la respuesta** (`contenido[].estado`) viene el **nombre display**: `"Activo"`, `"Deshabilitado"`, `"PendienteBaja"`, `"Baja"`.
> - El selector de filtros debe mapear label visual → valor enum al armar la query. No enviar el valor de la respuesta tal cual.

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

### Notas sobre campos de la respuesta

| Campo | Detalle |
| :--- | :--- |
| `estado` | Nombre display: `"Activo"`, `"Deshabilitado"`, `"PendienteBaja"`, `"Baja"`. |
| `rolGlobal` | Nombre del rol (`"USUARIO"`, `"ADMINISTRADOR"`, etc.). |
| `genero` | Objeto `{ idGenero, codigo }` (entidad, no string plano). |
| `fechaSolicitudBaja` | ISO 8601 si el usuario está en `PendienteBaja`; `null` en caso contrario. |
| `fechaHastaDeshabilitacion` | ISO 8601 (`"2026-10-15T00:00:00"`) si la deshabilitación tiene duración; `null` si es indefinida. |

---

## 4. Códigos de Respuesta HTTP

| Código HTTP | Escenario | Comportamiento Frontend |
| :---: | :--- | :--- |
| `200 OK` | Búsqueda procesada con éxito (con o sin resultados coincidentes). | Renderizar tabla/grilla de usuarios o estado vacío si `totalElementos === 0`. |
| `400 Bad Request` | Parámetro inválido: valor de `estado` fuera del enum (`MethodArgumentTypeMismatchException`), `page`/`size` no numéricos. | Corregir los parámetros de la query; no reintentar tal cual. |
| `401 Unauthorized` | Sesión caducada o sin token de autenticación. | Redirigir al login administrativo. |
| `403 Forbidden` | Usuario autenticado pero sin rol/permiso de administrador (`VER_USUARIOS`). | Mostrar pantalla de acceso denegado. |
| `500 Internal Server Error` | Excepción no controlada en el servidor. | Mostrar notificación Toast de error y permitir reintentar. |

### Errores de las acciones rápidas (§5.2)

| Endpoint | Código | Causa |
| :--- | :---: | :--- |
| `PATCH /admin/usuarios/{id}/habilitar` · `deshabilitar` | `403` | El usuario objetivo está en estado `Baja` (no se gestiona desde el panel). |
| `PATCH /admin/usuarios/{id}/deshabilitar` | `403` | Auto-deshabilitación: *"No podés deshabilitar tu propia cuenta."* |
| `PATCH /admin/usuarios/{id}/deshabilitar` | `400` | `motivo` vacío o >200 caracteres; `duracionDias` ≤ 0 (Bean Validation). |
| `GET /admin/usuarios/{id}` · `GET /admin/usuarios/{id}/perfiles` | `404` | Usuario inexistente. |

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
* **Deshabilitar**: Si el usuario está `Activo`, botón que abre modal solicitando **motivo (obligatorio, máx. 200 chars)** y duración en días opcional (`PATCH /admin/usuarios/{id}/deshabilitar`). Si no se indica duración, la deshabilitación es indefinida hasta `/habilitar`.

### 5.3. Filtros y Debounce
* Aplicar debounce de 300ms en los campos de texto (`nombre`, `apellido`, `correo`, `nombreArtisticoPerfil`).
* Selector desplegable para el filtro `estado` con mapeo label → valor de query:

| Label en la UI | Valor a enviar (`estado=`) |
| :--- | :--- |
| Todos los estados | *(omitir el parámetro)* |
| Activo | `ACTIVO` |
| Deshabilitado | `DESHABILITADO` |
| Pendiente de baja | `PENDIENTE_BAJA` |
| Baja | `BAJA` |

* Botón para limpiar todos los filtros aplicados.
* Enviar solo filtros activos (evitar parámetros vacíos) para URLs limpias.

---

## 6. Referencias de Implementación Backend

| Aspecto | Archivo |
| :--- | :--- |
| Endpoint y parámetros | `admin/controlador/AdminUsuarioController.java` (`buscar`) |
| Servicio, paginación y orden | `admin/servicio/AdminUsuarioService.java` (`buscar`) |
| Filtros | `admin/especificacion/UsuarioSpecifications.java` |
| DTO de filtro (enum `estado`) | `admin/dto/BuscarUsuariosAdminFiltro.java` |
| DTO de respuesta | `admin/dto/AdminUsuarioResponse.java` |
| Mapeo de respuesta | `admin/mapper/AdminUsuarioMapper.java` |
| Request de deshabilitar | `admin/dto/DeshabilitarUsuarioRequest.java` |
| Paginación | `common/dto/PaginaResponse.java` |
| Errores (400/403/404/500) | `common/exception/GlobalExceptionHandler.java` + `@PreAuthorize` |
| Seguridad | `security/SecurityConfig.java` |
