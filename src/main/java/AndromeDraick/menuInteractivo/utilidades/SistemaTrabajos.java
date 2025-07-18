package AndromeDraick.menuInteractivo.utilidades;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gestiona el trabajo asignado a cada jugador en memoria, con carga automática al join.
 */
public class SistemaTrabajos implements Listener {

    // Trabajos válidos en orden definido:
    private static final List<String> TRABAJOS_VALIDOS = List.of(
            "Granjero", "Minero", "Herrero", "Carpintero",
            "Agricultor", "Alquimista", "Guardia", "Cazador"
    );

    /** Mapa UUID → trabajo capitalizado. */
    private final Map<UUID,String> trabajosJugadores = new ConcurrentHashMap<>();

    public SistemaTrabajos(MenuInteractivo plugin) {
        // Registrar listener para join/quit
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, plugin);
    }

    /** Asigna un trabajo a un UUID. Devuelve true si fue válido y asignado. */
    public boolean setTrabajo(UUID uuid, String trabajo) {
        String cap = capitalizar(trabajo);
        if (!TRABAJOS_VALIDOS.contains(cap)) return false;
        trabajosJugadores.put(uuid, cap);
        // Podrías aquí llamar a la BBDD: plugin.getBaseDeDatos().actualizarTrabajo(uuid, cap);
        return true;
    }

    /** Obtiene el trabajo de un UUID, o "Sin trabajo" si no existe. */
    public String getTrabajo(UUID uuid) {
        return trabajosJugadores.getOrDefault(uuid, "Sin trabajo");
    }

    /** Quita el trabajo del jugador (vuelve a "Sin trabajo"). */
    public void removerTrabajo(UUID uuid) {
        trabajosJugadores.remove(uuid);
    }

    /** Devuelve los trabajos válidos, de forma inmutable y ordenada. */
    public List<String> getTrabajosValidos() {
        return Collections.unmodifiableList(TRABAJOS_VALIDOS);
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        texto = texto.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    // ————————————————
    // Listeners para auto-sincronización
    // ————————————————

    /** Al entrar un jugador, carga su trabajo desde la BBDD si quieres. */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        // Ejemplo: sincronizar con base de datos
        String trabajo = MenuInteractivo.getInstancia()
                .getBaseDeDatos()
                .obtenerTrabajo(p.getUniqueId());
        if (trabajo != null && !trabajo.equalsIgnoreCase("Sin trabajo")) {
            trabajosJugadores.put(p.getUniqueId(), trabajo);
        }
    }

    /** Al salir, opcionalmente liberamos memoria si hay muchos jugadores. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        trabajosJugadores.remove(e.getPlayer().getUniqueId());
    }
}
