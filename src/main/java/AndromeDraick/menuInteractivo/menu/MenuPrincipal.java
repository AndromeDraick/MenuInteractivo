package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.configuracion.ConfigTiendaManager;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import AndromeDraick.menuInteractivo.utilidades.SistemaTrabajos;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class MenuPrincipal implements Listener {

    private final MenuInteractivo plugin;
    private static final String TITULO_BASE = ChatColor.DARK_GREEN + "Menú Interactivo";

    public MenuPrincipal(MenuInteractivo plugin) {
        this.plugin = plugin;
        // Al construirse, se registra como listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void abrir(Player jugador) {
        ConfigTiendaManager config = plugin.getConfigTienda();
        double dinero = plugin.getEconomia().getBalance(jugador);
        String titulo = TITULO_BASE + " §7($ " + FormateadorNumeros.formatear(dinero) + ")";
        Inventory menu = Bukkit.createInventory(null, 54, titulo);
        // == BORDES decorativos ==
        ItemStack borde = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta metaBorde = borde.getItemMeta();
        metaBorde.setDisplayName(" ");
        borde.setItemMeta(metaBorde);

        int[] slotsBorde = {
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 17,
                18, 19, 20, 22, 23, 24, 25, 26
        };
        for (int slot : slotsBorde) {
            menu.setItem(slot, borde);
        }

        // == Ítem de tienda ==
        ItemStack tienda = new ItemStack(Material.EMERALD);
        ItemMeta meta = tienda.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Tienda");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para abrir la tienda"));
        tienda.setItemMeta(meta);
        menu.setItem(21, tienda);

        // == Ítem de trabajos ==
        ItemStack trabajoItem = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta metaTrabajo = trabajoItem.getItemMeta();
        metaTrabajo.setDisplayName(ChatColor.YELLOW + "Unirte a un trabajo");
        List<String> loreTrabajo = new ArrayList<>();
        loreTrabajo.add(ChatColor.GRAY + "Haz clic para ver los trabajos disponibles.");
        loreTrabajo.add(ChatColor.GRAY + "¡Cada trabajo desbloquea ítems únicos!");
        metaTrabajo.setLore(loreTrabajo);
        trabajoItem.setItemMeta(metaTrabajo);
        menu.setItem(23, trabajoItem);

        // == Ítem de bancos (slot 22) ==
        if (jugador.hasPermission("bmi.comandos.aceptar.banco")) {
            ItemStack bancoItem = new ItemStack(Material.GOLD_INGOT);
            ItemMeta metaBanco = bancoItem.getItemMeta();
            metaBanco.setDisplayName(ChatColor.GOLD + "Gestión de Bancos");
            metaBanco.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para revisar solicitudes de bancos"));
            bancoItem.setItemMeta(metaBanco);
            menu.setItem(22, bancoItem);
        }

        // == Perfil del jugador ==
        ItemStack cabeza = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) cabeza.getItemMeta();
        skullMeta.setDisplayName(ChatColor.AQUA + "Tu Perfil");
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(jugador.getUniqueId()));

        // == Ítem de gestión del reino ==
        ItemStack reino = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta metaReino = reino.getItemMeta();
        metaReino.setDisplayName(ChatColor.GOLD + "Gestión del Reino");
        metaReino.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para administrar tu reino"));
        reino.setItemMeta(metaReino);
        menu.setItem(31, reino);

        // Grupo del jugador con LuckPerms
        String grupo = "Desconocido";
        LuckPerms lp = plugin.getPermisos();
        if (lp != null) {
            User user = lp.getUserManager().getUser(jugador.getUniqueId());
            if (user != null) {
                grupo = user.getPrimaryGroup();
            }
        }

        // Trabajo del jugador
        SistemaTrabajos sistema = plugin.getSistemaTrabajos();
        String trabajo = sistema.getTrabajo(jugador.getUniqueId());

        // Base de datos
        GestorBaseDeDatos db = plugin.getBaseDeDatos();

        // Rarezas desbloqueadas por grupo
        List<String> rarezas = config.getRarezasDesbloqueadas(grupo);
        String rarezasTexto = rarezas.isEmpty()
                ? "Ninguna"
                : String.join(", ", rarezas);

        // Ítems permitidos por el trabajo
        Set<String> itemsPermitidos = new HashSet<>();
        for (String itemName : config.getItemsCustom()) {
            Map<String, Object> datos = config.getDatosItemCustom(itemName);
            if (!datos.containsKey("trabajo")) continue;

            String trabajos = String.valueOf(datos.get("trabajo")).toLowerCase();
            for (String t : trabajos.split(",")) {
                if (t.trim().equalsIgnoreCase(trabajo)) {
                    itemsPermitidos.add(itemName.toUpperCase(Locale.ROOT));
                    break;
                }
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Jugador: " + ChatColor.YELLOW + jugador.getName());
        lore.add(ChatColor.GRAY + "Dinero: " + ChatColor.GREEN + "$" + FormateadorNumeros.formatear(dinero));
        lore.add(ChatColor.GRAY + "Grupo: " + ChatColor.LIGHT_PURPLE + grupo);
        lore.add(ChatColor.GRAY + "Trabajo: " + ChatColor.BLUE + trabajo);
        lore.add(ChatColor.GRAY + "Rarezas desbloqueadas: " + ChatColor.GOLD + rarezasTexto);
        lore.add(ChatColor.GRAY + "Ítems de trabajo: " + ChatColor.AQUA +
                (itemsPermitidos.isEmpty() ? "Ninguno" : String.join(", ", itemsPermitidos)));
        lore.add(ChatColor.DARK_GRAY + "UUID: " + jugador.getUniqueId().toString().substring(0, 8));
        lore.add("");
        lore.add(ChatColor.GOLD + "¡Gracias por jugar!");

        skullMeta.setLore(lore);
        cabeza.setItemMeta(skullMeta);
        menu.setItem(13, cabeza);

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Solo reaccionamos si el título coincide
        if (!event.getView().getTitle().startsWith(TITULO_BASE)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null ||
                event.getCurrentItem().getType() == Material.AIR) return;

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        switch (slot) {
            case 21:
                MenuTienda.abrir(jugador, 0);
                break;
            case 22:
                if (jugador.hasPermission("bmi.comandos.aceptar.banco")) {
                    plugin.getMenuBancos().abrirSolicitudes(jugador);
                } else {
                    jugador.sendMessage(ChatColor.RED + "No tienes permiso para gestionar bancos.");
                }
                 break;
            case 23:
                plugin.getMenuTrabajos().abrir(jugador);
                break;
            case 31:
                plugin.getMenuReino().abrir(jugador);
                break;
        }
    }
}
