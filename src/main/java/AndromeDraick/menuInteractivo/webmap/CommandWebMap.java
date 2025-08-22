package AndromeDraick.menuInteractivo.webmap;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class CommandWebMap implements CommandExecutor, TabCompleter {
    private final MenuInteractivo plugin;
    private final WebMapManager manager;

    public CommandWebMap(MenuInteractivo plugin, WebMapManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("menuinteractivo.webmap.admin")) {
            s.sendMessage(ChatColor.RED + "No tienes permiso.");
            return true;
        }
        if (args.length == 0) {
            s.sendMessage(ChatColor.YELLOW + "/miwebmap <iniciar|parar|recargar|render|limpiar>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "iniciar":
                manager.enable();
                s.sendMessage(ChatColor.GREEN + "WebMap iniciado.");
                return true;
            case "parar":
                manager.disable();
                s.sendMessage(ChatColor.GREEN + "WebMap detenido.");
                return true;
            case "recargar":
                manager.disable();
                manager.enable();
                s.sendMessage(ChatColor.GREEN + "WebMap recargado.");
                return true;
            case "render":
                if (args.length < 3) {
                    s.sendMessage(ChatColor.YELLOW + "Uso: /miwebmap render <mundo> <radio_tiles>");
                    return true;
                }
                String mundo = args[1];
                int radio;
                try { radio = Integer.parseInt(args[2]); } catch (Exception e) { radio = 1; }
                manager.renderInicial(mundo, radio);
                s.sendMessage(ChatColor.GREEN + "Render inicial encolado para " + mundo + " radio " + radio);
                return true;
            case "limpiar":
                s.sendMessage(ChatColor.GRAY + "Por simplicidad, borra manualmente la carpeta de tiles si deseas limpiar.");
                return true;
            default:
                s.sendMessage(ChatColor.YELLOW + "/miwebmap <iniciar|parar|recargar|render|limpiar>");
                return true;
        }
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("iniciar","parar","recargar","render","limpiar");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("render")) {
            return org.bukkit.Bukkit.getWorlds().stream().map(World::getName).toList();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("render")) {
            return java.util.Arrays.asList("1","2","3","5","10");
        }
        return java.util.Collections.emptyList();
    }
}
