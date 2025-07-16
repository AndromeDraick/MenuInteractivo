package AndromeDraick.menuInteractivo.database;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        if (!archivo.exists()) plugin.saveResource("configuracion/config_basededatos.yml", false);

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
                "etiqueta_reino TEXT NOT NULL" +
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

    public void actualizarTrabajo(UUID uuid, String nuevoTrabajo) {
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE jugadores SET trabajo = ? WHERE uuid = ?"
        )) {
            ps.setString(1, nuevoTrabajo);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al actualizar trabajo: " + e.getMessage());
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
