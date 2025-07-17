package AndromeDraick.menuInteractivo.configuracion;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ConfigTiendaManager {

    private final File archivo;
    private final FileConfiguration config;

    public ConfigTiendaManager(MenuInteractivo plugin) {
        archivo = new File(plugin.getDataFolder(), "config_tienda.yml");
        config = YamlConfiguration.loadConfiguration(archivo);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // === Obtener precios base según el tipo de atributo ===

    public double getPrecioMaterial(String nombre) {
        return config.getDouble("materiales." + nombre, 0.0);
    }

    public double getPrecioBioma(String nombre) {
        return config.getDouble("biomas." + nombre, 0.0);
    }

    public double getPrecioRareza(String nombre) {
        return config.getDouble("rareza." + nombre, 0.0);
    }

    public double getPrecioDimension(String nombre) {
        return config.getDouble("dimensiones." + nombre, 0.0);
    }

    public double getPrecioCategoria(String nombre) {
        return config.getDouble("categorias." + nombre, 0.0);
    }

    // === Descuentos por grupo y trabajo ===

    public double getDescuentoGrupo(String grupo) {
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
        for (String parte : partes) {
            rarezas.add(parte.trim().toLowerCase());
        }
        return rarezas;
    }

    // === Items personalizados ===

    public Set<String> getItemsCustom() {
        return config.getConfigurationSection("items_custom") != null
                ? config.getConfigurationSection("items_custom").getKeys(false)
                : Collections.emptySet();
    }

    public Map<String, Object> getDatosItemCustom(String nombreItem) {
        if (!config.contains("items_custom." + nombreItem)) return Collections.emptyMap();
        return config.getConfigurationSection("items_custom." + nombreItem).getValues(false);
    }

    // === Para depurar o mostrar todas las secciones principales ===

    public Set<String> getSecciones() {
        return config.getKeys(false);
    }
}
