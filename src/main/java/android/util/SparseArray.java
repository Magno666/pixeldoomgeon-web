/* PixelDoomgeon — SparseArray sobre HashMap. GPL-3.0-or-later */
package android.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * En Android existe para no crear objetos Integer. Aqui eso da igual, asi
 * que es un HashMap con las claves ordenadas: el juego recorre por indice
 * con keyAt/valueAt y espera un orden estable.
 */
public class SparseArray<E> {

    private final Map<Integer, E> mapa = new HashMap<>();
    private List<Integer> orden = new ArrayList<>();
    private boolean sucio = false;

    public SparseArray() {}
    public SparseArray(int capacidadInicial) {}

    public void put(int key, E value) {
        if (!mapa.containsKey(key)) sucio = true;
        mapa.put(key, value);
    }

    public E get(int key) { return mapa.get(key); }
    public E get(int key, E porDefecto) { E v = mapa.get(key); return v != null ? v : porDefecto; }
    public void remove(int key) { if (mapa.remove(key) != null) sucio = true; }
    public void delete(int key) { remove(key); }
    public void clear() { mapa.clear(); orden.clear(); sucio = false; }
    public int size() { return mapa.size(); }

    public int keyAt(int index) { return claves().get(index); }
    public E valueAt(int index) { return mapa.get(claves().get(index)); }
    public int indexOfKey(int key) { return claves().indexOf(key); }

    private List<Integer> claves() {
        if (sucio || orden.size() != mapa.size()) {
            orden = new ArrayList<>(mapa.keySet());
            Collections.sort(orden);
            sucio = false;
        }
        return orden;
    }
}
