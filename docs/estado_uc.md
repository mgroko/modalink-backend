# Estado de casos de uso — ModaLink

Seguimiento del avance de implementación por caso de uso (UC), en base al documento
de requerimientos (Roko María Guillermina, Iteración 01). Agrupados según los
diagramas de casos de uso del propio documento (Figuras 2 a 7).

**Estados posibles:**

- `No iniciado` — sin código asociado.
- `En progreso` — implementación parcial o en curso.
- `Implementado` — funcionalidad completa, sin cobertura de tests.
- `Implementado y testeado` — funcionalidad completa con tests unitarios y/o de integración.

Actualizar esta tabla en el mismo commit/PR que cierra o avanza un UC. Referenciar el
número de UC en el mensaje de commit (ej. `feat(UC-03): registro de usuario`) para que
el historial de git sirva de bitácora adicional.

---

## Gestión de usuarios (14 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------------------|------------------------|-------|-------|
| UC-01 | Iniciar sesión | Implementado y testeado | `POST /auth/login` | Sí | |
| UC-02 | Cerrar sesión | Implementado y testeado | `POST /auth/logout` | Sí | |
| UC-03 | Registrarse | Implementado y testeado | `POST /auth/registro` | Sí | |
| UC-04 | Deshabilitar usuario | Implementado | `/admin/usuarios/{id}/deshabilitar` | No | |
| UC-05 | Habilitar usuario | Implementado y NO testeado | `/admin/usuarios/{id}/habilitar` | No | |
| UC-06 | Buscar usuario | Implementado y NO testeado | `GET /admin/usuarios` | No | |
| UC-07 | Solicitar baja en el sistema | En progreso | | | |
| UC-08 | Modificar datos personales | En progreso | `PUT /usuario/datos-personales` | | |
| UC-09 | Autenticar mediante Google OAuth | No iniciado | | | En roadmap cercano |
| UC-56 | Gestionar habilidades del sistema | No iniciado | | | |
| UC-57 | Gestionar características técnicas por profesión | No iniciado | | | |
| UC-58 | Buscar características técnicas | No iniciado | | | |
| UC-59 | Buscar profesiones | No iniciado | | | |
| UC-67 | Generar informe de auditoría | No iniciado | | | |

## Gestión de perfiles (12 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------|-------------------|-------|-------|
| UC-10 | Crear perfil | No iniciado | | | |
| UC-11 | Editar perfil | No iniciado | | | |
| UC-12 | Eliminar perfil | No iniciado | | | |
| UC-13 | Cambiar perfil activo | No iniciado | | | |
| UC-14 | Ver perfil | Iniciado | | | Iniciado para el panel de admin, fase muy temprana (posibilidad de ver los perfiles asociados al Usuario desde el panel) |
| UC-15 | Reportar perfil | No iniciado | | | |
| UC-16 | Buscar perfil | Iniciado | | | Busqueda desde el repo, findby... |
| UC-17 | Asignar en calendario día disponible | No iniciado | | | |
| UC-18 | Asignar en calendario día no disponible | No iniciado | | | |
| UC-23 | Configurar términos y condiciones | No iniciado | | | |
| UC-47 | Gestionar habilidades | No iniciado | | | |
| UC-48 | Buscar habilidades | No iniciado | | | |

## Gestión de publicaciones (11 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------|-------------------|-------|-------|
| UC-19 | Alta publicación | No iniciado | | | |
| UC-20 | Baja publicación | No iniciado | | | |
| UC-21 | Modificar publicación | No iniciado | | | |
| UC-22 | Buscar publicación | No iniciado | | | |
| UC-61 | Enviar solicitud de colaboración | No iniciado | | | |
| UC-62 | Aceptar solicitud de colaboración | No iniciado | | | |
| UC-63 | Rechazar solicitud de colaboración | No iniciado | | | |
| UC-64 | Eliminar solicitud de colaboración | No iniciado | | | |
| UC-65 | Buscar solicitudes de colaboración | No iniciado | | | |
| UC-66 | Interactuar con publicación | No iniciado | | | |
| UC-69 | Reportar publicación | No iniciado | | | |

## Gestión de proyectos (22 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------|-------------------|-------|-------|
| UC-24 | Crear proyecto | No iniciado | | | |
| UC-25 | Publicar proyecto | No iniciado | | | |
| UC-26 | Dar de alta postulación a proyecto | No iniciado | | | |
| UC-27 | Dar de baja postulación a proyecto | No iniciado | | | |
| UC-28 | Aceptar solicitud de incorporación | No iniciado | | | |
| UC-29 | Buscar proyecto | No iniciado | | | |
| UC-30 | Buscar invitaciones | No iniciado | | | |
| UC-31 | Invitar a proyecto | No iniciado | | | |
| UC-32 | Eliminar invitación a proyecto | No iniciado | | | |
| UC-33 | Visualizar cronograma de proyecto | No iniciado | | | |
| UC-34 | Gestionar postulaciones | No iniciado | | | |
| UC-35 | Cancelar proyecto | No iniciado | | | |
| UC-36 | Modificar proyecto | No iniciado | | | |
| UC-37 | Finalizar proyecto | No iniciado | | | |
| UC-38 | Eliminar integrante | No iniciado | | | |
| UC-40 | Darse de baja de proyecto | No iniciado | | | |
| UC-41 | Enviar mensaje a chat grupal | No iniciado | | | |
| UC-49 | Crear actividad en planificación | No iniciado | | | |
| UC-50 | Modificar actividad de planificación | No iniciado | | | |
| UC-51 | Eliminar actividad de planificación | No iniciado | | | |
| UC-52 | Buscar actividades | No iniciado | | | |
| UC-60 | Confirmar proyecto | No iniciado | | | |

## Gestión de contratos (5 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------|-------------------|-------|-------|
| UC-39 | Generar contrato | No iniciado | | | |
| UC-53 | Modificar contrato | No iniciado | | | |
| UC-54 | Eliminar contrato | No iniciado | | | |
| UC-55 | Firmar contrato | No iniciado | | | |
| UC-68 | Buscar contrato | No iniciado | | | |

## Gestión de conexiones y mensajería (5 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------|-------------------|-------|-------|
| UC-42 | Enviar mensaje privado | No iniciado | | | |
| UC-43 | Enviar solicitud de conexión | No iniciado | | | |
| UC-44 | Eliminar conexión | No iniciado | | | |
| UC-45 | Gestionar solicitudes de conexión | No iniciado | | | |
| UC-46 | Buscar solicitudes de conexión | No iniciado | | | |

---

## Resumen

| Módulo | Total UC | Implementado y testeado | Implementado | En progreso | No iniciado |
|--------------------------------------|----------|--------------------------|---------------|-------------|-------------|
| Gestión de usuarios | 14 | 2 | 0 | 0 | 12 |
| Gestión de perfiles | 12 | 0 | 0 | 0 | 12 |
| Gestión de publicaciones | 11 | 0 | 0 | 0 | 11 |
| Gestión de proyectos | 22 | 0 | 0 | 0 | 22 |
| Gestión de contratos | 5 | 0 | 0 | 0 | 5 |
| Gestión de conexiones y mensajería | 5 | 0 | 0 | 0 | 5 |
| **Total** | **69** | **3** | **0** | **0** | **67** |
