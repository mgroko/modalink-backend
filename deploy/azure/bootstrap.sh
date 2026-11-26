#!/usr/bin/env bash
# =============================================================
# ModaLink - bootstrap de una VM Ubuntu (Azure)
# Ejecutar como root: sudo bash bootstrap.sh
# =============================================================
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Ejecutá con sudo: sudo bash bootstrap.sh" >&2
  exit 1
fi

TARGET_USER="${SUDO_USER:-azureuser}"
if ! id "${TARGET_USER}" >/dev/null 2>&1; then
  TARGET_USER="azureuser"
fi

echo "==> Usuario de despliegue: ${TARGET_USER}"

# -------------------------------------------------------------
# Paquetes base
# -------------------------------------------------------------
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y \
  ca-certificates \
  curl \
  git \
  gnupg \
  ufw

# -------------------------------------------------------------
# Docker (repositorio oficial)
# -------------------------------------------------------------
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

. /etc/os-release
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y \
  docker-ce \
  docker-ce-cli \
  containerd.io \
  docker-buildx-plugin \
  docker-compose-plugin

systemctl enable --now docker
usermod -aG docker "${TARGET_USER}"

# -------------------------------------------------------------
# Swap (ayuda con picos de imágenes/PDF en una VM de 4 GB)
# -------------------------------------------------------------
if ! swapon --show | grep -q '/swapfile'; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# -------------------------------------------------------------
# Firewall del sistema operativo
# (las reglas de la VCN/NSG de Azure se configuran en el portal)
# -------------------------------------------------------------
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

# -------------------------------------------------------------
# Directorio de la aplicación
# -------------------------------------------------------------
mkdir -p /opt/modalink
chown "${TARGET_USER}:${TARGET_USER}" /opt/modalink

# Zona horaria del host (opcional, los contenedores ya usan TZ)
if command -v timedatectl >/dev/null 2>&1; then
  timedatectl set-timezone America/Argentina/Buenos_Aires || true
fi

cat <<EOF

=============================================================
Docker instalado y puertos 80/443 habilitados.

Cerrá la sesión SSH y volvé a entrar para usar docker sin sudo.

Luego:
  cd /opt/modalink
  git clone -b deploy/azure https://github.com/mgroko/modalink-backend.git backend
  cd backend/deploy/azure
  cp .env.example .env
  nano .env
  docker compose pull
  docker compose up -d
  docker compose logs -f backend

Health:
  curl -fsS https://TU_DOMINIO/api/actuator/health
=============================================================
EOF
