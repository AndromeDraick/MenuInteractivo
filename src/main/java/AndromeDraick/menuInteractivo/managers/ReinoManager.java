package AndromeDraick.menuInteractivo.managers;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.modelos.Reino;

import java.util.List;
import java.util.UUID;

public class ReinoManager {

    private final GestorBaseDeDatos db;

    public ReinoManager(GestorBaseDeDatos db) {
        this.db = db;
    }

    public boolean crearReino(Reino reino) {
        return db.insertarReino(reino);
    }

    public Reino obtenerReinoDeJugador(UUID uuidJugador) {
        return db.obtenerReinoDeJugador(uuidJugador);
    }

    public Reino obtenerReinoPorEtiqueta(String etiqueta) {
        return db.obtenerReinoPorEtiqueta(etiqueta);
    }

    public List<UUID> obtenerMiembros(String etiquetaReino) {
        return db.obtenerMiembrosDeReino(etiquetaReino);
    }

    public boolean transferirLiderazgo(UUID uuidActualRey, UUID nuevoRey) {
        return db.transferirLiderazgo(uuidActualRey, nuevoRey);
    }

    public boolean eliminarReino(String etiquetaReino) {
        return db.eliminarReino(etiquetaReino);
    }

    public boolean agregarMiembro(UUID uuidJugador, String etiquetaReino, String rol) {
        return db.agregarJugadorAReino(uuidJugador, etiquetaReino, rol);
    }

    public boolean cambiarRol(UUID uuidJugador, String nuevoRol) {
        return db.cambiarRolJugador(uuidJugador, nuevoRol);
    }

    public boolean quitarMiembro(UUID uuidJugador) {
        return db.eliminarJugadorDeReino(uuidJugador);
    }
}
