package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuHistoriaFamiliar implements Listener {

    private static final String TITULO = ChatColor.DARK_AQUA + "Historia Familiar";

    public static void abrir(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 54, TITULO);
        GestorBaseDeDatos db = MenuInteractivo.getInstancia().getBaseDeDatos();
        UUID uuid = jugador.getUniqueId();

        // Información principal del jugador
        String genero = db.getGenero(uuid);
        String nombreCompleto = db.getNombreCompletoRol(uuid);
        List<String> historia = db.getHistoriaFamiliar(uuid);

        // Cabeza del jugador con datos
        ItemStack cabezaJugador = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = cabezaJugador.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + jugador.getName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Nombre: " + ChatColor.AQUA + nombreCompleto);
        lore.add(ChatColor.GRAY + "Género: " + ChatColor.AQUA + genero);
        lore.add("");
        lore.add(ChatColor.DARK_PURPLE + "Relaciones familiares:");

        if (historia != null && !historia.isEmpty()) {
            for (String linea : historia) {
                lore.add(ChatColor.WHITE + "- " + linea);
            }
        } else {
            lore.add(ChatColor.GRAY + "Sin relaciones registradas.");
        }

        meta.setLore(lore);
        cabezaJugador.setItemMeta(meta);

        menu.setItem(22, cabezaJugador);

        // Botón cerrar
        ItemStack cerrar = new ItemStack(Material.BARRIER);
        ItemMeta metaCerrar = cerrar.getItemMeta();
        metaCerrar.setDisplayName(ChatColor.RED + "Cerrar");
        cerrar.setItemMeta(metaCerrar);
        menu.setItem(49, cerrar);

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_TOAST_IN, 1f, 1f);
    }

    @EventHandler
    public void alClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(TITULO)) {
            e.setCancelled(true);
            if (e.getRawSlot() == 49) {
                e.getWhoClicked().closeInventory();
            }
        }
    }

    @EventHandler
    public void alArrastrar(InventoryDragEvent e) {
        if (e.getView().getTitle().equals(TITULO)) {
            e.setCancelled(true);
        }
    }
}
