package AndromeDraick.menuInteractivo.configuracion;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ConfigTiendaManager {
    private final FileConfiguration config;        // config_tienda.yml
    private final FileConfiguration itemsConfig;   // configuracion/config_items_venta.yml

    public ConfigTiendaManager(MenuInteractivo plugin) {
        // load tienda config
        File archivo = new File(plugin.getDataFolder(), "config_tienda.yml");
        if (!archivo.exists()) plugin.saveResource("config_tienda.yml", false);
        this.config = YamlConfiguration.loadConfiguration(archivo);

        // load items-venta config
        File archivoItems = new File(plugin.getDataFolder(), "configuracion/config_items_venta.yml");
        if (!archivoItems.exists()) plugin.saveResource("configuracion/config_items_venta.yml", false);
        this.itemsConfig = YamlConfiguration.loadConfiguration(archivoItems);
    }

    /** Acceso al config principal **/
    public FileConfiguration getConfig() {
        return config;
    }

    /** Acceso al config de items en venta **/
    public FileConfiguration getItemsConfig() {
        return itemsConfig;
    }

    // === Precios base ===
    public double getPrecioMaterial(String nombre)   { return config.getDouble("materiales." + nombre, 0.0); }
    public double getPrecioBioma(String nombre)      { return config.getDouble("biomas."    + nombre, 0.0); }
    public double getPrecioRareza(String nombre)     { return config.getDouble("rareza."    + nombre, 0.0); }
    public double getPrecioDimension(String nombre)  { return config.getDouble("dimensiones." + nombre, 0.0); }
    public double getPrecioCategoria(String nombre)  { return config.getDouble("categorias."  + nombre, 0.0); }

    // === Descuentos ===
    public double getDescuentoGrupo(String grupo)    {
        return config.getDouble("descuentos_grupos." + grupo.toLowerCase(), 0.0);
    }
    public double getDescuentoTrabajo(String trabajo) {
        return config.getDouble("trabajos." + trabajo, 0.0);
    }

    // === Rarezas desbloqueadas por grupo ===
    public List<String> getRarezasDesbloqueadas(String grupo) {
        String entrada = config.getString("grupos_desbloqueos." + grupo);
        if (entrada == null || entrada.isEmpty()) return Collections.emptyList();
        String[] partes = entrada.split(",");
        List<String> rarezas = new ArrayList<>();
        for (String p : partes) rarezas.add(p.trim().toLowerCase());
        return rarezas;
    }

    // === Items que se venden (migrados a config_items_venta.yml) ===
    public Set<String> getItemsEnVenta() {
        if (itemsConfig.getConfigurationSection("items_custom") == null) {
            return Collections.emptySet();
        }
        return itemsConfig.getConfigurationSection("items_custom").getKeys(false);
    }

    public Map<String,Object> getDatosItemVenta(String nombreItem) {
        String path = "items_custom." + nombreItem;
        if (itemsConfig.getConfigurationSection(path) == null) {
            return Collections.emptyMap();
        }
        return itemsConfig.getConfigurationSection(path).getValues(false);
    }

    // (Opcional) métodos antiguos marcados como deprecated
    @Deprecated
    public Set<String> getItemsCustom() {
        return getItemsEnVenta();
    }
    @Deprecated
    public Map<String,Object> getDatosItemCustom(String nombreItem) {
        return getDatosItemVenta(nombreItem);
    }
}
