# Ficha técnica — Colaboraciones automáticas

> **Estado:** backend implementado, con verificación ORB y fallback multi-recorte.
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

La detección corre de forma **asíncrona después del commit** de la transacción
de subida, en un executor dedicado. El request no espera la decodificación, ORB
ni el multi-recorte: publica un evento con los IDs y responde.

- Solo considera imágenes de publicaciones.
- Solo considera publicaciones `ACTIVA`, perfiles `ACTIVO` y usuarios `ACTIVO`.
- Excluye:
  - la misma publicación;
  - el mismo perfil;
  - otros perfiles del mismo usuario;
  - publicaciones donde ya existe una etiqueta recíproca de colaborador;
  - propuestas `PENDIENTE` o `CONFIRMADA` para el mismo par.

### 2.1 Pipeline de coincidencia

1. **SHA-256 exacto**: si el contenido es idéntico, se acepta como `EXACTA`
   sin verificación adicional.
2. **pHash candidato**: se buscan imágenes con distancia pHash menor o igual a
   `phash-distancia-candidato`.
3. **Alta confianza**: si la distancia pHash es menor o igual a
   `phash-distancia-alta-confianza`, se acepta como `PERCEPTUAL_ALTA` sin ORB.
4. **ORB**: verificación principal para candidatos perceptuales. Se acepta como
   `PERCEPTUAL_ORB` si hay suficientes keypoints y matches buenos.
5. **Multi-recorte pHash**: fallback cuando ORB no alcanza. Compara recortes de
   ambas imágenes en una grilla y varias escalas; se acepta como
   `PERCEPTUAL_MULTICROP` si la distancia mínima no supera el máximo.

Configuración:

```properties
app.colaboracion-automatica.habilitada=true
app.colaboracion-automatica.phash-distancia-candidato=24
app.colaboracion-automatica.phash-distancia-alta-confianza=8
app.colaboracion-automatica.max-propuestas-por-imagen=10
app.colaboracion-automatica.orb-min-keypoints=50
app.colaboracion-automatica.orb-min-matches=50
app.colaboracion-automatica.orb-max-distancia=60
app.colaboracion-automatica.orb-max-distancia-inliers=40
app.colaboracion-automatica.multicrop-distancia-maxima=8
app.colaboracion-automatica.multicrop-grid=3
app.colaboracion-automatica.multicrop-escalas=90,80,70

# Detección asíncrona después del commit
app.colaboracion-automatica.async.core-pool-size=2
app.colaboracion-automatica.async.max-pool-size=4
app.colaboracion-automatica.async.queue-capacity=100
app.colaboracion-automatica.async.thread-name-prefix=colab-auto-
app.colaboracion-automatica.async.await-termination-seconds=60
```

La propuesta guarda la evidencia de la verificación:

| Columna | Tipo | Descripción |
|---|---|---|
| `distancia_phash` | `int4` | Distancia pHash entre las imágenes comparadas |
| `distancia_multicrop` | `int4` | Distancia mínima del fallback multi-recorte |
| `orb_matches` | `int4` | Matches ORB buenos |
| `orb_inliers` | `int4` | Matches ORB con distancia estricta |
| `tipo_coincidencia` | `varchar(30)` | `EXACTA`, `PERCEPTUAL_ALTA`, `PERCEPTUAL_ORB` o `PERCEPTUAL_MULTICROP` |

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
conservan el registro, los perfiles, los estados, las fechas, los hashes de
evidencia y los scores de verificación (`distancia_phash`, `distancia_multicrop`,
`orb_matches`, `orb_inliers`, `tipo_coincidencia`).

## 6. Notificaciones

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

## 7. Errores

| HTTP | Caso |
|---|---|
| `403` | El perfil activo no participa de la propuesta |
| `404` | Propuesta inexistente o sin perfil activo |
| `409` | Propuesta ya respondida, ya no pendiente o publicación no disponible |
| `400` | Filtros inválidos |

## 8. Frontend

- Mostrar una pestaña separada: `/colaboraciones-automaticas`.
- Por defecto, listar `PENDIENTE`.
- Para historial, enviar `todosLosEstados=true`.
- Mostrar ambas publicaciones y el perfil de la contraparte.
- Mostrar `puedeConfirmar` y `puedeRechazar`.
- Si una publicación está eliminada, mostrar “Publicación eliminada” sin imagen.
- Al confirmar o rechazar, refrescar el listado y el resumen de notificaciones.

## 9. Publicaciones históricas

Las publicaciones existentes se pueden procesar desde el panel administrador
para completar huellas y generar propuestas. Ver
`docs/frontend/ficha-tecnica-colaboraciones-automaticas-historicas.md`.
