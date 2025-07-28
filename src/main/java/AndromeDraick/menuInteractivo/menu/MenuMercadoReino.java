package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.model.ItemEnVenta;
import AndromeDraick.menuInteractivo.utilidades.CalculadoraPrecios;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import AndromeDraick.menuInteractivo.utilidades.SerializadorItemStack;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuMercadoReino implements Listener {

    private static final String TITULO_VENTA     = ChatColor.GOLD + "Poner a la venta";
    private static final String TITULO_MERCADO   = ChatColor.AQUA + "Mercado del Reino";
    private static final String TITULO_CUENTA    = ChatColor.GREEN + "Cuenta Personal";

    private static final int PRECIO_PORCENTAJE = 40;

    // Menú para seleccionar ítems del inventario personal y ponerlos en venta
    public static void abrirMenuVenta(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 54, TITULO_VENTA);
        FileConfiguration config = MenuInteractivo.getInstancia().getConfigTienda().getConfig();

        ItemStack[] inventario = jugador.getInventory().getContents();
        for (int i = 0; i < Math.min(inventario.length, 54); i++) {
            ItemStack item = inventario[i];
            if (item == null || item.getType() == Material.AIR) continue;

            Material material = item.getType();
            double precioBase = CalculadoraPrecios.calcularPrecioVenta(material, jugador);
            double precioMercado = precioBase * PRECIO_PORCENTAJE / 100.0;

            String nombreTraducido = material.name().toLowerCase().replace("_", " ");
            if (config.contains("items_custom." + material.name() + ".material")) {
                nombreTraducido = config.getString("items_custom." + material.name() + ".material");
            }

            ItemStack copia = item.clone();
            ItemMeta meta = copia.getItemMeta();
            List<String> lore = new ArrayList<>();

            if (precioMercado > 0) {
                meta.setDisplayName(ChatColor.GOLD + nombreTraducido);
                lore.add(ChatColor.GRAY + "Cantidad: " + item.getAmount());
                lore.add(ChatColor.BLUE + "Precio por unidad (" + PRECIO_PORCENTAJE + "%): " + FormateadorNumeros.formatear(precioMercado));
                lore.add(ChatColor.YELLOW + "Haz Click para poner en venta desde tu inventario.");
            } else {
                meta.setDisplayName(ChatColor.RED + "No vendible");
                lore.add(ChatColor.GRAY + "Este ítem no se puede vender");
            }

            meta.setLore(lore);
            copia.setItemMeta(meta);
            menu.setItem(i, copia);
        }

        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1.1f);
    }

    public static void manejarClickVenta(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_VENTA)) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        Material material = item.getType();
        double precioBase = CalculadoraPrecios.calcularPrecioVenta(material, jugador);
        double precioMercado = precioBase * PRECIO_PORCENTAJE / 100.0;

        if (precioMercado <= 0) {
            jugador.sendMessage(ChatColor.RED + "Este ítem no se puede poner a la venta.");
            return;
        }

        // Clonar y preparar el item a guardar
        ItemStack original = item.clone();
        int cantidad = original.getAmount();
        double precioTotal = precioMercado * cantidad;

        if (!jugador.getInventory().containsAtLeast(original, cantidad)) {
            jugador.sendMessage(ChatColor.RED + "Da clik en el ítem en tu inventario.");
            return;
        }

        // Obtener reino del jugador
        String etiquetaReino = MenuInteractivo.getInstancia().getBaseDeDatos().getReinoJugador(jugador.getUniqueId());
        if (etiquetaReino == null) {
            jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        // Serializar el ítem para guardarlo
        String itemSerializado = SerializadorItemStack.serializar(original);

        // Insertar en base de datos
        boolean exito = MenuInteractivo.getInstancia().getBaseDeDatos().insertarItemEnMercado(
                jugador.getUniqueId(), etiquetaReino, itemSerializado, cantidad, precioTotal
        );

        if (exito) {
            jugador.getInventory().removeItem(new ItemStack(material, cantidad));
            jugador.sendMessage(ChatColor.GREEN + "Has puesto en venta " + cantidad + "x " +
                    material.name().toLowerCase().replace("_", " ") +
                    " por " + FormateadorNumeros.formatear(precioTotal) + " en total.");
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            abrirMenuVenta(jugador); // refrescar menú
        } else {
            jugador.sendMessage(ChatColor.RED + "Ocurrió un error al registrar el ítem en el mercado.");
        }
    }

    public static void abrirMercadoDelReino(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 54, TITULO_MERCADO);

        String reino = MenuInteractivo.getInstancia().getBaseDeDatos().getReinoJugador(jugador.getUniqueId());
        if (reino == null) {
            jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        List<ItemEnVenta> items = MenuInteractivo.getInstancia().getBaseDeDatos().getItemsMercadoDelReino(reino);
        BancoManager bancoManager = MenuInteractivo.getInstancia().getBancoManager();
        String nombreMoneda = bancoManager.obtenerNombreMonedaDeReino(reino);
        if (nombreMoneda == null || nombreMoneda.isEmpty()) nombreMoneda = "monedas";

        for (int i = 0; i < Math.min(items.size(), 54); i++) {
            ItemEnVenta venta = items.get(i);
            ItemStack item = SerializadorItemStack.deserializar(venta.itemSerializado());
            if (item == null) continue;

            int cantidad = venta.cantidad();
            double precioTotal = venta.precio();
            double precioUnitario = precioTotal / Math.max(1, cantidad);

            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Cantidad: " + cantidad);
            lore.add(ChatColor.YELLOW + "Precio unitario: " + FormateadorNumeros.formatear(precioUnitario));
            lore.add(ChatColor.GOLD + "Total: " + FormateadorNumeros.formatear(precioTotal) + " " + nombreMoneda);
            lore.add(ChatColor.GRAY + "Click para comprar");
            lore.add(ChatColor.DARK_GRAY + "ID#" + venta.id());

            meta.setLore(lore);
            item.setItemMeta(meta);

            menu.setItem(i, item);
        }

        jugador.openInventory(menu);
    }

    public static void manejarClickCompra(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MERCADO)) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        // Extraer ID desde el lore
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            jugador.sendMessage(ChatColor.RED + "Este ítem no es válido.");
            return;
        }

        int id = -1;
        for (String line : meta.getLore()) {
            if (line.startsWith(ChatColor.DARK_GRAY + "ID#")) {
                try {
                    id = Integer.parseInt(line.replace(ChatColor.DARK_GRAY + "ID#", ""));
                } catch (NumberFormatException ignored) {}
                break;
            }
        }

        if (id == -1) {
            jugador.sendMessage(ChatColor.RED + "Error al identificar el ítem.");
            return;
        }

        // Buscar en base de datos por ID
        ItemEnVenta venta = MenuInteractivo.getInstancia().getBaseDeDatos().buscarItemEnMercadoPorID(id);
        if (venta == null) {
            jugador.sendMessage(ChatColor.RED + "Ese ítem ya no está disponible.");
            abrirMercadoDelReino(jugador);
            return;
        }

        double saldo = MenuInteractivo.getInstancia().getBaseDeDatos().obtenerSaldoCuentaPersonal(jugador.getUniqueId(), venta.reino());
        if (saldo < venta.precio()) {
            jugador.sendMessage(ChatColor.RED + "No tienes suficientes monedas del reino.");
            return;
        }

        boolean exito = MenuInteractivo.getInstancia().getBaseDeDatos().procesarCompraEnMercado(
                venta.id(), jugador.getUniqueId(), venta.precio()
        );

        if (exito) {
            jugador.getInventory().addItem(SerializadorItemStack.deserializar(venta.itemSerializado()));
            jugador.sendMessage(ChatColor.GREEN + "Has comprado el ítem correctamente.");
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.3f);
        } else {
            jugador.sendMessage(ChatColor.RED + "Error al procesar la compra.");
        }

        abrirMercadoDelReino(jugador); // refrescar
    }

    public static void abrirCuentaPersonal(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 27, TITULO_CUENTA);

        Map<String, Double> saldos = MenuInteractivo.getInstancia().getBaseDeDatos().obtenerTodosLosSaldosJugador(jugador.getUniqueId());
        BancoManager bancoManager = MenuInteractivo.getInstancia().getBancoManager();

        int slot = 0;
        for (Map.Entry<String, Double> entrada : saldos.entrySet()) {
            String reino = entrada.getKey();
            double cantidad = entrada.getValue();

            String nombreMoneda = bancoManager.obtenerNombreMonedaDeReino(reino);
            if (nombreMoneda == null || nombreMoneda.isEmpty()) nombreMoneda = "moneda";

            ItemStack moneda = new ItemStack(Material.GOLD_NUGGET);
            ItemMeta meta = moneda.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Reino: [" + reino.toUpperCase() + "]");
            meta.setLore(List.of(ChatColor.GRAY + "Saldo: $" + FormateadorNumeros.formatear(cantidad) + " " + nombreMoneda));
            moneda.setItemMeta(meta);

            menu.setItem(slot++, moneda);
            if (slot >= 27) break;
        }

        jugador.openInventory(menu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();
        if (titulo.equals(TITULO_VENTA)) {
            manejarClickVenta(event);
        } else if (titulo.equals(TITULO_MERCADO)) {
            manejarClickCompra(event);
        } else if (titulo.equals(TITULO_CUENTA)) {
            event.setCancelled(true); // Solo visual
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String titulo = event.getView().getTitle();
        if (titulo.equals(TITULO_VENTA) || titulo.equals(TITULO_MERCADO) || titulo.equals(TITULO_CUENTA)) {
            event.setCancelled(true);
        }
    }

}
