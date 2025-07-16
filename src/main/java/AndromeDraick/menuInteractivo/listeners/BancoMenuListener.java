package AndromeDraick.menuInteractivo.listeners;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.menu.MenuBancos;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class BancoMenuListener implements Listener {

    private final MenuBancos menuBancos;

    public BancoMenuListener(MenuInteractivo plugin) {
        this.menuBancos = new MenuBancos(plugin);
    }

    @EventHandler
    public void alClicInventario(InventoryClickEvent e) {
        menuBancos.manejarClick(e);
    }
}
