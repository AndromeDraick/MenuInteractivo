package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.utilidades.CalculadoraPrecios;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuSelectorCantidad {

    public static final String TITULO = ChatColor.DARK_GREEN + "Seleccionar cantidad";
    private static final Map<UUID, Material> comprasPendientes = new HashMap<>();

    public static void abrir(Player jugador, Material material) {
        comprasPendientes.put(jugador.getUniqueId(), material);

        Inventory menu = Bukkit.createInventory(null, 27, TITULO);

        int[] cantidades = {1, 16, 32, 64};
        int[] slots = {10, 12, 14, 16};

        for (int i = 0; i < cantidades.length; i++) {
            int cantidad = cantidades[i];
            ItemStack item = new ItemStack(Material.GREEN_WOOL, cantidad);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Comprar x" + cantidad);

            double precioTotal = CalculadoraPrecios.calcularPrecioCompra(material, jugador) * cantidad;
            meta.setLore(List.of(ChatColor.GRAY + "Precio total: " + ChatColor.GREEN + "$" + FormateadorNumeros.formatear(precioTotal)));
            item.setItemMeta(meta);
            menu.setItem(slots[i], item);
        }

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    public static Material getMaterialPendiente(UUID uuid) {
        return comprasPendientes.get(uuid);
    }

    public static void cancelarCompra(UUID uuid) {
        comprasPendientes.remove(uuid);
    }
}
