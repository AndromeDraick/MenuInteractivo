package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.MenuInteractivo;
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

public class ComandosBMI implements CommandExecutor {

    private final MenuInteractivo plugin;
    private final BancoManager bancoManager;
    private final Economy economia;

    public ComandosBMI(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos());
        this.economia = plugin.getEconomia();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede usarlo un jugador.");
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("ayuda")) {
            mostrarAyuda(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "crear":
                    cmdCrearBanco(p, args);
                    break;
                case "pendientes":
                    cmdListarPendientes(p);
                    break;
                case "aprobar":
                    cmdAprobarRechazar(p, args, true);
                    break;
                case "rechazar":
                    cmdAprobarRechazar(p, args, false);
                    break;
                case "listar":
                    cmdListarActivos(p);
                    break;
                case "unir":
                    cmdUnirSalir(p, args, true);
                    break;
                case "salir":
                    cmdUnirSalir(p, args, false);
                    break;
                case "saldo":
                    cmdSaldo(p, args);
                    break;
                case "depositar":
                case "retirar":
                    cmdMoverFondos(p, args, sub.equals("depositar"));
                    break;
                case "banco":
                    cmdBanco(p, args);
                    break;
                default:
                    p.sendMessage(ChatColor.RED + "Subcomando desconocido.");
                    mostrarAyuda(p);
            }
        } catch (Exception e) {
            p.sendMessage(ChatColor.RED + "Error al ejecutar el comando, mira la consola.");
            plugin.getLogger().severe("Error en ComandosBMI: " + e);
        }
        return true;
    }

    private void cmdCrearBanco(Player p, String[] args) {
        // /bmi crear banco <Nombre> <Etiqueta>
        if (args.length < 4 || !args[1].equalsIgnoreCase("banco")) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi crear banco <Nombre> <Etiqueta>");
            return;
        }
        String nombre = args[2];
        String etiqueta = args[3].toLowerCase();
        if (!etiqueta.matches("[a-z0-9_-]+")) {
            p.sendMessage(ChatColor.RED + "Etiqueta: minúsculas, números, '-' o '_'.");
            return;
        }
        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }
        if (bancoManager.crearBanco(etiqueta, nombre, reino, p.getUniqueId())) {
            p.sendMessage(ChatColor.GREEN + "Solicitud de banco '" + nombre + "' enviada.");
        } else {
            p.sendMessage(ChatColor.RED + "Error al solicitar banco. Revisa la consola.");
        }
    }

    private void cmdListarPendientes(Player p) {
        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }
        List<Banco> pendientes = bancoManager.obtenerBancosPendientes(reino);
        if (pendientes.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay solicitudes pendientes.");
            return;
        }
        p.sendMessage(ChatColor.GOLD + "Solicitudes pendientes:");
        pendientes.forEach(b -> p.sendMessage(" - " + b.getEtiqueta() + " (" + b.getNombre() + ")"));
    }

    private void cmdAprobarRechazar(Player p, String[] args, boolean aprobar) {
        // /bmi aprobar|rechazar <Etiqueta>
        if (args.length != 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi " + (aprobar ? "aprobar" : "rechazar") + " <Etiqueta>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        boolean ok = aprobar ? bancoManager.aprobarBanco(etiqueta)
                : bancoManager.rechazarBanco(etiqueta);
        if (ok) {
            p.sendMessage(ChatColor.GREEN + "Banco " + etiqueta + (aprobar ? " aprobado." : " rechazado."));
        } else {
            p.sendMessage(ChatColor.RED + "No se pudo " + (aprobar ? "aprobar " : "rechazar ") + etiqueta + ".");
        }
    }

    private void cmdListarActivos(Player p) {
        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }
        List<Banco> activos = bancoManager.obtenerBancosDeReino(reino);
        if (activos.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay bancos activos en tu reino.");
            return;
        }
        p.sendMessage(ChatColor.GOLD + "Bancos activos en '" + reino + "':");
        activos.forEach(b -> p.sendMessage(
                ChatColor.AQUA + b.getEtiqueta() + ChatColor.GRAY + " - " + b.getNombre()
        ));
    }

    private void cmdUnirSalir(Player p, String[] args, boolean unir) {
        // /bmi unir|salir <Etiqueta>
        if (args.length != 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi " + (unir ? "unir" : "salir") + " <Etiqueta>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        boolean ok = unir
                ? bancoManager.agregarSocio(etiqueta, p.getUniqueId())
                : bancoManager.quitarSocio(etiqueta, p.getUniqueId());
        if (ok) {
            p.sendMessage(ChatColor.GREEN + (unir ? "Te uniste a " : "Saliste de ") + etiqueta);
        } else {
            p.sendMessage(ChatColor.RED + "Error al " + (unir ? "unirte a " : "salir de ") + etiqueta);
        }
    }

    private void cmdSaldo(Player p, String[] args) {
        // /bmi saldo <Etiqueta>
        if (args.length != 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi saldo <Etiqueta>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        double saldo = bancoManager.obtenerSaldo(etiqueta);
        p.sendMessage(saldo >= 0
                ? ChatColor.GREEN + "Saldo de " + etiqueta + ": $" + saldo
                : ChatColor.RED + "Banco '" + etiqueta + "' no existe o no está aprobado.");
    }

    private void cmdMoverFondos(Player p, String[] args, boolean depositar) {
        // /bmi depositar|retirar <Etiqueta> <Monto>
        if (args.length != 3) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi " + (depositar ? "depositar" : "retirar") + " <Etiqueta> <Monto>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        double monto;
        try {
            monto = Double.parseDouble(args[2]);
            if (monto <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            p.sendMessage(ChatColor.RED + "Cantidad inválida, debe ser positiva.");
            return;
        }

        if (depositar) {
            if (economia.getBalance(p) < monto) {
                p.sendMessage(ChatColor.RED + "No tienes suficiente dinero en tu cuenta personal.");
                return;
            }
            economia.withdrawPlayer(p, monto);
            if (bancoManager.depositar(etiqueta, monto)) {
                p.sendMessage(ChatColor.GREEN + "Depositaste $" + monto + " en " + etiqueta);
            } else {
                p.sendMessage(ChatColor.RED + "Error al depositar en " + etiqueta);
            }
        } else {
            if (bancoManager.retirar(etiqueta, monto)) {
                p.sendMessage(ChatColor.YELLOW + "Retiraste $" + monto + " de " + etiqueta);
                economia.depositPlayer(p, monto);
            } else {
                p.sendMessage(ChatColor.RED + "Error al retirar de " + etiqueta);
            }
        }
    }

    private void cmdBanco(Player p, String[] args) {
        // /bmi banco <Etiqueta> | /bmi banco cuenta <Etiqueta>
        if (args.length == 2) {
            String etiqueta = args[1].toLowerCase();
            plugin.getMenuBancos().abrirMenuSolicitudes(p); // o .abrirMenuBanco(p, etiqueta)
        } else if (args.length == 3 && args[1].equalsIgnoreCase("cuenta")) {
            cmdSaldo(p, new String[] {"saldo", args[2]});
        } else {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi banco <Etiqueta> | /bmi banco cuenta <Etiqueta>");
        }
    }

    private void mostrarAyuda(Player p) {
        p.sendMessage(ChatColor.GOLD + "— Comandos de Banco —");
        p.sendMessage(ChatColor.YELLOW + "/bmi crear banco <Nombre> <Etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/bmi pendientes");
        p.sendMessage(ChatColor.YELLOW + "/bmi aprobar <Etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/bmi rechazar <Etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/bmi listar");
        p.sendMessage(ChatColor.YELLOW + "/bmi unir <Etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/bmi salir <Etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/bmi saldo <Etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/bmi depositar <Etiqueta> <Monto>");
        p.sendMessage(ChatColor.YELLOW + "/bmi retirar <Etiqueta> <Monto>");
        p.sendMessage(ChatColor.YELLOW + "/bmi banco <Etiqueta> [cuenta]");
        p.sendMessage(ChatColor.YELLOW + "/bmi ayuda");
    }
}
