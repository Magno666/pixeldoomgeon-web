/* PixelDoomgeon — PackageManager. GPL-3.0-or-later */
package android.content.pm;

/** El juego solo pregunta la version para enseñarla en el titulo. */
public class PackageManager {

    public static class NameNotFoundException extends Exception {}

    public PackageInfo getPackageInfo(String paquete, int flags)
            throws NameNotFoundException {
        return new PackageInfo();
    }
}
