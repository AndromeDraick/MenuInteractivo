package AndromeDraick.menuInteractivo.model;

import java.util.UUID;

public class SolicitudMoneda {
    private final int id;
    private final UUID uuidJugador;
    private final String etiquetaBanco;
    private final double cantidad;
    private final String estado;
    private final String fecha;

    public SolicitudMoneda(int id, UUID uuidJugador, String etiquetaBanco, double cantidad, String estado, String fecha) {
        this.id = id;
        this.uuidJugador = uuidJugador;
        this.etiquetaBanco = etiquetaBanco;
        this.cantidad = cantidad;
        this.estado = estado;
        this.fecha = fecha;
    }

    public int getId() {
        return id;
    }

    public UUID getUuidJugador() {
        return uuidJugador;
    }

    public String getEtiquetaBanco() {
        return etiquetaBanco;
    }

    public double getCantidad() {
        return cantidad;
    }

    public String getEstado() {
        return estado;
    }

    public String getFecha() {
        return fecha;
    }
}
