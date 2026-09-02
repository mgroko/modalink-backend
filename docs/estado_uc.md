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
| UC-01 | Iniciar sesión | Implementado y testeado | `POST /auth/login` | Sí | Primera iteracion |
| UC-02 | Cerrar sesión | Implementado y testeado | `POST /auth/logout` | Sí | Primera iteracion |
| UC-03 | Registrarse | Implementado y testeado | `POST /auth/registro` | Sí | Primera iteracion |
| UC-04 | Deshabilitar usuario | Implementado y testeado | `/admin/usuarios/{id}/deshabilitar` | Sí | Primera iteracion. Motivo obligatorio + duración opcional (días) con reactivación automática al vencer |
| UC-05 | Habilitar usuario | Implementado y testeado | `/admin/usuarios/{id}/habilitar` | Sí | Primera iteracion |
| UC-06 | Buscar usuario | Implementado y testeado | `GET /admin/usuarios` | Sí | Primera iteracion |
| UC-07 | Solicitar baja en el sistema | Implementado y testeado | `POST /usuario/solicitar-baja` y `POST /usuario/reactivar-cuenta` | Sí | Primera iteracion |
| UC-08 | Modificar datos personales | Implementado y testeado | `PUT /usuario/datos-personales` | Sí | Primera iteracion |
| UC-09 | Autenticar mediante Google OAuth | No iniciado | | | Primera iteracion |
| UC-56 | Gestionar habilidades del sistema | No iniciado | | | Segunda iteracion |
| UC-57 | Gestionar características técnicas por profesión | Implementado | `POST /admin/caracteristicas-tecnicas/{id}/valores`, ` PUT /admin/caracteristicas-tecnicas/{id}` y `DELETE /admin/caracteristicas-tecnicas/{id}` | No | Segunda iteracion |
| UC-58 | Buscar características técnicas | Implementado y testeado | `GET /profesiones/{id}/caracteristicas-tecnicas` | Sí | Primera iteracion |
| UC-59 | Buscar profesiones | Implementado y testeado | `GET /profesiones` | Sí | Primera iteracion |
| UC-67 | Generar informe de auditoría | No iniciado | | | Segunda iteracion |
| UC-70 | Recuperar contraseña | No iniciado | | | Segunda iteracion |

## Gestión de perfiles (12 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------|-------------------|-------|-------|
| UC-10 | Crear perfil | Implementado y testeado | `POST /perfiles` | Sí | Primera iteracion |
| UC-11 | Editar perfil | En progreso | `GET /perfiles/{idPerfil}` y `PUT /perfiles/{idPerfil}` | Sí (servicio/DTO) | Primera iteracion |
| UC-12 | Eliminar perfil | En progreso | `DELETE /perfiles/{idPerfil}` y `POST /perfiles/{idPerfil}/reactivar` | Sí (servicio/DTO) | Primera iteracion |
| UC-13 | Cambiar perfil activo | No iniciado | | | Primera iteracion |
| UC-14 | Ver perfil | Iniciado | `GET /admin/usuarios/{id}/perfiles` y `GET /usuarios/me/perfiles` | | Primera iteracion |
| UC-15 | Reportar perfil | No iniciado | | | Segunda iteracion |
| UC-16 | Buscar perfil | Iniciado | | | Primera iteracion |
| UC-17 | Asignar en calendario día disponible | Implementado y testeado | `DELETE /calendario/bloqueos/{idBloqueo}` | Sí | Módulo calendario: `GET /calendario` (agenda completa) y `PUT /calendario/jornada` (jornada laboral + margen) |
| UC-18 | Asignar en calendario día no disponible | Implementado y testeado | `POST /calendario/bloqueos` | Sí | Módulo calendario; bloqueos manuales sin solapamiento (trigger V14) |
| UC-23 | Configurar términos y condiciones | No iniciado | | | Segunda iteracion |
| UC-47 | Gestionar habilidades | No iniciado | | | Segunda iteracion |
| UC-48 | Buscar habilidades | No iniciado | | | Segunda iteracion |

