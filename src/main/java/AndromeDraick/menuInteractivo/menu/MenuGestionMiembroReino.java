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
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

public class MenuGestionMiembroReino implements Listener {

    private final MenuInteractivo plugin;

    public MenuGestionMiembroReino(MenuInteractivo plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Abre el menú para gestionar un miembro específico del reino.
     */
    public void abrir(Player rey, UUID miembroUUID) {
        GestorBaseDeDatos db = plugin.getBaseDeDatos();
        String reino = db.obtenerReinoJugador(rey.getUniqueId());
        if (reino == null) {
            rey.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        Inventory menu = Bukkit.createInventory(
                null, 27,
                ChatColor.RED + "Gestionar miembro"
        );

        // Botones del menú
        menu.setItem(11, crearItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Cambiar Rol"));
        menu.setItem(12, crearItem(Material.REDSTONE_BLOCK, ChatColor.RED + "Degradar"));
        menu.setItem(13, crearItem(Material.BARRIER, ChatColor.DARK_RED + "Expulsar"));
        menu.setItem(15, crearItem(Material.DIAMOND_BLOCK, ChatColor.AQUA + "Cambiar Título"));

        // Metadata única por jugador
        String metaKey = "gestion_miembro_" + rey.getUniqueId();
        rey.setMetadata(metaKey, new FixedMetadataValue(plugin, miembroUUID.toString()));

        rey.openInventory(menu);
        rey.playSound(rey.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    private ItemStack crearItem(Material mat, String nombre) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nombre);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player jugador)) return;
        String title = ChatColor.stripColor(e.getView().getTitle());
        if (!title.equalsIgnoreCase("Gestionar miembro")) return;
        e.setCancelled(true);

        // Metadata única por jugador
        String metaKey = "gestion_miembro_" + jugador.getUniqueId();
        if (!jugador.hasMetadata(metaKey)) return;

        UUID miembroUUID = UUID.fromString(jugador.getMetadata(metaKey).get(0).asString());
        GestorBaseDeDatos db = plugin.getBaseDeDatos();

        switch (e.getSlot()) {
            case 11 -> { // Cambiar Rol
                new MenuCambiarRolReino(plugin, miembroUUID).abrir(jugador);
            }
            case 12 -> { // Degradar
                jugador.sendMessage(ChatColor.RED + "Proximamente");
            }
            case 13 -> { // Expulsar
                jugador.sendMessage(ChatColor.DARK_RED + "Acción Irreversible");
                boolean ok = db.expulsarMiembroReino(miembroUUID);
                if (ok) {
                    jugador.sendMessage(ChatColor.GREEN + "Miembro expulsado correctamente.");
                } else {
                    jugador.sendMessage(ChatColor.RED + "No se pudo expulsar al miembro.");
                }
                jugador.closeInventory();
            }
            case 15 -> { // Cambiar título
                new MenuCambiarTituloReino(plugin, miembroUUID).abrir(jugador);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Gestionar miembro")) {
            e.setCancelled(true);
        }
    }
}
