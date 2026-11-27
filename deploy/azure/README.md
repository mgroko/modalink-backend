# Despliegue de ModaLink en Azure for Students

Guía para levantar el sistema completo (backend + frontend + PostgreSQL) en una VM
gratuita de **Azure for Students**, con HTTPS automático y DuckDNS.

> **Costo:** Azure for Students da **USD 100 de crédito por 12 meses** y no pide
> tarjeta. Una VM `Standard_B2s` (2 vCPU / 4 GB) consume aproximadamente
> USD 30/mes, así que el crédito alcanza para varios meses de prueba. Se puede
> estirar apagando (deallocating) la VM cuando no se usa.

---

## 1. Arquitectura

```text
Internet (HTTPS, DuckDNS)
   │
Caddy (contenedor web)
   ├── /api/*           → backend Spring Boot :8080 (se quita el prefijo /api)
   ├── /oauth2/*        → backend
   ├── /login/oauth2/*  → backend
   ├── /uploads/*       → backend (imágenes de emails)
   └── /*               → SPA Vue (index.html)
        │
        ├── PostgreSQL 17 (volumen pgdata)
        └── uploads (volumen persistente)
```

- **Un solo origen** (`https://TU_DOMINIO`): cookies JWT/CSRF first-party, sin
  problemas de `SameSite=None` ni de cookies de terceros.
- **PostgreSQL en la misma VM**, con volumen persistente.
- **Uploads en un volumen**, así las imágenes sobreviven reinicios y redeploys.
- **HTTPS automático** con Caddy + Let's Encrypt.
- **Arquitectura x86_64**, así que OpenCV y el writer WebP funcionan nativo
  (sin emulación).

---

## 2. Requisitos

- Cuenta de **Azure for Students** verificada con email institucional.
- Una cuenta gratuita de **DuckDNS**.
- Clave SSH (la generamos abajo).
- Los repositorios públicos:
  - `https://github.com/mgroko/modalink-backend` (backend)
  - `https://github.com/mgroko/modalink-frontend` (frontend)
- La rama **`deploy/azure`** pusheada en ambos repos. Es la que contiene estos
  archivos y los workflows que construyen las imágenes.

---

## 3. Crear la cuenta de Azure for Students

1. Entrá a <https://azure.microsoft.com/free/students/>.
2. Iniciá sesión con tu email institucional.
3. Completá la verificación de estudiante (edad, institución, email).
4. No hace falta tarjeta.
5. Al terminar, entrá al portal: <https://portal.azure.com/>.

> Si la verificación falla, el plan B es Hugging Face Spaces + Neon + Cloudflare
> Pages. Avisame y armamos esa variante.

---

## 4. Crear la VM

En el portal de Azure: **Create a resource → Virtual machine**.

| Campo | Valor |
|---|---|
| Resource group | `modalink-rg` (crear nuevo) |
| Virtual machine name | `modalink-vm` |
| Region | `East US` (más barato) o `Brazil South` (menos latencia, más caro) |
| Availability options | `No infrastructure redundancy required` |
| Security type | `Standard` |
| Image | `Ubuntu Server 24.04 LTS - x64 Gen2` |
| Size | `Standard_B2s` (2 vCPU, 4 GiB) |
| Authentication type | `SSH public key` |
| Username | `azureuser` |
| SSH public key source | `Use existing public key` o `Generate new key pair` |
| Public inbound ports | `SSH (22)`, `HTTP (80)`, `HTTPS (443)` |
| OS disk type | `Standard SSD` |
| OS disk size | `64 GiB` (o 30 GiB para ahorrar) |
| Public IP | `Create new` → **Static** |

> **IP estática:** cuesta unos centavos por mes, pero evita tener que actualizar
> DuckDNS cada vez que la VM se reinicia. Si preferís IP dinámica, usá el script
> `duckdns-update.sh` con un cron.

### 4.1. Generar la clave SSH (Windows PowerShell)

```powershell
ssh-keygen -t ed25519 -C "modalink-azure" -f $env:USERPROFILE\.ssh\modalink_azure
Get-Content $env:USERPROFILE\.ssh\modalink_azure.pub
```

Pegá el contenido del `.pub` en el campo **SSH public key** del portal.

### 4.2. Abrir los puertos en el NSG

