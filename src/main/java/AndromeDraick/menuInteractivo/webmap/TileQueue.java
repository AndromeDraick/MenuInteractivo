package AndromeDraick.menuInteractivo.webmap;

import java.util.concurrent.*;
import java.util.logging.Logger;

public class TileQueue {
    private final WebMapConfig cfg;
    private final TileRenderer renderer;
    private final Logger log;

    private final BlockingQueue<Job> queue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService workers = Executors.newFixedThreadPool(2);

    public TileQueue(WebMapConfig cfg, TileRenderer renderer, Logger log) {
        this.cfg = cfg;
        this.renderer = renderer;
        this.log = log;

        // Limitador simple: procesa ~limite_rps_render tiles/seg
        long periodMs = Math.max(1, 1000L / Math.max(1, cfg.limiteRpsRender));
        scheduler.scheduleAtFixedRate(this::tick, 500, periodMs, TimeUnit.MILLISECONDS);
    }

    public void enqueue(String world, int z, int x, int y) {
        queue.offer(new Job(world, z, x, y));
    }

    private void tick() {
        Job j = queue.poll();
        if (j == null) return;
        workers.submit(() -> {
            try {
                renderer.renderTile(j.world, j.z, j.x, j.y);
            } catch (Exception e) {
                log.warning("[WebMap] Error renderizando tile " + j + ": " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        scheduler.shutdownNow();
        workers.shutdownNow();
    }

    static class Job {
        final String world;
        final int z, x, y;
        Job(String w, int z, int x, int y) { this.world = w; this.z = z; this.x = x; this.y = y; }
        public String toString(){ return world+"/"+z+"/"+x+"/"+y; }
    }
}
