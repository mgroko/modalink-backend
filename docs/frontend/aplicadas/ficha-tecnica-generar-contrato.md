# Ficha técnica — UC-39 Generar contrato

**Módulo:** Contratos  
**Actor:** Director de proyecto  
**Estado backend:** implementado y testeado con tests unitarios, MVC e integración con PostgreSQL (Testcontainers).

---

## 1. Resumen

El director de un proyecto `PUBLICADO` o `CONFIRMADO` genera un contrato para uno o más miembros activos. El sistema:

1. valida el proyecto, el perfil activo y el rol Director;
2. permite elegir un tipo de acuerdo y los miembros destinatarios;
3. consolida un borrador legal a partir de la plantilla del tipo de acuerdo;
4. permite agregar cláusulas adicionales;
5. registra el contrato en estado `PENDIENTE_FIRMA`;
6. agrega la firma del director emisor y una firma `PENDIENTE` por destinatario;
7. registra los eventos `CREADO` y `EMITIDO`;
8. crea una notificación `CONTRATO_PENDIENTE_FIRMA` por cada parte.

La operación es atómica. Si falla una validación, no se persiste ningún contrato, versión, firma, evento ni notificación.

---

## 2. Flujo de negocio

### 2.1. Precondiciones

- El usuario está autenticado y tiene un perfil activo.
- El perfil activo es Director activo del proyecto.
- El proyecto está en estado `PUBLICADO` o `CONFIRMADO`.
- El proyecto tiene al menos un miembro activo.
- Se selecciona un tipo de acuerdo activo.
- Se selecciona al menos un miembro destinatario activo del proyecto.

### 2.2. Pasos

1. El director selecciona el proyecto (UC-29).
2. El frontend consulta `GET /proyectos/{idProyecto}/contratos/opciones`.
3. El sistema devuelve datos del proyecto, tipos de acuerdo, miembros activos y contratos activos por tipo.
4. El director elige el tipo de acuerdo y los destinatarios.
5. El frontend puede llamar a `POST /proyectos/{idProyecto}/contratos/previsualizar` para ver el borrador y los conflictos sin persistir.
6. El director agrega cláusulas adicionales y confirma.
7. El frontend llama a `POST /proyectos/{idProyecto}/contratos`.
8. El backend inserta el contrato como `BORRADOR`, agrega las firmas de los destinatarios y lo emite a `PENDIENTE_FIRMA`.
9. La base de datos valida, setea `fecha_emision`, agrega la firma del director, registra los eventos y crea las notificaciones.

### 2.3. Postcondiciones

- `contrato.estado = PENDIENTE_FIRMA`.
- `contrato.fecha_emision` no nula.
- `contrato.version_actual = 1` y una fila en `contrato_version`.
- Una firma `PENDIENTE` por destinatario y una firma del director (`DIRECTOR` o `DIRECTOR_DESTINATARIO`).
- Eventos `CREADO` y `EMITIDO`.
- Notificaciones `CONTRATO_PENDIENTE_FIRMA` con `entidad_tipo = 'CONTRATO'`, `entidad_id` e `id_proyecto`.

---

## 3. Endpoints

### 3.1. `GET /proyectos/{idProyecto}/contratos/opciones`

Devuelve el contexto para los pasos 2 y 3. No modifica datos.

Respuesta `200`:

