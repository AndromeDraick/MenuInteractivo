package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MenuEconomia {

    public static void abrir(Player jugador) {
        MenuInteractivo plugin = MenuInteractivo.getInstancia();
        GestorBaseDeDatos db = plugin.getGestorBaseDeDatos();

        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Economía del Reino");

        ItemStack imprimir = new ItemStack(Material.PAPER);
        ItemMeta metaImp = imprimir.getItemMeta();
        metaImp.setDisplayName(ChatColor.GREEN + "Imprimir Moneda");
        metaImp.setLore(Arrays.asList(ChatColor.GRAY + "Genera dinero del reino."));
        imprimir.setItemMeta(metaImp);
        menu.setItem(11, imprimir);

        ItemStack vender = new ItemStack(Material.EMERALD);
        ItemMeta metaVend = vender.getItemMeta();
        metaVend.setDisplayName(ChatColor.AQUA + "Vender Dinero");
        metaVend.setLore(Arrays.asList(ChatColor.GRAY + "Convierte tu dinero del servidor", ChatColor.GRAY + "en moneda del reino."));
        vender.setItemMeta(metaVend);
        menu.setItem(13, vender);

        ItemStack quemar = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta metaQuemar = quemar.getItemMeta();
        metaQuemar.setDisplayName(ChatColor.RED + "Quemar Moneda");
        metaQuemar.setLore(Arrays.asList(ChatColor.GRAY + "Reduce la cantidad de moneda", ChatColor.GRAY + "en circulación."));
        quemar.setItemMeta(metaQuemar);
        menu.setItem(15, quemar);

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    public static void manejarClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("Economía del Reino")) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        switch (slot) {
            case 11:
                jugador.performCommand("bmi mibanco imprimir 100");
                jugador.closeInventory();
                break;
            case 13:
                jugador.performCommand("bmi mibanco vender 100");
                jugador.closeInventory();
                break;
            case 15:
                jugador.performCommand("bmi mibanco quemar 100");
                jugador.closeInventory();
                break;
        }
    }
}
