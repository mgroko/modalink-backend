# Ficha Técnica — Características Técnicas "en uso" (sólo editable el nombre)

> **Actor:** Administrador · **Módulo:** Catálogo de características técnicas
> **Backend:** `AdminCaracteristicaTecnicaService` + `AdminCaracteristicaTecnicaController`
> **Base path:** `/admin/caracteristicas-tecnicas`
> **Auth:** `Authorization: Bearer <token>` (JWT) + authorities `VER_/CREAR_/MODIFICAR_/ELIMINAR_CARACTERISTICA`
> **Stack frontend:** Vue 3 (SPA) · Composition API + `<script setup>`

---

## 1. Regla de negocio

Una característica técnica está **"en uso"** cuando existe al menos una fila que la referencia en:

- `caracteristica_perfil` → perfiles que la tienen asignada, o
- `requerimiento_act_caract` / `requerimiento_gral_caract` → requerimientos de proyectos que la referencian.

Entonces:

| Situación | `PUT /admin/caracteristicas-tecnicas/{id}` | `DELETE /admin/caracteristicas-tecnicas/{id}` |
|---|---|---|
| **En uso** (`enUso: true`) | Sólo puede cambiar `nombre`. Si cambia `codigo`, `tipoDato`, `idProfesion` o `idUnidad` → **409** | **409** (bloqueado) |
| **Sin uso** (`enUso: false`) | Todos los campos editables | 204 (borrado físico) |

- El backend valida la regla **y** la BD tiene un `BEFORE UPDATE` trigger como red de seguridad: aunque existiera una race, Postgres rechaza la escritura.
- La regla aplica al **completo** del `PUT`: el backend compara la solicitud contra lo que está guardado, campo por campo (no contra "qué campos vinieron presentes en el JSON").

---

## 2. Qué cambió en el contrato (diff para el frontend)

| # | Cambio | Impacto |
|---|---|---|
| 1 | `CaracteristicaTecnicaResponse` tiene el campo nuevo **`enUso: boolean \| null`** | Tipar la interfaz; `null` = el contexto no lo calcula |
| 2 | `GET /admin/caracteristicas-tecnicas` calcula `enUso` por ítem | Habilita badge + bloqueo de campos en la UI de admin |
| 3 | `POST /admin/caracteristicas-tecnicas` responde `enUso: false` | Recién creada → nada bloqueado |
| 4 | `PUT` puede responder **409** por esta regla (además de 409 por código duplicado) | Manejar 409 en el submit del formulario de edición |
| 5 | `DELETE` ahora responde 409 también si sólo está en uso por **requerimientos** (antes sólo perfiles) | El modal de confirmación de baja debe tolerar 409 |
| 6 | `GET /profesiones/{id}/caracteristicas-tecnicas` (lado perfiles) devuelve `enUso: null` | No usar ese endpoint para decidir el bloqueo en admin |

---

## 3. Contrato de datos

### 3.1 `CaracteristicaTecnicaResponse` (respuesta de admin y de perfiles)

```json
{
  "idCaracteristica": 12,
  "codigo": "TIPO_VOZ",
  "nombre": "Tipo de voz",
  "unidad": { "idUnidad": 1, "nombre": "Kilogramo", "simbolo": "kg", "tipoDatoPermitido": "NUMERICO" },
  "idProfesion": 5,
  "profesion": "Músico",
  "tipoDato": "ENUMERADO",
  "valores": [
    { "idValor": 3, "codigo": "SOPRANO", "colorHex": "#FF0000" }
  ],
  "enUso": true
}
```

- `unidad`: objeto o `null` (si la característica no tiene unidad).
- `idProfesion` / `profesion`: pueden ser `null`.
- **`enUso`**: `true` | `false` en endpoints de **admin**; `null` en `GET /profesiones/{id}/caracteristicas-tecnicas`.
- `valores`: siempre presente (lista, posible vacía).

### 3.2 Tipos TypeScript sugeridos