```json
{
  "proyecto": {
    "idProyecto": 10,
    "nombre": "Editorial Primavera",
    "descripcion": "Producción editorial de moda",
    "estado": "PUBLICADO",
    "fechaInicio": null,
    "fechaEntrega": "2026-12-01"
  },
  "emisor": {
    "idMiembro": 3,
    "idPerfil": 7,
    "nombreArtistico": "Ana",
    "nombreCompleto": "Ana Pérez",
    "dni": "30111222"
  },
  "tiposAcuerdo": [
    {
      "idTipoAcuerdo": 1,
      "codigo": "CESION_DERECHOS_IMAGEN",
      "nombre": "Cesión de derechos de imagen",
      "descripcion": "Autorización para el uso de la imagen de las partes en el proyecto."
    }
  ],
  "miembros": [
    {
      "idMiembro": 8,
      "idPerfil": 12,
      "nombreArtistico": "Beto",
      "nombreCompleto": "Beto Gómez",
      "dni": "33111222",
      "profesion": "Modelo",
      "rolProyecto": "MIEMBRO",
      "esEmisor": false
    }
  ],
  "contratosActivos": [
    {
      "idMiembro": 8,
      "idTipoAcuerdo": 1,
      "idContrato": 44,
      "nombre": "Cesión de derechos de imagen - Editorial Primavera"
    }
  ]
}
```

`contratosActivos` permite que el frontend deshabilite combinaciones conflictivas antes del paso 7.

### 3.2. `POST /proyectos/{idProyecto}/contratos/previsualizar`

Genera el borrador sin persistir. Devuelve `200` aunque haya conflictos, para que el director pueda volver al paso 3.

Request:

```json
{
  "idTipoAcuerdo": 1,
  "idsMiembro": [8, 9],
  "clausulasAdicionales": [
    "La parte cedente conserva el derecho de aprobar el uso de su imagen fuera del territorio acordado.",
    "El material entregado deberá devolverse dentro de los 30 días."
  ]
}
```

Respuesta `200`:

```json
{
  "texto": "CONTRATO DE CESIÓN DE DERECHOS DE IMAGEN\n\nProyecto: Editorial Primavera...",
  "puedeGenerarse": false,
  "conflictos": [
    {
      "codigo": "CONTRATO_DUPLICADO_ACTIVO",
      "idMiembro": 8,
      "nombre": "Beto Gómez",
      "mensaje": "Beto Gómez ya tiene un contrato ACTIVO de este tipo en el proyecto."
    }
  ]
}
```

### 3.3. `POST /proyectos/{idProyecto}/contratos`

Genera y emite el contrato. Mismo request que la previsualización.

Respuesta `201`:

```json
{
  "idContrato": 55,
  "idProyecto": 10,
  "nombre": "Cesión de derechos de imagen - Editorial Primavera",
  "descripcion": "Contrato generado para el proyecto Editorial Primavera",
  "estado": "PENDIENTE_FIRMA",
  "versionActual": 1,
  "fechaCreacion": "2026-11-07T12:00:00",
  "fechaEmision": "2026-11-07T12:00:00",
  "tipoAcuerdo": {
    "idTipoAcuerdo": 1,
    "codigo": "CESION_DERECHOS_IMAGEN",
    "nombre": "Cesión de derechos de imagen",
    "descripcion": "Autorización para el uso de la imagen de las partes en el proyecto."
  },
  "texto": "CONTRATO DE CESIÓN DE DERECHOS DE IMAGEN...",
  "partes": [
    {
      "idMiembro": 3,
      "idPerfil": 7,
      "nombre": "Ana Pérez",
      "rolParte": "DIRECTOR",
      "estadoFirma": "PENDIENTE"
    },
    {
      "idMiembro": 8,
      "idPerfil": 12,
      "nombre": "Beto Gómez",
      "rolParte": "DESTINATARIO",
      "estadoFirma": "PENDIENTE"
    }
  ]
}
```

---

## 4. Validaciones y errores

