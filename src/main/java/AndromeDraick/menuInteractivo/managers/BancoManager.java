package AndromeDraick.menuInteractivo.managers;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import java.util.UUID;

/**
 * Encapsula operaciones de banco usando GestorBaseDeDatos.
 */
public class BancoManager {

    private final GestorBaseDeDatos db;

    public BancoManager(GestorBaseDeDatos db) {
        this.db = db;
    }

    /**
     * Crea un nuevo banco.
     * Nota: debes añadir en GestorBaseDeDatos un método:
     * public boolean insertarBanco(String etiqueta, String nombre, String reinoEtiqueta, UUID propietario)
     */
    public boolean crearBanco(String etiqueta, String nombre, String reinoEtiqueta, UUID propietario) {
        return db.insertarBanco(etiqueta, nombre, reinoEtiqueta, propietario);
    }

    /**
     * Deposita monto en el banco.
     * (GestorBaseDeDatos#depositarEnBanco ya existe) :contentReference[oaicite:0]{index=0}
     */
    public boolean depositar(String etiquetaBanco, double monto) {
        return db.depositarEnBanco(etiquetaBanco, monto);
    }

    /**
     * Retira monto del banco, si hay fondos suficientes.
     * (GestorBaseDeDatos#retirarDeBanco ya existe) :contentReference[oaicite:1]{index=1}
     */
    public boolean retirar(String etiquetaBanco, double monto) {
        return db.retirarDeBanco(etiquetaBanco, monto);
    }

    /**
     * Devuelve el saldo actual del banco.
     * (GestorBaseDeDatos#obtenerSaldoBanco ya existe) :contentReference[oaicite:2]{index=2}
     */
    public double obtenerSaldo(String etiquetaBanco) {
        return db.obtenerSaldoBanco(etiquetaBanco);
    }

    /**
     * Añade un socio al banco.
     * (GestorBaseDeDatos#agregarSocioBanco ya existe) :contentReference[oaicite:3]{index=3}
     */
    public boolean agregarSocio(String etiquetaBanco, UUID jugadorUUID) {
        return db.agregarSocioBanco(etiquetaBanco, jugadorUUID);
    }

    /**
     * Quita un socio del banco.
     * (GestorBaseDeDatos#quitarSocioBanco ya existe) :contentReference[oaicite:4]{index=4}
     */
    public boolean quitarSocio(String etiquetaBanco, UUID jugadorUUID) {
        return db.quitarSocioBanco(etiquetaBanco, jugadorUUID);
    }

    // Si en el futuro añades estos métodos en GestorBaseDeDatos, podrías reactivar:
    // public Banco obtenerBanco(String etiqueta) { … }
    // public List<Banco> obtenerBancosDeReino(String etiquetaReino) { … }
    // public List<Banco> obtenerBancosPendientes(String etiquetaReino) { … }
}
