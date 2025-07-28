package AndromeDraick.menuInteractivo.model;

import java.util.UUID;

public record ItemEnVenta(
        int id,
        UUID uuidVendedor,
        String reino,
        String itemSerializado,
        int cantidad,
        double precio
) {
}
