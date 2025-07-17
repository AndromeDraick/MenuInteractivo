package AndromeDraick.menuInteractivo.utilidades;

import org.bukkit.Material;

import java.util.*;

public class HistorialComprasManager {

    private static final Map<UUID, LinkedList<Material>> historial = new HashMap<>();
    private static final int MAX = 5;

    public static void registrarCompra(UUID uuid, Material material) {
        historial.putIfAbsent(uuid, new LinkedList<>());
        LinkedList<Material> lista = historial.get(uuid);

        lista.remove(material); // evitar duplicados
        lista.addFirst(material);

        while (lista.size() > MAX) {
            lista.removeLast();
        }
    }

    public static List<Material> obtenerRecientes(UUID uuid) {
        return historial.getOrDefault(uuid, new LinkedList<>());
    }
}
