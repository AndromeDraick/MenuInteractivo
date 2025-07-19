package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public class MenuMiembrosReino {

    public static void abrir(Player jugador) {
        MenuInteractivo plugin = MenuInteractivo.getInstancia();
        GestorBaseDeDatos db  = plugin.getBaseDeDatos();

        String reino = db.obtenerReinoJugador(jugador.getUniqueId());
        if (reino == null) {
            jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        List<UUID> miembros = db.obtenerMiembrosDeReino(reino);
        if (miembros.isEmpty()) {
            jugador.sendMessage(ChatColor.RED + "Tu reino no tiene miembros registrados.");
            return;
        }

        Inventory menu = Bukkit.createInventory(
                null, 36, ChatColor.AQUA + "Miembros del Reino " + reino
        );

        int slot = 0;
        for (UUID uuid : miembros) {
            OfflinePlayer miembro = Bukkit.getOfflinePlayer(uuid);
            String rol    = db.obtenerRolJugadorEnReino(uuid);   // "Rey"/"Reina"/"miembro"
            String titulo = db.getTituloJugador(uuid);           // "realeza", "nobleza", etc.

            ItemStack cabeza = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta   = (SkullMeta) cabeza.getItemMeta();
            meta.setOwningPlayer(miembro);
            meta.setDisplayName(ChatColor.YELLOW + miembro.getName());
            meta.setLore(List.of(
                    ChatColor.GRAY + "Rol: "    + ChatColor.GOLD + rol,
                    ChatColor.GRAY + "Título: " + ChatColor.AQUA + titulo
            ));
            cabeza.setItemMeta(meta);

            menu.setItem(slot++, cabeza);
        }

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }
    
}
