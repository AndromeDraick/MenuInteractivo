package AndromeDraick.menuInteractivo.webmap;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.HashMap;
import java.util.Map;

public class ColorMap {
    private final Map<Material, Integer> base = new HashMap<>();

    public ColorMap() {
        // Colores base simples; ajusta/añade para más fidelidad vanilla
        put(Material.GRASS_BLOCK, rgb(109, 153, 48));
        put(Material.DIRT,        rgb(134, 96, 67));
        put(Material.STONE,       rgb(125, 125, 125));
        put(Material.SAND,        rgb(218, 210, 158));
        put(Material.WATER,       rgb(64, 64, 255));    // se ajustará por profundidad si quieres
        put(Material.LAVA,        rgb(255, 83, 32));
        put(Material.SNOW,        rgb(240, 251, 251));
        // Fallback
        put(Material.AIR,         rgba(0,0,0,0));
    }

    public int colorFor(Material m, Biome biome) {
        // Podrías tintar pasto/hojas por bioma aquí
        return base.getOrDefault(m, rgb(150,150,150));
    }

    private void put(Material m, int color) { base.put(m, color); }

    public static int rgb(int r, int g, int b) {
        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
