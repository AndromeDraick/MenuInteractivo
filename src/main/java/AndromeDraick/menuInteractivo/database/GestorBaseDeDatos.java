package AndromeDraick.menuInteractivo.database;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.model.Banco;
import AndromeDraick.menuInteractivo.model.MonedasReinoInfo;
import AndromeDraick.menuInteractivo.model.Reino;
import org.bukkit.Bukkit;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GestorBaseDeDatos {
    private final MenuInteractivo plugin;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public GestorBaseDeDatos(MenuInteractivo plugin) {
        this.plugin = plugin;
        HikariProvider.init(plugin);
        crearTablas();
    }

    /**
     * Carga la configuración (SQLite o MySQL) y establece la conexión.
     */
    private void crearTablas() {
        try (Connection conn = HikariProvider.getConnection();
             Statement st = conn.createStatement()) {
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
                    "titulo TEXT NOT NULL, " +
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
                    "estadisticas TEXT DEFAULT '', " +
                    "fecha_trabajo DATETIME" +
                    ")");

            // Género de jugador
            st.executeUpdate("CREATE TABLE IF NOT EXISTS genero_jugador (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "genero TEXT NOT NULL" +
                    ")");

            // Contratos entre bancos y reinos
            st.executeUpdate("CREATE TABLE IF NOT EXISTS contratos_banco_reino (" +
                    "banco_etiqueta TEXT NOT NULL, " +
                    "reino_etiqueta TEXT NOT NULL, " +
                    "fecha_inicio DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "fecha_fin DATETIME NOT NULL, " +
                    "permisos TEXT NOT NULL, " + // Ejemplo: 'imprimir,quemar'
                    "estado TEXT DEFAULT 'pendiente', " +
                    "PRIMARY KEY(banco_etiqueta, reino_etiqueta) " +
                    ")");

            // Monedas del reino (movimiento económico)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS monedas_reino (" +
                    "reino_etiqueta TEXT PRIMARY KEY, " +
                    "cantidad_impresa REAL DEFAULT 0, " +
                    "cantidad_quemada REAL DEFAULT 0, " +
                    "dinero_convertido REAL DEFAULT 0, " +
                    "moneda TEXT NOT NULL, " +
                    "fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            // Historial de movimientos de moneda por banco
            st.executeUpdate("CREATE TABLE IF NOT EXISTS historial_monedas_banco (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "banco_etiqueta TEXT NOT NULL, " +
                    "tipo TEXT NOT NULL, " + // "imprimir", "quemar", "convertir"
                    "cantidad REAL NOT NULL, " +
                    "uuid_jugador TEXT NOT NULL, " +
                    "fecha DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            // Saldos de monedas de reinos por jugador
            st.executeUpdate("CREATE TABLE IF NOT EXISTS monederos_jugador (" +
                    "uuid TEXT NOT NULL, " +
                    "reino_etiqueta TEXT NOT NULL, " +
                    "cantidad REAL DEFAULT 0, " +
                    "PRIMARY KEY(uuid, reino_etiqueta)" +
                    ")");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creando tablas: " + e.getMessage());
        }
    }

    // —— Métodos para Bancos ——
    public boolean crearBanco(String etiqueta, String nombre, String reinoEtiqueta, UUID propietario) {
        String sql = "INSERT INTO bancos (etiqueta, nombre, reino_etiqueta, uuid_propietario) VALUES (?, ?, ?, ?)";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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

    public boolean existeBanco(String etiqueta) {
        String sql = "SELECT 1 FROM bancos WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, etiqueta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error verificando banco: " + e.getMessage());
            return false;
        }
    }

    public boolean setEstadoBanco(String etiquetaBanco, String estado) {
        String sql = "UPDATE bancos SET estado = ? WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error actualizando estado de banco: " + e.getMessage());
            return false;
        }
    }

    public double getSaldoBanco(String etiquetaBanco) {
        String sql = "SELECT fondos FROM bancos WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, etiquetaBanco);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("fondos");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error obteniendo saldo de banco: " + e.getMessage());
        }
        return 0;
    }

    public boolean depositarBanco(String etiquetaBanco, double monto) {
        String sql = "UPDATE bancos SET fondos = fondos + ? WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, monto);
            ps.setString(2, etiquetaBanco);
            ps.setDouble(3, monto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error retirando de banco: " + e.getMessage());
            return false;
        }
    }

    public boolean agregarJugadorABanco(UUID jugadorUUID, String etiquetaBanco) {
        String sql = "INSERT OR IGNORE INTO jugadores_banco (uuid, etiqueta_banco) VALUES (?, ?)";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error agregando jugador a banco: " + e.getMessage());
            return false;
        }
    }

    public boolean esMiembroDeBanco(UUID jugador, String bancoEtiqueta) {
        String sql = "SELECT 1 FROM jugadores_banco WHERE uuid = ? AND etiqueta_banco = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, jugador.toString());
            stmt.setString(2, bancoEtiqueta);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al verificar miembro del banco: " + e.getMessage());
            return false;
        }
    }

    public boolean esPropietarioBanco(UUID jugador, String bancoEtiqueta) {
        String sql = "SELECT 1 FROM bancos WHERE etiqueta = ? AND uuid_propietario = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bancoEtiqueta);
            stmt.setString(2, jugador.toString());
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al verificar propietario del banco: " + e.getMessage());
            return false;
        }
    }

    public List<String> obtenerHistorialBanco(String etiquetaBanco, int limite) {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT tipo, cantidad, uuid_jugador, fecha " +
                "FROM historial_monedas_banco WHERE banco_etiqueta = ? " +
                "ORDER BY fecha DESC LIMIT ?";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, etiquetaBanco);
            stmt.setInt(2, limite);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String tipo = rs.getString("tipo");
                double cantidad = rs.getDouble("cantidad");
                String uuid = rs.getString("uuid_jugador");
                String fecha = rs.getString("fecha");

                String jugador = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                lista.add(jugador + " -> " + tipo + " $" + cantidad + " (" + fecha + ")");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener historial del banco: " + e.getMessage());
        }

        return lista;
    }

    public boolean eliminarJugadorDeBanco(UUID jugadorUUID, String etiquetaBanco) {
        String sql = "DELETE FROM jugadores_banco WHERE uuid = ? AND etiqueta_banco = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
    // Género
    public boolean setGenero(UUID uuid, String genero) {
        String sql = "REPLACE INTO genero_jugador (uuid, genero) VALUES (?, ?)";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("genero");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error obteniendo género: " + e.getMessage());
        }
        return "Masculino";
    }


    public String getTituloJugador(UUID jugadorUUID) {
        String sql = "SELECT titulo FROM jugadores_reino WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("titulo");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener título del jugador: " + e.getMessage());
        }
        return "plebeyo";
    }

    public List<Reino> listarReinos() {
        List<Reino> reinos = new ArrayList<>();
        String sql = """
        SELECT 
          etiqueta, 
          nombre, 
          descripcion, 
          moneda, 
          uuid_rey, 
          fecha_creacion 
        FROM reinos
        """;

        try (Connection conn = HikariProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String etiqueta    = rs.getString("etiqueta");
                String nombre      = rs.getString("nombre");
                String descripcion = rs.getString("descripcion");
                String moneda      = rs.getString("moneda");
                UUID reyUUID       = UUID.fromString(rs.getString("uuid_rey"));

                java.sql.Timestamp ts = rs.getTimestamp("fecha_creacion");
                java.time.LocalDateTime fecha =
                        ts != null
                                ? ts.toLocalDateTime()
                                : java.time.LocalDateTime.now();

                reinos.add(new Reino(
                        etiqueta,
                        nombre,
                        descripcion != null ? descripcion : "",
                        moneda,
                        reyUUID,
                        fecha
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reinos;
    }

    /**
     * Elimina la pertenencia de un jugador a un reino.
     */
    public boolean salirJugadorReino(UUID jugadorUUID, String etiquetaReino) {
        String sql = "DELETE FROM jugadores_reino WHERE uuid = ? AND etiqueta_reino = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, etiquetaBanco);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertarContratoBancoReino(String banco, String reino, Timestamp inicio, Timestamp fin, String permisos) {
        String sql = "INSERT OR REPLACE INTO contratos_banco_reino " +
                "(banco_etiqueta, reino_etiqueta, fecha_inicio, fecha_fin, permisos) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, banco);
            stmt.setString(2, reino);
            stmt.setTimestamp(3, inicio);
            stmt.setTimestamp(4, fin);
            stmt.setString(5, permisos.toLowerCase());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al insertar contrato banco-reino: " + e.getMessage());
            return false;
        }
    }

    public String obtenerReinoDeBanco(String etiquetaBanco) {
        String sql = "SELECT reino_etiqueta FROM bancos WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, etiquetaBanco);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("reino_etiqueta");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener reino del banco: " + e.getMessage());
        }
        return null;
    }

    public String obtenerMonedaDeReino(String etiquetaReino) {
        // 1. Primero buscar en la subbase de datos económica (monedas_reino)
        String sqlMonedasReino = "SELECT moneda FROM monedas_reino WHERE reino_etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection(); // subbase de economía
             PreparedStatement stmt = conn.prepareStatement(sqlMonedasReino)) {

            stmt.setString(1, etiquetaReino);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("moneda");

        } catch (SQLException e) {
            plugin.getLogger().warning("No se encontró en monedas_reino: " + e.getMessage());
        }

        // 2. Si no se encontró, intentar en la base principal (reinos)
        String sqlReinos = "SELECT moneda FROM reinos WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection(); // conexión principal
             PreparedStatement stmt = conn.prepareStatement(sqlReinos)) {

            stmt.setString(1, etiquetaReino);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("moneda");

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener moneda desde reinos: " + e.getMessage());
        }

        return null;
    }


    public boolean tienePermisoContrato(String banco, String reino, String permisoBuscado) {
        String sql = "SELECT permisos, fecha_fin FROM contratos_banco_reino " +
                "WHERE banco_etiqueta = ? AND reino_etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, banco);
            stmt.setString(2, reino);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp fin = rs.getTimestamp("fecha_fin");
                if (fin.before(new Timestamp(System.currentTimeMillis()))) return false;
                String permisos = rs.getString("permisos");
                return Arrays.stream(permisos.split(","))
                        .map(String::trim)
                        .anyMatch(p -> p.equalsIgnoreCase(permisoBuscado));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al verificar permisos de contrato: " + e.getMessage());
        }
        return false;
    }

    public boolean aumentarMonedaImpresa(String etiquetaReino, double cantidad) {
        String selectSql = "SELECT cantidad_impresa FROM monedas_reino WHERE reino_etiqueta = ?";
        String updateSql = "UPDATE monedas_reino SET cantidad_impresa = cantidad_impresa + ? WHERE reino_etiqueta = ?";
        String insertSql = "INSERT INTO monedas_reino (reino_etiqueta, cantidad_impresa) VALUES (?, ?)";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setString(1, etiquetaReino);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setDouble(1, cantidad);
                    update.setString(2, etiquetaReino);
                    update.executeUpdate();
                    return true;
                }
            } else {
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    insert.setString(1, etiquetaReino);
                    insert.setDouble(2, cantidad);
                    insert.executeUpdate();
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al aumentar moneda impresa: " + e.getMessage());
            return false;
        }
    }

    public boolean aumentarMonedaQuemada(String etiquetaReino, double cantidad) {
        String selectSql = "SELECT cantidad_quemada FROM monedas_reino WHERE reino_etiqueta = ?";
        String updateSql = "UPDATE monedas_reino SET cantidad_quemada = cantidad_quemada + ? WHERE reino_etiqueta = ?";
        String insertSql = "INSERT INTO monedas_reino (reino_etiqueta, cantidad_quemada) VALUES (?, ?)";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setString(1, etiquetaReino);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setDouble(1, cantidad);
                    update.setString(2, etiquetaReino);
                    update.executeUpdate();
                    return true;
                }
            } else {
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    insert.setString(1, etiquetaReino);
                    insert.setDouble(2, cantidad);
                    insert.executeUpdate();
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al aumentar moneda quemada: " + e.getMessage());
            return false;
        }
    }

    public boolean aumentarDineroConvertido(String etiquetaReino, double cantidad) {
        String selectSql = "SELECT dinero_convertido FROM monedas_reino WHERE reino_etiqueta = ?";
        String updateSql = "UPDATE monedas_reino SET dinero_convertido = dinero_convertido + ? WHERE reino_etiqueta = ?";
        String insertSql = "INSERT INTO monedas_reino (reino_etiqueta, dinero_convertido) VALUES (?, ?)";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setString(1, etiquetaReino);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setDouble(1, cantidad);
                    update.setString(2, etiquetaReino);
                    update.executeUpdate();
                    return true;
                }
            } else {
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    insert.setString(1, etiquetaReino);
                    insert.setDouble(2, cantidad);
                    insert.executeUpdate();
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al aumentar dinero convertido: " + e.getMessage());
            return false;
        }
    }

    public List<MonedasReinoInfo> obtenerMonedasReinoInfo() {
        List<MonedasReinoInfo> lista = new ArrayList<>();

        String sql = "SELECT m.reino_etiqueta, r.moneda AS nombre_moneda, " +
                "m.cantidad_impresa, m.cantidad_quemada, m.dinero_convertido, m.fecha_creacion " +
                "FROM monedas_reino m " +
                "JOIN reinos r ON m.reino_etiqueta = r.etiqueta";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String etiqueta = rs.getString("reino_etiqueta");
                String nombre = rs.getString("nombre_moneda");
                double impresa = rs.getDouble("cantidad_impresa");
                double quemada = rs.getDouble("cantidad_quemada");
                double convertida = rs.getDouble("dinero_convertido");
                String fecha = rs.getString("fecha_creacion");

                lista.add(new MonedasReinoInfo(etiqueta, nombre, impresa, quemada, convertida, fecha));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener información de monedas: " + e.getMessage());
        }

        return lista;
    }

    public double obtenerSaldoMonedasJugador(String uuidJugador, String etiquetaReino) {
        String sql = "SELECT cantidad FROM monederos_jugador WHERE uuid = ? AND reino_etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuidJugador);
            ps.setString(2, etiquetaReino);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("cantidad");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error al obtener saldo: " + e.getMessage());
        }
        return 0.0;
    }

    public void registrarMovimiento(String bancoEtiqueta, String tipo, String uuidJugador, double cantidad) {
        String sql = "INSERT INTO historial_monedas_banco (banco_etiqueta, tipo, cantidad, uuid_jugador) VALUES (?, ?, ?, ?)";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bancoEtiqueta);
            ps.setString(2, tipo); // "imprimir", "quemar", "convertir"
            ps.setDouble(3, cantidad);
            ps.setString(4, uuidJugador);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error al registrar movimiento: " + e.getMessage());
        }
    }



    public MonedasReinoInfo obtenerMonedaPorNombre(String nombreMoneda) {
        String sql = "SELECT * FROM monedas_reino WHERE moneda = ?";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreMoneda);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new MonedasReinoInfo(
                        rs.getString("reino_etiqueta"),
                        rs.getString("moneda"),
                        rs.getDouble("cantidad_impresa"),
                        rs.getDouble("cantidad_quemada"),
                        rs.getDouble("dinero_convertido"),
                        rs.getString("fecha_creacion")
                );
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al obtener moneda por nombre: " + e.getMessage());
        }

        return null;
    }

    public double obtenerSaldoMonedaJugador(UUID uuid, String reinoEtiqueta) {
        String sql = "SELECT cantidad FROM monederos_jugador WHERE uuid = ? AND reino_etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, reinoEtiqueta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("cantidad");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al obtener saldo de moneda: " + e.getMessage());
        }
        return 0;
    }

    public void actualizarSaldoMoneda(UUID uuid, String reinoEtiqueta, double diferencia) {
        String select = "SELECT cantidad FROM monederos_jugador WHERE uuid = ? AND reino_etiqueta = ?";
        String update = "UPDATE monederos_jugador SET cantidad = ? WHERE uuid = ? AND reino_etiqueta = ?";
        String insert = "INSERT INTO monederos_jugador (uuid, reino_etiqueta, cantidad) VALUES (?, ?, ?)";

        try (Connection conn = HikariProvider.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psSelect = conn.prepareStatement(select)) {
                psSelect.setString(1, uuid.toString());
                psSelect.setString(2, reinoEtiqueta);
                ResultSet rs = psSelect.executeQuery();

                if (rs.next()) {
                    double actual = rs.getDouble("cantidad");
                    double nuevo = actual + diferencia;
                    try (PreparedStatement psUpdate = conn.prepareStatement(update)) {
                        psUpdate.setDouble(1, nuevo);
                        psUpdate.setString(2, uuid.toString());
                        psUpdate.setString(3, reinoEtiqueta);
                        psUpdate.executeUpdate();
                    }
                } else {
                    try (PreparedStatement psInsert = conn.prepareStatement(insert)) {
                        psInsert.setString(1, uuid.toString());
                        psInsert.setString(2, reinoEtiqueta);
                        psInsert.setDouble(3, diferencia);
                        psInsert.executeUpdate();
                    }
                }
            }

            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al actualizar saldo de moneda: " + e.getMessage());
        }
    }

    public List<MonedasReinoInfo> obtenerTodasLasMonedas() {
        List<MonedasReinoInfo> lista = new ArrayList<>();
        String sql = "SELECT r.etiqueta AS etiqueta_reino, r.moneda AS nombre_moneda, " +
                "m.cantidad_impresa, m.cantidad_quemada, m.dinero_convertido, m.fecha_creacion " +
                "FROM monedas_reino m " +
                "JOIN reinos r ON m.reino_etiqueta = r.etiqueta " +
                "ORDER BY (CASE WHEN (m.cantidad_impresa - m.cantidad_quemada) > 0 " +
                "THEN m.dinero_convertido / (m.cantidad_impresa - m.cantidad_quemada) ELSE 0 END) DESC, " +
                "m.fecha_creacion DESC";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                MonedasReinoInfo moneda = new MonedasReinoInfo(
                        rs.getString("etiqueta_reino"),
                        rs.getString("nombre_moneda"),
                        rs.getDouble("cantidad_impresa"),
                        rs.getDouble("cantidad_quemada"),
                        rs.getDouble("dinero_convertido"),
                        rs.getString("fecha_creacion")
                );
                lista.add(moneda);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    public boolean aceptarContratoBancoReino(String banco, String reino) {
        String sql = "UPDATE contratos_banco_reino SET estado = 'aceptado', fecha_inicio = CURRENT_TIMESTAMP WHERE banco_etiqueta = ? AND reino_etiqueta = ? AND estado = 'pendiente'";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, banco);
            ps.setString(2, reino);
            int filasActualizadas = ps.executeUpdate();
            return filasActualizadas > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al aceptar contrato banco-reino: " + e.getMessage());
            return false;
        }
    }

    public boolean rechazarContratoBancoReino(String banco, String reino) {
        String sql = "UPDATE contratos_banco_reino SET estado = 'rechazado' WHERE banco_etiqueta = ? AND reino_etiqueta = ? AND estado = 'pendiente'";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, banco);
            ps.setString(2, reino);
            int filasActualizadas = ps.executeUpdate();
            return filasActualizadas > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al rechazar contrato banco-reino: " + e.getMessage());
            return false;
        }
    }

