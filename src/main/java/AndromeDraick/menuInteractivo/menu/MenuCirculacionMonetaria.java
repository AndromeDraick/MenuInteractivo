package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.model.SolicitudMoneda;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuCirculacionMonetaria implements Listener {

    private final MenuInteractivo plugin;
    private final BancoManager bancoManager;
    private final DecimalFormat formato = new DecimalFormat("#.##");

    // Mapeo de jugador que abrió menú con ID de solicitud para clics
    private final Map<UUID, List<SolicitudMoneda>> solicitudesPorJugador = new HashMap<>();

    public MenuCirculacionMonetaria(MenuInteractivo plugin) {
        this.plugin = plugin;
        this.bancoManager = plugin.getBancoManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void abrirMenu(Player jugador, String etiquetaBanco) {
        List<SolicitudMoneda> solicitudes = bancoManager.obtenerSolicitudesPendientes(etiquetaBanco);
        solicitudesPorJugador.put(jugador.getUniqueId(), solicitudes);

        int size = Math.max(9, ((solicitudes.size() / 9) + 1) * 9);
        Inventory menu = Bukkit.createInventory(null, size, ChatColor.YELLOW + "Solicitudes de Moneda");

        for (int i = 0; i < solicitudes.size(); i++) {
            SolicitudMoneda solicitud = solicitudes.get(i);
            UUID jugadorUUID = solicitud.getUuidJugador();

            String reinoJugador = bancoManager.obtenerReinoJugador(jugadorUUID);
            String nombreMoneda = bancoManager.obtenerNombreMonedaDeReino(reinoJugador);
            boolean bancoTieneMoneda = bancoManager.bancoTieneMoneda(etiquetaBanco, reinoJugador);

            ItemStack item = new ItemStack(Material.YELLOW_BUNDLE);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(ChatColor.GOLD + "Solicitud #" + solicitud.getId());
            meta.setLore(List.of(
                    ChatColor.GRAY + "Jugador: " + Bukkit.getOfflinePlayer(jugadorUUID).getName(),
                    ChatColor.GRAY + "Cantidad: $" + ChatColor.GREEN + formato.format(solicitud.getCantidad()) + " " + nombreMoneda,
                    ChatColor.GRAY + "Moneda del reino: " + (bancoTieneMoneda ? ChatColor.AQUA + nombreMoneda : ChatColor.RED + "NO DISPONIBLE"),
                    ChatColor.GRAY + "Banco: " + ChatColor.AQUA + solicitud.getEtiquetaBanco(),
                    ChatColor.GRAY + "Fecha: " + solicitud.getFecha(),
                    ChatColor.YELLOW + "Click izquierdo: Aceptar",
                    ChatColor.RED + "Click derecho: Rechazar"
            ));

            item.setItemMeta(meta);
            menu.setItem(i, item);
        }

        jugador.openInventory(menu);
    }

    @EventHandler
    public void alClickearSolicitud(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();
        if (!titulo.equals(ChatColor.YELLOW + "Solicitudes de Moneda")) return;

        event.setCancelled(true);
        Player jugadorAceptador = (Player) event.getWhoClicked(); // quien hace clic
        List<SolicitudMoneda> solicitudes = solicitudesPorJugador.get(jugadorAceptador.getUniqueId());
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;
        int slot = event.getSlot();
        if (slot < 0 || slot >= solicitudes.size()) return;

        SolicitudMoneda solicitud = solicitudes.get(slot);
        boolean clickIzquierdo = event.isLeftClick();

        boolean exito = bancoManager.procesarSolicitud(solicitud.getId(), clickIzquierdo);

        if (exito && clickIzquierdo) {
            // Darle las monedas al jugador que aceptó la solicitud
            String bancoEtiqueta = solicitud.getEtiquetaBanco();
            String reino = bancoManager.obtenerReinoDeBanco(bancoEtiqueta);
            double cantidad = solicitud.getCantidad();

            bancoManager.modificarSaldoCuentaJugador(jugadorAceptador.getUniqueId(), bancoEtiqueta, cantidad);

            jugadorAceptador.sendMessage(ChatColor.GREEN + "Has recibido " + cantidad + " reinas,  por la solicitud aceptada, favor de entregarlas al respectivo lider banquero");
        }

        jugadorAceptador.sendMessage(ChatColor.GREEN + "Solicitud " + (clickIzquierdo ? "aceptada" : "rechazada") + " correctamente.");
        abrirMenu(jugadorAceptador, solicitud.getEtiquetaBanco());
    }

    @EventHandler
    public void alArrastrarSolicitud(InventoryDragEvent event) {
        String titulo = event.getView().getTitle();
        if (titulo.equals(ChatColor.YELLOW + "Solicitudes de Moneda")) {
            event.setCancelled(true);
        }
    }

}
