package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.configuracion.ConfigTiendaManager;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import AndromeDraick.menuInteractivo.utilidades.SistemaTrabajos;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class MenuPrincipal implements Listener {

    private final MenuInteractivo plugin;
    private static final String TITULO_BASE = ChatColor.DARK_GREEN + "Menú";

    public MenuPrincipal(MenuInteractivo plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void abrir(Player jugador) {
        ConfigTiendaManager config = plugin.getConfigTienda();
        double dinero = plugin.getEconomia().getBalance(jugador);
        String titulo = TITULO_BASE + " §7($ " + FormateadorNumeros.formatear(dinero) + ")";
        Inventory menu = Bukkit.createInventory(null, 54, titulo);

        // == BORDES decorativos ==
        ItemStack borde = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta metaBorde = borde.getItemMeta();
        metaBorde.setDisplayName(" ");
        borde.setItemMeta(metaBorde);

        int[] slotsBorde = {0, 1, 7, 8, 9, 17, 36, 44, 45, 46, 52, 53};
        for (int slot : slotsBorde) {
            menu.setItem(slot, borde);
        }

        // == Ítem de tienda ==
        ItemStack tienda = new ItemStack(Material.EMERALD);
        ItemMeta meta = tienda.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Tienda");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para abrir la tienda"));
        tienda.setItemMeta(meta);
        menu.setItem(21, tienda);

        // == Ítem de trabajos ==
        ItemStack trabajoItem = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta metaTrabajo = trabajoItem.getItemMeta();
        metaTrabajo.setDisplayName(ChatColor.YELLOW + "Unirte a un trabajo");
        metaTrabajo.setLore(Arrays.asList(
                ChatColor.GRAY + "Haz clic para ver los trabajos disponibles.",
                ChatColor.GRAY + "¡Cada trabajo desbloquea ítems únicos!"
        ));
        trabajoItem.setItemMeta(metaTrabajo);
        menu.setItem(23, trabajoItem);

        // == Ítem de bancos ==
        if (jugador.hasPermission("bmi.comandos.aceptar.banco")) {
            ItemStack bancoItem = new ItemStack(Material.GOLD_INGOT);
            ItemMeta metaBanco = bancoItem.getItemMeta();
            metaBanco.setDisplayName(ChatColor.GOLD + "Solicitudes de Bancos de tu Reino");
            metaBanco.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para revisar solicitudes de bancos"));
            bancoItem.setItemMeta(metaBanco);
            menu.setItem(22, bancoItem);
        }

        // == Perfil del jugador ==
        ItemStack cabeza = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) cabeza.getItemMeta();
        skullMeta.setDisplayName(ChatColor.AQUA + "Tu Perfil");
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(jugador.getUniqueId()));

        // == Ítem de gestión del reino ==
        ItemStack reino = new ItemStack(Material.CREEPER_BANNER_PATTERN);
        ItemMeta metaReino = reino.getItemMeta();
        metaReino.setDisplayName(ChatColor.GOLD + "Información del Reino");
        metaReino.setLore(Collections.singletonList(ChatColor.GRAY + "Haz clic para ver tu reino"));
        reino.setItemMeta(metaReino);
        menu.setItem(31, reino);

        // Vender ítems
        ItemStack vender = new ItemStack(Material.CHEST);
        ItemMeta metaVender = vender.getItemMeta();
        metaVender.setDisplayName(ChatColor.GREEN + "Vender al Mercado del Reino");
        metaVender.setLore(Arrays.asList(
                ChatColor.GRAY + "Haz clic para poner tus ítems en venta",
                ChatColor.GRAY + "por monedas del reino.",
                ChatColor.LIGHT_PURPLE + "Se agregó el valor conforme",
                ChatColor.LIGHT_PURPLE + "esté tu moneda del reino"
        ));
        vender.setItemMeta(metaVender);
        menu.setItem(39, vender);

        // Mercado
        ItemStack mercado = new ItemStack(Material.JUNGLE_SIGN);
        ItemMeta metaMercado = mercado.getItemMeta();
        metaMercado.setDisplayName(ChatColor.AQUA + "Mercado del Reino");
        metaMercado.setLore(Arrays.asList(
                ChatColor.GRAY + "Haz clic para ver lo que venden",
                ChatColor.GRAY + "otros miembros del reino.",
                ChatColor.LIGHT_PURPLE + "Se agregó el valor conforme",
                ChatColor.LIGHT_PURPLE + "esté tu moneda del reino"
        ));
        mercado.setItemMeta(metaMercado);
        menu.setItem(40, mercado);

        // Monedero
        ItemStack cuenta = new ItemStack(Material.FLOWER_BANNER_PATTERN);
        ItemMeta metaCuenta = cuenta.getItemMeta();
        metaCuenta.setDisplayName(ChatColor.GOLD + "Tu Monedero Real");
        metaCuenta.setLore(Arrays.asList(
                ChatColor.GRAY + "Haz clic para ver tu saldo",
                ChatColor.GRAY + "en cada reino donde participas."
        ));
        cuenta.setItemMeta(metaCuenta);
        menu.setItem(41, cuenta);

        // Grupo del jugador con LuckPerms
        String grupo = "Desconocido";
        LuckPerms lp = plugin.getPermisos();
        if (lp != null) {
            User user = lp.getUserManager().getUser(jugador.getUniqueId());
            if (user != null) {
                grupo = user.getPrimaryGroup();
            }
        }

        // Trabajo del jugador
        SistemaTrabajos sistema = plugin.getSistemaTrabajos();
        String trabajo = sistema.getTrabajo(jugador.getUniqueId());

        // Base de datos
        GestorBaseDeDatos db = plugin.getBaseDeDatos();

        // Obtener datos de personaje
        String nombreRol = "Desconocido";
        String apellidoPaterno = "";
        String apellidoMaterno = "";
        String genero = "Sin género";
        String raza = "Sin raza";

        Map<String, Object> datos = db.consultarFila(
                "SELECT nombre_rol, apellido_paterno_rol, apellido_materno_rol, genero, raza_rol " +
                        "FROM genero_jugador WHERE uuid = ?",
                jugador.getUniqueId().toString()
        );

        if (datos != null && !datos.isEmpty()) {
            nombreRol = String.valueOf(datos.get("nombre_rol"));
            apellidoPaterno = String.valueOf(datos.get("apellido_paterno_rol"));
            apellidoMaterno = String.valueOf(datos.get("apellido_materno_rol"));
            genero = String.valueOf(datos.get("genero"));
            raza = String.valueOf(datos.get("raza_rol"));
        }

        // Lore del perfil
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "N.DeUsuario: " + ChatColor.GRAY + jugador.getName());
        lore.add(ChatColor.GRAY + "Nombre: " + ChatColor.DARK_PURPLE + nombreRol);
        lore.add(ChatColor.GRAY + "Apellidos: " + ChatColor.DARK_PURPLE + apellidoPaterno + " " + apellidoMaterno);
        lore.add(ChatColor.GRAY + "Género: " + ChatColor.DARK_PURPLE + genero);
        lore.add(ChatColor.GRAY + "Trabajo: " + ChatColor.BLUE + trabajo);
        lore.add(ChatColor.GRAY + "Raza: " + ChatColor.YELLOW + raza);
        lore.add(ChatColor.GRAY + "Reinas: " + ChatColor.GREEN + "$" + FormateadorNumeros.formatear(dinero));
        lore.add(ChatColor.GOLD + "");
        lore.add(ChatColor.GOLD + "¡PARTE 1.8 DE 6: " + ChatColor.GRAY + "");
        lore.add(ChatColor.GRAY + "Actualización " + ChatColor.AQUA + " 1.8 " + ChatColor.GRAY + " trajo:");
        lore.add(ChatColor.GRAY + " -Recompensa Diaria!");
        lore.add(ChatColor.GRAY + " -Más de 32 Roles!");
        lore.add(ChatColor.GRAY + " -Correcciones Menores");

        skullMeta.setLore(lore);
        cabeza.setItemMeta(skullMeta);
        menu.setItem(13, cabeza);

        // == Recompensa diaria ==
        long ahora = System.currentTimeMillis();
        long ultimoReclamo = db.obtenerUltimoReclamo(jugador.getUniqueId());

        boolean disponible = (ultimoReclamo == 0L || (ahora - ultimoReclamo) >= 86400000L);

        ItemStack recompensa = new ItemStack(disponible ? Material.DIAMOND : Material.COAL);
        ItemMeta metaRecompensa = recompensa.getItemMeta();
        metaRecompensa.setDisplayName(disponible
                ? ChatColor.AQUA + "¡Recompensa Diaria Disponible!"
                : ChatColor.GRAY + "Recompensa Diaria");

        List<String> loreRecompensa = new ArrayList<>();
        if (disponible) {
            loreRecompensa.add(ChatColor.GREEN + "¡Haz clic para reclamar tu recompensa!");
            loreRecompensa.add(ChatColor.YELLOW + "Recompensa base: $285.71");
            loreRecompensa.add(ChatColor.GOLD + "Rangos obtienen más dinero.");
        } else {
            long faltante = 86400000L - (ahora - ultimoReclamo);
            long horas = faltante / 3600000L;
            long minutos = (faltante % 3600000L) / 60000L;

            loreRecompensa.add(ChatColor.RED + "Ya reclamaste tu recompensa diaria.");
            loreRecompensa.add(ChatColor.GRAY + "Próxima en: " +
                    ChatColor.YELLOW + horas + "h " + minutos + "m");
        }

        metaRecompensa.setLore(loreRecompensa);
        recompensa.setItemMeta(metaRecompensa);
        menu.setItem(30, recompensa);

        // Aviso si está disponible
        if (disponible) {
            jugador.sendMessage(ChatColor.GOLD + "¡Tu recompensa diaria está lista para reclamar!");
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }

        // Abrir menú
        jugador.openInventory(menu);
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(TITULO_BASE)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        switch (slot) {
            case 21 -> MenuTienda.abrir(jugador, 0);
            case 22 -> {
                if (jugador.hasPermission("bmi.comandos.aceptar.banco")) {
                    plugin.getMenuBancos().abrirSolicitudes(jugador);
                } else {
                    jugador.sendMessage(ChatColor.RED + "No tienes permiso para gestionar bancos.");
                }
            }
            case 23 -> plugin.getMenuTrabajos().abrir(jugador);
            case 30 -> manejarRecompensaDiaria(jugador, event);
            case 31 -> plugin.getMenuReino().abrir(jugador);
            case 39 -> MenuMercadoReino.abrirMenuVenta(jugador);
            case 40 -> MenuMercadoReino.abrirMercadoDelReino(jugador);
            case 41 -> MenuMercadoReino.abrirCuentaPersonal(jugador);
        }
    }

    private void manejarRecompensaDiaria(Player jugador, InventoryClickEvent event) {
        GestorBaseDeDatos db = plugin.getBaseDeDatos();
        long ahora = System.currentTimeMillis();
        long ultimoReclamo = db.obtenerUltimoReclamo(jugador.getUniqueId());

        if (ultimoReclamo > 0 && (ahora - ultimoReclamo) < 86400000L) {
            long faltante = 86400000L - (ahora - ultimoReclamo);
            long horas = faltante / 3600000L;
            jugador.sendMessage(ChatColor.RED + "¡Ya reclamaste tu recompensa diaria!");
            jugador.sendMessage(ChatColor.GRAY + "Podrás reclamar de nuevo en aproximadamente " + horas + " horas.");
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
            return;
        }

        // --- Cálculo de recompensa ---
        double recompensaBase = 285.71;
        double recompensa = recompensaBase;

        String grupoJugador = "default";
        LuckPerms lp = plugin.getPermisos();
        if (lp != null) {
            User user = lp.getUserManager().getUser(jugador.getUniqueId());
            if (user != null) {
                grupoJugador = user.getPrimaryGroup().toLowerCase();
            }
        }

        switch (grupoJugador) {
            case "piedra" -> recompensa = 295.0;
            case "platino" -> recompensa = 328.56;
            case "diamante" -> recompensa = 399.99;
            case "esmeralda" -> recompensa = 499.99;
            case "netherita" -> recompensa = 999.99;
            default -> recompensa = recompensaBase;
        }

        // Entregar dinero
        plugin.getEconomia().depositPlayer(jugador, recompensa);
        double extra = recompensa - recompensaBase;

        // Mensaje al jugador
        if (extra > 0) {
            jugador.sendMessage(ChatColor.GREEN + "¡Has reclamado tu recompensa diaria de $" +
                    FormateadorNumeros.formatear(recompensa) +
                    ChatColor.YELLOW + " (+" + FormateadorNumeros.formatear(extra) +
                    " extra por tu rango)!");
        } else {
            jugador.sendMessage(ChatColor.GREEN + "¡Has reclamado tu recompensa diaria de $" +
                    FormateadorNumeros.formatear(recompensa) + "!");
        }

        // Broadcast anónimo con monto
        if (grupoJugador.equals("diamante") || grupoJugador.equals("esmeralda") || grupoJugador.equals("netherita")) {
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[Reinos Y Naciones] " +
                    ChatColor.YELLOW + "¡Se ha reclamado una recompensa diaria de " +
                    ChatColor.GOLD + "$" + FormateadorNumeros.formatear(recompensa) + ChatColor.YELLOW + "!");
        }

        jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // Guardar el nuevo tiempo de reclamo
        db.actualizarUltimoReclamo(jugador.getUniqueId(), ahora);

        // Actualizar el ítem en el inventario
        ItemStack espera = new ItemStack(Material.COAL);
        ItemMeta metaEspera = espera.getItemMeta();
        metaEspera.setDisplayName(ChatColor.GRAY + "Recompensa Diaria");
        metaEspera.setLore(Collections.singletonList(ChatColor.RED + "Ya fue reclamada. Vuelve mañana."));
        espera.setItemMeta(metaEspera);

        event.getInventory().setItem(30, espera);
    }
}
