package AndromeDraick.menuInteractivo;

import AndromeDraick.menuInteractivo.comandos.Comandos;
import AndromeDraick.menuInteractivo.comandos.ComandosBMI;
import AndromeDraick.menuInteractivo.comandos.ComandosReino;
import AndromeDraick.menuInteractivo.configuracion.ConfigTiendaManager;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.listeners.BancoMenuListener;
import AndromeDraick.menuInteractivo.menu.EventosMenu;
import AndromeDraick.menuInteractivo.menu.MenuBancos;
import AndromeDraick.menuInteractivo.menu.MenuTrabajos;
import AndromeDraick.menuInteractivo.utilidades.SistemaTrabajos;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MenuInteractivo extends JavaPlugin {

    private static MenuInteractivo instancia;
    private ConfigTiendaManager configTienda;
    private Economy economia;
    private LuckPerms permisos;
    private SistemaTrabajos trabajos;
    private GestorBaseDeDatos baseDeDatos;
    private MenuBancos menuBancos;

    @Override
    public void onEnable() {
        instancia = this;

        // 1) Vault
        if (!setupEconomy()) {
            getLogger().severe("No se encontró Vault o un plugin de economía. Desactivando.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2) LuckPerms
        try {
            permisos = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms no cargó correctamente.");
        }

        // 3) Sistema de trabajos
        trabajos = new SistemaTrabajos();

        // 4) Configuración de tienda
        saveResource("config_tienda.yml", false);
        configTienda = new ConfigTiendaManager(this);

        // 5) Configuración y conexión a BBDD
        File cfgDir = new File(getDataFolder(), "configuracion");
        if (!cfgDir.exists()) cfgDir.mkdirs();
        File cfgBD = new File(cfgDir, "config_basededatos.yml");
        if (!cfgBD.exists()) saveResource("configuracion/config_basededatos.yml", false);
        baseDeDatos = new GestorBaseDeDatos(this);

        // 6) Comandos de menú/tienda
        Comandos comandos = new Comandos();
        getCommand("menu").setExecutor(comandos);
        getCommand("tmi").setExecutor(comandos);

        // 7) Comando de reinos (/rnmi)
        getCommand("rnmi").setExecutor(new ComandosReino(this));

        // 8) Comando de bancos (/bmi)
        getCommand("bmi").setExecutor(new ComandosBMI(this));

        // 9) Listeners
        Bukkit.getPluginManager().registerEvents(new EventosMenu(), this);
        Bukkit.getPluginManager().registerEvents(new BancoMenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuTrabajos.TrabajoJoinListener(), this);

        this.menuBancos = new MenuBancos(this);
        getServer().getPluginManager().registerEvents(menuBancos, this);

        getLogger().info("MenuInteractivo activado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MenuInteractivo desactivado.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        var rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economia = rsp.getProvider();
        return economia != null;
    }

    public static MenuInteractivo getInstancia() {
        return instancia;
    }

    public Economy getEconomia() {
        return economia;
    }

    public ConfigTiendaManager getConfigTienda() {
        return configTienda;
    }

    public LuckPerms getPermisos() {
        return permisos;
    }

    public SistemaTrabajos getSistemaTrabajos() {
        return trabajos;
    }

    public MenuBancos getMenuBancos() {
        return menuBancos;
    }

    public GestorBaseDeDatos getBaseDeDatos() {
        return baseDeDatos;
    }
}
