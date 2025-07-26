package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

public class MenuCuentaBanco implements Listener {

    private final MenuInteractivo plugin;
    private final BancoManager bancoManager;
    private final DecimalFormat formato = new DecimalFormat("#.##");
    private final Map<UUID, String> esperandoDeposito = new HashMap<>();
    private final Map<UUID, String> esperandoTransferencia = new HashMap<>();

    public MenuCuentaBanco(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.bancoManager = plugin.getBancoManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void abrirMenuCuenta(Player jugador, String etiquetaBanco) {
        UUID uuid = jugador.getUniqueId();
        String reino = bancoManager.obtenerReinoDeBanco(etiquetaBanco);
        String moneda = bancoManager.obtenerNombreMonedaDeReino(reino);
        bancoManager.crearCuentaMonedaSiNoExiste(uuid, reino);
        double saldo = bancoManager.obtenerSaldoCuenta(uuid, reino);

        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Cuenta: " + etiquetaBanco);

        // Saldo principal y otras monedas
        ItemStack saldoItem = new ItemStack(Material.EMERALD);
        ItemMeta saldoMeta = saldoItem.getItemMeta();
        saldoMeta.setDisplayName(ChatColor.GOLD + "Saldo actual");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.LIGHT_PURPLE + formato.format(saldo) + " " + moneda);

        Map<String, Double> otrosSaldos = bancoManager.obtenerSaldosDeJugador(uuid);
        if (otrosSaldos.size() > 1) {
            lore.add(ChatColor.GRAY + "Otras monedas:");
            for (Map.Entry<String, Double> entrada : otrosSaldos.entrySet()) {
                String otroReino = entrada.getKey();
                if (!otroReino.equals(reino)) {
                    double cantidad = entrada.getValue();
                    if (cantidad > 0) {
                        String nombreMoneda = bancoManager.obtenerNombreMonedaDeReino(otroReino);
                        lore.add(ChatColor.GRAY + "- " + formato.format(cantidad) + " " + nombreMoneda);
                    }
                }
            }
        }

        saldoMeta.setLore(lore);
        saldoItem.setItemMeta(saldoMeta);
        menu.setItem(13, saldoItem);

        // Izquierda: Depositar
        ItemStack depositar = new ItemStack(Material.GOLD_INGOT);
        ItemMeta depositarMeta = depositar.getItemMeta();
        depositarMeta.setDisplayName(ChatColor.GREEN + "Depositar monedas");
        depositarMeta.setLore(List.of(ChatColor.GRAY + "Haz clic para escribir la cantidad a depositar."));
        depositar.setItemMeta(depositarMeta);
        menu.setItem(11, depositar);

        // Derecha: Transferir
        ItemStack transferir = new ItemStack(Material.ENDER_PEARL);
        ItemMeta transferirMeta = transferir.getItemMeta();
        transferirMeta.setDisplayName(ChatColor.AQUA + "Transferir a jugador");
        transferirMeta.setLore(List.of(ChatColor.GRAY + "Haz clic para escribir el monto y jugador."));
        transferir.setItemMeta(transferirMeta);
        menu.setItem(15, transferir);

        // Atrás o salir
        ItemStack salir = new ItemStack(Material.BARRIER);
        ItemMeta salirMeta = salir.getItemMeta();
        salirMeta.setDisplayName(ChatColor.RED + "Cerrar menú");
        salir.setItemMeta(salirMeta);
        menu.setItem(26, salir);

        jugador.openInventory(menu);
    }

    @EventHandler
    public void alClickearCuenta(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();
        if (!titulo.startsWith(ChatColor.DARK_PURPLE + "Cuenta: ")) return;
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        String etiqueta = titulo.replace(ChatColor.DARK_PURPLE + "Cuenta: ", "");

        switch (event.getSlot()) {
            case 11 -> {
                jugador.closeInventory();
                jugador.sendMessage(ChatColor.YELLOW + "Escribe la cantidad de monedas que deseas depositar, no necesitas usar '/':");
                esperandoDeposito.put(jugador.getUniqueId(), etiqueta);
            }
            case 15 -> {
                jugador.closeInventory();
                jugador.sendMessage(ChatColor.YELLOW + "Escribe la cantidad y el jugador al que deseas transferir. Ejemplo:");
                jugador.sendMessage(ChatColor.GRAY + "   100 Pablo");
                esperandoTransferencia.put(jugador.getUniqueId(), etiqueta);
            }
            case 26 -> jugador.closeInventory();
        }
    }

    @EventHandler
    public void alEscribir(AsyncPlayerChatEvent event) {
        Player jugador = event.getPlayer();
        UUID uuid = jugador.getUniqueId();

        if (esperandoDeposito.containsKey(uuid)) {
            event.setCancelled(true);
            String etiqueta = esperandoDeposito.remove(uuid);
            try {
                double monto = Double.parseDouble(event.getMessage().trim());
                if (monto <= 0) {
                    jugador.sendMessage(ChatColor.RED + "El monto debe ser mayor a 0.");
                    return;
                }

                if (plugin.getEconomia().getBalance(jugador) < monto) {
                    jugador.sendMessage(ChatColor.RED + "No tienes suficientes Reinas para solicitar esa cantidad.");
                    return;
                }

                String reinoJugador = bancoManager.obtenerReinoJugador(uuid);

                if (plugin.getEconomia().getBalance(jugador) < monto) {
                    jugador.sendMessage(ChatColor.RED + "No tienes suficientes Reinas para solicitar esa cantidad.");
                    return;
                }

                boolean registrada = bancoManager.registrarSolicitudMoneda(uuid, etiqueta, monto, reinoJugador);
                if (registrada) {
                    plugin.getEconomia().withdrawPlayer(jugador, monto);
                    jugador.sendMessage(ChatColor.GREEN + "Tu solicitud fue enviada y se te descontaron " + formato.format(monto) + " Reinas.");
                } else {
                    jugador.sendMessage(ChatColor.RED + "No se pudo registrar la solicitud. Intenta más tarde.");
                }

            } catch (NumberFormatException e) {
                jugador.sendMessage(ChatColor.RED + "Número inválido.");
            }
            return;
        }

        if (esperandoTransferencia.containsKey(uuid)) {
            event.setCancelled(true);
            String etiqueta = esperandoTransferencia.remove(uuid);
            String[] partes = event.getMessage().trim().split(" ");
            if (partes.length != 2) {
                jugador.sendMessage(ChatColor.RED + "Formato inválido. Usa: cantidad jugador");
                return;
            }

            try {
                double monto = Double.parseDouble(partes[0]);
                OfflinePlayer destino = Bukkit.getOfflinePlayer(partes[1]);

                if (!destino.hasPlayedBefore()) {
                    jugador.sendMessage(ChatColor.RED + "El jugador no existe.");
                    return;
                }

                boolean exito = bancoManager.transferirEntreJugadores(uuid, destino.getUniqueId(), etiqueta, monto);
                if (exito) {
                    jugador.sendMessage(ChatColor.GREEN + "Has transferido " + formato.format(monto) + " monedas a " + destino.getName());
                } else {
                    jugador.sendMessage(ChatColor.RED + "Saldo insuficiente o error al transferir.");
                }
            } catch (NumberFormatException e) {
                jugador.sendMessage(ChatColor.RED + "Número inválido.");
            }
        }
    }
}
