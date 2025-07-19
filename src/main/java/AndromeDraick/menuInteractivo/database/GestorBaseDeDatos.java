package AndromeDraick.menuInteractivo.database;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.model.Banco;
import AndromeDraick.menuInteractivo.model.Reino;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.UUID;

public class GestorBaseDeDatos {
    private Connection conexion;
    private final MenuInteractivo plugin;
    private boolean usarMySQL;

    public GestorBaseDeDatos(MenuInteractivo plugin) {
        this.plugin = plugin;
        cargarConfiguracionYConectar();
        crearTablas();
    }

    /**
     * Carga la configuración (SQLite o MySQL) y establece la conexión.
     */
    private void cargarConfiguracionYConectar() {
        File cfgDir = new File(plugin.getDataFolder(), "configuracion");
        if (!cfgDir.exists()) cfgDir.mkdirs();
        File archivo = new File(cfgDir, "config_basededatos.yml");
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
                File sqliteFile = new File(plugin.getDataFolder(), "base_jugadores.db");
                conexion = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getPath());
            }
            plugin.getLogger().info("Conexión a BBDD establecida (" + (usarMySQL ? "MySQL" : "SQLite") + ").");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al conectar a la base de datos: " + e.getMessage());
        }
    }

    private void crearTablas() {
        try (Statement st = conexion.createStatement()) {
            // Reinos
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reinos (" +
                    "etiqueta TEXT PRIMARY KEY, " +
                    "nombre TEXT NOT NULL, " +
                    "uuid_rey TEXT NOT NULL, " +
                    "descripcion TEXT DEFAULT '', " +
                    "moneda TEXT NOT NULL, " +
                    "fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            // Jugadores de reino
            st.executeUpdate("CREATE TABLE IF NOT EXISTS jugadores_reino (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "etiqueta_reino TEXT NOT NULL, " +
                    "rol TEXT NOT NULL" +
                    ")");
            // Bancos
            st.executeUpdate("CREATE TABLE IF NOT EXISTS bancos (" +
                    "etiqueta TEXT PRIMARY KEY, " +
                    "nombre TEXT NOT NULL, " +
                    "reino_etiqueta TEXT NOT NULL, " +
                    "uuid_propietario TEXT NOT NULL, " +
                    "estado TEXT DEFAULT 'pendiente', " +
                    "fondos REAL DEFAULT 0, " +
                    "tasa_interes REAL DEFAULT 0.01, " +
                    "fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            // Jugadores de banco
            st.executeUpdate("CREATE TABLE IF NOT EXISTS jugadores_banco (" +
                    "uuid TEXT NOT NULL, " +
                    "etiqueta_banco TEXT NOT NULL, " +
                    "PRIMARY KEY(uuid, etiqueta_banco)" +
                    ")");
            // Jugadores generales
            st.executeUpdate("CREATE TABLE IF NOT EXISTS jugadores (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "nombre TEXT, " +
                    "trabajo TEXT, " +
                    "nivel INTEGER DEFAULT 1, " +
                    "puntos INTEGER DEFAULT 0, " +
                    "estadisticas TEXT DEFAULT ''" +
                    ")");
            // Género de jugador
            st.executeUpdate("CREATE TABLE IF NOT EXISTS genero_jugador (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "genero TEXT NOT NULL" +
                    ")");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creando tablas: " + e.getMessage());
        }
    }

    // —— Métodos para Reinos ——
    public boolean crearReino(String etiqueta, String nombre, UUID reyUUID) {
        String sql = "INSERT INTO reinos (etiqueta, nombre, uuid_rey) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, etiqueta);
            ps.setString(2, nombre);
            ps.setString(3, reyUUID.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error creando reino: " + e.getMessage());
            return false;
        }
    }

    public String obtenerReinoJugador(UUID jugadorUUID) {
        String sql = "SELECT etiqueta_reino FROM jugadores_reino WHERE uuid = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("etiqueta_reino");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error obteniendo reino jugador: " + e.getMessage());
        }
        return null;
    }

    public boolean agregarJugadorAReino(UUID jugadorUUID, String etiquetaReino, String rol) {
        String sql = "REPLACE INTO jugadores_reino (uuid, etiqueta_reino, rol) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, etiquetaReino);
            ps.setString(3, rol);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error uniendo jugador a reino: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarJugadorDeReino(UUID jugadorUUID) {
        String sql = "DELETE FROM jugadores_reino WHERE uuid = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error eliminando jugador de reino: " + e.getMessage());
            return false;
        }
    }

    // —— Métodos para Bancos ——
    public boolean crearBanco(String etiqueta, String nombre, String reinoEtiqueta, UUID propietario) {
        String sql = "INSERT INTO bancos (etiqueta, nombre, reino_etiqueta, uuid_propietario) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, etiqueta);
            ps.setString(2, nombre);
            ps.setString(3, reinoEtiqueta);
            ps.setString(4, propietario.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error creando banco: " + e.getMessage());
            return false;
        }
    }

    public boolean setEstadoBanco(String etiquetaBanco, String estado) {
        String sql = "UPDATE bancos SET estado = ? WHERE etiqueta = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error actualizando estado de banco: " + e.getMessage());
            return false;
        }
    }

    public boolean depositarBanco(String etiquetaBanco, double monto) {
        String sql = "UPDATE bancos SET fondos = fondos + ? WHERE etiqueta = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setDouble(1, monto);
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error depositando en banco: " + e.getMessage());
            return false;
        }
    }

    public boolean retirarBanco(String etiquetaBanco, double monto) {
        String sql = "UPDATE bancos SET fondos = fondos - ? WHERE etiqueta = ? AND fondos >= ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setDouble(1, monto);
            ps.setString(2, etiquetaBanco);
            ps.setDouble(3, monto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error retirando de banco: " + e.getMessage());
            return false;
        }
    }

    public double getSaldoBanco(String etiquetaBanco) {
        String sql = "SELECT fondos FROM bancos WHERE etiqueta = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, etiquetaBanco);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("fondos");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error obteniendo saldo de banco: " + e.getMessage());
        }
        return 0;
    }

    public boolean agregarJugadorABanco(UUID jugadorUUID, String etiquetaBanco) {
        String sql = "INSERT INTO jugadores_banco (uuid, etiqueta_banco) VALUES (?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error agregando jugador a banco: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarJugadorDeBanco(UUID jugadorUUID, String etiquetaBanco) {
        String sql = "DELETE FROM jugadores_banco WHERE uuid = ? AND etiqueta_banco = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error eliminando jugador de banco: " + e.getMessage());
            return false;
        }
    }

    public Banco obtenerBancoPorEtiqueta(String etiquetaBanco) {
        String sql = "SELECT * FROM bancos WHERE etiqueta = ?";
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
            plugin.getLogger().warning("Error obteniendo banco: " + e.getMessage());
        }
        return null;
    }

    public List<Banco> obtenerBancosDeReino(String etiquetaReino) {
        List<Banco> lista = new ArrayList<>();
        String sql = "SELECT * FROM bancos WHERE reino_etiqueta = ? AND estado = 'aprobado'";
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
            plugin.getLogger().warning("Error listando bancos: " + e.getMessage());
        }
        return lista;
    }

    public List<Banco> obtenerBancosPendientes(String etiquetaReino) {
        List<Banco> lista = new ArrayList<>();
        String sql = "SELECT * FROM bancos WHERE reino_etiqueta = ? AND estado = 'pendiente'";
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
            plugin.getLogger().warning("Error listando solicitudes de banco: " + e.getMessage());
        }
        return lista;
    }

    public String obtenerBancoDeJugador(UUID jugadorUUID) {
        String sql = "SELECT etiqueta_banco FROM jugadores_banco WHERE uuid = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("etiqueta_banco");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error obteniendo banco del jugador: " + e.getMessage());
        }
        return null;
    }

    // —— Métodos para Género ——
    public boolean setGenero(UUID uuid, String genero) {
        String sql = "REPLACE INTO genero_jugador (uuid, genero) VALUES (?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, genero);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error seteando género: " + e.getMessage());
            return false;
        }
    }

    public String getGenero(UUID uuid) {
        String sql = "SELECT genero FROM genero_jugador WHERE uuid = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("genero");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error obteniendo género: " + e.getMessage());
        }
        return "Masculino";
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
