package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandosBMI implements CommandExecutor {

    private final GestorBaseDeDatos db;
    private final Economy economia;

    public ComandosBMI(MenuInteractivo plugin) {
        this.db = plugin.getBaseDeDatos();           // antes: getGestorBaseDeDatos()
        this.economia = plugin.getEconomia();
        // El registro del executor lo debes hacer solo en onEnable():
        // plugin.getCommand("bmi").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores.");
            return true;
        }
        Player jugador = (Player) sender;

        if (args.length == 0) {
            jugador.sendMessage(ChatColor.YELLOW + "Usa /bmi ayuda para ver los comandos disponibles.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "crear":
                if (args.length < 4 || !args[1].equalsIgnoreCase("banco")) {
                    jugador.sendMessage(ChatColor.RED + "Uso correcto: /bmi crear banco <Nombre> <Etiqueta>");
                    return true;
                }
                // Lógica pendiente de creación (inserta en tu tabla de solicitudes)
                jugador.sendMessage(ChatColor.GREEN + "Solicitud de creación de banco enviada.");
                break;

            case "banco":
                if (args.length == 2) {
                    // /bmi banco <etiqueta>
                    jugador.sendMessage(ChatColor.YELLOW + "Abriendo menú del banco " + args[1]);
                    // Aquí abres tu GUI de banco...
                } else if (args.length == 3 && args[1].equalsIgnoreCase("cuenta")) {
                    // /bmi banco cuenta <etiqueta>
                    double saldo = db.obtenerSaldoBanco(args[2]);  // antes: obtenerFondosBanco
                    if (saldo < 0) {
                        jugador.sendMessage(ChatColor.RED +
                                "El banco con etiqueta " + args[2] + " no existe o no está aprobado.");
                    } else {
                        jugador.sendMessage(ChatColor.GREEN +
                                "El banco " + args[2] + " tiene $" + saldo + " en fondos.");
                    }
                } else {
                    jugador.sendMessage(ChatColor.RED +
                            "Uso correcto: /bmi banco <etiqueta> o /bmi banco cuenta <etiqueta>");
                }
                break;

            case "bancos":
                // /bmi bancos
                jugador.sendMessage(ChatColor.GREEN + "Mostrando bancos disponibles de tu reino...");
                break;

            case "mibanco":
                if (args.length < 3) {
                    jugador.sendMessage(ChatColor.RED +
                            "Uso correcto: /bmi mibanco <imprimir|vender|quemar> <cantidad>");
                    return true;
                }
                String accion = args[1].toLowerCase();
                double cantidad;
                try {
                    cantidad = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    jugador.sendMessage(ChatColor.RED + "La cantidad debe ser un número válido.");
                    return true;
                }

                // necesitas implementar en GestorBaseDeDatos:
                // public String obtenerBancoDeJugador(UUID jugadorUUID)
                String etiquetaBanco = db.obtenerBancoDeJugador(jugador.getUniqueId());
                if (etiquetaBanco == null) {
                    jugador.sendMessage(ChatColor.RED + "No estás vinculado a ningún banco.");
                    return true;
                }

                switch (accion) {
                    case "imprimir":
                        // imprimir = aumentar fondos del banco
                        if (db.depositarEnBanco(etiquetaBanco, cantidad)) {
                            jugador.sendMessage(ChatColor.GREEN +
                                    "Se imprimieron " + cantidad + " monedas para el banco " + etiquetaBanco);
                        } else {
                            jugador.sendMessage(ChatColor.RED +
                                    "Error al imprimir moneda en el banco.");
                        }
                        break;

                    case "vender":
                        // vender = jugador da saldo al banco
                        if (economia.getBalance(jugador) >= cantidad) {
                            economia.withdrawPlayer(jugador, cantidad);
                            if (db.depositarEnBanco(etiquetaBanco, cantidad)) {
                                jugador.sendMessage(ChatColor.GREEN +
                                        "Convertiste $" + cantidad + " en moneda del banco " + etiquetaBanco);
                            } else {
                                jugador.sendMessage(ChatColor.RED +
                                        "Error al acreditar fondos al banco.");
                            }
                        } else {
                            jugador.sendMessage(ChatColor.RED +
                                    "No tienes suficiente dinero para vender esa cantidad.");
                        }
                        break;

                    case "quemar":
                        // quemar = retirar fondos del banco
                        if (db.retirarDeBanco(etiquetaBanco, cantidad)) {
                            jugador.sendMessage(ChatColor.YELLOW +
                                    "Has quemado " + cantidad + " monedas del banco " + etiquetaBanco);
                        } else {
                            jugador.sendMessage(ChatColor.RED +
                                    "Error al quemar moneda o fondos insuficientes en el banco.");
                        }
                        break;

                    default:
                        jugador.sendMessage(ChatColor.RED +
                                "Acción no reconocida. Usa imprimir, vender o quemar.");
                        break;
                }
                break;

            default:
                jugador.sendMessage(ChatColor.YELLOW +
                        "Comando no reconocido. Usa /bmi ayuda para más información.");
                break;
        }

        return true;
    }
}
