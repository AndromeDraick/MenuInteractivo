package AndromeDraick.menuInteractivo.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Reino {
    private final String etiqueta;
    private final String nombre;
    private final String descripcion;
    private final String moneda;
    private final UUID reyUUID;
    private final LocalDateTime fechaCreacion;

    /**
     * Representa un reino con todos sus datos básicos.
     *
     * @param etiqueta      Código único (etiqueta) del reino
     * @param nombre        Nombre completo del reino
     * @param descripcion   Descripción breve del reino
     * @param moneda        Nombre de la moneda del reino
     * @param reyUUID       UUID del jugador que es Rey/Reina
     * @param fechaCreacion Fecha y hora de creación del reino
     */
    public Reino(String etiqueta,
                 String nombre,
                 String descripcion,
                 String moneda,
                 UUID reyUUID,
                 LocalDateTime fechaCreacion) {
        this.etiqueta      = etiqueta;
        this.nombre        = nombre;
        this.descripcion   = descripcion;
        this.moneda        = moneda;
        this.reyUUID       = reyUUID;
        this.fechaCreacion = fechaCreacion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getMoneda() {
        return moneda;
    }

    public UUID getReyUUID() {
        return reyUUID;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    @Override
    public String toString() {
        return "Reino{" +
                "etiqueta='" + etiqueta + '\'' +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", moneda='" + moneda + '\'' +
                ", reyUUID=" + reyUUID +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }
}
