package AndromeDraick.menuInteractivo.managers;

import AndromeDraick.menuInteractivo.database.GestorBaseDeDatos;
import AndromeDraick.menuInteractivo.model.Banco;

import java.util.List;
import java.util.UUID;

public class BancoManager {

    private final GestorBaseDeDatos db;

    public BancoManager(GestorBaseDeDatos db) {
        this.db = db;
    }

    public boolean crearBanco(Banco banco) {
        return db.insertarBanco(banco);
    }

    public boolean actualizarFondos(String etiquetaBanco, double cantidad, boolean depositar) {
        return db.actualizarFondosBanco(etiquetaBanco, cantidad, depositar);
    }

    public boolean cambiarEstado(String etiquetaBanco, String nuevoEstado) {
        return db.actualizarEstadoBanco(etiquetaBanco, nuevoEstado);
    }

    public Banco obtenerBanco(String etiqueta) {
        return db.obtenerBancoPorEtiqueta(etiqueta);
    }

    public List<Banco> obtenerBancosDeReino(String etiquetaReino) {
        return db.obtenerBancosDeReino(etiquetaReino);
    }

    public List<Banco> obtenerBancosPendientes(String etiquetaReino) {
        return db.obtenerBancosPendientes(etiquetaReino);
    }
}