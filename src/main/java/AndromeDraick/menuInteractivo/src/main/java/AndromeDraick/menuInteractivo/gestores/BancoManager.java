package AndromeDraick.menuInteractivo.gestores;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BancoManager {

    private final MenuInteractivo plugin;

    public BancoManager(MenuInteractivo plugin) {
        this.plugin = plugin;
    }

    public boolean crearBancoPendiente(String nombre, String etiqueta, String reinoEtiqueta, String uuidPropietario) {
        try {
            PreparedStatement ps = plugin.getGestorBaseDeDatos().getConexion().prepareStatement(
                    "INSERT INTO bancos (etiqueta, nombre, reino_etiqueta, uuid_propietario, estado) VALUES (?, ?, ?, ?, 'pendiente')"
            );
            ps.setString(1, etiqueta);
            ps.setString(2, nombre);
            ps.setString(3, reinoEtiqueta);
            ps.setString(4, uuidPropietario);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<String> obtenerBancosPendientes(String reinoEtiqueta) {
        List<String> bancos = new ArrayList<>();
        try {
            PreparedStatement ps = plugin.getGestorBaseDeDatos().getConexion().prepareStatement(
                    "SELECT etiqueta FROM bancos WHERE reino_etiqueta = ? AND estado = 'pendiente'"
            );
            ps.setString(1, reinoEtiqueta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bancos.add(rs.getString("etiqueta"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener bancos pendientes: " + e.getMessage());
        }
        return bancos;
    }

    public boolean cambiarEstadoBanco(String etiquetaBanco, String nuevoEstado) {
        try {
            PreparedStatement ps = plugin.getGestorBaseDeDatos().getConexion().prepareStatement(
                    "UPDATE bancos SET estado = ? WHERE etiqueta = ?"
            );
            ps.setString(1, nuevoEstado);
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al actualizar estado del banco: " + e.getMessage());
            return false;
        }
    }

    public String obtenerReinoDelJugador(Player jugador) {
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
}