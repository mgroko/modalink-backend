# Plan de implementación — Informe ejecutivo de estado y salud del proyecto

> **Propuesta base:** Propuesta 1 de `docs/4_pendiente/propuestas-informes.md`.
>
> **Estado:** plan de implementación. No describe código ya implementado.
>
> **Fecha:** 2026-11-25
>
> **Actualización 2026-11-26:** se reemplazó el índice numérico de salud por estado por área derivado de alertas, se incorporó el bloque de riesgo para la entrega, la antigüedad en decisiones y el diseño de encabezado/pie con metadatos.
>
> **Estado de implementación 2026-11-26:** MVP backend implementado: DTOs, estado por área, alertas, endpoints JSON/PDF, renderer con moodboard WebP, encabezado/pie y tests unitarios, MVC e integración. Los tests específicos del informe pasan. La suite completa no quedó verde por una falla preexistente en `ConfirmarProyectoIntegrationTest` y por artefactos de compilación obsoletos ajenos a este cambio.
>
> **Objetivo:** que un director activo pueda obtener, para un proyecto, un informe ejecutivo en JSON y PDF con estado, salud, moodboard, equipo, cronograma, contratos, riesgos e historial, sin modificar datos de negocio y sin cambios de esquema en el MVP.

---

## 1. Resumen ejecutivo

### 1.1. Alcance del MVP

Incluye:

- endpoint JSON de solo lectura para el director activo;
- endpoint PDF descargable;
- ficha del proyecto y resumen ejecutivo con estado por área;
- riesgo para la fecha de entrega y decisiones con antigüedad;
- moodboard con imágenes incrustadas;
- equipo activo e histórico y cobertura de requerimientos;
- resumen de cronograma CPM, avance, margen, conflictos y actividades vencidas;
- resumen de contratos y firmas;
- alertas priorizadas y decisiones sugeridas;
- historial de miembros y calificaciones agregadas con reglas de privacidad;
- pruebas unitarias, MVC, integración con PostgreSQL y PDF;
- documentación de endpoint y manual.

No incluye en el MVP:

- comparación contra un informe anterior;
- envío automático por email;
- variante para administrador;
- exportación CSV;
- hitos completos con fecha límite y cumplimiento;
- presupuesto, costos u honorarios;
- frontend;
- cambios de esquema.

### 1.2. Duración estimada

| Fase | Duración |
|---|---:|
| Fase 0 — Definición | 1–2 días |
| Fase 1 — Modelo de lectura y JSON | 3–4 días |
| Fase 2 — PDF | 4–5 días |
| Fase 3 — Testing y hardening | 2–3 días |
| Fase 4 — Documentación y demo | 1 día |
| **Total MVP** | **11–15 días hábiles** |

Con un desarrollador backend dedicado y revisiones diarias, el rango realista es **2,5 a 3 semanas**. Si se agrega comparación histórica o email programado, sumar 1–2 semanas.

### 1.3. Definition of Done

- [ ] El director activo obtiene `200` en JSON y PDF.
- [ ] Un miembro no director, un no miembro, un perfil inactivo o un usuario no autenticado reciben el error correcto.
- [ ] El JSON contiene todas las secciones y valores consistentes con la base.
- [ ] El PDF incluye portada, resumen, moodboard, equipo, cronograma, contratos, alertas e historial.
- [ ] Las imágenes WebP se convierten e incrustan correctamente; si falta una, el PDF no falla.
- [ ] El estado de cada área es determinista con un `Clock` fijo y se deriva únicamente de las alertas visibles en el informe.
- [ ] Ningún área aparece en verde si no tiene datos.
- [ ] El estado general coincide con el área más grave.
- [ ] Los estados vacíos se muestran como “sin datos”, no como “todo bien”.
- [ ] `mvn verify` finaliza en verde.
- [ ] No hay cambios de esquema en el MVP.
- [ ] El endpoint y el manual están documentados.
- [ ] Se revisó que no se expongan datos personales sensibles innecesarios.

---

## 2. Decisiones de diseño

| Decisión | Justificación |
|---|---|
| Solo lectura | El informe no debe alterar estados, fechas ni contratos |
| Solo director activo | Es el rol que toma decisiones sobre el proyecto |
| Sin cambios de esquema en MVP | Todo se puede derivar de tablas y servicios existentes |
| JSON + PDF | JSON para UI; PDF para compartir y defender |
| Reutilizar CPM existente | `ObtenerCronogramaService` ya calcula margen, viabilidad y ruta crítica |
| Reutilizar cupos existentes | `CupoRequerimientoService` es la fuente única de verdad |
| Reutilizar estilos PDF | `ModaLinkPdfEstilos` y `ModaLinkPdfPageEvent` |
| Renderizado PDF fuera de transacción | Evita mantener una conexión de base durante la conversión de imágenes |
| Imágenes WebP convertidas a JPEG/PNG | OpenPDF no soporta WebP nativo |
| Sin puntaje; estado por área derivado de alertas | Un número agregado oculta problemas y obliga a justificar pesos. El color es consecuencia de alertas visibles |
| Sin datos sensibles | No incluir DNI, correo, teléfono, mensajes privados ni textos de reseñas de terceros |

### 2.1. Modos del informe

| Estado del proyecto | Modo | Áreas evaluadas |
|---|---|---|
| `BORRADOR` | `BORRADOR` | `BRIEF` y `EQUIPO`; `CRONOGRAMA` y `CONTRATOS` en `NO_APLICA` |
| `PUBLICADO` | `OPERATIVO` | Las cuatro áreas |
| `CONFIRMADO` | `OPERATIVO` | Las cuatro áreas, con foco en entrega, contratos y equipo |
| `FINALIZADO` | `CIERRE` | Las cuatro, solo con alertas `INFO` y `BAJA`; ninguna pasa de `VERDE` |
| `CANCELADO` | `CIERRE` | Las cuatro, solo con alertas `INFO` y `BAJA`; incluye motivo y fecha de cancelación |

---

## 3. Arquitectura propuesta

### 3.1. Flujo

```text
GET /proyectos/{id}/informe
        |
        v
InformeProyectoController
        |
        v
GenerarInformeProyectoService  (transacción read-only)
  - valida usuario/perfil/director
  - carga proyecto, planificación, objetivos, requerimientos, moodboard
  - carga miembros, contratos, firmas, actividades, cronograma
  - carga pendientes y calificaciones
  - delega en DetectarAlertasProyectoService
  - delega en CalcularEstadoAreasService (usa las alertas)
        |
        v
InformeProyectoResponse (DTO completo)
        |
        +--> JSON al frontend
        |
        +--> InformeProyectoPdfRenderer (fuera de transacción)
                 |
                 +--> PdfImagenResolver (WebP -> JPEG/PNG)
                 +--> ModaLinkPdfPageEvent
```

### 3.2. Paquetes y archivos

Se sigue la estructura existente de `org.mgroko.backend.proyectos`.

#### Archivos nuevos

