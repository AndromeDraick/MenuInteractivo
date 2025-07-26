package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.model.MonedasReinoInfo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

public class MenuMonedas implements Listener {

    private final MenuInteractivo plugin;
    private final BancoManager bancoManager;
    private final DecimalFormat formato = new DecimalFormat("#.##");

    public MenuMonedas(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos(), plugin.getEconomia());
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void abrirMenu(Player jugador) {
        List<MonedasReinoInfo> monedas = bancoManager.obtenerMonedasReinoInfo();

        int size = Math.max(9, ((monedas.size() / 9) + 1) * 9);
        Inventory menu = Bukkit.createInventory(null, size, ChatColor.DARK_GREEN + "Monedas de los Reinos");

        for (MonedasReinoInfo moneda : monedas) {
            ItemStack item = new ItemStack(Material.YELLOW_BUNDLE);
            ItemMeta meta = item.getItemMeta();

            double impresas = moneda.getCantidadImpresa();
            double quemadas = moneda.getCantidadQuemada();
            double convertidas = moneda.getDineroConvertido();

            double enCirculacion = impresas - quemadas;
            if (enCirculacion < 1) enCirculacion = 1;

            double valor = convertidas / enCirculacion;
            double inverso = valor > 0 ? (1.0 / valor) : 0;

            ChatColor colorValor;
            if (valor >= 1) colorValor = ChatColor.GREEN;
            else if (valor <= 0.3) colorValor = ChatColor.RED;
            else if (valor >= 10.5) colorValor = ChatColor.AQUA;
            else colorValor = ChatColor.YELLOW;

            UUID uuidJugador = jugador.getUniqueId();
            String etiquetaReino = moneda.getEtiquetaReino();
            plugin.getBancoManager().crearCuentaMonedaSiNoExiste(uuidJugador, etiquetaReino);
            double saldoJugador = plugin.getBancoManager().obtenerSaldoCuenta(uuidJugador, etiquetaReino);

            meta.setDisplayName(ChatColor.GOLD + moneda.getNombreMoneda());
            meta.setLore(List.of(
                    ChatColor.GRAY + "Reino: " + ChatColor.YELLOW + moneda.getEtiquetaReino(),
                    ChatColor.GRAY + "" + colorValor + "  $1 "+ moneda.getNombreMoneda() + " = $" + formato.format(valor) + ChatColor.RED +" Reinas",
                    ChatColor.GRAY + "" + colorValor + "  $1 " + ChatColor.RED + "Reina = $" + formato.format(inverso) + moneda.getNombreMoneda(),
                    ChatColor.GRAY + "En circulación: " + ChatColor.GOLD + formato.format(enCirculacion),
                    ChatColor.GRAY + "Impresa: " + ChatColor.YELLOW + formato.format(impresas),
                    ChatColor.GRAY + "Quemada: " + ChatColor.RED + formato.format(quemadas),
                    ChatColor.GRAY + "Convertida: " + ChatColor.GREEN + formato.format(convertidas),
                    ChatColor.GRAY + "Fecha: " + ChatColor.WHITE + moneda.getFechaCreacion(),
                    ChatColor.GRAY + "Tu saldo: " + ChatColor.LIGHT_PURPLE + "$" + formato.format(saldoJugador)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
            menu.addItem(item);
        }

        jugador.openInventory(menu);
    }

    @EventHandler
    public void alClickearInventario(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Monedas de los Reinos")) {
            event.setCancelled(true);
        }
    }
}
