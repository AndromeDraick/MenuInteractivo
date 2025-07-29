package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.utilidades.CalculadoraPrecios;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ListenerConfirmarCompra implements Listener {

    @EventHandler
    public void alClicConfirmacion(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(MenuConfirmarCompra.TITULO)) return;

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }

        Player jugador = (Player) event.getWhoClicked();
        var uuid = jugador.getUniqueId();
        var material = MenuSelectorCantidad.getMaterialPendiente(uuid);
        var cantidad = MenuConfirmarCompra.getCantidad(uuid);

        if (material == null || cantidad <= 0) return;

        int slot = event.getRawSlot();
        var economia = MenuInteractivo.getInstancia().getEconomia();

        if (slot == 11) {
            double precio = CalculadoraPrecios.calcularPrecioCompra(material, jugador) * cantidad;
            if (!economia.has(jugador, precio)) {
                jugador.sendMessage(ChatColor.RED + "No tienes suficiente dinero.");
                jugador.closeInventory();
                return;
            }

            var respuesta = economia.withdrawPlayer(jugador, precio);
            if (respuesta.transactionSuccess()) {
                jugador.getInventory().addItem(new ItemStack(material, cantidad));
                jugador.sendMessage(ChatColor.GREEN + "¡Compra exitosa de " + cantidad + " " + material.name() + " por $" + FormateadorNumeros.formatear(precio) + "!");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            } else {
                jugador.sendMessage(ChatColor.RED + "Error al procesar la compra.");
            }

            MenuSelectorCantidad.cancelarCompra(uuid);
            MenuConfirmarCompra.cancelarCompra(uuid);
            jugador.closeInventory();
        }

        if (slot == 15) {
            jugador.sendMessage(ChatColor.YELLOW + "Compra cancelada.");
            jugador.closeInventory();
            MenuSelectorCantidad.cancelarCompra(uuid);
            MenuConfirmarCompra.cancelarCompra(uuid);

            Bukkit.getScheduler().runTaskLater(MenuInteractivo.getInstancia(), () -> {
                int pagina = MenuTienda.getPagina(jugador);
                MenuTienda.abrir(jugador, pagina);
            }, 2L);
        }
    }
}