| Archivo | Responsabilidad |
|---|---|
| `src/main/java/org/mgroko/backend/proyectos/controlador/InformeProyectoController.java` | Endpoints JSON y PDF |
| `src/main/java/org/mgroko/backend/proyectos/dto/InformeProyectoResponse.java` | DTO principal con records anidados |
| `src/main/java/org/mgroko/backend/proyectos/servicio/GenerarInformeProyectoService.java` | Orquestación y carga de datos |
| `src/main/java/org/mgroko/backend/proyectos/servicio/CalcularEstadoAreasService.java` | Mapea alertas a estado por área y calcula el riesgo de entrega |
| `src/main/java/org/mgroko/backend/proyectos/servicio/DetectarAlertasProyectoService.java` | Alertas y decisiones |
| `src/main/java/org/mgroko/backend/proyectos/mapper/InformeProyectoMapper.java` | Entidades -> DTO |
| `src/main/java/org/mgroko/backend/proyectos/pdf/InformeProyectoPdfRenderer.java` | Render del PDF |
| `src/main/java/org/mgroko/backend/proyectos/pdf/InformeProyectoPdfPageEvent.java` | Barra y pie del PDF |
| `src/main/java/org/mgroko/backend/proyectos/pdf/PdfImagenResolver.java` | Resolución segura y conversión de imágenes |
| `src/main/java/org/mgroko/backend/proyectos/exception/ErrorGenerandoInformeProyectoException.java` | Error de generación |

#### Archivos a modificar

| Archivo | Cambio |
|---|---|
| `src/main/java/org/mgroko/backend/common/exception/GlobalExceptionHandler.java` | Handler para `ErrorGenerandoInformeProyectoException` |
| `README.md` | Documentar endpoints |
| `docs/manual/06-proyectos-directores.md` | Sección “Informe de estado del proyecto” |
| `docs/5_estado_sistema/estado_uc.md` | Registrar la extensión como funcionalidad no numerada |
| `src/main/resources/application.properties` | Propiedades de umbrales e imágenes, si se decide configurabilidad |

#### Posibles modificaciones a repositorios

Solo si no alcanzan las consultas existentes:

| Repositorio | Consulta sugerida |
|---|---|
| `PostulacionGralRepository` | Resumen de pendientes por proyecto: total y fecha de la más antigua |
| `PostulacionActividadRepository` | Ídem para postulaciones de actividad |
| `InvitacionGralRepository` | Resumen de pendientes por proyecto |
| `InvitacionActividadRepository` | Resumen de pendientes por proyecto |

No se requieren migraciones. Si se agregan proyecciones, deben ser records en `repositorio/proyeccion`.

---

## 4. Contrato de API

### 4.1. `GET /proyectos/{idProyecto}/informe`

- **Rol:** `USUARIO`.
- **Autorización de negocio:** perfil activo y director activo del proyecto.
- **Respuesta:** `200 application/json`.
- **Errores:**
  - `401` no autenticado.
  - `403` perfil sin acceso, no director o perfil inactivo.
  - `404` proyecto inexistente.
  - `500` error inesperado.

Ejemplo abreviado:

```json
{
  "proyecto": {
    "idProyecto": 12,
    "nombre": "Campaña Primavera",
    "descripcion": "Producción editorial de moda",
    "estado": "CONFIRMADO",
    "privacidad": "PUBLICO",
    "fechaInicio": "2026-11-01",
    "fechaEntrega": "2026-12-15",
    "fechaFinalizacion": null,
    "fechaCancelacion": null,
    "finalizacionPrematura": false,
    "ubicacion": { "ciudad": "Rosario", "provincia": "Santa Fe" },
    "director": { "idPerfil": 8, "nombreArtistico": "Ana Dir" }
  },
  "generadoEn": "2026-11-25T10:00:00",
  "impresoEn": "2026-11-25T10:00:03",
  "generadoPor": {
    "idPerfil": 8,
    "nombreArtistico": "Ana Dir",
    "rol": "DIRECTOR"
  },
  "modo": "OPERATIVO",
  "versionInforme": "1.0",
  "resumen": {
    "estadoGeneral": "ROJO",
    "areas": [
      {
        "codigo": "CRONOGRAMA",
        "estado": "ROJO",
        "motivo": "2 actividades vencidas y 1 conflicto. Margen de 3 días."
      },
      {
        "codigo": "EQUIPO",
        "estado": "ROJO",
        "motivo": "Falta 1 fotógrafo. 1 miembro sin aceptar T&C."
      },
      {
        "codigo": "CONTRATOS",
        "estado": "AMARILLO",
        "motivo": "1 contrato pendiente de firma. Firmas: 4 de 6."
      },
      {
        "codigo": "BRIEF",
        "estado": "VERDE",
        "motivo": "5 de 6 ítems completos."
      }
    ],
    "riesgoEntrega": {
      "fechaFinProyectada": "2026-12-12",
      "fechaEntrega": "2026-12-15",
      "margenDias": 3,
      "criticasSinIniciar": 2,
      "vencidas": 2,
      "conflictos": 1,
      "avanceActividades": 0.62
    },
    "decisiones": [
      {
        "prioridad": "ALTA",
        "codigo": "POSTULACIONES_PENDIENTES",
        "mensaje": "Falta 1 fotógrafo; 5 postulaciones sin responder",
        "antiguedadDias": 9,
        "accionSugerida": "Responder postulaciones"
      }
    ]
  },
  "metricas": {
    "avanceActividades": 0.62,
    "avancePonderado": 0.58,
    "margenDias": 3,
    "margenMinutos": 4320,
    "coberturaCupos": 0.83,
    "estabilidadEquipo": 0.9,
    "preparacionContractual": 0.75,
    "completitudBrief": 0.83,
    "actividadesVencidas": 2,
    "postulacionesPendientes": 5,
    "contratosPendientesFirma": 1
  },
  "moodboard": {
    "idMoodboard": 3,
    "descripcion": "Paleta cálida, luz natural",
    "fechaCreacion": "2026-11-02T09:00:00",
    "totalImagenes": 8,
    "limiteConfigurado": 10,
    "imagenes": [
      { "idImagen": 1, "url": "/uploads/moodboard/moodboard_abc.webp" }
    ]
  },
  "equipo": {
    "totalActivos": 7,
    "totalDirectores": 1,
    "totalBajas": 2,
    "miembros": [
      {
        "idMiembro": 21,
        "idPerfil": 15,
        "nombreArtistico": "Luz Foto",
        "profesion": "Fotógrafo",
        "rol": "MIEMBRO",
        "estadoParticipacion": "ACTIVO",
        "fechaAlta": "2026-11-05T10:00:00",
        "fechaBaja": null,
        "motivoBaja": null,
        "tycAceptado": true
      }
    ],
    "miembrosHistoricos": [],
    "requerimientos": [
      {
        "idRequerimientoGral": 4,
        "profesion": "Fotógrafo",
        "descripcion": "Fotografía principal",
        "cantidad": 2,
        "cubiertos": 1,
        "faltantes": 1,
        "porcentajeCobertura": 0.5
      }
    ]
  },
  "cronograma": {
    "fechaInicio": "2026-11-01T09:00:00",
    "fechaFinProyectada": "2026-12-12T18:00:00",
    "fechaEntrega": "2026-12-15",
    "margenMinutos": 4320,
    "margenDias": 3,
    "viable": true,
    "estadoCronograma": "AJUSTADO",
    "totalActividades": 20,
    "actividadesCriticas": 5,
    "actividadesVencidas": 2,
    "conflictos": 1,
    "porEstado": {
      "PENDIENTE": 8,
      "EN_CURSO": 2,
      "FINALIZADA": 10,
      "CANCELADA": 0
    },
    "proximasActividades": []
  },
  "contratos": {
    "total": 3,
    "porEstado": {
      "BORRADOR": 0,
      "PENDIENTE_FIRMA": 1,
      "ACTIVO": 2,
      "RECHAZADO_POR_PARTES": 0,
      "CANCELADO": 0,
      "ELIMINADO": 0
    },
    "firmas": { "firmadas": 4, "pendientes": 2, "total": 6 },
    "contratos": []
  },
  "alertas": [
    {
      "codigo": "CUPOS_SIN_CUBRIR",
      "area": "EQUIPO",
      "severidad": "ALTA",
      "mensaje": "Falta 1 fotógrafo",
      "antiguedadDias": 9,
      "accionSugerida": "Reclutar o reasignar"
    }
  ],
  "historial": [],
  "calificaciones": {
    "disponible": false,
    "promedio": null,
    "cantidad": 0,
    "totalEsperado": 0
  }
}
```

