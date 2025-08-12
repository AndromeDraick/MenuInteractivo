package AndromeDraick.menuInteractivo.database;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.model.Banco;
import AndromeDraick.menuInteractivo.model.ItemEnVenta;
import AndromeDraick.menuInteractivo.model.MonedasReinoInfo;
import AndromeDraick.menuInteractivo.model.Reino;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
                    "fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "monedas_disponibles REAL DEFAULT 0 " +
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
                    "genero TEXT NOT NULL, " +
                    "nombre_rol TEXT NOT NULL, " +
                    "apellido_paterno_rol TEXT NOT NULL, " +
                    "apellido_materno_rol TEXT NOT NULL, " +
                    "descendiente_de TEXT NOT NULL, " +
                    "raza_rol TEXT NOT NULL, " +
                    "historia_familiar TEXT DEFAULT ''" +
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
            // Tabla de cuentas individuales de jugadores por reino
            st.executeUpdate("CREATE TABLE IF NOT EXISTS cuentas_monedas (" +
                    "uuid_jugador TEXT NOT NULL," +
                    "etiqueta_reino TEXT NOT NULL," +
                    "saldo REAL DEFAULT 0," +
                    "PRIMARY KEY(uuid_jugador, etiqueta_reino)" +
                    ")");
            // Solicitudes de adquisición de moneda
            st.executeUpdate("CREATE TABLE IF NOT EXISTS solicitudes_monedas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid_jugador TEXT NOT NULL," +
                    "etiqueta_banco TEXT NOT NULL," +
                    "cantidad REAL NOT NULL," +
                    "estado TEXT DEFAULT 'pendiente'," +
                    "fecha DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "reino_jugador TEXT DEFAULT ''" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS monedas_banco (" +
                    "etiqueta_banco TEXT NOT NULL, " +
                    "reino_etiqueta TEXT NOT NULL, " +
                    "cantidad_impresa REAL DEFAULT 0, " +
                    "cantidad_quemada REAL DEFAULT 0, " +
                    "cantidad_convertida REAL DEFAULT 0, " +
                    "PRIMARY KEY(etiqueta_banco, reino_etiqueta)" +
                    ")");
            // Cuentas individuales de jugadores por banco
            st.executeUpdate("CREATE TABLE IF NOT EXISTS cuentas_moneda (" +
                    "uuid_jugador TEXT NOT NULL, " +
                    "etiqueta_banco TEXT NOT NULL, " +
                    "saldo REAL DEFAULT 0, " +
                    "PRIMARY KEY(uuid_jugador, etiqueta_banco)" +
                    ")");
            // Ítems puestos a la venta por jugadores del reino
            st.executeUpdate("CREATE TABLE IF NOT EXISTS mercado_reino (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid_vendedor TEXT NOT NULL, " +
                    "etiqueta_reino TEXT NOT NULL, " +
                    "item_serializado TEXT NOT NULL, " +
                    "cantidad INTEGER NOT NULL, " +
                    "precio REAL NOT NULL, " +
                    "fecha_publicacion DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS recompensas_diarias (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "ultimo_reclamo BIGINT NOT NULL" +
                    ")");


        } catch (SQLException e) {
            plugin.getLogger().severe("Error creando tablas: " + e.getMessage());
        }
    }

    public long obtenerUltimoReclamo(UUID uuid) {
        String sql = "SELECT ultimo_reclamo FROM recompensas_diarias WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("ultimo_reclamo");
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al obtener último reclamo: " + e.getMessage());
        }
        return 0L; // Nunca ha reclamado
    }

    public void actualizarUltimoReclamo(UUID uuid, long tiempo) {
        // Para SQLite usamos INSERT OR REPLACE
        String sql = "INSERT INTO recompensas_diarias(uuid, ultimo_reclamo) VALUES(?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET ultimo_reclamo = excluded.ultimo_reclamo";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, tiempo);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al actualizar último reclamo: " + e.getMessage());
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

    public boolean renombrarBancoCompleto(String etiquetaActual, String nuevoNombre, String nuevaEtiqueta) {
        // Tablas relacionadas que contienen la etiqueta del banco
        String[] tablasConEtiqueta = {
                "bancos",
                "jugadores_banco",
                "monedas_banco",
                "cuentas_moneda",
                "historial_monedas_banco",
                "solicitudes_monedas",
                "contratos_banco_reino" // Incluimos la tabla de contratos
        };

        Connection conn = null;
        PreparedStatement psBanco = null;
        try {
            conn = HikariProvider.getConnection();
            conn.setAutoCommit(false); // Iniciamos transacción

            // 1. Actualizar la tabla principal de bancos
            String sqlBanco = "UPDATE bancos SET etiqueta = ?, nombre = ? WHERE etiqueta = ?";
            psBanco = conn.prepareStatement(sqlBanco);
            psBanco.setString(1, nuevaEtiqueta);
            psBanco.setString(2, nuevoNombre);
            psBanco.setString(3, etiquetaActual);
            int filas = psBanco.executeUpdate();

            if (filas == 0) {
                conn.rollback();
                return false;
            }

            // 2. Actualizar todas las tablas relacionadas que usan la etiqueta del banco
            for (String tabla : tablasConEtiqueta) {
                if (tabla.equals("bancos")) continue; // Ya actualizada

                String columna;
                switch (tabla) {
                    case "historial_monedas_banco" -> columna = "banco_etiqueta";
                    case "contratos_banco_reino" -> columna = "banco_etiqueta";
                    default -> columna = "etiqueta_banco";
                }

                String sqlUpdate = "UPDATE " + tabla + " SET " + columna + " = ? WHERE " + columna + " = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                    ps.setString(1, nuevaEtiqueta);
                    ps.setString(2, etiquetaActual);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            plugin.getLogger().warning("Error renombrando banco completo: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().warning("Error en rollback: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                if (psBanco != null) psBanco.close();
                if (conn != null) conn.setAutoCommit(true);
                if (conn != null) conn.close();
            } catch (SQLException ignored) {}
        }
    }

    public List<Banco> obtenerBancosDeJugador(UUID jugador, boolean soloPropietario) {
        List<Banco> bancos = new ArrayList<>();

        String sql;
        if (soloPropietario) {
            sql = "SELECT * FROM bancos WHERE uuid_propietario = ?";
        } else {
            sql = """
            SELECT DISTINCT b.* 
            FROM bancos b
            LEFT JOIN jugadores_banco jb ON b.etiqueta = jb.etiqueta_banco
            WHERE b.uuid_propietario = ? OR jb.uuid = ?
        """;
        }

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, jugador.toString());
            if (!soloPropietario) ps.setString(2, jugador.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Banco banco = new Banco(
                            rs.getString("etiqueta"),
                            rs.getString("nombre"),
                            rs.getString("reino_etiqueta"),
                            UUID.fromString(rs.getString("uuid_propietario")),
                            rs.getString("estado"),
                            rs.getDouble("fondos")
                    );
                    bancos.add(banco);
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener bancos de jugador: " + e.getMessage());
        }

        return bancos;
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

    public Map<String, String> obtenerDatosGeneroJugador(UUID uuid) {
        Map<String, String> datos = new HashMap<>();
        String sql = "SELECT nombre_rol, apellido_paterno_rol, apellido_materno_rol, genero, raza_rol " +
                "FROM genero_jugador WHERE uuid = ? LIMIT 1";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    datos.put("nombre_rol", rs.getString("nombre_rol"));
                    datos.put("apellido_paterno_rol", rs.getString("apellido_paterno_rol"));
                    datos.put("apellido_materno_rol", rs.getString("apellido_materno_rol"));
                    datos.put("genero", rs.getString("genero"));
                    datos.put("raza_rol", rs.getString("raza_rol"));
                } else {
                    // Si no existe el jugador, rellenamos con valores por defecto
                    datos.put("nombre_rol", "Desconocido");
                    datos.put("apellido_paterno_rol", "");
                    datos.put("apellido_materno_rol", "");
                    datos.put("genero", "Desconocido");
                    datos.put("raza_rol", "Desconocida");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Si ocurre un error, devolvemos datos por defecto
            datos.put("nombre_rol", "Error");
            datos.put("apellido_paterno_rol", "");
            datos.put("apellido_materno_rol", "");
            datos.put("genero", "Error");
            datos.put("raza_rol", "Error");
        }
        return datos;
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

    public String obtenerBancoPropietario(UUID uuid) {
        String sql = "SELECT etiqueta FROM bancos WHERE uuid_propietario = ? AND estado = 'aprobado'";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("etiqueta");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener banco propietario: " + e.getMessage());
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
        return null;
    }

    public boolean editarRolJugador(UUID uuid, String nuevoRol) {
        String sql = "UPDATE jugadores_reino SET rol = ? WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoRol);
            ps.setString(2, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("[MI] Error al editar rol del jugador: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarTituloJugador(UUID uuidJugador, String nuevoTitulo) {
        String sql = "UPDATE jugadores_reino SET titulo = ? WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevoTitulo);
            stmt.setString(2, uuidJugador.toString());
            int filas = stmt.executeUpdate();
            return filas > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al actualizar título del jugador: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarRolJugador(UUID uuidJugador, String nuevoRol) {
        String sql = "UPDATE jugadores_reino SET rol = ? WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevoRol);
            stmt.setString(2, uuidJugador.toString());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al actualizar el rol del jugador: " + e.getMessage());
            return false;
        }
    }

    public boolean agregarRelacionFamiliar(UUID uuid, String nuevaRelacion) {
        String select = "SELECT historia_familiar FROM genero_jugador WHERE uuid = ?";
        String update = "UPDATE genero_jugador SET historia_familiar = ? WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection()) {
            String historiaExistente = "";
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        historiaExistente = rs.getString("historia_familiar");
                    }
                }
            }

            String nuevaHistoria = historiaExistente == null || historiaExistente.isEmpty()
                    ? nuevaRelacion
                    : historiaExistente + ", " + nuevaRelacion;

            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setString(1, nuevaHistoria);
                ps.setString(2, uuid.toString());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error actualizando historia familiar: " + e.getMessage());
            return false;
        }
    }

    public boolean registrarRolCompleto(UUID uuid, String genero, String nombre, String apellidoP, String apellidoM, String descendencia, String raza) {
        String sql = "REPLACE INTO genero_jugador (uuid, genero, nombre_rol, apellido_paterno_rol, apellido_materno_rol, descendiente_de, raza_rol) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setString(2, capitalizarPalabras(genero));
            ps.setString(3, capitalizarPalabras(nombre));
            ps.setString(4, capitalizarPalabras(apellidoP));
            ps.setString(5, capitalizarPalabras(apellidoM));
            ps.setString(6, capitalizarPalabras(descendencia));
            ps.setString(7, capitalizarPalabras(raza));

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error registrando ficha de rol: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarNombre(UUID uuid, String nuevoNombre) {
        String sql = "UPDATE genero_jugador SET nombre_rol=? WHERE uuid=?";
        try (Connection conn = HikariProvider.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, capitalizarPalabras(nuevoNombre));
            stmt.setString(2, uuid.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizarGenero(UUID uuid, String nuevoGenero) {
        String sql = "UPDATE genero_jugador SET genero=? WHERE uuid=?";
        try (Connection conn = HikariProvider.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, capitalizarPalabras(nuevoGenero));
            stmt.setString(2, uuid.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizarRaza(UUID uuid, String nuevaRaza) {
        String sql = "UPDATE genero_jugador SET raza_rol=? WHERE uuid=?";
        try (Connection conn = HikariProvider.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, capitalizarPalabras(nuevaRaza));
            stmt.setString(2, uuid.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    private String capitalizarPalabras(String input) {
        if (input == null || input.isBlank()) return "";

        return Arrays.stream(input.trim().split("\\s+"))
                .map(palabra -> {
                    // Manejo de guiones o apóstrofos dentro de la palabra
                    String[] partesEspeciales = palabra.split("(?<=-)|(?=-)|(?<=')|(?=')");
                    StringBuilder palabraFinal = new StringBuilder();

                    for (String parte : partesEspeciales) {
                        if (parte.equals("-") || parte.equals("'")) {
                            palabraFinal.append(parte); // Mantener símbolos
                        } else if (!parte.isEmpty()) {
                            palabraFinal.append(Character.toUpperCase(parte.charAt(0)))
                                    .append(parte.substring(1).toLowerCase());
                        }
                    }

                    return palabraFinal.toString();
                })
                .collect(Collectors.joining(" "));
    }

    public List<String> getHistoriaFamiliar(UUID uuid) {
        List<String> historia = new ArrayList<>();
        String sql = "SELECT tipo, uuid_relacion FROM relaciones_familiares WHERE uuid = ?";

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tipo = rs.getString("tipo");
                    String uuidRelacion = rs.getString("uuid_relacion");

                    try {
                        UUID uuidRel = UUID.fromString(uuidRelacion);
                        OfflinePlayer relacionado = Bukkit.getOfflinePlayer(uuidRel);
                        String nombre = relacionado.getName() != null ? relacionado.getName() : "Desconocido";
                        historia.add(tipo + " de " + nombre);
                    } catch (IllegalArgumentException e) {
                        historia.add(tipo + " de UUID inválido");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error obteniendo historia familiar: " + e.getMessage());
        }

        return historia;
    }


    public String getNombreCompletoRol(UUID uuid) {
        String sql = "SELECT nombre_rol, apellido_paterno_rol, apellido_materno_rol FROM genero_jugador WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nombre = rs.getString("nombre_rol");
                    String apellidoPaterno = rs.getString("apellido_paterno_rol");
                    String apellidoMaterno = rs.getString("apellido_materno_rol");
                    return nombre + " " + apellidoPaterno + " " + apellidoMaterno;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error obteniendo nombre completo del rol: " + e.getMessage());
        }
        return null;
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
        // Normalizaciones
        final java.util.Locale L = java.util.Locale.ROOT;
        final String bancoNorm = banco == null ? "" : banco.trim().toLowerCase(L);
        final String reinoNorm = reino == null ? "" : reino.trim().toLowerCase(L);

        // Permisos canon: admite , o ;, recorta espacios y unifica "permiso, permiso2"
        final String permisosCanon = java.util.Arrays.stream(
                        (permisos == null ? "" : permisos.toLowerCase(L)).split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));

        final String sqlSel = """
        SELECT estado, fecha_fin
        FROM contratos_banco_reino
        WHERE TRIM(LOWER(banco_etiqueta)) = TRIM(LOWER(?))
          AND TRIM(LOWER(reino_etiqueta)) = TRIM(LOWER(?))
        """;

        final String sqlUpd = """
        UPDATE contratos_banco_reino
        SET fecha_inicio = ?, fecha_fin = ?, permisos = ?, estado = ?
        WHERE TRIM(LOWER(banco_etiqueta)) = TRIM(LOWER(?))
          AND TRIM(LOWER(reino_etiqueta)) = TRIM(LOWER(?))
        """;

        final String sqlIns = """
        INSERT INTO contratos_banco_reino
        (banco_etiqueta, reino_etiqueta, fecha_inicio, fecha_fin, permisos, estado)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = HikariProvider.getConnection()) {
            conn.setAutoCommit(false);

            String estadoActual = null;
            Timestamp finActual = null;

            // 1) ¿Existe el contrato?
            try (PreparedStatement ps = conn.prepareStatement(sqlSel)) {
                ps.setString(1, bancoNorm);
                ps.setString(2, reinoNorm);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        estadoActual = rs.getString("estado");
                        finActual = rs.getTimestamp("fecha_fin");
                    }
                }
            }

            final long now = System.currentTimeMillis();
            boolean existe = (estadoActual != null);

            // Regla de estado:
            // - Si existe y está ACEPTADO y AÚN VIGENTE -> conservar 'aceptado'
            // - En cualquier otro caso -> 'pendiente'
            String estadoNuevo = "pendiente";
            if (existe && "aceptado".equalsIgnoreCase(estadoActual)
                    && finActual != null && finActual.getTime() > now) {
                estadoNuevo = "aceptado";
            }

            if (existe) {
                // 2) Update (renovación)
                try (PreparedStatement up = conn.prepareStatement(sqlUpd)) {
                    up.setTimestamp(1, inicio);
                    up.setTimestamp(2, fin);
                    up.setString(3, permisosCanon);
                    up.setString(4, estadoNuevo);
                    up.setString(5, bancoNorm);
                    up.setString(6, reinoNorm);
                    up.executeUpdate();
                }
                plugin.getLogger().info("[Contrato] Renovado " + bancoNorm + "↔" + reinoNorm + " estado=" + estadoNuevo);
            } else {
                // 3) Insert (nuevo)
                try (PreparedStatement ins = conn.prepareStatement(sqlIns)) {
                    ins.setString(1, bancoNorm);
                    ins.setString(2, reinoNorm);
                    ins.setTimestamp(3, inicio);
                    ins.setTimestamp(4, fin);
                    ins.setString(5, permisosCanon);
                    ins.setString(6, "pendiente"); // nuevo siempre inicia como pendiente
                    ins.executeUpdate();
                }
                plugin.getLogger().info("[Contrato] Creado " + bancoNorm + "↔" + reinoNorm + " estado=pendiente");
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al insertar/renovar contrato banco-reino: " + e.getMessage());
            // Si quieres, intenta rollback explícito si tu pool lo permite:
            // try { conn.rollback(); } catch (Exception ignore) {}
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

    public String obtenerReinoDeBanco(String etiquetaBanco, Connection conn) throws SQLException {
        String sql = "SELECT reino_etiqueta FROM bancos WHERE etiqueta = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, etiquetaBanco);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("reino_etiqueta");
                }
            }
        }
        return null;
    }

    public boolean tienePermisoContrato(String banco, String reino, String permisoBuscado) {
        String sql = """
        SELECT permisos, fecha_fin
        FROM contratos_banco_reino
        WHERE LOWER(banco_etiqueta) = LOWER(?)
          AND LOWER(reino_etiqueta) = LOWER(?)
          AND estado = 'aceptado'
        """;

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, banco);
            stmt.setString(2, reino);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp fin = rs.getTimestamp("fecha_fin");
                if (fin.before(new Timestamp(System.currentTimeMillis()))) return false;

                String permisos = rs.getString("permisos");
                // Normalizamos espacios y comas
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
        // Usamos LOWER() para evitar problemas de mayúsculas/minúsculas
        String sql = """
        UPDATE contratos_banco_reino
        SET estado = 'aceptado', fecha_inicio = CURRENT_TIMESTAMP
        WHERE LOWER(banco_etiqueta) = LOWER(?)
          AND LOWER(reino_etiqueta) = LOWER(?)
          AND estado = 'pendiente'
        """;

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
        String sql = """
        UPDATE contratos_banco_reino
        SET estado = 'rechazado'
        WHERE LOWER(banco_etiqueta) = LOWER(?)
          AND LOWER(reino_etiqueta) = LOWER(?)
          AND estado = 'pendiente'
        """;

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
            int filas = ps.executeUpdate();

            if (filas > 0) {
                // Actualizar caché inmediatamente
                setReinoCached(jugadorUUID, null);
                return true;
            }
            return false;

        } catch (SQLException e) {
            plugin.getLogger().warning("Error eliminando jugador de reino: " + e.getMessage());
            return false;
        }
    }

    /**
     * Expulsa a un jugador de su reino eliminando toda su asociación.
     *
     * @param uuidJugador UUID del jugador a expulsar.
     * @return true si fue expulsado correctamente, false si no tenía reino.
     */
    public boolean expulsarMiembroReino(UUID uuidJugador, boolean avisarSiNoEstaba) {
        try (Connection conn = HikariProvider.getConnection()) {
            conn.setAutoCommit(false);

            // 1) Borrar la membresía
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM jugadores_reino WHERE uuid = ?")) {
                del.setString(1, uuidJugador.toString());
                int filas = del.executeUpdate();

                conn.commit();

                if (filas > 0) {
                    setReinoCached(uuidJugador, null);
                    return true;
                } else {
                    if (avisarSiNoEstaba) {
                        plugin.getLogger().info("[MI] Jugador " + uuidJugador + " no estaba en ningún reino.");
                    }
                    return false;
                }
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[MI] Error al expulsar miembro del reino (tx): " + e.getMessage());
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

    //-----------------------------------------------------------------------------------------------------------

    //---------------------------------------------Cuenta personal de para bancos-----------------------------------


    public void crearCuentaSiNoExiste(UUID jugador, String etiquetaReino) {
        try (Connection conn = HikariProvider.getConnection()) {
            String sql = "INSERT OR IGNORE INTO cuentas_monedas (uuid_jugador, etiqueta_reino, saldo) VALUES (?, ?, 0)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, jugador.toString());
                ps.setString(2, etiquetaReino);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[MI] Error al crear cuenta de moneda: " + e.getMessage());
        }
    }

    public boolean modificarSaldoJugador(UUID jugador, String etiquetaReino, double cantidad) {
        crearCuentaSiNoExiste(jugador, etiquetaReino);
        try (Connection conn = HikariProvider.getConnection()) {
            String sql = "UPDATE cuentas_monedas SET saldo = saldo + ? WHERE uuid_jugador = ? AND etiqueta_reino = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, cantidad);
                ps.setString(2, jugador.toString());
                ps.setString(3, etiquetaReino);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[MI] Error al modificar saldo de jugador: " + e.getMessage());
            return false;
        }
    }

    public double obtenerSaldoJugador(UUID jugador, String etiquetaReino) {
        crearCuentaSiNoExiste(jugador, etiquetaReino);
        try (Connection conn = HikariProvider.getConnection()) {
            String sql = "SELECT saldo FROM cuentas_monedas WHERE uuid_jugador = ? AND etiqueta_reino = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, jugador.toString());
                ps.setString(2, etiquetaReino);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getDouble("saldo");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[MI] Error al obtener saldo de jugador: " + e.getMessage());
        }
        return 0;
    }

    public boolean transferirEntreJugadores(UUID emisor, UUID receptor, String etiquetaReino, double monto) {
        if (monto <= 0) return false;

        String selectSql = "SELECT saldo FROM cuentas_monedas WHERE uuid_jugador = ? AND etiqueta_reino = ?";
        String updateSql = "UPDATE cuentas_monedas SET saldo = saldo + ? WHERE uuid_jugador = ? AND etiqueta_reino = ?";
        String insertSql = "INSERT INTO cuentas_monedas (uuid_jugador, etiqueta_reino, saldo) VALUES (?, ?, ?)";

        try (Connection conn = HikariProvider.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Obtener saldo del emisor
            double saldoActual = 0;
            try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                psSelect.setString(1, emisor.toString());
                psSelect.setString(2, etiquetaReino);
                ResultSet rs = psSelect.executeQuery();
                if (rs.next()) {
                    saldoActual = rs.getDouble("saldo");
                } else {
                    // El emisor no tiene cuenta, no puede transferir
                    conn.rollback();
                    return false;
                }
            }

            if (saldoActual < monto) {
                conn.rollback();
                return false; // saldo insuficiente
            }

            // 2. Restar al emisor
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setDouble(1, -monto);
                psUpdate.setString(2, emisor.toString());
                psUpdate.setString(3, etiquetaReino);
                psUpdate.executeUpdate();
            }

            // 3. Sumar al receptor (crear cuenta si no existe)
            boolean receptorExiste = false;
            try (PreparedStatement psCheck = conn.prepareStatement(selectSql)) {
                psCheck.setString(1, receptor.toString());
                psCheck.setString(2, etiquetaReino);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next()) {
                    receptorExiste = true;
                }
            }

            if (receptorExiste) {
                try (PreparedStatement psAdd = conn.prepareStatement(updateSql)) {
                    psAdd.setDouble(1, monto);
                    psAdd.setString(2, receptor.toString());
                    psAdd.setString(3, etiquetaReino);
                    psAdd.executeUpdate();
                }
            } else {
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setString(1, receptor.toString());
                    psInsert.setString(2, etiquetaReino);
                    psInsert.setDouble(3, monto);
                    psInsert.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            plugin.getLogger().severe("[MI] Error al transferir monedas entre jugadores: " + e.getMessage());
            return false;
        }
    }

    public boolean aumentarMonedasDisponiblesBanco(String etiquetaBanco, double cantidad) {
        String sql = "UPDATE bancos SET monedas_disponibles = monedas_disponibles + ? WHERE etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, cantidad);
            stmt.setString(2, etiquetaBanco);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("[MI] Error al aumentar monedas disponibles del banco: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Double> obtenerTodosLosSaldosDeJugador(UUID uuid) {
        Map<String, Double> saldos = new HashMap<>();
        String sql = "SELECT etiqueta_banco, saldo FROM cuentas_moneda WHERE uuid_jugador = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String reino = rs.getString("etiqueta_banco");
                double saldo = rs.getDouble("saldo");
                saldos.put(reino, saldo);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al obtener saldos: " + e.getMessage());
        }
        return saldos;
    }

    public Map<String, Double> obtenerMonedasImpresasPorBanco(String etiquetaBanco) {
        Map<String, Double> mapa = new HashMap<>();
        String sql = "SELECT reino_etiqueta, SUM(cantidad_impresa - cantidad_quemada) AS disponible " +
                "FROM monedas_banco WHERE etiqueta_banco = ? GROUP BY reino_etiqueta";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, etiquetaBanco);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mapa.put(rs.getString("reino_etiqueta"), rs.getDouble("disponible"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al obtener monedas impresas por banco: " + e.getMessage());
        }
        return mapa;
    }

    public void descontarCantidadImpresaMoneda(String etiquetaBanco, String reinoEtiqueta, double cantidad, Connection conn) throws SQLException {
        String sql = "UPDATE monedas_banco SET cantidad_impresa = cantidad_impresa - ? " +
                "WHERE etiqueta_banco = ? AND reino_etiqueta = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, cantidad);
            stmt.setString(2, etiquetaBanco);
            stmt.setString(3, reinoEtiqueta);
            stmt.executeUpdate();
        }
    }


    public double obtenerCantidadImpresaDisponible(String etiquetaBanco, String reinoEtiqueta) {
        String sql = "SELECT (cantidad_impresa - cantidad_quemada) AS disponible " +
                "FROM monedas_banco WHERE etiqueta_banco = ? AND reino_etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, etiquetaBanco);
            stmt.setString(2, reinoEtiqueta);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getDouble("disponible");
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al obtener cantidad disponible: " + e.getMessage());
        }
        return 0.0;
    }

    public boolean registrarSolicitudMoneda(UUID jugadorUUID, String etiquetaBanco, double cantidad, String reinoJugador) {
        String sql = "INSERT INTO solicitudes_monedas (uuid_jugador, etiqueta_banco, cantidad, fecha, estado, reino_jugador) VALUES (?, ?, ?, CURRENT_TIMESTAMP, 'pendiente', ?)";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, jugadorUUID.toString());
            stmt.setString(2, etiquetaBanco);
            stmt.setDouble(3, cantidad);
            stmt.setString(4, reinoJugador);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al registrar solicitud de moneda: " + e.getMessage());
            return false;
        }
    }

    public boolean aumentarMonedaImpresaBanco(String etiquetaBanco, String etiquetaReino, double cantidad) {
        String sqlUpdate = "UPDATE monedas_banco SET cantidad_impresa = cantidad_impresa + ? " +
                "WHERE etiqueta_banco = ? AND reino_etiqueta = ?";
        String sqlInsert = "INSERT INTO monedas_banco (etiqueta_banco, reino_etiqueta, cantidad_impresa) " +
                "VALUES (?, ?, ?)";

        try (Connection conn = HikariProvider.getConnection()) {
            // Primero intentamos actualizar
            try (PreparedStatement update = conn.prepareStatement(sqlUpdate)) {
                update.setDouble(1, cantidad);
                update.setString(2, etiquetaBanco);
                update.setString(3, etiquetaReino);
                int filas = update.executeUpdate();
                if (filas > 0) return true;
            }

            // Si no existe la fila, insertamos nueva
            try (PreparedStatement insert = conn.prepareStatement(sqlInsert)) {
                insert.setString(1, etiquetaBanco);
                insert.setString(2, etiquetaReino);
                insert.setDouble(3, cantidad);
                return insert.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al aumentar moneda impresa por banco: " + e.getMessage());
            return false;
        }
    }

    public boolean aumentarMonedaQuemadaBanco(String etiquetaBanco, String etiquetaReino, double cantidad) {
        String sqlUpdate = "UPDATE monedas_banco SET cantidad_quemada = cantidad_quemada + ? " +
                "WHERE etiqueta_banco = ? AND reino_etiqueta = ?";
        String sqlInsert = "INSERT INTO monedas_banco (etiqueta_banco, reino_etiqueta, cantidad_quemada) " +
                "VALUES (?, ?, ?)";

        try (Connection conn = HikariProvider.getConnection()) {
            try (PreparedStatement update = conn.prepareStatement(sqlUpdate)) {
                update.setDouble(1, cantidad);
                update.setString(2, etiquetaBanco);
                update.setString(3, etiquetaReino);
                if (update.executeUpdate() > 0) return true;
            }

            try (PreparedStatement insert = conn.prepareStatement(sqlInsert)) {
                insert.setString(1, etiquetaBanco);
                insert.setString(2, etiquetaReino);
                insert.setDouble(3, cantidad);
                return insert.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al aumentar moneda quemada por banco: " + e.getMessage());
            return false;
        }
    }

    public boolean aumentarMonedaConvertidaBanco(String etiquetaBanco, String etiquetaReino, double cantidad) {
        String sqlUpdate = "UPDATE monedas_banco SET cantidad_convertida = cantidad_convertida + ? " +
                "WHERE etiqueta_banco = ? AND reino_etiqueta = ?";
        String sqlInsert = "INSERT INTO monedas_banco (etiqueta_banco, reino_etiqueta, cantidad_convertida) " +
                "VALUES (?, ?, ?)";

        try (Connection conn = HikariProvider.getConnection()) {
            try (PreparedStatement update = conn.prepareStatement(sqlUpdate)) {
                update.setDouble(1, cantidad);
                update.setString(2, etiquetaBanco);
                update.setString(3, etiquetaReino);
                if (update.executeUpdate() > 0) return true;
            }

            try (PreparedStatement insert = conn.prepareStatement(sqlInsert)) {
                insert.setString(1, etiquetaBanco);
                insert.setString(2, etiquetaReino);
                insert.setDouble(3, cantidad);
                return insert.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al aumentar moneda convertida por banco: " + e.getMessage());
            return false;
        }
    }
    // Obtener saldo de cuenta de banco por jugador
    public double obtenerSaldoCuentaBanco(UUID jugador, String etiquetaBanco) {
        String sql = "SELECT saldo FROM cuentas_moneda WHERE uuid_jugador = ? AND etiqueta_banco = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugador.toString());
            ps.setString(2, etiquetaBanco);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("saldo");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[BD] Error al obtener saldo cuenta: " + e.getMessage());
        }
        return 0.0;
    }

    // Establecer o actualizar saldo de cuenta
    public void establecerSaldoCuentaBanco(UUID jugador, String etiquetaBanco, double saldo) {
        String sql = "INSERT INTO cuentas_moneda (uuid_jugador, etiqueta_banco, saldo) VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid_jugador, etiqueta_banco) DO UPDATE SET saldo = excluded.saldo";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugador.toString());
            ps.setString(2, etiquetaBanco);
            ps.setDouble(3, saldo);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[BD] Error al guardar saldo cuenta: " + e.getMessage());
        }
    }

    // Añadir saldo (positivo o negativo)
    public void modificarSaldoCuentaBanco(UUID jugador, String etiquetaBanco, double delta) {
        double actual = obtenerSaldoCuentaBanco(jugador, etiquetaBanco);
        establecerSaldoCuentaBanco(jugador, etiquetaBanco, actual + delta);
    }

//-------------------------------Mercado de Reino--------------------------------------------

    //-------------------------------------------------------------------------------------

    public boolean insertarItemEnMercado(UUID uuid, String reino, String itemSerializado, int cantidad, double precio) {
        String sql = "INSERT INTO mercado_reino (uuid_vendedor, etiqueta_reino, item_serializado, cantidad, precio) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, reino);
            stmt.setString(3, itemSerializado);
            stmt.setInt(4, cantidad);
            stmt.setDouble(5, precio);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("[MI] Error insertando item en mercado: " + e.getMessage());
            return false;
        }
    }

    public List<ItemEnVenta> getItemsMercadoDelReino(String reino) {
        List<ItemEnVenta> items = new ArrayList<>();
        String sql = "SELECT * FROM mercado_reino WHERE etiqueta_reino = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reino);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new ItemEnVenta(
                            rs.getInt("id"),
                            UUID.fromString(rs.getString("uuid_vendedor")),
                            rs.getString("etiqueta_reino"),
                            rs.getString("item_serializado"),
                            rs.getInt("cantidad"),
                            rs.getDouble("precio")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[MI] Error al obtener mercado: " + e.getMessage());
        }
        return items;
    }

    public boolean procesarCompraEnMercado(int id, UUID comprador, double precio) {
        String sqlGetItem = "SELECT uuid_vendedor, etiqueta_reino FROM mercado_reino WHERE id = ?";
        String sqlEliminar = "DELETE FROM mercado_reino WHERE id = ?";

        try (Connection conn = HikariProvider.getConnection()) {
            conn.setAutoCommit(false);

            String vendedor;
            String reino;

            // 1️⃣ Obtener datos del ítem en venta
            try (PreparedStatement stmt = conn.prepareStatement(sqlGetItem)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) return false;
                    vendedor = rs.getString("uuid_vendedor");
                    reino = rs.getString("etiqueta_reino");
                }
            }

            // 2️⃣ Obtener banco principal del comprador
            String bancoComprador = obtenerBancoPrincipal(comprador, reino, conn);
            if (bancoComprador == null) {
                plugin.getLogger().warning("[MI] Comprador no tiene bancos en este reino.");
                conn.rollback();
                return false;
            }

            // 3️⃣ (Eliminado) ✅ Ya no verificamos saldo del banco principal

            // 4️⃣ Verificar saldo suficiente en la cuenta global
            double saldoGlobal = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT saldo FROM cuentas_monedas WHERE uuid_jugador = ? AND etiqueta_reino = ?")) {
                ps.setString(1, comprador.toString());
                ps.setString(2, reino);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) saldoGlobal = rs.getDouble("saldo");
                }
            }
            if (saldoGlobal < precio) {
                plugin.getLogger().warning("[MI] Comprador sin saldo suficiente en la cuenta global.");
                conn.rollback();
                return false;
            }

            // 5️⃣ Obtener banco principal del vendedor
            String bancoVendedor = obtenerBancoPrincipal(UUID.fromString(vendedor), reino, conn);

            // 6️⃣ Descontar al comprador en el banco principal
//            restarSaldoBanco(comprador, bancoComprador, precio, conn);

            // 🔹 También reflejar en cuentas_monedas globales
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE cuentas_monedas SET saldo = saldo - ? WHERE uuid_jugador = ? AND etiqueta_reino = ?")) {
                ps.setDouble(1, precio);
                ps.setString(2, comprador.toString());
                ps.setString(3, reino);
                ps.executeUpdate();
            }

            // 7️⃣ Sumar al vendedor
            if (bancoVendedor != null) {
                sumarSaldoBanco(UUID.fromString(vendedor), bancoVendedor, precio, conn);
            }

            // 🔹 También reflejar en cuentas_monedas globales del vendedor
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE cuentas_monedas SET saldo = saldo + ? WHERE uuid_jugador = ? AND etiqueta_reino = ?")) {
                ps.setDouble(1, precio);
                ps.setString(2, vendedor);
                ps.setString(3, reino);
                ps.executeUpdate();
            }

            // 8️⃣ Eliminar ítem del mercado
            try (PreparedStatement eliminar = conn.prepareStatement(sqlEliminar)) {
                eliminar.setInt(1, id);
                eliminar.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            plugin.getLogger().warning("[MI] Error al procesar compra en mercado: " + e.getMessage());
            return false;
        }
    }

    private String obtenerBancoPrincipal(UUID jugador, String reino, Connection conn) throws SQLException {
        String sql = """
        SELECT b.etiqueta, IFNULL(cmb.saldo, 0) AS saldo
        FROM bancos b
        LEFT JOIN cuentas_moneda cmb ON cmb.etiqueta_banco = b.etiqueta AND cmb.uuid_jugador = ?
        WHERE b.reino_etiqueta = ? AND b.estado = 'aprobado'
        ORDER BY saldo DESC
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, jugador.toString());
            stmt.setString(2, reino);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("etiqueta"); // Mayor saldo o primero
            }
        }
        return null;
    }

    private void sumarSaldoBanco(UUID jugador, String banco, double monto, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE cuentas_moneda SET saldo = saldo + ? WHERE uuid_jugador = ? AND etiqueta_banco = ?")) {
            stmt.setDouble(1, monto);
            stmt.setString(2, jugador.toString());
            stmt.setString(3, banco);
            stmt.executeUpdate();
        }
    }

    public boolean tieneSaldoSuficienteEnBancos(UUID jugador, String reino, double montoNecesario) {
        String sql = """
        SELECT IFNULL(cmb.saldo, 0) AS saldo
        FROM bancos b
        LEFT JOIN cuentas_moneda cmb 
            ON cmb.etiqueta_banco = b.etiqueta 
            AND cmb.uuid_jugador = ?
        WHERE b.reino_etiqueta = ? AND b.estado = 'aprobado'
        ORDER BY saldo DESC
        """;

        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, jugador.toString());
            stmt.setString(2, reino);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double saldoMaximo = rs.getDouble("saldo");
                    return saldoMaximo >= montoNecesario;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[MI] Error al verificar saldo en bancos: " + e.getMessage());
        }
        return false;
    }

    public Map<String, Double> obtenerTodosLosSaldosJugador(UUID jugador) {
        Map<String, Double> saldos = new HashMap<>();
        String sql = "SELECT etiqueta_reino, saldo FROM cuentas_monedas WHERE uuid_jugador = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, jugador.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    saldos.put(rs.getString("etiqueta_reino"), rs.getDouble("saldo"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[MI] Error al obtener saldos jugador: " + e.getMessage());
        }
        return saldos;
    }

    public ItemEnVenta buscarItemEnMercadoPorID(int id) {
        String sql = "SELECT * FROM mercado_reino WHERE id = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ItemEnVenta(
                            rs.getInt("id"),
                            UUID.fromString(rs.getString("uuid_vendedor")),
                            rs.getString("etiqueta_reino"),
                            rs.getString("item_serializado"),
                            rs.getInt("cantidad"),
                            rs.getDouble("precio")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[MI] Error al buscar ítem por ID: " + e.getMessage());
        }
        return null;
    }

    public String getReinoJugador(UUID uuid) {
        String sql = "SELECT etiqueta_reino FROM jugadores_reino WHERE uuid = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("etiqueta_reino");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[MI] Error al obtener el reino del jugador: " + e.getMessage());
        }
        return null;
    }

    // Total de monedas impresas en el reino
    public double obtenerTotalMonedasImpresas(String etiquetaReino) {
        double total = 0;
        String sql = "SELECT SUM(cantidad_impresa) FROM monedas_banco WHERE reino_etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, etiquetaReino);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) total = rs.getDouble(1);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al obtener total impresas: " + e.getMessage());
        }
        return total;
    }

    // Total de monedas quemadas en el reino
    public double obtenerTotalMonedasQuemadas(String etiquetaReino) {
        double total = 0;
        String sql = "SELECT SUM(cantidad_quemada) FROM monedas_banco WHERE reino_etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, etiquetaReino);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) total = rs.getDouble(1);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al obtener total quemadas: " + e.getMessage());
        }
        return total;
    }

    // Total de dinero convertido en el reino
    public double obtenerTotalDineroConvertido(String etiquetaReino) {
        double total = 0;
        String sql = "SELECT SUM(cantidad_convertida) FROM monedas_banco WHERE reino_etiqueta = ?";
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, etiquetaReino);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) total = rs.getDouble(1);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[MenuInteractivo] Error al obtener total convertido: " + e.getMessage());
        }
        return total;
    }

    /**
     * Consulta una fila completa de la base de datos y la devuelve como Map<String, Object>.
     *
     * @param sql    Consulta SQL con un WHERE, ejemplo: "SELECT * FROM genero_jugador WHERE uuid = ?"
     * @param params Parámetros que reemplazarán los ? en el SQL
     * @return Map con nombre de columna -> valor, o null si no hay resultados
     */
    public Map<String, Object> consultarFila(String sql, Object... params) {
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                if (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        fila.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    return fila;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error en consultarFila: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

//    ----------------------------------------------------------------------------------------
//
//    ---------------------------------Daño a aliados---------------------------------------------------


    // Dentro de GestorBaseDeDatos
    private final Map<UUID, String> cacheReinos = new ConcurrentHashMap<>();

    public String getReinoCached(UUID uuid) {
        return cacheReinos.get(uuid);
    }

    public void setReinoCached(UUID uuid, String reino) {
        if (reino == null) {
            cacheReinos.remove(uuid);
        } else {
            cacheReinos.put(uuid, reino);
        }
    }

    public void limpiarCacheJugador(UUID uuid) {
        cacheReinos.remove(uuid);
    }

    public void limpiarTodaCache() {
        cacheReinos.clear();
    }

    public String getReinoDeJugador(String uuidStr) {
        UUID uuid = UUID.fromString(uuidStr);

        // 1) Revisar caché
        String cached = getReinoCached(uuid);
        if (cached != null) return cached;

        // 2) Consultar en la base de datos si no está en caché
        String reino = null;
        try (Connection conn = HikariProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT etiqueta_reino FROM jugadores_reino WHERE uuid = ? LIMIT 1")) {
            ps.setString(1, uuidStr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    reino = rs.getString("etiqueta_reino");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error consultando el reino del jugador " + uuidStr + ": " + e.getMessage());
        }

        // 3) Guardar en caché si existe
        if (reino != null) {
            setReinoCached(uuid, reino);
        }
        return reino;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getBaseDeDatos().limpiarCacheJugador(event.getPlayer().getUniqueId());
    }



}
