# Pruebas del navegador

Playwright de verdad, no `chrome --headless --virtual-time-budget`: ese
modo frena `requestAnimationFrame` tanto que la página no junta treinta
cuadros en diez minutos, y parece que el juego está roto cuando no lo
está.

Playwright no está instalado en este proyecto — se usa el del proyecto
vecino por ruta absoluta, para no añadir una dependencia de 300 MB a un
repo que no la necesita para compilar.

    node pruebas/adornos.mjs              # contra el build local
    URL=https://pixeldoomgeon.jaliscomundial.com/juego/ node pruebas/adornos.mjs

Para el build local hace falta servirlo:

    mkdir -p /tmp/pd && cd /tmp/pd
    cp .../sitio/index.html index.html     # y quitarle el ?v=
    ln -s .../build/generated/teavm/js/juego.js juego.js
    ln -s /var/www/pixeldoomgeon/juego/assets assets
    python3 -m http.server 8799

| prueba | qué contesta |
|---|---|
| `adornos.mjs` | ¿las goteras caen sobre la tubería? Compara la posición que predice `Adornos` con los píxeles de la captura. |
| `adornos-por-piso.mjs` | lo mismo en las cinco familias de nivel. `METAS=1,7,12,17,22` |
| `ritmo.mjs` | cuadros por segundo en el piso 1 contra el 17, que es el que más adornos tiene. |

Todas necesitan `?diag` — lo ponen solas. Las órdenes viven en
`web/Diagnostico.java` y se dan por `window.__pdOrden`.
