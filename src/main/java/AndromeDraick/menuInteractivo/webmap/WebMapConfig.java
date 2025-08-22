package AndromeDraick.menuInteractivo.webmap;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class WebMapConfig {
    public boolean habilitado;
    public int puerto;
    public String bind;
    public String rutaWeb;
    public String rutaTiles;
    public java.util.List<String> mundos;
    public int zoomMin;
    public int zoomMax;
    public int tileSize;
    public boolean sombreadoAltura;
    public boolean actualizarPorEventos;
    public int limiteRpsRender;
    public boolean enableIndex;

    public static WebMapConfig cargar(File dataFolder) {
        File cfg = new File(dataFolder.getParentFile(), "config_webmap.yml"); // plugins/MenuInteractivo/config_webmap.yml
        FileConfiguration y = YamlConfiguration.loadConfiguration(cfg);

        WebMapConfig c = new WebMapConfig();
        c.habilitado = y.getBoolean("habilitado", true);
        c.puerto = y.getInt("puerto", 8123);
        c.bind = y.getString("bind", "0.0.0.0");
        c.rutaWeb = y.getString("ruta_web", "plugins/MenuInteractivo/webmap/web");
        c.rutaTiles = y.getString("ruta_tiles", "plugins/MenuInteractivo/webmap/tiles");
        c.mundos = y.getStringList("mundos");
        if (c.mundos == null || c.mundos.isEmpty()) {
            c.mundos = java.util.Collections.singletonList("world");
        }
        c.zoomMin = y.getInt("zoom_min", 0);
        c.zoomMax = y.getInt("zoom_max", 4);
        c.tileSize = y.getInt("tile_size", 256);
        c.sombreadoAltura = y.getBoolean("sombreado_altura", true);
        c.actualizarPorEventos = y.getBoolean("actualizar_por_eventos", true);
        c.limiteRpsRender = y.getInt("limite_rps_render", 2);
        c.enableIndex = y.getBoolean("enable_index", true);

        // Asegurar directorios
        new File(c.rutaWeb).mkdirs();
        new File(c.rutaTiles).mkdirs();

        return c;
    }
}
