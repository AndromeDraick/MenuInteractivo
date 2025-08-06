package AndromeDraick.menuInteractivo.chat;

import AndromeDraick.menuInteractivo.database.HikariProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ChatDataManager {

    public static class ChatPlayerData {
        public String trabajo = "";
        public String titulo = "";
        public String rol = "";
        public String genero = "";
        public String nombreRol = "";
        public String apellidoPaternoRol = "";
        public String apellidoMaternoRol = "";

        public boolean tieneReino() {
            return titulo != null && !titulo.isEmpty();
        }
        public boolean tieneTrabajo() {
            return trabajo != null && !trabajo.isEmpty();
        }
    }

    /**
     * Obtiene los datos del jugador desde la base de datos
     */
    public ChatPlayerData getPlayerData(UUID uuid) {
        ChatPlayerData data = new ChatPlayerData();

        try (Connection conn = HikariProvider.getConnection()) {

            // 1. Jugadores - trabajo
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT trabajo FROM jugadores WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        data.trabajo = rs.getString("trabajo");
                    }
                }
            }

            // 2. Genero jugador
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT genero, nombre_rol, apellido_paterno_rol, apellido_materno_rol FROM genero_jugador WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        data.genero = rs.getString("genero");
                        data.nombreRol = rs.getString("nombre_rol");
                        data.apellidoPaternoRol = rs.getString("apellido_paterno_rol");
                        data.apellidoMaternoRol = rs.getString("apellido_materno_rol");
                    }
                }
            }

            // 3. Jugadores de reino
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT titulo, rol FROM jugadores_reino WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        data.titulo = rs.getString("titulo");
                        data.rol = rs.getString("rol");
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return data;
    }
}
