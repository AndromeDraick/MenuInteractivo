package AndromeDraick.menuInteractivo.managers;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.model.Reino;

import java.util.List;
import java.util.UUID;

/**
 * Encapsula operaciones de reino usando GestorBaseDeDatos.
 */
public class ReinoManager {

    private final GestorBaseDeDatos db;

    public ReinoManager(GestorBaseDeDatos db) {
        this.db = db;
    }

    /** Lista todos los reinos registrados. */
    public List<Reino> listarReinos() {
        return db.listarReinos();
    }

    /** Elimina un reino por su etiqueta. */
    public boolean eliminarReino(String etiqueta) {
        return db.eliminarReino(etiqueta);
    }

    /** Saca a un jugador de su reino. */
    public boolean salirReino(UUID jugadorUUID) {
        return db.eliminarJugadorDeReino(jugadorUUID);
    }

    /** Devuelve la etiqueta del reino al que pertenece el jugador (o null). */
    public String obtenerReinoJugador(UUID jugadorUUID) {
        return db.obtenerReinoJugador(jugadorUUID);
    }

    public boolean crearReino(String etiqueta, String nombre, String moneda, UUID reyUUID) {
        return db.crearReino(etiqueta, nombre, moneda, reyUUID);
    }

    /** Asocia un jugador a un reino con rol y título custom */
    public boolean unirReino(UUID jugadorUUID, String etiquetaReino, String rol, String titulo) {
        return db.agregarJugadorAReino(jugadorUUID, etiquetaReino, rol, titulo);
    }

    /** Devuelve el rol del jugador dentro de su reino (o null). */
    public String obtenerRolJugador(UUID jugadorUUID) {
        return db.obtenerRolJugadorEnReino(jugadorUUID);
    }

    /** Transfiere la jefatura de un reino a otro jugador. */
    public boolean transferirLiderazgo(String etiquetaReino, UUID nuevoReyUUID) {
        return db.transferirLiderazgoReino(etiquetaReino, nuevoReyUUID);
    }

    /** Obtiene la lista de UUIDs de miembros de un reino. */
    public List<UUID> obtenerMiembros(String etiquetaReino) {
        return db.obtenerMiembrosDeReino(etiquetaReino);
    }
}
