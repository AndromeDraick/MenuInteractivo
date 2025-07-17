package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.gestores.BancoManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class MenuBancos {

    private final MenuInteractivo plugin;
    private final BancoManager bancoManager;

    public MenuBancos(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.bancoManager = new BancoManager(plugin);
    }

    public void abrirMenu(Player jugador) {
        String reino = bancoManager.obtenerReinoDelJugador(jugador);
        if (reino == null) {
            jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        List<String> pendientes = bancoManager.obtenerBancosPendientes(reino);
        Inventory menu = Bukkit.createInventory(null, InventoryType.CHEST, ChatColor.DARK_GREEN + "Solicitudes de Bancos");

        for (String etiqueta : pendientes) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Banco: " + etiqueta);
            meta.setLore(List.of(
                    ChatColor.GRAY + "Click Izquierdo: Aceptar",
                    ChatColor.RED + "Click Derecho: Rechazar"
            ));
            item.setItemMeta(meta);
            menu.addItem(item);
        }

        jugador.openInventory(menu);
    }

    public void manejarClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getView().getTitle().contains("Solicitudes de Bancos")) {
            e.setCancelled(true);
            Player jugador = (Player) e.getWhoClicked();
            ItemStack item = e.getCurrentItem();
            if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return;

            String etiqueta = ChatColor.stripColor(item.getItemMeta().getDisplayName().replace("Banco: ", ""));
            if (e.getClick().isLeftClick()) {
                bancoManager.cambiarEstadoBanco(etiqueta, "aceptado");
                jugador.sendMessage(ChatColor.GREEN + "Has aceptado el banco: " + etiqueta);
            } else if (e.getClick().isRightClick()) {
                bancoManager.cambiarEstadoBanco(etiqueta, "rechazado");
                jugador.sendMessage(ChatColor.RED + "Has rechazado el banco: " + etiqueta);
            }
            jugador.closeInventory();
        }
    }
}
