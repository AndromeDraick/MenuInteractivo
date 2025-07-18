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

public class MenuLista implements Listener {

    private static class ListaHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private static final String TITULO_LISTA = ChatColor.DARK_AQUA + "Bancos del Reino ";
    private final BancoManager bancoManager;
    private final MenuBancos menuBancos;

    public MenuLista(MenuInteractivo plugin, MenuBancos menuBancos) {
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos());
        this.menuBancos   = menuBancos;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void abrirListaActivos(Player p) {
        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }
        List<Banco> activos = bancoManager.obtenerBancosDeReino(reino);
        if (activos.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay bancos activos en tu reino.");
            return;
        }
        int size = ((activos.size() - 1) / 9 + 1) * 9;
        Inventory inv = Bukkit.createInventory(new ListaHolder(), size, TITULO_LISTA + reino);
        for (Banco b : activos) {
            ItemStack it = new ItemStack(Material.BOOK);
            ItemMeta m  = it.getItemMeta();
            m.setDisplayName(ChatColor.YELLOW + b.getEtiqueta());
            m.setLore(List.of(
                    ChatColor.GRAY  + b.getNombre(),
                    ChatColor.GREEN + "Haz clic para ver detalles"
            ));
            it.setItemMeta(m);
            inv.addItem(it);
        }
        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ListaHolder)) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta()) return;

        String tag = ChatColor.stripColor(it.getItemMeta().getDisplayName());
        menuBancos.abrirIndividual(p, tag);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof ListaHolder) {
            e.setCancelled(true);
        }
    }
}
