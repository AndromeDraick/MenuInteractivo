package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.utilidades.CalculadoraPrecios;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuConfirmacion {

    private static final String TITULO = ChatColor.DARK_GREEN + "Confirmar compra";
    private static final Map<UUID, Material> comprasPendientes = new HashMap<>();

    public static void abrir(Player jugador, Material material) {
        double precio = CalculadoraPrecios.calcularPrecioCompra(material, jugador);
        if (precio <= 0) {
            jugador.sendMessage(ChatColor.RED + "Error al obtener precio del ítem.");
            return;
        }

        comprasPendientes.put(jugador.getUniqueId(), material);

        Inventory menu = Bukkit.createInventory(null, 27, TITULO);

        // Botón Confirmar
        ItemStack confirmar = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta metaC = confirmar.getItemMeta();
        metaC.setDisplayName(ChatColor.GREEN + "Confirmar compra");
        metaC.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para confirmar."));
        confirmar.setItemMeta(metaC);
        menu.setItem(11, confirmar);

        // Botón Cancelar
        ItemStack cancelar = new ItemStack(Material.RED_CONCRETE);
        ItemMeta metaX = cancelar.getItemMeta();
        metaX.setDisplayName(ChatColor.RED + "Cancelar");
        metaX.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para cancelar."));
        cancelar.setItemMeta(metaX);
        menu.setItem(15, cancelar);

        // Ítem en venta (Slot central)
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String nombreTraducido = material.name().toLowerCase().replace("_", " ");
        var configTienda = MenuInteractivo.getInstancia().getConfigTienda();
        var datos = configTienda.getDatosItemCustom(material.name());
        if (datos != null && datos.containsKey("material")) {
            nombreTraducido = String.valueOf(datos.get("material"));
        }

        meta.setDisplayName(ChatColor.GOLD + nombreTraducido);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Precio: " + ChatColor.GREEN + "$" + FormateadorNumeros.formatear(precio)));
        item.setItemMeta(meta);
        menu.setItem(13, item);

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    public static void manejarClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO)) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        UUID uuid = jugador.getUniqueId();
        Material material = comprasPendientes.get(uuid);
        if (material == null) return;

        int slot = event.getRawSlot();
        var economia = MenuInteractivo.getInstancia().getEconomia();

        if (slot == 11) {
            // Confirmar compra
            double precio = CalculadoraPrecios.calcularPrecioCompra(material, jugador);
            if (!economia.has(jugador, precio)) {
                jugador.sendMessage(ChatColor.RED + "No tienes suficiente dinero.");
                jugador.closeInventory();
                return;
            }

            var respuesta = economia.withdrawPlayer(jugador, precio);
            if (respuesta.transactionSuccess()) {
                jugador.getInventory().addItem(new ItemStack(material, 1));
                jugador.sendMessage(ChatColor.GREEN + "¡Compra exitosa de " + material.name() + " por $" + FormateadorNumeros.formatear(precio) + "!");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            } else {
                jugador.sendMessage(ChatColor.RED + "Error al procesar la compra.");
            }

            comprasPendientes.remove(uuid);
            jugador.closeInventory();
        }

        if (slot == 15) {
            jugador.sendMessage(ChatColor.YELLOW + "Compra cancelada.");
            jugador.closeInventory();
            comprasPendientes.remove(uuid);

            Bukkit.getScheduler().runTaskLater(MenuInteractivo.getInstancia(), () -> {
                int pagina = MenuTienda.getPagina(jugador);
                MenuTienda.abrir(jugador, pagina);
            }, 2L);
        }
    }
}
