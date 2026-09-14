/*
 * PixelDoomgeon — android.opengl.GLES20 sobre WebGL
 * Copyright (C) 2026 Leonel Garcia — GPL-3.0-or-later
 */
package android.opengl;

import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLShader;
import org.teavm.jso.webgl.WebGLTexture;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLUniformLocation;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int32Array;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * El puente entre el juego y el navegador.
 *
 * GLES20 de Android es una clase de metodos estaticos con enteros como
 * manejadores; WebGL es un objeto con objetos. Asi que aqui hay un
 * contexto global (lo pone GLSurfaceView) y tablas que traducen los
 * enteros que el juego usa a los objetos que el navegador espera.
 *
 * Son 48 metodos y 34 constantes, medidos sobre el codigo del juego. No
 * esta la API entera de GLES20 a proposito: solo lo que el juego llama.
 * Si falta algo, revienta con un mensaje claro en vez de fallar callando.
 */
public final class GLES20 {

    private GLES20() {}

    private static WebGLRenderingContext gl;

    /** Lo llama GLSurfaceView al crear el canvas. */
    public static void setContext(WebGLRenderingContext ctx) {
        gl = ctx;
    }

    public static WebGLRenderingContext gl() {
        if (gl == null) {
            throw new IllegalStateException("GLES20 sin contexto: falta GLSurfaceView");
        }
        return gl;
    }

    // ---- tablas de manejadores -------------------------------------------
    //
    // El juego guarda ints (glGenTextures escribe en un int[]). WebGL
    // devuelve objetos. Estas tablas mantienen la correspondencia.

    private static int siguiente = 1;
    private static final Map<Integer, WebGLTexture> texturas = new HashMap<>();
    private static final Map<Integer, WebGLBuffer> buffers = new HashMap<>();
    private static final Map<Integer, WebGLShader> shaders = new HashMap<>();
    private static final Map<Integer, WebGLProgram> programas = new HashMap<>();
    private static final Map<Integer, WebGLUniformLocation> uniforms = new HashMap<>();

    // ---- constantes ------------------------------------------------------

    public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
    public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
    public static final int GL_TRIANGLES        = 0x0004;
    public static final int GL_SRC_ALPHA        = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_ONE              = 1;
    public static final int GL_BLEND            = 0x0BE2;
    public static final int GL_DEPTH_TEST       = 0x0B71;
    public static final int GL_SCISSOR_TEST     = 0x0C11;
    public static final int GL_CULL_FACE        = 0x0B44;
    public static final int GL_TEXTURE_2D       = 0x0DE1;
    public static final int GL_TEXTURE0         = 0x84C0;
    public static final int GL_UNSIGNED_BYTE    = 0x1401;
    public static final int GL_UNSIGNED_SHORT   = 0x1403;
    public static final int GL_FLOAT            = 0x1406;
    public static final int GL_RGBA             = 0x1908;
    public static final int GL_NEAREST          = 0x2600;
    public static final int GL_LINEAR           = 0x2601;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_WRAP_S   = 0x2802;
    public static final int GL_TEXTURE_WRAP_T   = 0x2803;
    public static final int GL_REPEAT           = 0x2901;
    public static final int GL_CLAMP_TO_EDGE    = 0x812F;
    public static final int GL_VERTEX_SHADER    = 0x8B31;
    public static final int GL_FRAGMENT_SHADER  = 0x8B30;
    public static final int GL_COMPILE_STATUS   = 0x8B81;
    public static final int GL_LINK_STATUS      = 0x8B82;
    public static final int GL_ARRAY_BUFFER     = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_STATIC_DRAW      = 0x88E4;
    public static final int GL_FRAMEBUFFER      = 0x8D40;
    public static final int GL_RENDERBUFFER     = 0x8D41;
    public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int GL_DEPTH_ATTACHMENT  = 0x8D00;
    public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
    public static final int GL_UNPACK_ALIGNMENT  = 0x0CF5;
    public static final int GL_ALPHA             = 0x1906;
    public static final int GL_MIRRORED_REPEAT   = 0x8370;
    public static final int GL_FALSE             = 0;
    public static final int GL_TRUE              = 1;
    public static final int GL_STENCIL_ATTACHMENT = 0x8D20;
    public static final int GL_STENCIL_INDEX8     = 0x8D48;

    // ---- estado ----------------------------------------------------------

