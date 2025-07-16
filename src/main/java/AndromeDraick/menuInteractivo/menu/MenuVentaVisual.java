package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.utilidades.CalculadoraPrecios;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuVentaVisual {

    private static final String TITULO = ChatColor.DARK_RED + "Vender Ítems";

    public static void abrir(Player jugador) {
        Inventory venta = Bukkit.createInventory(null, 54, TITULO);
        FileConfiguration config = MenuInteractivo.getInstancia().getConfigTienda().getConfig();

        ItemStack[] inventario = jugador.getInventory().getContents();
        for (int i = 0; i < Math.min(inventario.length, 54); i++) {
            ItemStack item = inventario[i];
            if (item == null || item.getType() == Material.AIR) continue;

            Material material = item.getType();
            double precio = CalculadoraPrecios.calcularPrecioVenta(material, jugador);

            String nombreTraducido = material.name().toLowerCase().replace("_", " ");
            if (config.contains("items_custom." + material.name() + ".material")) {
                nombreTraducido = config.getString("items_custom." + material.name() + ".material");
            }

            ItemStack copia = item.clone();
            ItemMeta meta = copia.getItemMeta();
            List<String> lore = new ArrayList<>();

            if (precio > 0) {
                meta.setDisplayName(ChatColor.GOLD + nombreTraducido);
                lore.add(ChatColor.GRAY + "Cantidad: " + item.getAmount());
                lore.add(ChatColor.GREEN + "Precio por unidad: $" + FormateadorNumeros.formatear(precio));
                lore.add(ChatColor.YELLOW + "Click para vender");
            } else {
                meta.setDisplayName(ChatColor.RED + "No vendible");
                lore.add(ChatColor.GRAY + "Este ítem no se puede vender");
            }

            meta.setLore(lore);
            copia.setItemMeta(meta);
            venta.setItem(i, copia);
        }

        jugador.openInventory(venta);
        jugador.playSound(jugador.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1.1f);
    }

    public static void manejarClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO)) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        Material material = item.getType();
        double precio = CalculadoraPrecios.calcularPrecioVenta(material, jugador);
        if (precio <= 0) {
            jugador.sendMessage(ChatColor.RED + "Este ítem no se puede vender.");
            return;
        }

        int cantidad = item.getAmount();
        double total = precio * cantidad;

        jugador.getInventory().removeItem(new ItemStack(material, cantidad));
        MenuInteractivo.getInstancia().getEconomia().depositPlayer(jugador, total);

        String nombreTraducido = material.name().toLowerCase().replace("_", " ");
        FileConfiguration config = MenuInteractivo.getInstancia().getConfigTienda().getConfig();
        if (config.contains("items_custom." + material.name() + ".material")) {
            nombreTraducido = config.getString("items_custom." + material.name() + ".material");
        }

        jugador.sendMessage(ChatColor.GREEN + "Vendiste " + cantidad + " de " + nombreTraducido +
                " por $" + FormateadorNumeros.formatear(total));
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        abrir(jugador); // refresca el menú
    }
}
