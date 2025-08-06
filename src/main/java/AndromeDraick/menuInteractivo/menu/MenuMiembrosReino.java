package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MenuMiembrosReino implements Listener {
    private final MenuInteractivo plugin;

    public MenuMiembrosReino(MenuInteractivo plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Abre el menú de miembros del reino.
     */
    public void abrir(Player jugador) {
        GestorBaseDeDatos db = plugin.getBaseDeDatos();
        String reino = db.obtenerReinoJugador(jugador.getUniqueId());
        if (reino == null) {
            jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        List<UUID> miembros = db.obtenerMiembrosDeReino(reino);
        if (miembros.isEmpty()) {
            jugador.sendMessage(ChatColor.RED + "Tu reino no tiene miembros registrados.");
            return;
        }

        int size = ((miembros.size() - 1) / 9 + 1) * 9;
        size = Math.min(size, 54); // máximo tamaño permitido (6 filas)
        Inventory menu = Bukkit.createInventory(
                null, size,
                ChatColor.AQUA + "Miembros del Reino " + reino
        );

        int slot = 0;
        for (UUID uuid : miembros) {
            OfflinePlayer miembro = Bukkit.getOfflinePlayer(uuid);
            String rol     = db.obtenerRolJugadorEnReino(uuid);
            String titulo  = db.getTituloJugador(uuid);
            String trabajo = db.obtenerTrabajoJugador(uuid);
            ChatColor colorTrabajo = obtenerColorTrabajo(trabajo);

            // Obtener datos de genero_jugador en una sola consulta
            Map<String, String> datos = db.obtenerDatosGeneroJugador(uuid);
            String nombreRol = limpiarCampo(datos.get("nombre_rol"));
            String apellidoP = limpiarCampo(datos.get("apellido_paterno_rol"));
            String apellidoM = limpiarCampo(datos.get("apellido_materno_rol"));
            String genero    = limpiarCampo(datos.get("genero"));
            String razaRol   = limpiarCampo(datos.get("raza_rol"));

            // Crear cabeza de jugador
            ItemStack cabeza = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta   = (SkullMeta) cabeza.getItemMeta();
            meta.setOwningPlayer(miembro);
            meta.setDisplayName(ChatColor.YELLOW + nombreRol);
            meta.setLore(List.of(
                    ChatColor.GRAY + "Titulo: "    + ChatColor.GOLD + rol,
                    ChatColor.GRAY + "Estatus: " + ChatColor.AQUA + titulo,
                    ChatColor.GRAY + "Nombre: " + ChatColor.WHITE + nombreRol,
                    ChatColor.GRAY + "Apellidos: " + ChatColor.WHITE + apellidoP + " " + apellidoM,
                    ChatColor.GRAY + "Género: " + ChatColor.YELLOW + genero,
                    ChatColor.GRAY + "Raza: " + ChatColor.GREEN + razaRol,
                    ChatColor.GRAY + "Trabajo: " + colorTrabajo + trabajo
            ));
            cabeza.setItemMeta(meta);

            menu.setItem(slot++, cabeza);
        }

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    /**
     * Limpia valores nulos, vacíos o con caracteres no deseados.
     */
    private String limpiarCampo(String valor) {
        if (valor == null || valor.trim().isEmpty() || valor.contains("<") || valor.equals(">")) {
            return "Desconocido";
        }
        return valor;
    }

    /**
     * Asigna un color dependiendo del trabajo del jugador.
     */
    private ChatColor obtenerColorTrabajo(String trabajo) {
        if (trabajo == null || trabajo.equalsIgnoreCase("Sin trabajo") || trabajo.isEmpty()) {
            return ChatColor.DARK_GRAY;
        }

        return switch (trabajo.toLowerCase()) {
            case "minero"     -> ChatColor.GRAY;
            case "herrero"    -> ChatColor.RED;
            case "alquimista" -> ChatColor.LIGHT_PURPLE;
            case "agricultor" -> ChatColor.GREEN;
            case "pescador"   -> ChatColor.BLUE;
            case "leñador"    -> ChatColor.DARK_GREEN;
            case "cazador"    -> ChatColor.GOLD;
            case "mago"       -> ChatColor.DARK_PURPLE;
            default           -> ChatColor.WHITE; // Trabajo desconocido
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player jugador)) return;
        String raw = e.getView().getTitle();
        String title = ChatColor.stripColor(raw);
        if (!title.startsWith("Miembros del Reino")) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        GestorBaseDeDatos db = plugin.getBaseDeDatos();
        String rolJugador = db.obtenerRolJugadorEnReino(jugador.getUniqueId());
        if (!(rolJugador.equalsIgnoreCase("Rey") ||
                rolJugador.equalsIgnoreCase("Reina") ||
                rolJugador.equalsIgnoreCase("Emperador") ||
                rolJugador.equalsIgnoreCase("Emperatriz"))) {

//            jugador.sendMessage(ChatColor.RED + "Solo el Rey, la Reina, el Emperador o la Emperatriz pueden gestionar miembros.");
            return;
        }

        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta != null && meta.getOwningPlayer() != null) {
            UUID miembroUUID = meta.getOwningPlayer().getUniqueId();
            new MenuGestionMiembroReino(plugin).abrir(jugador, miembroUUID);
        }
    }


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        String raw = e.getView().getTitle();
        String title = ChatColor.stripColor(raw);
        if (!title.startsWith("Miembros del Reino")) return;
        e.setCancelled(true);
    }
}