### 4.2. `GET /proyectos/{idProyecto}/informe/pdf`

- **Rol:** `USUARIO`.
- **Autorización de negocio:** ídem JSON.
- **Respuesta:** `200 application/pdf`.
- **Headers:**
  - `Content-Type: application/pdf`
  - `Content-Disposition: attachment; filename="informe_proyecto_12_20261125.pdf"`
  - `Cache-Control: no-store`
  - `X-Content-Type-Options: nosniff`
- **Generación en memoria:** igual que `AdminAuditoriaController`, para que un error se resuelva como JSON antes de enviar headers de descarga.
- **Metadatos del documento:** el PDF debe incluir proyecto, corte, generado por, modo, versión, sistema, fecha de impresión y “Hoja X / Y”, siguiendo el patrón del informe de auditoría.
- **Límite:** si el PDF supera el tamaño máximo configurado, responder `500` con código `INFORME_DEMASIADO_GRANDE` o reducir imágenes automáticamente. Preferencia: reducir imágenes y continuar.

### 4.3. Encabezado, pie y metadatos del PDF

El informe de auditoría ya resuelve este patrón en `AuditoriaPdfRenderer.metadatos(...)` y `ModaLinkPdfPageEvent`. El informe de proyecto debe reutilizarlo con sus propios campos.

#### Barra superior, en todas las páginas

| Zona | Contenido |
|---|---|
| Izquierda | `ModaLink · Informe de proyecto` |
| Derecha | `{nombre del proyecto} · Corte {dd/MM/yyyy HH:mm}` |

La barra debe ser discreta, igual que en auditoría: fondo azul, texto blanco, sin competir con el título de portada. Si el nombre del proyecto es largo, truncarlo a 60 caracteres con elipsis.

#### Bloque de metadatos, solo en la primera página

Reemplaza o complementa la fila de ficha del proyecto:

| Campo | Valor |
|---|---|
| `PROYECTO` | Nombre + `#idProyecto` |
| `CORTE` | `generadoEn` con fecha y hora |
| `GENERADO POR` | Nombre artístico del director que generó el informe |
| `MODO` | `BORRADOR`, `OPERATIVO` o `CIERRE` |
| `VERSIÓN` | `versionInforme` (por ejemplo `1.0`) |
| `SISTEMA` | `ModaLink` |

En auditoría el campo equivalente se llama `GENERADO POR`; se mantiene ese nombre para conservar la consistencia entre documentos. Ese campo es el que identifica a quien generó/imprimió el informe.

Si se quiere ser consistente con auditoría, usar una tabla de 4–6 celdas con fondo `FONDO`, borde `BORDE` y etiquetas en mayúsculas pequeñas.

#### Pie de página, en todas las páginas

| Zona | Contenido |
|---|---|
| Izquierda | `Impreso el {dd/MM/yyyy HH:mm} · Generado por {nombre} · Foto del momento, no reemplaza la auditoría` |
| Centro | `ModaLink · Informe de proyecto` |
| Derecha | `Hoja X / Y` |

Reglas:

- `impresoEn` se captura al renderizar; `generadoEn` se captura al armar el DTO. Pueden diferir por segundos.
- `generadoPor` identifica al director que solicitó el informe. Si el PDF se imprime después, conserva ese dato; no se intenta identificar a quien lo imprime físicamente.
- No incluir correo, DNI ni usuario completo en el pie.
- Si el espacio es insuficiente, priorizar: fecha de impresión, “Generado por”, número de hoja. El aviso legal puede ir en el bloque “Cómo leer este informe”.
- En modo demo se puede agregar “Datos de ejemplo”, pero nunca en producción.
- El pie debe ser legible en blanco y negro.

### 4.4. Notas de seguridad

- No agregar `/proyectos/**` a rutas públicas: ya está protegido por `SecurityConfig` con rol `USUARIO`.
- La autorización fina se hace en el servicio con `ProyectoSecurityService.validarDirector`.
- No confiar en el `idPerfilActivo` enviado por el cliente: se toma de `ContextoAutenticacion`.
- No exponer rutas absolutas del sistema de archivos en el JSON.
- El resolver de imágenes debe aceptar solo URLs que empiecen con `/uploads/` y resolverlas dentro de `app.storage.upload-dir`, rechazando `..`, rutas absolutas y esquemas externos.

---

## 5. Modelo de datos del informe

`InformeProyectoResponse` puede definirse como un record principal con records anidados, siguiendo el estilo de `AdminDashboardResumen`. Si el archivo queda demasiado grande, separar en:

- `InformeProyectoResponse`
- `InformeProyectoResumen`
- `InformeProyectoMetricas`
- `InformeProyectoMoodboard`
- `InformeProyectoEquipo`
- `InformeProyectoCronograma`
- `InformeProyectoContratos`
- `InformeProyectoAlerta`
- `InformeProyectoHistorial`
- `InformeProyectoCalificaciones`

### 5.1. Campos mínimos por sección

| Sección | Campos |
|---|---|
| Proyecto | id, nombre, descripción, estado, privacidad, fechas, finalización prematura, ubicación, director |
| Generación | `generadoEn`, `impresoEn`, `generadoPor`, `modo`, `versionInforme` |
| Resumen | `estadoGeneral`, áreas con motivo, `riesgoEntrega`, decisiones con antigüedad |
| Métricas | avance simple/ponderado, margen, cobertura, estabilidad, contratos, brief, vencidas, pendientes |
| Moodboard | id, descripción, fecha, total, límite, imágenes |
| Equipo | activos, directores, bajas, miembros, históricos, requerimientos |
| Cronograma | resumen CPM, por estado, vencidas, conflictos, próximas actividades |
| Contratos | total, por estado, firmas, detalle |
| Alertas | código, `area`, severidad, mensaje, `antiguedadDias`, acción, entidad |
| Historial | eventos de baja/eliminación y cambios relevantes |
| Calificaciones | disponible, promedio, cantidad, total esperado |