```ts
export interface UnidadMedida {
  idUnidad: number
  nombre: string
  simbolo: string
  tipoDatoPermitido: string
}

export interface ValorCaracteristica {
  idValor: number
  codigo: string
  colorHex: string | null
}

export interface CaracteristicaTecnica {
  idCaracteristica: number
  codigo: string
  nombre: string | null
  unidad: UnidadMedida | null
  idProfesion: number | null
  profesion: string | null
  tipoDato: 'ENUMERADO' | 'TEXTO' | 'NUMERICO'
  valores: ValorCaracteristica[]
  /** admin: boolean · perfiles: null */
  enUso: boolean | null
}

export interface AdminCaracteristicaTecnicaRequest {
  codigo: string        // obligatorio, máx 50, se normaliza trim + MAYÚSCULAS
  nombre: string | null // máx 100
  idUnidad: number | null
  idProfesion: number   // obligatorio
  tipoDato: 'ENUMERADO' | 'TEXTO' | 'NUMERICO'  // obligatorio
  valores?: ValorCaracteristica[]  // sólo se usa en POST; el PUT lo ignora
}
```

### 3.3 `AdminCaracteristicaTecnicaRequest` (PUT completo)

```json
{
  "codigo": "TIPO_VOZ",
  "nombre": "Tipo de voz",
  "idUnidad": 1,
  "idProfesion": 5,
  "tipoDato": "ENUMERADO",
  "valores": []
}
```

> ⚠️ El `PUT` es **completo y se compara contra lo guardado**: hay que reenviar **siempre todos los campos con sus valores actuales**.
> - Si la característica NO tiene unidad → enviar `"idUnidad": null` **explícitamente** (omitirlo también es `null`, pero sé consistente).
> - Si tiene unidad y enviás `null`, el backend lo interpreta como un cambio de unidad → 409 si está en uso.
> - `valores` en el `PUT` **no se modifica** (se ignora): el ABM de valores tiene sus propios endpoints (ver §5).

---

## 4. Matriz de campos editables (para habilitar/deshabilitar inputs)

Campos comparados por el backend en el `PUT` cuando `enUso === true`:

| Campo del form | Relación con el bloqueo | UI recomendada con `enUso: true` |
|---|---|---|
| `nombre` | **Libre** (el único editable) | Input habilitado |
| `codigo` | Inmutable | Input **deshabilitado** + readonly visual |
| `tipoDato` | Inmutable | Select **deshabilitado** |
| `idProfesion` | Inmutable | Select **deshabilitado** |
| `idUnidad` | Inmutable | Select **deshabilitado** |
| `valores` (ABM) | Ver §5 | Depende de cada endpoint de valores |

Recomendación UX: **deshabilitar** (no ocultar) los campos bloqueados y mostrar un badge/aviso
"En uso — sólo editable el nombre", de modo que el admin vea por qué no puede editarlos.

---

## 5. Endpoints

| # | Método | Ruta | Permiso | Éxito | Cambio |
|---|---|---|---|---|---|
| 1 | `GET` | `/admin/caracteristicas-tecnicas` | `VER_CARACTERISTICAS` | 200 | Cada ítem trae `enUso` calculado |
| 2 | `POST` | `/admin/caracteristicas-tecnicas` | `CREAR_CARACTERISTICA` | 201 | Devuelve `enUso: false` |
| 3 | `PUT` | `/admin/caracteristicas-tecnicas/{id}` | `MODIFICAR_CARACTERISTICA` | 200 | **409** si en uso y cambió algo distinto de `nombre` |
| 4 | `DELETE` | `/admin/caracteristicas-tecnicas/{id}` | `ELIMINAR_CARACTERISTICA` | 204 | **409** si en uso (perfiles o requerimientos) |
| 5 | `POST` | `/admin/caracteristicas-tecnicas/{id}/valores` | `CREAR_CARACTERISTICA` | 201 | sin cambios |
| 6 | `PUT` | `/admin/caracteristicas-tecnicas/{id}/valores/{idValor}` | `MODIFICAR_CARACTERISTICA` | 200 | sin cambios |
| 7 | `DELETE` | `/admin/caracteristicas-tecnicas/{id}/valores/{idValor}` | `ELIMINAR_CARACTERISTICA` | 204 | 409 si el valor está en uso por perfiles |
| 8 | `GET` | `/profesiones/{id}/caracteristicas-tecnicas` | público/auth | 200 | `enUso: null` (lado perfiles) |

### Orden de validaciones del `PUT` (útil para mapear errores)

