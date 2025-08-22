package AndromeDraick.menuInteractivo.economia;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.utilidades.FoliaCompat;
import AndromeDraick.menuInteractivo.database.HikariProvider;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PagoPorTiempoManager implements Listener {

    private final MenuInteractivo plugin;
    private Economy economy;
    private Permission perms;
    private YamlConfiguration cfg;
    private Object schedulerHandle;

    private final Map<UUID, Progreso> progreso = new ConcurrentHashMap<>();
    private final Map<String, Tarifas> tarifas = new HashMap<>();

    private boolean habilitado;
    private boolean msgMinuto;
    private boolean msgHora;
    private boolean msg12h;

    public PagoPorTiempoManager(MenuInteractivo plugin) { this.plugin = plugin; }

    /* ===== Ciclo de vida ===== */
    public void start() {
        if (!cargarConfig() || !hookVault()) {
            plugin.getLogger().warning("[MI] PagoPorTiempo deshabilitado (config o Vault faltante).");
            return;
        }
        crearTabla();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        schedulerHandle = FoliaCompat.runGlobalRepeating(plugin, 20L * 60, 20L * 60, this::tickMinuto);
        if (schedulerHandle == null) plugin.getLogger().warning("[MI] No se pudo iniciar tarea PagoPorTiempo.");
    }

    public void stop() {
        FoliaCompat.cancel(plugin, schedulerHandle);
        for (Map.Entry<UUID, Progreso> e : progreso.entrySet()) guardarProgreso(e.getKey(), e.getValue());
        HandlerList.unregisterAll(this);
    }

    public void reload() {
        boolean estabaOn = schedulerHandle != null;
        FoliaCompat.cancel(plugin, schedulerHandle);
        schedulerHandle = null;
        cargarConfig();
        if (estabaOn) schedulerHandle = FoliaCompat.runGlobalRepeating(plugin, 20L * 60, 20L * 60, this::tickMinuto);
    }

    /* ===== Eventos ===== */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        progreso.put(e.getPlayer().getUniqueId(), cargarProgreso(e.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        Progreso p = progreso.remove(id);
        if (p != null) guardarProgreso(id, p);
    }

    /* ===== Loop por minuto ===== */
    private void tickMinuto() {
        if (!habilitado || economy == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            FoliaCompat.runOnPlayerThread(plugin, p, () -> procesarMinuto(p));
        }
    }

    private void procesarMinuto(Player p) {
        UUID id = p.getUniqueId();
        Progreso prog = progreso.computeIfAbsent(id, k -> new Progreso());
        String grupo = obtenerGrupoPrimario(p);
        Tarifas t = tarifas.getOrDefault(grupo, tarifas.get("default"));

        // Pago por minuto
        if (t.minuto > 0) {
            economy.depositPlayer(p, t.minuto);
            if (msgMinuto)
                p.sendMessage(ChatColor.GREEN + "Has recibido " + ChatColor.YELLOW + t.minuto
                        + ChatColor.GREEN + " por 1 minuto (" + ChatColor.AQUA + grupo + ChatColor.GREEN + ").");
        }

        prog.minutosParaHora++;
        prog.minutosPara12h++;

        // Bono por hora
        while (prog.minutosParaHora >= 60) {
            prog.minutosParaHora -= 60;
            if (t.hora > 0) {
                economy.depositPlayer(p, t.hora);
                if (msgHora)
                    p.sendMessage(ChatColor.GOLD + "¡Bono 1h! +" + ChatColor.YELLOW + t.hora
                            + ChatColor.GOLD + " (" + ChatColor.AQUA + grupo + ChatColor.GOLD + ")");
            }
        }

        // Bono por 12 horas
        while (prog.minutosPara12h >= 720) {
            prog.minutosPara12h -= 720;
            if (t.doceHoras > 0) {
                economy.depositPlayer(p, t.doceHoras);
                if (msg12h)
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "¡Bono 12h! +" + ChatColor.YELLOW + t.doceHoras
                            + ChatColor.LIGHT_PURPLE + " (" + ChatColor.AQUA + grupo + ChatColor.LIGHT_PURPLE + ")");
            }
        }
    }

    /* ===== Config / Vault ===== */
    private boolean cargarConfig() {
        try {
            File dir = new File(plugin.getDataFolder(), "configuracion");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "tiempo_pago.yml");
            if (!file.exists()) {
                YamlConfiguration def = new YamlConfiguration();
                def.set("habilitado", true);
                def.set("mensajes.minuto", false);
                def.set("mensajes.hora", true);
                def.set("mensajes.doce_horas", true);
                Map<String, double[]> m = new LinkedHashMap<>();
                m.put("default",   new double[]{0.3, 10, 100});
                m.put("piedra",    new double[]{0.4, 15, 150});
                m.put("platino",   new double[]{0.6, 22, 220});
                m.put("diamante",  new double[]{0.8, 30, 300});
                m.put("esmeralda", new double[]{1.0, 50, 500});
                m.put("netherita", new double[]{1.8, 100, 850});
                for (Map.Entry<String, double[]> e : m.entrySet()) {
                    def.set("grupos." + e.getKey() + ".minuto", e.getValue()[0]);
                    def.set("grupos." + e.getKey() + ".hora", e.getValue()[1]);
                    def.set("grupos." + e.getKey() + ".doce_horas", e.getValue()[2]);
                }
                def.save(file);
            }
            this.cfg = YamlConfiguration.loadConfiguration(file);
            this.habilitado = cfg.getBoolean("habilitado", true);
            this.msgMinuto = cfg.getBoolean("mensajes.minuto", false);
            this.msgHora = cfg.getBoolean("mensajes.hora", true);
            this.msg12h = cfg.getBoolean("mensajes.doce_horas", true);

            tarifas.clear();
            if (cfg.isConfigurationSection("grupos")) {
                for (String g : cfg.getConfigurationSection("grupos").getKeys(false)) {
                    double mi = cfg.getDouble("grupos." + g + ".minuto", 0.0);
                    double ho = cfg.getDouble("grupos." + g + ".hora", 0.0);
                    double dh = cfg.getDouble("grupos." + g + ".doce_horas", 0.0);
                    tarifas.put(g.toLowerCase(Locale.ROOT), new Tarifas(mi, ho, dh));
                }
            }
            tarifas.putIfAbsent("default", new Tarifas(0.3, 10, 100));
            return true;
        } catch (Exception ex) {
            plugin.getLogger().severe("[MI] Error cargando tiempo_pago.yml: " + ex.getMessage());
            return false;
        }
    }

    private boolean hookVault() {
        try {
            RegisteredServiceProvider<Economy> eco = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (eco == null) { plugin.getLogger().severe("[MI] Vault Economy no encontrado."); return false; }
            economy = eco.getProvider();

            RegisteredServiceProvider<Permission> p = Bukkit.getServicesManager().getRegistration(Permission.class);
            if (p != null) perms = p.getProvider();
            if (perms == null) plugin.getLogger().warning("[MI] Vault Permission no encontrado (usaré 'default').");
            return true;
        } catch (Throwable t) {
            plugin.getLogger().severe("[MI] Error hookeando Vault: " + t.getMessage());
            return false;
        }
    }

    private String obtenerGrupoPrimario(Player p) {
        String g = "default";
        try {
            if (perms != null) {
                String primary = perms.getPrimaryGroup(p);
                if (primary != null && !primary.isBlank()) g = primary.toLowerCase(Locale.ROOT);
            }
        } catch (Throwable ignored) {}
        if (!tarifas.containsKey(g)) g = "default";
        return g;
    }

    /* ===== SQL ===== */
    private void crearTabla() {
        String sql = "CREATE TABLE IF NOT EXISTS tiempo_juego_pagos ("
                + "uuid TEXT PRIMARY KEY,"
                + "minutos_para_hora INTEGER DEFAULT 0,"
                + "minutos_para_12h INTEGER DEFAULT 0,"
                + "ultima_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        try (Connection c = HikariProvider.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) { plugin.getLogger().severe("[MI] Error creando tabla tiempo_juego_pagos: " + e.getMessage()); }
    }

    private Progreso cargarProgreso(UUID uuid) {
        Progreso p = new Progreso();
        String sql = "SELECT minutos_para_hora, minutos_para_12h FROM tiempo_juego_pagos WHERE uuid = ?";
        try (Connection c = HikariProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p.minutosParaHora = rs.getInt("minutos_para_hora");
                    p.minutosPara12h = rs.getInt("minutos_para_12h");
                } else {
                    insertarFila(uuid);
                }
            }
        } catch (SQLException e) { plugin.getLogger().warning("[MI] cargarProgreso: " + e.getMessage()); }
        return p;
    }

    private void insertarFila(UUID uuid) {
        String up = "REPLACE INTO tiempo_juego_pagos (uuid, minutos_para_hora, minutos_para_12h, ultima_actualizacion) "
                + "VALUES (?, 0, 0, CURRENT_TIMESTAMP)";
        try (Connection c = HikariProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(up)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning("[MI] insertarFila: " + e.getMessage()); }
    }

    private void guardarProgreso(UUID uuid, Progreso p) {
        String up = "REPLACE INTO tiempo_juego_pagos (uuid, minutos_para_hora, minutos_para_12h, ultima_actualizacion) "
                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection c = HikariProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(up)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, p.minutosParaHora);
            ps.setInt(3, p.minutosPara12h);
            ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning("[MI] guardarProgreso: " + e.getMessage()); }
    }

    /* ===== DTOs ===== */
    private static final class Progreso { int minutosParaHora = 0; int minutosPara12h = 0; }
    private static final class Tarifas {
        final double minuto, hora, doceHoras;
        Tarifas(double min, double h, double d) { this.minuto = min; this.hora = h; this.doceHoras = d; }
    }
}
