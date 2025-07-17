package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.utilidades.SistemaTrabajos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuTrabajos {

    private static final String TITULO = ChatColor.GOLD + "Elige un trabajo";

    private static final Map<String, TrabajoVisual> TRABAJOS_PREDEFINIDOS = Map.of(
            "granjero", new TrabajoVisual(Material.WHEAT, ChatColor.GREEN),
            "minero", new TrabajoVisual(Material.IRON_PICKAXE, ChatColor.GRAY),
            "cazador", new TrabajoVisual(Material.BONE, ChatColor.RED),
            "herrero", new TrabajoVisual(Material.ANVIL, ChatColor.DARK_GRAY),
            "carpintero", new TrabajoVisual(Material.OAK_LOG, ChatColor.GOLD),
            "agricultor", new TrabajoVisual(Material.HAY_BLOCK, ChatColor.YELLOW),
            "alquimista", new TrabajoVisual(Material.BREWING_STAND, ChatColor.LIGHT_PURPLE),
            "guardia", new TrabajoVisual(Material.SHIELD, ChatColor.BLUE)
    );

    public static void abrir(Player jugador) {
        SistemaTrabajos sistema = MenuInteractivo.getInstancia().getSistemaTrabajos();
        Set<String> trabajos = sistema.getTrabajosValidos();

        int size = Math.max(27, ((trabajos.size() - 1) / 9 + 1) * 9);
        Inventory menu = Bukkit.createInventory(null, size, TITULO);

        int slot = 10;
        for (String trabajo : trabajos) {
            String key = trabajo.toLowerCase();
            TrabajoVisual visual = TRABAJOS_PREDEFINIDOS.getOrDefault(
                    key,
                    new TrabajoVisual(Material.BOOK, ChatColor.WHITE)
            );
            menu.setItem(slot, crearTrabajoItem(key, visual));
            slot = siguienteSlot(slot, size);
        }

        // Botón para volver al menú principal (slot 22 por defecto)
        ItemStack volver = new ItemStack(Material.BARRIER);
        ItemMeta metaVolver = volver.getItemMeta();
        metaVolver.setDisplayName(ChatColor.RED + "Volver al Menú Principal");
        metaVolver.setLore(List.of(ChatColor.GRAY + "Haz clic para regresar."));
        volver.setItemMeta(metaVolver);
        menu.setItem(22, volver);

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private static int siguienteSlot(int actual, int size) {
        actual++;
        if (actual % 9 == 0 || actual >= size) {
            actual += 2;
        }
        return actual;
    }

    private static ItemStack crearTrabajoItem(String nombre, TrabajoVisual visual) {
        ItemStack item = new ItemStack(visual.material());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(visual.color() + capitalizar(nombre));
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Haz clic para unirte como " + nombre.toLowerCase() + ".",
                ChatColor.YELLOW + "Desbloquea ítems y ventajas únicas."
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static void manejarClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO)) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String nombreClicado = ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();

        if (nombreClicado.equals("volver al menú principal")) {
            MenuPrincipal.abrir(jugador);
            jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        SistemaTrabajos sistema = MenuInteractivo.getInstancia().getSistemaTrabajos();
        GestorBaseDeDatos baseDeDatos = MenuInteractivo.getInstancia().getBaseDeDatos();

        if (!sistema.getTrabajosValidos().contains(capitalizar(nombreClicado))) {
            jugador.sendMessage(ChatColor.RED + "Trabajo desconocido.");
            return;
        }

        String actual = sistema.getTrabajo(jugador);
        if (!actual.equalsIgnoreCase("Sin trabajo") && !actual.equalsIgnoreCase("")) {
            jugador.sendMessage(ChatColor.RED + "Ya tienes un trabajo asignado (" + actual + "). No puedes cambiarlo.");
            return;
        }

        sistema.setTrabajo(jugador, nombreClicado);
        baseDeDatos.actualizarTrabajo(jugador.getUniqueId(), nombreClicado);

        jugador.sendMessage(ChatColor.GREEN + "¡Te has unido como " + capitalizar(nombreClicado) + "!");
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        jugador.closeInventory();
    }

    private static String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }

    private record TrabajoVisual(Material material, ChatColor color) {}
}
