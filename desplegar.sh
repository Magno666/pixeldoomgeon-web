#!/bin/bash
# PixelDoomgeon — deja el build en el sitio. GPL-3.0-or-later
#
# Copiar juego.js encima no basta: el navegador se queda con el que ya
# tiene en cache -- son 6 MB, los cachea con ganas -- y el jugador sigue
# viendo los bugs de la version anterior por mas que se haya desplegado.
# Leonel lo probo en el telefono contra un juego.js viejo mas de una vez.
# Por eso el <script> lleva ?v= y aqui se sella con el build y la fecha.
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")" && pwd)"
DESTINO="${1:-/var/www/pixeldoomgeon/juego}"
JS="$RAIZ/build/generated/teavm/js/juego.js"

[ -f "$JS" ] || { echo "no hay juego.js -- corre ./gradlew generateJavaScript"; exit 1; }
[ -d "$DESTINO" ] || { echo "no existe $DESTINO"; exit 1; }

BUILD=$(grep -oE 'FP build v[0-9]+' \
  /root/proyectos/sprouted-build/app/src/main/java/com/github/dachhack/sprout/FirstPerson.java \
  | head -1 | tr -d ' ' | tr '[:upper:]' '[:lower:]')
SELLO="${BUILD:-build}-$(date +%Y%m%d%H%M)"

# El sello que ya estaba servido. Hay que leerlo AHORA: la copia de
# index.html de mas abajo lo borra, y el del repo no lleva ninguno.
# Re-sellar cuando el JS no cambio le cuesta 6 MB de descarga a cada
# persona que este jugando en ese momento, y no arregla nada.
VIEJO=$(grep -o 'juego\.js?v=[^"]*' "$DESTINO/index.html" 2>/dev/null \
  | head -1 | sed 's/.*?v=//' || true)

# La pagina sale del repo, no de lo que hubiera en el servidor: si vive
# solo en /var/www no hay forma de saber que cambio ni de volver atras.
cp "$RAIZ/sitio/index.html" "$DESTINO/index.html"

# La portada y la bitacora vivian SOLO en el servidor -- el comentario de
# arriba decia una cosa y el script hacia otra. Ahora salen del repo como
# el resto.
RAIZ_SITIO="$DESTINO/.."
cp "$RAIZ/sitio/portada.html"  "$RAIZ_SITIO/index.html"
cp "$RAIZ/sitio/bitacora.html" "$RAIZ_SITIO/bitacora.html"

# El icono. favicon.ico va en la raiz porque el navegador lo pide ahi sin
# preguntar, aunque la pagina enlace otro -- eran los unicos 404 del log.
mkdir -p "$RAIZ_SITIO/icono"
cp "$RAIZ/sitio/icono/favicon.ico" "$RAIZ_SITIO/favicon.ico"
cp "$RAIZ/sitio/icono/"*.png "$RAIZ_SITIO/icono/"

# La arena vive un nivel arriba, junto a /juego/, y usa el mismo juego.js
# -- por eso no lleva copia propia: el navegador ya lo tiene en cache de
# haber jugado, y son seis megas que no vale la pena duplicar.
mkdir -p "$DESTINO/../arena"
cp "$RAIZ/sitio/arena/index.html" "$DESTINO/../arena/index.html"
if [ -f "$DESTINO/juego.js" ] && cmp -s "$JS" "$DESTINO/juego.js" && [ -n "$VIEJO" ]; then
  SELLO="$VIEJO"
  echo "juego.js sin cambios -- se conserva el sello $SELLO"
else
  cp "$JS" "$DESTINO/juego.js"
fi
sed -i -E "s|<script src=\"juego\.js(\?v=[^\"]*)?\"|<script src=\"juego.js?v=$SELLO\"|" \
  "$DESTINO/index.html"

echo "desplegado $SELLO"
grep -o 'juego\.js?v=[^"]*' "$DESTINO/index.html"
