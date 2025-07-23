package AndromeDraick.menuInteractivo.utilidades;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SistemaTrabajos implements Listener {

    private static final List<String> TRABAJOS_VALIDOS = List.of(
            "Granjero", "Minero", "Herrero", "Carpintero",
            "Agricultor", "Alquimista", "Guardia", "Cazador"
    );

    private final Map<UUID,String> trabajosJugadores = new ConcurrentHashMap<>();
    private final Map<UUID, LocalDateTime> fechasAsignacion = new ConcurrentHashMap<>();

    public SistemaTrabajos(MenuInteractivo plugin) {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, plugin);
    }

    public boolean setTrabajo(UUID uuid, String trabajo) {
        String cap = capitalizar(trabajo);
        if (!TRABAJOS_VALIDOS.contains(cap)) return false;

        trabajosJugadores.put(uuid, cap);
        fechasAsignacion.put(uuid, LocalDateTime.now());

        MenuInteractivo.getInstancia().getBaseDeDatos().actualizarTrabajo(uuid, cap);
        MenuInteractivo.getInstancia().getBaseDeDatos().actualizarFechaTrabajo(uuid, LocalDateTime.now());

        return true;
    }

    public String getTrabajo(UUID uuid) {
        return trabajosJugadores.getOrDefault(uuid, "Sin trabajo");
    }

    public void removerTrabajo(UUID uuid) {
        trabajosJugadores.remove(uuid);
        fechasAsignacion.remove(uuid);
    }

    public List<String> getTrabajosValidos() {
        return Collections.unmodifiableList(TRABAJOS_VALIDOS);
    }

    public boolean puedeCambiarTrabajo(UUID uuid) {
        LocalDateTime asignacion = fechasAsignacion.get(uuid);
        if (asignacion == null) return true;

        Duration diferencia = Duration.between(asignacion, LocalDateTime.now());
        return diferencia.toHours() >= 48;
    }

    public String tiempoRestante(UUID uuid) {
        LocalDateTime asignacion = fechasAsignacion.get(uuid);
        if (asignacion == null) return "0h";

        long faltan = 48 - Duration.between(asignacion, LocalDateTime.now()).toHours();
        return faltan + "h";
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        var bd = MenuInteractivo.getInstancia().getBaseDeDatos();
        String trabajo = bd.obtenerTrabajoJugador(uuid);
        if (trabajo != null && !trabajo.equalsIgnoreCase("Sin trabajo")) {
            trabajosJugadores.put(uuid, trabajo);
        }

        LocalDateTime fecha = bd.obtenerFechaTrabajo(uuid);
        if (fecha != null) {
            fechasAsignacion.put(uuid, fecha);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        trabajosJugadores.remove(uuid);
        fechasAsignacion.remove(uuid);
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        texto = texto.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
