package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.managers.BancoManager;
import AndromeDraick.menuInteractivo.model.Banco;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class MenuBancos implements Listener {

    private static final String PLAIN_SOLICITUDES   = "Solicitudes de Bancos";
    private static final String PLAIN_LISTA         = "Bancos del Reino ";
    private static final String PLAIN_INDIVIDUAL    = "Banco: ";

    private static final String TITULO_SOLICITUDES  = ChatColor.DARK_GREEN + PLAIN_SOLICITUDES;
    private static final String TITULO_LISTA        = ChatColor.DARK_AQUA  + PLAIN_LISTA;
    private static final String TITULO_INDIVIDUAL   = ChatColor.GOLD       + PLAIN_INDIVIDUAL;

    private final MenuInteractivo plugin;
    private final BancoManager bancoManager;
    private final Economy economia;

    public MenuBancos(MenuInteractivo plugin) {
        this.plugin       = plugin;
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos(),plugin.getEconomia());
        this.economia     = plugin.getEconomia();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /** 1) Menú de solicitudes pendientes */
    public void abrirSolicitudes(Player p) {
        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }

        if (!bancoManager.esReyDeReino(p.getUniqueId(), reino)) {
            p.sendMessage(ChatColor.RED + "Solo el rey del reino puede revisar las solicitudes.");
            return;
        }

        List<Banco> pendientes = bancoManager.obtenerBancosPendientes(reino);
        if (pendientes.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay solicitudes pendientes.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null,
                ((pendientes.size() - 1) / 9 + 1) * 9,
                TITULO_SOLICITUDES
        );
        for (Banco b : pendientes) {
            ItemStack it = new ItemStack(Material.PAPER);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName(ChatColor.YELLOW + b.getEtiqueta());
            m.setLore(List.of(
                    ChatColor.GRAY + b.getNombre(),
                    ChatColor.GREEN + "Izquierdo: aprobar",
                    ChatColor.RED   + "Derecho: rechazar"
            ));
            it.setItemMeta(m);
            inv.addItem(it);
        }
        p.openInventory(inv);
    }

    /** 2) Menú de bancos aprobados */
    public void abrirListaActivos(Player p) {
        String reino = bancoManager.obtenerReinoJugador(p.getUniqueId());
        if (reino == null) {
            p.sendMessage(ChatColor.RED + "No perteneces a ningún reino.");
            return;
        }
        List<Banco> activos = bancoManager.obtenerBancosDeReino(reino);
        if (activos.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "No hay bancos activos en tu reino.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null,
                ((activos.size() - 1) / 9 + 1) * 9,
                TITULO_LISTA + reino
        );
        for (Banco b : activos) {
            ItemStack it = new ItemStack(Material.BOOK);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName(ChatColor.YELLOW + b.getEtiqueta());
            m.setLore(List.of(
                    ChatColor.GRAY + b.getNombre(),
                    ChatColor.GREEN + "Haz clic para ver detalles"
            ));
            it.setItemMeta(m);
            inv.addItem(it);
        }
        p.openInventory(inv);
    }

    /** 3) Menú individual de un banco: saldo, retirar/ingresar */
    public void abrirIndividual(Player p, String etiqueta) {
        String propia = bancoManager.obtenerBancoDeJugador(p.getUniqueId());
        if (!etiqueta.equals(propia)) {
            p.sendMessage(ChatColor.RED + "No estás vinculado a ese banco.");
            return;
        }
        Banco b = bancoManager.obtenerBanco(etiqueta);
        if (b == null) {
            p.sendMessage(ChatColor.RED + "Error: el banco no fue encontrado.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, TITULO_INDIVIDUAL + etiqueta);

        // Saldo de monedas impresas disponibles
        ItemStack saldoMonedas = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = saldoMonedas.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Monedas impresas disponibles");
        meta.setLore(List.of(ChatColor.GOLD + String.valueOf(bancoManager.obtenerCantidadImpresaDisponible(etiqueta))));
        saldoMonedas.setItemMeta(meta);
        inv.setItem(13, saldoMonedas);

        // Obtener número de solicitudes pendientes
        int solicitudes = bancoManager.obtenerSolicitudesPendientes(etiqueta).size();

        // Botón para abrir MenuCirculacionMonetaria
        ItemStack verSolicitudes = new ItemStack(Material.YELLOW_BUNDLE);
        verSolicitudes.setAmount(Math.min(64, Math.max(1, solicitudes))); // prevenir 0 o más de 64
        ItemMeta m = verSolicitudes.getItemMeta();
        m.setDisplayName(ChatColor.YELLOW + "Ver solicitudes de moneda");
        m.setLore(List.of(
                ChatColor.GRAY + "Pendientes: " + solicitudes,
                ChatColor.GRAY + "Haz clic para revisar y aceptar"
        ));
        verSolicitudes.setItemMeta(m);
        inv.setItem(15, verSolicitudes);

        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String rawTitle = e.getView().getTitle();
        String title    = ChatColor.stripColor(rawTitle);

        // 1) Solo seguimos si es uno de nuestros menús
        if (!isMiMenu(title)) return;

        // 2) Cancelamos TODO click interno
        e.setCancelled(true);

        // 3) Procesamos acción
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta()) return;

        // 3.1) Solicitudes
        if (title.equals(PLAIN_SOLICITUDES)) {
            String tag = ChatColor.stripColor(it.getItemMeta().getDisplayName());
            if (e.isLeftClick()) {
                bancoManager.aprobarBanco(tag);
                p.sendMessage(ChatColor.GREEN + "Banco " + tag + " aprobado.");
            } else {
                bancoManager.rechazarBanco(tag);
                p.sendMessage(ChatColor.RED   + "Banco " + tag + " rechazado.");
            }
            p.closeInventory();
            return;
        }

        // 3.2) Lista activos
        if (title.startsWith(PLAIN_LISTA)) {
            String tag = ChatColor.stripColor(it.getItemMeta().getDisplayName());
            abrirIndividual(p, tag);
            return;
        }

        // 3.3) Vista individual

        if (title.startsWith(PLAIN_INDIVIDUAL)) {
            String tag = title.substring(PLAIN_INDIVIDUAL.length());
            ItemMeta meta = it.getItemMeta();
            if (it.getType() == Material.YELLOW_BUNDLE && meta.getDisplayName().contains("Ver solicitudes")) {
                plugin.getMenuCirculacionMonetaria().abrirMenu(p, tag);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent e) {
        String title = ChatColor.stripColor(e.getView().getTitle());
        if (isMiMenu(title)) {
            e.setCancelled(true);
        }
    }
    private boolean isMiMenu(String title) {
        return title.equals(PLAIN_SOLICITUDES)
                || title.startsWith(PLAIN_LISTA)
                || title.startsWith(PLAIN_INDIVIDUAL);
    }
}
