package AndromeDraick.menuInteractivo.database;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.model.Reino;
import AndromeDraick.menuInteractivo.model.Banco;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


import java.sql.*;
import java.util.UUID;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GestorBaseDeDatos {

    private Connection conexion;
    private boolean usarMySQL;
    private final MenuInteractivo plugin;

    public GestorBaseDeDatos(MenuInteractivo plugin) {
        this.plugin = plugin;
        cargarConfiguracionYConectar();
        crearTablasEconomia();
    }

    private void cargarConfiguracionYConectar() {
        File archivo = new File(plugin.getDataFolder(), "configuracion/config_basededatos.yml");
        if (!archivo.exists())
            plugin.saveResource("configuracion/config_basededatos.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(archivo);
        usarMySQL = config.getString("tipo", "sqlite").equalsIgnoreCase("mysql");

        try {
            if (usarMySQL) {
                String host = config.getString("mysql.host");
                int puerto = config.getInt("mysql.puerto");
                String base = config.getString("mysql.base");
                String usuario = config.getString("mysql.usuario");
                String contrasena = config.getString("mysql.contrasena");
                String url = "jdbc:mysql://" + host + ":" + puerto + "/" + base + "?useSSL=false&autoReconnect=true";

                conexion = DriverManager.getConnection(url, usuario, contrasena);
            } else {
                File sqliteFile = new File(plugin.getDataFolder(), "jugadores.db");
                conexion = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getPath());
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al conectar a la base de datos: " + e.getMessage());
        }
    }

    private void crearTablasEconomia() {
        String sqlReinos = "CREATE TABLE IF NOT EXISTS reinos (" +
                "etiqueta TEXT PRIMARY KEY," +
                "nombre TEXT NOT NULL," +
                "uuid_rey TEXT NOT NULL" +
                ")";

        String sqlJugadores = "CREATE TABLE IF NOT EXISTS jugadores (" +
                "uuid TEXT PRIMARY KEY," +
                "nombre TEXT," +
                "trabajo TEXT," +
                "nivel INTEGER DEFAULT 1," +
                "puntos INTEGER DEFAULT 0," +
                "estadisticas TEXT DEFAULT ''" +
                ")";

        String sqlBancos = "CREATE TABLE IF NOT EXISTS bancos (" +
                "etiqueta TEXT PRIMARY KEY," +
                "nombre TEXT NOT NULL," +
                "reino_etiqueta TEXT NOT NULL," +
                "uuid_propietario TEXT NOT NULL," +
                "estado TEXT DEFAULT 'pendiente'," +
                "fondos REAL DEFAULT 0" +
                ")";

        String sqlJugadoresBanco = "CREATE TABLE IF NOT EXISTS jugadores_banco (" +
                "uuid TEXT NOT NULL," +
                "etiqueta_banco TEXT NOT NULL," +
                "PRIMARY KEY(uuid, etiqueta_banco)" +
                ")";

        String sqlJugadoresReino = "CREATE TABLE IF NOT EXISTS jugadores_reino (" +
                "uuid TEXT PRIMARY KEY," +
                "reino TEXT NOT NULL," +
                "etiqueta_reino TEXT NOT NULL," +
                "rol TEXT NOT NULL" +
                ")";

        try (Statement st = conexion.createStatement()) {
            st.executeUpdate(sqlReinos);
            st.executeUpdate(sqlJugadores);
            st.executeUpdate(sqlBancos);
            st.executeUpdate(sqlJugadoresBanco);
            st.executeUpdate(sqlJugadoresReino);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al crear tablas de economía/reinos: " + e.getMessage());
        }
    }

    public void guardarJugador(UUID uuid, String nombre, String trabajo) {
        try (PreparedStatement ps = conexion.prepareStatement(
                "INSERT OR REPLACE INTO jugadores (uuid, nombre, trabajo) VALUES (?, ?, ?)"
        )) {
            ps.setString(1, uuid.toString());
            ps.setString(2, nombre);
            ps.setString(3, trabajo);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al guardar jugador: " + e.getMessage());
        }
    }

    public boolean crearReino(String etiqueta, String nombre, UUID reyUUID) {
        String sql = "INSERT INTO reinos (etiqueta, nombre, uuid_rey) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, etiqueta);
            ps.setString(2, nombre);
            ps.setString(3, reyUUID.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Reino> listarReinos() {
        List<Reino> reinos = new ArrayList<>();
        String sql = "SELECT etiqueta, nombre, uuid_rey FROM reinos";
        try (Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reinos.add(new Reino(
                        rs.getString("etiqueta"),
                        rs.getString("nombre"),
                        UUID.fromString(rs.getString("uuid_rey"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reinos;
    }

    /**
     * Elimina un reino por su etiqueta.
     */
    public boolean eliminarReino(String etiqueta) {
        String sql = "DELETE FROM reinos WHERE etiqueta = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, etiqueta);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Asocia un jugador a un reino.
     */
    public boolean unirJugadorReino(UUID jugadorUUID, String etiquetaReino) {
        String sql = "INSERT INTO jugadores_reino (uuid_jugador, etiqueta_reino) VALUES (?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, etiquetaReino);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina la pertenencia de un jugador a un reino.
     */
    public boolean salirJugadorReino(UUID jugadorUUID, String etiquetaReino) {
        String sql = "DELETE FROM jugadores_reino WHERE uuid_jugador = ? AND etiqueta_reino = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, etiquetaReino);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // MÉTODOS DE GESTIÓN DE BANCOS:

    /**
     * Deposita monto en un banco.
     */
    public boolean depositarEnBanco(String etiquetaBanco, double monto) {
        String sql = "UPDATE bancos SET fondos = fondos + ? WHERE etiqueta = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setDouble(1, monto);
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Devuelve la etiqueta del único banco al que está vinculado un jugador.
     */
    public String obtenerBancoDeJugador(UUID jugadorUUID) {
        String sql = "SELECT etiqueta_banco FROM jugadores_banco WHERE uuid = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("etiqueta_banco");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insertarBanco(String etiqueta, String nombre, String reinoEtiqueta, UUID propietario) {
        String sql = """
            INSERT INTO bancos
              (etiqueta, nombre, reino_etiqueta, uuid_propietario, estado, fondos)
            VALUES
              (?, ?, ?, ?, 'pendiente', 0)
            """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, etiqueta);
            ps.setString(2, nombre);
            ps.setString(3, reinoEtiqueta);
            ps.setString(4, propietario.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cambia el estado de un banco ('aprobado', 'rechazado', etc.).
     */
    public boolean actualizarEstadoBanco(String etiquetaBanco, String nuevoEstado) {
        String sql = "UPDATE bancos SET estado = ? WHERE etiqueta = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Devuelve la información completa de un banco por su etiqueta.
     */
    public Banco obtenerBancoPorEtiqueta(String etiquetaBanco) {
        String sql = """
            SELECT etiqueta, nombre, reino_etiqueta,
                   uuid_propietario, estado, fondos
              FROM bancos
             WHERE etiqueta = ?
            """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, etiquetaBanco);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Banco(
                            rs.getString("etiqueta"),
                            rs.getString("nombre"),
                            rs.getString("reino_etiqueta"),
                            UUID.fromString(rs.getString("uuid_propietario")),
                            rs.getString("estado"),
                            rs.getDouble("fondos")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lista todos los bancos aprobados de un reino.
     */
    public List<Banco> obtenerBancosDeReino(String etiquetaReino) {
        List<Banco> lista = new ArrayList<>();
        String sql = """
            SELECT etiqueta, nombre, reino_etiqueta,
                   uuid_propietario, estado, fondos
              FROM bancos
             WHERE reino_etiqueta = ? AND estado = 'aprobado'
            """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, etiquetaReino);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Banco(
                            rs.getString("etiqueta"),
                            rs.getString("nombre"),
                            rs.getString("reino_etiqueta"),
                            UUID.fromString(rs.getString("uuid_propietario")),
                            rs.getString("estado"),
                            rs.getDouble("fondos")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Lista todas las solicitudes de banco pendientes de un reino.
     */
    public List<Banco> obtenerBancosPendientes(String etiquetaReino) {
        List<Banco> lista = new ArrayList<>();
        String sql = """
            SELECT etiqueta, nombre, reino_etiqueta,
                   uuid_propietario, estado, fondos
              FROM bancos
             WHERE reino_etiqueta = ? AND estado = 'pendiente'
            """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, etiquetaReino);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Banco(
                            rs.getString("etiqueta"),
                            rs.getString("nombre"),
                            rs.getString("reino_etiqueta"),
                            UUID.fromString(rs.getString("uuid_propietario")),
                            rs.getString("estado"),
                            rs.getDouble("fondos")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Retira monto de un banco, comprobando saldo suficiente.
     */
    public boolean retirarDeBanco(String etiquetaBanco, double monto) {
        String sql = "UPDATE bancos SET fondos = fondos - ? WHERE etiqueta = ? AND fondos >= ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setDouble(1, monto);
            ps.setString(2, etiquetaBanco);
            ps.setDouble(3, monto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene el saldo actual de un banco.
     */
    public double obtenerSaldoBanco(String etiquetaBanco) {
        String sql = "SELECT fondos FROM bancos WHERE etiqueta = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, etiquetaBanco);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("fondos");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Agrega un socio a un banco.
     */
    public boolean agregarSocioBanco(String etiquetaBanco, UUID jugadorUUID) {
        String sql = "INSERT INTO jugadores_banco (uuid, etiqueta_banco) VALUES (?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Expulsa a un socio de un banco.
     */
    public boolean quitarSocioBanco(String etiquetaBanco, UUID jugadorUUID) {
        String sql = "DELETE FROM jugadores_banco WHERE uuid = ? AND etiqueta_banco = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String obtenerTrabajo(UUID uuid) {
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT trabajo FROM jugadores WHERE uuid = ?"
        )) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("trabajo");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener trabajo: " + e.getMessage());
        }
        return "Sin trabajo";
    }

    public boolean actualizarTrabajo(UUID jugadorUUID, String trabajo) {
        String sql = "UPDATE jugadores SET trabajo = ? WHERE uuid = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, trabajo);
            ps.setString(2, jugadorUUID.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String obtenerReinoJugador(UUID uuidJugador) {
        try (PreparedStatement stmt = conexion.prepareStatement("SELECT reino FROM jugadores_reino WHERE uuid = ?")) {
            stmt.setString(1, uuidJugador.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("reino");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener el reino del jugador: " + e.getMessage());
        }
        return null;
    }

    public String obtenerRolJugadorEnReino(UUID uuidJugador) {
        try (PreparedStatement stmt = conexion.prepareStatement("SELECT rol FROM jugadores_reino WHERE uuid = ?")) {
            stmt.setString(1, uuidJugador.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("rol");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener el rol del jugador: " + e.getMessage());
        }
        return null;
    }

    public boolean eliminarJugadorDeReino(UUID uuidJugador) {
        try (PreparedStatement stmt = conexion.prepareStatement("DELETE FROM jugadores_reino WHERE uuid = ?")) {
            stmt.setString(1, uuidJugador.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al eliminar jugador de reino: " + e.getMessage());
        }
        return false;
    }

    public boolean agregarJugadorAReino(UUID uuidJugador, String etiquetaReino, String rol) {
        try (PreparedStatement stmt = conexion.prepareStatement("REPLACE INTO jugadores_reino (uuid, reino, rol) VALUES (?, ?, ?)")) {
            stmt.setString(1, uuidJugador.toString());
            stmt.setString(2, etiquetaReino);
            stmt.setString(3, rol);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al agregar jugador a reino: " + e.getMessage());
        }
        return false;
    }

    public boolean transferirLiderazgoReino(String etiquetaReino, UUID nuevoReyUUID) {
        try (PreparedStatement stmt = conexion.prepareStatement("UPDATE reinos SET uuid_rey = ? WHERE etiqueta = ?")) {
            stmt.setString(1, nuevoReyUUID.toString());
            stmt.setString(2, etiquetaReino);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al transferir liderazgo de reino: " + e.getMessage());
        }
        return false;
    }

    public List<UUID> obtenerMiembrosDeReino(String etiquetaReino) {
        List<UUID> miembros = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT uuid FROM jugadores_reino WHERE etiqueta_reino = ?"
        )) {
            ps.setString(1, etiquetaReino);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    miembros.add(UUID.fromString(rs.getString("uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener miembros del reino: " + e.getMessage());
        }
        return miembros;
    }


    public Connection getConexion() {
        return conexion;
    }
}
