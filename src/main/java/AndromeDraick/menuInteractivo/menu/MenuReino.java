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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MenuReino implements Listener {

    private static final String TITULO = ChatColor.DARK_PURPLE + "Menú del Reino";
    private final MenuInteractivo plugin;
    private final GestorBaseDeDatos db;

    public MenuReino(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.db = plugin.getBaseDeDatos();
        // Registramos este listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /** 1) Abre el inventario del reino */
    public void abrir(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 27, TITULO);

        // Botón Economía
        ItemStack economia = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta me = economia.getItemMeta();
        me.setDisplayName(ChatColor.GOLD + "Economía del Reino");
        me.setLore(Arrays.asList(
                ChatColor.GRAY + "Ver estadísticas económicas",
                ChatColor.GRAY + "y acceso a los bancos"
        ));
        economia.setItemMeta(me);
        menu.setItem(11, economia);

        // Botón Miembros
        ItemStack miembros = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta mm = miembros.getItemMeta();
        mm.setDisplayName(ChatColor.AQUA + "Miembros del Reino");
        mm.setLore(Arrays.asList(ChatColor.GRAY + "Ver quién pertenece a tu reino."));
        miembros.setItemMeta(mm);
        menu.setItem(13, miembros);

        // Botón Salir
        ItemStack salir = new ItemStack(Material.BARRIER);
        ItemMeta ms = salir.getItemMeta();
        ms.setDisplayName(ChatColor.RED + "Salir del Reino");
        ms.setLore(Arrays.asList(ChatColor.GRAY + "Abandonarás tu reino actual."));
        salir.setItemMeta(ms);
        menu.setItem(15, salir);

        // Botón Volver
        ItemStack volver = new ItemStack(Material.ARROW);
        ItemMeta mv = volver.getItemMeta();
        mv.setDisplayName(ChatColor.GRAY + "← Volver al Menú Principal");
        volver.setItemMeta(mv);
        menu.setItem(26, volver);

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    /** 2) Maneja los clics en el inventario */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO)) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        switch (slot) {
            case 11 -> MenuEconomia.abrir(jugador);
            case 13 -> MenuMiembrosReino.abrir(jugador);
            case 15 -> {
                String reino = db.obtenerReinoJugador(jugador.getUniqueId());
                if (reino == null) {
                    jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
                    return;
                }
                String rol = db.obtenerRolJugadorEnReino(jugador.getUniqueId());
                if ("rey".equalsIgnoreCase(rol)) {
                    jugador.sendMessage(ChatColor.RED +
                            "No puedes abandonar el reino siendo el Rey. Transfiere el liderazgo primero.");
                    return;
                }
                if (db.eliminarJugadorDeReino(jugador.getUniqueId())) {
                    jugador.sendMessage(ChatColor.GREEN + "Has abandonado el reino " + reino + ".");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f);
                } else {
                    jugador.sendMessage(ChatColor.RED +
                            "Ocurrió un error al intentar abandonar el reino.");
                }
            }
            case 26 -> MenuPrincipal.abrir(jugador);
        }
    }
}