### 5.2. Reglas de privacidad del DTO

- Miembros: nombre artístico, profesión, rol, estado y fechas. No incluir DNI, correo, teléfono ni usuario completo.
- Director: nombre artístico e id. No incluir correo.
- Contratos: nombre, estado, versión y conteo de firmas. No incluir texto completo del contrato.
- Calificaciones: solo agregados y conteos. No incluir reseñas textuales de terceros.
- Historial: nombres artísticos y motivos de baja; no incluir datos de auditoría como IP u operador.

---

## 6. Reglas de negocio y métricas

### 6.1. Validaciones de acceso

1. `idPerfilActivo` no nulo; si es nulo, `PerfilActivoNoSeleccionadoException`.
2. Perfil perteneciente al usuario autenticado y en estado `ACTIVO`; si no, `PerfilNoEncontradoException` o `PerfilEnBajaException`.
3. Proyecto existente; si no, `ProyectoNoEncontradoException`.
4. `ProyectoSecurityService.validarDirector(idProyecto, idPerfilActivo)`; si no es director activo, `AccesoDenegadoProyectoException` (`403`).
5. No permitir administradores en el endpoint de usuario; la variante admin queda para fase 2.

### 6.2. Fórmulas

#### Avance

```text
totalNoCanceladas = actividades con estado != CANCELADA
avanceActividades = finalizadas / totalNoCanceladas
avancePonderado = suma(duracionMinutos de finalizadas) / suma(duracionMinutos de no canceladas)
```

Si no hay actividades: `null`, no `0`.

#### Margen

```text
margenMinutos = resumenCronograma.margenMinutos
margenDias = round(margenMinutos / 1440)
```

Si no hay fecha de entrega: `null` y alerta de información incompleta.

#### Cobertura de cupos

```text
Por requerimiento:
  cubiertos = miembros ACTIVO vinculados al requerimiento
  faltantes = max(0, cantidad - cubiertos)
  porcentaje = cantidad > 0 ? cubiertos / cantidad : 1

Global:
  cobertura = suma(cubiertos) / suma(cantidad)
```

Solo requerimientos no archivados (`fechaArchivado == null`) para la métrica principal. Los archivados se listan en anexo.

#### Estabilidad del equipo

Dato informativo para la sección de equipo; no define el estado del área.

```text
totalMiembrosHistoricos = activos + bajas voluntarias + eliminados
bajas = bajas voluntarias + eliminados
estabilidad = totalMiembrosHistoricos > 0 ? 1 - (bajas / totalMiembrosHistoricos) : 1
```

#### Preparación contractual

```text
contratosConsiderados = contratos en PENDIENTE_FIRMA o ACTIVO
firmasFirmadas = firmas en estado FIRMADO
firmasTotales = firmas en estado PENDIENTE o FIRMADO
preparacion = firmasTotales > 0 ? firmasFirmadas / firmasTotales : null
```

Si no hay contratos considerados: `null` y área `CONTRATOS` en `SIN_DATOS`.

#### Completitud del brief

Seis ítems, cada uno vale 1/6:

1. descripción no vacía;
2. al menos un objetivo;
3. al menos un requerimiento;
4. moodboard con al menos una imagen;
5. ubicación cargada;
6. fecha de entrega cargada.

```text
completitud = itemsCompletos / 6
```

#### Actividades vencidas

Para cada actividad `PENDIENTE` o `EN_CURSO` con `fechaHoraInicio` no nula:

```text
finCalculado = fechaHoraInicio + duracionMinutos
vencida = finCalculado < ahora
```

Si `fechaHoraInicio` es nula y el proyecto está `CONFIRMADO`, generar alerta de dato inconsistente.

#### Velocidad (MVP aproximado)

No hay `fecha_finalizacion` de actividad. Para el MVP:

```text
semanasTranscurridas = max(1, diasDesdeInicio / 7)
velocidadAproximada = actividadesFinalizadas / semanasTranscurridas
```

Etiquetar como “ritmo promedio desde el inicio”, no como “velocidad semanal exacta”. La versión exacta queda para fase 2 con auditoría o nuevo campo.

### 6.3. Estado por área

No hay puntaje numérico. El estado de cada área se deriva de sus alertas.

#### Regla de color

| Condición del área | Estado |
|---|---|
| Tiene al menos una alerta `ALTA` | `ROJO` |
| Tiene alertas `MEDIA` y ninguna `ALTA` | `AMARILLO` |
| Sin alertas, o solo `BAJA`/`INFO`, y con datos | `VERDE` |
| Sin alertas y sin datos para evaluarla | `SIN_DATOS` |
| El modo del informe no evalúa esa área | `NO_APLICA` |

#### Mapeo alerta → área

| Área | Códigos de alerta |
|---|---|
| `CRONOGRAMA` | `SIN_FECHA_ENTREGA`, `SIN_ACTIVIDADES`, `CRONOGRAMA_INVIABLE`, `MARGEN_NEGATIVO`, `MARGEN_AJUSTADO`, `CONFLICTOS_CRONOGRAMA`, `ACTIVIDADES_VENCIDAS`, `ACTIVIDADES_CRITICAS_SIN_INICIAR` |
| `EQUIPO` | `SIN_REQUERIMIENTOS`, `CUPOS_SIN_CUBRIR`, `MIEMBROS_SIN_TYC`, `POSTULACIONES_PENDIENTES`, `INVITACIONES_PENDIENTES` |
| `CONTRATOS` | `CONTRATOS_PENDIENTES_FIRMA` |
| `BRIEF` | `SIN_MOODBOARD`, `MOODBOARD_INCOMPLETO`, `BRIEF_INCOMPLETO` |

Toda alerta operativa nueva debe declarar su área. Un test debe verificar que no quede ningún código operativo sin mapear.

#### Estado general

- Es el estado más grave entre las áreas, con el orden `ROJO` > `AMARILLO` > `VERDE`.
- `SIN_DATOS` y `NO_APLICA` no cuentan.
- Si todas las áreas son `SIN_DATOS` o `NO_APLICA`, el estado general es `SIN_DATOS`.

#### Modos del informe

| Modo | Áreas evaluadas |
|---|---|
| `BORRADOR` | `BRIEF` y `EQUIPO`; `CRONOGRAMA` y `CONTRATOS` en `NO_APLICA` |
| `OPERATIVO` | Las cuatro |
| `CIERRE` | Las cuatro, solo con alertas `INFO` y `BAJA`; ninguna pasa de `VERDE` |

#### Motivo de cada área

- Es una línea generada a partir de las alertas del área, de mayor a menor severidad.
- Máximo dos frases.
- Si el área es `VERDE`, resume el dato principal (por ejemplo, “5 de 6 ítems completos”).
- Si el área es `SIN_DATOS`, indicar qué dato falta.

#### Riesgo para la entrega

Se calcula con los datos existentes del cronograma CPM:

```text
fechaFinProyectada = resumenCronograma.fechaFinProyectada
fechaEntrega       = planificacion.fechaEntrega
margenDias         = round(margenMinutos / 1440)
criticasSinIniciar = actividades en ruta crítica con estado PENDIENTE
vencidas           = actividades no finalizadas con fin calculado vencido
conflictos         = resumenCronograma.conflictos.size()
avanceActividades  = finalizadas / totalNoCanceladas
```

