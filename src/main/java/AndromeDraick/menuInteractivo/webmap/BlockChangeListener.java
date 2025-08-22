package AndromeDraick.menuInteractivo.webmap;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.logging.Logger;

public class BlockChangeListener implements Listener {
    private final WebMapConfig cfg;
    private final TileQueue queue;
    private final Logger log;

    public BlockChangeListener(WebMapConfig cfg, TileQueue queue, Logger log) {
        this.cfg = cfg;
        this.queue = queue;
        this.log = log;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        actualizarTile(e.getBlock().getWorld(), e.getBlock().getX(), e.getBlock().getZ());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        actualizarTile(e.getBlock().getWorld(), e.getBlock().getX(), e.getBlock().getZ());
    }

    private void actualizarTile(World w, int bx, int bz) {
        if (!cfg.mundos.contains(w.getName())) return;
        int ts = cfg.tileSize;
        int tx = TileIndex.tileXFromBlock(bx, ts);
        int ty = TileIndex.tileYFromBlock(bz, ts);
        // z = 0 (nativo)
        queue.enqueue(w.getName(), 0, tx, ty);
    }

    public static void unregister(Listener l) {
        HandlerList.unregisterAll(l);
    }
}
