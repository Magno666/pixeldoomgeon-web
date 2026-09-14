/* PixelDoomgeon — descriptor de asset, solo para el audio. GPL-3.0-or-later */
package android.content.res;

public class AssetFileDescriptor {
    public final String nombre;
    public AssetFileDescriptor(String nombre) { this.nombre = nombre; }
    public Object getFileDescriptor() { return null; }
    public long getStartOffset() { return 0; }
    public long getLength() { return 0; }
    public void close() {}
}
