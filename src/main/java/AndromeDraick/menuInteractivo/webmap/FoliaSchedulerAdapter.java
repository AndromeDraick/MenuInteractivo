package AndromeDraick.menuInteractivo.webmap;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.World;

import java.lang.reflect.Method;

public class FoliaSchedulerAdapter implements SchedulerAdapter {
    private final MenuInteractivo plugin;
    private Object regionScheduler;   // io.papermc.paper.threadedregions.scheduler.RegionScheduler
    private Method executeMethod;     // execute(World, int blockX, int blockZ, Runnable)

    public FoliaSchedulerAdapter(MenuInteractivo plugin) {
        this.plugin = plugin;
        try {
            Class<?> regionClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            Class<?> serverClass = Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            Method getRegionScheduler = serverClass.getMethod("getRegionScheduler");
            Object paperServer = serverClass.cast(org.bukkit.Bukkit.getServer());
            regionScheduler = getRegionScheduler.invoke(paperServer);
            executeMethod = regionClass.getMethod("execute", World.class, int.class, int.class, Runnable.class);
        } catch (Throwable t) {
            plugin.getLogger().warning("[WebMap] Folia no detectada correctamente, se degradará a ejecutar en el hilo principal.");
            regionScheduler = null;
            executeMethod = null;
        }
    }

    @Override
    public void runRegionTask(World world, int blockX, int blockZ, Runnable task) {
        if (regionScheduler != null && executeMethod != null) {
            try {
                executeMethod.invoke(regionScheduler, world, blockX, blockZ, task);
                return;
            } catch (Throwable ignored) {}
        }
        // Fallback: ejecutar en el hilo principal
        org.bukkit.Bukkit.getScheduler().runTask(plugin, task);
    }
}
