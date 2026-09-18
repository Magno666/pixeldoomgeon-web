#!/bin/bash
# PixelDoomgeon — arma libs/juego.jar desde el fork. GPL-3.0-or-later
#
# La receta estaba solo en el README, y ahi dice que equivocarse costo
# una tarde: si al jar se le cuela algo que no sea com/github o
# com/watabou, TeaVM resuelve web.Main desde esa copia vieja y compila
# 34 KB de nada, sin avisar. Por eso el jar se arma desde cero cada vez
# y solo con esos dos arboles.
#
# source 8 es obligatorio: el juego usa `_` como nombre de variable en
# los layouts del Sokoban, reservado desde Java 9.
set -euo pipefail

FORK="${FORK:-/root/proyectos/sprouted-build}"
RAIZ="$(cd "$(dirname "$0")" && pwd)"
TRABAJO="$(mktemp -d)"
trap 'rm -rf "$TRABAJO"' EXIT

[ -d "$FORK/app/src/main/java" ] || { echo "no esta el fork en $FORK"; exit 1; }

find "$FORK/app/src/main/java" -name "*.java" > "$TRABAJO/fuentes.txt"
echo "compilando $(wc -l < "$TRABAJO/fuentes.txt") archivos..."

# El shim de Android COMPILADO, no en sourcepath: sus fuentes importan
# org.teavm.jso, que necesita Java 17, y el juego se compila con source 8.
# Pasarlo como sourcepath arrastra a TeaVM y no compila nada.
SHIM="$RAIZ/build/classes/java/main"
[ -d "$SHIM" ] || { echo "no hay shim compilado -- corre ./gradlew classes"; exit 1; }

mkdir -p "$TRABAJO/clases"
if ! javac -nowarn -source 8 -target 8 -encoding UTF-8 \
  -cp "$SHIM:$RAIZ/libs/json.jar" \
  -d "$TRABAJO/clases" @"$TRABAJO/fuentes.txt" 2> "$TRABAJO/errores.txt"; then
  grep -E ": error" "$TRABAJO/errores.txt" | head -20
  echo "FALLO la compilacion"
  exit 1
fi

for arbol in com/github com/watabou; do
  [ -d "$TRABAJO/clases/$arbol" ] || { echo "falta $arbol"; exit 1; }
done

jar cf "$TRABAJO/juego.jar" -C "$TRABAJO/clases" com/github \
                            -C "$TRABAJO/clases" com/watabou
cp "$TRABAJO/juego.jar" "$RAIZ/libs/juego.jar"
echo "libs/juego.jar: $(unzip -l "$RAIZ/libs/juego.jar" | tail -1)"
