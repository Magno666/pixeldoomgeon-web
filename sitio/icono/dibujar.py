#!/usr/bin/env python3
"""
PixelDoomgeon -- el icono del sitio. GPL-3.0-or-later

Un pasillo en perspectiva de un punto: es lo unico que dice "primera
persona" de un vistazo y no se parece a ningun otro fork de Pixel
Dungeon, que era el requisito -- el icono de Sprouted no se toca.

La boca del pasillo es el marco entero del icono, sin margen. Con
margen las cuatro paredes se estrechan por los dos lados a la vez y
sale un moño, no un pasillo; probado.

Cada pixel cae en una de cinco zonas segun a que borde apunta desde el
punto de fuga, que esta en el centro. La pared izquierda va iluminada y
la derecha en sombra: sin esa diferencia las dos paredes se leen como
un solo rombo.
"""
from PIL import Image

LADO = 16
FONDO = LADO / 2 - 0.5          # centro geometrico de la rejilla
MEDIO = 2                        # medio ancho del fondo del pasillo

MURO_LUZ   = (224, 168, 58, 255)   # el oro de la barra de carga
MURO_SOMBRA= (138, 101, 36, 255)
TECHO      = (21, 18, 12, 255)
SUELO      = (58, 46, 20, 255)
LEJOS      = (220, 227, 221, 255)  # la luz al final
MARCO      = (11, 13, 12, 255)     # el negro de la pagina


def zona(x, y):
    dx = x - FONDO
    dy = y - FONDO
    if abs(dx) <= MEDIO and abs(dy) <= MEDIO:
        # El borde del hueco lejano, para que no flote.
        if abs(dx) == MEDIO or abs(dy) == MEDIO:
            return MURO_LUZ
        return LEJOS
    if abs(dx) > abs(dy):
        return MURO_LUZ if dx < 0 else MURO_SOMBRA
    return TECHO if dy < 0 else SUELO


def pintar(escala):
    img = Image.new("RGBA", (LADO, LADO))
    px = img.load()
    for y in range(LADO):
        for x in range(LADO):
            px[x, y] = zona(x, y)
    if escala > 1:
        # NEAREST: es pixel art, interpolar lo emborrona.
        img = img.resize((LADO * escala, LADO * escala), Image.NEAREST)
    return img


if __name__ == "__main__":
    pintar(1).save("favicon-16.png")
    pintar(2).save("favicon-32.png")
    pintar(12).save("icono-192.png")
    pintar(32).save("icono-512.png")
    pintar(1).save("favicon.ico", sizes=[(16, 16), (32, 32)])
    pintar(11).save("apple-touch-icon.png")   # 176 px, suficiente para iOS
    print("hecho")