| HTTP | Código | Situación |
|---|---|---|
| `400` | — (errores de validación) | Body inválido, ids vacíos o cláusula demasiado larga |
| `400` | `TIPO_ACUERDO_INVALIDO` | Tipo de acuerdo inexistente o inactivo |
| `403` | `ACCESO_DENEGADO_PROYECTO` | El perfil activo no es Director activo |
| `404` | `PROYECTO_NO_ENCONTRADO` | Proyecto inexistente |
| `404` | `PERFIL_NO_ENCONTRADO` | Perfil activo inexistente o ajeno |
| `409` | `PROYECTO_NO_APTO_PARA_CONTRATO` | Proyecto no está `PUBLICADO` o `CONFIRMADO` |
| `409` | `PROYECTO_SIN_MIEMBROS_ACTIVOS` | No hay miembros activos |
| `409` | `CONTRATO_SIN_DESTINATARIOS` | No se seleccionó ningún destinatario |
| `409` | `DESTINATARIO_INVALIDO` | Miembro inactivo, ajeno al proyecto o inexistente |
| `409` | `CONTRATO_DUPLICADO_ACTIVO` | Algún destinatario ya tiene un contrato `ACTIVO` del mismo tipo |
| `409` | `CONTRATO_TEXTO_INVALIDO` | Texto vacío o fuera de límite |

Para `CONTRATO_DUPLICADO_ACTIVO` la respuesta incluye `conflictos[]` con `idMiembro`, `nombre` y `mensaje`, para que el frontend vuelva al paso 3.

---

## 5. Modelo de datos y triggers

Tablas:

- `tipo_acuerdo_contrato`
- `contrato`
- `contrato_version`
- `contrato_evento`
- `firma_contrato`

Reglas principales de la base:

- Todo contrato nuevo nace en `BORRADOR`.
- La emisión a `PENDIENTE_FIRMA` exige proyecto `PUBLICADO`/`CONFIRMADO`, emisor Director activo, al menos un destinatario y texto no vacío.
- La emisión agrega la firma del director emisor. Si el director fue seleccionado como destinatario, queda como `DIRECTOR_DESTINATARIO`.
- La emisión registra `fecha_emision`, el evento `EMITIDO` y las notificaciones de las partes.
- Los eventos `CREADO`, `EMITIDO`, `ACTIVADO`, `CANCELADO` y `ELIMINADO` referencian la PK real de `contrato_version`.
- Las notificaciones de contrato guardan `id_proyecto`.

### Correcciones aplicadas sobre la propuesta SQL original

1. `contrato_evento.id_contrato_version` se resolvía con `NEW.version_actual` en lugar de la PK de `contrato_version`. Se corrigió en `fn_post_transicion_contrato`.
2. Las notificaciones de contrato no guardaban `id_proyecto`. Se agregó a `fn_notificar_contrato_usuario`.
3. `firma_contrato.rol_parte` era `varchar(20)` y no admitía `DIRECTOR_DESTINATARIO` (21 caracteres). Se amplió a `varchar(30)`.

---

## 6. Migración y esquemas

- Migración local: `docs/3_diseño/migraciones/2026-11-07_uc39_generar_contrato.sql`.
- Esquema canónico actualizado: `docs/3_diseño/ModaLinkBD.sql`.
- Esquema de tests actualizado: `src/test/resources/ModaLinkBD.sql`.

La migración crea el bloque completo de contratos, incluidos los triggers de baja de firmantes, índices, FKs y seeds de `tipo_acuerdo_contrato` y `notificacion_tipo`.

---

## 7. Pruebas

- `GeneradorTextoContratoServiceTest`: placeholders, cláusulas, plantilla genérica, longitudes.
- `GenerarContratoServiceTest`: camino feliz, conflicto duplicado, no persistencia.
- `ContratoControllerTest`: `200`, `201`, `400`, `404`, `409`.
- `GenerarContratoIntegrationTest`: contrato `PENDIENTE_FIRMA`, versión 1, firmas, eventos con `id_contrato_version`, notificaciones con `id_proyecto`, director como `DIRECTOR_DESTINATARIO`, proyecto no apto, destinatario ajeno y duplicado activo.
- Regresión: UC-38, UC-40, notificaciones y confirmación de proyecto.

---

## 8. Pendiente / siguientes UC

- UC-53 Modificar contrato.
- UC-54 Eliminar contrato.
- UC-55 Firmar contrato.
- UC-68 Buscar contrato.

La máquina de estados, la auditoría de firmas y la baja de firmantes ya quedaron en la base para esos casos.
