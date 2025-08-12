package AndromeDraick.menuInteractivo.utilidades;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class NametagManager {

    private final Plugin plugin;

    public NametagManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void ocultarJugador(Player jugador) {
        // Ejecutar en región del jugador (Folia compatible)
        plugin.getServer().getRegionScheduler().runDelayed(plugin, jugador.getLocation(), task -> {
            String nombreFalso = "§6"; // Esto se ve vacío pero es un color invisible

            jugador.setCustomName(nombreFalso);
            jugador.setCustomNameVisible(true);
            jugador.setDisplayName(nombreFalso);
            jugador.setPlayerListName(nombreFalso);
        }, 20L); // 1 segundo de espera para asegurar que haya cargado
    }

    public void ocultarTodos() {
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            ocultarJugador(jugador);
        }
    }
}