Si no hay fecha de entrega o no hay actividades, el bloque se muestra como “Sin datos”.

#### Antigüedad de alertas y decisiones

| Alerta | Fecha de referencia |
|---|---|
| `POSTULACIONES_PENDIENTES`, `INVITACIONES_PENDIENTES` | La pendiente más antigua |
| `CONTRATOS_PENDIENTES_FIRMA` | Fecha del contrato en `PENDIENTE_FIRMA` |
| `ACTIVIDADES_VENCIDAS` | El fin calculado más antiguo |
| `MIEMBROS_SIN_TYC` | Fecha de alta del miembro |
| Resto | `null` |

```text
antiguedadDias = días entre la fecha de referencia y ahora (Clock)
```

#### Estabilidad del equipo

Sigue existiendo como dato informativo en la sección de equipo, pero no define el estado del área:

```text
totalMiembrosHistoricos = activos + bajas voluntarias + eliminados
bajas = bajas voluntarias + eliminados
estabilidad = totalMiembrosHistoricos > 0 ? 1 - (bajas / totalMiembrosHistoricos) : 1
```

#### Explicabilidad

Cada área debe devolver:

- `codigo`;
- `estado`;
- `motivo`;
- opcionalmente `metricas` con los valores usados.

No se devuelve `peso`, `puntaje` ni `indiceSalud`.

### 6.4. Catálogo de alertas

| Código | Área | Severidad | Condición | Antigüedad | Acción sugerida |
|---|---|---|---|---|---|
| `SIN_FECHA_ENTREGA` | CRONOGRAMA | ALTA | Proyecto no borrador y sin fecha de entrega | `null` | Completar planificación |
| `SIN_MOODBOARD` | BRIEF | MEDIA | Proyecto publicado/confirmado sin imágenes | `null` | Cargar referencias visuales |
| `MOODBOARD_INCOMPLETO` | BRIEF | BAJA | Menos imágenes que el mínimo esperado o sin descripción | `null` | Completar el concepto |
| `BRIEF_INCOMPLETO` | BRIEF | MEDIA | El brief no tiene los 6 ítems completos y no hay otra alerta de brief | `null` | Completar la información del proyecto |
| `SIN_REQUERIMIENTOS` | EQUIPO | ALTA | Proyecto publicado/confirmado sin requerimientos | `null` | Definir el equipo necesario |
| `CUPOS_SIN_CUBRIR` | EQUIPO | ALTA | Faltantes > 0 en proyecto confirmado | `null` | Reclutar o reasignar |
| `CUPOS_SIN_CUBRIR` | EQUIPO | MEDIA | Faltantes > 0 en proyecto publicado | `null` | Revisar postulaciones |
| `CRONOGRAMA_INVIABLE` | CRONOGRAMA | ALTA | `viable == false` | `null` | Reprogramar |
| `MARGEN_NEGATIVO` | CRONOGRAMA | ALTA | `margenDias < 0` | `null` | Acortar alcance o reprogramar |
| `MARGEN_AJUSTADO` | CRONOGRAMA | MEDIA | `0 <= margenDias < 7` | `null` | Revisar plan |
| `CONFLICTOS_CRONOGRAMA` | CRONOGRAMA | ALTA | `conflictos > 0` | `null` | Resolver conflictos |
| `ACTIVIDADES_VENCIDAS` | CRONOGRAMA | ALTA | Alguna actividad no finalizada con fin calculado vencido | Fin calculado más antiguo | Reprogramar |
| `ACTIVIDADES_CRITICAS_SIN_INICIAR` | CRONOGRAMA | ALTA | Actividad en ruta crítica y estado `PENDIENTE` | `null` | Priorizar inicio |
| `CONTRATOS_PENDIENTES_FIRMA` | CONTRATOS | MEDIA/ALTA | Contrato `PENDIENTE_FIRMA` con más de N días | Fecha de emisión del contrato | Escalar firma |
| `MIEMBROS_SIN_TYC` | EQUIPO | MEDIA | Miembro activo sin `idTycAceptado` | Fecha de alta del miembro | Solicitar aceptación |
| `POSTULACIONES_PENDIENTES` | EQUIPO | MEDIA | Pendientes > 0; ALTA si la más antigua supera N días | Postulación más antigua | Responder postulaciones |
| `INVITACIONES_PENDIENTES` | EQUIPO | MEDIA | Pendientes > 0; ALTA si la más antigua supera N días | Invitación más antigua | Hacer seguimiento |
| `SIN_ACTIVIDADES` | CRONOGRAMA | MEDIA | Proyecto confirmado sin actividades | `null` | Planificar |
| `PROYECTO_CANCELADO` | — | INFO | Estado `CANCELADO` | `null` | Consultar motivo |
| `SIN_CALIFICACIONES` | — | BAJA | Proyecto finalizado y calificaciones incompletas | `null` | Promover calificación |

Reglas:

- En modo `CIERRE` no se generan alertas operativas; solo `INFO` y `BAJA`.
- Ordenar por severidad y, dentro de ella, por antigüedad descendente.
- Las decisiones del resumen son las primeras 3 alertas `ALTA`; si no hay, las 3 `MEDIA`; si no hay, un mensaje de “sin acciones críticas”.
- Evitar duplicados: agrupar por código y entidad.
- Toda alerta debe declarar `area`; las alertas `INFO`/`BAJA` pueden usar `null` para no afectar el color.

### 6.5. Umbrales configurables

Propiedades sugeridas en `application.properties`:

```properties
app.informe-proyecto.margen-ajustado-dias=7
app.informe-proyecto.contrato-pendiente-dias=7
app.informe-proyecto.postulacion-pendiente-dias=7
app.informe-proyecto.max-imagenes-pdf=12
app.informe-proyecto.max-imagen-px=1200
app.informe-proyecto.max-bytes-pdf=15728640
```

Si no se quieren agregar propiedades, usar constantes en `CalcularEstadoAreasService` y `DetectarAlertasProyectoService`, pero las propiedades facilitan ajustes sin recompilar.

---

## 7. Plan por fases

### Fase 0 — Definición y acuerdos (1–2 días)

| # | Tarea | Salida |
|---|---|---|
| 0.1 | Confirmar alcance: solo director activo, todos los estados, sin admin | Acta de alcance |
| 0.2 | Validar la regla de color por área, el mapeo alerta → área y los umbrales | Tabla aprobada |
| 0.3 | Validar estructura del JSON y del PDF | Mock/wireframe |
| 0.4 | Confirmar política de privacidad | Lista de campos permitidos/prohibidos |
| 0.5 | Confirmar que no habrá cambios de esquema en MVP | Riesgos aceptados |

**Criterio de salida:** decisiones firmadas y sin preguntas bloqueantes.

### Fase 1 — Modelo de lectura y endpoint JSON (3–4 días)

