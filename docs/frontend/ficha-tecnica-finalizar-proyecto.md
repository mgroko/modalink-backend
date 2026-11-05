# Ficha técnica — UC-37 Finalizar proyecto

**Módulo:** Proyectos  
**Actor:** Director de proyecto  
**Estado backend:** implementado y testeado con tests unitarios, MVC e integración con PostgreSQL (Testcontainers).

---

## 1. Resumen

El director finaliza un proyecto en estado `Confirmado` cuando todas las actividades están `Finalizada` o `Cancelada` y la fecha de entrega del cronograma ya se alcanzó. Si la fecha todavía no se alcanzó, puede finalizarlo de forma anticipada con una confirmación explícita.

El backend:

1. valida el estado del proyecto, la fecha de entrega y las actividades;
2. si la fecha es futura, devuelve una advertencia y exige `confirmarFinalizacionPrematura`;
3. exige que el director califique a todos los miembros activos;
4. guarda las calificaciones y sus reseñas;
5. cambia el proyecto a `Finalizado`, registra `fechaFinalizacion` y marca `finalizacionPrematura` cuando corresponde;
6. publica un evento para solicitar a los demás participantes que califiquen al equipo;
7. deja el proyecto visible en el historial propio de los integrantes.

La operación es atómica. Si falla una validación, el proyecto sigue `Confirmado` y no se persiste ninguna calificación.

La carga de imágenes del resultado (UC-19 Alta publicación) queda fuera de alcance porque UC-19 aún no está implementado.

---

## 2. Flujo de negocio

### 2.1. Precondiciones

- Proyecto en estado `Confirmado`.
- `Planificacion.fechaEntrega` no nula. Puede ser menor o igual a la fecha actual o futura con confirmación explícita.
- Todas las actividades en estado `Finalizada` o `Cancelada`.
- Actor con perfil activo, membresía activa, rol Director y permiso `FINALIZAR_PROYECTO`.

### 2.2. Pasos

1. El director abre el detalle del proyecto y consulta el preview de finalización.
2. El sistema muestra los bloqueos, las advertencias, los miembros a calificar y las calificaciones ya registradas.
3. Si la fecha de entrega es futura, el sistema pide confirmar la finalización anticipada.
4. El director completa la valoración y la reseña de cada miembro activo.
5. El director confirma la finalización.
6. El sistema valida las calificaciones, guarda todo y cambia el estado a `Finalizado`.
7. Después del commit, el sistema notifica a los participantes activos que deben calificar al equipo.

### 2.3. Postcondiciones

- `Proyecto.estado = FINALIZADO`.
- `Proyecto.fechaFinalizacion` con la fecha y hora actuales del backend.
- `Proyecto.finalizacionPrematura = true` si se confirmó antes de la fecha de entrega.
- Calificaciones del director registradas en `calificacion_perfil`.
- Notificaciones `CALIFICACION_SOLICITADA` para los participantes activos.
- El proyecto aparece en `/proyectos/propios?estado=FINALIZADO` para los integrantes activos.
- Los participantes pueden calificarse entre sí mediante `POST /proyectos/{id}/calificaciones`.

---

## 3. Endpoints

### 3.1. Previsualizar finalización

```http
GET /proyectos/{idProyecto}/finalizacion
```

Requiere sesión, perfil activo, membresía activa, rol Director y permiso `FINALIZAR_PROYECTO`.

Respuesta `200`:

```json
{
  "puedeFinalizar": true,
  "estado": "Confirmado",
  "fechaEntrega": "2026-11-05",
  "fechaActual": "2026-11-05",
  "requiereConfirmacionPrematura": false,
  "diasAnticipacion": 0,
  "bloqueos": [],
  "advertencias": [],
  "miembrosACalificar": [
    {
      "idPerfil": 20,
      "nombreArtistico": "Miembro Uno",
      "nombreUsuario": "Ana",
      "apellidoUsuario": "Gómez",
      "rol": "MIEMBRO",
      "yaCalificado": false
    }
  ],
  "calificacionesRequeridas": 1,
  "calificacionesRealizadas": 0,
  "calificacionesPendientes": 1
}
```

