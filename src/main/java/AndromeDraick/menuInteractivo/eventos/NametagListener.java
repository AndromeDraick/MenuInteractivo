package AndromeDraick.menuInteractivo.eventos;

import AndromeDraick.menuInteractivo.utilidades.NametagManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NametagListener implements Listener {

    private final NametagManager nametagManager;

    public NametagListener(NametagManager nametagManager) {
        this.nametagManager = nametagManager;
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent e) {
        nametagManager.ocultarJugador(e.getPlayer());
    }
}
