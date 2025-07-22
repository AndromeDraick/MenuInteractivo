package AndromeDraick.menuInteractivo.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import AndromeDraick.menuInteractivo.MenuInteractivo;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class HikariProvider {
    private static HikariDataSource ds;
    private static boolean esMySQL = false;

    public static void init(MenuInteractivo plugin) {
        File cfgDir = new File(plugin.getDataFolder(), "configuracion");
        if (!cfgDir.exists()) cfgDir.mkdirs();
        File configFile = new File(cfgDir, "config_basededatos.yml");
        if (!configFile.exists()) plugin.saveResource("configuracion/config_basededatos.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        esMySQL = config.getString("tipo", "sqlite").equalsIgnoreCase("mysql");

        HikariConfig hikariConfig = new HikariConfig();
        if (esMySQL) {
            String host = config.getString("mysql.host");
            int port = config.getInt("mysql.puerto");
            String db = config.getString("mysql.base");
            String user = config.getString("mysql.usuario");
            String pass = config.getString("mysql.contrasena");
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db +
                    "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            hikariConfig.setUsername(user);
            hikariConfig.setPassword(pass);
        } else {
            File sqliteFile = new File(plugin.getDataFolder(), "base_jugadores.db");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + sqliteFile.getPath());
            hikariConfig.setUsername("");
            hikariConfig.setPassword("");
        }

        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(20000);
        hikariConfig.setIdleTimeout(300_000);
        hikariConfig.setMaxLifetime(600_000);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");


        ds = new HikariDataSource(hikariConfig);
        plugin.getLogger().info("[MenuInteractivo] HikariCP inicializado (" +
                (esMySQL ? "MySQL" : "SQLite") + ").");
    }

    public static Connection getConnection() throws SQLException {
        if (ds == null) {
            throw new IllegalStateException("HikariProvider no inicializado");
        }
        return ds.getConnection();
    }

    public static boolean esMySQL() {
        return esMySQL;
    }

    public static void closePool() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }
}
