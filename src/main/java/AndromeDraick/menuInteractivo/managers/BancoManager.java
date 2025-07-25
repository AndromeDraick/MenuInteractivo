package AndromeDraick.menuInteractivo.managers;

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

    public boolean depositarAMiCuenta(UUID jugadorUUID, String etiquetaBanco, double cantidad) {
        if (cantidad <= 0) return false;

        String reino = obtenerReinoDeBanco(etiquetaBanco);
        if (reino == null) return false;

        String moneda = obtenerNombreMonedaDeReino(reino);
        if (moneda == null) return false;

        OfflinePlayer player = Bukkit.getOfflinePlayer(jugadorUUID);
        if (!player.hasPlayedBefore()) return false;

        if (economia.getBalance(player) < cantidad) return false;

        // Retirar dinero al jugador
        economia.withdrawPlayer(player, cantidad);

        // Agregar a su cuenta bancaria interna
        return db.modificarSaldoJugador(jugadorUUID, reino, cantidad);
    }

    public boolean esPropietarioBanco(UUID jugadorUUID, String etiquetaBanco) {
        return db.esPropietarioBanco(jugadorUUID, etiquetaBanco);
    }


    public boolean transferirEntreJugadores(UUID emisor, UUID receptor, String etiquetaBanco, double monto) {
        String reino = obtenerReinoDeBanco(etiquetaBanco);
        return db.transferirEntreJugadores(emisor, receptor, reino, monto);
    }

    public boolean registrarSolicitudMoneda(UUID jugador, String bancoEtiqueta, double cantidad) {
        String sql = "INSERT INTO solicitudes_monedas (uuid_jugador, etiqueta_banco, cantidad) VALUES (?, ?, ?)";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, jugador.toString());
            stmt.setString(2, bancoEtiqueta);
            stmt.setDouble(3, cantidad);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MI] Error al registrar solicitud de moneda: " + e.getMessage());
            return false;
        }
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
        String select = "SELECT uuid_jugador, etiqueta_banco, cantidad FROM solicitudes_monedas WHERE id = ?";
        String update = "UPDATE solicitudes_monedas SET estado = ? WHERE id = ?";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmtSelect = conn.prepareStatement(select);
             PreparedStatement stmtUpdate = conn.prepareStatement(update)) {

            stmtSelect.setInt(1, idSolicitud);
            ResultSet rs = stmtSelect.executeQuery();
            if (!rs.next()) return false;

            UUID jugador = UUID.fromString(rs.getString("uuid_jugador"));
            String banco = rs.getString("etiqueta_banco");
            double cantidad = rs.getDouble("cantidad");

            String nuevoEstado = aceptar ? "aceptada" : "rechazada";

            stmtUpdate.setString(1, nuevoEstado);
            stmtUpdate.setInt(2, idSolicitud);
            stmtUpdate.executeUpdate();

            if (aceptar) {
                // Verificar si el banco tiene saldo impreso disponible
                double disponible = obtenerCantidadImpresaDisponible(banco);
                if (disponible < cantidad) return false;

                descontarCantidadImpresaDisponible(banco, cantidad);
                depositarAMiCuenta(jugador, banco, cantidad);
            }

            return true;

        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MI] Error al procesar solicitud de moneda: " + e.getMessage());
            return false;
        }
    }

    public double obtenerCantidadImpresaDisponible(String etiquetaBanco) {
        String sql = "SELECT monedas_disponibles FROM bancos WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, etiquetaBanco);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("monedas_disponibles");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MI] Error al obtener monedas disponibles del banco: " + e.getMessage());
        }
        return 0;
    }

    public boolean descontarCantidadImpresaDisponible(String etiquetaBanco, double cantidad) {
        double disponible = obtenerCantidadImpresaDisponible(etiquetaBanco);
        if (disponible < cantidad) return false;

        String sql = "UPDATE bancos SET monedas_disponibles = monedas_disponibles - ? WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, cantidad);
            stmt.setString(2, etiquetaBanco);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MI] Error al descontar monedas del banco: " + e.getMessage());
            return false;
        }
    }

}
