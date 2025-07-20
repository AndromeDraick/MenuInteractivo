package AndromeDraick.menuInteractivo.managers;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.model.Banco;
import AndromeDraick.menuInteractivo.model.MonedasReinoInfo;
import org.bukkit.Bukkit;


import java.sql.*;
import java.util.ArrayList;
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
        return db.insertarContratoBancoReino(bancoEtiqueta, reinoEtiqueta, fechaInicio, fechaFin, permisos);
    }

    public boolean incrementarCantidadQuemada(String etiquetaReino, double cantidad) {
        return db.aumentarMonedaQuemada(etiquetaReino, cantidad);
    }

    public String obtenerBancoPropietario(UUID jugadorUUID) {
        return db.obtenerBancoDeJugador(jugadorUUID);
    }

    public boolean reinoExiste(String etiquetaReino) {
        return db.obtenerReino(etiquetaReino) != null;  // o contar filas con SELECT COUNT
    }

    public List<MonedasReinoInfo> obtenerMonedasReinoInfo() {
        return db.obtenerMonedasReinoInfo();
    }


    public boolean incrementarDineroConvertido(String etiquetaReino, double cantidad) {
        return db.aumentarDineroConvertido(etiquetaReino, cantidad);
    }

    public MonedasReinoInfo obtenerInfoMonedaPorNombre(String nombreMoneda) {
        return db.obtenerMonedaPorNombre(nombreMoneda);
    }

    public double getSaldoMonedaJugador(UUID uuid, String reinoEtiqueta) {
        return db.obtenerSaldoMonedaJugador(uuid, reinoEtiqueta);
    }

    public void restarMonedaJugador(UUID uuid, String reinoEtiqueta, double cantidad) {
        db.actualizarSaldoMoneda(uuid, reinoEtiqueta, -cantidad);
    }

    public void sumarMonedaJugador(UUID uuid, String reinoEtiqueta, double cantidad) {
        db.actualizarSaldoMoneda(uuid, reinoEtiqueta, cantidad);
    }

    public double obtenerSaldoMonedaJugador(String uuidJugador, String etiquetaReino) {
        double saldo = 0.0;
        try (Connection connection = db.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT saldo FROM monedas_jugadores WHERE uuid_jugador = ? AND etiqueta_reino = ?")) {
            ps.setString(1, uuidJugador);
            ps.setString(2, etiquetaReino);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                saldo = rs.getDouble("saldo");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return saldo;
    }


    public boolean esMiembroOBancoPropietario(UUID jugador, String etiquetaBanco) {
        return db.esPropietarioBanco(jugador, etiquetaBanco) ||
                db.esMiembroDeBanco(jugador, etiquetaBanco);
    }

    public List<String> obtenerHistorialBanco(String etiquetaBanco, int limite) {
        return db.obtenerHistorialBanco(etiquetaBanco, limite);
    }

    public void registrarMovimiento(String nombreMoneda, String accion, String jugador, double cantidad, String fecha) {
        try (Connection connection = db.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO movimientos_monedas (nombre_moneda, accion, jugador, cantidad, fecha) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, nombreMoneda);
            ps.setString(2, accion);
            ps.setString(3, jugador);
            ps.setDouble(4, cantidad);
            ps.setString(5, fecha);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error al registrar movimiento de moneda: " + e.getMessage());
        }
    }



    public String obtenerReinoDeBanco(String etiquetaBanco) {
        return db.obtenerReinoDeBanco(etiquetaBanco);
    }

    public String obtenerNombreMonedaDeReino(String etiquetaReino) {
        return db.obtenerMonedaDeReino(etiquetaReino);
    }

    public boolean tienePermisoContrato(String banco, String reino, String permiso) {
        return db.tienePermisoContrato(banco, reino, permiso);
    }

    public boolean incrementarCantidadImpresa(String etiquetaReino, double cantidad) {
        return db.aumentarMonedaImpresa(etiquetaReino, cantidad);
    }


    /** Devuelve la etiqueta del reino al que pertenece el jugador. */
    public String obtenerReinoJugador(UUID jugadorUUID) {
        return db.obtenerReinoJugador(jugadorUUID);
    }

    /** Crea un nuevo banco (estado 'pendiente'). */
    public boolean crearBanco(String etiqueta, String nombre, String reinoEtiqueta, UUID propietario) {
        return db.crearBanco(etiqueta, nombre, reinoEtiqueta, propietario);
    }

    public boolean existeBanco(String etiqueta) {
        return db.existeBanco(etiqueta);
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