`puedeFinalizar` indica que el proyecto cumple las condiciones de estado, fecha y actividades. La UI debe completar además las calificaciones de todos los `miembrosACalificar` antes de enviar el `POST`.

### 3.2. Finalizar

```http
POST /proyectos/{idProyecto}/finalizar
Content-Type: application/json
```

```json
{
  "confirmarFinalizacionPrematura": false,
  "calificaciones": [
    {
      "idPerfil": 20,
      "valoracion": 5,
      "resenia": "Excelente desempeño durante todo el proyecto."
    },
    {
      "idPerfil": 30,
      "valoracion": 4,
      "resenia": "Muy buena predisposición y cumplimiento."
    }
  ]
}
```

Si el preview devuelve `requiereConfirmacionPrematura: true`, el frontend debe enviar `confirmarFinalizacionPrematura: true`. Sin ese flag, el backend responde `409 CONFIRMACION_PREMATURA_REQUERIDA`.

Respuesta `200`:

```json
{
  "proyecto": {
    "idProyecto": 100,
    "nombre": "Desfile Primavera",
    "estado": "Finalizado",
    "fechaFinalizacion": "2026-11-05T15:30:00",
    "finalizacionPrematura": true
  },
  "fechaFinalizacion": "2026-11-05T15:30:00",
  "calificacionesRegistradas": 2
}
```

### 3.3. Consultar calificaciones entre participantes

```http
GET /proyectos/{idProyecto}/calificaciones
```

Requiere proyecto `Finalizado` y membresía activa.

Devuelve:

- `calificacionesCompletas`;
- `calificacionesRequeridas`;
- `calificacionesRealizadas`;
- `calificacionesPendientes`;
- `miembrosACalificar[]` con `yaCalificado`;
- `calificacionesEmitidas[]`.

### 3.4. Calificar entre participantes

```http
POST /proyectos/{idProyecto}/calificaciones
Content-Type: application/json
```

Mismo formato de `calificaciones[]` que la finalización.

Reglas:

- solo se permite cuando el proyecto está `Finalizado`;
- cada participante activo debe calificar a todos los demás participantes activos;
- se deben enviar todas las calificaciones pendientes en una sola solicitud;
- no se permite calificar dos veces al mismo compañero;
- la reseña es obligatoria;
- la valoración es de 1 a 5.

---

## 4. Reglas de negocio

### 4.1. Estado y fecha

- Solo se finaliza un proyecto `Confirmado`.
- Si ya está `Finalizado`, responde `PROYECTO_YA_FINALIZADO`.
- Si está en otro estado, responde `ESTADO_PROYECTO_INVALIDO`.
- La fecha de entrega del cronograma debe existir.
- Si la fecha es menor o igual a la actual, se finaliza normalmente.
- Si la fecha es futura, el preview devuelve la advertencia `FECHA_ENTREGA_FUTURA`, `requiereConfirmacionPrematura = true` y `diasAnticipacion`.
- Si el POST no incluye `confirmarFinalizacionPrematura: true`, responde `409 CONFIRMACION_PREMATURA_REQUERIDA`.
- Si se confirma, se finaliza y se persiste `finalizacionPrematura = true`.

### 4.2. Actividades

- `Finalizada` es válida.
- `Cancelada` es válida.
- `Pendiente` y `En curso` bloquean la finalización con `ACTIVIDAD_NO_FINALIZADA`.
- Un proyecto sin actividades responde `SIN_ACTIVIDADES`.

### 4.3. Calificaciones del director

- Debe calificar a todos los miembros activos excepto a sí mismo.
- Se incluyen otros directores como destinatarios calificables.
- Si falta algún miembro, responde `409` con `CALIFICACIONES_INCOMPLETAS` y la lista `faltantes`.
- No se permite auto-calificación, perfiles ajenos al proyecto, duplicados ni calificaciones ya registradas.
- `valoracion` debe estar entre 1 y 5.
- `resenia` es obligatoria, no puede estar vacía y tiene un máximo de 200 caracteres.

### 4.4. Calificaciones entre participantes

- Ocurren después de la finalización.
- Cada miembro activo califica a todos los demás miembros activos.
- El director ya calificó al equipo durante la finalización; su vista queda completa.
- La operación es por proyecto y por par `(emisor, receptor)`.
- La constraint única `UQ_calificacion_proyecto_emisor_receptor` impide duplicados en la base.

