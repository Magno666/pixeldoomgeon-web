/* PixelDoomgeon — anotacion de Android, aqui no hace nada. GPL-3.0-or-later */
package android.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD,
         ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE})
public @interface SuppressLint { String[] value(); }