//    public boolean eliminarContratoBancoReino(String banco, String reino) {
//        String sql = "DELETE FROM contratos_banco_reino WHERE banco_etiqueta = ? AND reino_etiqueta = ?";
//        try (Connection conn = HikariProvider.getConnection();
//             PreparedStatement ps = conn.prepareStatement(sql)) {
//            ps.setString(1, banco);
//            ps.setString(2, reino);
//            return ps.executeUpdate() > 0;
//        } catch (SQLException e) {
//            plugin.getLogger().severe("Error al eliminar contrato: " + e.getMessage());
//            return false;
//        }
//    }

//-------------------------------------------------------------------------------------------------------------------------------

    // ------------------------------------------Metodos de Reino-------------------------------------------------------------

    public boolean crearReino(String etiqueta, String nombre, String descripcion, String moneda, UUID reyUUID) {
        etiqueta = etiqueta.toLowerCase();

        String sqlReino = "INSERT INTO reinos (etiqueta, nombre, descripcion, uuid_rey, moneda) VALUES (?, ?, ?, ?, ?)";
        String sqlMoneda = "INSERT INTO monedas_reino (reino_etiqueta, moneda) VALUES (?, ?)";

        try (
                Connection conn = HikariProvider.getConnection();
                PreparedStatement psReino = conn.prepareStatement(sqlReino);
                PreparedStatement psMoneda = conn.prepareStatement(sqlMoneda)
        ) {
            psReino.setString(1, etiqueta);
            psReino.setString(2, nombre);
            psReino.setString(3, descripcion != null ? descripcion : "");
            psReino.setString(4, reyUUID.toString());
            psReino.setString(5, moneda);
            int filasInsertadas = psReino.executeUpdate();

            if (filasInsertadas > 0) {
                psMoneda.setString(1, etiqueta);
                psMoneda.setString(2, moneda);
                psMoneda.executeUpdate();
                return true;
            }

            return false;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error creando reino o moneda: " + e.getMessage());
            return false;
        }
    }

    public boolean esReyDeReino(UUID jugadorUUID, String etiquetaReino) {
        etiquetaReino = etiquetaReino.toLowerCase();

        String sql = "SELECT uuid_rey FROM reinos WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, etiquetaReino);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return jugadorUUID.toString().equals(rs.getString("uuid_rey"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error comprobando si es rey: " + e.getMessage());
        }
        return false;
    }

    public String obtenerRolJugadorEnReino(UUID uuidJugador) {
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT rol FROM jugadores_reino WHERE uuid = ?")) {
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
        etiquetaReino = etiquetaReino.toLowerCase();

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE reinos SET uuid_rey = ? WHERE etiqueta = ?")) {
            stmt.setString(1, nuevoReyUUID.toString());
            stmt.setString(2, etiquetaReino);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al transferir liderazgo de reino: " + e.getMessage());
        }
        return false;
    }

    public List<UUID> obtenerMiembrosDeReino(String etiquetaReino) {
        etiquetaReino = etiquetaReino.toLowerCase();

        List<UUID> miembros = new ArrayList<>();
        String sqlMiembros = "SELECT uuid FROM jugadores_reino WHERE etiqueta_reino = ?";
        String sqlRey      = "SELECT uuid_rey FROM reinos WHERE etiqueta = ?";

        try (Connection conn = HikariProvider.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlMiembros)) {
                ps.setString(1, etiquetaReino);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        miembros.add(UUID.fromString(rs.getString("uuid")));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlRey)) {
                ps.setString(1, etiquetaReino);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        UUID uuidRey = UUID.fromString(rs.getString("uuid_rey"));
                        if (!miembros.contains(uuidRey)) {
                            miembros.add(uuidRey);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener miembros del reino: " + e.getMessage());
        }

        System.out.println("Miembros del reino " + etiquetaReino + ": " + miembros);

        return miembros;
    }

    public List<String> obtenerTodosLosReinos() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT etiqueta FROM reinos";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("etiqueta"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    public String obtenerReino(String etiquetaReino) {
        etiquetaReino = etiquetaReino.toLowerCase();

        String sql = "SELECT etiqueta FROM reinos WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, etiquetaReino);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("etiqueta");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error al verificar si el reino existe: " + e.getMessage());
        }
        return null;
    }

    public boolean eliminarReino(String etiqueta) {
        etiqueta = etiqueta.toLowerCase();

        String sql = "DELETE FROM reinos WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, etiqueta);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean agregarJugadorAReino(UUID jugadorUUID, String etiquetaReino, String rol, String titulo) {
        etiquetaReino = etiquetaReino.toLowerCase();

        String sql = "INSERT INTO jugadores_reino (uuid, etiqueta_reino, rol, titulo) VALUES (?, ?, ?, ?)";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, etiquetaReino);
            ps.setString(3, rol);
            ps.setString(4, titulo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error uniendo jugador al reino: " + e.getMessage());
            return false;
        }
    }

    public String obtenerReinoJugador(UUID jugadorUUID) {
        String sql = "SELECT etiqueta_reino FROM jugadores_reino WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("etiqueta_reino");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error obteniendo reino jugador: " + e.getMessage());
        }
        return null;
    }

    public boolean eliminarJugadorDeReino(UUID jugadorUUID) {
        String sql = "DELETE FROM jugadores_reino WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error eliminando jugador de reino: " + e.getMessage());
            return false;
        }
    }

    //--------------------------------------------------------------------------------------------------------

    //--------------------------------------------Metodos de Trabajo-----------------------------------------

    /** Obtiene la fecha de último trabajo del jugador */
    public LocalDateTime obtenerFechaTrabajo(UUID uuid) {
        String sql = "SELECT fecha_trabajo FROM jugadores WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String fechaStr = rs.getString("fecha_trabajo");
                    if (fechaStr != null) {
                        return LocalDateTime.parse(fechaStr, FORMATO_FECHA);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener fecha_trabajo de jugador: " + e.getMessage());
        }
        return null;
    }

    /** Actualiza la fecha de asignación de trabajo de un jugador */
    public boolean actualizarFechaTrabajo(UUID uuid, LocalDateTime fecha) {
        String sql = "UPDATE jugadores SET fecha_trabajo = ? WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fecha.format(FORMATO_FECHA));
            ps.setString(2, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al actualizar fecha_trabajo: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarTrabajo(UUID jugadorUUID, String trabajo) {
        String sql;

        if (HikariProvider.esMySQL()) {
            sql = "INSERT INTO jugadores (uuid, trabajo) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE trabajo = VALUES(trabajo)";
        } else {
            sql = "INSERT INTO jugadores (uuid, trabajo) VALUES (?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET trabajo = excluded.trabajo";
        }

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugadorUUID.toString());
            ps.setString(2, trabajo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error actualizando trabajo: " + e.getMessage());
            return false;
        }
    }

    public String obtenerTrabajoJugador(UUID uuid) {
        String sql = "SELECT trabajo FROM jugadores WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String trabajo = rs.getString("trabajo");
                    return (trabajo != null && !trabajo.isEmpty()) ? trabajo : "Sin trabajo";
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error obteniendo trabajo del jugador: " + e.getMessage());
        }
        return "Sin trabajo";
    }


}
