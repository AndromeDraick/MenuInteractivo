package AndromeDraick.menuInteractivo.chat;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;

public class ChatFormatListener implements Listener {

    private final MenuInteractivo plugin;
    private final ChatDataManager dataManager;
    private FileConfiguration chatConfig;

    public ChatFormatListener(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.dataManager = new ChatDataManager();
        loadChatConfig();
    }

    private void loadChatConfig() {
        File chatFile = new File(plugin.getDataFolder(), "config_chat.yml");
        if (!chatFile.exists()) {
            plugin.saveResource("config_chat.yml", false);
        }
        this.chatConfig = YamlConfiguration.loadConfiguration(chatFile);
    }

    private String getPrimaryGroup(Player player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
            return user.getPrimaryGroup().toLowerCase();
        } catch (Exception e) {
            return "default";
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatDataManager.ChatPlayerData data = dataManager.getPlayerData(player.getUniqueId());

        // Obtener grupo principal
        String grupo = getPrimaryGroup(player);

        // Obtener formato según grupo desde config_chat.yml
        String formato = chatConfig.getString("chat.formatos." + grupo);
        if (formato == null) {
            formato = chatConfig.getString("chat.formatos.default", "&7%nombre_rol%: &f%mensaje%");
        }

        String sinTrabajo = chatConfig.getString("chat.sin_trabajo", "&7Sin trabajo");
        String sinReino = chatConfig.getString("chat.sin_reino", "&7Sin reino");

        // Reemplazar placeholders
        String mensaje = formato
                .replace("%mensaje%", event.getMessage())
                .replace("%grupo%", grupo)
                .replace("%trabajo%", data.tieneTrabajo() ? data.trabajo : sinTrabajo)
                .replace("%titulo%", data.tieneReino() ? data.titulo : sinReino)
                .replace("%rol%", data.tieneReino() ? data.rol : "")
                .replace("%genero%", data.genero != null ? data.genero : "")
                .replace("%nombre_rol%", data.nombreRol != null ? data.nombreRol : player.getName())
                .replace("%apellido_paterno_rol%", data.apellidoPaternoRol != null ? data.apellidoPaternoRol : "")
                .replace("%apellido_materno_rol%", data.apellidoMaternoRol != null ? data.apellidoMaternoRol : "");

        // Traducir colores
        mensaje = ChatColor.translateAlternateColorCodes('&', mensaje);

        // Cancelar chat por defecto y enviar personalizado
        event.setCancelled(true);
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            online.sendMessage(mensaje);
        }
    }
}
