package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MenuListaBancos {

    private final MenuInteractivo plugin;

    public MenuListaBancos(MenuInteractivo plugin) {
        this.plugin = plugin;
    }

    public void abrirMenu(Player jugador) {
        String etiquetaReino = obtenerReinoJugador(jugador);
        if (etiquetaReino == null) {
            jugador.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        List<String> bancos = obtenerBancosAceptados(etiquetaReino);
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + "Bancos del Reino " + etiquetaReino);

        for (String etiqueta : bancos) {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Banco: " + etiqueta);
            meta.setLore(List.of(
                    ChatColor.GRAY + "Haz clic para abrir el menú del banco"
            ));
            item.setItemMeta(meta);
            menu.addItem(item);
        }

        jugador.openInventory(menu);
    }

    private String obtenerReinoJugador(Player jugador) {
        try {
            PreparedStatement ps = plugin.getGestorBaseDeDatos().getConexion().prepareStatement(
                    "SELECT etiqueta_reino FROM jugadores_reino WHERE uuid = ?"
            );
            ps.setString(1, jugador.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("etiqueta_reino");
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener reino del jugador: " + e.getMessage());
        }
        return null;
    }

    private List<String> obtenerBancosAceptados(String etiquetaReino) {
        List<String> bancos = new ArrayList<>();
        try {
            PreparedStatement ps = plugin.getGestorBaseDeDatos().getConexion().prepareStatement(
                    "SELECT etiqueta FROM bancos WHERE reino_etiqueta = ? AND estado = 'aceptado'"
            );
            ps.setString(1, etiquetaReino);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bancos.add(rs.getString("etiqueta"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener bancos aceptados: " + e.getMessage());
        }
        return bancos;
    }
}
