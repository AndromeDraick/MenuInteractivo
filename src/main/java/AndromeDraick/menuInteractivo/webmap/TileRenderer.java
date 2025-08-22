package AndromeDraick.menuInteractivo.webmap;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Biome;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class TileRenderer {
    private final MenuInteractivo plugin;
    private final WebMapConfig cfg;
    private final SchedulerAdapter scheduler;
    private final ColorMap colors = new ColorMap();

    public TileRenderer(MenuInteractivo plugin, WebMapConfig cfg, SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.scheduler = scheduler;
    }

    public File getTileFile(String world, int z, int x, int y) {
        File dir = new File(cfg.rutaTiles, world + File.separator + z + File.separator + x);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, y + ".png");
    }

    public void renderTile(String worldName, int z, int tileX, int tileY) {
        // Por ahora tratamos z=0 como nativo (1:1). Más adelante generamos mipmaps para z>0.
        if (z != 0) {
            // Regla simple: renderizar a z=0 y luego downsample o delegar a otro método
            renderDownsampled(worldName, z, tileX, tileY);
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        final int ts = cfg.tileSize;
        final int startX = tileX * ts;
        final int startZ = tileY * ts;

        BufferedImage img = new BufferedImage(ts, ts, BufferedImage.TYPE_INT_ARGB);

        // Lectura del mundo debe ser en el hilo correcto (scheduler)
        scheduler.runRegionTask(world, startX, startZ, () -> {
            for (int dx = 0; dx < ts; dx++) {
                int x = startX + dx;
                for (int dz = 0; dz < ts; dz++) {
                    int zLocal = startZ + dz;
                    int argb = sampleTopColor(world, x, zLocal);
                    if (cfg.sombreadoAltura) {
                        argb = applyHeightShade(world, x, zLocal, argb);
                    }
                    img.setRGB(dx, dz, argb);
                }
            }
            // Escribir PNG fuera del hilo del mundo
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveTileImage(worldName, 0, tileX, tileY, img));
        });
    }

    private void saveTileImage(String world, int z, int x, int y, BufferedImage img) {
        try {
            File out = getTileFile(world, z, x, y);
            javax.imageio.ImageIO.write(img, "png", out);
        } catch (Exception e) {
            plugin.getLogger().warning("[WebMap] Error guardando tile: " + e.getMessage());
        }
    }

    private int sampleTopColor(World w, int x, int z) {
        int y = w.getMaxHeight() - 1;
        Material topMat = Material.AIR;
        Biome biome = w.getBiome(x, z);

        // Busca desde arriba hasta encontrar material visible
        for (; y >= w.getMinHeight(); y--) {
            Block b = w.getBlockAt(x, y, z);
            Material m = b.getType();
            if (m == Material.AIR) continue;
            // Puedes refinar: ignorar plantas altas, etc. y mostrar el “bloque sólido”
            topMat = m;
            break;
        }
        return colors.colorFor(topMat, biome);
    }

    private int applyHeightShade(World w, int x, int z, int argb) {
        int y = w.getHighestBlockYAt(x, z);
        // Sombrado leve basado en altura (normaliza a 0..1)
        double norm = (y - w.getMinHeight()) / (double) (w.getMaxHeight() - w.getMinHeight());
        double shade = 0.85 + 0.30 * norm; // 0.85..1.15
        int a = (argb >> 24) & 0xFF;
        int r = (int) Math.min(255, ((argb >> 16) & 0xFF) * shade);
        int g = (int) Math.min(255, ((argb >> 8) & 0xFF) * shade);
        int b = (int) Math.min(255, (argb & 0xFF) * shade);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    // Downsample simple de z-1 (toma 4 tiles hijos del z-1 y calcula 1 tile en z)
    private void renderDownsampled(String world, int z, int tileX, int tileY) {
        // Para mantener el ejemplo breve: devolvemos transparente si no existe el z-1.
        // (Luego puedes implementar generador de mipmaps real).
        try {
            int ts = cfg.tileSize;
            BufferedImage img = new BufferedImage(ts, ts, BufferedImage.TYPE_INT_ARGB);
            File out = getTileFile(world, z, tileX, tileY);
            out.getParentFile().mkdirs();
            ImageIO.write(img, "png", out);
        } catch (Exception e) {
            plugin.getLogger().warning("[WebMap] Downsample fallido: " + e.getMessage());
        }
    }
}
