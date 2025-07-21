package AndromeDraick.menuInteractivo.utilidades;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public class CalculadoraPrecios {

    public static double calcularPrecioCompra(Material material, Player jugador) {
        var cfg = MenuInteractivo.getInstancia().getConfigTienda();
        String key = material.name();

        Map<String,Object> itemDatos = cfg.getDatosItemVenta(key);
        if (itemDatos == null || itemDatos.isEmpty()) return -1;

        String tipo      = (String) itemDatos.getOrDefault("material", "Piedra");
        String bioma     = ((String) itemDatos.getOrDefault("bioma", "overworld"))
                .toLowerCase(Locale.ROOT);
        String rareza    = ((String) itemDatos.getOrDefault("rareza", "comun"))
                .toLowerCase(Locale.ROOT);
        String dimension = ((String) itemDatos.getOrDefault("dimension", "overworld"))
                .toLowerCase(Locale.ROOT);
        String categoria = (String) itemDatos.getOrDefault("categoria", "Building_Blocks");

        double precioBase     = cfg.getPrecioMaterial(tipo);
        double extraBioma     = cfg.getPrecioBioma(bioma);
        double extraRareza    = cfg.getPrecioRareza(rareza);
        double extraDimension = cfg.getPrecioDimension(dimension);
        double extraCategoria = cfg.getPrecioCategoria(categoria);

        double total = precioBase
                + extraBioma
                + extraRareza
                + extraDimension
                + extraCategoria;

        // Descuento por grupo
        String grupo = MenuInteractivo.getInstancia()
                .getPermisos()
                .getUserManager()
                .getUser(jugador.getUniqueId())
                .getPrimaryGroup();
        double desGrupo = cfg.getDescuentoGrupo(grupo);
        total *= (1.0 - desGrupo / 100.0);

        // Descuento por trabajo
        String trabajo = MenuInteractivo.getInstancia()
                .getSistemaTrabajos()
                .getTrabajo(jugador.getUniqueId());
        double desTrabajo = cfg.getDescuentoTrabajo(trabajo);
        total -= total * (desTrabajo / 100.0);

        return total;
    }

    public static double calcularPrecioVenta(Material material, Player jugador) {
        double precioCompra = calcularPrecioCompra(material, jugador);
        return precioCompra < 0 ? -1 : precioCompra * 0.55;
    }

}
