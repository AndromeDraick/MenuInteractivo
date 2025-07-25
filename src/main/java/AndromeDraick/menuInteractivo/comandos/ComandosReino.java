package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.managers.ReinoManager;
import AndromeDraick.menuInteractivo.model.Reino;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

import java.util.stream.Collectors;

public class ComandosReino implements CommandExecutor, TabCompleter {

    private final MenuInteractivo plugin;
    private final ReinoManager manager;

    private static final List<String> SUBS = List.of(
            "crear", "unirse", "salir", "eliminar", "lista", "info", "transferir", "exiliar"
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

    private void cmdCrear(Player p, String[] args) {
        if (!p.hasPermission("menuinteractivo.reino.comandos.crear")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para crear reinos.");
            return;
        }

        if (args.length < 4) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /rnmi crear <etiqueta> <nombre> <moneda> [descripcion]");
            return;
        }

        if (manager.obtenerReinoJugador(p.getUniqueId()) != null) {
            p.sendMessage(ChatColor.RED + "Ya perteneces a un reino. No puedes crear otro.");
            return;
        }

        String etiqueta = args[1];
        String nombreArg = args[2];
        String moneda = args[3];

        if (!etiqueta.matches("^[A-Za-z0-9\\[\\]]{1,5}$")) {
            p.sendMessage(ChatColor.RED + "Etiqueta inválida. Máximo 5 caracteres, sólo letras, dígitos o []");
            return;
        }

        String nombre = nombreArg.replace("_", " ");
        if (nombre.isBlank()) {
            p.sendMessage(ChatColor.RED + "Nombre inválido. Usa '_' para espacios, mínimo 1 carácter.");
            return;
        }

        if (!moneda.matches("^[A-Za-z_]{1,18}$")) {
            p.sendMessage(ChatColor.RED + "Moneda inválida. Máximo 18 letras o guiones bajos (usa _ como espacio).");
            return;
        }

        moneda = moneda.replace("_", " ");

        if (manager.listarReinos().stream()
                .anyMatch(r -> r.getEtiqueta().equalsIgnoreCase(etiqueta))) {
            p.sendMessage(ChatColor.RED + "Ya existe un reino con esa etiqueta.");
            return;
        }

        // Concatenar descripción si hay más args
        String descripcion = args.length > 4
                ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)).replace("_", " ")
                : "";

        if (!manager.crearReino(etiqueta, nombre, descripcion, moneda, p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Error al crear el reino. ¿Has revisado la consola?");
            return;
        }

        String genero = plugin.getBaseDeDatos().getGenero(p.getUniqueId());
        String rolJugador = genero.equalsIgnoreCase("Femenino") ? "Reina" : "Rey";
        String tituloSocial = "realeza";

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
        if (!p.hasPermission("menuinteractivo.reino.comandos.salir")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para salirte del reino.");
            return;
        }
        String miReino = manager.obtenerReinoJugador(p.getUniqueId());
        if (miReino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        String rol = Optional.ofNullable(manager.obtenerRolJugador(p.getUniqueId())).orElse("");
        if (rol.equalsIgnoreCase("Rey") || rol.equalsIgnoreCase("Reina")) {
            p.sendMessage(ChatColor.RED + "Los " + rol.toLowerCase() + " no pueden abandonar su reino.");
            return;
        }

        if (manager.salirReino(p.getUniqueId())) {
            p.sendMessage(ChatColor.GREEN + "Has salido del reino '" + miReino + "'.");
        } else {
            p.sendMessage(ChatColor.RED + "Error al salir del reino.");
        }
    }


    private void cmdEliminar(Player p, String[] args) {
        if (!p.hasPermission("menuinteractivo.reino.comandos.eliminar")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para eliminar reinos.");
            return;
        }
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
        if (!p.hasPermission("menuinteractivo.reino.comandos.listar")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para unirte a reinos.");
            return;
        }
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
        if (!p.hasPermission("menuinteractivo.reino.comandos.info")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para checar información de reinos.");
            return;
        }
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

        p.sendMessage(ChatColor.GREEN + "— Información de " + ChatColor.GOLD + r.getNombre() + ChatColor.GREEN + " —");

        p.sendMessage(ChatColor.GRAY + "Etiqueta: " +
                ChatColor.DARK_GRAY + "" + ChatColor.BOLD + r.getEtiqueta());

        OfflinePlayer creador = Bukkit.getOfflinePlayer(r.getReyUUID());
        String nombreCreador = creador.getName() != null ? creador.getName() : r.getReyUUID().toString();
        String rolRey = Optional.ofNullable(manager.obtenerRolJugador(r.getReyUUID())).orElse("Sin rol");

        p.sendMessage(ChatColor.GRAY + "Creador: " +
                ChatColor.AQUA + nombreCreador +
                ChatColor.GRAY + " (" + ChatColor.GOLD + rolRey + ChatColor.GRAY + ")");

        String desc = r.getDescripcion() == null || r.getDescripcion().isBlank()
                ? "Sin descripción"
                : r.getDescripcion();
        p.sendMessage(ChatColor.GRAY + "Descripción: " +
                ChatColor.WHITE + desc);

        p.sendMessage(ChatColor.GRAY + "Moneda: " +
                ChatColor.GREEN + r.getMoneda());

        if (r.getFechaCreacion() != null) {
            p.sendMessage(ChatColor.GRAY + "Fecha de creación: " +
                    ChatColor.YELLOW + r.getFechaCreacion()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        } else {
            p.sendMessage(ChatColor.GRAY + "Fecha de creación: " +
                    ChatColor.RED + "No disponible");
        }

        List<UUID> miembros = manager.obtenerMiembros(etiqueta);
        p.sendMessage(ChatColor.GRAY + "Miembros: " +
                ChatColor.YELLOW + miembros.size());

        BancoManager bm = new BancoManager(plugin.getBaseDeDatos(), plugin.getEconomia());
        p.sendMessage(ChatColor.GRAY + "Bancos activos: " +
                ChatColor.YELLOW + bm.obtenerBancosDeReino(etiqueta).size());
        p.sendMessage(ChatColor.GRAY + "Solicitudes pendientes: " +
                ChatColor.YELLOW + bm.obtenerBancosPendientes(etiqueta).size());
    }

    private void cmdTransferir(Player p, String[] args) {
        if (!p.hasPermission("menuinteractivo.reino.comandos.transferir")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para transferir liderazgo del reino.");
            return;
        }
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
        if (target == null) {
            p.sendMessage(ChatColor.RED + "El jugador debe estar conectado para transferir el reino.");
            return;
        }

        String reinoTarget = manager.obtenerReinoJugador(target.getUniqueId());
        if (reinoTarget == null || !reinoTarget.equalsIgnoreCase(etiqueta)) {
            p.sendMessage(ChatColor.RED +
                    "El jugador debe ser miembro del reino '" + etiqueta + "'.");
            return;
        }

        if (manager.transferirLiderazgo(etiqueta, target.getUniqueId())) {
            String genero = plugin.getBaseDeDatos().getGenero(target.getUniqueId());
            String nuevoRol = genero.equalsIgnoreCase("Femenino") ? "Reina" : "Rey";

            p.sendMessage(ChatColor.GREEN +
                    "Has transferido el liderazgo de '" + etiqueta + "' a " + targetName + ".");
            target.sendMessage(ChatColor.GREEN +
                    "Ahora eres la/el " + nuevoRol + " del reino '" + etiqueta + "'.");
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
        if (!(sender instanceof Player p)) return Collections.emptyList();

        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> {
                        if (s.equals("crear") && !p.hasPermission("menuinteractivo.reino.comandos.crear"))
                            return false;
                        if (s.equals("eliminar") && !p.hasPermission("menuinteractivo.reino.comandos.eliminar"))
                            return false;
                        if (s.equals("info") && !p.hasPermission("menuinteractivo.reino.comandos.info"))
                            return false;
                        if (s.equals("unirse") && !p.hasPermission("menuinteractivo.reino.comandos.unirse"))
                            return false;
                        if (s.equals("salir") && !p.hasPermission("menuinteractivo.reino.comandos.salir"))
                            return false;
                        if (s.equals("exiliar") && !p.hasPermission("menuinteractivo.reino.comandos.exiliar"))
                            return false;
                        if (s.equals("transferir") && !p.hasPermission("menuinteractivo.reino.comandos.transferir"))
                            return false;
                        return s.startsWith(args[0].toLowerCase());
                    })
                    .toList();
        }

        String sub = args[0].toLowerCase();

        // —— Subcomando 'crear' —— //
        if (sub.equals("crear")) {
            return switch (args.length) {
                case 2 -> List.of("REI01", "[XYZ]", "ABCD").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
                case 3 -> List.of("Mexico", "Azteca", "Condenados", "Strados").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .toList();
                case 4 -> List.of("Quetzales", "platas", "cobre_Aztecas", "gemas_Condenadas").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                        .toList();
                case 5 -> List.of("Un_reino_poderoso", "Cuna_de_héroes", "Dominio_de_la_magia").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[4].toLowerCase()))
                        .toList();
                default -> List.of();
            };
        }

        if (args.length == 2) {
            switch (sub) {
                case "salir", "eliminar", "info", "lista", "unirse" -> {
                    return manager.listarReinos().stream()
                            .map(Reino::getEtiqueta)
                            .filter(e -> e != null && e.toLowerCase().startsWith(args[1].toLowerCase()))
                            .toList();
                }
                case "transferir" -> {
                    return manager.listarReinos().stream()
                            .filter(r -> r.getReyUUID().equals(p.getUniqueId())) // solo tus reinos
                            .map(Reino::getEtiqueta)
                            .filter(e -> e != null && e.toLowerCase().startsWith(args[1].toLowerCase()))
                            .toList();
                }
                case "exiliar" -> {
                    String reinoJugador = manager.obtenerReinoJugador(p.getUniqueId());
                    if (reinoJugador == null) return List.of();
                    return manager.obtenerMiembros(reinoJugador).stream()
                            .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                            .filter(Objects::nonNull)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .toList();
                }
            }
        }

        if (args.length == 3 && sub.equals("transferir")) {
            String etiqueta = args[1].toLowerCase();
            return manager.obtenerMiembros(etiqueta).stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .filter(Objects::nonNull)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }

}
