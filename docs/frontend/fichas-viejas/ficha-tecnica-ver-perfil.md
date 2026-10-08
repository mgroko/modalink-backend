# Ficha Técnica Frontend: Ver Perfil Completo (UC-14)

Esta especificación técnica detalla el contrato de la API, el modelo de datos, la navegación desde la búsqueda (**UC-16**), la visualización condicional de acciones basada en la propiedad del perfil y las directrices de UX/UI para la pantalla de **Detalle de Perfil**.

---

## 1. Endpoint y Contexto de Acceso

* **Método:** `GET`
* **URL:** `/perfiles/{idPerfil}`
* **Parámetros de Ruta (Path Variable):**
  * `idPerfil` (Long, requerido): Identificador único del perfil que se desea visualizar.
* **Autenticación requerida:** Sí (Bearer Token JWT o Cookie de sesión activa).
* **Restricción de Acceso y Reglas de Negocio** (`VerPerfilService.obtenerDetalle`):
  * **Usuario autenticado inválido:** Si el usuario autenticado no existe o su estado no permite acceso (`Deshabilitado` o `Baja`), la API responde `401 Unauthorized`.
  * **Perfil de Terceros (público / comunitario):** Solo se puede visualizar si el perfil se encuentra en estado `Activo` **Y** el usuario propietario del perfil se encuentra en estado `Activo`. Si el perfil está en `PendienteBaja`, `Deshabilitado` o `Baja`, o el propietario no está `Activo`, la API responde `404 Not Found`.
  * **Perfil Propio:** El dueño puede visualizar su perfil en estado `Activo`, `PendienteBaja` **o** `Deshabilitado` (la respuesta incluye `esPropietario: true`). Solo si el perfil está en estado `Baja` (baja definitiva) responde `404 Not Found`.

---

## 2. Modelo de Respuesta (JSON Contract)

### Código HTTP `200 OK`

```json
{
  "idPerfil": 105,
  "nombreArtistico": "Elena Rostova",
  "biografia": "Modelo editorial con 4 años de experiencia en pasarelas y sesiones fotográficas.",
  "estado": "Activo",
  "fechaSolicitudBaja": null,
  "idProfesion": 1,
  "profesion": "Modelo",
  "idImagen": 42,
  "fotoUrl": "/uploads/perfiles/foto-elena.jpg",
  "idUsuario": 18,
  "nombreUsuario": "Elena",
  "apellidoUsuario": "Gómez",
  "genero": "FEM",
  "ciudad": {
    "idCiudad": 12,
    "idExterno": "030077",
    "fuenteApi": "GEOREF",
    "nombre": "Rosario",
    "provincia": {
      "idProvincia": 3,
      "idExterno": "03",
      "fuenteApi": "GEOREF",
      "nombre": "Santa Fe",
      "pais": {
        "idPais": 1,
        "idExterno": "AR",
        "fuenteApi": "GEOREF",
        "nombre": "Argentina"
      }
    }
  },
  "habilidades": [
    "Pasarela",
    "Fotogenia",
    "Comercial",
    "Maquillaje artístico"
  ],
  "caracteristicas": [
    {
      "idCaracteristica": 2,
      "codigo": "ALTURA",
      "valor": "178",
      "idValor": null,
      "codigoValor": null,
      "colorHex": null
    },
    {
      "idCaracteristica": 5,
      "codigo": "COLOR_OJOS",
      "valor": null,
      "idValor": 14,
      "codigoValor": "Verde",
      "colorHex": "#2e7d32"
    }
  ],
  "esPropietario": false
}
```

### Campos notables

| Campo | Comportamiento |
| :--- | :--- |
| `estado` | Siempre uno de: `"Activo"`, `"PendienteBaja"`, `"Deshabilitado"`, `"Baja"`. Solo `"Baja"` es inaccesible. |
| `fechaSolicitudBaja` | ISO 8601 si el perfil está en `PendienteBaja`; `null` en caso contrario. |
| `ciudad` | Objeto anidado o `null` si el usuario no tiene ubicación registrada. **No existen campos planos `localidad`/`provincia`.** |
| `codigoValor` | Es la **etiqueta visible** del valor predefinido (campo `etiqueta` de BD), no un código corto. `null` si la característica es de texto libre. |
| `esPropietario` | `true` solo cuando el perfil consultado pertenece al usuario autenticado. |

