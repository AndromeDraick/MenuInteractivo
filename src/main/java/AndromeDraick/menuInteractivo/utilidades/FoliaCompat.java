package AndromeDraick.menuInteractivo.utilidades;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class FoliaCompat {
    private FoliaCompat() {}
    private static Boolean IS_FOLIA;

    public static boolean isFolia() {
        if (IS_FOLIA != null) return IS_FOLIA;
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); IS_FOLIA = true; }
        catch (ClassNotFoundException e) { IS_FOLIA = false; }
        return IS_FOLIA;
    }

    public static Object runGlobalRepeating(Plugin plugin, long initialDelayTicks, long periodTicks, Runnable task) {
        if (!isFolia()) {
            int id = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, initialDelayTicks, periodTicks);
            return Integer.valueOf(id);
        }
        try {
            Method getGRS = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            Object grs = getGRS.invoke(Bukkit.getServer());
            Method runAtFixedRate = grs.getClass().getMethod("runAtFixedRate",
                    Plugin.class, Consumer.class, long.class, long.class);
            return runAtFixedRate.invoke(grs, plugin, (Consumer<Object>) st -> task.run(), initialDelayTicks, periodTicks);
        } catch (Throwable t) {
            plugin.getLogger().warning("[MI] No se pudo programar tarea global en Folia: " + t.getMessage());
            return null;
        }
    }

    public static void runOnPlayerThread(Plugin plugin, Player player, Runnable task) {
        if (!isFolia()) { task.run(); return; }
        try {
            Method getScheduler = player.getClass().getMethod("getScheduler");
            Object ps = getScheduler.invoke(player);
            Method run = ps.getClass().getMethod("run", Plugin.class, Consumer.class, Object.class);
            run.invoke(ps, plugin, (Consumer<Object>) st -> task.run(), null);
        } catch (Throwable t) { task.run(); }
    }

    public static void cancel(Plugin plugin, Object handle) {
        if (handle == null) return;
        if (!isFolia()) {
            if (handle instanceof Integer) Bukkit.getScheduler().cancelTask((Integer) handle);
            return;
        }
        try { handle.getClass().getMethod("cancel").invoke(handle); } catch (Throwable ignored) {}
    }
}
