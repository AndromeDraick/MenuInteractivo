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
import java.util.UUID;
import java.util.stream.Collectors;

public class ComandosReino implements CommandExecutor, TabCompleter {

    private final MenuInteractivo plugin;
    private final ReinoManager manager;

    private static final List<String> SUBS = List.of(
            "crear", "unir", "salir", "eliminar", "listar", "info", "transferir"
    );

    public ComandosReino(MenuInteractivo plugin) {
        this.plugin  = plugin;
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
                case "crear"    -> {
                    if (!p.hasPermission("menuinteractivo.reino.comandos.crear")) {
                        p.sendMessage(ChatColor.RED + "No tienes permiso para crear reinos.");
                    } else {
                        cmdCrear(p, args);
                    }
                }
                case "unir"     -> cmdUnir(p, args);
                case "salir"    -> cmdSalir(p);
                case "eliminar" -> cmdEliminar(p, args);
                case "listar"   -> cmdListar(p);
                case "info"     -> cmdInfo(p, args);
                case "transferir" -> cmdTransferir(p, args);
                default -> {
                    p.sendMessage(ChatColor.RED + "Subcomando desconocido.");
                    mostrarAyuda(p);
                }
            }
        } catch (Exception ex) {
            p.sendMessage(ChatColor.RED + "Ocurrió un error al ejecutar el comando. Revisa la consola.");
            plugin.getLogger().severe("Error en ComandosReino: " + ex.getMessage());
            ex.printStackTrace();
        }
        return true;
    }

    private void cmdCrear(Player p, String[] args) {
        // Ahora esperamos 3 parámetros: etiqueta, nombre, moneda
        if (args.length < 4) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi crear <etiqueta> <nombre> <moneda>");
            return;
        }

        String etiqueta = args[1];
        String nombreArg = args[2];
        String moneda    = args[3];

        // 1) Validar etiqueta: sólo letras, dígitos o [ ], hasta 5 caracteres
        if (!etiqueta.matches("^[A-Za-z0-9\\[\\]]{1,5}$")) {
            p.sendMessage(ChatColor.RED + "Etiqueta inválida. Máximo 5 caracteres, sólo letras, dígitos o []");
            return;
        }

        // 2) Validar nombre: puede usar _ en lugar de espacios, al menos 1 carácter
        if (nombreArg.length() < 1) {
            p.sendMessage(ChatColor.RED + "Nombre inválido. Usa '_' para espacios, al menos 1 carácter.");
            return;
        }
        String nombre = nombreArg.replace("_", " ");

        // 3) Validar moneda: sólo letras, hasta 8 caracteres
        if (!moneda.matches("^[A-Za-z]{1,8}$")) {
            p.sendMessage(ChatColor.RED + "Moneda inválida. Máximo 8 letras, sólo A–Z.");
            return;
        }

        // 4) Etiqueta única
        if (manager.listarReinos().stream()
                .anyMatch(r -> r.getEtiqueta().equalsIgnoreCase(etiqueta))) {
            p.sendMessage(ChatColor.RED + "Ya existe un reino con esa etiqueta.");
            return;
        }

        // 5) Crear en BD (ahora con moneda) y asociar creador
        if (manager.crearReino(etiqueta, nombre, moneda, p.getUniqueId())) {
            manager.unirReino(p.getUniqueId(), etiqueta);
            p.sendMessage(ChatColor.GREEN +
                    "Reino '" + nombre + "' (moneda: " + moneda + ") creado con etiqueta '" +
                    etiqueta + "' y ahora eres su rey.");
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
        if (manager.listarReinos().stream()
                .map(Reino::getEtiqueta)
                .noneMatch(e -> e.equalsIgnoreCase(etiqueta))) {
            p.sendMessage(ChatColor.RED + "No existe ningún reino con etiqueta '" + etiqueta + "'.");
            return;
        }
        if (manager.obtenerReinoJugador(p.getUniqueId()) != null) {
            p.sendMessage(ChatColor.RED + "Ya perteneces a un reino. Primero usa /rnmi salir.");
            return;
        }
        if (manager.unirReino(p.getUniqueId(), etiqueta)) {
            p.sendMessage(ChatColor.GREEN + "Te has unido al reino '" + etiqueta + "'.");
        } else {
            p.sendMessage(ChatColor.RED +
                    "Error al unirte al reino. ¿Existe y está aprobado?");
        }
    }

    private void cmdSalir(Player p) {
        String miReino = manager.obtenerReinoJugador(p.getUniqueId());
        if (miReino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }
        if (manager.salirReino(p.getUniqueId())) {
            p.sendMessage(ChatColor.GREEN + "Has salido del reino '" + miReino + "'.");
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
            p.sendMessage(ChatColor.AQUA + r.getEtiqueta() +
                    ChatColor.GRAY + ": " + r.getNombre());
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
        p.sendMessage(ChatColor.GREEN + "— Información de " + r.getNombre() + " —");
        p.sendMessage(ChatColor.GRAY + "Etiqueta: " + r.getEtiqueta());
        p.sendMessage(ChatColor.GRAY + "Rey: " + r.getReyUUID());
        p.sendMessage(ChatColor.GRAY + "Miembros: " +
                manager.obtenerMiembros(etiqueta).size());
    }

    private void cmdTransferir(Player p, String[] args) {
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
        if (!reino.getReyUUID().equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Solo el rey puede transferir el liderazgo.");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(targetName);
        if (target == null ||
                !manager.obtenerReinoJugador(target.getUniqueId()).equalsIgnoreCase(etiqueta)) {
            p.sendMessage(ChatColor.RED +
                    "El jugador debe estar conectado y ser miembro de ese reino.");
            return;
        }
        if (manager.transferirLiderazgo(etiqueta, target.getUniqueId())) {
            p.sendMessage(ChatColor.GREEN +
                    "Has transferido el liderazgo de '" + etiqueta + "' a " + targetName +".");
            target.sendMessage(ChatColor.GREEN +
                    "Ahora eres el rey del reino '" + etiqueta + "'.");
        } else {
            p.sendMessage(ChatColor.RED +
                    "Error al transferir liderazgo. Revisa la consola.");
        }
    }

    private void mostrarAyuda(Player p) {
        p.sendMessage(ChatColor.GOLD + "— Comandos de Reinos —");
        p.sendMessage(ChatColor.YELLOW + "/rnmi crear <etiqueta> <nombre>");
        p.sendMessage(ChatColor.YELLOW + "/rnmi unir <etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/rnmi salir");
        p.sendMessage(ChatColor.YELLOW + "/rnmi eliminar <etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/rnmi listar");
        p.sendMessage(ChatColor.YELLOW + "/rnmi info <etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/rnmi transferir <etiqueta> <jugador>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player p = (Player) sender;

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
        if (args.length == 2) {
            switch (sub) {
                case "unir", "salir", "eliminar", "info" -> {
                    return manager.listarReinos().stream()
                            .map(Reino::getEtiqueta)
                            .filter(e -> e.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "transferir" -> {
                    if (args.length == 2) {
                        return manager.listarReinos().stream()
                                .map(Reino::getEtiqueta)
                                .filter(e -> e.startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }
            }
        } else if (args.length == 3 && sub.equals("transferir")) {
            String etiqueta = args[1].toLowerCase();
            return manager.obtenerMiembros(etiqueta).stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .filter(Objects::nonNull)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
