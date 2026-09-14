/* PixelDoomgeon — base64 a mano. GPL-3.0-or-later */
package android.content;

/**
 * java.util.Base64 existe en Java 8, pero TeaVM no siempre lo trae
 * completo, y esto es corto. Se usa para que los bytes de una partida
 * sobrevivan dentro de una cadena de localStorage.
 */
final class Base64 {

    private Base64() {}

    private static final String A =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    static String codificar(byte[] datos) {
        StringBuilder sb = new StringBuilder((datos.length + 2) / 3 * 4);
        for (int i = 0; i < datos.length; i += 3) {
            int b = (datos[i] & 0xFF) << 16;
            if (i + 1 < datos.length) b |= (datos[i + 1] & 0xFF) << 8;
            if (i + 2 < datos.length) b |= (datos[i + 2] & 0xFF);
            sb.append(A.charAt((b >> 18) & 63));
            sb.append(A.charAt((b >> 12) & 63));
            sb.append(i + 1 < datos.length ? A.charAt((b >> 6) & 63) : '=');
            sb.append(i + 2 < datos.length ? A.charAt(b & 63) : '=');
        }
        return sb.toString();
    }

    static byte[] decodificar(String s) {
        int relleno = 0;
        int largo = s.length();
        while (largo > 0 && s.charAt(largo - 1) == '=') { relleno++; largo--; }
        int n = largo * 6 / 8;
        byte[] out = new byte[n];
        int bits = 0, acum = 0, pos = 0;
        for (int i = 0; i < largo; i++) {
            int v = A.indexOf(s.charAt(i));
            if (v < 0) continue;
            acum = (acum << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                if (pos < n) out[pos++] = (byte) ((acum >> bits) & 0xFF);
            }
        }
        return out;
    }
}
