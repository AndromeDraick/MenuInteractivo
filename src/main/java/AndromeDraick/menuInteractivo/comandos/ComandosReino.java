package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.managers.ReinoManager;
import AndromeDraick.menuInteractivo.model.Reino;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ComandosReino implements CommandExecutor, TabCompleter {

    private final MenuInteractivo plugin;
    private final ReinoManager manager;

    // Subcomandos válidos para /rnmi
    private static final List<String> SUBS = List.of(
            "crear", "unir", "salir", "eliminar", "listar", "info", "transferir"
    );

    public ComandosReino(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.manager = new ReinoManager(plugin.getBaseDeDatos());
        PluginCommand cmd = plugin.getCommand("rnmi");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden usar este comando.");
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0) {
            mostrarAyuda(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "crear":
                    if (!p.hasPermission("menuinteractivo.reino.comandos.crear")) {
                        p.sendMessage(ChatColor.RED + "No tienes permiso para crear reinos.");
                        return true;
                    }
                    cmdCrear(p, args);
                    break;
                case "unir":
                    cmdUnir(p, args);
                    break;
                case "salir":
                    cmdSalir(p, args);
                    break;
                case "eliminar":
                    cmdEliminar(p, args);
                    break;
                case "listar":
                    cmdListar(p);
                    break;
                case "info":
                    cmdInfo(p, args);
                    break;
                case "transferir":
                    cmdTransferir(p, args);
                    break;
                default:
                    p.sendMessage(ChatColor.RED + "Subcomando desconocido.");
                    mostrarAyuda(p);
                    break;
            }
        } catch (Exception ex) {
            p.sendMessage(ChatColor.RED + "Ocurrió un error al ejecutar el comando. Revisa la consola.");
            plugin.getLogger().severe("Error en ComandosReino: " + ex.getMessage());
            ex.printStackTrace();
        }
        return true;
    }


    private void cmdCrear(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi crear <etiqueta> <nombre>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        String nombre   = args[2];

        // Validación de etiqueta
        if (!etiqueta.matches("[a-z0-9_-]+")) {
            p.sendMessage(ChatColor.RED + "La etiqueta solo puede contener minúsculas, números, guiones o guión bajo.");
            return;
        }
        // Comprobar duplicado
        List<Reino> todos = manager.listarReinos();
        if (todos.stream().anyMatch(r -> r.getEtiqueta().equalsIgnoreCase(etiqueta))) {
            p.sendMessage(ChatColor.RED + "Ya existe un reino con esa etiqueta.");
            return;
        }
        // Crear reino
        if (manager.crearReino(etiqueta, nombre, p.getUniqueId())) {
            // Asociar creador como miembro/rey
            manager.unirReino(p.getUniqueId(), etiqueta);
            p.sendMessage(ChatColor.GREEN + "Reino '" + nombre + "' creado con etiqueta '" + etiqueta + "' y ahora eres su rey.");
        } else {
            p.sendMessage(ChatColor.RED + "Error al crear el reino. ¿Has revisado la consola?");
        }
    }

    private void cmdUnir(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi unir <etiqueta>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        // Comprobar que existe
        List<String> etiquetas = manager.listarReinos()
                .stream()
                .map(Reino::getEtiqueta)
                .collect(Collectors.toList());
        if (!etiquetas.contains(etiqueta)) {
            p.sendMessage(ChatColor.RED + "No existe ningún reino con etiqueta '" + etiqueta + "'.");
            return;
        }
        // Comprobar que no estés ya en uno
        String miReino = manager.obtenerReinoJugador(p.getUniqueId());
        if (miReino != null) {
            p.sendMessage(ChatColor.RED + "Ya perteneces al reino '" + miReino + "'. Primero usa /rnmi salir.");
            return;
        }
        if (manager.unirReino(p.getUniqueId(), etiqueta)) {
            p.sendMessage(ChatColor.GREEN + "Te has unido al reino '" + etiqueta + "'.");
        } else {
            p.sendMessage(ChatColor.RED + "Error al unirte al reino. ¿Está en estado 'aprobado'?");
        }
    }

    private void cmdSalir(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi salir <etiqueta>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        String miReino = manager.obtenerReinoJugador(p.getUniqueId());
        if (!etiqueta.equalsIgnoreCase(miReino)) {
            p.sendMessage(ChatColor.RED + "No perteneces al reino '" + etiqueta + "'.");
            return;
        }
        if (manager.salirReino(p.getUniqueId(), etiqueta)) {
            p.sendMessage(ChatColor.GREEN + "Has salido del reino '" + etiqueta + "'.");
        } else {
            p.sendMessage(ChatColor.RED + "Error al salir del reino.");
        }
    }

    private void cmdEliminar(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi eliminar <etiqueta>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        // Solo el creador (rey) puede eliminar
        Reino r = manager.listarReinos().stream()
                .filter(x -> x.getEtiqueta().equalsIgnoreCase(etiqueta))
                .findFirst().orElse(null);
        if (r == null) {
            p.sendMessage(ChatColor.RED + "Reino '" + etiqueta + "' no encontrado.");
            return;
        }
        if (!r.getReyUUID().equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Solo el rey de este reino puede eliminarlo.");
            return;
        }
        if (manager.eliminarReino(etiqueta)) {
            p.sendMessage(ChatColor.GREEN + "Reino '" + etiqueta + "' eliminado.");
        } else {
            p.sendMessage(ChatColor.RED + "Error al eliminar el reino.");
        }
    }

    private void cmdListar(Player p) {
        List<Reino> reinos = manager.listarReinos();
        if (reinos.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay reinos creados.");
            return;
        }
        p.sendMessage(ChatColor.GOLD + "— Reinos registrados —");
        for (Reino r : reinos) {
            p.sendMessage(ChatColor.AQUA + r.getEtiqueta() + ChatColor.GRAY + ": " + r.getNombre());
        }
    }

    private void cmdInfo(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi info <etiqueta>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        Reino r = manager.listarReinos().stream()
                .filter(x -> x.getEtiqueta().equalsIgnoreCase(etiqueta))
                .findFirst().orElse(null);
        if (r == null) {
            p.sendMessage(ChatColor.RED + "Reino '" + etiqueta + "' no encontrado.");
            return;
        }
        // Muestra info básica
        p.sendMessage(ChatColor.GREEN + "— Información de " + r.getNombre() + " —");
        p.sendMessage(ChatColor.GRAY + "Etiqueta: " + r.getEtiqueta());
        p.sendMessage(ChatColor.GRAY + "Rey: " + r.getReyUUID());
        // Si quieres mostrar miembros, se necesitaría un método en manager que devuelva la lista de UUIDs
        // List<UUID> miembros = manager.obtenerMiembros(etiqueta);
        // p.sendMessage(ChatColor.GRAY + "Miembros: " + miembros.size());
    }

    private void cmdTransferir(Player p, String[] args) {
        // /rnmi transferir <etiqueta> <jugador>
        if (args.length < 3) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi transferir <etiqueta> <jugador>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        String targetName = args[2];
        Reino reino = manager.listarReinos().stream()
                .filter(r -> r.getEtiqueta().equalsIgnoreCase(etiqueta))
                .findFirst().orElse(null);
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "Reino '" + etiqueta + "' no encontrado.");
            return;
        }
        // Solo el rey actual puede transferir
        if (!reino.getReyUUID().equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Solo el rey puede transferir el liderazgo.");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(targetName);
        if (target == null || !manager.obtenerReinoJugador(target.getUniqueId())
                .equalsIgnoreCase(etiqueta)) {
            p.sendMessage(ChatColor.RED + "El jugador debe estar conectado y ser miembro de ese reino.");
            return;
        }
        // Transfiere liderazgo
        if (manager.transferirLiderazgo(etiqueta, target.getUniqueId())) {
            p.sendMessage(ChatColor.GREEN +
                    "Has transferido el liderazgo de '" + etiqueta + "' a " + targetName + ".");
            target.sendMessage(ChatColor.GREEN +
                    "Ahora eres el rey del reino '" + etiqueta + "'.");
        } else {
            p.sendMessage(ChatColor.RED + "Error al transferir liderazgo. Revisa la consola.");
        }
    }


    private void mostrarAyuda(Player p) {
        p.sendMessage(ChatColor.GOLD + "— Comandos de Reinos —");
        p.sendMessage(ChatColor.YELLOW + "/rnmi crear <etiqueta> <nombre>");
        p.sendMessage(ChatColor.YELLOW + "/rnmi unir <etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/rnmi salir <etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/rnmi eliminar <etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/rnmi listar");
        p.sendMessage(ChatColor.YELLOW + "/rnmi info <etiqueta>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player p = (Player) sender;

        // Primer argumento: subcomandos
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> {
                        if (s.equals("crear") && !p.hasPermission("menuinteractivo.reino.comandos.crear"))
                            return false;
                        return s.startsWith(args[0].toLowerCase());
                    })
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase();

        // Solo los subcomandos que usan etiqueta como segundo arg
        if (args.length == 2) {
            switch (sub) {
                case "unir":
                case "salir":
                    // sugerir etiquetas de todos los reinos disponibles
                    return manager.listarReinos().stream()
                            .map(Reino::getEtiqueta)
                            .filter(e -> e.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "eliminar":
                case "info":
                    // sugerir etiquetas de reinos existentes
                    return manager.listarReinos().stream()
                            .map(Reino::getEtiqueta)
                            .filter(e -> e.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "transferir":
                    if (args.length == 2) {
                        // sugerir etiquetas de reinos
                        return manager.listarReinos().stream()
                                .map(Reino::getEtiqueta)
                                .filter(e -> e.startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    if (args.length == 3) {
                        // sugerir nombres de jugadores en el reino
                        String etiqueta = args[1].toLowerCase();
                        return manager.obtenerMiembros(etiqueta).stream()
                                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                                .filter(Objects::nonNull)
                                .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    break;

                default:
                    // crear y listar no completan más
                    return Collections.emptyList();
            }
        }

        // No sugerir nada más para args.length > 2
        return Collections.emptyList();
    }
}
