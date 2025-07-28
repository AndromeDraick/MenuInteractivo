package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class ComandoRMI implements CommandExecutor, TabCompleter {

    private final GestorBaseDeDatos db;
    private final Map<UUID, RelacionPendiente> solicitudes = new HashMap<>();

    public ComandoRMI(MenuInteractivo plugin) {
        this.db = plugin.getBaseDeDatos();
        plugin.getCommand("rmi").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        if (args.length >= 2) {
            // --- SUBCOMANDO: registro rol ---
            if (args[0].equalsIgnoreCase("registro") && args[1].equalsIgnoreCase("rol")) {
                return procesarRegistroRol(jugador, args);
            }

            // --- SUBCOMANDO: aprobar familiar <nick> ---
            if (args.length == 3 && args[0].equalsIgnoreCase("aprobar") && args[1].equalsIgnoreCase("familiar")) {
                return procesarRespuestaSolicitud(jugador, args[2], true);
            }

            // --- SUBCOMANDO: rechazar familiar <nick> ---
            if (args.length == 3 && args[0].equalsIgnoreCase("rechazar") && args[1].equalsIgnoreCase("familiar")) {
                return procesarRespuestaSolicitud(jugador, args[2], false);
            }

            // --- SUBCOMANDO: <relacion> de <jugador> ---
            if (args.length >= 3 && args[1].equalsIgnoreCase("de")) {
                return solicitarRelacionFamiliar(jugador, args);
            }
            // --- SUBCOMANDO: editar rol <jugador> <...>
            if (args[0].equalsIgnoreCase("editar") && args[1].equalsIgnoreCase("rol") && args.length >= 9) {
                if (!jugador.hasPermission("menuinteractivo.registro.admin")) {
                    jugador.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    return true;
                }
                return procesarEdicionRol(jugador, args);
            }

        }

        mostrarAyuda(jugador);
        return true;
    }

    // ========== REGISTRO DE ROL ==========
    private boolean procesarRegistroRol(Player jugador, String[] args) {
        if (args.length < 8) {
            jugador.sendMessage(ChatColor.YELLOW + "Uso: /rmi registro rol <genero> <nombre> <apellidoP> <apellidoM> <descendencia> <raza>");
            return true;
        }

        // Verificar si ya tiene rol registrado
        String generoExistente = db.getGenero(jugador.getUniqueId());
        if (generoExistente != null && !generoExistente.isEmpty()) {
            jugador.sendMessage(ChatColor.RED + "Ya tienes un rol registrado. No puedes registrarte nuevamente.");
            return true;
        }

        String genero = args[2];
        String nombre = args[3].replace("_", " ");
        String apellidoPaterno = args[4].replace("_", " ");
        String apellidoMaterno = args[5].replace("_", " ");
        String descendencia = args[6];
        String raza = args[7];

        boolean exito = db.registrarRolCompleto(jugador.getUniqueId(), genero, nombre, apellidoPaterno, apellidoMaterno, descendencia, raza);

        if (exito) {
            jugador.sendMessage(ChatColor.GREEN + "¡Registro de rol completado correctamente!");
        } else {
            jugador.sendMessage(ChatColor.RED + "Ocurrió un error al registrar tu rol.");
        }

        return true;
    }

    // ========== SOLICITUD DE RELACIÓN ==========
    private boolean solicitarRelacionFamiliar(Player jugador, String[] args) {
        String tipoRelacion = args[0].toLowerCase(Locale.ROOT);

        if (!List.of("hijo", "hija", "hermano", "hermana", "esposo", "esposa").contains(tipoRelacion)) {
            jugador.sendMessage(ChatColor.RED + "Tipo de relación inválido. Usa hijo(a), hermano(a) o esposo(a).");
            return true;
        }

        OfflinePlayer objetivo = Bukkit.getOfflinePlayer(args[2]);
        if (objetivo == null || !objetivo.hasPlayedBefore()) {
            jugador.sendMessage(ChatColor.RED + "El jugador especificado no existe o no ha jugado antes.");
            return true;
        }

        UUID uuidObjetivo = objetivo.getUniqueId();
        if (objetivo.isOnline()) {
            Player jugadorObjetivo = (Player) objetivo;
            jugadorObjetivo.sendMessage(ChatColor.GOLD + jugador.getName() + " quiere ser tu " + tipoRelacion + ".");
            jugadorObjetivo.sendMessage(ChatColor.YELLOW + "Usa /rmi aprobar familiar " + jugador.getName() + " para aceptar.");
            jugadorObjetivo.sendMessage(ChatColor.RED + "O usa /rmi rechazar familiar " + jugador.getName() + " para rechazar.");
        }

        solicitudes.put(uuidObjetivo, new RelacionPendiente(jugador.getUniqueId(), tipoRelacion));
        jugador.sendMessage(ChatColor.GREEN + "Solicitud enviada a " + args[2] + ".");
        return true;
    }

    //=========

    private boolean procesarEdicionRol(Player ejecutor, String[] args) {
        OfflinePlayer objetivo = Bukkit.getOfflinePlayer(args[2]);
        if (objetivo == null || !objetivo.hasPlayedBefore()) {
            ejecutor.sendMessage(ChatColor.RED + "Ese jugador no existe o no ha ingresado antes.");
            return true;
        }
        if (ejecutor.hasPermission("menuinteractivo.comando.registro.admin")) {
            ejecutor.sendMessage(ChatColor.GRAY + "/rmi editar rol <jugador> <género> <nombre> <apellidoP> <apellidoM> <descendencia> <raza>");
        }

        String genero = args[3];
        String nombre = args[4];
        String apellidoPaterno = args[5];
        String apellidoMaterno = args[6];
        String descendencia = args[7];
        String raza = args[8];

        boolean exito = db.registrarRolCompleto(objetivo.getUniqueId(), genero, nombre, apellidoPaterno, apellidoMaterno, descendencia, raza);

        if (exito) {
            ejecutor.sendMessage(ChatColor.GREEN + "Ficha de rol editada correctamente para " + objetivo.getName() + ".");
            if (objetivo.isOnline()) {
                ((Player) objetivo).sendMessage(ChatColor.YELLOW + "Tu ficha de rol ha sido editada por un administrador.");
            }
        } else {
            ejecutor.sendMessage(ChatColor.RED + "Ocurrió un error al editar la ficha de rol.");
        }

        return true;
    }

    // ========== RESPUESTA A SOLICITUD ==========
    private boolean procesarRespuestaSolicitud(Player jugador, String nickSolicitante, boolean aprobar) {
        OfflinePlayer solicitante = Bukkit.getOfflinePlayer(nickSolicitante);
        if (solicitante == null || !solicitudes.containsKey(jugador.getUniqueId())) {
            jugador.sendMessage(ChatColor.RED + "No tienes ninguna solicitud pendiente de ese jugador.");
            return true;
        }

        RelacionPendiente pendiente = solicitudes.get(jugador.getUniqueId());
        if (!pendiente.uuidSolicitante.equals(solicitante.getUniqueId())) {
            jugador.sendMessage(ChatColor.RED + "Ese jugador no te ha enviado una solicitud.");
            return true;
        }

        solicitudes.remove(jugador.getUniqueId());

        if (!aprobar) {
            jugador.sendMessage(ChatColor.YELLOW + "Rechazaste la solicitud de " + nickSolicitante + ".");
            if (solicitante.isOnline())
                ((Player) solicitante).sendMessage(ChatColor.RED + jugador.getName() + " rechazó tu solicitud.");
            return true;
        }

        String nombreCompleto = db.getNombreCompletoRol(jugador.getUniqueId());
        if (nombreCompleto == null) {
            jugador.sendMessage(ChatColor.RED + "No se encontró tu ficha de rol.");
            return true;
        }

        String lineaRelacion = ChatColor.GRAY + capitalizar(pendiente.tipoRelacion) + " de: " + ChatColor.AQUA + nombreCompleto;
        db.agregarRelacionFamiliar(pendiente.uuidSolicitante, ChatColor.stripColor(lineaRelacion));

        jugador.sendMessage(ChatColor.GREEN + "Has aprobado la relación con " + nickSolicitante + ".");
        if (solicitante.isOnline())
            ((Player) solicitante).sendMessage(ChatColor.GREEN + jugador.getName() + " ha aprobado la relación como " + pendiente.tipoRelacion + ".");

        return true;
    }

    // ========== AYUDA ==========
    private void mostrarAyuda(Player jugador) {
        jugador.sendMessage(ChatColor.YELLOW + "Uso:");
        jugador.sendMessage(ChatColor.GRAY + "/rmi registro rol <genero> <nombre> <apellidoP> <apellidoM> <descendencia> <raza>");
        jugador.sendMessage(ChatColor.GRAY + "/rmi <hijo(a)|hermano(a)|esposo(a)> de <jugador>");
        jugador.sendMessage(ChatColor.GRAY + "/rmi aprobar familiar <nick>");
        jugador.sendMessage(ChatColor.GRAY + "/rmi rechazar familiar <nick>");
    }

    private String capitalizar(String palabra) {
        return palabra.substring(0, 1).toUpperCase() + palabra.substring(1);
    }

    private static class RelacionPendiente {
        UUID uuidSolicitante;
        String tipoRelacion;

        public RelacionPendiente(UUID uuid, String tipoRelacion) {
            this.uuidSolicitante = uuid;
            this.tipoRelacion = tipoRelacion;
        }
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player jugador = (Player) sender;

        if (args.length == 1) {
            return List.of("registro", "hijo", "hija", "hermano", "hermana", "esposo", "esposa", "aprobar", "rechazar").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        // /rmi registro ...
        if (args[0].equalsIgnoreCase("registro")) {
            if (args.length == 2) return List.of("rol").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase())).toList();

            if (args[1].equalsIgnoreCase("rol")) {
                return switch (args.length) {
                    case 3 -> List.of("masculino", "femenino", "otro").stream()
                            .filter(s -> s.startsWith(args[2].toLowerCase())).toList();
                    case 4 -> List.of("<Nombre>");
                    case 5 -> List.of("<ApellidoPaterno>");
                    case 6 -> List.of("<ApellidoMaterno>");
                    case 7 -> List.of("nobleza", "militar", "plebeyo", "extranjero").stream()
                            .filter(s -> s.startsWith(args[6].toLowerCase())).toList();
                    case 8 -> List.of("humano", "elfo", "vampiro", "semienderman", "semidragon", "aracnohuman", "atlantiano").stream()
                            .filter(s -> s.startsWith(args[7].toLowerCase())).toList();
                    default -> Collections.emptyList();
                };
            }
        }

        // /rmi hijo de <jugador>
        if (args.length == 2 && List.of("hijo", "hija", "hermano", "hermana", "esposo", "esposa").contains(args[0].toLowerCase())) {
            return args[1].isEmpty() || "de".startsWith(args[1].toLowerCase()) ? List.of("de") : Collections.emptyList();
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("de")) {
            return null; // Lista de jugadores online/offline
        }

        // /rmi aprobar familiar <nick>
        if (args.length == 2 && args[0].equalsIgnoreCase("aprobar")) {
            return args[1].isEmpty() || "familiar".startsWith(args[1].toLowerCase()) ? List.of("familiar") : Collections.emptyList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("aprobar") && args[1].equalsIgnoreCase("familiar")) {
            return null; // Lista de jugadores
        }

        // /rmi rechazar familiar <nick>
        if (args.length == 2 && args[0].equalsIgnoreCase("rechazar")) {
            return args[1].isEmpty() || "familiar".startsWith(args[1].toLowerCase()) ? List.of("familiar") : Collections.emptyList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("rechazar") && args[1].equalsIgnoreCase("familiar")) {
            return null; // Lista de jugadores
        }

        // /rmi editar rol <jugador> ...
        if (args[0].equalsIgnoreCase("editar") && args.length == 2) {
            return List.of("rol").stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("editar") && args[1].equalsIgnoreCase("rol")) {
            return null; // lista de jugadores
        }
        if (args[0].equalsIgnoreCase("editar") && args[1].equalsIgnoreCase("rol")) {
            return switch (args.length) {
                case 4 -> List.of("masculino", "femenino", "otro").stream()
                        .filter(s -> s.startsWith(args[3].toLowerCase())).toList();
                case 5 -> List.of("<Nombre>");
                case 6 -> List.of("<ApellidoPaterno>");
                case 7 -> List.of("<ApellidoMaterno>");
                case 8 -> List.of("nobleza", "militar", "plebeyo", "extranjero").stream()
                        .filter(s -> s.startsWith(args[7].toLowerCase())).toList();
                case 9 -> List.of("humano", "elfo", "vampiro", "semienderman", "semidragon", "aracnohuman", "atlantiano").stream()
                        .filter(s -> s.startsWith(args[8].toLowerCase())).toList();
                default -> Collections.emptyList();
            };
        }

        return Collections.emptyList();
    }
}
