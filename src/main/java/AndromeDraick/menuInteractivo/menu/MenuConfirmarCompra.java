// MenuConfirmarCompra.java
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

public class MenuConfirmarCompra {

    public static final String TITULO = ChatColor.DARK_GREEN + "Confirmar compra";
    private static final Map<UUID, Integer> cantidadesSeleccionadas = new HashMap<>();

    public static void abrir(Player jugador, Material material, int cantidad) {
        double precio = CalculadoraPrecios.calcularPrecioCompra(material, jugador) * cantidad;
        if (precio <= 0) {
            jugador.sendMessage(ChatColor.RED + "Error al obtener precio del ítem.");
            return;
        }

        cantidadesSeleccionadas.put(jugador.getUniqueId(), cantidad);

        Inventory menu = Bukkit.createInventory(null, 27, TITULO);

        // Confirmar
        ItemStack confirmar = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta metaC = confirmar.getItemMeta();
        metaC.setDisplayName(ChatColor.GREEN + "Confirmar compra");
        metaC.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para confirmar."));
        confirmar.setItemMeta(metaC);
        menu.setItem(11, confirmar);

        // Cancelar
        ItemStack cancelar = new ItemStack(Material.RED_CONCRETE);
        ItemMeta metaX = cancelar.getItemMeta();
        metaX.setDisplayName(ChatColor.RED + "Cancelar");
        metaX.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para cancelar."));
        cancelar.setItemMeta(metaX);
        menu.setItem(15, cancelar);

        // Ítem en venta
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String nombreTraducido = material.name().toLowerCase().replace("_", " ");
        var configTienda = MenuInteractivo.getInstancia().getConfigTienda();
        var datos = configTienda.getDatosItemCustom(material.name());
        if (datos != null && datos.containsKey("material")) {
            nombreTraducido = String.valueOf(datos.get("material"));
        }

        meta.setDisplayName(ChatColor.GOLD + nombreTraducido);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Total: " + ChatColor.GREEN + "$" + FormateadorNumeros.formatear(precio)));
        item.setItemMeta(meta);
        menu.setItem(13, item);

        jugador.openInventory(menu);
    }

    public static int getCantidad(UUID uuid) {
        return cantidadesSeleccionadas.getOrDefault(uuid, 1);
    }

    public static void cancelarCompra(UUID uuid) {
        cantidadesSeleccionadas.remove(uuid);
    }
}
