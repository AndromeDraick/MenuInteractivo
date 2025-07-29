package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.HikariProvider;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.model.Banco;
import AndromeDraick.menuInteractivo.model.MonedasReinoInfo;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ComandosBMI implements CommandExecutor, TabCompleter {

    private final MenuInteractivo plugin;
    private final BancoManager bancoManager;
    private final Economy economia;

    private static final List<String> SUBS = List.of(
            "crear", "pendientes", "aprobar", "rechazar",
            "listar", "unir", "salir", "saldo",
            "depositar", "retirar", "banco", "ayuda", "monedas", "historial",
            "imprimir", "quemar", "convertir", "contrato", "intercambiar"
    );

    public ComandosBMI(MenuInteractivo plugin) {
        this.plugin       = plugin;
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos(), plugin.getEconomia());
        this.economia     = plugin.getEconomia();
        PluginCommand cmd = plugin.getCommand("bmi");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede usarlo un jugador.");
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("ayuda")) {
            if (!p.hasPermission("menuinteractivo.banco.ayuda")) {
                p.sendMessage(ChatColor.RED + "No tienes permiso para ver la ayuda.");
                return true;
            }
            mostrarAyuda(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        String permisoBase = "menuinteractivo.banco.";
        Map<String, String> permisosPorSub = new HashMap<>();
        permisosPorSub.put("crear", "crear");
        permisosPorSub.put("pendientes", "pendientes");
        permisosPorSub.put("aprobar", "aprobar");
        permisosPorSub.put("rechazar", "rechazar");
        permisosPorSub.put("listar", "lista");
        permisosPorSub.put("unir", "unirse");
        permisosPorSub.put("salir", "salir");
        permisosPorSub.put("saldo", "saldo");
        permisosPorSub.put("depositar", "depositar");
        permisosPorSub.put("retirar", "retirar");
        permisosPorSub.put("banco", "banco");
        permisosPorSub.put("historial", "historial");

        if (permisosPorSub.containsKey(sub) && !p.hasPermission(permisoBase + permisosPorSub.get(sub))) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        try {
            switch (sub) {
                case "crear"      -> cmdCrearBanco(p, args);
                case "renombrar" -> cmdRenombrarBanco(p, args);
                case "limpiarhuerfanos" -> cmdLimpiarHuerfanos(sender);
                case "pendientes" -> cmdListarPendientes(p);
                case "aprobar"    -> cmdAprobarRechazar(p, args, true);
                case "rechazar"   -> cmdAprobarRechazar(p, args, false);
                case "listar"     -> plugin.getMenuBancos().abrirListaActivos(p);
                case "unir"       -> cmdUnirSalir(p, args, true);
                case "salir"      -> cmdUnirSalir(p, args, false);
                case "saldo"      -> cmdSaldo(p, args);
                case "depositar", "retirar" -> cmdMoverFondos(p, args, sub.equals("depositar"));
                case "banco"      -> cmdBanco(p, args);
                case "contrato"   -> cmdContrato(p, args);
                case "imprimir"   -> cmdImprimirMoneda(p, args);
                case "quemar"     -> cmdQuemarMoneda(p, args);
                case "convertir"  -> cmdConvertirMoneda(p, args);
                case "monedas"    -> plugin.getMenuMonedas().abrirMenu(p);
                case "historial"  -> cmdHistorialBanco(p, args);
                case "intercambiar" -> cmdIntercambiarMonedas(p, args);
                default -> {
                    p.sendMessage(ChatColor.RED + "Subcomando desconocido.");
                    mostrarAyuda(p);
                }
            }
        } catch (Exception e) {
            p.sendMessage(ChatColor.RED + "Error al ejecutar el comando, mira la consola.");
            plugin.getLogger().severe("Error en ComandosBMI: " + e);
            e.printStackTrace();
        }
        return true;
    }

    private void cmdCrearBanco(Player p, String[] args) {

        if (!p.hasPermission("menuinteractivo.banco.crear")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para crear bancos.");
            return;
        }

        // /bmi crear banco <Nombre> <Etiqueta>
        if (args.length < 4 || !args[1].equalsIgnoreCase("banco")) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi crear banco <Nombre> <Etiqueta>");
            return;
        }

        String nombre   = args[2];
        String etiqueta = args[3].toLowerCase();

        if (!etiqueta.matches("[a-z0-9_-]+")) {
            p.sendMessage(ChatColor.RED + "La etiqueta solo puede contener minúsculas, números, '-' o '_'.");
            return;
        }

        if (bancoManager.existeBanco(etiqueta)) {
            p.sendMessage(ChatColor.RED + "Ya existe un banco con esa etiqueta.");
            return;
        }

        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        // Crear banco y agregar al jugador como miembro
        boolean creado = bancoManager.crearBanco(etiqueta, nombre, reino, p.getUniqueId());
        if (creado) {
            bancoManager.agregarJugadorABanco(p.getUniqueId(), etiqueta);
            p.sendMessage(ChatColor.GREEN + "Solicitud de banco '" + nombre + "' enviada.");

            // Notificar a jugadores con rol 'rey' o 'lider' del mismo reino
            for (Player online : Bukkit.getOnlinePlayers()) {
                String reinoJugador = bancoManager.obtenerReinoJugador(online.getUniqueId());
                if (reino.equalsIgnoreCase(reinoJugador)) {
                    String rol = bancoManager.obtenerRolJugadorEnReino(online.getUniqueId());
                    if (rol != null && (rol.equalsIgnoreCase("rey") || rol.equalsIgnoreCase("lider"))) {
                        online.sendMessage(ChatColor.LIGHT_PURPLE + "⚠️ " + ChatColor.YELLOW + "Hay una nueva solicitud de banco en tu reino.");
                        online.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.GREEN + "/bmi aprobar " + etiqueta + ChatColor.GRAY + " para aprobarla.");
                        online.playSound(online.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f); // volumen y pitch
                    }
                }
            }

        } else {
            p.sendMessage(ChatColor.RED + "Error al solicitar banco. Revisa la consola.");
        }
    }

    private void cmdRenombrarBanco(Player p, String[] args) {
        if (!p.hasPermission("menuinteractivo.banco.renombrar")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para renombrar bancos.");
            return;
        }

        // /bmi renombrar banco <etiquetaActual> <nuevoNombre> <nuevaEtiqueta>
        if (args.length < 5 || !args[1].equalsIgnoreCase("banco")) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi renombrar banco <etiquetaActual> <nuevoNombre> <nuevaEtiqueta>");
            return;
        }

        String etiquetaActual = args[2].toLowerCase();
        String nuevoNombre = args[3];
        String nuevaEtiqueta = args[4].toLowerCase();

        if (!nuevaEtiqueta.matches("[a-z0-9_-]+")) {
            p.sendMessage(ChatColor.RED + "La nueva etiqueta solo puede contener minúsculas, números, '-' o '_'.");
            return;
        }

        if (!bancoManager.existeBanco(etiquetaActual)) {
            p.sendMessage(ChatColor.RED + "No existe un banco con la etiqueta especificada.");
            return;
        }

        if (bancoManager.existeBanco(nuevaEtiqueta)) {
            p.sendMessage(ChatColor.RED + "Ya existe un banco con la nueva etiqueta.");
            return;
        }

        // Validar que el jugador es propietario
        if (!bancoManager.esPropietarioBanco(p.getUniqueId(), etiquetaActual)) {
            p.sendMessage(ChatColor.RED + "Solo el propietario del banco puede renombrarlo.");
            return;
        }

        boolean actualizado = bancoManager.renombrarBancoCompleto(etiquetaActual, nuevoNombre, nuevaEtiqueta);
        if (actualizado) {
            p.sendMessage(ChatColor.GREEN + "✅ El banco ha sido renombrado a '" + nuevoNombre + "' con la etiqueta '" + nuevaEtiqueta + "'.");
        } else {
            p.sendMessage(ChatColor.RED + "❌ No se pudo renombrar el banco. Revisa la consola.");
        }
    }

    private void cmdLimpiarHuerfanos(CommandSender sender) {
        if (!(sender instanceof ConsoleCommandSender || sender.isOp())) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por la consola o por administradores.");
            return;
        }

        // Primero obtenemos las etiquetas huérfanas
        String selectSql = "SELECT jb.etiqueta_banco FROM jugadores_banco jb " +
                "LEFT JOIN bancos b ON jb.etiqueta_banco = b.etiqueta " +
                "WHERE b.etiqueta IS NULL;";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery()) {

            List<String> huerfanos = new ArrayList<>();
            while (rs.next()) {
                huerfanos.add(rs.getString("etiqueta_banco"));
            }

            if (huerfanos.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No se encontraron bancos huérfanos.");
                plugin.getLogger().info("[MenuInteractivo] Limpieza ejecutada: 0 huérfanos encontrados.");
                return;
            }

            // Mostramos los huérfanos encontrados
            sender.sendMessage(ChatColor.GOLD + "Bancos huérfanos encontrados: " + huerfanos.size());
            sender.sendMessage(ChatColor.GRAY + String.join(", ", huerfanos));

            // Log detallado en consola
            plugin.getLogger().warning("[MenuInteractivo] Se encontraron " + huerfanos.size() + " bancos huérfanos:");
            for (String etiqueta : huerfanos) {
                plugin.getLogger().warning(" - Banco huérfano: " + etiqueta);
            }

            // Ahora los eliminamos
            String deleteSql = "DELETE FROM jugadores_banco WHERE etiqueta_banco IN (" +
                    huerfanos.stream().map(s -> "?").collect(Collectors.joining(",")) + ");";

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                for (int i = 0; i < huerfanos.size(); i++) {
                    deleteStmt.setString(i + 1, huerfanos.get(i));
                }
                int eliminados = deleteStmt.executeUpdate();
                sender.sendMessage(ChatColor.GREEN + "Registros huérfanos eliminados: " + eliminados);

                // Log final
                plugin.getLogger().info("[MenuInteractivo] Eliminación completa: " + eliminados + " registros borrados.");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al eliminar bancos huérfanos: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "Ocurrió un error al eliminar los registros.");
        }
    }

    private void cmdListarPendientes(Player p) {

        if (!p.hasPermission("menuinteractivo.banco.pendientes")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para ver solicitudes pendientes.");
            return;
        }

        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }
        List<Banco> pend = bancoManager.obtenerBancosPendientes(reino);
        if (pend.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay solicitudes pendientes.");
            return;
        }
        p.sendMessage(ChatColor.GOLD + "Solicitudes pendientes:");
        pend.forEach(b ->
                p.sendMessage(" - " + b.getEtiqueta() + " (" + b.getNombre() + ")")
        );
    }

    private void cmdAprobarRechazar(Player p, String[] args, boolean aprobarBanco) {

        String permiso = aprobarBanco ? "menuinteractivo.banco.aprobar" : "menuinteractivo.banco.rechazar";
        if (!p.hasPermission(permiso)) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para " + (aprobarBanco ? "aprobar" : "rechazar") + " bancos.");
            return;
        }

        // Puede ser:
        // /bmi aprobar|rechazar <etiquetaBanco>
        // o
        // /bmi aprobar|rechazar contrato <etiquetaBanco>
        if (args.length < 2 || args.length > 3) {
            p.sendMessage(ChatColor.YELLOW +
                    "Uso:\n" +
                    "  /bmi " + (aprobarBanco ? "aprobar" : "rechazar") + " <etiquetaBanco>\n" +
                    "  /bmi " + (aprobarBanco ? "aprobar"  : "rechazar") + " contrato <etiquetaBanco>");
            return;
        }

        boolean esContrato = args.length == 3 && args[1].equalsIgnoreCase("contrato");
        String etiqueta = esContrato ? args[2].toLowerCase() : args[1].toLowerCase();

        if (esContrato) {
            // Validaciones
            String reinoJugador = bancoManager.obtenerReinoJugador(p.getUniqueId());
            if (reinoJugador == null) {
                p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
                return;
            }
            // Verifica que jugador es líder o propietario del reino
            String rol = bancoManager.obtenerRolJugadorEnReino(p.getUniqueId());
            Set<String> rolesValidos = Set.of("rey", "reina", "lider");
            if (!rolesValidos.contains(rol.toLowerCase())) {
                p.sendMessage(ChatColor.RED + "Solo el rey, reina o líder puede aceptar/rechazar contratos.");
                return;
            }

            boolean ok;
            if (aprobarBanco) {
                ok = bancoManager.confirmarContrato(etiqueta, reinoJugador);
            } else {
                ok = bancoManager.rechazarContrato(etiqueta, reinoJugador);
            }

            if (ok) {
                p.sendMessage(ChatColor.GREEN +
                        "Contrato con banco '" + etiqueta + "' " +
                        (aprobarBanco ? "aprobado." : "rechazado."));

                if (aprobarBanco) {
                    UUID propietario = bancoManager.obtenerUUIDPropietarioBanco(etiqueta);
                    if (propietario != null) {
                        Player propietarioOnline = Bukkit.getPlayer(propietario);
                        if (propietarioOnline != null && propietarioOnline.isOnline()) {
                            propietarioOnline.sendMessage(ChatColor.AQUA + "📜 Tu contrato con el reino '" + reinoJugador + "' ha sido aprobado.");
                            propietarioOnline.playSound(propietarioOnline.getLocation(),
                                    org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.1f);
                        }
                    }
                }

            } else {
                p.sendMessage(ChatColor.RED +
                        "No se pudo " + (aprobarBanco ? "aprobar " : "rechazar ") +
                        "el contrato con '" + etiqueta + "'.");
            }


        } else {
            // El funcionamiento normal para bancos
            boolean ok = aprobarBanco
                    ? bancoManager.aprobarBanco(etiqueta)
                    : bancoManager.rechazarBanco(etiqueta);
            if (ok) {
                p.sendMessage(ChatColor.GREEN +
                        "Banco " + etiqueta + (aprobarBanco ? " aprobado." : " rechazado."));

                if (aprobarBanco) {
                    // Notificar al propietario si está conectado
                    UUID propietario = bancoManager.obtenerUUIDPropietarioBanco(etiqueta);
                    if (propietario != null) {
                        Player propietarioOnline = Bukkit.getPlayer(propietario);
                        if (propietarioOnline != null && propietarioOnline.isOnline()) {
                            propietarioOnline.sendMessage(ChatColor.GOLD + "✅ Tu banco '" + etiqueta + "' ha sido aprobado.");
                            propietarioOnline.playSound(propietarioOnline.getLocation(),
                                    org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        }
                    }
                }

            } else {
                p.sendMessage(ChatColor.RED +
                        "No se pudo " + (aprobarBanco ? "aprobar " : "rechazar ") + etiqueta + ".");
            }

        }
    }


    private void cmdContrato(Player p, String[] args) {
        // /bmi contrato <reino> <tiempo> permite <acciones>
        if (args.length < 5 || !args[3].equalsIgnoreCase("permite")) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi contrato <Reino> <Tiempo> permite <acciones>");
            p.sendMessage(ChatColor.YELLOW + "Ej: /bmi contrato RNS 1w permite \"imprimir, quemar\"");
            return;
        }

        String reino = args[1].toLowerCase();
        String tiempo = args[2].toLowerCase();
        String permisosRaw = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        String permisos = permisosRaw.replace("\"", "").replace("'", "").toLowerCase();

        // Validar tiempo
        long duracionMs;
        if (tiempo.endsWith("d")) duracionMs = Integer.parseInt(tiempo.replace("d", "")) * 86400000L;
        else if (tiempo.endsWith("w")) duracionMs = Integer.parseInt(tiempo.replace("w", "")) * 7 * 86400000L;
        else if (tiempo.endsWith("m")) duracionMs = Integer.parseInt(tiempo.replace("m", "")) * 30L * 86400000L;
        else if (tiempo.endsWith("y")) duracionMs = Integer.parseInt(tiempo.replace("y", "")) * 365L * 86400000L;
        else {
            p.sendMessage(ChatColor.RED + "Formato de tiempo inválido. Usa d (día), w (semana), m (mes), y (año).");
            return;
        }

        UUID uuid = p.getUniqueId();
        String banco = bancoManager.obtenerBancoPropietario(uuid).toLowerCase();
        if (banco == null) {
            p.sendMessage(ChatColor.RED + "No eres dueño de ningún banco aprobado.");
            return;
        }

        if (!bancoManager.bancoEstaAprobado(banco)) {
            p.sendMessage(ChatColor.YELLOW + "⏳ Tu banco aún está pendiente de aprobación.");
            p.sendMessage(ChatColor.GRAY + "Debes esperar a que un rey o líder apruebe tu banco antes de enviar contratos.");
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            return;
        }

        if (!bancoManager.reinoExiste(reino)) {
            p.sendMessage(ChatColor.RED + "El reino '" + reino + "' no existe.");
            return;
        }

        long ahora = System.currentTimeMillis();
        long fechaFin = ahora + duracionMs;

        boolean ok = bancoManager.crearContrato(banco, reino, new Timestamp(ahora), new Timestamp(fechaFin), permisos);
        if (ok) {
            p.sendMessage(ChatColor.GREEN + "Contrato enviado al reino " + reino +
                    " por " + tiempo + " con permisos: " + permisos);

            // Notificar a jugadores del reino con rol de rey o líder
            for (Player online : Bukkit.getOnlinePlayers()) {
                String reinoJugador = bancoManager.obtenerReinoJugador(online.getUniqueId());
                if (reino.equalsIgnoreCase(reinoJugador)) {
                    String rol = bancoManager.obtenerRolJugadorEnReino(online.getUniqueId());
                    if (rol != null && (rol.equalsIgnoreCase("rey") || rol.equalsIgnoreCase("lider"))) {
                        online.sendMessage(ChatColor.LIGHT_PURPLE + "📜 " + ChatColor.YELLOW + "Un banco te ha enviado un contrato.");
                        online.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.GREEN + "/bmi aprobar contrato " + banco + ChatColor.GRAY + " para aprobarlo.");
                        online.playSound(online.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f); // volumen y pitch

                    }
                }
            }

        } else {
            p.sendMessage(ChatColor.RED + "Error al crear contrato. ¿Ya existe uno activo?");
        }
    }

    private void cmdImprimirMoneda(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi imprimir \"<nombre de moneda>\" <cantidad>");
            return;
        }

        String cantidadStr = args[args.length - 1];
        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadStr);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            p.sendMessage(ChatColor.RED + "Cantidad inválida.");
            return;
        }

        String nombreMoneda = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1))
                .replace("\"", "").trim();

        String banco = bancoManager.obtenerBancoPropietario(p.getUniqueId());
        if (banco == null) {
            p.sendMessage(ChatColor.RED + "No eres propietario de ningún banco aprobado.");
            return;
        }

        String reinoDelBanco = bancoManager.obtenerReinoDeBanco(banco);
        if (reinoDelBanco == null) {
            p.sendMessage(ChatColor.RED + "No se pudo determinar el reino del banco.");
            return;
        }

        String monedaOficial = bancoManager.obtenerNombreMonedaDeReino(reinoDelBanco);
        if (!nombreMoneda.equalsIgnoreCase(monedaOficial)) {
            p.sendMessage(ChatColor.RED + "Solo puedes imprimir la moneda oficial del reino: " + monedaOficial);
            return;
        }

        if (!bancoManager.tienePermisoContrato(banco, reinoDelBanco, "imprimir")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para imprimir moneda. Contrato no autorizado.");
            return;
        }

        if (bancoManager.incrementarCantidadImpresa(reinoDelBanco, cantidad)
                && bancoManager.aumentarMonedasDisponiblesBanco(banco, cantidad)
                && bancoManager.aumentarMonedaImpresaBanco(banco, reinoDelBanco, cantidad)) {

            bancoManager.registrarMovimiento(banco, "imprimir", p.getUniqueId().toString(), cantidad);
            bancoManager.modificarSaldoCuentaJugador(p.getUniqueId(), banco, cantidad);
            p.sendMessage(ChatColor.GREEN + "Se imprimieron " + cantidad + " " + nombreMoneda + " para el reino " + reinoDelBanco);

        } else {
            p.sendMessage(ChatColor.RED + "Error al imprimir moneda. Revisa la consola.");
        }

    }

    private void cmdQuemarMoneda(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi quemar \"<nombre de moneda>\" <cantidad>");
            return;
        }

        String cantidadStr = args[args.length - 1];
        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadStr);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            p.sendMessage(ChatColor.RED + "Cantidad inválida.");
            return;
        }

        String nombreMoneda = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1))
                .replace("\"", "").trim();

        String banco = bancoManager.obtenerBancoPropietario(p.getUniqueId());
        if (banco == null) {
            p.sendMessage(ChatColor.RED + "No eres propietario de ningún banco aprobado.");
            return;
        }

        String reinoDelBanco = bancoManager.obtenerReinoDeBanco(banco);
        if (reinoDelBanco == null) {
            p.sendMessage(ChatColor.RED + "No se pudo determinar el reino del banco.");
            return;
        }

        String monedaOficial = bancoManager.obtenerNombreMonedaDeReino(reinoDelBanco);
        if (!nombreMoneda.equalsIgnoreCase(monedaOficial)) {
            p.sendMessage(ChatColor.RED + "Solo puedes quemar la moneda oficial del reino: " + monedaOficial);
            return;
        }

        if (!bancoManager.tienePermisoContrato(banco, reinoDelBanco, "quemar")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para quemar moneda. Contrato no autorizado.");
            return;
        }

        double disponibles = bancoManager.obtenerCantidadImpresaDisponible(banco);
        if (disponibles < cantidad) {
            p.sendMessage(ChatColor.RED + "No tienes suficientes monedas impresas disponibles para quemar.");
            return;
        }

        if (bancoManager.incrementarCantidadQuemada(reinoDelBanco, cantidad)
                && bancoManager.descontarCantidadImpresaDisponible(banco, cantidad)
                && bancoManager.aumentarMonedaQuemadaBanco(banco, reinoDelBanco, cantidad)) {

            bancoManager.registrarMovimiento(banco, "quemar", p.getUniqueId().toString(), cantidad);
            bancoManager.modificarSaldoCuentaJugador(p.getUniqueId(), banco, -cantidad);
            p.sendMessage(ChatColor.YELLOW + "Se quemaron " + cantidad + " " + nombreMoneda + " del reino " + reinoDelBanco);

        } else {
            p.sendMessage(ChatColor.RED + "Error al quemar moneda. Revisa la consola.");
        }
    }

    private void cmdConvertirMoneda(Player p, String[] args) {
        if (args.length < 4) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi convertir <dineroServidor> a \"<nombre de moneda>\"");
            return;
        }

        double dineroServidor;
        try {
            dineroServidor = Double.parseDouble(args[1]);
            if (dineroServidor <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            p.sendMessage(ChatColor.RED + "Cantidad inválida.");
            return;
        }

        // Buscar el índice de "a"
        int indiceA = -1;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("a")) {
                indiceA = i;
                break;
            }
        }

        if (indiceA == -1 || indiceA == args.length - 1) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi convertir <dineroServidor> a \"<nombre de moneda>\"");
            return;
        }

        String nombreMoneda = String.join(" ", Arrays.copyOfRange(args, indiceA + 1, args.length))
                .replace("\"", "").trim();

        String banco = bancoManager.obtenerBancoPropietario(p.getUniqueId());
        if (banco == null) {
            p.sendMessage(ChatColor.RED + "No eres propietario de ningún banco aprobado.");
            return;
        }

        String reinoDelBanco = bancoManager.obtenerReinoDeBanco(banco);
        if (reinoDelBanco == null) {
            p.sendMessage(ChatColor.RED + "No se pudo determinar el reino del banco.");
            return;
        }

        String monedaOficial = bancoManager.obtenerNombreMonedaDeReino(reinoDelBanco);
        if (!nombreMoneda.equalsIgnoreCase(monedaOficial)) {
            p.sendMessage(ChatColor.RED + "Solo puedes convertir dinero a la moneda oficial del reino: " + monedaOficial);
            return;
        }

        if (!bancoManager.tienePermisoContrato(banco, reinoDelBanco, "convertir")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para convertir dinero. Contrato no autorizado.");
            return;
        }

        if (economia.getBalance(p) < dineroServidor) {
            p.sendMessage(ChatColor.RED + "No tienes suficiente dinero en tu cuenta.");
            return;
        }

        if (bancoManager.incrementarDineroConvertido(reinoDelBanco, dineroServidor)
                && bancoManager.aumentarMonedaConvertidaBanco(banco, reinoDelBanco, dineroServidor)) {

            economia.withdrawPlayer(p, dineroServidor);
            bancoManager.registrarMovimiento(banco, "convertir", p.getUniqueId().toString(), dineroServidor);
            bancoManager.modificarSaldoCuentaJugador(p.getUniqueId(), banco, dineroServidor);
            p.sendMessage(ChatColor.GREEN + "Convertiste $" + dineroServidor + " del servidor en valor para la moneda del reino " + reinoDelBanco);

        } else {
            p.sendMessage(ChatColor.RED + "Error al convertir dinero. Revisa la consola.");
        }
    }

    private void cmdHistorialBanco(Player p, String[] args) {

        if (!p.hasPermission("menuinteractivo.banco.historial")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para ver el historial de bancos.");
            return;
        }

        if (args.length != 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi historial <etiqueta_banco>");
            return;
        }

        String etiqueta = args[1].toLowerCase();

        // Verifica si el jugador es miembro o dueño
        if (!bancoManager.esMiembroOBancoPropietario(p.getUniqueId(), etiqueta)) {
            p.sendMessage(ChatColor.RED + "No tienes acceso al historial de este banco.");
            return;
        }

        List<String> historial = bancoManager.obtenerHistorialBanco(etiqueta, 10);
        if (historial.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay movimientos registrados aún para " + etiqueta + ".");
            return;
        }

        p.sendMessage(ChatColor.GOLD + "— Historial de " + etiqueta + " —");
        historial.forEach(linea -> p.sendMessage(ChatColor.GRAY + "- " + linea));
    }

    private void cmdIntercambiarMonedas(Player p, String[] args) {
        // /bmi intercambiar <cantidad> "<moneda_origen>" a "<moneda_destino>"
        if (args.length < 5) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi intercambiar <cantidad> \"<moneda_origen>\" a \"<moneda_destino>\"");
            return;
        }

        double cantidad;
        try {
            cantidad = Double.parseDouble(args[1]);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            p.sendMessage(ChatColor.RED + "Cantidad inválida.");
            return;
        }

        // Buscar el índice de "a"
        int indiceA = -1;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("a")) {
                indiceA = i;
                break;
            }
        }

        if (indiceA == -1 || indiceA == args.length - 1) {
            p.sendMessage(ChatColor.RED + "Faltan monedas origen o destino.");
            return;
        }

        String monedaOrigen = String.join(" ", Arrays.copyOfRange(args, 2, indiceA)).replace("\"", "").trim();
        String monedaDestino = String.join(" ", Arrays.copyOfRange(args, indiceA + 1, args.length)).replace("\"", "").trim();

        if (monedaOrigen.isEmpty() || monedaDestino.isEmpty()) {
            p.sendMessage(ChatColor.RED + "Faltan nombres válidos para las monedas.");
            return;
        }

        MonedasReinoInfo origen = bancoManager.obtenerInfoMonedaPorNombre(monedaOrigen);
        MonedasReinoInfo destino = bancoManager.obtenerInfoMonedaPorNombre(monedaDestino);

        if (origen == null || destino == null) {
            p.sendMessage(ChatColor.RED + "Alguna de las monedas no existe.");
            return;
        }

        double valorOrigen = origen.getCantidadImpresa() - origen.getCantidadQuemada() > 0
                ? origen.getDineroConvertido() / (origen.getCantidadImpresa() - origen.getCantidadQuemada())
                : 0;

        double valorDestino = destino.getCantidadImpresa() - destino.getCantidadQuemada() > 0
                ? destino.getDineroConvertido() / (destino.getCantidadImpresa() - destino.getCantidadQuemada())
                : 0;

        if (valorOrigen <= 0 || valorDestino <= 0) {
            p.sendMessage(ChatColor.RED + "Alguna de las monedas tiene valor nulo o negativo.");
            return;
        }

        double enDineroServidor = cantidad * valorOrigen;
        double cantidadDestino = enDineroServidor / valorDestino;

        UUID uuid = p.getUniqueId();

        double saldoOrigen = bancoManager.getSaldoMonedaJugador(uuid, origen.getEtiquetaReino());
        if (saldoOrigen < cantidad) {
            p.sendMessage(ChatColor.RED + "No tienes suficiente saldo de " + monedaOrigen + " (" + saldoOrigen + ")");
            return;
        }

        bancoManager.restarMonedaJugador(uuid, origen.getEtiquetaReino(), cantidad);
        bancoManager.sumarMonedaJugador(uuid, destino.getEtiquetaReino(), cantidadDestino);

        p.sendMessage(ChatColor.GREEN + "Intercambiaste " + cantidad + " " + monedaOrigen +
                " por " + String.format("%.2f", cantidadDestino) + " " + monedaDestino);
    }

    private void cmdUnirSalir(Player p, String[] args, boolean unir) {

        String permiso = unir ? "menuinteractivo.banco.unirse" : "menuinteractivo.banco.salir";
        if (!p.hasPermission(permiso)) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para " + (unir ? "unirte a" : "salir de") + " bancos.");
            return;
        }

        // /bmi unir|salir <Etiqueta>
        if (args.length != 2) {
            p.sendMessage(ChatColor.YELLOW +
                    "Uso: /bmi " + (unir ? "unir" : "salir") + " <Etiqueta>");
            return;
        }

        String etiqueta = args[1].toLowerCase();

        if (!unir) {
            // Evitar que el propietario se salga de su propio banco
            String bancoPropio = bancoManager.obtenerBancoPropietario(p.getUniqueId());
            if (bancoPropio != null && bancoPropio.equalsIgnoreCase(etiqueta)) {
                p.sendMessage(ChatColor.RED + "No puedes salir de tu propio banco. Eres el propietario.");
                return;
            }
        }

        boolean ok = unir
                ? bancoManager.agregarJugadorABanco(p.getUniqueId(), etiqueta)
                : bancoManager.eliminarJugadorDeBanco(p.getUniqueId(), etiqueta);

        if (ok) {
            p.sendMessage(ChatColor.GREEN +
                    (unir ? "Te uniste a " : "Saliste de ") + etiqueta);
        } else {
            p.sendMessage(ChatColor.RED +
                    "Error al " + (unir ? "unirte a " : "salir de ") + etiqueta);
        }
    }

    private void cmdSaldo(Player p, String[] args) {

        if (!p.hasPermission("menuinteractivo.banco.saldo")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para consultar saldos.");
            return;
        }

        // /bmi saldo <Etiqueta>
        if (args.length != 2) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi saldo <Etiqueta>");
            return;
        }
        String etiqueta = args[1].toLowerCase();
        double saldo = bancoManager.getSaldoBanco(etiqueta);
        p.sendMessage(saldo >= 0
                ? ChatColor.GREEN + "Saldo de " + etiqueta + ": $" + saldo
                : ChatColor.RED + "Banco '" + etiqueta + "' no existe o no está aprobado.");
    }

    private void cmdMoverFondos(Player p, String[] args, boolean depositar) {

        String permiso = depositar ? "menuinteractivo.banco.depositar" : "menuinteractivo.banco.retirar";
        if (!p.hasPermission(permiso)) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para " + (depositar ? "depositar" : "retirar") + " fondos.");
            return;
        }

        // /bmi depositar|retirar <Etiqueta> <Monto>
        if (args.length != 3) {
            p.sendMessage(ChatColor.YELLOW +
                    "Uso: /bmi " + (depositar ? "depositar" : "retirar") +
                    " <Etiqueta> <Monto>");
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
            if (bancoManager.depositarBanco(etiqueta, monto)) {
                p.sendMessage(ChatColor.GREEN + "Depositaste $" + monto + " en " + etiqueta);
            } else {
                p.sendMessage(ChatColor.RED + "Error al depositar en " + etiqueta);
            }
        } else {
            if (bancoManager.retirarBanco(etiqueta, monto)) {
                economia.depositPlayer(p, monto);
                p.sendMessage(ChatColor.YELLOW + "Retiraste $" + monto + " de " + etiqueta);
            } else {
                p.sendMessage(ChatColor.RED + "Error al retirar de " + etiqueta);
            }
        }
    }

    private void cmdBanco(Player p, String[] args) {

        if (!p.hasPermission("menuinteractivo.banco.banco")) {
            p.sendMessage(ChatColor.RED + "No tienes permiso para acceder a la interfaz del banco.");
            return;
        }

        // /bmi banco <Etiqueta> | /bmi banco cuenta <Etiqueta>
        if (args.length == 2) {
            String etiqueta = args[1].toLowerCase();

            // ✅ Verificar que el banco exista en la tabla 'bancos'
            if (!bancoManager.existeBanco(etiqueta)) {
                p.sendMessage(ChatColor.RED + "Este banco no existe o fue eliminado.");
                return;
            }

            // Solo el propietario del banco puede usar este menú
            if (!bancoManager.esPropietarioBanco(p.getUniqueId(), etiqueta)) {
                p.sendMessage(ChatColor.RED + "Solo el propietario del banco puede acceder a este menú.");
                return;
            }

            plugin.getMenuBancos().abrirIndividual(p, etiqueta);

        } else if (args.length == 3 && args[1].equalsIgnoreCase("cuenta")) {
            String etiqueta = args[2].toLowerCase();

            // ✅ Verificar que el banco exista en la tabla 'bancos'
            if (!bancoManager.existeBanco(etiqueta)) {
                p.sendMessage(ChatColor.RED + "Este banco no existe o fue eliminado.");
                return;
            }

            if (!bancoManager.esMiembroOBancoPropietario(p.getUniqueId(), etiqueta)) {
                p.sendMessage(ChatColor.RED + "No eres miembro ni dueño de este banco.");
                return;
            }

            plugin.getMenuCuentaBanco().abrirMenuCuenta(p, etiqueta);

        } else {
            p.sendMessage(ChatColor.YELLOW +
                    "Uso: /bmi banco <Etiqueta> | /bmi banco cuenta <Etiqueta>");
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return Collections.emptyList();

        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());

        // Primer argumento: lista de subcomandos
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> p.hasPermission("menuinteractivo.banco." + switch (s) {
                        case "crear" -> "crear";
                        case "pendientes" -> "pendientes";
                        case "aprobar" -> "aprobar";
                        case "rechazar" -> "rechazar";
                        case "listar" -> "lista";
                        case "unir" -> "unirse";
                        case "salir" -> "salir";
                        case "saldo" -> "saldo";
                        case "depositar", "retirar" -> s;
                        case "banco" -> "banco";
                        case "historial" -> "historial";
                        case "monedas" -> "monedas";
                        case "renombrar" -> "renombrar";
                        case "ayuda" -> "ayuda";
                        default -> "";
                    }) && s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase();
        if (!SUBS.contains(sub)) return Collections.emptyList();

        switch (sub) {
            case "crear" -> {
                if (!p.hasPermission("menuinteractivo.banco.crear")) return Collections.emptyList();

                if (args.length == 2) {
                    return List.of("banco").stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }

                if (args.length == 3) {
                    return List.of("BancoCentral", "BancoReal", "Fondo", "CajaAhorro").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }

                if (args.length == 4) {
                    return List.of("central", "real", "fondo", "ahorro", "economia", "tesoro").stream()
                            .filter(s -> s.startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }

            case "pendientes" -> {
                if (!p.hasPermission("menuinteractivo.banco.pendientes")) return Collections.emptyList();
            }
            case "aprobar", "rechazar" -> {
                if (!p.hasPermission("menuinteractivo.banco." + sub)) return Collections.emptyList();
                if (args.length == 2 && reino != null) {
                    List<String> opciones = bancoManager.obtenerBancosPendientes(reino).stream()
                            .map(Banco::getEtiqueta)
                            .collect(Collectors.toList());
                    if (sub.equals("aprobar")) opciones.add("contrato");
                    return opciones.stream()
                            .filter(e -> e.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("contrato") && reino != null) {
                    return bancoManager.obtenerBancosPendientes(reino).stream()
                            .map(Banco::getEtiqueta)
                            .filter(e -> e.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            case "listar" -> {
                if (!p.hasPermission("menuinteractivo.banco.lista")) return Collections.emptyList();
            }
            case "unir", "salir", "saldo", "historial" -> {
                String perm = switch (sub) {
                    case "unir" -> "menuinteractivo.banco.unirse";
                    case "salir" -> "menuinteractivo.banco.salir";
                    default -> "menuinteractivo.banco." + sub;
                };
                if (!p.hasPermission(perm)) return Collections.emptyList();
                if (args.length == 2 && reino != null) {
                    return bancoManager.obtenerBancosDeReino(reino).stream()
                            .map(Banco::getEtiqueta)
                            .filter(e -> e.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            case "banco" -> {
                if (!p.hasPermission("menuinteractivo.banco.banco")) return Collections.emptyList();
                if (args.length == 2 && reino != null) {
                    List<String> etiquetas = bancoManager.obtenerBancosDeReino(reino).stream()
                            .map(Banco::getEtiqueta)
                            .collect(Collectors.toList());
                    etiquetas.add("cuenta");
                    return etiquetas.stream()
                            .filter(e -> e.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("cuenta") && reino != null) {
                    return bancoManager.obtenerBancosDeReino(reino).stream()
                            .map(Banco::getEtiqueta)
                            .filter(e -> e.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            case "contrato" -> {
                if (!p.hasPermission("menuinteractivo.banco")) return Collections.emptyList();
                if (args.length == 2) {
                    return bancoManager.obtenerReinosDisponibles().stream()
                            .filter(r -> r.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 3) return List.of("1d", "7d", "1w", "1m", "1y");
                if (args.length == 4) return List.of("permite");
                if (args.length == 5) return List.of("\"imprimir, quemar\"", "\"imprimir, convertir\"", "\"imprimir, quemar, convertir\"");
            }
            case "imprimir", "quemar" -> {
                if (!p.hasPermission("menuinteractivo.banco")) return Collections.emptyList();
                if (args.length == 2) {
                    return bancoManager.obtenerMonedasJugables().stream()
                            .map(m -> "\"" + m + "\"")
                            .filter(m -> m.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 3) {
                    return List.of("100", "500", "1000").stream()
                            .filter(m -> m.startsWith(args[2]))
                            .collect(Collectors.toList());
                }
            }
            case "convertir" -> {
                if (!p.hasPermission("menuinteractivo.banco")) return Collections.emptyList();
                if (args.length == 2) return List.of("100", "500", "1000");
                if (args.length == 3) return List.of("a");
                if (args.length >= 4) {
                    return bancoManager.obtenerMonedasJugables().stream()
                            .map(m -> "\"" + m + "\"")
                            .filter(m -> m.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            case "intercambiar" -> {
                if (!p.hasPermission("menuinteractivo.banco")) return Collections.emptyList();
                if (args.length == 2) return List.of("100", "500", "1000");
                if (args.length == 3 || args.length == 5) {
                    return bancoManager.obtenerMonedasJugables().stream()
                            .map(m -> "\"" + m + "\"")
                            .filter(m -> m.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 4) return List.of("a");
            }
            case "renombrar" -> {
                if (!p.hasPermission("menuinteractivo.banco.renombrar")) return Collections.emptyList();
                // /bmi renombrar banco <etiquetaActual> <nuevoNombre> <nuevaEtiqueta>
                if (args.length == 2) {
                    return List.of("banco").stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("banco")) {
                    // Sugerir bancos donde el jugador es propietario
                    return bancoManager.obtenerBancosDeJugador(p.getUniqueId(), true).stream() // true = solo propietario
                            .map(Banco::getEtiqueta)
                            .filter(e -> e.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 4 && args[1].equalsIgnoreCase("banco")) {
                    return List.of("NuevoNombre", "BancoDelPueblo", "BancoVIP").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 5 && args[1].equalsIgnoreCase("banco")) {
                    return List.of("nueva_etiqueta", "vip", "central", "oro").stream()
                            .filter(s -> s.startsWith(args[4].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        return Collections.emptyList();
    }
}
