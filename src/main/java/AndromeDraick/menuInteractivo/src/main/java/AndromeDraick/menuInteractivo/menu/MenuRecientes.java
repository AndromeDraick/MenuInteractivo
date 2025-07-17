package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.utilidades.CalculadoraPrecios;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import AndromeDraick.menuInteractivo.utilidades.HistorialComprasManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class MenuRecientes {

    public static void abrir(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Compras Recientes");

        List<Material> recientes = HistorialComprasManager.obtenerRecientes(jugador.getUniqueId());
        FileConfiguration config = MenuInteractivo.getInstancia().getConfigTienda().getConfig();

        int slot = 0;
        for (Material material : recientes) {
            if (slot >= menu.getSize()) break;

            double precio = CalculadoraPrecios.calcularPrecioCompra(material, jugador);
            String nombreBase = material.name().toLowerCase().replace("_", " ");

            // Si hay una traducción personalizada en el config, usarla
            if (config.contains("items_custom." + material.name() + ".nombre")) {
                nombreBase = ChatColor.translateAlternateColorCodes('&',
                        config.getString("items_custom." + material.name() + ".nombre"));
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(ChatColor.GOLD + nombreBase);
            meta.setLore(List.of(
                    ChatColor.GRAY + "Precio: " + ChatColor.GREEN + "$" + FormateadorNumeros.formatear(precio),
                    ChatColor.YELLOW + "Haz clic para volver a comprar"
            ));
            item.setItemMeta(meta);
            menu.setItem(slot++, item);
        }

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1.2f);
    }
}
