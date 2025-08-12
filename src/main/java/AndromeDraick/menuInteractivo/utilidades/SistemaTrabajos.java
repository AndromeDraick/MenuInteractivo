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

    // Formas base válidas (neutras, para validación y normalización)
    private static final List<String> TRABAJOS_BASE_VALIDOS = List.of(
            "Granjero", "Minero", "Herrero", "Carpintero",
            "Agricultor", "Alquimista", "Guardia", "Cazador"
    );

    // Trabajo base por defecto si no tiene (se aplica género al entrar)
    private static final String TRABAJO_BASE_POR_DEFECTO = "Agricultor";

    private final Map<UUID, String> trabajosJugadores = new ConcurrentHashMap<>();
    private final Map<UUID, LocalDateTime> fechasAsignacion = new ConcurrentHashMap<>();

    public SistemaTrabajos(MenuInteractivo plugin) {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, plugin);
    }

    /**
     * Asigna un trabajo al jugador respetando su género (masc/fem).
     * Acepta nombres ya feminizados/masculinizados o en base.
     */
    public boolean setTrabajo(UUID uuid, String trabajoEntrada) {
        if (trabajoEntrada == null || trabajoEntrada.isBlank()) return false;

        // 1) Normalizamos a la forma base (p.ej. "Minera" -> "Minero")
        String base = normalizarABase(trabajoEntrada);
        if (!TRABAJOS_BASE_VALIDOS.contains(base)) return false;

        // 2) Tomamos el género real del jugador y aplicamos la forma correcta
        String genero = getGeneroSeguro(uuid);
        String trabajoFinal = aplicarGenero(base, genero);

        // 3) Guardamos en cache y BD + fecha
        trabajosJugadores.put(uuid, trabajoFinal);
        LocalDateTime ahora = LocalDateTime.now();
        fechasAsignacion.put(uuid, ahora);

        var bd = MenuInteractivo.getInstancia().getBaseDeDatos();
        bd.actualizarTrabajo(uuid, trabajoFinal);
        bd.actualizarFechaTrabajo(uuid, ahora);

        return true;
    }

    public String getTrabajo(UUID uuid) {
        return trabajosJugadores.getOrDefault(uuid, "Sin trabajo");
    }

    public void removerTrabajo(UUID uuid) {
        trabajosJugadores.remove(uuid);
        fechasAsignacion.remove(uuid);
    }

    /**
     * Devuelve las etiquetas base (neutras). Útiles para menús.
     */
    public List<String> getTrabajosValidos() {
        return Collections.unmodifiableList(TRABAJOS_BASE_VALIDOS);
    }

    public boolean puedeCambiarTrabajo(UUID uuid) {
        LocalDateTime asignacion = fechasAsignacion.get(uuid);
        if (asignacion == null) return true;

        Duration diferencia = Duration.between(asignacion, LocalDateTime.now());
        return diferencia.toHours() >= 5;
    }

    public String tiempoRestante(UUID uuid) {
        LocalDateTime asignacion = fechasAsignacion.get(uuid);
        if (asignacion == null) return "0h";

        long faltan = 5 - Duration.between(asignacion, LocalDateTime.now()).toHours();
        if (faltan < 0) faltan = 0;
        return faltan + "h";
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        var bd = MenuInteractivo.getInstancia().getBaseDeDatos();

        // Cargar desde BD
        String trabajoEnBD = bd.obtenerTrabajoJugador(uuid);
        LocalDateTime fecha = bd.obtenerFechaTrabajo(uuid);
        if (fecha != null) {
            fechasAsignacion.put(uuid, fecha);
        }

        String genero = getGeneroSeguro(uuid);

        if (trabajoEnBD == null || trabajoEnBD.isBlank() || trabajoEnBD.equalsIgnoreCase("Sin trabajo")) {
            // No tiene trabajo: asignar por defecto (base) ajustado a su género
            String trabajoFinal = aplicarGenero(TRABAJO_BASE_POR_DEFECTO, genero);
            trabajosJugadores.put(uuid, trabajoFinal);
            LocalDateTime ahora = LocalDateTime.now();
            fechasAsignacion.put(uuid, ahora);
            bd.actualizarTrabajo(uuid, trabajoFinal);
            bd.actualizarFechaTrabajo(uuid, ahora);
        } else {
            // Tiene trabajo: aseguramos que la forma coincida con su género
            String base = normalizarABase(trabajoEnBD);
            // Si por alguna razón lo de BD no es válido, forzamos al base por defecto
            if (!TRABAJOS_BASE_VALIDOS.contains(base)) {
                base = TRABAJO_BASE_POR_DEFECTO;
            }
            String trabajoFinal = aplicarGenero(base, genero);
            trabajosJugadores.put(uuid, trabajoFinal);

            // Si cambió (p.ej. antes "Minero", ahora debe ser "Minera"), persistimos
            if (!trabajoFinal.equalsIgnoreCase(trabajoEnBD)) {
                bd.actualizarTrabajo(uuid, trabajoFinal);
                // No tocamos la fecha de asignación por no ser un "cambio voluntario"
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        // En memoria limpiamos; en BD ya quedó persistido
        trabajosJugadores.remove(uuid);
        fechasAsignacion.remove(uuid);
    }

    // ------------------------
    // Helpers de género/normalización
    // ------------------------

    private String getGeneroSeguro(UUID uuid) {
        String g = MenuInteractivo.getInstancia().getBaseDeDatos().getGenero(uuid);
        if (g == null) return "Desconocido";
        g = g.trim();
        if (g.equalsIgnoreCase("Femenino") || g.equalsIgnoreCase("Masculino")) return g;
        return "Desconocido";
    }

    /**
     * Aplica forma masc/fem/neutral a partir de la base y el género.
     * Base siempre debe estar en TRABAJOS_BASE_VALIDOS.
     */
    private String aplicarGenero(String base, String genero) {
        boolean esF = genero.equalsIgnoreCase("Femenino");

        // Alquimista y Guardia suelen ser neutros; mantenemos tal cual.
        switch (base) {
            case "Granjero":   return esF ? "Granjera"    : "Granjero";
            case "Minero":     return esF ? "Minera"      : "Minero";
            case "Herrero":    return esF ? "Herrera"     : "Herrero";
            case "Carpintero": return esF ? "Carpintera"  : "Carpintero";
            case "Agricultor": return esF ? "Agricultora" : "Agricultor";
            case "Alquimista": return "Alquimista"; // neutral
            case "Guardia":    return "Guardia";    // neutral
            case "Cazador":    return esF ? "Cazadora"    : "Cazador";
            default:           return base; // fallback
        }
    }

    /**
     * Convierte una entrada cualquiera ("minera", "MINERO", "Minero") a su base ("Minero").
     * Solo devuelve formas base definidas en TRABAJOS_BASE_VALIDOS.
     */
    private String normalizarABase(String entrada) {
        if (entrada == null) return "";
        String t = entrada.trim().toLowerCase(Locale.ROOT);

        // Mapeos femeninos/masculinos → base
        switch (t) {
            case "granjero":
            case "granjera":
                return "Granjero";

            case "minero":
            case "minera":
                return "Minero";

            case "herrero":
            case "herrera":
                return "Herrero";

            case "carpintero":
            case "carpintera":
                return "Carpintero";

            case "agricultor":
            case "agricultora":
                return "Agricultor";

            case "alquimista":
                return "Alquimista";

            case "guardia":
                return "Guardia";

            case "cazador":
            case "cazadora":
                return "Cazador";

            default:
                // Intento de capitalizar y validar por si ya viene en base
                String cap = capitalizar(entrada);
                return TRABAJOS_BASE_VALIDOS.contains(cap) ? cap : "";
        }
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        texto = texto.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
