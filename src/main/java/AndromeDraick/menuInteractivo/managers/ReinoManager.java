package AndromeDraick.menuInteractivo.managers;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.model.Reino;
import java.util.List;
import java.util.UUID;

public class ReinoManager {
    private final GestorBaseDeDatos db;

    public ReinoManager(GestorBaseDeDatos db) {
        this.db = db;
    }

    public boolean crearReino(String etiqueta, String nombre, UUID reyUUID) {
        return db.crearReino(etiqueta, nombre, reyUUID);
    }

    public List<Reino> listarReinos() {
        return db.listarReinos();
    }

    public boolean eliminarReino(String etiqueta) {
        return db.eliminarReino(etiqueta);
    }

    public boolean unirReino(UUID jugadorUUID, String etiquetaReino) {
        return db.unirJugadorReino(jugadorUUID, etiquetaReino);
    }

    public boolean salirReino(UUID jugadorUUID, String etiquetaReino) {
        return db.salirJugadorReino(jugadorUUID, etiquetaReino);
    }
}

