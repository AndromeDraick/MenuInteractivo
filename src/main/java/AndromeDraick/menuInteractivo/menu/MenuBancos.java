// src/main/java/AndromeDraick/menuInteractivo/menu/MenuBancos.java
package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.model.Banco;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class MenuBancos implements Listener {

    private static class IndividualHolder implements InventoryHolder {
        private final String etiqueta;
        IndividualHolder(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
        @Override public Inventory getInventory() { return null; }
    }

    private static final String TITULO_INDIVIDUAL = ChatColor.GOLD + "Banco: ";
    private final BancoManager bancoManager;
    private final Economy economia;

    public MenuBancos(MenuInteractivo plugin) {
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos());
        this.economia     = plugin.getEconomia();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void abrirIndividual(Player p, String etiqueta) {
        String propia = bancoManager.obtenerBancoDeJugador(p.getUniqueId());
        if (!etiqueta.equals(propia)) {
            p.sendMessage(ChatColor.RED + "No estás vinculado a ese banco.");
            return;
        }
        Banco b = bancoManager.obtenerBanco(etiqueta);
        Inventory inv = Bukkit.createInventory(new IndividualHolder(etiqueta), 27, TITULO_INDIVIDUAL + etiqueta);

        ItemStack saldo = new ItemStack(Material.SUNFLOWER);
        ItemMeta ms = saldo.getItemMeta();
        ms.setDisplayName(ChatColor.YELLOW + "Saldo");
        ms.setLore(List.of(ChatColor.GREEN + "$" + b.getFondos()));
        saldo.setItemMeta(ms);
        inv.setItem(11, saldo);

        ItemStack ret = new ItemStack(Material.REDSTONE);
        ItemMeta mr = ret.getItemMeta();
        mr.setDisplayName(ChatColor.RED + "Retirar $100");
        ret.setItemMeta(mr);
        inv.setItem(13, ret);

        ItemStack ing = new ItemStack(Material.EMERALD);
        ItemMeta mi = ing.getItemMeta();
        mi.setDisplayName(ChatColor.GREEN + "Ingresar $100");
        ing.setItemMeta(mi);
        inv.setItem(15, ing);

        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof IndividualHolder)) return;
        e.setCancelled(true);
        IndividualHolder ih = (IndividualHolder) e.getInventory().getHolder();
        Player p = (Player) e.getWhoClicked();
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta()) return;

        String tag = ih.getEtiqueta();
        if (it.getType() == Material.REDSTONE) {
            double fondos = bancoManager.obtenerSaldo(tag);
            if (fondos >= 100 && bancoManager.retirar(tag, 100)) {
                economia.depositPlayer(p, 100);
                p.sendMessage(ChatColor.GREEN + "Retiraste $100 de " + tag);
            } else {
                p.sendMessage(ChatColor.RED + "No hay fondos suficientes.");
            }
        } else if (it.getType() == Material.EMERALD) {
            if (economia.getBalance(p) >= 100 && bancoManager.depositar(tag, 100)) {
                economia.withdrawPlayer(p, 100);
                p.sendMessage(ChatColor.GREEN + "Ingresaste $100 al banco " + tag);
            } else {
                p.sendMessage(ChatColor.RED + "No tienes $100.");
            }
        }
        p.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof IndividualHolder) {
            e.setCancelled(true);
        }
    }
}