---

## 3. Definición de Tipos TypeScript

```typescript
export interface PaisResponse {
  idPais: number;
  idExterno: string;
  fuenteApi: string;
  nombre: string;
}

export interface ProvinciaResponse {
  idProvincia: number;
  idExterno: string;
  fuenteApi: string;
  nombre: string;
  pais: PaisResponse;
}

export interface CiudadResponse {
  idCiudad: number;
  idExterno: string;
  fuenteApi: string;
  nombre: string;
  provincia: ProvinciaResponse;
}

export interface CaracteristicaResponse {
  idCaracteristica: number;
  codigo: string;
  valor: string | null;
  idValor: number | null;
  codigoValor: string | null; // etiqueta visible del valor predefinido
  colorHex: string | null;    // formato #RRGGBB
}

export type EstadoPerfil = 'Activo' | 'PendienteBaja' | 'Deshabilitado' | 'Baja';

export interface PerfilDetalleResponse {
  idPerfil: number;
  nombreArtistico: string;
  biografia: string;
  estado: EstadoPerfil;
  fechaSolicitudBaja: string | null; // Formato ISO 8601 si aplica
  idProfesion: number;
  profesion: string;
  idImagen: number | null;
  fotoUrl: string | null;
  idUsuario: number;
  nombreUsuario: string;
  apellidoUsuario: string;
  genero: string | null;
  ciudad: CiudadResponse | null;
  habilidades: string[];
  caracteristicas: CaracteristicaResponse[];
  esPropietario: boolean;
}
```

---

## 4. Integración y Flujo de Navegación desde el Frontend

### 4.1. Navegación desde la Búsqueda (UC-16)
Cuando el usuario interactúa con una tarjeta de perfil en la vista de búsqueda:
```typescript
// Ejemplo en React / Next.js
const handleSelectPerfil = (idPerfil: number) => {
  router.push(`/perfiles/${idPerfil}`);
};

// En el componente Card:
<article 
  key={perfil.idPerfil} 
  onClick={() => handleSelectPerfil(perfil.idPerfil)}
  className="cursor-pointer hover:shadow-lg transition duration-200"
>
  ...
</article>
```

### 4.2. Obtención de Datos (Data Fetching)
```typescript
export async function fetchPerfilDetalle(idPerfil: number): Promise<PerfilDetalleResponse> {
  const response = await fetch(`/api/perfiles/${idPerfil}`, {
    headers: {
      'Accept': 'application/json',
      // Incluir Authorization Bearer si no se usan cookies HTTP-Only automáticas
    }
  });

  if (response.status === 404) {
    // El backend incluye un mensaje descriptivo en el cuerpo
    const error = await response.json().catch(() => null);
    throw new Error(error?.message ?? 'El perfil solicitado no existe o no se encuentra disponible.');
  }

  if (response.status === 401) {
    throw new Error('Tu sesión ha expirado. Inicia sesión nuevamente.');
  }

  if (!response.ok) {
    throw new Error('Error al recuperar el perfil.');
  }

  return response.json();
}
```

### 4.3. Formato del Cuerpo de Error (Backend)

Todas las respuestas de error siguen esta estructura (`GlobalExceptionHandler.buildErrorResponse`):

```json
{
  "message": "Perfil no encontrado.",
  "httpStatus": 404,
  "timestamp": 1696800000000
}
```

- `message`: texto listo para mostrar al usuario.
- `httpStatus`: código HTTP numérico.
- `timestamp`: milisegundos desde epoch.

---

## 5. Lógica de UI Condicional según `esPropietario`

El campo booleano `esPropietario` permite a la vista adaptar sus llamadas a la acción (CTA) sin necesidad de consultar endpoints adicionales:

