package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

public class ComandosBanco implements CommandExecutor {
    private final GestorBaseDeDatos db;

    public ComandosBanco(GestorBaseDeDatos db) {
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 1) {
            player.sendMessage("Uso: /bmi <crear|depositar|retirar|saldo|invitar|expulsar>");
            return false;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "depositar":
                if (args.length < 3) {
                    player.sendMessage("Uso: /bmi depositar <etiquetaBanco> <monto>");
                    return false;
                }
                double montoDep = Double.parseDouble(args[2]);
                if (db.depositarEnBanco(args[1], montoDep)) {
                    player.sendMessage("Has depositado " + montoDep + " en " + args[1]);
                } else {
                    player.sendMessage("Error al depositar.");
                }
                break;
            case "retirar":
                if (args.length < 3) {
                    player.sendMessage("Uso: /bmi retirar <etiquetaBanco> <monto>");
                    return false;
                }
                double montoRet = Double.parseDouble(args[2]);
                if (db.retirarDeBanco(args[1], montoRet)) {
                    player.sendMessage("Has retirado " + montoRet + " de " + args[1]);
                } else {
                    player.sendMessage("Error al retirar o saldo insuficiente.");
                }
                break;
            case "saldo":
                if (args.length < 2) {
                    player.sendMessage("Uso: /bmi saldo <etiquetaBanco>");
                    return false;
                }
                double saldo = db.obtenerSaldoBanco(args[1]);
                player.sendMessage("Saldo de " + args[1] + ": " + saldo);
                break;
            case "invitar":
                if (args.length < 3) {
                    player.sendMessage("Uso: /bmi invitar <etiquetaBanco> <jugador>");
                    return false;
                }
                Player target = player.getServer().getPlayer(args[2]);
                if (target != null && db.agregarSocioBanco(args[1], target.getUniqueId())) {
                    player.sendMessage("Has invitado a " + target.getName() + " al banco " + args[1]);
                } else {
                    player.sendMessage("Error al invitar o jugador no encontrado.");
                }
                break;
            case "expulsar":
                if (args.length < 3) {
                    player.sendMessage("Uso: /bmi expulsar <etiquetaBanco> <jugador>");
                    return false;
                }
                Player objetivo = player.getServer().getPlayer(args[2]);
                if (objetivo != null && db.quitarSocioBanco(args[1], objetivo.getUniqueId())) {
                    player.sendMessage("Has expulsado a " + objetivo.getName() + " de " + args[1]);
                } else {
                    player.sendMessage("Error al expulsar o jugador no encontrado.");
                }
                break;
            default:
                player.sendMessage("Subcomando desconocido.");
        }
        return true;
    }
}