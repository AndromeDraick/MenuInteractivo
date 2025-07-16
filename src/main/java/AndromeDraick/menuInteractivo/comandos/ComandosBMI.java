package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandosBMI implements CommandExecutor {

    private final MenuInteractivo plugin;
    private final GestorBaseDeDatos db;

    public ComandosBMI(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.db = plugin.getGestorBaseDeDatos();
        plugin.getCommand("bmi").setExecutor(this);
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
                // Aquí va la lógica para crear un banco pendiente de aprobación
                jugador.sendMessage(ChatColor.GREEN + "Solicitud de creación de banco enviada.");
                break;

            case "banco":
                if (args.length == 2) {
                    // /bmi banco <etiqueta>
                    jugador.sendMessage(ChatColor.YELLOW + "Abriendo menú del banco " + args[1]);
                    // Abrir menú individual del banco
                } else if (args.length == 3 && args[1].equalsIgnoreCase("cuenta")) {
                    // /bmi banco cuenta <etiqueta>
                    double saldo = db.obtenerFondosBanco(args[2]);
                    if (saldo == -1) {
                        jugador.sendMessage(ChatColor.RED + "El banco con etiqueta " + args[2] + " no existe o no está aprobado.");
                    } else {
                        jugador.sendMessage(ChatColor.GREEN + "El banco " + args[2] + " tiene $" + saldo + " en fondos.");
                    }
                } else {
                    jugador.sendMessage(ChatColor.RED + "Uso correcto: /bmi banco <etiqueta> o /bmi banco cuenta <etiqueta>");
                }
                break;

            case "bancos":
                // Abrir menú de bancos aceptados por el reino del jugador
                jugador.sendMessage(ChatColor.GREEN + "Mostrando bancos disponibles de tu reino...");
                break;

            case "mibanco":
                if (args.length < 3) {
                    jugador.sendMessage(ChatColor.RED + "Uso correcto: /bmi mibanco <imprimir|vender|quemar> <cantidad>");
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

                String etiquetaBanco = db.obtenerBancoDeJugador(jugador.getUniqueId());
                if (etiquetaBanco == null) {
                    jugador.sendMessage(ChatColor.RED + "No estás vinculado a ningún banco.");
                    return true;
                }

                switch (accion) {
                    case "imprimir":
                        db.actualizarFondosBanco(etiquetaBanco, cantidad);
                        jugador.sendMessage(ChatColor.GREEN + "Se imprimieron " + cantidad + " monedas para el banco " + etiquetaBanco);
                        break;
                    case "vender":
                        if (plugin.getEconomia().getBalance(jugador) >= cantidad) {
                            plugin.getEconomia().withdrawPlayer(jugador, cantidad);
                            db.actualizarFondosBanco(etiquetaBanco, cantidad);
                            jugador.sendMessage(ChatColor.GREEN + "Convertiste $" + cantidad + " en moneda del banco " + etiquetaBanco);
                        } else {
                            jugador.sendMessage(ChatColor.RED + "No tienes suficiente dinero para vender esa cantidad.");
                        }
                        break;
                    case "quemar":
                        db.actualizarFondosBanco(etiquetaBanco, -cantidad);
                        jugador.sendMessage(ChatColor.YELLOW + "Has quemado " + cantidad + " monedas del banco " + etiquetaBanco);
                        break;
                    default:
                        jugador.sendMessage(ChatColor.RED + "Acción no reconocida. Usa imprimir, vender o quemar.");
                        break;
                }
                break;

            default:
                jugador.sendMessage(ChatColor.YELLOW + "Comando no reconocido. Usa /bmi ayuda para más información.");
                break;
        }

        return true;
    }
}