## Gestión de publicaciones (11 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------|-------------------|-------|-------|
| UC-19 | Alta publicación | No iniciado | | | Segunda iteracion |
| UC-20 | Baja publicación | No iniciado | | | Segunda iteracion |
| UC-21 | Modificar publicación | No iniciado | | | Segunda iteracion |
| UC-22 | Buscar publicación | No iniciado | | | Segunda iteracion |
| UC-61 | Enviar solicitud de colaboración | No iniciado | | | Segunda iteracion |
| UC-62 | Aceptar solicitud de colaboración | No iniciado | | | Segunda iteracion |
| UC-63 | Rechazar solicitud de colaboración | No iniciado | | | Segunda iteracion |
| UC-64 | Eliminar solicitud de colaboración | No iniciado | | | Segunda iteracion |
| UC-65 | Buscar solicitudes de colaboración | No iniciado | | | Segunda iteracion |
| UC-66 | Interactuar con publicación | No iniciado | | | Segunda iteracion |
| UC-69 | Reportar publicación | No iniciado | | | Segunda iteracion |

## Gestión de proyectos (22 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------|-------------------|-------|-------|
| UC-24 | Crear proyecto | No iniciado | | | Primera iteracion |
| UC-25 | Publicar proyecto | No iniciado | | | Primera iteracion |
| UC-26 | Dar de alta postulación a proyecto | No iniciado | | | Primera iteracion |
| UC-27 | Dar de baja postulación a proyecto | No iniciado | | | Primera iteracion |
| UC-28 | Aceptar solicitud de incorporación | No iniciado | | | Primera iteracion |
| UC-29 | Buscar proyecto | No iniciado | | | Primera iteracion |
| UC-30 | Buscar invitaciones | No iniciado | | | Primera iteracion |
| UC-31 | Invitar a proyecto | No iniciado | | | Primera iteracion |
| UC-32 | Eliminar invitación a proyecto | No iniciado | | | Primera iteracion |
| UC-33 | Visualizar cronograma de proyecto | No iniciado | | | Primera iteracion |
| UC-34 | Gestionar postulaciones | No iniciado | | | Primera iteracion |
| UC-35 | Cancelar proyecto | No iniciado | | | Primera iteracion |
| UC-36 | Modificar proyecto | No iniciado | | | Primera iteracion |
| UC-37 | Finalizar proyecto | No iniciado | | | Primera iteracion |
| UC-38 | Eliminar integrante | No iniciado | | | Primera iteracion |
| UC-40 | Darse de baja de proyecto | No iniciado | | | Primera iteracion |
| UC-41 | Enviar mensaje a chat grupal | No iniciado | | | Segunda iteracion |
| UC-49 | Crear actividad en planificación | No iniciado | | | Primera iteracion |
| UC-50 | Modificar actividad de planificación | No iniciado | | | Primera iteracion |
| UC-51 | Eliminar actividad de planificación | No iniciado | | | Primera iteracion |
| UC-52 | Buscar actividades | No iniciado | | | Primera iteracion |
| UC-60 | Confirmar proyecto | No iniciado | | | Primera iteracion |

## Gestión de contratos (5 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------|-------------------|-------|-------|
| UC-39 | Generar contrato | No iniciado | | | Segunda iteracion |
| UC-53 | Modificar contrato | No iniciado | | | Segunda iteracion |
| UC-54 | Eliminar contrato | No iniciado | | | Segunda iteracion |
| UC-55 | Firmar contrato | No iniciado | | | Segunda iteracion |
| UC-68 | Buscar contrato | No iniciado | | | Segunda iteracion |

## Gestión de conexiones y mensajería (5 UC)

| UC | Nombre | Estado | Endpoint / clase | Tests | Notas |
|------|--------------------------------------------|--------------|-------------------|-------|-------|
| UC-42 | Enviar mensaje privado | No iniciado | | | Segunda iteracion |
| UC-43 | Enviar solicitud de conexión | No iniciado | | | Segunda iteracion |
| UC-44 | Eliminar conexión | No iniciado | | | Segunda iteracion |
| UC-45 | Gestionar solicitudes de conexión | No iniciado | | | Segunda iteracion |
| UC-46 | Buscar solicitudes de conexión | No iniciado | | | Segunda iteracion |

---

## Resumen

| Módulo | Total UC | Implementado y testeado | Implementado | En progreso | No iniciado |
|--------------------------------------|----------|--------------------------|---------------|-------------|-------------|
| Gestión de usuarios | 14 | 10 | 0 | 0 | 4 |
| Gestión de perfiles | 12 | 3 | 0 | 4 | 5 |
| Gestión de publicaciones | 11 | 0 | 0 | 0 | 11 |
| Gestión de proyectos | 22 | 0 | 0 | 0 | 22 |
| Gestión de contratos | 5 | 0 | 0 | 0 | 5 |
| Gestión de conexiones y mensajería | 5 | 0 | 0 | 0 | 5 |
| **Total** | **69** | **13** | **0** | **4** | **52** |
