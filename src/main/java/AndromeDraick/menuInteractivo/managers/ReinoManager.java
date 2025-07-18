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

    /** Crea un nuevo reino y devuelve true si tuvo éxito. */
    public boolean crearReino(String etiqueta, String nombre, UUID reyUUID) {
        return db.crearReino(etiqueta, nombre, reyUUID);
    }

    /** Lista todos los reinos registrados. */
    public List<Reino> listarReinos() {
        return db.listarReinos();
    }

    /** Elimina un reino por su etiqueta. */
    public boolean eliminarReino(String etiqueta) {
        return db.eliminarReino(etiqueta);
    }

    /** Une a un jugador a un reino (rol \"miembro\"). */
    public boolean unirReino(UUID jugadorUUID, String etiquetaReino) {
        return db.agregarJugadorAReino(jugadorUUID, etiquetaReino, "miembro");
    }

    /** Saca a un jugador de su reino. */
    public boolean salirReino(UUID jugadorUUID) {
        return db.eliminarJugadorDeReino(jugadorUUID);
    }

    /** Devuelve la etiqueta del reino al que pertenece el jugador (o null). */
    public String obtenerReinoJugador(UUID jugadorUUID) {
        return db.obtenerReinoJugador(jugadorUUID);
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