El asistente crea un Network Security Group (NSG). Verificá en
**VM → Networking → Inbound security rules**:

| Puerto | Origen | Uso |
|---|---|---|
| 22 | **Tu IP** (no `Any`) | SSH |
| 80 | `Any` | Let's Encrypt / HTTP |
| 443 | `Any` | HTTPS |

No abras `5432` (PostgreSQL) ni `8080` (backend): solo se accede por la red
interna de Docker.

### 4.3. Conectarse

```powershell
ssh -i $env:USERPROFILE\.ssh\modalink_azure azureuser@IP_PUBLICA
```

---

## 5. Configurar DuckDNS

1. Entrá a <https://www.duckdns.org/> e iniciá sesión.
2. Creá un subdominio, por ejemplo `modalink`.
3. En **current ip**, poné la IP pública estática de la VM.
4. Guardá y verificá:

```powershell
nslookup modalink.duckdns.org
```

Debe devolver la IP de Azure.

En el `.env` vas a usar:

```dotenv
SITE_ADDRESS=modalink.duckdns.org
SITE_URL=https://modalink.duckdns.org
```

> `SITE_ADDRESS` es solo el hostname, **sin** `https://`. `SITE_URL` sí lleva
> `https://`.

---

## 6. Preparar la VM (Docker + firewall + swap)

Desde tu máquina, copiá el script y ejecutalo:

```powershell
scp -i $env:USERPROFILE\.ssh\modalink_azure `
  deploy\azure\bootstrap.sh azureuser@IP_PUBLICA:/tmp/bootstrap.sh

ssh -i $env:USERPROFILE\.ssh\modalink_azure `
  "sudo bash /tmp/bootstrap.sh"
```

O, si la rama ya está pusheada:

```bash
sudo bash -c "$(curl -fsSL https://raw.githubusercontent.com/mgroko/Proyecto-MVP/deploy/azure/deploy/azure/bootstrap.sh)"
```

El script instala Docker, habilita el firewall (22/80/443), crea 2 GB de swap y
deja listo `/opt/modalink`.

**Importante:** cerrá la sesión SSH y volvé a entrar para que tu usuario
pertenezca al grupo `docker`.

---

## 7. Clonar el backend y configurar variables

```bash
cd /opt/modalink
git clone -b deploy/azure https://github.com/mgroko/modalink-backend.git backend
cd backend/deploy/azure

cp .env.example .env
chmod 600 .env
nano .env
```

Generá secretos fuertes:

```bash
openssl rand -base64 48   # JWT_SECRET
openssl rand -base64 32   # POSTGRES_PASSWORD
```

Completá como mínimo:

```dotenv
SITE_ADDRESS=modalink.duckdns.org
SITE_URL=https://modalink.duckdns.org

POSTGRES_PASSWORD=...
JWT_SECRET=...
ADMIN_EMAIL=tu-email@ejemplo.com
ADMIN_PASSWORD=...
ADMIN_DNI=00000000
```

Dejá `COOKIE_SECURE=true` y `COOKIE_SAME_SITE=Lax`: al ser un solo origen, es
correcto y seguro.

### 7.1. Imágenes de GHCR

Los workflows de GitHub Actions publican:

- `ghcr.io/mgroko/modalink-backend:latest`
- `ghcr.io/mgroko/modalink-frontend:latest`

Si tu usuario/organización no es `mgroko`, cambiá `BACKEND_IMAGE` y `WEB_IMAGE`
en el `.env` por el owner correcto.

En este repositorio los paquetes quedaron **públicos** (verificado con la API de
GHCR), así que la VM puede hacer `docker compose pull` sin login. Si en tu fork
quedan privados, tenés dos opciones:

**A. Hacerlos públicos (más simple):**

GitHub → tu perfil → **Packages** → `modalink-backend` → **Package settings** →
**Change visibility** → **Public**. Repetir con `modalink-frontend`.

**B. Mantenerlos privados y loguearte en la VM:**

```bash
echo "TU_PAT_CON_READ_PACKAGES" | docker login ghcr.io -u mgroko --password-stdin
```

---

## 8. Construir y publicar las imágenes

Cada push a `deploy/azure` dispara los workflows:

- Backend: `.github/workflows/publish-image.yml`
- Frontend: `.github/workflows/publish-image.yml`

