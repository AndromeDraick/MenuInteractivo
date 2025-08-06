package AndromeDraick.menuInteractivo.listeners;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.projectiles.ProjectileSource;

public class ReinoFriendlyFireListener implements Listener {

    private final MenuInteractivo plugin;
    private final GestorBaseDeDatos db;

    public ReinoFriendlyFireListener(MenuInteractivo plugin, GestorBaseDeDatos db) {
        this.plugin = plugin;
        this.db = db;
    }

    /**
     * Cancela daño entre jugadores del mismo reino (melee, flechas, tridentes)
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;

        // Si el atacante es un jugador directo
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        }
        // Si el atacante es un proyectil (flecha, tridente, etc.)
        else if (event.getDamager() instanceof Projectile proj) {
            ProjectileSource shooter = proj.getShooter();
            if (shooter instanceof Player p) {
                attacker = p;
            }
        }

        if (attacker == null) return;

        // Obtener reinos desde la base de datos (con caché interno)
        String reinoVictima = db.getReinoDeJugador(victim.getUniqueId().toString());
        String reinoAtacante = db.getReinoDeJugador(attacker.getUniqueId().toString());

        // Si ambos están en el mismo reino, cancelar daño
        if (reinoVictima != null && reinoVictima.equals(reinoAtacante)) {
            event.setCancelled(true);
        }
    }

    /**
     * Evita que las pociones afecten a jugadores aliados del mismo reino
     */
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if (!(event.getPotion().getShooter() instanceof Player thrower)) return;

        String reinoAtacante = db.getReinoDeJugador(thrower.getUniqueId().toString());
        if (reinoAtacante == null) return;

        event.getAffectedEntities().forEach(entity -> {
            if (entity instanceof Player victim) {
                String reinoVictima = db.getReinoDeJugador(victim.getUniqueId().toString());
                if (reinoVictima != null && reinoVictima.equals(reinoAtacante)) {
                    // Evita efectos en aliados
                    event.setIntensity(victim, 0);
                }
            }
        });
    }
}
