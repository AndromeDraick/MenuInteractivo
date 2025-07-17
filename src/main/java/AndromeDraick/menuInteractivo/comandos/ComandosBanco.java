package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.model.Banco;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ComandosBanco implements CommandExecutor {

    private final BancoManager bancoManager;
    private final Economy economia;

    public ComandosBanco(BancoManager bancoManager, Economy economia) {
        this.bancoManager = bancoManager;
        this.economia = economia;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo para jugadores.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 0) {
            return ayuda(p);
        }

        switch (args[0].toLowerCase()) {

            case "crear":
                if (args.length < 4 || !args[1].equalsIgnoreCase("banco")) {
                    p.sendMessage(ChatColor.RED + "Uso: /bmi crear banco <Nombre> <Etiqueta>");
                    return true;
                }
                String nombre = args[2], etiqueta = args[3];
                if (bancoManager.crearBanco(etiqueta, nombre, obtenerReino(p), p.getUniqueId())) {
                    p.sendMessage(ChatColor.GREEN +
                            "Banco '" + nombre + "' solicitado con etiqueta " + etiqueta);
                } else {
                    p.sendMessage(ChatColor.RED + "Error al crear solicitud de banco.");
                }
                break;

            case "pendientes":
                List<Banco> pendientes = bancoManager.obtenerBancosPendientes(obtenerReino(p));
                p.sendMessage(ChatColor.GOLD + "Solicitudes pendientes:");
                pendientes.forEach(b -> p.sendMessage(" - " + b.getEtiqueta()));
                break;

            case "aprobar":
                if (args.length != 2) {
                    p.sendMessage(ChatColor.RED + "Uso: /bmi aprobar <Etiqueta>");
                    return true;
                }
                if (bancoManager.aprobarBanco(args[1])) {
                    p.sendMessage(ChatColor.GREEN + "Banco " + args[1] + " aprobado");
                } else {
                    p.sendMessage(ChatColor.RED + "No se pudo aprobar el banco.");
                }
                break;

            case "rechazar":
                if (args.length != 2) {
                    p.sendMessage(ChatColor.RED + "Uso: /bmi rechazar <Etiqueta>");
                    return true;
                }
                if (bancoManager.rechazarBanco(args[1])) {
                    p.sendMessage(ChatColor.GREEN + "Banco " + args[1] + " rechazado");
                } else {
                    p.sendMessage(ChatColor.RED + "No se pudo rechazar el banco.");
                }
                break;

            case "listar":
                List<Banco> activos = bancoManager.obtenerBancosDeReino(obtenerReino(p));
                activos.forEach(b -> p.sendMessage(
                        ChatColor.AQUA + b.getEtiqueta() +
                                ChatColor.GRAY + " (" + b.getEstado() + ")"));
                break;

            case "unir":
            case "salir":
                if (args.length != 2) {
                    p.sendMessage(ChatColor.RED +
                            "Uso: /bmi " + args[0] + " <Etiqueta>");
                    return true;
                }
                boolean unido = args[0].equalsIgnoreCase("unir")
                        ? bancoManager.agregarSocio(args[1], p.getUniqueId())
                        : bancoManager.quitarSocio(args[1], p.getUniqueId());
                p.sendMessage(unido
                        ? ChatColor.GREEN + (args[0].equalsIgnoreCase("unir")
                        ? "Te uniste a " : "Saliste de ") + args[1]
                        : ChatColor.RED + "Error al ejecutar la acción.");
                break;

            case "saldo":
                if (args.length != 2) {
                    p.sendMessage(ChatColor.RED + "Uso: /bmi saldo <Etiqueta>");
                    return true;
                }
                double saldo = bancoManager.obtenerSaldo(args[1]);
                p.sendMessage(ChatColor.YELLOW +
                        "Saldo de " + args[1] + ": $" + saldo);
                break;

            case "depositar":
            case "retirar":
                if (args.length != 3) {
                    p.sendMessage(ChatColor.RED +
                            "Uso: /bmi " + args[0] + " <Etiqueta> <Monto>");
                    return true;
                }
                String tag = args[1];
                double monto;
                try {
                    monto = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    p.sendMessage(ChatColor.RED + "Monto inválido.");
                    return true;
                }
                boolean ok;
                if (args[0].equalsIgnoreCase("depositar")) {
                    if (economia.getBalance(p) < monto) {
                        p.sendMessage(ChatColor.RED + "No tienes suficiente dinero.");
                        return true;
                    }
                    economia.withdrawPlayer(p, monto);
                    ok = bancoManager.depositar(tag, monto);
                } else {
                    ok = bancoManager.retirar(tag, monto);
                }
                p.sendMessage(ok
                        ? ChatColor.GREEN + "Operación completada."
                        : ChatColor.RED + "Error en la operación.");
                break;

            default:
                return ayuda(p);
        }
        return true;
    }

    private boolean ayuda(Player p) {
        p.sendMessage(ChatColor.GOLD + "–– Comandos de Banco ––");
        p.sendMessage("/bmi crear banco <Nombre> <Etiqueta>");
        p.sendMessage("/bmi pendientes");
        p.sendMessage("/bmi aprobar <Etiqueta>");
        p.sendMessage("/bmi rechazar <Etiqueta>");
        p.sendMessage("/bmi listar");
        p.sendMessage("/bmi unir <Etiqueta>");
        p.sendMessage("/bmi salir <Etiqueta>");
        p.sendMessage("/bmi saldo <Etiqueta>");
        p.sendMessage("/bmi depositar <Etiqueta> <Monto>");
        p.sendMessage("/bmi retirar <Etiqueta> <Monto>");
        return true;
    }

    private String obtenerReino(Player p) {
        return bancoManager.obtenerReinoJugador(p.getUniqueId());
    }
}