Podés verlos en **GitHub → Actions**. El backend tarda unos minutos porque
compila el JAR (sin correr tests) y sube una imagen de ~1.8 GB (el JAR pesa
~482 MB por los binarios de OpenCV/OpenBLAS de todas las plataformas).

Si preferís construirlas localmente (tu Windows tiene Docker x86_64), también
podés hacerlo:

```powershell
# Backend
mvn -B -DskipTests package
docker build -t ghcr.io/mgroko/modalink-backend:latest .

# Frontend
cd C:\Users\HP\Desktop\modalink-frontend
docker build -f deploy/azure/Dockerfile `
  --build-arg VITE_API_BASE_URL=/api `
  -t ghcr.io/mgroko/modalink-frontend:latest .
```

Después hacé `docker push` (con `docker login ghcr.io`) o subí las imágenes por
un registry.

---

## 9. Levantar el sistema

```bash
cd /opt/modalink/backend/deploy/azure

docker compose pull
docker compose up -d
docker compose ps
docker compose logs -f backend
```

La primera vez:

1. PostgreSQL crea la base y carga `ModaLinkBD.sql` (puede tardar 10–30 s).
2. El backend valida el esquema (`ddl-auto=validate`) y crea el admin inicial.
3. Caddy obtiene el certificado de Let's Encrypt.

Esperá a ver en los logs algo como `Started Application` y a que el healthcheck
pase a `healthy`.

### 9.1. Verificar

```bash
curl -fsS https://modalink.duckdns.org/api/actuator/health
# {"status":"UP"}
```

Abrí `https://modalink.duckdns.org/` e iniciá sesión con `ADMIN_EMAIL` y
`ADMIN_PASSWORD`.

Verificá la base:

```bash
docker compose exec db psql -U modalink -d modalink -c '\dt' | head
```

---

## 10. Google OAuth (opcional)

1. En Google Cloud Console → **APIs & Services → Credentials**.
2. Abrí tu OAuth Client ID (Web application) o creá uno.
3. En **Authorized redirect URIs** agregá:

```text
https://modalink.duckdns.org/login/oauth2/code/google
```

4. Guardá el Client ID y el Client Secret en el `.env`:

```dotenv
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

5. Reiniciá el backend:

```bash
docker compose up -d backend
```

Si la app de Google está en modo **Testing**, agregá tu email como test user en
la pantalla de consentimiento.

> El login local y el admin funcionan sin Google. Podés dejar OAuth para después.

---

## 11. Emails (opcional)

Por defecto están desactivados:

```dotenv
NOTIFICACIONES_EMAIL_HABILITADO=false
```

Para habilitarlos, por ejemplo con Brevo (300 emails/día gratis):

```dotenv
NOTIFICACIONES_EMAIL_HABILITADO=true
NOTIFICACIONES_EMAIL_REMITENTE=no-responder@tudominio.com
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
SPRING_MAIL_USERNAME=tu_usuario
SPRING_MAIL_PASSWORD=tu_smtp_key
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
```

Después:

```bash
docker compose up -d backend
docker compose logs -f backend
```

Sin SMTP real, las notificaciones in-app siguen funcionando; los emails y el
auto-login por email no se envían.

---

## 12. Operación

### Logs

```bash
docker compose logs -f backend
docker compose logs -f web
docker compose logs -f db
```

### Reiniciar vs recrear

`docker compose restart backend` reinicia el proceso, pero **no recarga el
`.env`**. Si cambiaste variables de entorno, usá:

```bash
docker compose up -d --force-recreate backend
```

### Actualizar el backend

El backend se publica como imagen en GHCR. El flujo es:

1. Pusheás a la rama `deploy/azure` de
   `https://github.com/mgroko/modalink-backend`.
2. GitHub Actions construye
   `ghcr.io/mgroko/modalink-backend:latest`.
3. En la VM:

```bash
cd /opt/modalink/backend
git pull
cd deploy/azure
docker compose pull backend
docker compose up -d --force-recreate backend
docker compose logs -f backend
```

`git pull` actualiza el compose, los scripts y este README; la imagen del
backend viene de GHCR. Si solo cambiaste código Java, alcanza con el pull y el
recreate.

### Actualizar el frontend

