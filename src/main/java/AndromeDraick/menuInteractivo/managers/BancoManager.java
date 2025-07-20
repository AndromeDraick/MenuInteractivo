package AndromeDraick.menuInteractivo.managers;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.model.Banco;
import AndromeDraick.menuInteractivo.model.MonedasReinoInfo;

import java.security.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public boolean incrementarDineroConvertido(String etiquetaReino, double cantidad) {
        return db.aumentarDineroConvertido(etiquetaReino, cantidad);
    }

    public List<MonedasReinoInfo> obtenerTodasLasMonedas() {
        return db.obtenerMonedasReinoInfo();
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

    public List<MonedasReinoInfo> obtenerTodasLasMonedas() {
        List<MonedasReinoInfo> lista = new ArrayList<>();

        String sql = "SELECT r.etiqueta, r.moneda, " +
                "COALESCE(SUM(m.impreso), 0) AS total_impreso, " +
                "COALESCE(SUM(m.quemado), 0) AS total_quemado, " +
                "COALESCE(SUM(m.convertido), 0) AS total_convertido, " +
                "MIN(m.fecha) AS fecha_creacion " +
                "FROM reinos r " +
                "LEFT JOIN monedas_banco_movimientos m ON r.etiqueta = m.reino " +
                "GROUP BY r.etiqueta, r.moneda";

        try (Connection conn = db.getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                String etiqueta = rs.getString("etiqueta");
                String moneda = rs.getString("moneda");
                double impreso = rs.getDouble("total_impreso");
                double quemado = rs.getDouble("total_quemado");
                double convertido = rs.getDouble("total_convertido");
                String fecha = rs.getString("fecha_creacion");

                lista.add(new MonedasReinoInfo(etiqueta, moneda, impreso, quemado, convertido, fecha));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }


    public boolean esMiembroOBancoPropietario(UUID jugador, String etiquetaBanco) {
        return db.esPropietarioBanco(jugador, etiquetaBanco) ||
                db.esMiembroDeBanco(jugador, etiquetaBanco);
    }

    public List<String> obtenerHistorialBanco(String etiquetaBanco, int limite) {
        return db.obtenerHistorialBanco(etiquetaBanco, limite);
    }


    public void registrarMovimiento(String banco, String tipo, double cantidad, UUID jugador) {
        db.registrarMovimientoMoneda(banco, tipo, cantidad, jugador);
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
