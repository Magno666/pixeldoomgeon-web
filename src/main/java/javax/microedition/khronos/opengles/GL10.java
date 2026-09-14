/* PixelDoomgeon — GL10. El juego no lo usa como objeto, pero si lee sus
   constantes. GPL-3.0-or-later */
package javax.microedition.khronos.opengles;

public interface GL10 {
    int GL_BLEND                 = 0x0BE2;
    int GL_ONE                   = 1;
    int GL_SRC_ALPHA             = 0x0302;
    int GL_ONE_MINUS_SRC_ALPHA   = 0x0303;
    int GL_SCISSOR_TEST          = 0x0C11;
    int GL_DEPTH_TEST            = 0x0B71;
    int GL_CULL_FACE             = 0x0B44;
    int GL_COLOR_BUFFER_BIT      = 0x00004000;
    int GL_DEPTH_BUFFER_BIT      = 0x00000100;
}
