package AndromeDraick.menuInteractivo.webmap;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class PaperSchedulerAdapter implements SchedulerAdapter {
    private final MenuInteractivo plugin;

    public PaperSchedulerAdapter(MenuInteractivo plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runRegionTask(World world, int blockX, int blockZ, Runnable task) {
        // En Paper, basta con ejecutar en el main thread
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