| Elemento / Acción | `esPropietario: true` (Perfil Propio) | `esPropietario: false` (Perfil de Tercero) |
| :--- | :---: | :---: |
| **Botón "Editar perfil" (UC-11)** | **Visible** (redirige al formulario de edición) | Oculto |
| **Botón "Gestionar agenda" (UC-17/18)** | **Visible** (acceso a calendario de disponibilidad) | Oculto |
| **Banner de advertencia `PendienteBaja`** | **Visible** si `estado === 'PendienteBaja'` con botón para reactivar (UC-12) | Oculto (terceros nunca ven perfiles en este estado) |
| **Botón "Contactar / Colaborar" (UC-61)** | Oculto | **Visible** |
| **Botón "Reportar perfil" (UC-15)** | Oculto | **Visible** (abre modal de reporte) |
| **Visualización de disponibilidad en calendario** | Muestra vista de administración y bloqueos propios | Muestra vista de solo lectura de días/horarios libres |

> **Nota de estado:** Si `esPropietario === true` y `estado === 'Deshabilitado'`, considerar mostrar un banner informativo (el perfil está deshabilitado pero el dueño aún puede verlo y editarlo).

---

## 6. Manejo de Errores y Estados de Respuesta

| Código HTTP | Causa | Acción recomendada en Frontend |
| :---: | :--- | :--- |
| `200 OK` | Perfil encontrado y accesible. | Renderizar el componente de detalle completo. |
| `401 Unauthorized` | Sesión expirada, token no enviado, o usuario autenticado con estado `Deshabilitado`/`Baja`. | Redirigir a `/login?redirect=/perfiles/{idPerfil}`. |
| `404 Not Found` | Perfil inexistente; perfil ajeno en `PendienteBaja`/`Deshabilitado`/`Baja`; propietario del perfil ajeno no `Activo`; o perfil propio en `Baja`. | Mostrar pantalla de error amigable usando el `message` del cuerpo de respuesta (ej: *"Perfil no encontrado."*) con botón para volver a `/perfiles/buscar`. |
| `500 Internal Error` | Error no controlado en servidor. | Mostrar mensaje de alerta y opción de reintentar. |

---

## 7. Recomendaciones de UX y Rendimiento (Cota de 2 segundos)

1. **Skeleton Screen:** Mostrar un esqueleto animado para foto de perfil, badges de habilidades y lista de características técnicas mientras la promesa se resuelve.
2. **Badge de Estado / Especialidad:** Destacar la profesión (`profesion`) y ubicación (`ciudad.nombre`, `ciudad.provincia.nombre`) en el encabezado principal del perfil. Ocultar la sección de ubicación si `ciudad === null`.
3. **Mapeo de Características Técnicas:**
   - Si la característica tiene `colorHex`, renderizar un círculo de color (ej. color de ojos o tono de cabello).
   - Para características con `idValor`/`codigoValor`, mostrar la **etiqueta** (`codigoValor`) como valor visible.
   - Para características con `valor` (texto libre), mostrar el texto directamente.
   - Renderizar las características en formato de cuadrícula o chips estructurados.
4. **Habilidades en Chips/Tags:** Mostrar la lista de `habilidades` ordenadas alfabéticamente como chips visuales.
5. **Fecha de Solicitud de Baja:** Si `fechaSolicitudBaja !== null`, mostrar cuenta regresiva de reactivación (UC-12) solo para el propietario.

---

## 8. Referencias de Implementación Backend

| Aspecto | Archivo |
| :--- | :--- |
| Endpoint y parámetros | `perfiles/controlador/PerfilController.java` (`obtener`) |
| Reglas de acceso y negocio | `perfiles/servicio/VerPerfilService.java` |
| DTO de respuesta | `perfiles/dto/PerfilDetalleResponse.java` |
| Mapeo de respuesta | `perfiles/mapper/PerfilMapper.java` (`toDetalleResponse`) |
| Manejo de errores (404/401/500) | `common/exception/GlobalExceptionHandler.java` |
| Seguridad | `security/SecurityConfig.java` |
