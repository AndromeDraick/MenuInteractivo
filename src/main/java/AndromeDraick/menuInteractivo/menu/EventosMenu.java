package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.utilidades.CalculadoraPrecios;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import AndromeDraick.menuInteractivo.utilidades.HistorialComprasManager;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class EventosMenu implements Listener {

    @EventHandler
    public void alClicEnInventario(InventoryClickEvent event) {
        Player jugador = (Player) event.getWhoClicked();
        String titulo = event.getView().getTitle();

        // Manejar clics en los distintos menús personalizados
        MenuTienda.manejarClick(event);
        MenuConfirmacion.manejarClick(event);
        MenuVentaVisual.manejarClick(event);

        // Menú de Compras Recientes
        if (titulo.equals(ChatColor.DARK_AQUA + "Compras Recientes")) {
            event.setCancelled(true);

            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            Material material = item.getType();
            double precio = CalculadoraPrecios.calcularPrecioCompra(material, jugador);

            if (precio <= 0) {
                jugador.sendMessage(ChatColor.RED + "No se pudo calcular el precio del ítem.");
                return;
            }

            if (!MenuInteractivo.getInstancia().getEconomia().has(jugador, precio)) {
                jugador.sendMessage(ChatColor.RED + "No tienes suficiente dinero.");
                return;
            }

            EconomyResponse respuesta = MenuInteractivo.getInstancia().getEconomia().withdrawPlayer(jugador, precio);
            if (respuesta.transactionSuccess()) {
                jugador.getInventory().addItem(new ItemStack(material, 1));

                // Traducción del nombre (material en config)
                FileConfiguration config = MenuInteractivo.getInstancia().getConfigTienda().getConfig();
                String nombreTraducido = material.name().toLowerCase().replace("_", " ");
                if (config.contains("items_custom." + material.name() + ".material")) {
                    nombreTraducido = config.getString("items_custom." + material.name() + ".material");
                }

                jugador.sendMessage(ChatColor.GREEN + "Recompraste 1 " + nombreTraducido + " por $" + FormateadorNumeros.formatear(precio));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.2f);
                HistorialComprasManager.registrarCompra(jugador.getUniqueId(), material);
            } else {
                jugador.sendMessage(ChatColor.RED + "Error: " + respuesta.errorMessage);
            }
        }
    }
}
