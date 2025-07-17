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
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }
        Player jugador = (Player) sender;

        if (args.length == 0) {
            mostrarAyuda(jugador);
            return true;
        }

        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "crear":
                    handleCrearBanco(jugador, args);
                    break;
                case "banco":
                    handleBanco(jugador, args);
                    break;
                case "bancos":
                    handleListarBancos(jugador);
                    break;
                case "mibanco":
                    handleMiBanco(jugador, args);
                    break;
                case "ayuda":
                    mostrarAyuda(jugador);
                    break;
                default:
                    jugador.sendMessage(ChatColor.RED + "Subcomando desconocido.");
                    mostrarAyuda(jugador);
            }
        } catch (Exception e) {
            jugador.sendMessage(ChatColor.RED + "Ocurrió un error al ejecutar el comando.");
            plugin.getLogger().severe("Error en ComandosBMI: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private void handleCrearBanco(Player p, String[] args) {
        // /bmi crear banco <Nombre> <Etiqueta>
        if (args.length < 4 || !args[1].equalsIgnoreCase("banco")) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi crear banco <Nombre> <Etiqueta>");
            return;
        }
        String nombre = args[2];
        String etiqueta = args[3].toLowerCase();

        if (!etiqueta.matches("[a-z0-9_-]+")) {
            p.sendMessage(ChatColor.RED + "La etiqueta solo puede contener minúsculas, números, '-' o '_'.");
            return;
        }
        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }
        if (bancoManager.crearBanco(etiqueta, nombre, reino, p.getUniqueId())) {
            p.sendMessage(ChatColor.GREEN + "Solicitud de creación de banco '" + nombre + "' enviada.");
        } else {
            p.sendMessage(ChatColor.RED + "Error al crear la solicitud de banco. Revisa la consola.");
        }
    }

    private void handleBanco(Player p, String[] args) {
        // /bmi banco <etiqueta>
        // /bmi banco cuenta <etiqueta>
        if (args.length == 2) {
            String etiqueta = args[1].toLowerCase();
            p.sendMessage(ChatColor.YELLOW + "Abriendo interfaz del banco '" + etiqueta + "'...");
            // Ejemplo: plugin.getMenuBancos().abrirMenuBanco(p, etiqueta);
        } else if (args.length == 3 && args[1].equalsIgnoreCase("cuenta")) {
            String etiqueta = args[2].toLowerCase();
            double saldo = bancoManager.obtenerSaldo(etiqueta);
            p.sendMessage(saldo >= 0
                    ? ChatColor.GREEN + "El banco '" + etiqueta + "' tiene $" + saldo
                    : ChatColor.RED + "El banco '" + etiqueta + "' no existe o no está aprobado.");
        } else {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi banco <etiqueta> | /bmi banco cuenta <etiqueta>");
        }
    }

    private void handleListarBancos(Player p) {
        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }
        List<Banco> bancos = bancoManager.obtenerBancosDeReino(reino);
        if (bancos.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay bancos aprobados en tu reino.");
            return;
        }
        p.sendMessage(ChatColor.GOLD + "Bancos aprobados en '" + reino + "':");
        bancos.forEach(b -> p.sendMessage(
                ChatColor.AQUA + b.getEtiqueta() +
                        ChatColor.GRAY + " - " + b.getNombre() +
                        ChatColor.WHITE + " ($" + b.getFondos() + ")"
        ));
    }

    private void handleMiBanco(Player p, String[] args) {
        // /bmi mibanco <imprimir|vender|quemar> <cantidad>
        if (args.length != 3) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi mibanco <imprimir|vender|quemar> <cantidad>");
            return;
        }
        String accion = args[1].toLowerCase();
        double monto;
        try {
            monto = Double.parseDouble(args[2]);
            if (monto <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            p.sendMessage(ChatColor.RED + "La cantidad debe ser un número positivo.");
            return;
        }

        String etiqueta = bancoManager.obtenerBancoDeJugador(p.getUniqueId());
        if (etiqueta == null) {
            p.sendMessage(ChatColor.RED + "No estás vinculado a ningún banco.");
            return;
        }

        switch (accion) {
            case "imprimir":
                if (bancoManager.depositar(etiqueta, monto)) {
                    p.sendMessage(ChatColor.GREEN + "Se imprimieron " + monto + " monedas en el banco '" + etiqueta + "'.");
                } else {
                    p.sendMessage(ChatColor.RED + "Error al imprimir moneda en el banco.");
                }
                break;

            case "vender":
                if (economia.getBalance(p) < monto) {
                    p.sendMessage(ChatColor.RED + "No tienes suficiente dinero.");
                    return;
                }
                economia.withdrawPlayer(p, monto);
                if (bancoManager.depositar(etiqueta, monto)) {
                    p.sendMessage(ChatColor.GREEN + "Convertiste $" + monto + " en moneda del banco '" + etiqueta + "'.");
                } else {
                    p.sendMessage(ChatColor.RED + "Error al acreditar fondos en el banco.");
                }
                break;

            case "quemar":
                if (bancoManager.retirar(etiqueta, monto)) {
                    p.sendMessage(ChatColor.YELLOW + "Has quemado " + monto + " monedas del banco '" + etiqueta + "'.");
                } else {
                    p.sendMessage(ChatColor.RED + "Error al quemar moneda o fondos insuficientes.");
                }
                break;

            default:
                p.sendMessage(ChatColor.RED + "Acción desconocida. Usa imprimir, vender o quemar.");
        }
    }

    private void mostrarAyuda(Player p) {
        p.sendMessage(ChatColor.GOLD + "— Comandos de Banco —");
        p.sendMessage(ChatColor.YELLOW + "/bmi crear banco <Nombre> <Etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/bmi banco <Etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/bmi banco cuenta <Etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/bmi bancos");
        p.sendMessage(ChatColor.YELLOW + "/bmi mibanco <imprimir|vender|quemar> <Cantidad>");
        p.sendMessage(ChatColor.YELLOW + "/bmi ayuda");
    }
}
