package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuRegistroRol {

    private static final String TITULO = ChatColor.DARK_AQUA + "Registro Inicial de Rol";
    private static final Map<UUID, String> generosSeleccionados = new HashMap<>();

    public static void abrir(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 27, TITULO);

        menu.setItem(10, crearOpcion(Material.BLUE_CONCRETE, "Masculino"));
        menu.setItem(13, crearOpcion(Material.PINK_CONCRETE, "Femenino"));
        menu.setItem(16, crearOpcion(Material.LIME_CONCRETE, "Otro"));

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private static ItemStack crearOpcion(Material material, String nombre) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + nombre);
        meta.setLore(java.util.List.of(ChatColor.GRAY + "Haz clic para seleccionar"));
        item.setItemMeta(meta);
        return item;
    }

    public static void manejarClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO)) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        UUID uuid = jugador.getUniqueId();

        int slot = event.getRawSlot();
        String genero = switch (slot) {
            case 10 -> "Masculino";
            case 13 -> "Femenino";
            case 16 -> "Otro";
            default -> null;
        };

        if (genero != null) {
            generosSeleccionados.put(uuid, genero);
            jugador.sendMessage(ChatColor.GREEN + "Has seleccionado el género: " + ChatColor.AQUA + genero);
            jugador.closeInventory();
            Bukkit.getScheduler().runTaskLater(MenuInteractivo.getInstancia(), () -> {
//                MenuDatosRol.abrir(jugador, genero); <-----NO DISPONIBLE POR AHORA.
            }, 10L);
        }
    }
}
