package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.menu.MenuRecientes;
import AndromeDraick.menuInteractivo.menu.MenuVentaVisual;
import AndromeDraick.menuInteractivo.utilidades.CalculadoraPrecios;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import AndromeDraick.menuInteractivo.utilidades.HistorialComprasManager;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.*;

public class Comandos implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command comando, String etiqueta, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores.");
            return true;
        }

        if (etiqueta.equalsIgnoreCase("menu")) {
            String genero = MenuInteractivo.getInstancia().getBaseDeDatos().getGenero(jugador.getUniqueId());
            if (genero == null) {
                jugador.sendMessage(ChatColor.RED + "Antes de usar el menú, debes registrar tu ficha de rol.");
                jugador.sendMessage(ChatColor.YELLOW + "Usa: /rmi registro rol <genero> <nombre> <apellidoP> <apellidoM> <descendencia> <raza>");
                return true;
            }

            MenuInteractivo.getInstancia().getMenuPrincipal().abrir(jugador);
            return true;
        }

        if (!etiqueta.equalsIgnoreCase("tmi")) return false;

        if (args.length == 1 && args[0].equalsIgnoreCase("vender")) {
            MenuVentaVisual.abrir(jugador);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("recientes")) {
            MenuRecientes.abrir(jugador);
            return true;
        }

        if (args.length < 3) {
            jugador.sendMessage(ChatColor.RED + "Uso: /tmi <comprar|vender> <item> <cantidad>");
            return true;
        }

        String subcomando = args[0].toLowerCase();
        String itemStr = args[1].toUpperCase();
        int cantidad;

        try {
            cantidad = Integer.parseInt(args[2]);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            jugador.sendMessage(ChatColor.RED + "La cantidad debe ser un número válido mayor a 0.");
            return true;
        }

        Material material;
        try {
            material = Material.valueOf(itemStr);
        } catch (IllegalArgumentException e) {
            jugador.sendMessage(ChatColor.RED + "El ítem '" + itemStr + "' no es válido.");
            return true;
        }

        var configTienda = MenuInteractivo.getInstancia().getConfigTienda();
        FileConfiguration config = configTienda.getConfig();

        String nombreTraducido = material.name().toLowerCase().replace("_", " ");
        if (config.contains("items_custom." + material.name() + ".nombre")) {
            nombreTraducido = ChatColor.stripColor(config.getString("items_custom." + material.name() + ".nombre"));
        }

        if (subcomando.equals("comprar")) {
            double precioUnitario = CalculadoraPrecios.calcularPrecioCompra(material, jugador);
            if (precioUnitario < 0) {
                jugador.sendMessage(ChatColor.RED + "Ese ítem no tiene un precio configurado.");
                return true;
            }

            double total = precioUnitario * cantidad;
            var economia = MenuInteractivo.getInstancia().getEconomia();

            if (economia.has(jugador, total)) {
                EconomyResponse respuesta = economia.withdrawPlayer(jugador, total);
                if (respuesta.transactionSuccess()) {
                    jugador.getInventory().addItem(new ItemStack(material, cantidad));
                    jugador.sendMessage(ChatColor.GREEN + "Compraste " + cantidad + " de " + nombreTraducido +
                            " por $" + FormateadorNumeros.formatear(total) + ".");
                    HistorialComprasManager.registrarCompra(jugador.getUniqueId(), material);
                } else {
                    jugador.sendMessage(ChatColor.RED + "Error al realizar la transacción: " + respuesta.errorMessage);
                }
            } else {
                jugador.sendMessage(ChatColor.RED + "No tienes suficiente dinero. Te cuesta $" + FormateadorNumeros.formatear(total));
            }

            return true;

        } else if (subcomando.equals("vender")) {
            double precioUnitario = CalculadoraPrecios.calcularPrecioVenta(material, jugador);
            if (precioUnitario < 0) {
                jugador.sendMessage(ChatColor.RED + "Ese ítem no se puede vender.");
                return true;
            }

            int enInventario = contarItem(jugador, material);
            if (enInventario < cantidad) {
                jugador.sendMessage(ChatColor.RED + "No tienes suficientes ítems para vender. Tienes: " + enInventario);
                return true;
            }

            eliminarItems(jugador, material, cantidad);
            double total = precioUnitario * cantidad;
            var economia = MenuInteractivo.getInstancia().getEconomia();
            EconomyResponse respuesta = economia.depositPlayer(jugador, total);

            if (respuesta.transactionSuccess()) {
                jugador.sendMessage(ChatColor.YELLOW + "Vendiste " + cantidad + " de " + nombreTraducido +
                        " por $" + FormateadorNumeros.formatear(total) + ".");
            } else {
                jugador.sendMessage(ChatColor.RED + "Error al pagar por la venta: " + respuesta.errorMessage);
            }

            return true;

        } else {
            jugador.sendMessage(ChatColor.RED + "Uso: /tmi <comprar|vender> <item> <cantidad>");
            return true;
        }
    }

    private int contarItem(Player jugador, Material material) {
        return jugador.getInventory().all(material).values().stream()
                .mapToInt(ItemStack::getAmount).sum();
    }

    private void eliminarItems(Player jugador, Material material, int cantidad) {
        var inventario = jugador.getInventory();
        for (var entry : inventario.all(material).entrySet()) {
            var slot = entry.getKey();
            var item = entry.getValue();
            int amount = item.getAmount();

            if (cantidad >= amount) {
                inventario.clear(slot);
                cantidad -= amount;
            } else {
                item.setAmount(amount - cantidad);
                break;
            }

            if (cantidad <= 0) break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command comando, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        if (!comando.getName().equalsIgnoreCase("tmi")) return Collections.emptyList();

        switch (args.length) {
            case 1 -> {
                List<String> sub = Arrays.asList("comprar", "vender", "recientes");
                return StringUtil.copyPartialMatches(args[0].toLowerCase(), sub, new ArrayList<>(sub.size()));
            }
            case 2 -> {
                List<String> mats = new ArrayList<>();
                for (Material mat : Material.values()) {
                    if (mat.isItem()) mats.add(mat.name().toLowerCase());
                }
                Collections.sort(mats);
                return StringUtil.copyPartialMatches(args[1].toLowerCase(), mats, new ArrayList<>());
            }
            case 3 -> {
                List<String> cant = Arrays.asList("1", "16", "32", "64");
                List<String> matches = StringUtil.copyPartialMatches(args[2], cant, new ArrayList<>());
                matches.add("1 ← cantidad");
                return matches;
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }
}