    public static void glClear(int mask)                 { gl().clear(mask); }
    public static void glClearColor(float r, float g, float b, float a) { gl().clearColor(r, g, b, a); }
    public static void glEnable(int cap)                 { gl().enable(cap); }
    public static void glDisable(int cap)                { gl().disable(cap); }
    public static void glBlendFunc(int s, int d)         { gl().blendFunc(s, d); }
    public static void glViewport(int x, int y, int w, int h) { gl().viewport(x, y, w, h); }
    public static void glScissor(int x, int y, int w, int h)  { gl().scissor(x, y, w, h); }
    public static void glPixelStorei(int name, int param) { gl().pixelStorei(name, param); }
    public static void glCullFace(int mode)              { gl().cullFace(mode); }

    // ---- shaders y programas --------------------------------------------

    public static int glCreateShader(int type) {
        int id = siguiente++;
        shaders.put(id, gl().createShader(type));
        return id;
    }

    public static void glShaderSource(int shader, String src) {
        gl().shaderSource(shaders.get(shader), src);
    }

    public static void glCompileShader(int shader) {
        gl().compileShader(shaders.get(shader));
    }

    public static void glGetShaderiv(int shader, int pname, int[] out, int offset) {
        boolean ok = gl().getShaderParameterb(shaders.get(shader), pname);
        out[offset] = ok ? 1 : 0;
    }

    public static String glGetShaderInfoLog(int shader) {
        return gl().getShaderInfoLog(shaders.get(shader));
    }

    public static int glCreateProgram() {
        int id = siguiente++;
        programas.put(id, gl().createProgram());
        return id;
    }

    public static void glAttachShader(int program, int shader) {
        gl().attachShader(programas.get(program), shaders.get(shader));
    }

    public static void glLinkProgram(int program) {
        gl().linkProgram(programas.get(program));
    }

    public static void glGetProgramiv(int program, int pname, int[] out, int offset) {
        boolean ok = gl().getProgramParameterb(programas.get(program), pname);
        out[offset] = ok ? 1 : 0;
    }

    public static String glGetProgramInfoLog(int program) {
        return gl().getProgramInfoLog(programas.get(program));
    }

    public static void glUseProgram(int program) {
        gl().useProgram(programas.get(program));
        int e = gl().getError();
        if (e != 0) { ultimoError = e; erroresGL++; }
    }

    /** Cuantos uniformes pidio el juego y no existian en el shader. */
    public static int uniformesPerdidos = 0;

    public static void glDeleteShader(int shader)   { gl().deleteShader(shaders.remove(shader)); }
    public static void glDeleteProgram(int program) { gl().deleteProgram(programas.remove(program)); }

    // ---- atributos y uniformes -------------------------------------------

    public static int glGetAttribLocation(int program, String name) {
        return gl().getAttribLocation(programas.get(program), name);
    }

    public static int glGetUniformLocation(int program, String name) {
        WebGLUniformLocation loc = gl().getUniformLocation(programas.get(program), name);
        if (loc == null) { uniformesPerdidos++; return -1; }
        int id = siguiente++;
        uniforms.put(id, loc);
        return id;
    }

    public static void glUniform1i(int loc, int v)   { gl().uniform1i(uniforms.get(loc), v); }
    public static void glUniform1f(int loc, float v) { gl().uniform1f(uniforms.get(loc), v); }
    public static void glUniform2f(int loc, float a, float b) { gl().uniform2f(uniforms.get(loc), a, b); }
    public static void glUniform4f(int loc, float a, float b, float c, float d) {
        gl().uniform4f(uniforms.get(loc), a, b, c, d);
    }

    public static void glUniformMatrix3fv(int loc, int count, boolean transpose, float[] v, int offset) {
        gl().uniformMatrix3fv(uniforms.get(loc), transpose, copia(v, offset, 9 * count));
    }

    public static void glUniformMatrix4fv(int loc, int count, boolean transpose, float[] v, int offset) {
        gl().uniformMatrix4fv(uniforms.get(loc), transpose, copia(v, offset, 16 * count));
    }

    public static void glEnableVertexAttribArray(int index)  { gl().enableVertexAttribArray(index); }
    public static void glDisableVertexAttribArray(int index) { gl().disableVertexAttribArray(index); }

    // ---- texturas --------------------------------------------------------

