package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

public class MenuBancoIndividual {

    private final MenuInteractivo plugin;

    public MenuBancoIndividual(MenuInteractivo plugin) {
        this.plugin = plugin;
    }

    public void abrirMenu(Player jugador, String etiquetaBanco) {
        if (!estaRegistradoEnBanco(jugador, etiquetaBanco)) {
            jugador.sendMessage(ChatColor.RED + "Primero debes unirte a ese banco.");
            return;
        }

        double fondos = obtenerFondosBanco(etiquetaBanco);

        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Banco: " + etiquetaBanco);

        ItemStack saldo = new ItemStack(Material.SUNFLOWER);
        ItemMeta metaSaldo = saldo.getItemMeta();
        metaSaldo.setDisplayName(ChatColor.YELLOW + "Fondos del Banco");
        metaSaldo.setLore(Collections.singletonList(ChatColor.GREEN + "$" + fondos));
        saldo.setItemMeta(metaSaldo);
        menu.setItem(11, saldo);

        ItemStack retirar = new ItemStack(Material.REDSTONE);
        ItemMeta metaRetirar = retirar.getItemMeta();
        metaRetirar.setDisplayName(ChatColor.RED + "Retirar Dinero");
        metaRetirar.setLore(Collections.singletonList(ChatColor.GRAY + "Click para retirar $100"));
        retirar.setItemMeta(metaRetirar);
        menu.setItem(13, retirar);

        ItemStack ingresar = new ItemStack(Material.EMERALD);
        ItemMeta metaIngresar = ingresar.getItemMeta();
        metaIngresar.setDisplayName(ChatColor.GREEN + "Ingresar Dinero");
        metaIngresar.setLore(Collections.singletonList(ChatColor.GRAY + "Click para ingresar $100"));
        ingresar.setItemMeta(metaIngresar);
        menu.setItem(15, ingresar);

        jugador.openInventory(menu);
    }

    private boolean estaRegistradoEnBanco(Player jugador, String etiqueta) {
        try {
            PreparedStatement ps = plugin.getGestorBaseDeDatos().getConexion().prepareStatement(
                    "SELECT 1 FROM jugadores_banco WHERE uuid = ? AND etiqueta_banco = ?"
            );
            ps.setString(1, jugador.getUniqueId().toString());
            ps.setString(2, etiqueta);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al verificar si el jugador está registrado en el banco: " + e.getMessage());
            return false;
        }
    }

    private double obtenerFondosBanco(String etiqueta) {
        try {
            PreparedStatement ps = plugin.getGestorBaseDeDatos().getConexion().prepareStatement(
                    "SELECT fondos FROM bancos WHERE etiqueta = ?"
            );
            ps.setString(1, etiqueta);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("fondos");
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener fondos del banco: " + e.getMessage());
        }
        return 0.0;
    }

    private void actualizarFondosBanco(String etiqueta, double nuevoMonto) {
        try {
            PreparedStatement ps = plugin.getGestorBaseDeDatos().getConexion().prepareStatement(
                    "UPDATE bancos SET fondos = ? WHERE etiqueta = ?"
            );
            ps.setDouble(1, nuevoMonto);
            ps.setString(2, etiqueta);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al actualizar fondos del banco: " + e.getMessage());
        }
    }

    public void manejarClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String titulo = ChatColor.stripColor(e.getView().getTitle());
        if (!titulo.startsWith("Banco: ")) return;

        e.setCancelled(true);
        Player jugador = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String etiqueta = titulo.replace("Banco: ", "").trim();
        double fondosActuales = obtenerFondosBanco(etiqueta);

        if (item.getType() == Material.REDSTONE) {
            // Retirar $100 si el banco tiene suficiente dinero
            if (fondosActuales >= 100) {
                actualizarFondosBanco(etiqueta, fondosActuales - 100);
                plugin.getEconomia().depositPlayer(jugador, 100);
                jugador.sendMessage(ChatColor.GREEN + "Has retirado $100 del banco " + etiqueta);
            } else {
                jugador.sendMessage(ChatColor.RED + "El banco no tiene suficientes fondos para esa operación.");
            }
        } else if (item.getType() == Material.EMERALD) {
            // Ingresar $100 si el jugador tiene suficiente dinero
            if (plugin.getEconomia().getBalance(jugador) >= 100) {
                plugin.getEconomia().withdrawPlayer(jugador, 100);
                actualizarFondosBanco(etiqueta, fondosActuales + 100);
                jugador.sendMessage(ChatColor.GREEN + "Has ingresado $100 al banco " + etiqueta);
            } else {
                jugador.sendMessage(ChatColor.RED + "No tienes suficiente dinero para ingresar.");
            }
        }

        jugador.closeInventory();
    }
}