1. `tipoDato` inválido → **400**
2. **Regla en uso** (si `enUso`) → **409** ← *nuevo*
3. `codigo` duplicado en otra característica → **409**
4. Cambio de `ENUMERADO` a otro tipo con valores cargados → **409**
5. `tipoDato` ≠ `ENUMERADO` con `valores` en la solicitud → **400**
6. `idUnidad` inexistente o incompatible con `tipoDato` → **404** / **400**
7. `idProfesion` inexistente → **404**

---

## 6. Errores

**Cuerpo estándar de error (todas las rutas):**

```json
{
  "message": "La característica técnica está en uso por perfiles o requerimientos; sólo puede modificarse el nombre.",
  "httpStatus": 409,
  "timestamp": 1770000000000
}
```

**Mensajes exactos de esta regla:**

| Operación | HTTP | `message` |
|---|---|---|
| PUT con cambio de `codigo`/`tipoDato`/`idProfesion`/`idUnidad` estando en uso | 409 | `La característica técnica está en uso por perfiles o requerimientos; sólo puede modificarse el nombre.` |
| DELETE estando en uso | 409 | `La característica técnica está en uso por perfiles o requerimientos y no puede eliminarse.` |

> Otros 409 preexistentes a distinguir en el mensaje: código duplicado
> (`Ya existe una característica técnica con el código X.`) y cambio de tipo con valores
> (`La característica posee valores de catálogo; elimínelos antes de cambiar su tipo.`).

---

## 7. Adaptación en Vue 3

### 7.1 Composable para el formulario de edición

```ts
// composables/useCaracteristicaEnUso.ts
import { computed, type Ref } from 'vue'
import type { CaracteristicaTecnica } from '@/types/caracteristicas'

export function useCaracteristicaEnUso(caracteristica: Ref<CaracteristicaTecnica | null>) {
  const enUso = computed(() => caracteristica.value?.enUso === true)

  /** true si el campo debe quedar bloqueado en el form */
  const bloquearCampo = (campo: 'codigo' | 'tipoDato' | 'idProfesion' | 'idUnidad') =>
    enUso.value && campo !== 'nombre'

  /** Mensaje de aviso para el form */
  const avisoEnUso = computed(() =>
    enUso.value
      ? 'Esta característica está en uso por perfiles o requerimientos: sólo podés modificar el nombre.'
      : null,
  )

  return { enUso, bloquearCampo, avisoEnUso }
}
```

### 7.2 Uso en el componente

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'
import { useToast } from 'vue-toastification'
import type { AdminCaracteristicaTecnicaRequest } from '@/types/caracteristicas'

const props = defineProps<{ caracteristicaId: number }>()
const router = useRouter()
const toast = useToast()

const form = ref<AdminCaracteristicaTecnicaRequest | null>(null)
const enUso = ref(false)
const guardando = ref(false)

async function cargar() {
  const { data } = await api.get('/admin/caracteristicas-tecnicas')
  const c = data.find((x: any) => x.idCaracteristica === props.caracteristicaId)
  enUso.value = c.enUso === true
  form.value = {
    codigo: c.codigo,
    nombre: c.nombre ?? '',
    idUnidad: c.unidad?.idUnidad ?? null,   // importante: null explícito si no tiene
    idProfesion: c.idProfesion,
    tipoDato: c.tipoDato,
    valores: [],                            // el PUT lo ignora
  }
}

const soloNombreEditable = computed(() => enUso.value)

async function guardar() {
  guardando.value = true
  try {
    await api.put(`/admin/caracteristicas-tecnicas/${props.caracteristicaId}`, form.value)
    toast.success('Característica actualizada')
    router.push('/admin/caracteristicas')
  } catch (e: any) {
    const status = e.response?.status
    const msg = e.response?.data?.message
    if (status === 409) {
      toast.error(msg ?? 'La característica está en uso: sólo puede modificarse el nombre.')
      await cargar()        // resincroniza el form con el estado real
    } else if (status === 400 || status === 404) {
      toast.error(msg ?? 'Revisá los datos cargados')
    } else {
      toast.error('Error inesperado al guardar')
    }
  } finally {
    guardando.value = false
  }
}
</script>
```

```vue
<template>
  <form v-if="form" @submit.prevent="guardar">
    <p v-if="soloNombreEditable" class="aviso-en-uso">
      En uso por perfiles o requerimientos — sólo editable: nombre.
    </p>

    <label>Nombre <input v-model="form.nombre" :disabled="false" /></label>

    <label>Código  <input v-model="form.codigo" :disabled="soloNombreEditable" /></label>

    <label>Tipo de dato
      <select v-model="form.tipoDato" :disabled="soloNombreEditable">…</select>
    </label>

    <label>Profesión
      <select v-model="form.idProfesion" :disabled="soloNombreEditable">…</select>
    </label>

    <label>Unidad
      <select v-model="form.idUnidad" :disabled="soloNombreEditable">…</select>
    </label>

    <button type="submit" :disabled="guardando">Guardar</button>
  </form>
