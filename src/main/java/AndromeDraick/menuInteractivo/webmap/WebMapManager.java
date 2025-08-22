package AndromeDraick.menuInteractivo.webmap;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class WebMapManager {
    private final MenuInteractivo plugin;
    private final Logger log;
    private final File dataFolder;

    private WebMapConfig cfg;
    private WebMapServer server;
    private TileQueue tileQueue;
    private TileRenderer tileRenderer;
    private SchedulerAdapter schedulerAdapter;
    private BlockChangeListener listener;

    public WebMapManager(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.dataFolder = plugin.getDataFolder();
    }

    public void enable() {
        cfg = WebMapConfig.cargar(dataFolder);

        // Detectar Folia o Paper (simple heurística)
        if (isFolia()) {
            schedulerAdapter = new FoliaSchedulerAdapter(plugin);
            log.info("[WebMap] Usando FoliaSchedulerAdapter.");
        } else {
            schedulerAdapter = new PaperSchedulerAdapter(plugin);
            log.info("[WebMap] Usando PaperSchedulerAdapter.");
        }

        // Render
        tileRenderer = new TileRenderer(plugin, cfg, schedulerAdapter);
        tileQueue = new TileQueue(cfg, tileRenderer, log);

        // Eventos
        if (cfg.actualizarPorEventos) {
            listener = new BlockChangeListener(cfg, tileQueue, log);
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }

        // HTTP
        if (cfg.habilitado) {
            try {
                server = new WebMapServer(log, cfg);
                server.start();
            } catch (Exception e) {
                log.severe("[WebMap] Error iniciando servidor HTTP: " + e.getMessage());
            }
        }

        log.info("[WebMap] Cargado. Mundos: " + String.join(", ", cfg.mundos));
    }

    public void disable() {
        if (server != null) {
            server.stop();
            server = null;
        }
        if (listener != null) {
            BlockChangeListener.unregister(listener);
            listener = null;
        }
        if (tileQueue != null) {
            tileQueue.shutdown();
            tileQueue = null;
        }
        tileRenderer = null;
        schedulerAdapter = null;
    }

    public void renderInicial(String mundo, int radioTiles) {
        World w = Bukkit.getWorld(mundo);
        if (w == null) {
            log.warning("[WebMap] Mundo no encontrado: " + mundo);
            return;
        }
        // Render en radio de tiles alrededor del (0,0)
        int z = cfg.zoomMax; // puedes usar z nativo como “nivel base”
        int ts = cfg.tileSize;

        // Para simplicidad arrancamos en z nativo = 0 (1 bloque = 1 px) y mapeamos tiles
        int zNativo = 0;
        for (int tx = -radioTiles; tx <= radioTiles; tx++) {
            for (int ty = -radioTiles; ty <= radioTiles; ty++) {
                tileQueue.enqueue(w.getName(), zNativo, tx, ty);
            }
        }
        log.info("[WebMap] Pre-render en " + mundo + " encola " + ((radioTiles*2+1)*(radioTiles*2+1)) + " tiles.");
    }

    public TileQueue getTileQueue() {
        return tileQueue;
    }

    public WebMapConfig getCfg() {
        return cfg;
    }

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
