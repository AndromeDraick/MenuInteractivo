package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.utilidades.CalculadoraPrecios;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuConfirmacion {

    private static final String TITULO_CANTIDAD = ChatColor.DARK_GREEN + "Seleccionar cantidad";
    private static final String TITULO_CONFIRMAR = ChatColor.DARK_GREEN + "Confirmar compra";
    private static final Map<UUID, Material> comprasPendientes = new HashMap<>();
    private static final Map<UUID, Integer> cantidadesSeleccionadas = new HashMap<>();

    public static void abrirSelectorCantidad(Player jugador, Material material) {
        comprasPendientes.put(jugador.getUniqueId(), material);

        Inventory menu = Bukkit.createInventory(null, 27, TITULO_CANTIDAD);

        int[] cantidades = {1, 16, 32, 64};
        int[] slots = {10, 12, 14, 16};

        for (int i = 0; i < cantidades.length; i++) {
            int cantidad = cantidades[i];
            ItemStack item = new ItemStack(Material.GREEN_WOOL, cantidad);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Comprar x" + cantidad);

            double precioTotal = CalculadoraPrecios.calcularPrecioCompra(material, jugador) * cantidad;
            meta.setLore(List.of(ChatColor.GRAY + "Precio total: " + ChatColor.GREEN + "$" + FormateadorNumeros.formatear(precioTotal)));
            item.setItemMeta(meta);
            menu.setItem(slots[i], item);
        }

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    public static void abrirConfirmacion(Player jugador, Material material, int cantidad) {
        double precio = CalculadoraPrecios.calcularPrecioCompra(material, jugador) * cantidad;
        if (precio <= 0) {
            jugador.sendMessage(ChatColor.RED + "Error al obtener precio del ítem.");
            return;
        }

        cantidadesSeleccionadas.put(jugador.getUniqueId(), cantidad);

        Inventory menu = Bukkit.createInventory(null, 27, TITULO_CONFIRMAR);

        // Confirmar
        ItemStack confirmar = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta metaC = confirmar.getItemMeta();
        metaC.setDisplayName(ChatColor.GREEN + "Confirmar compra");
        metaC.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para confirmar."));
        confirmar.setItemMeta(metaC);
        menu.setItem(11, confirmar);

        // Cancelar
        ItemStack cancelar = new ItemStack(Material.RED_CONCRETE);
        ItemMeta metaX = cancelar.getItemMeta();
        metaX.setDisplayName(ChatColor.RED + "Cancelar");
        metaX.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para cancelar."));
        cancelar.setItemMeta(metaX);
        menu.setItem(15, cancelar);

        // Ítem en venta
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String nombreTraducido = material.name().toLowerCase().replace("_", " ");
        var configTienda = MenuInteractivo.getInstancia().getConfigTienda();
        var datos = configTienda.getDatosItemCustom(material.name());
        if (datos != null && datos.containsKey("material")) {
            nombreTraducido = String.valueOf(datos.get("material"));
        }

        meta.setDisplayName(ChatColor.GOLD + nombreTraducido);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Total: " + ChatColor.GREEN + "$" + FormateadorNumeros.formatear(precio)));
        item.setItemMeta(meta);
        menu.setItem(13, item);

        jugador.openInventory(menu);
    }

    public static void manejarClick(InventoryClickEvent event) {
        Player jugador = (Player) event.getWhoClicked();
        UUID uuid = jugador.getUniqueId();
        Material material = comprasPendientes.get(uuid);
        if (material == null) return;

        String titulo = event.getView().getTitle();
        event.setCancelled(true);

        if (titulo.equals(TITULO_CANTIDAD)) {
            int slot = event.getRawSlot();
            Map<Integer, Integer> mapaSlotCantidad = Map.of(10, 1, 12, 16, 14, 32, 16, 64);
            if (mapaSlotCantidad.containsKey(slot)) {
                int cantidad = mapaSlotCantidad.get(slot);
                abrirConfirmacion(jugador, material, cantidad);
            }
        }

        if (titulo.equals(TITULO_CONFIRMAR)) {
            int slot = event.getRawSlot();
            var economia = MenuInteractivo.getInstancia().getEconomia();
            int cantidad = cantidadesSeleccionadas.getOrDefault(uuid, 1);

            if (slot == 11) {
                double precio = CalculadoraPrecios.calcularPrecioCompra(material, jugador) * cantidad;
                if (!economia.has(jugador, precio)) {
                    jugador.sendMessage(ChatColor.RED + "No tienes suficiente dinero.");
                    jugador.closeInventory();
                    return;
                }

                var respuesta = economia.withdrawPlayer(jugador, precio);
                if (respuesta.transactionSuccess()) {
                    jugador.getInventory().addItem(new ItemStack(material, cantidad));
                    jugador.sendMessage(ChatColor.GREEN + "¡Compra exitosa de " + cantidad + " " + material.name() + " por $" + FormateadorNumeros.formatear(precio) + "!");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                } else {
                    jugador.sendMessage(ChatColor.RED + "Error al procesar la compra.");
                }

                comprasPendientes.remove(uuid);
                cantidadesSeleccionadas.remove(uuid);
                jugador.closeInventory();
            }

            if (slot == 15) {
                jugador.sendMessage(ChatColor.YELLOW + "Compra cancelada.");
                jugador.closeInventory();
                comprasPendientes.remove(uuid);
                cantidadesSeleccionadas.remove(uuid);

                Bukkit.getScheduler().runTaskLater(MenuInteractivo.getInstancia(), () -> {
                    int pagina = MenuTienda.getPagina(jugador);
                    MenuTienda.abrir(jugador, pagina);
                }, 2L);
            }
        }
    }
}
