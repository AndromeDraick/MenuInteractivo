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
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import java.util.stream.Collectors;

public class ComandosReino implements CommandExecutor, TabCompleter {

    private final MenuInteractivo plugin;
    private final ReinoManager manager;

    private static final List<String> SUBS = List.of(
            "crear", "unir", "salir", "eliminar", "listar", "info", "transferir", "exiliar"
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
                case "unirse"     -> cmdUnirse(p, args);
                case "salir"    -> cmdSalir(p);
                case "eliminar" -> cmdEliminar(p, args);
                case "lista"   -> cmdLista(p);
                case "info"     -> cmdInfo(p, args);
                case "transferir" -> cmdTransferir(p, args);
                case "exiliar"   -> cmdExiliar(p, args);
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

    // === En ComandosReino.java ===

    private void cmdCrear(Player p, String[] args) {
        if (!p.hasPermission("menuinteractivo.reino.comandos.crear")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para crear reinos.");
            return;
        }
        // Ahora esperamos: etiqueta, nombre, moneda
        if (args.length < 4) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi crear <etiqueta> <nombre> <moneda>");
            return;
        }

        String etiqueta  = args[1];
        String nombreArg = args[2];
        String moneda    = args[3];

        // 1) Validar etiqueta: hasta 5 chars, letras, dígitos o [ ]
        if (!etiqueta.matches("^[A-Za-z0-9\\[\\]]{1,5}$")) {
            p.sendMessage(ChatColor.RED + "Etiqueta inválida. Máximo 5 caracteres, sólo letras, dígitos o []");
            return;
        }

        // 2) Nombre: '_' → espacio, al menos 1 char
        String nombre = nombreArg.replace("_", " ");
        if (nombre.isBlank()) {
            p.sendMessage(ChatColor.RED + "Nombre inválido. Usa '_' para espacios, mínimo 1 carácter.");
            return;
        }

        // 3) Moneda: sólo letras, hasta 8 chars
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

        // 5) Crear el reino en la tabla 'reinos'
        if (!manager.crearReino(etiqueta, nombre, moneda, p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Error al crear el reino. ¿Has revisado la consola?");
            return;
        }

        // 6) Determinar rol (Rey/Reina) según género, y título social "realeza"
        String genero    = plugin.getBaseDeDatos().getGenero(p.getUniqueId());
        String rolJugador = genero.equalsIgnoreCase("Femenino") ? "Reina" : "Rey";
        String tituloSocial = "realeza";

        // 7) Asociar al creador con rol y título en 'jugadores_reino'
        manager.unirReino(p.getUniqueId(), etiqueta, rolJugador, tituloSocial);

        p.sendMessage(ChatColor.GREEN +
                "Reino '" + nombre + "' (moneda: " + moneda + ") creado con etiqueta '" +
                etiqueta + "' y ahora eres " + rolJugador + " de la " + tituloSocial + ".");
    }

    private void cmdExiliar(Player p, String[] args) {
        if (!p.hasPermission("menuinteractivo.reino.comandos.exiliar")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para exiliar personas.");
            return;
        }
        // /rnmi exiliar <jugador>
        if (args.length < 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi exiliar <jugador>");
            return;
        }
        // Sólo el rey o la reina pueden exiliar
        String etiqueta = manager.obtenerReinoJugador(p.getUniqueId());
        String rol      = manager.obtenerRolJugador(p.getUniqueId());
        if (etiqueta == null || !(rol.equalsIgnoreCase("Rey") || rol.equalsIgnoreCase("Reina"))) {
            p.sendMessage(ChatColor.RED + "Solo el rey o la reina puede exiliar miembros.");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null
                || !etiqueta.equalsIgnoreCase(manager.obtenerReinoJugador(target.getUniqueId()))) {
            p.sendMessage(ChatColor.RED + "Ese jugador no está en tu reino o no está en línea.");
            return;
        }

        // Exiliar (sacarlo del reino)
        if (manager.salirReino(target.getUniqueId())) {
            p.sendMessage(ChatColor.GREEN + targetName + " ha sido exiliado del reino.");
            target.sendMessage(ChatColor.RED + "Has sido exiliado del reino '" + etiqueta + "'.");
        } else {
            p.sendMessage(ChatColor.RED + "Error al exiliar a " + targetName + ".");
        }
    }

    private void cmdUnirse(Player p, String[] args) {
        // Permiso para unirse
        if (!p.hasPermission("menuinteractivo.reino.comandos.unirse")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para unirte a reinos.");
            return;
        }
        // Uso correcto
        if (args.length < 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi unirse <etiqueta>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        // El reino debe existir
        if (manager.listarReinos().stream()
                .map(Reino::getEtiqueta)
                .noneMatch(e -> e.equalsIgnoreCase(etiqueta))) {
            p.sendMessage(ChatColor.RED + "No existe ningún reino con etiqueta '" + etiqueta + "'.");
            return;
        }
        // No estar ya en uno
        if (manager.obtenerReinoJugador(p.getUniqueId()) != null) {
            p.sendMessage(ChatColor.RED + "Ya perteneces a un reino. Primero usa /rnmi salir.");
            return;
        }

        // Determinar rol según género (por defecto Campesino)
        String genero    = plugin.getBaseDeDatos().getGenero(p.getUniqueId());
        String rolJugador = genero.equalsIgnoreCase("Femenino")
                ? "Campesina"
                : "Campesino";
        // Usamos título social genérico
        String tituloSocial = "plebeyo";

        // Unir al jugador con rol y título
        if (manager.unirReino(p.getUniqueId(), etiqueta, rolJugador, tituloSocial)) {
            p.sendMessage(ChatColor.GREEN +
                    "Te has unido al reino '" + etiqueta + "' como " + rolJugador + ".");
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

        // Impedir que el Rey o la Reina salgan de su propio reino
        String rol = manager.obtenerRolJugador(p.getUniqueId());  // devuelve "Rey", "Reina" o "miembro"
        if (rol.equalsIgnoreCase("Rey") || rol.equalsIgnoreCase("Reina")) {
            p.sendMessage(ChatColor.RED + "Los " + rol.toLowerCase() + " no pueden abandonar su reino.");
            return;
        }

        // El resto de miembros sí pueden salir
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

    private void cmdLista(Player p) {
        List<Reino> reinos = manager.listarReinos();
        if (reinos.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay reinos creados.");
            return;
        }
        p.sendMessage(ChatColor.GOLD + "— Reinos registrados —");

        // Necesitamos LuckPerms para consultar el grupo del creador
        LuckPerms lp = plugin.getPermisos();

        for (Reino r : reinos) {
            // Obtener grupo principal del rey
            String grupo = "default";
            if (lp != null) {
                User user = lp.getUserManager().getUser(r.getReyUUID());
                if (user != null) {
                    grupo = user.getPrimaryGroup();
                }
            }

            // Colorizar el nombre del reino según grupo
            String nombre = r.getNombre();
            String nombreColoreado;
            switch (grupo.toLowerCase()) {
                case "platino"   ->
                        nombreColoreado = colorName(nombre, ChatColor.DARK_GRAY, ChatColor.GRAY);
                case "diamante"  ->
                        nombreColoreado = colorName(nombre, ChatColor.DARK_AQUA, ChatColor.AQUA);
                case "esmeralda" ->
                        nombreColoreado = colorName(nombre, ChatColor.DARK_GREEN, ChatColor.GREEN);
                case "netherita" ->
                        nombreColoreado = colorName(nombre, ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE);
                default ->
                        nombreColoreado = ChatColor.GRAY + nombre;
            }

            // La etiqueta siempre en DARK_GRAY + BOLD
            String etiquetaColoreada = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + r.getEtiqueta();
            p.sendMessage(etiquetaColoreada + ChatColor.GRAY + ": " + nombreColoreado);
        }
    }

    // 2) Helper privado para aplicar &outer &middle &outer al nombre
    private String colorName(String name, ChatColor outer, ChatColor middle) {
        int len   = name.length();
        int part1 = (len + 2) / 3;
        int part2 = (2 * len + 2) / 3;
        String s1 = name.substring(0, part1);
        String s2 = name.substring(part1, part2);
        String s3 = name.substring(part2);
        return outer + s1 + middle + s2 + outer + s3;
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
        p.sendMessage(ChatColor.YELLOW + "/rnmi unirse <etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/rnmi salir");
        p.sendMessage(ChatColor.YELLOW + "/rnmi eliminar <etiqueta>");
        p.sendMessage(ChatColor.YELLOW + "/rnmi lista");
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
                case "salir", "eliminar", "info", "lista" -> {
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
                case "unirse" -> {
                    if (args.length == 2) {
                        return manager.listarReinos().stream()
                                .map(Reino::getEtiqueta)
                                .filter(e -> e.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }

                case "exiliar" -> {
                    return manager.obtenerMiembros(
                                    manager.obtenerReinoJugador(p.getUniqueId())
                            ).stream()
                            .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
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
