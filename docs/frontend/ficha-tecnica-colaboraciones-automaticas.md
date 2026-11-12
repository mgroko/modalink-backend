# Ficha técnica — Colaboraciones automáticas

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-19/21 (imágenes), UC-20 (baja), UC-22 (búsqueda), UC-61/62/63/64/65 (colaboración manual), UC-66 (interacciones) y UC-69 (reportes).

## 1. Objetivo

Detectar que dos publicaciones independientes comparten una imagen o una imagen
muy similar. El sistema propone una colaboración recíproca y solo la crea si
**ambos dueños** confirman.

Cada publicación conserva su dueño. Al confirmar:

- el perfil B aparece como colaborador de la publicación de A;
- el perfil A aparece como colaborador de la publicación de B;
- **no se crea ninguna conexión de red**.

Si después quieren ser conexión, deben usar el flujo manual de UC-43.

## 2. Detección

Se ejecuta al crear una publicación o al agregar imágenes nuevas con UC-21.

- Solo considera imágenes de publicaciones.
- Compara por SHA-256 exacto y por pHash perceptual con OpenCV.
- Solo considera publicaciones `ACTIVA`, perfiles `ACTIVO` y usuarios `ACTIVO`.
- Excluye:
  - la misma publicación;
  - el mismo perfil;
  - otros perfiles del mismo usuario;
  - publicaciones donde ya existe una etiqueta recíproca de colaborador;
  - propuestas `PENDIENTE` o `CONFIRMADA` para el mismo par.

Configuración:

```properties
app.colaboracion-automatica.habilitada=true
app.colaboracion-automatica.phash-distancia-maxima=8
app.colaboracion-automatica.max-propuestas-por-imagen=10
```

## 3. Endpoints

```http
GET  /colaboraciones-automaticas
GET  /colaboraciones-automaticas/{id}
POST /colaboraciones-automaticas/{id}/confirmar
POST /colaboraciones-automaticas/{id}/rechazar
```

### 3.1 Listado

Parámetros:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `estado` | `PENDIENTE`, `CONFIRMADA`, `RECHAZADA`, `CANCELADA`, `DISUELTA` | Default `PENDIENTE` |
| `todosLosEstados` | boolean | `true` ignora el estado por defecto |
| `idPublicacion` | long | Filtra por publicación propia o de la contraparte |
| `idPerfilContraparte` | long | Filtra por el otro perfil |
| `fechaDesde` | `YYYY-MM-DD` | Primer día incluido |
| `fechaHasta` | `YYYY-MM-DD` | Último día incluido |
| `page` | int | Default 0 |
| `size` | int | Default 20, máximo 100 |

La búsqueda siempre se acota al perfil activo.

### 3.2 Respuesta

```json
{
  "idColaboracionAutomatica": 1,
  "estado": "PENDIENTE",
  "miEstado": "PENDIENTE",
  "estadoContraparte": "CONFIRMADA",
  "fechaDeteccion": "2026-11-11T12:00:00",
  "fechaConfirmacion": null,
  "fechaDisolucion": null,
  "publicacionPropia": {
    "idPublicacion": 10,
    "titulo": "Editorial Otoño",
    "portadaUrl": "/uploads/publicaciones/a.webp",
    "estado": "ACTIVA",
    "eliminada": false
  },
  "publicacionContraparte": {
    "idPublicacion": 20,
    "titulo": "Campaña Verano",
    "portadaUrl": "/uploads/publicaciones/b.webp",
    "estado": "ACTIVA",
    "eliminada": false
  },
  "perfilContraparte": {
    "idPerfil": 8,
    "nombreArtistico": "Sol",
    "profesion": "Fotógrafa",
    "fotoUrl": "/uploads/perfiles/sol.webp"
  },
  "imagen": {
    "idImagen": 99,
    "url": "/uploads/publicaciones/compartida.webp"
  },
  "puedeConfirmar": true,
  "puedeRechazar": true
}
```

Si una publicación fue eliminada, `idPublicacion`, `titulo` y `portadaUrl` se
devuelven `null`, `eliminada` es `true` y `estado` es `ELIMINADA`.

### 3.3 Confirmar

```http
POST /colaboraciones-automaticas/{id}/confirmar
```

- Solo puede confirmar uno de los dos dueños.
- La primera confirmación deja la propuesta `PENDIENTE`.
- La segunda confirmación:
  - agrega las etiquetas recíprocas;
  - marca `CONFIRMADA`;
  - notifica a ambos.

### 3.4 Rechazar

```http
POST /colaboraciones-automaticas/{id}/rechazar
```

- Marca `RECHAZADA`.
- No agrega colaboradores.
- Notifica a ambos.

## 4. Portfolio

Las etiquetas automáticas no hacen que la publicación del otro perfil aparezca
en el portfolio propio. El filtro se aplica en el backend:

- publicaciones propias: siempre se incluyen;
- colaboraciones `MANUAL`: se incluyen;
- colaboraciones `AMBOS`: se incluyen;
- colaboraciones solo `AUTOMATICA`: se excluyen del portfolio.

La etiqueta del colaborador sigue apareciendo dentro de la publicación del otro
perfil, con su `origen`.

## 5. Ciclo de vida

| Situación | Resultado |
|---|---|
| Se elimina una publicación con propuesta pendiente | `CANCELADA`, se limpian notificaciones, la otra publicación no se toca |
| Se elimina una publicación con colaboración confirmada | La otra publicación sigue existiendo; la propuesta conserva el registro y la publicación eliminada queda en `null` |
| Se elimina una imagen con propuesta pendiente | `CANCELADA` |
| Un dueño se quita como colaborador | Se quita la etiqueta recíproca y la propuesta pasa a `DISUELTA` |
| Se quita una etiqueta manual | No afecta colaboraciones automáticas |

No se conservan imágenes, portadas ni URLs de publicaciones eliminadas. Sí se
conservan el registro, los perfiles, los estados, las fechas y los hashes de
evidencia.

## 5. Notificaciones

Tipos:

- `COLABORACION_AUTOMATICA_DETECTADA`
- `COLABORACION_AUTOMATICA_CONFIRMADA`
- `COLABORACION_AUTOMATICA_RECHAZADA`
- `COLABORACION_AUTOMATICA_CANCELADA`
- `COLABORACION_AUTOMATICA_DISUELTA`

Entidad: `COLABORACION_AUTOMATICA`.

Link del frontend:

```text
/colaboraciones-automaticas/{id}
```

## 6. Errores

| HTTP | Caso |
|---|---|
| `403` | El perfil activo no participa de la propuesta |
| `404` | Propuesta inexistente o sin perfil activo |
| `409` | Propuesta ya respondida, ya no pendiente o publicación no disponible |
| `400` | Filtros inválidos |

## 7. Frontend

- Mostrar una pestaña separada: `/colaboraciones-automaticas`.
- Por defecto, listar `PENDIENTE`.
- Para historial, enviar `todosLosEstados=true`.
- Mostrar ambas publicaciones y el perfil de la contraparte.
- Mostrar `puedeConfirmar` y `puedeRechazar`.
- Si una publicación está eliminada, mostrar “Publicación eliminada” sin imagen.
- Al confirmar o rechazar, refrescar el listado y el resumen de notificaciones.
