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

    private static final String TITULO_VENTA   = ChatColor.GOLD + "Poner a la venta";
    private static final String TITULO_MERCADO = ChatColor.AQUA + "Mercado del Reino";
    private static final String TITULO_CUENTA  = ChatColor.GREEN + "Cuenta Personal";

    // -------------------------------
    // Abrir menú de venta
    // -------------------------------
    public static void abrirMenuVenta(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 54, TITULO_VENTA);
        FileConfiguration config = MenuInteractivo.getInstancia().getConfigTienda().getConfig();

        String etiquetaReino = MenuInteractivo.getInstancia().getBaseDeDatos().getReinoJugador(jugador.getUniqueId());
        if (etiquetaReino == null) {
            jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        BancoManager bancoManager = MenuInteractivo.getInstancia().getBancoManager();
        double valorMonedaServidor = bancoManager.calcularValorMonedaReino(etiquetaReino);
        if (valorMonedaServidor <= 0) valorMonedaServidor = 0.01;

        String nombreMoneda = bancoManager.obtenerNombreMonedaDeReino(etiquetaReino);
        if (nombreMoneda == null || nombreMoneda.isEmpty()) nombreMoneda = "Moneda";

        ItemStack[] inventario = jugador.getInventory().getContents();
        for (int i = 0; i < Math.min(inventario.length, 54); i++) {
            ItemStack item = inventario[i];
            if (item == null || item.getType() == Material.AIR) continue;

            Material material = item.getType();
            double precioServidorUnit = CalculadoraPrecios.calcularPrecioVenta(material, jugador);
            double precioReinoUnit = precioServidorUnit / valorMonedaServidor;

            String nombreTraducido = material.name().toLowerCase().replace("_", " ");
            if (config.contains("items_custom." + material.name() + ".material")) {
                nombreTraducido = config.getString("items_custom." + material.name() + ".material");
            }

            ItemStack copia = item.clone();
            ItemMeta meta = copia.getItemMeta();
            List<String> lore = new ArrayList<>();

            if (precioReinoUnit > 0) {
                meta.setDisplayName(ChatColor.GOLD + nombreTraducido);
                lore.add(ChatColor.GRAY + "Cantidad: " + item.getAmount());
                lore.add(ChatColor.BLUE + "Precio de unidad por: $" + FormateadorNumeros.formatear(precioReinoUnit) + " " + nombreMoneda);
                lore.add(ChatColor.DARK_GRAY + "(~$" + FormateadorNumeros.formatear(precioServidorUnit) + " Reinas)");
                lore.add(ChatColor.YELLOW + "Clickea tu item desde tu inventario para poner en venta.");
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

    // -------------------------------
    // Manejar click en menú de venta
    // -------------------------------
    public static void manejarClickVenta(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_VENTA)) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        String etiquetaReino = MenuInteractivo.getInstancia().getBaseDeDatos().getReinoJugador(jugador.getUniqueId());
        if (etiquetaReino == null) {
            jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        BancoManager bancoManager = MenuInteractivo.getInstancia().getBancoManager();
        double valorMonedaServidor = bancoManager.calcularValorMonedaReino(etiquetaReino);
        if (valorMonedaServidor <= 0) valorMonedaServidor = 0.01;

        Material material = item.getType();
        double precioServidorUnit = CalculadoraPrecios.calcularPrecioVenta(material, jugador);
        double precioReinoUnit = precioServidorUnit / valorMonedaServidor;

        if (precioReinoUnit <= 0) {
            jugador.sendMessage(ChatColor.RED + "Este ítem no se puede poner a la venta.");
            return;
        }

        ItemStack original = item.clone();
        int cantidad = original.getAmount();

        double precioTotalReino = precioReinoUnit * cantidad;
        double precioTotalServidor = precioServidorUnit * cantidad;

        if (!jugador.getInventory().containsAtLeast(original, cantidad)) {
            jugador.sendMessage(ChatColor.RED + "Debes tener el ítem en tu inventario para venderlo.");
            return;
        }

        String nombreMoneda = bancoManager.obtenerNombreMonedaDeReino(etiquetaReino);
        if (nombreMoneda == null || nombreMoneda.isEmpty()) nombreMoneda = "Moneda";

        String itemSerializado = SerializadorItemStack.serializar(original);
        boolean exito = MenuInteractivo.getInstancia().getBaseDeDatos()
                .insertarItemEnMercado(jugador.getUniqueId(), etiquetaReino, itemSerializado, cantidad, precioTotalReino);

        if (exito) {
            jugador.getInventory().removeItem(new ItemStack(material, cantidad));
            jugador.sendMessage(ChatColor.GREEN + "¡Has puesto en venta " + cantidad + "x "
                    + material.name().toLowerCase().replace("_", " ") + "!");
            jugador.sendMessage(ChatColor.YELLOW + "Precio: " + ChatColor.GOLD
                    + FormateadorNumeros.formatear(precioTotalReino) + ChatColor.YELLOW + " " + nombreMoneda
                    + ChatColor.GRAY + "(~$" + FormateadorNumeros.formatear(precioTotalServidor) + " Reinas)");

            jugador.playSound(jugador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            abrirMenuVenta(jugador); // refrescar menú
        } else {
            jugador.sendMessage(ChatColor.RED + "Ocurrió un error al registrar el ítem en el mercado.");
        }
    }

    // -------------------------------
    // Abrir mercado del reino
    // -------------------------------
    public static void abrirMercadoDelReino(Player jugador) {
        String reino = MenuInteractivo.getInstancia().getBaseDeDatos().getReinoJugador(jugador.getUniqueId());
        if (reino == null) {
            jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        BancoManager bancoManager = MenuInteractivo.getInstancia().getBancoManager();
        double valorMonedaServidor = bancoManager.calcularValorMonedaReino(reino);
        if (valorMonedaServidor <= 0) valorMonedaServidor = 0.01;

        String etiquetaReino = MenuInteractivo.getInstancia().getBaseDeDatos().getReinoJugador(jugador.getUniqueId());
        String nombreMoneda = bancoManager.obtenerNombreMonedaDeReino(etiquetaReino);
        if (nombreMoneda == null || nombreMoneda.isEmpty()) nombreMoneda = "Moneda";


        List<ItemEnVenta> items = MenuInteractivo.getInstancia().getBaseDeDatos().getItemsMercadoDelReino(reino);
        Inventory menu = Bukkit.createInventory(null, 54, TITULO_MERCADO);

        for (int i = 0; i < Math.min(items.size(), 54); i++) {
            ItemEnVenta venta = items.get(i);
            ItemStack item = SerializadorItemStack.deserializar(venta.itemSerializado());
            if (item == null) continue;

            int cantidad = venta.cantidad();
            double precioReino = venta.precio();
            double precioServidor = precioReino * valorMonedaServidor;

            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Cantidad: " + cantidad);
            lore.add(ChatColor.YELLOW + "Precio: $" + FormateadorNumeros.formatear(precioReino) + " " + nombreMoneda);
            lore.add(ChatColor.DARK_GRAY + "(~$" + FormateadorNumeros.formatear(precioServidor) + " Reinas)");
            lore.add(ChatColor.GRAY + "Click para comprar");
            lore.add(ChatColor.DARK_GRAY + "ID#" + venta.id());

            meta.setLore(lore);
            item.setItemMeta(meta);
            menu.setItem(i, item);
        }

        jugador.openInventory(menu);
    }

    // -------------------------------
    // Manejar click de compra
    // -------------------------------
    public static void manejarClickCompra(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MERCADO)) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        UUID compradorUUID = jugador.getUniqueId();

        String etiquetaReino = MenuInteractivo.getInstancia()
                .getBaseDeDatos()
                .getReinoJugador(compradorUUID);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return;

        // 1️⃣ Obtener ID del ítem
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

        // 2️⃣ Obtener venta de la base de datos
        ItemEnVenta venta = MenuInteractivo.getInstancia().getBaseDeDatos().buscarItemEnMercadoPorID(id);
        if (venta == null) {
            jugador.sendMessage(ChatColor.RED + "Ese ítem ya no está disponible.");
            abrirMercadoDelReino(jugador);
            return;
        }

        String reino = venta.reino();
        BancoManager bancoManager = MenuInteractivo.getInstancia().getBancoManager();
        double valorMonedaServidor = bancoManager.calcularValorMonedaReino(reino);
        if (valorMonedaServidor <= 0) valorMonedaServidor = 0.01;

        double precioReino = venta.precio();
        double precioServidor = precioReino * valorMonedaServidor;

        String nombreMoneda = bancoManager.obtenerNombreMonedaDeReino(etiquetaReino);
        if (nombreMoneda == null || nombreMoneda.isEmpty()) nombreMoneda = "Moneda";

        // 3️⃣ Verificar saldo REAL en bancos
        boolean tieneSaldo = MenuInteractivo.getInstancia().getBaseDeDatos()
                .tieneSaldoSuficienteEnBancos(compradorUUID, reino, precioReino);

        if (!tieneSaldo) {
            jugador.sendMessage(ChatColor.RED + "No tienes suficientes " + nombreMoneda + " en tus bancos para comprar este ítem.");
            return;
        }

        // 4️⃣ Procesar compra (descuenta del banco principal y suma al vendedor)
        boolean exito = MenuInteractivo.getInstancia().getBaseDeDatos()
                .procesarCompraEnMercado(venta.id(), compradorUUID, precioReino);

        if (exito) {
            jugador.getInventory().addItem(SerializadorItemStack.deserializar(venta.itemSerializado()));
            jugador.sendMessage(ChatColor.GREEN + "¡Has comprado el ítem correctamente!");
            jugador.sendMessage(ChatColor.YELLOW + "Pagaste $" + ChatColor.GOLD
                    + FormateadorNumeros.formatear(precioReino) + ChatColor.YELLOW + " " + nombreMoneda
                    + ChatColor.GRAY + " (~$" + FormateadorNumeros.formatear(precioServidor) + " Reinas)");

            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
        } else {
            jugador.sendMessage(ChatColor.RED + "Error al procesar la compra.");
        }

        abrirMercadoDelReino(jugador);
    }

    // -------------------------------
    // Abrir cuenta personal
    // -------------------------------
    public static void abrirCuentaPersonal(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 27, TITULO_CUENTA);

        Map<String, Double> saldos = MenuInteractivo.getInstancia()
                .getBaseDeDatos()
                .obtenerTodosLosSaldosJugador(jugador.getUniqueId());

        BancoManager bancoManager = MenuInteractivo.getInstancia().getBancoManager();

        int slot = 0;
        for (Map.Entry<String, Double> entrada : saldos.entrySet()) {
            String reino = entrada.getKey();
            double cantidad = entrada.getValue();

            String nombreMoneda = bancoManager.obtenerNombreMonedaDeReino(reino);
            if (nombreMoneda == null || nombreMoneda.isEmpty()) nombreMoneda = "Moneda";

            double valorMonedaServidor = bancoManager.calcularValorMonedaReino(reino);
            if (valorMonedaServidor <= 0) valorMonedaServidor = 0.01;

            double valorEquivalente = cantidad * valorMonedaServidor;

            ItemStack moneda = new ItemStack(Material.PRISMARINE_CRYSTALS);
            ItemMeta meta = moneda.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Reino: [" + reino.toUpperCase() + "]");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Saldo: " + ChatColor.GOLD + "$" + FormateadorNumeros.formatear(cantidad) + " " + nombreMoneda,
                    ChatColor.BLUE + "Valor actual: $" + FormateadorNumeros.formatear(valorEquivalente),
                    ChatColor.DARK_GRAY + "($1 " + nombreMoneda + " = $" + FormateadorNumeros.formatear(valorMonedaServidor) + " Reinas)"
            ));
            moneda.setItemMeta(meta);

            menu.setItem(slot++, moneda);
            if (slot >= 27) break;
        }

        jugador.openInventory(menu);
    }

    // -------------------------------
    // Eventos de inventario
    // -------------------------------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();
        if (titulo.equals(TITULO_VENTA)) {
            manejarClickVenta(event);
        } else if (titulo.equals(TITULO_MERCADO)) {
            manejarClickCompra(event);
        } else if (titulo.equals(TITULO_CUENTA)) {
            event.setCancelled(true);
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
