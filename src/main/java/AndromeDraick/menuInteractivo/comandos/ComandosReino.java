package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ComandosReino implements CommandExecutor {

    private final MenuInteractivo plugin;
    private final GestorBaseDeDatos db;

    public ComandosReino(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.db = plugin.getGestorBaseDeDatos();
        plugin.getCommand("rnmi").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player jugador = (Player) sender;
        UUID uuid = jugador.getUniqueId();

        if (args.length == 0) {
            jugador.sendMessage(ChatColor.YELLOW + "Usa /rnmi ayuda para ver los comandos disponibles.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "crear":
                if (!jugador.hasPermission("bmi.comandos.crear.reino")) {
                    jugador.sendMessage(ChatColor.RED + "No tienes permiso para crear un reino.");
                    return true;
                }

                if (args.length < 3 || !args[1].equalsIgnoreCase("reino")) {
                    jugador.sendMessage(ChatColor.RED + "Uso correcto: /rnmi crear reino <Nombre> <Etiqueta>");
                    return true;
                }

                String nombre = args[2];
                String etiqueta = args[3].toLowerCase();

                try {
                    PreparedStatement ps = db.getConexion().prepareStatement("INSERT INTO reinos (etiqueta, nombre, uuid_rey) VALUES (?, ?, ?)");
                    ps.setString(1, etiqueta);
                    ps.setString(2, nombre);
                    ps.setString(3, uuid.toString());
                    ps.executeUpdate();

                    PreparedStatement ps2 = db.getConexion().prepareStatement("INSERT INTO jugadores_reino (uuid, etiqueta_reino) VALUES (?, ?)");
                    ps2.setString(1, uuid.toString());
                    ps2.setString(2, etiqueta);
                    ps2.executeUpdate();

                    jugador.sendMessage(ChatColor.GREEN + "Has fundado el Reino " + nombre + " con la etiqueta " + etiqueta + ".");
                } catch (SQLException e) {
                    jugador.sendMessage(ChatColor.RED + "Error: Esa etiqueta ya está en uso o ocurrió un problema.");
                }
                break;

            case "unirse":
                if (args.length < 3 || !args[1].equalsIgnoreCase("reino")) {
                    jugador.sendMessage(ChatColor.RED + "Uso correcto: /rnmi unirse reino <Nombre o Etiqueta>");
                    return true;
                }

                String identificador = args[2].toLowerCase();

                try {
                    PreparedStatement ps = db.getConexion().prepareStatement("SELECT etiqueta FROM reinos WHERE etiqueta = ? OR nombre = ?");
                    ps.setString(1, identificador);
                    ps.setString(2, identificador);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        String etiquetaFinal = rs.getString("etiqueta");

                        PreparedStatement ps2 = db.getConexion().prepareStatement("INSERT OR REPLACE INTO jugadores_reino (uuid, etiqueta_reino) VALUES (?, ?)");
                        ps2.setString(1, uuid.toString());
                        ps2.setString(2, etiquetaFinal);
                        ps2.executeUpdate();

                        jugador.sendMessage(ChatColor.GREEN + "Te has unido al Reino " + etiquetaFinal + ".");
                    } else {
                        jugador.sendMessage(ChatColor.RED + "Ese reino no existe.");
                    }
                } catch (SQLException e) {
                    jugador.sendMessage(ChatColor.RED + "Error al intentar unirse al reino.");
                }
                break;

            default:
                jugador.sendMessage(ChatColor.YELLOW + "Comando no reconocido. Usa /rnmi ayuda para más información.");
                break;
        }

        return true;
    }
}