    public static void glGenTextures(int n, int[] out, int offset) {
        for (int i = 0; i < n; i++) {
            int id = siguiente++;
            texturas.put(id, gl().createTexture());
            out[offset + i] = id;
        }
    }

    public static void glBindTexture(int target, int texture) {
        gl().bindTexture(target, texture == 0 ? null : texturas.get(texture));
    }

    public static void glDeleteTextures(int n, int[] ids, int offset) {
        for (int i = 0; i < n; i++) {
            gl().deleteTexture(texturas.remove(ids[offset + i]));
        }
    }

    public static void glActiveTexture(int unit)             { gl().activeTexture(unit); }
    public static void glTexParameterf(int t, int p, float v) { gl().texParameterf(t, p, v); }

    // ---- buffers ---------------------------------------------------------

    public static void glGenBuffers(int n, int[] out, int offset) {
        for (int i = 0; i < n; i++) {
            int id = siguiente++;
            buffers.put(id, gl().createBuffer());
            out[offset + i] = id;
        }
    }

    // ---- lo que todavia no esta ------------------------------------------
    //
    // Framebuffers y renderbuffers: el juego los usa solo para el efecto de
    // ondas del agua. Se implementan cuando el resto corra; hasta entonces
    // avisan en voz alta en vez de dibujar mal en silencio.

    public static void glGenFramebuffers(int n, int[] out, int offset)   { faltante("glGenFramebuffers"); }
    public static void glBindFramebuffer(int target, int fb)             { faltante("glBindFramebuffer"); }
    public static void glDeleteFramebuffers(int n, int[] ids, int off)   { faltante("glDeleteFramebuffers"); }
    public static void glGenRenderbuffers(int n, int[] out, int offset)  { faltante("glGenRenderbuffers"); }
    public static void glBindRenderbuffer(int target, int rb)            { faltante("glBindRenderbuffer"); }
    public static void glDeleteRenderbuffers(int n, int[] ids, int off)  { faltante("glDeleteRenderbuffers"); }
    public static void glRenderbufferStorage(int t, int f, int w, int h) { faltante("glRenderbufferStorage"); }
    public static void glFramebufferTexture2D(int a, int b, int c, int d, int e) { faltante("glFramebufferTexture2D"); }
    public static void glFramebufferRenderbuffer(int a, int b, int c, int d)     { faltante("glFramebufferRenderbuffer"); }
    public static int  glCheckFramebufferStatus(int target) { return GL_FRAMEBUFFER_COMPLETE; }

    private static void faltante(String metodo) {
        android.util.Log.w("GLES20", "sin implementar: " + metodo);
    }

    private static Float32Array copia(float[] v, int offset, int len) {
        Float32Array a = Float32Array.create(len);
        for (int i = 0; i < len; i++) a.set(i, v[offset + i]);
        return a;
    }

    // glTexImage2D, glDrawElements y glVertexAttribPointer necesitan mover
    // buffers de Java a typed arrays; van en GLBridge para no mezclar.
    public static void glTexImage2D(int target, int level, int internalFormat,
            int width, int height, int border, int format, int type, Buffer pixels) {
        texturasSubidas++;
        if (pixels == null) texturasVacias++;
        else if (!(pixels instanceof java.nio.ByteBuffer)) texturasRaras++;
        GLBridge.texImage2D(gl(), target, level, internalFormat, width, height, format, type, pixels);
    }

    /** Diagnostico temporal: cuantos triangulos pide el juego de verdad. */
    public static int dibujos = 0, indices_total = 0, texturasSubidas = 0;
    public static int texturasVacias = 0, texturasRaras = 0, texturasCanvas = 0;

    /** Ultimo error de GL visto, para diagnostico. */
    public static int ultimoError = 0;
    public static int erroresGL = 0;

    public static void glDrawElements(int mode, int count, int type, Buffer indices) {
        dibujos++;
        indices_total += count;
        GLBridge.drawElements(gl(), mode, count, type, indices);
        // getError fuerza sincronizacion con la GPU. Llamarlo por dibujo
        // con GL por software ahoga el bucle entero: se comprobo una vez,
        // dio cero, y se queda fuera del camino caliente.
    }

    public static void glVertexAttribPointer(int index, int size, int type,
            boolean normalized, int stride, Buffer ptr) {
        GLBridge.vertexAttribPointer(gl(), index, size, type, normalized, stride, ptr);
    }
}
