package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class MenuCambiarRolReino implements Listener {

    private final MenuInteractivo plugin;
    private final Map<Integer, String> opcionesRol = new LinkedHashMap<>();

    public MenuCambiarRolReino(MenuInteractivo plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void abrir(Player ejecutor, UUID uuidObjetivo) {
        GestorBaseDeDatos db = plugin.getBaseDeDatos();
        String genero = db.getGenero(uuidObjetivo);
        if (genero == null) genero = "Desconocido";

        // Guardamos temporalmente el UUID del objetivo en metadata
        ejecutor.setMetadata("gestion_miembro_" + ejecutor.getUniqueId(), new org.bukkit.metadata.FixedMetadataValue(plugin, uuidObjetivo.toString()));

        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Cambiar Titulo del ciudadano");

        int slot = 0;

        // Roles principales
        agregarOpcion(menu, slot++, "Emperador", "Emperatriz", genero, Material.END_CRYSTAL);
        agregarOpcion(menu, slot++, "Rey", "Reina", genero, Material.GOLD_BLOCK);
        agregarOpcion(menu, slot++, "Príncipe", "Princesa", genero, Material.PURPLE_WOOL);

        // Nobleza
        agregarOpcion(menu, slot++, "Archiduque", "Archiduquesa", genero, Material.REDSTONE_BLOCK);
        agregarOpcion(menu, slot++, "Gran Duque", "Gran Duquesa", genero, Material.EMERALD_BLOCK);
        agregarOpcion(menu, slot++, "Duque", "Duquesa", genero, Material.DIAMOND_BLOCK);
        agregarOpcion(menu, slot++, "Marqués", "Marquesa", genero, Material.COPPER_BLOCK);
        agregarOpcion(menu, slot++, "Conde", "Condesa", genero, Material.IRON_BLOCK);
        agregarOpcion(menu, slot++, "Vizconde", "Vizcondesa", genero, Material.QUARTZ_BLOCK);
        agregarOpcion(menu, slot++, "Barón", "Baronesa", genero, Material.STONE_BRICKS);
        agregarOpcion(menu, slot++, "Señor", "Señora", genero, Material.BOOK);

        // Militares y caballería
        agregarOpcion(menu, slot++, "Caballero", "Dama", genero, Material.SHIELD);
        agregarOpcion(menu, slot++, "Capitán", "Capitana", genero, Material.PURPLE_WOOL);
        agregarOpcion(menu, slot++, "Teniente", "Teniente", genero, Material.PURPLE_WOOL);
        agregarOpcion(menu, slot++, "Escudero", "Escudera", genero, Material.LEATHER_CHESTPLATE);
        agregarOpcion(menu, slot++, "Arquero", "Arquera", genero, Material.LEATHER_CHESTPLATE);
        agregarOpcion(menu, slot++, "Miliciano", "Miliciana", genero, Material.LEATHER_CHESTPLATE);

        // Otros
        agregarOpcion(menu, slot++, "Heraldo", "Heralda", genero, Material.GOLD_NUGGET);
        agregarOpcion(menu, slot++, "Trompetero", "Trompetera", genero, Material.GOLD_NUGGET);
        agregarOpcion(menu, slot++, "Sanador", "Sanadora", genero, Material.GOLD_NUGGET);
        agregarOpcion(menu, slot++, "Ingeniero", "Ingeniera", genero, Material.GOLD_NUGGET);

        // Población general
        agregarOpcion(menu, slot++, "Burgués", "Burguesa", genero, Material.GOLD_NUGGET);
        agregarOpcion(menu, slot++, "Campesino", "Campesina", genero, Material.WHEAT);
        agregarOpcion(menu, slot++, "Siervo", "Sierva", genero, Material.CHAIN);
//      agregarOpcion(menu, slot++, "Esclavo", "Esclava", genero, Material.CHAIN_COMMAND_BLOCK);
        agregarOpcion(menu, slot++, "Obrero", "Obrera", genero, Material.BRICKS);
        agregarOpcion(menu, slot++, "Artesano", "Artesana", genero, Material.CRAFTING_TABLE);

        ejecutor.openInventory(menu);
        ejecutor.playSound(ejecutor.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.1f);
    }

    private void agregarOpcion(Inventory menu, int slot, String rolM, String rolF, String genero, Material material) {
        String rolFinal = genero.equalsIgnoreCase("Femenino") ? rolF : rolM;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + rolFinal);
        item.setItemMeta(meta);
        menu.setItem(slot, item);
        opcionesRol.put(slot, rolFinal);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player jugador)) return;

        String title = ChatColor.stripColor(e.getView().getTitle());
        if (!title.equalsIgnoreCase("Cambiar Titulo del ciudadano")) return;

        e.setCancelled(true);

        String metaKey = "gestion_miembro_" + jugador.getUniqueId();
        if (!jugador.hasMetadata(metaKey)) return;

        UUID uuidObjetivo = UUID.fromString(jugador.getMetadata(metaKey).get(0).asString());

        int slot = e.getRawSlot();
        if (!opcionesRol.containsKey(slot)) return;

        String nuevoRol = opcionesRol.get(slot);
        GestorBaseDeDatos db = plugin.getBaseDeDatos();

        String rolActual = db.obtenerRolJugadorEnReino(uuidObjetivo);
        if (rolActual != null && rolActual.equalsIgnoreCase(nuevoRol)) {
            jugador.sendMessage(ChatColor.YELLOW + "Este jugador ya tiene ese rol.");
            return;
        }

        boolean ok = db.actualizarRolJugador(uuidObjetivo, nuevoRol);

        if (ok) {
            jugador.sendMessage(ChatColor.GREEN + "Has asignado el rol: " + ChatColor.YELLOW + nuevoRol);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.0f);
        } else {
            jugador.sendMessage(ChatColor.RED + "No se pudo actualizar el rol.");
        }

        jugador.closeInventory();
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        String title = ChatColor.stripColor(e.getView().getTitle());
        if (title.equalsIgnoreCase("Cambiar Titulo del ciudadano")) e.setCancelled(true);
    }
}
