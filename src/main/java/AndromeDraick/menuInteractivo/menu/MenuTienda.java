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
            Map.entry("Food_&_Drinks", "Comida y Bebidas"),
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
        ConfigTiendaManager config = plugin.getConfigTienda();
        var economia               = plugin.getEconomia();

        // Guardar página actual
        paginaPorJugador.put(jugador.getUniqueId(), pagina);

        // Título con saldo
        String titulo = TITULO
                + " §7($ " + FormateadorNumeros.formatear(economia.getBalance(jugador)) + ")";
        Inventory tienda = Bukkit.createInventory(null, 54, titulo);

        // ————— Categorías —————
        int slotCat = 0;
        for (String clave : nombresCategoriaES.keySet()) {
            if (slotCat >= 9) break;
            ItemStack papel = new ItemStack(Material.PAPER);
            ItemMeta meta = papel.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + nombresCategoriaES.get(clave));
            meta.setLore(List.of(ChatColor.GRAY + "Haz clic para filtrar por categoría"));
            papel.setItemMeta(meta);
            tienda.setItem(slotCat++, papel);
        }

        // ————— Mostrar todo —————
        ItemStack mapa = new ItemStack(Material.MAP);
        ItemMeta metaMapa = mapa.getItemMeta();
        metaMapa.setDisplayName(ChatColor.YELLOW + "Mostrar todo");
        metaMapa.setLore(List.of(ChatColor.GRAY + "Haz clic para ver todos los ítems"));
        mapa.setItemMeta(metaMapa);
        tienda.setItem(13, mapa);

        // ————— Filtrar por categoría si hay —————
        Set<String> claves = new HashSet<>(config.getItemsCustom());
        String categoria = categoriaPorJugador.getOrDefault(jugador.getUniqueId(), "");
        if (!categoria.isEmpty()) {
            claves.removeIf(clave -> {
                String cat = (String) config.getDatosItemCustom(clave).get("categoria");
                return cat == null || !cat.equalsIgnoreCase(categoria);
            });
        }

        // ————— Datos de grupo y trabajo —————
        String grupo   = "default";
        var lp         = plugin.getPermisos();
        if (lp != null) {
            var user = lp.getUserManager().getUser(jugador.getUniqueId());
            if (user != null) grupo = user.getPrimaryGroup();
        }
        String trabajo = plugin.getSistemaTrabajos().getTrabajo(jugador.getUniqueId());

        // ————— Rarezas desbloqueadas por grupo —————
        List<String> rarezasDesbloqueadas = config.getRarezasDesbloqueadas(grupo);
        boolean esGrupoVIP = !rarezasDesbloqueadas.isEmpty();

        // ————— Filtrar ítems por trabajo (solo no-VIP) y por rareza —————
        claves.removeIf(clave -> {
            Map<String,Object> datos = config.getDatosItemCustom(clave);
            if (datos == null || datos.isEmpty()) return true;

            // Si no es VIP, aplica requisito de trabajo
            if (!esGrupoVIP && datos.containsKey("trabajo")) {
                String[] permitidos = ((String) datos.get("trabajo")).split(",");
                boolean tieneAcceso = Arrays.stream(permitidos)
                        .map(String::trim)
                        .anyMatch(t -> t.equalsIgnoreCase(trabajo));
                if (!tieneAcceso) return true;
            }

            // Filtrar por rareza
            String rareza = ((String) datos.getOrDefault("rareza", "comun"))
                    .toLowerCase(Locale.ROOT);
            return !rareza.equals("comun") && !rarezasDesbloqueadas.contains(rareza);
        });

        // ————— Resto de la paginación y colocación de ítems … —————
        List<String> listaFinal = new ArrayList<>(claves);
        int inicio = pagina * ITEMS_POR_PAGINA;
        int fin    = Math.min(listaFinal.size(), inicio + ITEMS_POR_PAGINA);
        int slot   = 0;
        for (int i = inicio; i < fin && slot < slotsDeVenta.length; i++) {
            String clave = listaFinal.get(i);
            Material mat;
            try { mat = Material.valueOf(clave); }
            catch (IllegalArgumentException e) { continue; }

            double precio = CalculadoraPrecios.calcularPrecioCompra(mat, jugador);
            if (precio < 0) continue;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            Map<String,Object> datos = config.getDatosItemCustom(clave);
            String nombreTrad = datos.containsKey("material")
                    ? String.valueOf(datos.get("material"))
                    : clave.replace("_", " ");
            meta.setDisplayName(ChatColor.GOLD + nombreTrad);
            meta.setLore(List.of(
                    ChatColor.GRAY + "Precio: " + ChatColor.GREEN + "$" +
                            FormateadorNumeros.formatear(precio),
                    ChatColor.YELLOW + "Haz clic para comprar 1 unidad"
            ));
            item.setItemMeta(meta);
            tienda.setItem(slotsDeVenta[slot++], item);
        }

        // ————— Navegación y botones finales … —————
        if (pagina > 0)    tienda.setItem(45, crearBoton(Material.ARROW, "§ePágina anterior"));
        if (fin < listaFinal.size()) tienda.setItem(53, crearBoton(Material.ARROW, "§ePágina siguiente"));

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
                        ((listaFinal.size()-1)/ITEMS_POR_PAGINA + 1)
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
        var configTienda = MenuInteractivo.getInstancia().getConfigTienda();
        var economia = MenuInteractivo.getInstancia().getEconomia();
        var inventario = jugador.getInventory();

        double total = 0;
        int vendidos = 0;

        for (ItemStack item : inventario.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            String nombre = item.getType().name();
            if (!configTienda.getItemsCustom().contains(nombre)) continue;

            double precio = CalculadoraPrecios.calcularPrecioVenta(item.getType(), jugador);
            if (precio <= 0) continue;

            total += precio * item.getAmount();
            vendidos += item.getAmount();
            inventario.remove(item);
        }

        if (vendidos == 0) {
            jugador.sendMessage(ChatColor.RED + "No tienes ítems válidos para vender.");
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
            return;
        }

        economia.depositPlayer(jugador, total);
        jugador.sendMessage(ChatColor.GREEN + "Vendiste " + vendidos + " ítems por $" + FormateadorNumeros.formatear(total));
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    public static int getPagina(Player jugador) {
        return paginaPorJugador.getOrDefault(jugador.getUniqueId(), 0);
    }
}
