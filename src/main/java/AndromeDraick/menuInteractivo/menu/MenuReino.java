package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MenuReino {

    public static void abrir(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Menú del Reino");

        ItemStack economia = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta metaEco = economia.getItemMeta();
        metaEco.setDisplayName(ChatColor.GOLD + "Economía del Reino");
        metaEco.setLore(Arrays.asList(ChatColor.GRAY + "Ver estadísticas económicas", ChatColor.GRAY + "y acceso a los bancos"));
        economia.setItemMeta(metaEco);
        menu.setItem(11, economia);

        ItemStack miembros = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta metaMiembros = miembros.getItemMeta();
        metaMiembros.setDisplayName(ChatColor.AQUA + "Miembros del Reino");
        metaMiembros.setLore(Arrays.asList(ChatColor.GRAY + "Ver quién pertenece a tu reino."));
        miembros.setItemMeta(metaMiembros);
        menu.setItem(13, miembros);

        ItemStack salir = new ItemStack(Material.BARRIER);
        ItemMeta metaSalir = salir.getItemMeta();
        metaSalir.setDisplayName(ChatColor.RED + "Salir del Reino");
        metaSalir.setLore(Arrays.asList(ChatColor.GRAY + "Abandonarás tu reino actual."));
        salir.setItemMeta(metaSalir);
        menu.setItem(15, salir);

        // Botón para volver al menú principal
        ItemStack volver = new ItemStack(Material.ARROW);
        ItemMeta metaVolver = volver.getItemMeta();
        metaVolver.setDisplayName(ChatColor.GRAY + "← Volver al Menú Principal");
        volver.setItemMeta(metaVolver);
        menu.setItem(26, volver);

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    public static void manejarClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("Menú del Reino")) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        MenuInteractivo plugin = MenuInteractivo.getInstancia();
        GestorBaseDeDatos db = plugin.getBaseDeDatos();

        switch (slot) {
            case 11:
                MenuEconomia.abrir(jugador);
                break;
            case 13:
                MenuMiembrosReino.abrir(jugador);
                break;
            case 15:
                String reino = db.obtenerReinoJugador(jugador.getUniqueId());
                if (reino == null) {
                    jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
                    return;
                }

                String rol = db.obtenerRolJugadorEnReino(jugador.getUniqueId());
                if (rol != null && rol.equalsIgnoreCase("rey")) {
                    jugador.sendMessage(ChatColor.RED + "No puedes abandonar el reino siendo el Rey. Transfiere el liderazgo o disuélvelo primero.");
                    return;
                }

                boolean exito = db.eliminarJugadorDeReino(jugador.getUniqueId());
                if (exito) {
                    jugador.sendMessage(ChatColor.GREEN + "Has abandonado el reino " + reino + ".");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f);
                } else {
                    jugador.sendMessage(ChatColor.RED + "Ocurrió un error al intentar abandonar el reino.");
                }
                break;
            case 26:
                MenuPrincipal.abrir(jugador);
                break;
        }
    }
}
