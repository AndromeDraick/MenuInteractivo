package AndromeDraick.menuInteractivo.managers;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.model.Banco;

import java.security.Timestamp;
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

    public boolean crearContrato(String bancoEtiqueta, String reinoEtiqueta, Timestamp fechaInicio, Timestamp fechaFin, String permisos) {
        return baseDeDatos.insertarContratoBancoReino(bancoEtiqueta, reinoEtiqueta, fechaInicio, fechaFin, permisos);
    }

    public String obtenerReinoDeBanco(String etiquetaBanco) {
        return baseDeDatos.obtenerReinoDeBanco(etiquetaBanco);
    }

    public String obtenerNombreMonedaDeReino(String etiquetaReino) {
        return baseDeDatos.obtenerMonedaDeReino(etiquetaReino);
    }

    public boolean tienePermisoContrato(String banco, String reino, String permiso) {
        return baseDeDatos.tienePermisoContrato(banco, reino, permiso);
    }

    public boolean incrementarCantidadImpresa(String etiquetaReino, double cantidad) {
        return baseDeDatos.aumentarMonedaImpresa(etiquetaReino, cantidad);
    }


    /** Devuelve la etiqueta del reino al que pertenece el jugador. */
    public String obtenerReinoJugador(UUID jugadorUUID) {
        return db.obtenerReinoJugador(jugadorUUID);
    }

    /** Crea un nuevo banco (estado 'pendiente'). */
    public boolean crearBanco(String etiqueta, String nombre, String reinoEtiqueta, UUID propietario) {
        return db.crearBanco(etiqueta, nombre, reinoEtiqueta, propietario);
    }

    /** Aprueba una solicitud de banco. */
    public boolean aprobarBanco(String etiquetaBanco) {
        return db.setEstadoBanco(etiquetaBanco, "aprobado");
    }

    /** Rechaza una solicitud de banco. */
    public boolean rechazarBanco(String etiquetaBanco) {
        return db.setEstadoBanco(etiquetaBanco, "rechazado");
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
    public boolean depositarBanco(String etiquetaBanco, double monto) {
        return db.depositarBanco(etiquetaBanco, monto);
    }

    /** Retira un monto del banco (si hay saldo suficiente). */
    public boolean retirarBanco(String etiquetaBanco, double monto) {
        return db.retirarBanco(etiquetaBanco, monto);
    }

    /** Obtiene el saldo actual del banco. */
    public double getSaldoBanco(String etiquetaBanco) {
        return db.getSaldoBanco(etiquetaBanco);
    }

    /** Añade un jugador como socio al banco. */
    public boolean agregarJugadorABanco(UUID jugadorUUID, String etiquetaBanco) {
        return db.agregarJugadorABanco(jugadorUUID, etiquetaBanco);
    }

    /** Elimina un jugador del banco. */
    public boolean eliminarJugadorDeBanco(UUID jugadorUUID, String etiquetaBanco) {
        return db.eliminarJugadorDeBanco(jugadorUUID, etiquetaBanco);
    }

    /** Devuelve la etiqueta del banco al que está vinculado el jugador (o null). */
    public String obtenerBancoDeJugador(UUID jugadorUUID) {
        return db.obtenerBancoDeJugador(jugadorUUID);
    }
}
