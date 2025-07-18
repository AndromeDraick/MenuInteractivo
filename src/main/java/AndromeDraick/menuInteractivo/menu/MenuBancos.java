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

    // Constantes “plain” (sin color) para comparar
    private static final String PLAIN_SOLICITUDES   = "Solicitudes de Bancos";
    private static final String PLAIN_LISTA         = "Bancos del Reino ";
    private static final String PLAIN_INDIVIDUAL    = "Banco: ";

    // Constantes coloreadas para mostrar en el título
    private static final String TITULO_SOLICITUDES  = ChatColor.DARK_GREEN + PLAIN_SOLICITUDES;
    private static final String TITULO_LISTA        = ChatColor.DARK_AQUA  + PLAIN_LISTA;
    private static final String TITULO_INDIVIDUAL   = ChatColor.GOLD       + PLAIN_INDIVIDUAL;

    private final MenuInteractivo plugin;
    private final BancoManager bancoManager;
    private final Economy economia;

    public MenuBancos(MenuInteractivo plugin) {
        this.plugin       = plugin;
        this.bancoManager = new BancoManager(plugin.getBaseDeDatos());
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
    private void abrirIndividual(Player p, String etiqueta) {
        String propia = bancoManager.obtenerBancoDeJugador(p.getUniqueId());
        if (!etiqueta.equals(propia)) {
            p.sendMessage(ChatColor.RED + "No estás vinculado a ese banco.");
            return;
        }
        Banco b = bancoManager.obtenerBanco(etiqueta);
        Inventory inv = Bukkit.createInventory(null, 27, TITULO_INDIVIDUAL + etiqueta);

        // Saldo
        ItemStack saldo = new ItemStack(Material.SUNFLOWER);
        ItemMeta ms = saldo.getItemMeta();
        ms.setDisplayName(ChatColor.YELLOW + "Saldo");
        ms.setLore(List.of(ChatColor.GREEN + "$" + b.getFondos()));
        saldo.setItemMeta(ms);
        inv.setItem(11, saldo);

        // Retirar $100
        ItemStack ret = new ItemStack(Material.REDSTONE);
        ItemMeta mr = ret.getItemMeta();
        mr.setDisplayName(ChatColor.RED + "Retirar $100");
        ret.setItemMeta(mr);
        inv.setItem(13, ret);

        // Ingresar $100
        ItemStack ing = new ItemStack(Material.EMERALD);
        ItemMeta mi = ing.getItemMeta();
        mi.setDisplayName(ChatColor.GREEN + "Ingresar $100");
        ing.setItemMeta(mi);
        inv.setItem(15, ing);

        p.openInventory(inv);
    }
    // === EVENT HANDLERS ===

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
            String tag    = title.substring(PLAIN_INDIVIDUAL.length());
            double fondos = bancoManager.obtenerSaldo(tag);

            if (it.getType() == Material.REDSTONE) {
                // retirar
                if (fondos >= 100 && bancoManager.retirar(tag, 100)) {
                    economia.depositPlayer(p, 100);
                    p.sendMessage(ChatColor.GREEN + "Retiraste $100 de " + tag);
                } else {
                    p.sendMessage(ChatColor.RED + "No hay fondos suficientes en el banco.");
                }
            } else if (it.getType() == Material.EMERALD) {
                // ingresar
                if (economia.getBalance(p) >= 100 && bancoManager.depositar(tag, 100)) {
                    economia.withdrawPlayer(p, 100);
                    p.sendMessage(ChatColor.GREEN + "Ingresaste $100 al banco " + tag);
                } else {
                    p.sendMessage(ChatColor.RED + "No tienes $100 para ingresar.");
                }
            }
            p.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent e) {
        String title = ChatColor.stripColor(e.getView().getTitle());
        if (isMiMenu(title)) {
            e.setCancelled(true);
        }
    }

    // Helper para detectar cualquier menú propio
    private boolean isMiMenu(String title) {
        return title.equals(PLAIN_SOLICITUDES)
                || title.startsWith(PLAIN_LISTA)
                || title.startsWith(PLAIN_INDIVIDUAL);
    }
}
