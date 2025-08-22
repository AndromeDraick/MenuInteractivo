package AndromeDraick.menuInteractivo.webmap;

public class TileIndex {
    // Conversión bloque → tile en zoom nativo (1px = 1bloque)
    public static int tileXFromBlock(int blockX, int tileSize) {
        return (int) Math.floor((double) blockX / (double) tileSize);
    }
    public static int tileYFromBlock(int blockZ, int tileSize) {
        return (int) Math.floor((double) blockZ / (double) tileSize);
    }

    // Posición dentro del tile (0..tileSize-1)
    public static int localX(int blockX, int tileSize) {
        int v = blockX % tileSize;
        if (v < 0) v += tileSize;
        return v;
    }
    public static int localY(int blockZ, int tileSize) {
        int v = blockZ % tileSize;
        if (v < 0) v += tileSize;
        return v;
    }
}
