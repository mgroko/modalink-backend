# Plan — Colaboraciones automáticas: verificación y detección asíncrona

> **Estado:** Fase 1, 2 y 3 implementadas. Fase 4 (calibración) en curso.
> **Objetivo de esta etapa:** priorizar precisión sin penalizar la latencia de subida.

## 1. Estado actual

| Fase | Alcance | Estado |
|---|---|---|
| 1 | Filtro de portfolio y `origen` del colaborador | Implementada |
| 2 | Pipeline SHA-256 → pHash → ORB → multi-recorte | Implementada |
| 3 | Persistencia de scores y `tipo_coincidencia` | Implementada |
| 4 | Calibración de umbrales con imágenes reales | En curso |
| 5 | Detección asíncrona después del commit | Pendiente |

## 2. Fase 4 — Calibración

### Objetivo

Elegir umbrales que prioricen **precisión**: evitar propuestas falsas aunque se
pierdan algunas colaboraciones dudosas.

### Herramienta

Harness de calibración en
`C:\Users\HP\AppData\Local\Temp\opencode\calibracion-colaboraciones`
(`CalibracionHarness.java`, `BenchmarkRansac.java`, `README.md`).

Mide por par:

- distancia pHash;
- keypoints y matches ORB;
- ratio test de Lowe;
- inliers geométricos reales con RANSAC;
- distancia multi-recorte.

### Resultado con datos reales (dev local)

32 imágenes de publicaciones, 423 pares comparables (distinta publicación,
perfil y usuario):

| Rango pHash | Pares | Resultado |
|---|---|---|
| ≤ 8 | 3 | 3 verdaderos positivos |
| 9–16 | 0 | sin pares |
| 17–24 | 21 | todos falsos positivos (RANSAC 0–5) |
| 25–32 | 218 | falsos positivos; el candidato 24 los excluye |

Hallazgo principal: la regla ORB actual (`matches >= 50` con distancia ≤ 60)
acepta alrededor de 10 de los 21 falsos positivos del rango 17–24. Los inliers
RANSAC separan perfectamente en esta muestra: positivos 314–500, falsos 0–5.

### Recomendación

1. Mantener `phash-distancia-candidato=24`.
2. Mantener `phash-distancia-alta-confianza=8`.
3. Calcular matches con `knnMatch` + ratio 0.75 y ejecutar RANSAC sobre esos
   matches.
4. Exigir `orb-inliers >= 10` (validar rango 8–12 con más datos).
5. Persistir los inliers geométricos en `orb_inliers` (hoy es un proxy por
   distancia).
6. Mantener `multicrop-distancia-maxima=8`.

### Rendimiento medido

| Etapa | Promedio |
|---|---|
| Decodificar 2 imágenes | 23–58 ms |
| ORB detectAndCompute (2) | 44–83 ms |
| `knnMatch` + ratio | 2–3 ms |
| RANSAC sobre matches filtrados por ratio | 1–2 ms |
| RANSAC sobre matches crudos | 1–85 ms |
| Multi-recorte pHash | 5–7 ms |

Conclusión: RANSAC sobre matches filtrados por ratio agrega ~4–5 ms por
candidato. Con máximo 10 candidatos por imagen, el peor caso es ~40–50 ms y el
caso típico (0–2 candidatos) es < 10 ms.

### Pendiente de Fase 4

- Reunir más pares reales, sobre todo positivos con pHash entre 9 y 24
  (recortes, filtros, re-subidas) para confirmar `orb-inliers >= 10`.
- Repetir el análisis con una muestra más grande antes de fijar el valor.
- Actualizar `application.properties` y la ficha técnica cuando se apruebe.

## 3. Fase 5 — Detección asíncrona después del commit

### Problema

Hoy `DetectarColaboracionAutomaticaService.detectar` se ejecuta de forma
síncrona dentro del request de subida, desde:

- `CrearPublicacionService`;
- `ModificarPublicacionService`.

El request paga la lectura de archivos, la decodificación, ORB, RANSAC y el
fallback multi-recorte. Aunque RANSAC sea barato, la suma puede ser perceptible
para el usuario.

### Objetivo

Responder la subida sin esperar la detección. La propuesta se genera después de
que la transacción de subida confirma, en un hilo aparte.

### Diseño propuesto

1. Publicar un evento de dominio dentro de la transacción de subida:
   - `ColaboracionAutomaticaSolicitada` con `idPublicacion` e `idImagenes`.
   - No pasar entidades JPA al evento.
2. Escuchar con `@TransactionalEventListener(phase = AFTER_COMMIT)`.
3. Ejecutar la detección en un bean separado con `@Async` (evitar
   autollamada, que no pasa por el proxy de Spring).
4. En el hilo asíncrono, abrir una transacción nueva y recargar la publicación
   y las imágenes por ID. Así se evita `LazyInitializationException` y se
   garantiza que los datos ya están visibles.
5. Usar un `ThreadPoolTaskExecutor` acotado y configurable.
6. Mantener la idempotencia existente:
   - índice único de propuestas pendientes;
   - `existsByParYEstados`;
   - captura de `DataIntegrityViolationException`.
7. Si la detección falla, registrar el error y no afectar la subida. Evaluar
   reintentos o una tabla outbox si se necesita garantía de entrega.

### Configuración propuesta

```properties
app.colaboracion-automatica.async.habilitada=true
app.colaboracion-automatica.async.core-pool-size=2
app.colaboracion-automatica.async.max-pool-size=4
app.colaboracion-automatica.async.queue-capacity=100
```

### Alternativas

- **AFTER_COMMIT + @Async (recomendada para el MVP):** simple, sin tabla nueva.
- **Outbox + scheduler:** más robusta ante reinicios y picos; agrega una tabla y
  un proceso de reintento.
- **Cola externa:** innecesaria para el volumen actual.

### Criterios de aceptación

- La respuesta de subida no incluye el tiempo de ORB/RANSAC.
- Se siguen creando propuestas para coincidencias exactas y perceptuales.
- No se duplican propuestas bajo concurrencia.
- Un fallo de detección no hace fallar la subida; queda logueado.
- El evento no se dispara si la transacción de subida hace rollback.
- Tests:
  - evento publicado solo al commit;
  - rollback no dispara detección;
  - ejecución asíncrona con IDs;
  - idempotencia/concurrencia.

### Preguntas abiertas

- ¿Alcanza con AFTER_COMMIT + @Async o se quiere outbox con reintentos?
- ¿La propuesta debe aparecer “al instante” o se tolera unos segundos?
- ¿Hace falta mostrar un estado “analizando imágenes” en el frontend?

## 4. Orden de implementación sugerido

1. Cerrar Fase 4 con la muestra ampliada y aprobar `orb-inliers >= 10`.
2. Implementar RANSAC sobre matches filtrados por ratio.
3. Implementar la detección asíncrona post-commit.
4. Actualizar la ficha técnica y `application.properties`.
5. Re-ejecutar tests unitarios, de integración y la calibración.

## 5. Fase 6 — Backfill histórico

El procesamiento de publicaciones existentes se diseña en una ficha aparte:

`docs/3_diseño/ficha-tecnica-colaboraciones-automaticas-historicas.md`

Incluye backfill de huellas, detección histórica, job asíncrono, progreso,
errores, idempotencia y auditoría.

## 6. Fuera de alcance

- Implementación de la interfaz de administración del backfill.
- Badge de `origen` en el frontend.
- Cola externa o workers separados.
