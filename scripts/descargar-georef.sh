# Descarga el catálogo oficial de provincias y localidades desde Georef
# y lo deja como recurso estático versionado del proyecto (no requiere
# volver a pegarle a la API nunca más).
set -euo pipefail

DEST="src/main/resources/georef"
mkdir -p "$DEST"

echo "Descargando provincias..."
curl.exe -s "https://apis.datos.gob.ar/georef/api/provincias?campos=id,nombre,centroide&max=30" \
  -o "$DEST/provincias.json"

echo "Descargando localidades por provincia..."
IDS=$(jq.exe -r '.provincias[].id' "$DEST/provincias.json" | tr -d '\r')
for ID in $IDS; do
  echo "  - provincia $ID"
  curl.exe -s "https://apis.datos.gob.ar/georef/api/localidades?provincia=${ID}&campos=id,nombre,centroide,provincia&max=5000" \
    -o "$DEST/tmp_${ID}.json"
done

jq.exe -s '{localidades: [.[].localidades] | add}' "$DEST"/tmp_*.json > "$DEST/localidades.json"
rm "$DEST"/tmp_*.json

echo "Listo. Archivos generados en $DEST (committealos al repo)."