El frontend se construye desde `https://github.com/mgroko/modalink-frontend`,
rama `deploy/azure`. En la VM **no** hace falta clonar el frontend:

1. Pusheás a la rama `deploy/azure` del repo frontend.
2. GitHub Actions construye
   `ghcr.io/mgroko/modalink-frontend:latest`.
3. En la VM:

```bash
cd /opt/modalink/backend/deploy/azure
docker compose pull web
docker compose up -d --force-recreate web
docker compose logs -f web
```

Si cambiaste el `Caddyfile`, también se reconstruye la imagen `web`; no toca el
backend.

### Actualizar ambos

```bash
cd /opt/modalink/backend
git pull
cd deploy/azure
docker compose pull
docker compose up -d --force-recreate backend web
```

No hace falta recrear `db` para una actualización normal. Si lo hacés, el
volumen de PostgreSQL se mantiene, pero el servicio se reinicia y la app queda
unos segundos sin base.

### Cambios en `.env`

Después de editar `.env`:

```bash
docker compose up -d --force-recreate backend
```

Si cambiaste `SITE_ADDRESS` o `SITE_URL`, además recreá `web`:

```bash
docker compose up -d --force-recreate backend web
```

Y actualizá DuckDNS y Google Cloud si corresponde.

Si cambiaste `POSTGRES_PASSWORD` **después** de inicializar la base, la password
vieja sigue dentro de PostgreSQL. Opciones: volver a la anterior, cambiarla con
`ALTER USER`, o recrear el volumen con `docker compose down -v` (destructivo:
borra base y uploads).

### Cambios en el compose o en Caddy

- `docker-compose.yml` del backend: `git pull` en la VM y
  `docker compose up -d`.
- `Caddyfile` del frontend: push al repo frontend, esperá la imagen y después
  `docker compose pull web && docker compose up -d --force-recreate web`.

### Migraciones de base

`ddl-auto=validate` no aplica migraciones. Si agregás tablas o columnas, aplicá
el SQL **antes** de arrancar el backend nuevo:

```bash
# revisar docs/3_diseño/migraciones/
docker compose exec -T db psql -U modalink -d modalink < migracion.sql
```

Si el esquema no coincide, el backend falla al iniciar.

### Comandos útiles

```bash
# Estado de los servicios
docker compose ps

# Health desde la VM
curl -fsS https://modalink.duckdns.org/api/actuator/health

# Shell de PostgreSQL
docker compose exec db psql -U modalink -d modalink

# Uso de recursos
docker stats

# Reiniciar solo el backend (no recarga .env)
docker compose restart backend

# Recrear backend y web con la config actual
docker compose up -d --force-recreate backend web

# Detener el stack (la VM sigue prendida)
docker compose stop

# Volver a levantar el stack
docker compose start
```

### Backup

```bash
cd /opt/modalink/backend/deploy/azure

docker compose exec -T db pg_dump -U modalink -d modalink -Fc \
  > modalink-$(date +%F).dump

docker run --rm \
  -v modalink_uploads:/data \
  -v "$PWD":/backup \
  alpine tar czf /backup/uploads-$(date +%F).tar.gz -C /data .
```

Guardá esos archivos fuera de la VM.

---

## 13. Detener la VM y controlar el costo

Sí, podés detenerla; lo importante es que quede **Stopped (Deallocated)**.

### Desde el portal

VM → **Stop**. Verificá que el estado final sea **Stopped (Deallocated)**. Si
el portal te deja la VM en "Stopped" a secas, usá **Stop (Deallocate)** o el
comando de CLI.

### Desde CLI

```bash
# Detener y desasignar (deja de cobrar compute)
az vm deallocate --resource-group modalink-rg --name modalink-vm

# Volver a prender
az vm start --resource-group modalink-rg --name modalink-vm
```

`az vm stop` **no** desasigna: la VM queda detenida pero el compute se sigue
facturando. Usá `az vm deallocate`.

### Qué pasa cuando está detenida

- El sitio deja de responder: DuckDNS sigue apuntando a la IP, pero no hay
  servicio escuchando.
- El **disco** y la **IP estática** siguen existiendo y se facturan
  (≈ USD 8,45/mes). El crédito se consume mucho más lento.
