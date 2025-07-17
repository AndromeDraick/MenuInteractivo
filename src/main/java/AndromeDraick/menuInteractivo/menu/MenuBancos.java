package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.model.Banco;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class MenuBancos implements Listener {

    private static final String TITULO = ChatColor.DARK_GREEN + "Solicitudes de Bancos";

    private final MenuInteractivo plugin;
    private final BancoManager bancoManager;

    public MenuBancos(MenuInteractivo plugin) {
        this.plugin = plugin;
        // Ahora BancoManager se construye con el GestorBaseDeDatos del plugin
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos());
    }

    /**
     * Abre el inventario con las solicitudes de bancos pendientes para el reino del jugador
     */
    public void abrirMenuSolicitudes(Player jugador) {
        String reino = bancoManager.obtenerReinoJugador(jugador.getUniqueId());
        if (reino == null) {
            jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        List<Banco> pendientes = bancoManager.obtenerBancosPendientes(reino);
        if (pendientes.isEmpty()) {
            jugador.sendMessage(ChatColor.YELLOW + "No hay solicitudes de bancos pendientes en tu reino.");
            return;
        }

        // Calculamos un tamaño múltiplo de 9
        int size = ((pendientes.size() - 1) / 9 + 1) * 9;
        Inventory menu = Bukkit.createInventory(null, size, TITULO);

        for (Banco b : pendientes) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + b.getEtiqueta() + " - " + b.getNombre());
            meta.setLore(List.of(
                    ChatColor.GRAY + "Propietario: " + b.getPropietarioUuid(),
                    ChatColor.GRAY + "Estado: " + b.getEstado(),
                    ChatColor.GREEN + "Click Izquierdo: Aprobar",
                    ChatColor.RED   + "Click Derecho: Rechazar"
            ));
            item.setItemMeta(meta);
            menu.addItem(item);
        }

        jugador.openInventory(menu);
    }

    /**
     * Maneja clics dentro del inventario de solicitudes
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String openedTitle = e.getView().getTitle();
        if (!openedTitle.equals(TITULO)) return;        e.setCancelled(true);
        Player jugador = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return;

        String display = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        // Formato: "etiqueta - nombre"
        String etiqueta = display.split(" - ")[0];

        if (e.isLeftClick()) {
            if (bancoManager.aprobarBanco(etiqueta)) {
                jugador.sendMessage(ChatColor.GREEN + "Banco " + etiqueta + " aprobado.");
            } else {
                jugador.sendMessage(ChatColor.RED + "Error al aprobar el banco " + etiqueta + ".");
            }
        } else if (e.isRightClick()) {
            if (bancoManager.rechazarBanco(etiqueta)) {
                jugador.sendMessage(ChatColor.RED + "Banco " + etiqueta + " rechazado.");
            } else {
                jugador.sendMessage(ChatColor.RED + "Error al rechazar el banco " + etiqueta + ".");
            }
        }

        jugador.closeInventory();
    }
}
