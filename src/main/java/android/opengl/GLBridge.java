/* PixelDoomgeon — buffers de Java a WebGL. GPL-3.0-or-later */
package android.opengl;

import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Uint16Array;
import org.teavm.jso.typedarrays.Uint8Array;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;

/**
 * Lo unico que GLES20 y WebGL no comparten: como se pasan los datos.
 *
 * Android manda java.nio.Buffer directo a la GPU. WebGL quiere typed
 * arrays de JavaScript. Aqui se copia de uno al otro.
 *
 * Se copia, no se comparte: es mas lento en teoria, pero el juego sube
 * la geometria una vez por nivel y los billboards son cuatro vertices.
 * Optimizar esto antes de que algo se vea lento seria trabajo a ciegas.
 */
final class GLBridge {

    private GLBridge() {}

    static void texImage2D(WebGLRenderingContext gl, int target, int level,
            int internalFormat, int width, int height, int format, int type, Buffer pixels) {
        Uint8Array datos = null;

        // El juego sube texturas como IntBuffer: un int por pixel, escrito
        // en el orden de bytes de la maquina. En ARM y en x86 eso es
        // little-endian, asi que el byte bajo del int es el primero que ve
        // GL. Con GL_RGBA ese primero es el rojo.
        //
        // Hasta ahora solo se convertia ByteBuffer, asi que las diez
        // texturas del juego se subian VACIAS: mil seiscientos dibujos por
        // segundo, cero errores de GL, y pantalla negra.
        if (pixels instanceof IntBuffer) {
            IntBuffer b = (IntBuffer) pixels;
            int n = b.remaining();
            datos = Uint8Array.create(n * 4);
            int pos = b.position();
            for (int i = 0; i < n; i++) {
                int p = b.get(pos + i);
                datos.set(i * 4,     (short) (p & 0xFF));
                datos.set(i * 4 + 1, (short) ((p >> 8) & 0xFF));
                datos.set(i * 4 + 2, (short) ((p >> 16) & 0xFF));
                datos.set(i * 4 + 3, (short) ((p >>> 24) & 0xFF));
            }
        } else if (pixels instanceof ByteBuffer) {
            ByteBuffer b = (ByteBuffer) pixels;
            int n = b.remaining();
            datos = Uint8Array.create(n);
            int pos = b.position();
            for (int i = 0; i < n; i++) {
                datos.set(i, (short) (b.get(pos + i) & 0xFF));
            }
        }
        gl.texImage2D(target, level, internalFormat, width, height, 0, format, type, datos);
    }

    static void drawElements(WebGLRenderingContext gl, int mode, int count, int type, Buffer indices) {
        // WebGL dibuja desde el buffer ligado, no desde memoria del cliente,
        // asi que hay que subirlo cada vez. El juego llama a esto una vez
        // por malla, no por triangulo.
        if (indices instanceof ShortBuffer) {
            ShortBuffer b = (ShortBuffer) indices;
            Uint16Array a = Uint16Array.create(count);
            int pos = b.position();
            for (int i = 0; i < count; i++) {
                a.set(i, b.get(pos + i) & 0xFFFF);
            }
            org.teavm.jso.webgl.WebGLBuffer buf = gl.createBuffer();
            gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, buf);
            gl.bufferData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, a,
                          WebGLRenderingContext.STREAM_DRAW);
            gl.drawElements(mode, count, type, 0);
            gl.deleteBuffer(buf);
        }
    }

    static void vertexAttribPointer(WebGLRenderingContext gl, int index, int size,
            int type, boolean normalized, int stride, Buffer ptr) {
        if (ptr instanceof FloatBuffer) {
            FloatBuffer b = (FloatBuffer) ptr;
            int n = b.remaining();
            Float32Array a = Float32Array.create(n);
            int pos = b.position();
            for (int i = 0; i < n; i++) {
                a.set(i, b.get(pos + i));
            }
            org.teavm.jso.webgl.WebGLBuffer buf = gl.createBuffer();
            gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buf);
            gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, a,
                          WebGLRenderingContext.STREAM_DRAW);
            gl.vertexAttribPointer(index, size, type, normalized, stride, 0);
        }
    }
}
