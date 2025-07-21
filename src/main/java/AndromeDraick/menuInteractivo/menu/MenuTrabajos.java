package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.utilidades.SistemaTrabajos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Menú de selección de trabajos.
 * - Dinámico en tamaño
 * - Usa la lista ordenada de SistemaTrabajos
 * - Maneja clics internamente
 */
public class MenuTrabajos implements Listener {
    private static final String TITULO = ChatColor.GOLD + "Elige un trabajo";
    private final MenuInteractivo plugin;
    private final SistemaTrabajos sistema;

    public MenuTrabajos(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.sistema = plugin.getSistemaTrabajos();
        // Registrar este listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /** Abre el inventario de trabajos para el jugador */
    public void abrir(Player jugador) {
        List<String> trabajos = sistema.getTrabajosValidos();
        // filas = ceil(trabajos.size()/9), al menos 1 fila
        int filas = Math.max(1, (trabajos.size() + 8) / 9);
        // dejamos siempre al menos 3 filas para estética; hasta un máximo de 6
        filas = Math.min(Math.max(filas, 3), 6);
        Inventory menu = Bukkit.createInventory(null, filas * 9, TITULO);

        // Rellenar con items de trabajo
        for (int i = 0; i < trabajos.size(); i++) {
            String nombre = trabajos.get(i);
            TrabajoVisual visual = TrabajoVisual.of(nombre);
            ItemStack item = new ItemStack(visual.material());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(visual.color() + nombre);
            meta.setLore(List.of(
                    ChatColor.GRAY + "Haz clic para unirte como " + nombre + ".",
                    ChatColor.YELLOW + "Desbloquea ventajas únicas."
            ));
            item.setItemMeta(meta);
            menu.setItem(i, item);
        }

        // Botón Volver al final (centro de la última fila)
        int volverSlot = filas * 9 - 5;
        ItemStack volver = new ItemStack(Material.BELL);
        ItemMeta mv = volver.getItemMeta();
        mv.setDisplayName(ChatColor.RED + "Volver al Menú Principal");
        volver.setItemMeta(mv);
        menu.setItem(volverSlot, volver);

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITULO)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String clicked = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        // Volver al menú principal
        if (clicked.equalsIgnoreCase("Volver al Menú Principal")) {
            plugin.getMenuPrincipal().abrir(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        // Intentar asignar trabajo
        String trabajo = clicked;
        // Primero validamos en memoria
        boolean valido = sistema.setTrabajo(p.getUniqueId(), trabajo);
        if (!valido) {
            p.sendMessage(ChatColor.RED + "Trabajo desconocido: " + trabajo);
            return;
        }
        // Luego persistimos
        boolean guardado = plugin.getBaseDeDatos().actualizarTrabajo(p.getUniqueId(), trabajo);
        if (!guardado) {
            p.sendMessage(ChatColor.RED + "No se pudo guardar tu trabajo. Inténtalo de nuevo más tarde.");
            return;
        }

        p.sendMessage(ChatColor.GREEN + "¡Asignado como " + trabajo + "!");
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        p.closeInventory();
    }

    /** Define el icono y color para cada trabajo. */
    private enum TrabajoVisual {
        GRANJERO   ("Granjero",   Material.WHEAT,           ChatColor.GREEN),
        MINERO     ("Minero",     Material.IRON_PICKAXE,    ChatColor.GRAY),
        CAZADOR    ("Cazador",    Material.BONE,            ChatColor.RED),
        HERRERO    ("Herrero",    Material.ANVIL,           ChatColor.DARK_GRAY),
        CARPINTERO ("Carpintero", Material.OAK_LOG,         ChatColor.GOLD),
        AGRICULTOR ("Agricultor", Material.HAY_BLOCK,       ChatColor.YELLOW),
        ALQUIMISTA ("Alquimista", Material.BREWING_STAND,   ChatColor.LIGHT_PURPLE),
        GUARDIA    ("Guardia",    Material.SHIELD,          ChatColor.BLUE),
        DEFAULT    ("",           Material.BOOK,            ChatColor.WHITE);

        private final String name;
        private final Material material;
        private final ChatColor color;
        TrabajoVisual(String name, Material mat, ChatColor col) {
            this.name = name;
            this.material = mat;
            this.color = col;
        }
        public Material material() { return material; }
        public ChatColor color()     { return color; }
        public static TrabajoVisual of(String clave) {
            for (TrabajoVisual tv : values()) {
                if (tv.name.equalsIgnoreCase(clave)) return tv;
            }
            return DEFAULT;
        }
    }
}