| # | Tarea | Archivos |
|---|---|---|
| 1.1 | Crear DTO principal y records anidados | `proyectos/dto/InformeProyectoResponse.java` |
| 1.2 | Crear servicio orquestador con validación de director | `proyectos/servicio/GenerarInformeProyectoService.java` |
| 1.3 | Crear detector de alertas y decisiones; se ejecuta antes del estado | `proyectos/servicio/DetectarAlertasProyectoService.java` |
| 1.4 | Crear `CalcularEstadoAreasService` puro y testeable; usa las alertas | `proyectos/servicio/CalcularEstadoAreasService.java` |
| 1.5 | Crear mapper entidades -> DTO | `proyectos/mapper/InformeProyectoMapper.java` |
| 1.6 | Agregar consultas de pendientes si faltan | Repositorios de postulaciones/invitaciones |
| 1.7 | Crear controller con endpoint JSON | `proyectos/controlador/InformeProyectoController.java` |
| 1.8 | Agregar logs de duración y tamaño | Servicio/controller |
| 1.9 | Tests unitarios de estado por área, alertas y servicio | `src/test/java/.../proyectos/servicio/` |
| 1.10 | Test MVC del endpoint y autorización | `src/test/java/.../proyectos/controlador/` |

#### Detalle de carga de datos

| Sección | Fuente |
|---|---|
| Proyecto y ubicación | `ProyectoRepository.findByIdConUbicacion` |
| Director | `MiembroProyectoRepository.buscarDirectoresDeProyectos` o `findMiembroActivoConRol` |
| Planificación y fecha de entrega | `PlanificacionRepository.findByProyectoIdProyecto` |
| Objetivos | `ObjetivoRepository.findByProyectoIdProyecto` |
| Requerimientos | `RequerimientoGralProyectoRepository.findByProyectoIdProyecto` |
| Cobertura | `CupoRequerimientoService.calcularGenerales` |
| Moodboard | `MoodboardRepository.findByProyectoIdProyecto` |
| Miembros | `MiembroProyectoRepository.buscarTodosDeProyectoConPerfilProfesionYRol` |
| Actividades | `ActividadRepository.findConPredecesorasByPlanificacion` |
| Cronograma | `ObtenerCronogramaService.obtener` |
| Contratos | `ContratoRepository.findByProyectoIdProyectoConTipo` |
| Firmas | `FirmaContratoRepository.findConPartesPorContratos` |
| Pendientes | Repositorios de postulaciones/invitaciones |
| Calificaciones | `CalificacionPerfilRepository.findByProyectoIdProyecto` |
| Límite de moodboard | `ConfiguracionSistemaService.obtenerMaxImagenesMoodboard` |

#### Reglas de implementación

- El servicio debe ser `@Transactional(readOnly = true)` y mapear todo a DTO antes de salir.
- No llamar al renderer PDF dentro de la transacción.
- Usar `Clock` inyectado para `ahora`, `generadoEn` y cálculos de vencimiento.
- Reutilizar `ObtenerCronogramaService` en lugar de duplicar CPM.
- Si `ObtenerCronogramaService` falla por falta de planificación, devolver cronograma vacío y alerta, no un `500`.
- Los requerimientos archivados no deben afectar la cobertura principal.
- Los miembros históricos no deben contar como cupo cubierto.

**Criterio de salida:** `GET /proyectos/{id}/informe` devuelve JSON completo y los tests unitarios/MVC pasan.

### Fase 2 — PDF (4–5 días)

| # | Tarea | Archivos |
|---|---|---|
| 2.1 | Crear resolver seguro de imágenes | `proyectos/pdf/PdfImagenResolver.java` |
| 2.2 | Implementar decodificación WebP y conversión a JPEG/PNG | `PdfImagenResolver` |
| 2.3 | Crear page event del informe | `proyectos/pdf/InformeProyectoPdfPageEvent.java` |
| 2.4 | Crear renderer con secciones y tablas | `proyectos/pdf/InformeProyectoPdfRenderer.java` |
| 2.5 | Implementar portada, estado por área, riesgo para la entrega y decisiones | Renderer |
| 2.6 | Implementar moodboard en grilla | Renderer + resolver |
| 2.7 | Implementar equipo y requerimientos | Renderer |
| 2.8 | Implementar cronograma, conflictos y alertas | Renderer |
| 2.9 | Implementar contratos, historial y calificaciones | Renderer |
| 2.10 | Implementar endpoint PDF y headers | Controller |
| 2.11 | Crear excepción y handler global | `ErrorGenerandoInformeProyectoException`, `GlobalExceptionHandler` |
| 2.12 | Implementar encabezado, pie y metadatos | `InformeProyectoPdfPageEvent`, `InformeProyectoPdfRenderer` |
| 2.13 | Tests de PDF y de imágenes | `src/test/java/.../proyectos/pdf/` |

#### Lineamientos visuales

- A4 vertical, márgenes laterales de 36–40 pt.
- Reutilizar `ModaLinkPdfEstilos` (azul, gris, borde, fondo).
- Barra superior azul en todas las páginas: “ModaLink · Informe de proyecto” a la izquierda y “{Proyecto} · Corte {fecha/hora}” a la derecha.
- Pie de página en todas las páginas: “Impreso el {fecha/hora} · Generado por {nombre} · Foto del momento, no reemplaza la auditoría”; centro “ModaLink · Informe de proyecto”; derecha “Hoja X / Y”.
- Primera página: título grande, badges de estado/modo/privacidad, descripción y ficha con director, inicio, entrega y ubicación.
- Resumen con estado general, cuatro tarjetas de área (color + motivo), bloque de riesgo para la entrega y tabla de decisiones con antigüedad.
- Mantener siempre texto junto al color (`ROJO`, `AMARILLO`, `VERDE`) para que el informe sea legible en blanco y negro.
- Moodboard: grilla de 3–4 columnas (4 en el mockup), imágenes de hasta 150–180 pt de lado, pie con id/fecha.
- Tablas con `setHeaderRows(1)` y `setSplitRows(true)` para que no se corten mal.
- Página nueva antes de secciones grandes: equipo, cronograma y contratos.
- Si no hay datos: celda o párrafo “Sin datos disponibles”.
- No incluir enlaces clicables a archivos locales.
- Limitar a `max-imagenes-pdf`; si hay más, mostrar “N imágenes adicionales no incluidas”.
- Si una imagen falla, mostrar placeholder gris con “Imagen no disponible”.

#### Estructura de páginas según mockup

| Página | Contenido |
|---|---|
| 1 | Portada, badges, descripción, ficha (director, inicio, entrega, ubicación), estado por área, riesgo para la entrega y tabla “Requiere tu decisión” |
| 2 | Moodboard, equipo (activos, directores, bajas, estabilidad), cobertura por requerimiento y tabla de miembros |
| 3 | Cronograma (KPIs, línea de tiempo, distribución por estado, próximas actividades) y contratos (KPIs, barra de firmas, tabla) |
| 4 | Alertas priorizadas, historial del equipo, calificaciones y bloque “Cómo leer este informe” |

Notas:

- No incluir índice.
- La cantidad de páginas es dinámica; el mockup de 4 páginas es solo referencia.
- “Cómo leer este informe” explica que el color de cada área surge de sus alertas y que “sin datos” nunca es verde.
- Las secciones vacías deben conservar su lugar con un mensaje explícito, no desaparecer sin contexto.
- El bloque de calificaciones se muestra como “Sin datos” hasta que el proyecto esté finalizado.

