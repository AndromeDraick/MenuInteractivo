package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.model.MonedasReinoInfo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.List;

public class MenuMonedas {

    private final MenuInteractivo plugin;
    private final BancoManager bancoManager;
    private final DecimalFormat formato = new DecimalFormat("#.##");

    public MenuMonedas(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos());
    }

    public void abrirMenu(Player jugador) {
        // Usamos el nuevo método actualizado directamente desde BancoManager
        List<MonedasReinoInfo> monedas = bancoManager.obtenerMonedasReinoInfo();

        int size = Math.max(9, ((monedas.size() / 9) + 1) * 9);
        Inventory menu = Bukkit.createInventory(null, size, ChatColor.DARK_GREEN + "Monedas de los Reinos");

        for (MonedasReinoInfo moneda : monedas) {
            ItemStack item = new ItemStack(Material.GOLD_NUGGET);
            ItemMeta meta = item.getItemMeta();

            double impresas = moneda.getCantidadImpresa();
            double quemadas = moneda.getCantidadQuemada();
            double convertidas = moneda.getDineroConvertido();
            double valor = (impresas - quemadas) > 0 ? convertidas / (impresas - quemadas) : 0;

            double saldoJugador = bancoManager.obtenerSaldoMonedaJugador(jugador.getUniqueId().toString(), moneda.getEtiquetaReino());

            meta.setDisplayName(ChatColor.GOLD + moneda.getNombreMoneda());
            meta.setLore(List.of(
                    ChatColor.GRAY + "Reino: " + ChatColor.YELLOW + moneda.getEtiquetaReino(),
                    ChatColor.GRAY + "Valor: " + ChatColor.AQUA + formato.format(valor) + " $ por 1 moneda",
                    ChatColor.GRAY + "Impresa: " + ChatColor.YELLOW + formato.format(impresas),
                    ChatColor.GRAY + "Quemada: " + ChatColor.RED + formato.format(quemadas),
                    ChatColor.GRAY + "Convertida: " + ChatColor.GREEN + formato.format(convertidas),
                    ChatColor.GRAY + "Fecha: " + ChatColor.WHITE + moneda.getFechaCreacion(),
                    ChatColor.GRAY + "Tu saldo: " + ChatColor.LIGHT_PURPLE + formato.format(saldoJugador)
            ));
            item.setItemMeta(meta);
            menu.addItem(item);
        }

        jugador.openInventory(menu);
    }
}
