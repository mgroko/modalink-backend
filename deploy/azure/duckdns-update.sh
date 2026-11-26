#!/usr/bin/env bash
# Actualiza la IP de un subdominio DuckDNS con la IP pública actual de la VM.
# Uso:
#   DUCKDNS_DOMAIN=modalink DUCKDNS_TOKEN=xxxx sudo -E bash duckdns-update.sh
#
# Cron sugerido (cada 5 minutos):
#   */5 * * * * DUCKDNS_DOMAIN=modalink DUCKDNS_TOKEN=xxxx /opt/modalink/backend/deploy/azure/duckdns-update.sh >> /var/log/duckdns.log 2>&1
set -euo pipefail

: "${DUCKDNS_DOMAIN:?Definí DUCKDNS_DOMAIN}"
: "${DUCKDNS_TOKEN:?Definí DUCKDNS_TOKEN}"

curl -fsS "https://www.duckdns.org/update?domains=${DUCKDNS_DOMAIN}&token=${DUCKDNS_TOKEN}&ip="
echo
