//package AndromeDraick.menuInteractivo.menu;
//
//import AndromeDraick.menuInteractivo.MenuInteractivo;
//import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
//import net.wesjd.anvilgui.AnvilGUI;
//import org.bukkit.ChatColor;
//import org.bukkit.entity.Player;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//public class MenuDatosRol {
//
//    private static final Map<UUID, RolTemporal> datosPendientes = new HashMap<>();
//
//    public static void abrir(Player jugador, String genero) {
//        datosPendientes.put(jugador.getUniqueId(), new RolTemporal(genero));
//        solicitarNombre(jugador);
//    }
//
//    private static void solicitarNombre(Player jugador) {
//        new AnvilGUI.Builder()
//                .plugin(MenuInteractivo.getInstancia())
//                .title("Escribe tu nombre de rol")
//                .text("Ej: Luis")
//                .conComplete(completion -> {
//                    Player p = completion.getPlayer();
//                    String texto = completion.getText();
//                    datosPendientes.get(p.getUniqueId()).nombre = texto;
//                    solicitarApellidoPaterno(p);
//                    return AnvilGUI.Response.close();
//                })
//                .open(jugador);
//    }
//
//    private static void solicitarApellidoPaterno(Player jugador) {
//        new AnvilGUI.Builder()
//                .plugin(MenuInteractivo.getInstancia())
//                .title("Apellido paterno")
//                .text("Ej: Torres")
//                .onComplete((p, texto) -> {
//                    datosPendientes.get(p.getUniqueId()).apellidoPaterno = texto;
//                    solicitarApellidoMaterno(p);
//                    return AnvilGUI.Response.close();
//                }).open(jugador);
//    }
//
//    private static void solicitarApellidoMaterno(Player jugador) {
//        new AnvilGUI.Builder()
//                .plugin(MenuInteractivo.getInstancia())
//                .title("Apellido materno")
//                .text("Ej: Rodríguez")
//                .onComplete((p, texto) -> {
//                    datosPendientes.get(p.getUniqueId()).apellidoMaterno = texto;
//                    solicitarDescendencia(p);
//                    return AnvilGUI.Response.close();
//                }).open(jugador);
//    }
//
//    private static void solicitarDescendencia(Player jugador) {
//        new AnvilGUI.Builder()
//                .plugin(MenuInteractivo.getInstancia())
//                .title("¿Descendiente de quién?")
//                .text("Ej: familia real")
//                .onComplete((p, texto) -> {
//                    datosPendientes.get(p.getUniqueId()).descendencia = texto;
//                    solicitarRaza(p);
//                    return AnvilGUI.Response.close();
//                }).open(jugador);
//    }
//
//    private static void solicitarRaza(Player jugador) {
//        new AnvilGUI.Builder()
//                .plugin(MenuInteractivo.getInstancia())
//                .title("¿Cuál es tu raza?")
//                .text("Ej: elfo")
//                .onComplete((p, texto) -> {
//                    RolTemporal rol = datosPendientes.remove(p.getUniqueId());
//                    rol.raza = texto;
//
//                    GestorBaseDeDatos db = MenuInteractivo.getInstancia().getBaseDeDatos();
//                    boolean exito = db.registrarRolCompleto(p.getUniqueId(), rol.genero, rol.nombre, rol.apellidoPaterno, rol.apellidoMaterno, rol.descendencia, rol.raza);
//
//                    if (exito) {
//                        p.sendMessage(ChatColor.GREEN + "¡Registro completado! Puedes ver tu ficha con /rmi ficha");
//                    } else {
//                        p.sendMessage(ChatColor.RED + "Error al registrar tu ficha.");
//                    }
//
//                    return AnvilGUI.Response.close();
//                }).open(jugador);
//    }
//
//    private static class RolTemporal {
//        String genero, nombre, apellidoPaterno, apellidoMaterno, descendencia, raza;
//        public RolTemporal(String genero) {
//            this.genero = genero;
//        }
//    }
//}
