package AndromeDraick.menuInteractivo.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class ListenerSelectorCantidad implements Listener {

    @EventHandler
    public void alClicCantidad(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(MenuSelectorCantidad.TITULO)) return;

        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        var material = MenuSelectorCantidad.getMaterialPendiente(jugador.getUniqueId());
        if (material == null) return;

        Map<Integer, Integer> mapaSlotCantidad = Map.of(10, 1, 12, 16, 14, 32, 16, 64);
        int slot = event.getRawSlot();

        if (mapaSlotCantidad.containsKey(slot)) {
            int cantidad = mapaSlotCantidad.get(slot);
            MenuConfirmarCompra.abrir(jugador, material, cantidad);
        }
    }
}
