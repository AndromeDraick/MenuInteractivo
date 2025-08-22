package AndromeDraick.menuInteractivo.webmap;

import org.bukkit.World;

public interface SchedulerAdapter {
    void runRegionTask(World world, int blockX, int blockZ, Runnable task);
}
