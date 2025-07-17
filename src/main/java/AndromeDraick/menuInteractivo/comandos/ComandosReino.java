package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.managers.ReinoManager;
import AndromeDraick.menuInteractivo.model.Reino;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.UUID;

public class ComandosReino implements CommandExecutor {
    private final ReinoManager reinoManager;

    public ComandosReino(GestorBaseDeDatos db) {
        this.reinoManager = new ReinoManager(db);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 1) {
            player.sendMessage("Uso: /rnmi <crear|unir|salir|eliminar|listar>");
            return false;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "crear":
                if (args.length < 3) {
                    player.sendMessage("Uso: /rnmi crear <etiqueta> <nombre>");
                    return false;
                }
                if (reinoManager.crearReino(args[1], args[2], player.getUniqueId())) {
                    player.sendMessage("Reino creado: " + args[2]);
                } else {
                    player.sendMessage("Error al crear reino.");
                }
                break;
            case "unir":
                if (args.length < 2) {
                    player.sendMessage("Uso: /rnmi unir <etiqueta>");
                    return false;
                }
                if (reinoManager.unirReino(player.getUniqueId(), args[1])) {
                    player.sendMessage("Te has unido al reino " + args[1]);
                } else {
                    player.sendMessage("Error al unirte al reino.");
                }
                break;
            case "salir":
                if (args.length < 2) {
                    player.sendMessage("Uso: /rnmi salir <etiqueta>");
                    return false;
                }
                if (reinoManager.salirReino(player.getUniqueId(), args[1])) {
                    player.sendMessage("Has salido del reino " + args[1]);
                } else {
                    player.sendMessage("Error al salir del reino.");
                }
                break;
            case "eliminar":
                if (args.length < 2) {
                    player.sendMessage("Uso: /rnmi eliminar <etiqueta>");
                    return false;
                }
                if (reinoManager.eliminarReino(args[1])) {
                    player.sendMessage("Reino eliminado: " + args[1]);
                } else {
                    player.sendMessage("Error al eliminar reino.");
                }
                break;
            case "listar":
                List<Reino> reinos = reinoManager.listarReinos();
                if (reinos.isEmpty()) {
                    player.sendMessage("No hay reinos.");
                } else {
                    player.sendMessage("Reinos disponibles:");
                    for (Reino r : reinos) {
                        player.sendMessage("- " + r.getEtiqueta() + ": " + r.getNombre());
                    }
                }
                break;
            default:
                player.sendMessage("Subcomando desconocido.");
        }
        return true;
    }
}