# Ficha Técnica Frontend: Ver Perfil Completo (UC-14)

Esta especificación técnica detalla el contrato de la API, el modelo de datos, la navegación desde la búsqueda (**UC-16**), la visualización condicional de acciones basada en la propiedad del perfil y las directrices de UX/UI para la pantalla de **Detalle de Perfil**.

---

## 1. Endpoint y Contexto de Acceso

* **Método:** `GET`
* **URL:** `/perfiles/{idPerfil}`
* **Parámetros de Ruta (Path Variable):**
  * `idPerfil` (Long, requerido): Identificador único del perfil que se desea visualizar.
* **Content-Type:** `application/json`
* **Autenticación requerida:** Sí (Bearer Token JWT o Cookie de sesión activa).
* **Restricción de Acceso y Reglas de Negocio:**
  * **Perfil de Terceros (público / comunitario):** Solo se puede visualizar si el perfil se encuentra en estado `Activo` **Y** el usuario propietario del perfil se encuentra en estado `Activo`. Si el perfil o el usuario están en `PendienteBaja`, `Deshabilitado` o `Baja`, la API responderá con `404 Not Found`.
  * **Perfil Propio:** Si el usuario autenticado consulta su propio perfil, la API permite la visualización tanto en estado `Activo` como en `PendienteBaja` (marcando `esPropietario: true`). Si el perfil fue dado de baja definitivamente (`Baja`), retorna `404 Not Found`.

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
  "fotoUrl": "https://res.cloudinary.com/modalink/image/upload/v123456789/perfiles/foto-elena.jpg",
  "idUsuario": 18,
  "nombreUsuario": "Elena",
  "apellidoUsuario": "Gómez",
  "genero": "FEM",
  "localidad": "Rosario",
  "provincia": "Santa Fe",
  "habilidades": [
    "Pasarela",
    "Fotogenia",
    "Comercial",
    "Maquillaje artístico"
  ],
  "caracteristicas": [
    {
      "idCaracteristica": 2,
      "codigo": "altura",
      "valor": "178",
      "idValor": null,
      "codigoValor": null,
      "colorHex": null
    },
    {
      "idCaracteristica": 5,
      "codigo": "color_ojos",
      "valor": "Verde",
      "idValor": 14,
      "codigoValor": "VERDE",
      "colorHex": "#2e7d32"
    }
  ],
  "esPropietario": false
}
```

---

## 3. Definición de Tipos TypeScript

```typescript
export interface CaracteristicaResponse {
  idCaracteristica: number;
  codigo: string;
  valor: string | null;
  idValor: number | null;
  codigoValor: string | null;
  colorHex: string | null;
}

export interface PerfilDetalleResponse {
  idPerfil: number;
  nombreArtistico: string;
  biografia: string;
  estado: 'Activo' | 'PendienteBaja' | 'Baja';
  fechaSolicitudBaja: string | null; // Formato ISO 8601 si aplica
  idProfesion: number;
  profesion: string;
  idImagen: number | null;
  fotoUrl: string | null;
  idUsuario: number;
  nombreUsuario: string;
  apellidoUsuario: string;
  genero: string | null;
  localidad: string | null;
  provincia: string | null;
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
    throw new Error('El perfil solicitado no existe o no se encuentra disponible.');
  }

  if (!response.ok) {
    throw new Error('Error al recuperar el perfil.');
  }

  return response.json();
}
```

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

---

## 6. Manejo de Errores y Estados de Respuesta

| Código HTTP | Causa | Acción recomendada en Frontend |
| :---: | :--- | :--- |
| `200 OK` | Perfil encontrado y activo / accesible. | Renderizar el componente de detalle completo. |
| `401 Unauthorized` | Sesión expirada o token no enviado. | Redirigir a `/login?redirect=/perfiles/{idPerfil}`. |
| `404 Not Found` | Perfil inexistente, perfil ajeno en `PendienteBaja`/`Baja`, o dueño del perfil ajeno en estado inactivo. | Mostrar pantalla de error amigable: *"El perfil que buscas no está disponible o ha sido dado de baja."* con botón para volver a `/perfiles/buscar`. |
| `500 Internal Error` | Error no controlado en servidor. | Mostrar mensaje de alerta y opción de reintentar. |

---

## 7. Recomendaciones de UX y Rendimiento (Cota de 2 segundos)

1. **Skeleton Screen:** Mostrar un esqueleto animado para foto de perfil, badges de habilidades y lista de características técnicas mientras la promesa se resuelve.
2. **Badge de Estado / Especialidad:** Destacar la profesión (`profesion`) y ubicación (`localidad, provincia`) en el encabezado principal del perfil.
3. **Mapeo de Características Técnicas:**
   - Si la característica tiene `colorHex`, renderizar un círculo de color (ej. color de ojos o tono de cabello).
   - Renderizar las características en formato de cuadrícula o chips estructurados.
4. **Habilidades en Chips/Tags:** Mostrar la lista de `habilidades` ordenadas alfabéticamente como chips visuales.