#### Resolver de imágenes

Contrato sugerido:

```java
public final class PdfImagenResolver {
    public Optional<Image> resolver(String url); // devuelve imagen lista para OpenPDF
}
```

Reglas:

1. Aceptar solo URLs que empiecen con `/uploads/`.
2. Quitar el prefijo `/uploads/` y normalizar.
3. Resolver contra `Paths.get(uploadDir).toAbsolutePath().normalize()`.
4. Verificar que el path final siga dentro del upload root.
5. Rechazar `..`, paths absolutos, `http://`, `https://`, `file:`.
6. Leer bytes; si no existe o no es imagen, devolver `Optional.empty()`.
7. Decodificar con `ImageIO` (el plugin WebP ya está en el proyecto).
8. Escalar manteniendo aspecto a un máximo de 1200 px de lado.
9. Convertir a JPEG con fondo blanco si tiene transparencia; o PNG si se prefiere conservar alpha.
10. Devolver `Image` de OpenPDF.
11. Cachear por URL dentro de una misma generación para no decodificar dos veces.
12. No loguear rutas absolutas ni contenido.

#### Manejo de errores PDF

- Si falla una imagen: continuar.
- Si falla una sección: incluir texto de error y continuar, salvo que sea la estructura base.
- Si falla el documento completo: lanzar `ErrorGenerandoInformeProyectoException`.
- Handler global: `500`, código `ERROR_GENERANDO_INFORME`.
- Generar en `ByteArrayOutputStream` y devolver `byte[]`.

**Criterio de salida:** el PDF se descarga, abre correctamente y contiene todas las secciones.

### Fase 3 — Testing y hardening (2–3 días)

| # | Tarea | Detalle |
|---|---|---|
| 3.1 | Test de integración con PostgreSQL | Crear proyecto completo, llamar servicio y renderer |
| 3.2 | Test de seguridad | Director 200; miembro 403; no miembro 403; inactivo 403; inexistente 404 |
| 3.3 | Test de imágenes | WebP válido, archivo faltante, archivo corrupto, path traversal |
| 3.4 | Test de estados vacíos | Sin moodboard, sin requerimientos, sin contratos, sin actividades |
| 3.5 | Test de estados de proyecto | Borrador, publicado, confirmado, finalizado, cancelado |
| 3.6 | Test de performance | 100 miembros, 300 actividades, 20 contratos, 20 imágenes |
| 3.7 | Revisión de consultas | Evitar N+1; revisar SQL generado |
| 3.8 | Revisión de tamaño PDF | Verificar límite y reducción de imágenes |
| 3.9 | Ejecutar `mvn test` y `mvn verify` | Suite completa |

#### Casos de prueba mínimos

| Caso | Resultado esperado |
|---|---|
| Director activo pide JSON | `200`, secciones completas |
| Director activo pide PDF | `200`, `%PDF`, tamaño > 1 KB |
| Miembro no director | `403` |
| Usuario sin perfil activo | Error de perfil activo |
| Perfil inactivo | `403` o error de perfil en baja |
| Proyecto inexistente | `404` |
| Proyecto sin moodboard | PDF con “Sin imágenes” |
| Proyecto sin contratos | Área contratos `SIN_DATOS` o `NO_APLICA`, sin verde artificial |
| Proyecto sin fecha de entrega | Margen `null`, alerta `SIN_FECHA_ENTREGA`, área cronograma `ROJO` |
| Proyecto finalizado | Modo `CIERRE`, solo alertas `INFO`/`BAJA` |
| Proyecto cancelado | Incluye motivo y fecha |
| Imagen WebP | Se incrusta correctamente |
| Imagen inexistente | Placeholder, PDF válido |
| URL con `../` | Se rechaza, PDF válido |
| `Clock` fijo | Estados, antigüedad y vencimientos deterministas |
| Área sin datos | `SIN_DATOS`, no `VERDE` |
| Estado general | Coincide con el área más grave |
| Encabezado y pie | Incluyen proyecto, corte, generado por, fecha de impresión y `Hoja X / Y` |
| Muchas imágenes | Respeta `max-imagenes-pdf` |
| Muchos miembros/actividades | Sin N+1 y dentro del tiempo objetivo |

#### Objetivos de performance

| Operación | Objetivo |
|---|---|
| JSON con 100 miembros / 300 actividades | < 2 s en entorno local |
| PDF con 12 imágenes | < 5 s en entorno local |
| Consultas por informe | < 20 queries |
| Tamaño PDF típico | < 8 MB |
| Tamaño máximo PDF | 15 MB, con reducción automática |

### Fase 4 — Documentación y demo (1 día)

| # | Tarea | Archivo |
|---|---|---|
| 4.1 | Documentar endpoints | `README.md` |
| 4.2 | Documentar uso para director | `docs/manual/06-proyectos-directores.md` |
| 4.3 | Registrar extensión | `docs/5_estado_sistema/estado_uc.md` |
| 4.4 | Preparar guion de demo | `docs/4_pendiente/demo-informe-proyecto.md` opcional |
| 4.5 | Verificar enlaces y referencias | README/docs |

**Criterio de salida:** una persona ajena al desarrollo puede generar el informe siguiendo el manual.

### Fase 5 — Opcional post-MVP

| Mejora | Valor | Complejidad |
|---|---|---|
| Snapshot por informe | Comparar contra el informe anterior | M |
| Bloqueos por persona | Quién falta firmar, quién no aceptó T&C y a quién responder | S/M |
| Cambios desde el último informe | Requiere snapshot `informe_proyecto_snapshot` | M |
| Email periódico | Enviar link al PDF cada semana | M |
| Variante admin | Administrador consulta cualquier proyecto | S |
| CSV de actividades/equipo | Análisis externo | S |
| Hitos completos | Próximos hitos reales | M |
| Fecha de finalización de actividad | Velocidad exacta | S/M |
| Frontend | Dashboard interactivo | M/L |
| Firma/versionado del informe | Evidencia formal | M |

---

## 8. Estrategia de pruebas

### 8.1. Unitarias

- `CalcularEstadoAreasService`:
  - una alerta `ALTA` → área `ROJO`;
  - solo `MEDIA` → `AMARILLO`;
  - solo `BAJA`/`INFO` con datos → `VERDE`;
  - sin alertas y sin datos → `SIN_DATOS`;
  - modo `BORRADOR`, `OPERATIVO` y `CIERRE`;
  - estado general = área más grave;
  - `SIN_DATOS` y `NO_APLICA` no cuentan;
  - riesgo de entrega con margen negativo, sin fecha y sin actividades;
  - todo código de alerta operativa tiene un área asignada;
- `DetectarAlertasProyectoService`:
  - cada código;
  - mapeo de área;
  - deduplicación;
  - antigüedad con `Clock` fijo;
  - priorización de decisiones por severidad y antigüedad.
- `GenerarInformeProyectoService`:
  - validaciones de acceso;
  - mapeo de secciones;
  - estados vacíos;
  - cálculo de métricas.

### 8.2. MVC

`@WebMvcTest` con `@MockitoBean` del servicio, siguiendo `AdminAuditoriaControllerTest`:

