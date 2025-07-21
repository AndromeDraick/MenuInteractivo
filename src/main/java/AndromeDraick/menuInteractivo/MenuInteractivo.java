package AndromeDraick.menuInteractivo;

import AndromeDraick.menuInteractivo.comandos.Comandos;
import AndromeDraick.menuInteractivo.comandos.ComandosBMI;
import AndromeDraick.menuInteractivo.comandos.ComandosReino;
import AndromeDraick.menuInteractivo.configuracion.ConfigTiendaManager;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.menu.*;
import AndromeDraick.menuInteractivo.utilidades.SistemaTrabajos;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.milkbowl.vault.economy.Economy;
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
    private MenuPrincipal menuPrincipal;
    private MenuTrabajos  menuTrabajos;
    private MenuReino menuReino;
    private MenuMiembrosReino menuMiembrosReino;
    private MenuMonedas menuMonedas;


    @Override
    public void onEnable() {
        instancia = this;

        getServer().getConsoleSender().sendMessage("§8===============================");
        getServer().getConsoleSender().sendMessage("§6  Plugin §eMenuInteractivo §7por §bAndromeDraick");
        ColoryEstado(true);

        // 1) Vault
        if (!setupEconomy()) {
            getLogger().severe("§c  No se encontró Vault o un plugin de economía. Desactivando.");
            getServer().getPluginManager().disablePlugin(this);
            getServer().getConsoleSender().sendMessage("§8===============================");
            return;
        }

        // 2) LuckPerms
        try {
            permisos = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().severe("§c LuckPerms no cargó correctamente.");
        }

        // 3) Sistema de trabajos
        trabajos = new SistemaTrabajos(this);

        // 4) Configuración de tienda
        File cfgDir = new File(getDataFolder(), "configuracion");
        if (!cfgDir.exists()) cfgDir.mkdirs();
        saveResource("config_tienda.yml", false);
        File cfgIV = new File(cfgDir, "config_items_venta.yml");
        if (!cfgIV.exists()) saveResource("configuracion/config_items_venta.yml", false);
        configTienda = new ConfigTiendaManager(this);
        baseDeDatos = new GestorBaseDeDatos(this);

        // 5) Configuración y conexión a BBDD
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
        getServer().getPluginManager().registerEvents(new EventosMenu(), this);

        new MenuEconomia(this);

        this.menuMiembrosReino = new MenuMiembrosReino(this);
        this.menuBancos = new MenuBancos(this);
        this.menuPrincipal = new MenuPrincipal(this);
        this.menuTrabajos  = new MenuTrabajos(this);
        this.menuReino     = new MenuReino(this);
        menuMonedas = new MenuMonedas(this);

        getLogger().info("  MenuInteractivo activado correctamente.");

        getServer().getConsoleSender().sendMessage("§6  Apóyanos en: ");
        getServer().getConsoleSender().sendMessage("§b  https://www.patreon.com/c/andromedraick/membership");
        getServer().getConsoleSender().sendMessage("§8===============================");
    }

    private void ColoryEstado(boolean activado) {
        String estado = activado ? "ACTIVADO" : "DESACTIVADO";
        String color = activado ? "§a" : "§c";
        getServer().getConsoleSender().sendMessage("§7  Versión: §f" + getDescription().getVersion());
        getServer().getConsoleSender().sendMessage("§7  Estado: " + color + estado);
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

    public MenuMonedas getMenuMonedas() {
        return menuMonedas; }

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

    public MenuPrincipal getMenuPrincipal() {
        return menuPrincipal;
    }

    public MenuTrabajos  getMenuTrabajos()  {
        return menuTrabajos;
    }

    public MenuReino     getMenuReino()     {
        return menuReino;
    }
    public MenuMiembrosReino getMenuMiembrosReino() {
        return menuMiembrosReino;
    }


    public GestorBaseDeDatos getBaseDeDatos() {
        return baseDeDatos;
    }
}
