package AndromeDraick.menuInteractivo;

import AndromeDraick.menuInteractivo.comandos.Comandos;
import AndromeDraick.menuInteractivo.comandos.ComandosBanco;
import AndromeDraick.menuInteractivo.comandos.ComandosReino;
import AndromeDraick.menuInteractivo.configuracion.ConfigTiendaManager;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.listeners.BancoMenuListener;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.menu.EventosMenu;
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

    @Override
    public void onEnable() {
        instancia = this;

        // Vault
        if (!setupEconomy()) {
            getLogger().severe("No se encontró un sistema de economía (Vault + plugin). ¡Desactivando!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // LuckPerms
        try {
            permisos = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms no está cargado correctamente.");
        }

        // Sistema de trabajos
        trabajos = new SistemaTrabajos();

        // Cargar config_tienda.yml
        saveResource("config_tienda.yml", false);
        configTienda = new ConfigTiendaManager(this);

        // Crear carpeta configuracion y cargar base de datos
        File carpetaConfig = new File(getDataFolder(), "configuracion");
        if (!carpetaConfig.exists()) carpetaConfig.mkdirs();

        File archivoBD = new File(carpetaConfig, "config_basededatos.yml");
        if (!archivoBD.exists()) saveResource("configuracion/config_basededatos.yml", false);

        baseDeDatos = new GestorBaseDeDatos(this); // Se conecta automáticamente en el constructor

        // Registrar comandos del menú y tienda
        Comandos comandos = new Comandos();
        getCommand("menu").setExecutor(comandos);
        getCommand("tmi").setExecutor(comandos);

        // Registrar comandos de reinos y bancos
        getCommand("rnmi").setExecutor(new ComandosReino(baseDeDatos));


        BancoManager bancoManager = new BancoManager(baseDeDatos);
        getCommand("bmi").setExecutor(new ComandosBanco(bancoManager, getEconomia()));
        getServer().getPluginManager().registerEvents(new BancoMenuListener(this), this);


        // Registrar eventos de menú y banco
        Bukkit.getPluginManager().registerEvents(new EventosMenu(), this);
        getServer().getPluginManager().registerEvents(new BancoMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuTrabajos.TrabajoJoinListener(), this);

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

    public GestorBaseDeDatos getBaseDeDatos() {
        return baseDeDatos;
    }
}