### 4.5. Atomicidad y concurrencia

- La finalización usa lock pesimista sobre el proyecto (`findByIdParaFinalizar`).
- Si dos solicitudes intentan finalizar el mismo proyecto, la segunda recibe `PROYECTO_YA_FINALIZADO`.
- Las calificaciones y el cambio de estado se confirman en la misma transacción.

### 4.6. Historial

No existe una tabla separada de historial. El proyecto finalizado aparece en el historial propio porque:

- la membresía del perfil sigue activa;
- el proyecto está en estado `Finalizado`;
- `/proyectos/propios` admite el filtro `estado=FINALIZADO`.

### 4.7. Notificaciones

- Tipo: `CALIFICACION_SOLICITADA`.
- Destinatarios: miembros activos excepto el director que finalizó.
- Mensaje: `El proyecto '{nombre}' finalizó. Calificá a los integrantes del equipo.`
- Si `finalizacionPrematura = true`, el mensaje dice `finalizó de forma anticipada`.
- `entidadTipo = "PROYECTO"`, `entidadId = idProyecto`, `idProyecto = idProyecto`.
- La notificación incluye el campo `link`:

```json
{
  "tipo": "CALIFICACION_SOLICITADA",
  "link": "/proyectos/100/calificaciones"
}
```

Es una ruta relativa; el frontend debe anteponer su base URL o resolverla con su router.

---

## 5. Modelo de datos

### 5.1. Proyecto

Se agregó:

```sql
ALTER TABLE proyecto
    ADD COLUMN fecha_finalizacion timestamp;

ALTER TABLE proyecto
    ADD COLUMN finalizacion_prematura boolean NOT NULL DEFAULT false;
```

`ProyectoResponse` y `ProyectoPropioResponse` exponen `fechaFinalizacion` y `finalizacionPrematura`.

### 5.2. Calificaciones

```sql
CREATE TABLE calificacion_perfil(
    id_calificacion       int8 GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    id_proyecto           int8 NOT NULL,
    id_perfil_emisor      int8 NOT NULL,
    id_perfil_receptor    int8 NOT NULL,
    valoracion            int4 NOT NULL,
    resenia               varchar(200) NOT NULL,
    fecha_registro        timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT "PK_calificacion_perfil" PRIMARY KEY (id_calificacion),
    CONSTRAINT chk_calificacion_valoracion CHECK (valoracion BETWEEN 1 AND 5),
    CONSTRAINT chk_calificacion_no_autoreview CHECK (id_perfil_emisor <> id_perfil_receptor),
    CONSTRAINT chk_calificacion_resenia CHECK (btrim(resenia) <> ''),
    CONSTRAINT "UQ_calificacion_proyecto_emisor_receptor"
        UNIQUE (id_proyecto, id_perfil_emisor, id_perfil_receptor)
);
```

### 5.3. Notificación

El tipo `CALIFICACION_SOLICITADA` se agregó a `notificacion_tipo` y a `NotificacionTitulos`.

### 5.4. Migración

Archivo:

`docs/3_diseño/migraciones/2026-11-05_uc37_finalizar_proyecto.sql`  
`docs/3_diseño/migraciones/2026-11-05_uc37_finalizacion_prematura.sql`

Recordar que el directorio de migraciones está incluido en `.gitignore`; si se quiere versionar el archivo hay que forzar el `git add` o ajustar la regla.

---

## 6. Errores

