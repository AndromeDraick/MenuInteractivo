package AndromeDraick.menuInteractivo.managers;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.model.Banco;

import java.util.List;
import java.util.UUID;

/**
 * Encapsula operaciones de banco usando GestorBaseDeDatos.
 */
public class BancoManager {

    private final GestorBaseDeDatos db;

    public BancoManager(GestorBaseDeDatos db) {
        this.db = db;
    }

    /** Crea un nuevo banco (estado 'pendiente'). */
    public boolean crearBanco(String etiqueta, String nombre, String reinoEtiqueta, UUID propietario) {
        return db.insertarBanco(etiqueta, nombre, reinoEtiqueta, propietario);
    }

    /** Aprueba una solicitud de banco. */
    public boolean aprobarBanco(String etiquetaBanco) {
        return db.actualizarEstadoBanco(etiquetaBanco, "aprobado");
    }

    /** Rechaza una solicitud de banco. */
    public boolean rechazarBanco(String etiquetaBanco) {
        return db.actualizarEstadoBanco(etiquetaBanco, "rechazado");
    }

    /** Devuelve el objeto Banco completo por su etiqueta. */
    public Banco obtenerBanco(String etiquetaBanco) {
        return db.obtenerBancoPorEtiqueta(etiquetaBanco);
    }

    /** Lista todos los bancos aprobados de un reino. */
    public List<Banco> obtenerBancosDeReino(String etiquetaReino) {
        return db.obtenerBancosDeReino(etiquetaReino);
    }

    /** Lista todas las solicitudes de banco pendientes de un reino. */
    public List<Banco> obtenerBancosPendientes(String etiquetaReino) {
        return db.obtenerBancosPendientes(etiquetaReino);
    }

    /** Deposita un monto en el banco. */
    public boolean depositar(String etiquetaBanco, double monto) {
        return db.depositarEnBanco(etiquetaBanco, monto);
    }

    /** Retira un monto del banco (si hay saldo suficiente). */
    public boolean retirar(String etiquetaBanco, double monto) {
        return db.retirarDeBanco(etiquetaBanco, monto);
    }

    /** Obtiene el saldo actual del banco. */
    public double obtenerSaldo(String etiquetaBanco) {
        return db.obtenerSaldoBanco(etiquetaBanco);
    }

    /** Añade un socio al banco. */
    public boolean agregarSocio(String etiquetaBanco, UUID jugadorUUID) {
        return db.agregarSocioBanco(etiquetaBanco, jugadorUUID);
    }

    /** Quita un socio del banco. */
    public boolean quitarSocio(String etiquetaBanco, UUID jugadorUUID) {
        return db.quitarSocioBanco(etiquetaBanco, jugadorUUID);
    }

    /** Devuelve la etiqueta del banco al que está vinculado el jugador (o null). */
    public String obtenerBancoDeJugador(UUID jugadorUUID) {
        return db.obtenerBancoDeJugador(jugadorUUID);
    }
}
