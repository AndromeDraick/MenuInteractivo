package AndromeDraick.menuInteractivo.utilidades;

import java.util.*;
import org.bukkit.entity.Player;

public class SistemaTrabajos {

    private final Map<UUID, String> trabajosJugadores = new HashMap<>();

    private final Set<String> trabajosValidos = new HashSet<>(Arrays.asList(
            "Granjero", "Minero", "Herrero", "Carpintero",
            "Agricultor", "Alquimista", "Guardia", "Cazador"
    ));

    // === Métodos para jugadores online ===

    public void setTrabajo(Player jugador, String trabajo) {
        setTrabajo(jugador.getUniqueId(), trabajo);
    }

    public String getTrabajo(Player jugador) {
        return getTrabajo(jugador.getUniqueId());
    }

    public boolean tieneTrabajo(Player jugador) {
        return trabajosJugadores.containsKey(jugador.getUniqueId());
    }

    public void removerTrabajo(Player jugador) {
        trabajosJugadores.remove(jugador.getUniqueId());
    }

    // === Métodos para UUIDs directos (offline/online) ===

    public void setTrabajo(UUID uuid, String trabajo) {
        if (!trabajosValidos.contains(capitalizar(trabajo))) return;
        trabajosJugadores.put(uuid, capitalizar(trabajo));
    }

    public String getTrabajo(UUID uuid) {
        return trabajosJugadores.getOrDefault(uuid, "Sin trabajo");
    }

    public boolean tieneTrabajo(UUID uuid) {
        return trabajosJugadores.containsKey(uuid);
    }

    public void removerTrabajo(UUID uuid) {
        trabajosJugadores.remove(uuid);
    }

    public Set<String> getTrabajosValidos() {
        return Collections.unmodifiableSet(trabajosValidos);
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }
}
