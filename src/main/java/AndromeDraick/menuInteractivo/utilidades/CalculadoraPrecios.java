package AndromeDraick.menuInteractivo.utilidades;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public class CalculadoraPrecios {

    public static double calcularPrecioCompra(Material material, Player jugador) {
        var configTienda = MenuInteractivo.getInstancia().getConfigTienda();
        String nombreMaterial = material.name();

        Map<String, Object> item = configTienda.getDatosItemCustom(nombreMaterial);
        if (item == null || item.isEmpty()) return -1;

        String tipo = (String) item.getOrDefault("material", "Piedra");
        String bioma = ((String) item.getOrDefault("bioma", "overworld")).toLowerCase(Locale.ROOT);
        String rareza = ((String) item.getOrDefault("rareza", "comun")).toLowerCase(Locale.ROOT);
        String dimension = ((String) item.getOrDefault("dimension", "overworld")).toLowerCase(Locale.ROOT);
        String categoria = (String) item.getOrDefault("categoria", "Building_Blocks");

        double precioBase = configTienda.getPrecioMaterial(tipo);
        double extraBioma = configTienda.getPrecioBioma(bioma);
        double extraRareza = configTienda.getPrecioRareza(rareza);
        double extraDimension = configTienda.getPrecioDimension(dimension);
        double extraCategoria = configTienda.getPrecioCategoria(categoria);

        double precioTotal = precioBase + extraBioma + extraRareza + extraDimension + extraCategoria;

        // Descuento por grupo
        double descuentoGrupo = configTienda.getDescuentoGrupo(jugador.getName());
        precioTotal *= (1.0 - descuentoGrupo / 100.0);

        // Descuento por trabajo
        precioTotal = aplicarDescuentoPorTrabajo(jugador, precioTotal);

        return precioTotal;
    }

    public static double calcularPrecioVenta(Material material, Player jugador) {
        double compra = calcularPrecioCompra(material, jugador);
        if (compra < 0) return -1;
        return compra * 0.8; // 20% menos al vender
    }

    public static double aplicarDescuentoPorTrabajo(Player jugador, double precio) {
        var configTienda = MenuInteractivo.getInstancia().getConfigTienda();
        var sistemaTrabajos = MenuInteractivo.getInstancia().getSistemaTrabajos();
        String trabajo = sistemaTrabajos.getTrabajo(jugador);
        if (trabajo != null) {
            double descuento = configTienda.getDescuentoTrabajo(trabajo);
            precio -= (precio * descuento / 100.0);
        }
        return precio;
    }
}