- JSON 200;
- PDF 200 y headers;
- 403/404;
- verificación de `@PreAuthorize`;
- Content-Disposition.

### 8.3. Integración

`@SpringBootTest` + `AbstractPostgresIntegrationTest`:

- insertar datos con `JdbcTemplate` o repositorios;
- probar servicio real y renderer;
- verificar PDF;
- verificar consultas y estados;
- usar `@Transactional` para revertir.

### 8.4. PDF

- Validar cabecera `%PDF`.
- Validar texto extraído con `PdfTextExtractor` de OpenPDF, si está disponible.
- Validar tamaño y cantidad de páginas.
- Validar que no falle con imagen faltante.
- Validar que el pie tenga “Hoja X / Y”.
- Validar que el encabezado incluya proyecto y corte.
- Validar que el pie incluya fecha de impresión y “Generado por”.
- Validar que no aparezcan DNI, correo ni texto de contratos.

### 8.5. Seguridad

- Test de path traversal.
- Test de acceso por rol.
- Test de perfil inactivo.
- Test de proyecto privado.
- Verificar que no se expongan DNI, correo ni texto de contratos.

---

## 9. Criterios de aceptación

### Funcionales

- [ ] El director activo puede generar el informe de su proyecto en cualquier estado.
- [ ] El informe muestra nombre, estado, fechas, ubicación y director.
- [ ] El informe muestra el moodboard con imágenes y cantidad.
- [ ] El informe muestra miembros activos e históricos.
- [ ] El informe muestra cobertura por requerimiento.
- [ ] El informe muestra avance, margen, viabilidad, conflictos y vencidas.
- [ ] El informe muestra contratos y firmas.
- [ ] El informe muestra alertas y decisiones.
- [ ] El informe muestra historial y calificaciones cuando corresponde.
- [ ] El PDF se descarga con nombre correcto.

### No funcionales

- [ ] Sin cambios de esquema en el MVP.
- [ ] Sin N+1 evidente.
- [ ] Sin rutas absolutas ni datos sensibles.
- [ ] Sin fallo si falta una imagen.
- [ ] Sin fallo si faltan secciones.
- [ ] Tiempo y tamaño dentro de los objetivos.
- [ ] `mvn verify` en verde.
- [ ] Documentación actualizada.

### De negocio

- [ ] El director puede responder las cinco preguntas de la propuesta en menos de dos minutos.
- [ ] Las alertas son accionables y no ruidosas.
- [ ] El color de cada área se explica con alertas visibles en el mismo informe y nunca las contradice.
- [ ] Ningún área aparece en verde si no tiene datos.
- [ ] El estado general coincide con el área más grave.

---

## 10. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
|---|---|---|
| OpenPDF no soporta WebP | Moodboard vacío o error | Convertir con ImageIO/WebP plugin; test de fixture |
| Imágenes muy grandes | PDF lento o pesado | Redimensionar, limitar cantidad, comprimir JPEG |
| Proyectos con muchos datos | N+1 y timeout | Consultas batch existentes; límites; medir |
| Métricas sin fecha de actividad | Velocidad imprecisa | Etiquetar como aproximada; fase 2 |
| Sin snapshots | No se puede comparar períodos | Aclarar en UI; fase 2 |
| Color percibido como demasiado severo | El director ignora el informe | Mostrar siempre el motivo; ajustar qué alertas son `ALTA` desde configuración |
| Alerta nueva sin área asignada | Estado general incompleto | Test que verifica el mapeo completo |
| Encabezado o pie incompletos | Pérdida de trazabilidad | Test de metadatos y reutilización del patrón de auditoría |
| Exposición de datos de miembros | Privacidad | DTO acotado; revisión de campos |
| Path traversal en imágenes | Seguridad | Resolver solo bajo upload root; tests |
| Cambios de esquema innecesarios | Riesgo de regresión | MVP sin migraciones |
| Scope creep hacia frontend/admin | Retraso | Fases separadas y backlog explícito |

---

## 11. Cronograma sugerido

| Semana | Actividad |
|---|---|
| Semana 1 | Fase 0 + Fase 1 (DTO, servicio, estado por área, alertas, JSON) |
| Semana 2 | Fase 2 (PDF, imágenes, metadatos, endpoint) |
| Semana 3 | Fase 3 + Fase 4 (testing, hardening, documentación, demo) |

Hitos:

- **Día 3:** JSON con datos reales.
- **Día 7:** estado por área, alertas y antigüedad aprobados.
- **Día 12:** PDF completo con encabezado y pie.
- **Día 15:** suite verde y documentación.
- **Día 16:** demo.

---

## 12. Rollout y operación

- **Activación:** el endpoint es aditivo; no requiere feature flag. Si se desea, agregar `app.informe-proyecto.habilitado=true` y devolver `404`/`503` cuando esté apagado.
- **Monitoreo:**
  - log `idProyecto`, `idPerfil`, duración, cantidad de imágenes y bytes del PDF;
  - no loguear nombres, correos ni contenido;
  - métrica de errores `ERROR_GENERANDO_INFORME`.
- **Backout:** quitar endpoints y clases; no hay migraciones que revertir.
- **Capacidad:** el PDF es bajo demanda; si se vuelve frecuente, agregar cache por proyecto+fecha o generación asíncrona.
- **Soporte:** documentar que el informe es una foto del momento y que no reemplaza la auditoría.

---

## 13. Backlog posterior recomendado

1. Snapshot `informe_proyecto_snapshot` para comparación.
2. `fecha_finalizacion` de actividad para velocidad exacta.
3. Hitos completos.
4. Email periódico con link al PDF.
5. Variante admin.
6. CSV de miembros/actividades.
7. Frontend con descarga y filtros.
8. Firma/versionado del informe para evidencia formal.

---

## 14. Referencias internas

- `docs/4_pendiente/propuestas-informes.md`
- `src/main/java/org/mgroko/backend/auditoria/pdf/AuditoriaPdfRenderer.java`
- `src/main/java/org/mgroko/backend/common/pdf/ModaLinkPdfEstilos.java`
- `src/main/java/org/mgroko/backend/common/pdf/ModaLinkPdfPageEvent.java`
- `src/main/java/org/mgroko/backend/actividades/servicio/ObtenerCronogramaService.java`
- `src/main/java/org/mgroko/backend/proyectos/servicio/CupoRequerimientoService.java`
- `src/main/java/org/mgroko/backend/proyectos/servicio/ProyectoSecurityService.java`
- `src/main/java/org/mgroko/backend/admin/servicio/AdminProyectoService.java`
- `src/main/java/org/mgroko/backend/storage/servicio/LocalStorageService.java`
- `src/main/java/org/mgroko/backend/storage/servicio/ProcesadorImagen.java`
- `docs/3_diseño/ModaLinkBD.sql`
- Mockups adjuntos del informe (4 páginas de referencia)

---

## 15. Nota final

Este plan no modifica el sistema. La implementación debe comenzar por la Fase 0 y no avanzar a PDF hasta que el JSON, el estado por área y las alertas estén aprobados. El objetivo es entregar un informe confiable, explicable y útil para el director, no un PDF con muchos datos sin jerarquía.
