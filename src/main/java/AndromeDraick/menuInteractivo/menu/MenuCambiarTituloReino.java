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

public class MenuCambiarTituloReino implements Listener {

    private final MenuInteractivo plugin;

    public MenuCambiarTituloReino(MenuInteractivo plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void abrir(Player administrador, UUID objetivo) {
        // Guardamos el UUID objetivo en metadata
        administrador.setMetadata("gestion_miembro_" + administrador.getUniqueId(), new FixedMetadataValue(plugin, objetivo.toString()));

        Inventory menu = Bukkit.createInventory(null, 9, ChatColor.GOLD + "Cambiar Estatus del ciudadano");

        menu.setItem(2, crearItem(Material.GOLDEN_HELMET, ChatColor.LIGHT_PURPLE + "Realeza"));
        menu.setItem(3, crearItem(Material.CHAINMAIL_CHESTPLATE, ChatColor.BLUE + "Nobleza"));
        menu.setItem(4, crearItem(Material.IRON_SWORD, ChatColor.GREEN + "Militar"));
        menu.setItem(5, crearItem(Material.ENCHANTING_TABLE, ChatColor.DARK_PURPLE + "Mago"));
        menu.setItem(6, crearItem(Material.LEATHER_BOOTS, ChatColor.GRAY + "Plebeyo"));

        administrador.openInventory(menu);
        administrador.playSound(administrador.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    private ItemStack crearItem(Material material, String nombre) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nombre);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player jugador)) return;

        String titulo = ChatColor.stripColor(e.getView().getTitle());
        if (!titulo.equalsIgnoreCase("Cambiar Estatus del ciudadano")) return;

        e.setCancelled(true);

        String metaKey = "gestion_miembro_" + jugador.getUniqueId();
        if (!jugador.hasMetadata(metaKey)) return;

        UUID objetivo = UUID.fromString(jugador.getMetadata(metaKey).get(0).asString());

        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String nuevoTitulo = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        GestorBaseDeDatos db = plugin.getBaseDeDatos();

        String tituloActual = db.getTituloJugador(objetivo);
        if (tituloActual != null && tituloActual.equalsIgnoreCase(nuevoTitulo)) {
            jugador.sendMessage(ChatColor.YELLOW + "Este jugador ya tiene este título.");
            return;
        }

        boolean actualizado = db.actualizarTituloJugador(objetivo, nuevoTitulo);

        if (actualizado) {
            jugador.sendMessage(ChatColor.GREEN + "Se ha cambiado el título a: " + ChatColor.YELLOW + nuevoTitulo);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        } else {
            jugador.sendMessage(ChatColor.RED + "No se pudo actualizar el título del jugador.");
        }

        jugador.closeInventory();
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        String titulo = ChatColor.stripColor(e.getView().getTitle());
        if (!titulo.equalsIgnoreCase("Cambiar Estatus del ciudadano")) return;
        e.setCancelled(true);
    }
}