| HTTP | Código | Cuándo |
|---:|---|---|
| `400` | `CALIFICACION_INVALIDA` | Valoración fuera de 1-5, reseña vacía/larga, auto-calificación, perfil ajeno o duplicado. |
| `403` | — | El perfil no es miembro activo o no tiene `FINALIZAR_PROYECTO`. |
| `404` | — | Proyecto o perfil no encontrado. |
| `404` | — | No hay perfil activo seleccionado. |
| `409` | `PROYECTO_YA_FINALIZADO` | El proyecto ya está finalizado. |
| `409` | `ESTADO_PROYECTO_INVALIDO` | El proyecto no está `Confirmado`. |
| `409` | `PLANIFICACION_NO_ENCONTRADA` | No existe planificación. |
| `409` | `FECHA_ENTREGA_NO_DEFINIDA` | La planificación no tiene fecha de entrega. |
| `409` | `CONFIRMACION_PREMATURA_REQUERIDA` | La fecha de entrega es futura y no se confirmó la finalización anticipada. |
| `409` | `SIN_ACTIVIDADES` | No hay actividades. |
| `409` | `ACTIVIDAD_NO_FINALIZADA` | Hay actividades `Pendiente` o `En curso`. |
| `409` | `CALIFICACIONES_INCOMPLETAS` | Faltan calificaciones obligatorias. |
| `409` | `PROYECTO_NO_CALIFICABLE` | Se intentó calificar entre participantes un proyecto no finalizado. |

Los errores de finalización conservan `{ message, httpStatus, timestamp }` y agregan `codigo` y `bloqueos`. Los errores de calificaciones incompletas agregan `faltantes`.

---

## 7. Recomendaciones para el frontend

1. Al abrir el diálogo de finalización, llamar al `GET /finalizacion` y mostrar `bloqueos`, `miembrosACalificar` y contadores.
2. Habilitar “Finalizar” solo si `puedeFinalizar === true`, el director completó todas las calificaciones y, si `requiereConfirmacionPrematura === true`, aceptó la confirmación explícita.
3. Mostrar `advertencias[]` y los `diasAnticipacion` cuando corresponda.
4. Enviar todas las calificaciones del director en un solo `POST /finalizar`, incluyendo `confirmarFinalizacionPrematura`.
5. Si el backend responde `CALIFICACIONES_INCOMPLETAS`, mostrar `faltantes` y mantener el diálogo abierto.
6. Si responde `CONFIRMACION_PREMATURA_REQUERIDA`, mostrar la confirmación y reintentar con el flag.
7. Tras un `200`, usar `estado = "Finalizado"`, `fechaFinalizacion` y `finalizacionPrematura`.
8. Mostrar el badge “Finalizado anticipadamente” cuando `finalizacionPrematura === true`.
9. Para la calificación posterior entre participantes, usar `GET /calificaciones` y enviar todas las pendientes en un solo `POST /calificaciones`.
10. Usar el campo `link` de la notificación para abrir `/proyectos/{id}/calificaciones`.
11. No implementar la subida de imágenes del resultado hasta que UC-19 esté disponible.

---

## 8. Tests

- `FinalizarProyectoServiceTest`
- `ContextoFinalizacionProyectoServiceTest`
- `CalificarEquipoServiceTest`
- `ProyectoControllerTest`
- `CalificacionControllerTest`
- `FinalizarProyectoRequestValidationTest`
- `CalificarEquipoRequestValidationTest`
- `ProyectoFinalizadoNotificacionListenerTest`
- `FinalizarProyectoIntegrationTest`
- `ProyectoPropioIntegrationTest`
- `NotificacionMapperTest`

La integración cubre estado, fecha, calificaciones, historial, evento, actividades canceladas, doble finalización, finalización anticipada sin/con confirmación, calificaciones entre participantes y constraint única.

---

## 9. Referencias

- `src/main/java/org/mgroko/backend/proyectos/servicio/FinalizarProyectoService.java`
- `src/main/java/org/mgroko/backend/proyectos/servicio/ContextoFinalizacionProyectoService.java`
- `src/main/java/org/mgroko/backend/proyectos/servicio/PrevisualizarFinalizacionService.java`
- `src/main/java/org/mgroko/backend/proyectos/servicio/CalificarEquipoService.java`
- `src/main/java/org/mgroko/backend/proyectos/controlador/ProyectoController.java`
- `src/main/java/org/mgroko/backend/proyectos/controlador/CalificacionController.java`
- `src/main/java/org/mgroko/backend/notificaciones/servicio/ProyectoFinalizadoNotificacionListener.java`
- `src/main/java/org/mgroko/backend/modelo/CalificacionPerfil.java`
- `docs/3_diseño/migraciones/2026-11-05_uc37_finalizar_proyecto.sql`  
`docs/3_diseño/migraciones/2026-11-05_uc37_finalizacion_prematura.sql`
