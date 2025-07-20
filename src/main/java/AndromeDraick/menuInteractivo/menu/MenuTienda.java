package AndromeDraick.menuInteractivo.menu;

import AndromeDraick.menuInteractivo.MenuInteractivo;
import AndromeDraick.menuInteractivo.configuracion.ConfigTiendaManager;
import AndromeDraick.menuInteractivo.utilidades.CalculadoraPrecios;
import AndromeDraick.menuInteractivo.utilidades.FormateadorNumeros;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuTienda {

    private static final String TITULO = ChatColor.DARK_GREEN + "Tienda Oficial";
    private static final Map<UUID, Integer> paginaPorJugador = new HashMap<>();
    private static final int ITEMS_POR_PAGINA = 36;
    private static final Map<UUID, String> categoriaPorJugador = new HashMap<>();

    private static final int[] slotsDeVenta = {
            18,19,20,21,22,23,24,25,26,
            27,28,29,30,31,32,33,34,35,
            36,37,38,39,40,41,42,43,44
    };

    private static final Map<String, String> nombresCategoriaES = Map.ofEntries(
            Map.entry("Building_Blocks", "Bloques de Construcción"),
            Map.entry("Colored_Blocks", "Bloques de Colores"),
            Map.entry("Natural_Blocks", "Bloques Naturales"),
            Map.entry("Functional_Blocks", "Bloques Funcionales"),
            Map.entry("Redstone_Blocks", "Bloques de Redstone"),
            Map.entry("Tools_&_Utilities", "Herramientas y Utilidades"),
            Map.entry("Combat", "Combate"),
            Map.entry("Food_&_Drinks", "Comidas"),
            Map.entry("Ingredients", "Ingredientes"),
            Map.entry("Spawn_Eggs", "Huevos de Spawn")
    );

    private static String obtenerNombreOriginalDesdeTraducido(String traducido) {
        return nombresCategoriaES.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(traducido))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(traducido.replace(" ", "_"));
    }

    public static void abrir(Player jugador, int pagina) {
        MenuInteractivo plugin     = MenuInteractivo.getInstancia();
        ConfigTiendaManager cfg    = plugin.getConfigTienda();
        var economia               = plugin.getEconomia();

        paginaPorJugador.put(jugador.getUniqueId(), pagina);

        String titulo = TITULO
                + " §7($ " + FormateadorNumeros.formatear(economia.getBalance(jugador)) + ")";
        Inventory tienda = Bukkit.createInventory(null, 54, titulo);

        // — Categorías (0–8) —
        int slotCat = 0;
        for (String clave : nombresCategoriaES.keySet()) {
            if (slotCat >= 9) break;
            ItemStack papel = new ItemStack(Material.PAPER);
            ItemMeta m = papel.getItemMeta();
            m.setDisplayName(ChatColor.AQUA + nombresCategoriaES.get(clave));
            m.setLore(List.of(ChatColor.GRAY + "Haz clic para filtrar categoría"));
            papel.setItemMeta(m);
            tienda.setItem(slotCat++, papel);
        }

        // — Mostrar todo —
        ItemStack mapa = new ItemStack(Material.MAP);
        ItemMeta mm = mapa.getItemMeta();
        mm.setDisplayName(ChatColor.YELLOW + "Mostrar todo");
        mm.setLore(List.of(ChatColor.GRAY + "Ver todos los ítems"));
        mapa.setItemMeta(mm);
        tienda.setItem(13, mapa);

        // — Filtro por categoría seleccionada —
        Set<String> claves = new HashSet<>(cfg.getItemsEnVenta());
        String catSel = categoriaPorJugador.getOrDefault(jugador.getUniqueId(), "");
        if (!catSel.isEmpty()) {
            claves.removeIf(itemKey -> {
                String cat = (String) cfg.getDatosItemVenta(itemKey).get("categoria");
                return cat == null || !cat.equalsIgnoreCase(catSel);
            });
        }

        // — Datos de grupo y trabajo —
        String grupo = "default";
        var lp = plugin.getPermisos();
        if (lp != null) {
            var user = lp.getUserManager().getUser(jugador.getUniqueId());
            if (user != null) grupo = user.getPrimaryGroup();
        }
        String trabajo = plugin.getSistemaTrabajos().getTrabajo(jugador.getUniqueId());

        // — Rarezas desbloqueadas y VIP check —
        List<String> rarezasVIP = cfg.getRarezasDesbloqueadas(grupo);
        boolean esVIP = !rarezasVIP.isEmpty();

        // — Filtrar por trabajo (solo no-VIP) y por rareza —
        claves.removeIf(itemKey -> {
            Map<String,Object> datos = cfg.getDatosItemVenta(itemKey);
            if (datos.isEmpty()) return true;

            if (!esVIP && datos.containsKey("trabajo")) {
                String[] permitidos = ((String) datos.get("trabajo")).split(",");
                boolean ok = Arrays.stream(permitidos)
                        .map(String::trim)
                        .anyMatch(t -> t.equalsIgnoreCase(trabajo));
                if (!ok) return true;
            }

            String rareza = ((String) datos.getOrDefault("rareza", "comun"))
                    .toLowerCase(Locale.ROOT);
            return !rareza.equals("comun") && !rarezasVIP.contains(rareza);
        });

        // — Paginación e inserción de ítems …
        List<String> lista = new ArrayList<>(claves);
        int inicio = pagina * ITEMS_POR_PAGINA;
        int fin    = Math.min(lista.size(), inicio + ITEMS_POR_PAGINA);
        int slot   = 0;
        for (int i = inicio; i < fin && slot < slotsDeVenta.length; i++) {
            String key = lista.get(i);
            Material mat;
            try { mat = Material.valueOf(key); }
            catch (IllegalArgumentException e) { continue; }

            double precio = CalculadoraPrecios.calcularPrecioCompra(mat, jugador);
            if (precio < 0) continue;

            ItemStack item = new ItemStack(mat);
            ItemMeta im = item.getItemMeta();

            im.setLore(List.of(
                    ChatColor.GRAY + "Precio: " + ChatColor.GREEN + "$" +
                            FormateadorNumeros.formatear(precio),
                    ChatColor.YELLOW + "Clic para comprar 1"
            ));
            item.setItemMeta(im);

            tienda.setItem(slotsDeVenta[slot++], item);
        }

        // ————— Navegación y botones finales … —————
        if (pagina > 0)    tienda.setItem(45, crearBoton(Material.ARROW, "§ePágina anterior"));
        if (fin < lista.size()) tienda.setItem(53, crearBoton(Material.ARROW, "§ePágina siguiente"));

        ItemStack volver = new ItemStack(Material.ARROW);
        ItemMeta mv = volver.getItemMeta();
        mv.setDisplayName(ChatColor.RED + "Volver al Menú Principal");
        mv.setLore(List.of(ChatColor.GRAY + "Haz clic para regresar"));
        volver.setItemMeta(mv);
        tienda.setItem(46, volver);

        ItemStack vender = new ItemStack(Material.GOLD_INGOT);
        ItemMeta mv2 = vender.getItemMeta();
        mv2.setDisplayName(ChatColor.RED + "Vender inventario");
        mv2.setLore(List.of(
                ChatColor.GRAY + "Haz clic para vender ítems válidos",
                ChatColor.GRAY + "Página " + (pagina+1) + "/" +
                        ((lista.size()-1)/ITEMS_POR_PAGINA + 1)
        ));
        vender.setItemMeta(mv2);
        tienda.setItem(49, vender);

        jugador.openInventory(tienda);
        jugador.playSound(jugador.getLocation(),
                Sound.BLOCK_CHEST_OPEN, 0.8f, 1.1f);
    }


    public static void manejarClick(InventoryClickEvent event) {
        MenuInteractivo plugin = MenuInteractivo.getInstancia();
        Player jugador = (Player) event.getWhoClicked();
        if (!event.getView().getTitle().startsWith(TITULO)) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        UUID uuid = jugador.getUniqueId();
        int pagina = paginaPorJugador.getOrDefault(uuid, 0);
        int slot = event.getRawSlot();

        if (slot >= 0 && slot <= 9 && item.hasItemMeta()) {
            String nombreTraducido = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            String nombreOriginal = obtenerNombreOriginalDesdeTraducido(nombreTraducido);
            categoriaPorJugador.put(uuid, nombreOriginal);
            abrir(jugador, 0);
            return;
        }

        if (slot == 9) {
            categoriaPorJugador.remove(uuid);
            abrir(jugador, 0);
            return;
        }

        if (slot == 45) {
            abrir(jugador, Math.max(0, pagina - 1));
            return;
        }

        if (slot == 46) {
            plugin.getMenuPrincipal().abrir(jugador);
            return;
        }

        if (slot == 53) {
            abrir(jugador, pagina + 1);
            return;
        }

        if (slot == 49) {
            venderInventario(jugador);
            return;
        }

        Material mat = item.getType();
        double precio = CalculadoraPrecios.calcularPrecioCompra(mat, jugador);
        if (precio < 0) return;

        if (!MenuInteractivo.getInstancia().getEconomia().has(jugador, precio)) {
            jugador.sendMessage(ChatColor.RED + "No tienes suficiente dinero.");
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.9f);
            return;
        }

        MenuConfirmacion.abrir(jugador, mat);
    }

    private static ItemStack crearBoton(Material material, String nombre) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nombre);
        item.setItemMeta(meta);
        return item;
    }

    private static void venderInventario(Player jugador) {
        var cfg      = MenuInteractivo.getInstancia().getConfigTienda();
        var economia = MenuInteractivo.getInstancia().getEconomia();
        var inventario = jugador.getInventory();

        // Determinar grupo y trabajo del jugador
        String grupo = "default";
        var lp = MenuInteractivo.getInstancia().getPermisos();
        if (lp != null) {
            var user = lp.getUserManager().getUser(jugador.getUniqueId());
            if (user != null) grupo = user.getPrimaryGroup();
        }
        String trabajo = MenuInteractivo.getInstancia()
                .getSistemaTrabajos()
                .getTrabajo(jugador.getUniqueId());

        // Lista de rarezas que desbloquea este grupo
        List<String> rarezasVIP = cfg.getRarezasDesbloqueadas(grupo);
        boolean esVIP = !rarezasVIP.isEmpty();

        double total = 0;
        int vendidos = 0;

        for (ItemStack item : inventario.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            String nombre = item.getType().name();

            // Solo ítems configurados en venta
            if (!cfg.getItemsEnVenta().contains(nombre)) continue;

            // Obtener datos del ítem
            Map<String,Object> datos = cfg.getDatosItemVenta(nombre);
            if (datos.isEmpty()) continue;

            // Si no es VIP, validar requisito de trabajo
            if (!esVIP && datos.containsKey("trabajo")) {
                String[] trabajosPermitidos = ((String) datos.get("trabajo")).split(",");
                boolean coincide = Arrays.stream(trabajosPermitidos)
                        .map(String::trim)
                        .anyMatch(t -> t.equalsIgnoreCase(trabajo));
                if (!coincide) continue;
            }

            // Validar rareza (común o desbloqueada para VIP)
            String rareza = ((String) datos.getOrDefault("rareza", "comun"))
                    .toLowerCase(Locale.ROOT);
            if (!rareza.equals("comun") && !rarezasVIP.contains(rareza)) continue;

            // Calcular precio de venta
            double precio = CalculadoraPrecios.calcularPrecioVenta(item.getType(), jugador);
            if (precio <= 0) continue;

            // Sumar al total y marcar como vendido
            total += precio * item.getAmount();
            vendidos += item.getAmount();
            inventario.remove(item);
        }

        if (vendidos == 0) {
            jugador.sendMessage(ChatColor.RED + "No tienes ítems válidos para vender.");
            jugador.playSound(jugador.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
            return;
        }

        economia.depositPlayer(jugador, total);
        jugador.sendMessage(ChatColor.GREEN + "Vendiste " + vendidos +
                " ítems por $" + FormateadorNumeros.formatear(total));
        jugador.playSound(jugador.getLocation(),
                Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }


    public static int getPagina(Player jugador) {
        return paginaPorJugador.getOrDefault(jugador.getUniqueId(), 0);
    }
}
