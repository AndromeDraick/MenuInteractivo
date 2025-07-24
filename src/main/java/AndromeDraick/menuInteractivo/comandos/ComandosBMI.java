package AndromeDraick.menuInteractivo.comandos;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.model.Banco;
import AndromeDraick.menuInteractivo.model.MonedasReinoInfo;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos());
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
            mostrarAyuda(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "crear"      -> cmdCrearBanco(p, args);
                case "pendientes" -> cmdListarPendientes(p);
                case "aprobar"    -> cmdAprobarRechazar(p, args, true);
                case "rechazar"   -> cmdAprobarRechazar(p, args, false);
                case "listar"     -> plugin.getMenuBancos().abrirListaActivos(p);
                case "unir"       -> cmdUnirSalir(p, args, true);
                case "salir"      -> cmdUnirSalir(p, args, false);
                case "saldo"      -> cmdSaldo(p, args);
                case "depositar", "retirar" -> cmdMoverFondos(p, args, sub.equals("depositar"));
                case "banco"      -> cmdBanco(p, args);
                case "contrato" -> cmdContrato(p, args);
                case "imprimir" -> cmdImprimirMoneda(p, args);
                case "quemar" -> cmdQuemarMoneda(p, args);
                case "convertir" -> cmdConvertirMoneda(p, args);
                case "monedas" -> plugin.getMenuMonedas().abrirMenu(p);
                case "historial" -> cmdHistorialBanco(p, args);
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
            bancoManager.agregarJugadorABanco(p.getUniqueId(), etiqueta); // nuevo paso para registrar al dueño como miembro
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
            if (!"rey".equalsIgnoreCase(rol) && !"lider".equalsIgnoreCase(rol)) {
                p.sendMessage(ChatColor.RED + "Solo el rey o líder puede aceptar/rechazar contratos.");
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

        String reino = args[1];
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
        String banco = bancoManager.obtenerBancoPropietario(uuid);
        if (banco == null) {
            p.sendMessage(ChatColor.RED + "No eres dueño de ningún banco aprobado.");
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
        } else {
            p.sendMessage(ChatColor.RED + "Error al crear contrato. ¿Ya existe uno activo?");
        }
    }

    private void cmdImprimirMoneda(Player p, String[] args) {
        if (args.length != 3) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi imprimir <moneda> <cantidad>");
            return;
        }

        String nombreMoneda = args[1];
        double cantidad;
        try {
            cantidad = Double.parseDouble(args[2]);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            p.sendMessage(ChatColor.RED + "Cantidad inválida.");
            return;
        }

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

        if (bancoManager.incrementarCantidadImpresa(reinoDelBanco, cantidad)) {
            bancoManager.registrarMovimiento(banco, "imprimir", p.getUniqueId().toString(), cantidad);
            p.sendMessage(ChatColor.GREEN + "Se imprimieron " + cantidad + " " + nombreMoneda + " para el reino " + reinoDelBanco);
        } else {
            p.sendMessage(ChatColor.RED + "Error al imprimir moneda. Revisa la consola.");
        }
    }

    private void cmdQuemarMoneda(Player p, String[] args) {
        if (args.length != 3) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi quemar <moneda> <cantidad>");
            return;
        }

        String nombreMoneda = args[1];
        double cantidad;
        try {
            cantidad = Double.parseDouble(args[2]);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            p.sendMessage(ChatColor.RED + "Cantidad inválida.");
            return;
        }

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

        if (bancoManager.incrementarCantidadQuemada(reinoDelBanco, cantidad)) {
            bancoManager.registrarMovimiento(banco, "quemar", p.getUniqueId().toString(), cantidad);
            p.sendMessage(ChatColor.YELLOW + "Se quemaron " + cantidad + " " + nombreMoneda + " del reino " + reinoDelBanco);
        } else {
            p.sendMessage(ChatColor.RED + "Error al quemar moneda. Revisa la consola.");
        }
    }

    private void cmdConvertirMoneda(Player p, String[] args) {
        if (args.length != 4 || !args[2].equalsIgnoreCase("a")) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi convertir <dineroServidor> a <moneda>");
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

        String nombreMoneda = args[3];
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

        if (bancoManager.incrementarDineroConvertido(reinoDelBanco, dineroServidor)) {
            economia.withdrawPlayer(p, dineroServidor);
            bancoManager.registrarMovimiento(banco, "convertir", p.getUniqueId().toString(), dineroServidor);
            p.sendMessage(ChatColor.GREEN + "Convertiste $" + dineroServidor + " del servidor en valor para la moneda del reino " + reinoDelBanco);
        } else {
            p.sendMessage(ChatColor.RED + "Error al convertir dinero. Revisa la consola.");
        }
    }

    private void cmdHistorialBanco(Player p, String[] args) {
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
        // /bmi intercambiar <cantidad> <moneda_origen> a <moneda_destino>
        if (args.length != 5 || !args[3].equalsIgnoreCase("a")) {
            p.sendMessage(ChatColor.YELLOW + "Uso: /bmi intercambiar <cantidad> <moneda_origen> a <moneda_destino>");
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

        String monedaOrigen = args[2];
        String monedaDestino = args[4];

        // Obtener info de ambas monedas
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

        // Calcular valor intermedio en dinero del servidor y convertirlo
        double enDineroServidor = cantidad * valorOrigen;
        double cantidadDestino = enDineroServidor / valorDestino;

        UUID uuid = p.getUniqueId();

        // Verificar saldo
        double saldoOrigen = bancoManager.getSaldoMonedaJugador(uuid, origen.getEtiquetaReino());
        if (saldoOrigen < cantidad) {
            p.sendMessage(ChatColor.RED + "No tienes suficiente saldo de " + monedaOrigen + " (" + saldoOrigen + ")");
            return;
        }

        // Realizar intercambio
        bancoManager.restarMonedaJugador(uuid, origen.getEtiquetaReino(), cantidad);
        bancoManager.sumarMonedaJugador(uuid, destino.getEtiquetaReino(), cantidadDestino);

        p.sendMessage(ChatColor.GREEN + "Intercambiaste " + cantidad + " " + monedaOrigen +
                " por " + String.format("%.2f", cantidadDestino) + " " + monedaDestino);
    }

    private void cmdUnirSalir(Player p, String[] args, boolean unir) {
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
        // /bmi banco <Etiqueta> | /bmi banco cuenta <Etiqueta>
        if (args.length == 2) {
            String etiqueta = args[1].toLowerCase();

            if (!bancoManager.esMiembroOBancoPropietario(p.getUniqueId(), etiqueta)) {
                p.sendMessage(ChatColor.RED + "No eres miembro ni dueño de este banco.");
                return;
            }

            plugin.getMenuBancos().abrirIndividual(p, etiqueta);
        } else if (args.length == 3 && args[1].equalsIgnoreCase("cuenta")) {
            cmdSaldo(p, new String[]{ "saldo", args[2] });
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

        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase();
        if (!SUBS.contains(sub)) return Collections.emptyList();

        switch (sub) {
            case "crear" -> {
                if (args.length == 2) return List.of("banco");
            }
            case "aprobar", "rechazar" -> {
                if (args.length == 2 && reino != null) {
                    return bancoManager.obtenerBancosPendientes(reino).stream()
                            .map(Banco::getEtiqueta)
                            .filter(e -> e.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            case "unir", "salir", "saldo", "historial" -> {
                if (args.length == 2 && reino != null) {
                    return bancoManager.obtenerBancosDeReino(reino).stream()
                            .map(Banco::getEtiqueta)
                            .filter(e -> e.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            case "depositar", "retirar" -> {
                if (args.length == 2 && reino != null) {
                    return bancoManager.obtenerBancosDeReino(reino).stream()
                            .map(Banco::getEtiqueta)
                            .filter(e -> e.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 3) {
                    return List.of("100", "500", "1000").stream()
                            .filter(m -> m.startsWith(args[2]))
                            .collect(Collectors.toList());
                }
            }
            case "banco" -> {
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
                if (args.length == 2) {
                    return bancoManager.obtenerMonedasJugables().stream()
                            .filter(m -> m.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 3) {
                    return List.of("100", "500", "1000").stream()
                            .filter(m -> m.startsWith(args[2]))
                            .collect(Collectors.toList());
                }
            }
            case "convertir" -> {
                if (args.length == 2) return List.of("100", "500", "1000");
                if (args.length == 3) return List.of("a");
                if (args.length == 4) {
                    return bancoManager.obtenerMonedasJugables().stream()
                            .filter(m -> m.startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            case "intercambiar" -> {
                if (args.length == 2) return List.of("100", "500", "1000");
                if (args.length == 3 || args.length == 5) {
                    return bancoManager.obtenerMonedasJugables().stream()
                            .filter(m -> m.startsWith(args[args.length - 1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 4) return List.of("a");
            }
        }

        return Collections.emptyList();
    }
}
