package AndromeDraick.menuInteractivo.managers;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.database.HikariProvider;
import AndromeDraick.menuInteractivo.model.Banco;
import AndromeDraick.menuInteractivo.model.MonedasReinoInfo;
import AndromeDraick.menuInteractivo.model.SolicitudMoneda;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Encapsula operaciones de banco usando GestorBaseDeDatos.
 */
public class BancoManager {

    private final GestorBaseDeDatos db;
    private final Economy economia;

    public BancoManager(GestorBaseDeDatos db, Economy economia) {
        this.db = db;
        this.economia = economia;
    }

    /**
     * Calcula el valor dinámico de la moneda de un reino.
     * El valor se basa en la fórmula: dineroConvertido / (impreso - quemado)
     * Si no hay datos, retorna 1.0 por defecto.
     */
    public double calcularValorMonedaReino(String etiquetaReino) {
        if (etiquetaReino == null || etiquetaReino.isEmpty()) return 1.0;

        GestorBaseDeDatos db = MenuInteractivo.getInstancia().getBaseDeDatos();
        double totalImpreso = db.obtenerTotalMonedasImpresas(etiquetaReino);
        double totalQuemado = db.obtenerTotalMonedasQuemadas(etiquetaReino);
        double dineroConvertido = db.obtenerTotalDineroConvertido(etiquetaReino);

        double circulante = totalImpreso - totalQuemado;
        if (circulante <= 0) circulante = 1; // Evitar división por 0

        double valor = dineroConvertido / circulante;
        if (valor <= 0) valor = 0.01; // Evitar 0 absoluto

        return valor;
    }

    public boolean crearContrato(String bancoEtiqueta, String reinoEtiqueta, Timestamp fechaInicio, Timestamp fechaFin, String permisos) {
        return db.insertarContratoBancoReino(bancoEtiqueta, reinoEtiqueta, fechaInicio, fechaFin, permisos);
    }

    public boolean incrementarCantidadQuemada(String etiquetaReino, double cantidad) {
        return db.aumentarMonedaQuemada(etiquetaReino, cantidad);
    }

    public boolean renombrarBancoCompleto(String etiquetaActual, String nuevoNombre, String nuevaEtiqueta) {
        return db.renombrarBancoCompleto(etiquetaActual, nuevoNombre, nuevaEtiqueta);
    }

    public List<Banco> obtenerBancosDeJugador(UUID jugador, boolean soloPropietario) {
        return db.obtenerBancosDeJugador(jugador, soloPropietario);
    }

    public String obtenerBancoPropietario(UUID jugadorUUID) {
        return db.obtenerBancoPropietario(jugadorUUID); // ahora usa el nuevo método real
    }