</template>
```

### 7.3 En el listado de admin

```vue
<tr v-for="c in caracteristicas" :key="c.idCaracteristica">
  <td>{{ c.codigo }}</td>
  <td>{{ c.nombre }}</td>
  <td>{{ c.profesion }}</td>
  <td>
    <span v-if="c.enUso" class="badge badge-warning">En uso</span>
  </td>
  <td>
    <RouterLink :to="`/admin/caracteristicas/${c.idCaracteristica}/editar`">Editar</RouterLink>
    <button @click="eliminar(c)" :disabled="c.enUso">Eliminar</button>
  </td>
</tr>
```

- **Habilitar el botón "Eliminar"** sólo cuando `!c.enUso` (mejor UX que dejar fallar con 409).
- Alternativa: dejarlo habilitado y mostrar el `message` del 409 en un toast (el backend siempre responde 409, nunca 500).

### 7.4 En el modal de confirmación de baja (DELETE)

```
Si responde 409 → mostrar message del body y refrescar el listado.
No diferenciar por texto: usar siempre e.response.data.message.
```

---

## 8. Notas de integración y edge cases

1. **`enUso` es snapshot**: se calcula en el GET. Puede cambiar entre que cargás el formulario y que hacés submit (otro admin asignó el perfil / se creó un requerimiento). **Siempre** manejar el 409 en el PUT como fuente de verdad.
2. **`GET /profesiones/{id}/caracteristicas-tecnicas` → `enUso: null`**: es el catálogo para armar perfiles; no lo uses para bloquear la UI de admin.
3. **No mezclar los dos 409**: si `enUso` es `false` y llega 409, es por código duplicado (o valores de catálogo); el `message` lo distingue.
4. **Normalización de `codigo`**: el backend hace `trim()` + MAYÚSCULAS antes de comparar/guardar; si cambiás sólo el case, **no** cuenta como cambio (no dispara 409).
5. **ABM de valores es independiente**: agregar/editar/eliminar valores usa `/valores` (§5, filas 5-7). Esos endpoints no están sujetos a la regla "sólo nombre" (sí existe el 409 de valor en uso al eliminar).
6. **Race condition (muy improbable)**: si dos requests llegan a la vez, el trigger de BD rechaza la escritura y el backend responde **500** (sin body de negocio). Podés tratar cualquier 500 del PUT con un retry + re-fetch.
7. **Refrescar `enUso`** después de operaciones que lo alteran: crear/editar/eliminar un valor, asignar/quitar la característica de un perfil, o modificar requerimientos de proyectos.

---

## 9. Checklist de QA (aceptación)

- [ ] `GET /admin/caracteristicas-tecnicas` → cada ítem tiene `enUso` booleano.
- [ ] Con `enUso: true`: en el form sólo el nombre es editable; el resto aparece deshabilitado.
- [ ] Con `enUso: true`, PUT cambiando **sólo** `nombre` → 200 y respuesta con `enUso: true`.
- [ ] Con `enUso: true`, PUT cambiando `codigo` o `tipoDato` o `idProfesion` o `idUnidad` → **409** con el mensaje exacto de §6.
- [ ] Con `enUso: false`, PUT cambiando cualquier campo → 200.
- [ ] PUT con `enUso: true` enviando los **mismos** valores actuales → 200 (no debe haber falsos positivos).
- [ ] DELETE con `enUso: true` → 409 (y botón deshabilitado en la lista).
- [ ] Crear una característica nueva → `enUso: false` en la respuesta 201.
- [ ] `GET /profesiones/{id}/caracteristicas-tecnicas` → `enUso: null` (y la vista de perfiles no se rompe).