- La base, los uploads y el certificado de Caddy **persisten** en los volúmenes
  `pgdata`, `uploads` y `caddy_data`.
- Al iniciar de nuevo, Docker arranca solo y los contenedores se reinician
  (`restart: unless-stopped`). Esperá 1–2 minutos y verificá:

```bash
cd /opt/modalink/backend/deploy/azure
docker compose ps
curl -fsS https://modalink.duckdns.org/api/actuator/health
```

- Si la IP fuera **dinámica**, podría cambiar al detener/iniciar; actualizá
  DuckDNS. Con IP estática no cambia.
- No borres el resource group si querés conservar datos. Si lo vas a borrar,
  primero hacé backup de PostgreSQL y uploads.

### Presupuesto

- Configurá alertas en **Cost Management + Billing → Budgets** (por ejemplo,
  USD 50, USD 80 y USD 95).
- Si el crédito se agota, el stack Docker es portable a otra VM o a Hugging Face
  Spaces (con storage efímero).

---

## 14. Troubleshooting

| Síntoma | Causa probable | Solución |
|---|---|---|
| `502 Bad Gateway` | Backend aún no arrancó o falló | `docker compose logs backend`; esperar el healthcheck |
| `Schema-validation: missing table` | El esquema no se cargó | Revisar logs de `db`; borrar volumen y reiniciar (ver abajo) |
| `password authentication failed` | `.env` cambió después de crear la DB | Recrear el volumen o actualizar la password en PostgreSQL |
| Caddy no obtiene certificado | DNS, puerto 80/443 cerrado | `nslookup`, NSG, `ufw status`, `docker compose logs web` |
| `unauthorized` al hacer `docker compose pull` | Paquete GHCR privado | Hacerlo público o `docker login ghcr.io` |
| Backend OOM / reinicios | Poca RAM o heap alto | `docker stats`; bajar `JAVA_TOOL_OPTIONS` o subir la VM |
| Imágenes 404 o URL `//app/uploads` | `APP_STORAGE_UPLOAD_DIR` absoluto | Dejarlo en `uploads` (relativo a `/app`); el volumen va en `/app/uploads` |
| Cookies no se guardan | Estás entrando por IP o HTTP | Usar `https://TU_DOMINIO`, `COOKIE_SECURE=true` |
| `redirect_uri_mismatch` | URI distinto en Google | Copiar exactamente `https://TU_DOMINIO/login/oauth2/code/google` |
| `permission denied` con docker | Falta relogin tras `usermod` | Cerrar y volver a entrar por SSH |

### Reset de la base (destructivo)

Solo para un entorno de prueba, si el esquema quedó mal cargado:

```bash
docker compose down -v   # borra PostgreSQL Y uploads
docker compose up -d
```

---

## 15. Checklist de seguridad

- [ ] NSG: 22 solo desde tu IP; 80/443 desde cualquier lado; nada más.
- [ ] `.env` con permisos `600` y **nunca** commiteado.
- [ ] `JWT_SECRET`, `POSTGRES_PASSWORD` y `ADMIN_PASSWORD` fuertes.
- [ ] `COOKIE_SECURE=true` y acceso por HTTPS.
- [ ] Cambiar `ADMIN_EMAIL` por uno real.
- [ ] No reutilizar el `.env` de desarrollo (tiene secretos de Google y valores
      de ejemplo).
- [ ] Rotar el Client Secret de Google si alguna vez se compartió.
- [ ] Mantener Ubuntu y Docker actualizados.
- [ ] Backups de PostgreSQL y uploads.

---

## 16. Archivos de este despliegue

```text
backend/
├── Dockerfile                         # imagen de ejecución del backend
├── .dockerignore
├── .gitattributes                     # LF para scripts
├── .github/workflows/publish-image.yml
└── deploy/azure/
    ├── docker-compose.yml
    ├── .env.example
    ├── bootstrap.sh
    ├── duckdns-update.sh
    └── README.md                      # este archivo

frontend/
├── .dockerignore
├── .github/workflows/publish-image.yml
└── deploy/azure/
    ├── Dockerfile                     # Node build + Caddy
    └── Caddyfile
```

El frontend se compila con `VITE_API_BASE_URL=/api`, así que todas las llamadas
(axios, SSE, imágenes) van al mismo origen y Caddy las deriva al backend.