    public boolean bancoEstaAprobado(String etiquetaBanco) {
        String sql = "SELECT estado FROM bancos WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, etiquetaBanco);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("estado").equalsIgnoreCase("aprobado");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error al verificar si el banco está aprobado: " + e.getMessage());
        }
        return false;
    }

    public UUID obtenerUUIDPropietarioBanco(String etiquetaBanco) {
        String sql = "SELECT uuid_propietario FROM bancos WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, etiquetaBanco);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid_propietario"));
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error al obtener UUID del propietario del banco '" + etiquetaBanco + "': " + e.getMessage());
        }
        return null;
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

    public double obtenerSaldoMonedasJugador(String uuidJugador, String etiquetaReino) {
        return db.obtenerSaldoMonedasJugador(uuidJugador, etiquetaReino);
    }


    public boolean esMiembroOBancoPropietario(UUID jugador, String etiquetaBanco) {
        return db.esPropietarioBanco(jugador, etiquetaBanco) ||
                db.esMiembroDeBanco(jugador, etiquetaBanco);
    }

    public List<String> obtenerHistorialBanco(String etiquetaBanco, int limite) {
        return db.obtenerHistorialBanco(etiquetaBanco, limite);
    }

    public void registrarMovimiento(String bancoEtiqueta, String tipo, String uuidJugador, double cantidad) {
        db.registrarMovimiento(bancoEtiqueta, tipo, uuidJugador, cantidad);
    }

    public String obtenerReinoDeBanco(String etiquetaBanco) {
        return db.obtenerReinoDeBanco(etiquetaBanco);
    }

    public String obtenerReinoDeBanco(String etiquetaBanco, Connection conn) throws SQLException {
        return db.obtenerReinoDeBanco(etiquetaBanco, conn);
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

    public boolean esReyDeReino(UUID jugadorUUID, String etiquetaReino) {
        return db.esReyDeReino(jugadorUUID, etiquetaReino);
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

    public List<String> obtenerMonedasJugables() {
        // Devuelve una lista con los nombres de todas las monedas disponibles en el servidor
        return db.obtenerTodasLasMonedas().stream()
                .map(MonedasReinoInfo::getNombreMoneda)
                .collect(Collectors.toList());
    }

    public List<String> obtenerReinosDisponibles() {
        // Devuelve una lista con los nombres/etiquetas de todos los reinos conocidos
        return db.obtenerTodosLosReinos(); // este debe devolver List<String>
    }

    public boolean confirmarContrato(String bancoEtiqueta, String reinoEtiqueta) {
        return db.aceptarContratoBancoReino(bancoEtiqueta, reinoEtiqueta);
    }

    public boolean rechazarContrato(String bancoEtiqueta, String reinoEtiqueta) {
        return db.rechazarContratoBancoReino(bancoEtiqueta, reinoEtiqueta);
    }


    public String obtenerRolJugadorEnReino(UUID uuid) {
        return db.obtenerRolJugadorEnReino(uuid);
    }

    /** Asegura que el jugador tenga cuenta de moneda, y si no la tiene, la crea. */
    public void crearCuentaMonedaSiNoExiste(UUID jugador, String etiquetaReino) {
        db.crearCuentaSiNoExiste(jugador, etiquetaReino);
    }

    /** Modifica el saldo de la cuenta del jugador (positivo o negativo). */
    public void modificarSaldoCuenta(UUID jugador, String etiquetaReino, double cantidad) {
        db.modificarSaldoJugador(jugador, etiquetaReino, cantidad);
    }

    /** Devuelve el saldo actual de la cuenta del jugador. */
    public double obtenerSaldoCuenta(UUID jugador, String etiquetaReino) {
        return db.obtenerSaldoJugador(jugador, etiquetaReino);
    }

    public void depositarAMiCuenta(UUID uuidJugador, String reino, double cantidad, Connection conn) throws SQLException {
        String sql = "INSERT INTO cuentas_monedas (uuid_jugador, etiqueta_reino, saldo) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid_jugador, etiqueta_reino) DO UPDATE SET saldo = saldo + ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuidJugador.toString());
            stmt.setString(2, reino);
            stmt.setDouble(3, cantidad);
            stmt.setDouble(4, cantidad);
            stmt.executeUpdate();
        }
    }

    public boolean esPropietarioBanco(UUID jugadorUUID, String etiquetaBanco) {
        return db.esPropietarioBanco(jugadorUUID, etiquetaBanco);
    }


    public boolean transferirEntreJugadores(UUID emisor, UUID receptor, String etiquetaBanco, double monto) {
        String reino = obtenerReinoDeBanco(etiquetaBanco);
        return db.transferirEntreJugadores(emisor, receptor, reino, monto);
    }

    public boolean registrarSolicitudMoneda(UUID jugadorUUID, String etiquetaBanco, double cantidad, String reinoJugador) {
        return db.registrarSolicitudMoneda(jugadorUUID, etiquetaBanco, cantidad, reinoJugador);
    }

    public List<SolicitudMoneda> obtenerSolicitudesPendientes(String bancoEtiqueta) {
        List<SolicitudMoneda> lista = new ArrayList<>();
        String sql = "SELECT id, uuid_jugador, cantidad, fecha FROM solicitudes_monedas WHERE etiqueta_banco = ? AND estado = 'pendiente'";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bancoEtiqueta);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                UUID uuid = UUID.fromString(rs.getString("uuid_jugador"));
                double cantidad = rs.getDouble("cantidad");
                String fecha = rs.getString("fecha");
                lista.add(new SolicitudMoneda(id, uuid, bancoEtiqueta, cantidad, "pendiente", fecha));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MI] Error al obtener solicitudes pendientes: " + e.getMessage());
        }
        return lista;
    }

    public boolean procesarSolicitud(int idSolicitud, boolean aceptar) {
        String select = "SELECT uuid_jugador, etiqueta_banco, cantidad, estado FROM solicitudes_monedas WHERE id = ?";
        String update = "UPDATE solicitudes_monedas SET estado = ? WHERE id = ?";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmtSelect = conn.prepareStatement(select);
             PreparedStatement stmtUpdate = conn.prepareStatement(update)) {

            conn.setAutoCommit(false); // 🟡 Iniciar transacción

            stmtSelect.setInt(1, idSolicitud);
            ResultSet rs = stmtSelect.executeQuery();
            if (!rs.next()) return false;

            String estadoActual = rs.getString("estado");
            if (!estadoActual.equalsIgnoreCase("pendiente")) return false;

            UUID jugador = UUID.fromString(rs.getString("uuid_jugador"));
            String etiquetaBanco = rs.getString("etiqueta_banco");
            double cantidad = rs.getDouble("cantidad");

            stmtUpdate.setString(1, aceptar ? "aceptada" : "rechazada");
            stmtUpdate.setInt(2, idSolicitud);
            stmtUpdate.executeUpdate();

            if (aceptar) {
                double disponible = obtenerCantidadImpresaDisponible(etiquetaBanco, conn);
                if (disponible < cantidad) {
                    conn.rollback();
                    return false;
                }

                descontarCantidadImpresaDisponible(etiquetaBanco, cantidad, conn);
                String reino = obtenerReinoDeBanco(etiquetaBanco, conn);
                db.descontarCantidadImpresaMoneda(etiquetaBanco, reino, cantidad, conn); // 🟢 nuevo: también actualiza monedas_banco
                depositarAMiCuenta(jugador, reino, cantidad, conn);
            }

            conn.commit(); // ✅ Confirmar cambios
            return true;

        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MI] Error al procesar solicitud de moneda: " + e.getMessage());
            return false;
        }
    }

    public double obtenerCantidadImpresaDisponible(String etiquetaBanco, Connection conn) throws SQLException {
        String sql = "SELECT monedas_disponibles FROM bancos WHERE etiqueta = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, etiquetaBanco);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("monedas_disponibles");
                }
            }
        }
        return 0;
    }

    public void descontarCantidadImpresaDisponible(String etiquetaBanco, double cantidad, Connection conn) throws SQLException {
        String sql = "UPDATE bancos SET monedas_disponibles = monedas_disponibles - ? WHERE etiqueta = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, cantidad);
            stmt.setString(2, etiquetaBanco);
            stmt.executeUpdate();
        }
    }

    public boolean aumentarMonedasDisponiblesBanco(String etiquetaBanco, double cantidad) {
        return db.aumentarMonedasDisponiblesBanco(etiquetaBanco, cantidad);
    }

    public double obtenerCantidadImpresaDisponible(String etiquetaBanco) {
        try (Connection conn = HikariProvider.getConnection()) {
            return obtenerCantidadImpresaDisponible(etiquetaBanco, conn);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MI] Error al obtener cantidad impresa disponible: " + e.getMessage());
            return 0;
        }
    }

    public boolean descontarCantidadImpresaDisponible(String etiquetaBanco, double cantidad) {
        try (Connection conn = HikariProvider.getConnection()) {
            descontarCantidadImpresaDisponible(etiquetaBanco, cantidad, conn);
            return true;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MI] Error al descontar cantidad disponible: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Double> obtenerSaldosDeJugador(UUID uuid) {
        return db.obtenerTodosLosSaldosDeJugador(uuid);
    }

    public Map<String, Double> obtenerMonedasImpresasPorBanco(String etiquetaBanco) {
        return db.obtenerMonedasImpresasPorBanco(etiquetaBanco);
    }

    public boolean bancoTieneMoneda(String etiquetaBanco, String reino) {
        double disponible = db.obtenerCantidadImpresaDisponible(etiquetaBanco, reino);
        return disponible > 0;
    }

    public boolean aumentarMonedaImpresaBanco(String banco, String reino, double cantidad) {
        return db.aumentarMonedaImpresaBanco(banco, reino, cantidad);
    }

    public boolean aumentarMonedaQuemadaBanco(String banco, String reino, double cantidad) {
        return db.aumentarMonedaQuemadaBanco(banco, reino, cantidad);
    }

    public boolean aumentarMonedaConvertidaBanco(String banco, String reino, double cantidad) {
        return db.aumentarMonedaConvertidaBanco(banco, reino, cantidad);
    }

    public double getSaldoCuentaJugador(UUID jugador, String etiquetaBanco) {
        return db.obtenerSaldoCuentaBanco(jugador, etiquetaBanco);
    }

    public void setSaldoCuentaJugador(UUID jugador, String etiquetaBanco, double saldo) {
        db.establecerSaldoCuentaBanco(jugador, etiquetaBanco, saldo);
    }

    public void modificarSaldoCuentaJugador(UUID jugador, String etiquetaBanco, double delta) {
        db.modificarSaldoCuentaBanco(jugador, etiquetaBanco, delta);
    }


}
