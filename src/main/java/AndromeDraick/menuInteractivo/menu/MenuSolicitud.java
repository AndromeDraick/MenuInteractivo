package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.model.Banco;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class MenuSolicitud implements Listener {

    private static class SolicitudesHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private static final String TITULO_SOLICITUDES = ChatColor.DARK_GREEN + "Solicitudes de Bancos";
    private final BancoManager bancoManager;

    public MenuSolicitud(MenuInteractivo plugin) {
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void abrirSolicitudes(Player p) {
        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }
        List<Banco> pendientes = bancoManager.obtenerBancosPendientes(reino);
        if (pendientes.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay solicitudes pendientes.");
            return;
        }
        int size = ((pendientes.size() - 1) / 9 + 1) * 9;
        Inventory inv = Bukkit.createInventory(new SolicitudesHolder(), size, TITULO_SOLICITUDES);
        for (Banco b : pendientes) {
            ItemStack it = new ItemStack(Material.PAPER);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName(ChatColor.YELLOW + b.getEtiqueta());
            m.setLore(List.of(
                    ChatColor.GRAY  + b.getNombre(),
                    ChatColor.GREEN + "Izquierdo: aprobar",
                    ChatColor.RED   + "Derecho: rechazar"
            ));
            it.setItemMeta(m);
            inv.addItem(it);
        }
        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof SolicitudesHolder)) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta()) return;

        String tag = ChatColor.stripColor(it.getItemMeta().getDisplayName());
        if (e.isLeftClick()) {
            bancoManager.aprobarBanco(tag);
            p.sendMessage(ChatColor.GREEN + "Banco " + tag + " aprobado.");
        } else {
            bancoManager.rechazarBanco(tag);
            p.sendMessage(ChatColor.RED   + "Banco " + tag + " rechazado.");
        }
        p.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SolicitudesHolder) {
            e.setCancelled(true);
        }
    }
}
