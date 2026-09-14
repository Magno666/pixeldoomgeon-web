/* PixelDoomgeon — android.graphics.RectF. GPL-3.0-or-later */
package android.graphics;

public class RectF {
    public float left, top, right, bottom;
    public RectF() {}
    public RectF(float l, float t, float r, float b) { set(l, t, r, b); }
    public RectF(RectF o) { set(o.left, o.top, o.right, o.bottom); }
    public void set(float l, float t, float r, float b) { left = l; top = t; right = r; bottom = b; }
    public void set(RectF o) { set(o.left, o.top, o.right, o.bottom); }
    public void setEmpty() { left = top = right = bottom = 0; }
    public boolean isEmpty() { return left >= right || top >= bottom; }
    public float width() { return right - left; }
    public float height() { return bottom - top; }
    public float centerX() { return (left + right) * 0.5f; }
    public float centerY() { return (top + bottom) * 0.5f; }
    public void offset(float dx, float dy) { left += dx; top += dy; right += dx; bottom += dy; }
    public boolean contains(float x, float y) { return x >= left && x < right && y >= top && y < bottom; }
}
