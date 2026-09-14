/* PixelDoomgeon — android.graphics.Rect. GPL-3.0-or-later */
package android.graphics;

public class Rect {
    public int left, top, right, bottom;
    public Rect() {}
    public Rect(int l, int t, int r, int b) { set(l, t, r, b); }
    public Rect(Rect o) { set(o.left, o.top, o.right, o.bottom); }
    public void set(int l, int t, int r, int b) { left = l; top = t; right = r; bottom = b; }
    public void setEmpty() { left = top = right = bottom = 0; }
    public boolean isEmpty() { return left >= right || top >= bottom; }
    public int width() { return right - left; }
    public int height() { return bottom - top; }
    public void union(int x, int y) {
        if (isEmpty()) { set(x, y, x + 1, y + 1); return; }
        if (x < left) left = x; else if (x + 1 > right) right = x + 1;
        if (y < top) top = y;  else if (y + 1 > bottom) bottom = y + 1;
    }
}
