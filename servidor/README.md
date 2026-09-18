# El bloque de nginx

Copia de `/etc/nginx/sites-enabled/pixeldoomgeon.jaliscomundial.com`. Vive
aquí por lo mismo que la portada: si sólo existe en `/etc` no hay forma de
saber qué cambió ni de volver atrás.

**No se despliega solo.** `desplegar.sh` no lo toca a propósito — tocar la
configuración de nginx en un servidor con catorce sitios no es algo que
deba pasar sin querer. Para aplicarlo:

    cp servidor/nginx-pixeldoomgeon.conf \
       /etc/nginx/sites-available/pixeldoomgeon.jaliscomundial.com
    nginx -t && systemctl reload nginx

## Lo que hace, y por qué

- `open_file_cache` — son 188 archivos pedidos casi a la vez; guardar sus
  descriptores en memoria evita repetir el `stat()` de cada uno.
- `gzip_static on` — sirve el `juego.js.gz` que deja `desplegar.sh`. Sin
  esto nginx comprime los 6.25 MB del bundle **en cada carga**.
- `juego.js` con `immutable` un año — su `<script src>` lleva `?v=<sello>`,
  así que la URL cambia sola cuando el contenido cambia. El `index.html`
  sigue en `no-cache` porque es quien lleva el sello: cachearlo fue el
  fallo que dejó a Leonel probando contra un `juego.js` viejo.

## Lo que NO se hizo, y por qué

**HTTP/2.** En nginx 1.24 el flag va en el `listen`, y los catorce sitios
del servidor comparten la misma IP y el mismo puerto 443: activarlo aquí
se lo activa a todos, incluidos los de clientes. El `http2 on;` por bloque
llegó en nginx 1.25.1, que no está en los repos de Ubuntu noble. Con la
precarga ya en 6 s de mediana no compensa ni cambiar de nginx ni pedir una
segunda IP.
