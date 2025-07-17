package AndromeDraick.menuInteractivo.model;

import java.util.UUID;

public class Banco {
    private final String etiqueta;
    private final String nombre;
    private final String reinoEtiqueta;
    private final UUID propietarioUuid;
    private final String estado;
    private final double fondos;

    public Banco(String etiqueta, String nombre, String reinoEtiqueta, UUID propietarioUuid, String estado, double fondos) {
        this.etiqueta = etiqueta;
        this.nombre = nombre;
        this.reinoEtiqueta = reinoEtiqueta;
        this.propietarioUuid = propietarioUuid;
        this.estado = estado;
        this.fondos = fondos;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getNombre() {
        return nombre;
    }

    public String getReinoEtiqueta() {
        return reinoEtiqueta;
    }

    public UUID getPropietarioUuid() {
        return propietarioUuid;
    }

    public String getEstado() {
        return estado;
    }

    public double getFondos() {
        return fondos;
    }

    @Override
    public String toString() {
        return "Banco{" +
                "etiqueta='" + etiqueta + '\'' +
                ", nombre='" + nombre + '\'' +
                ", reinoEtiqueta='" + reinoEtiqueta + '\'' +
                ", propietarioUuid=" + propietarioUuid +
                ", estado='" + estado + '\'' +
                ", fondos=" + fondos +
                '}';
    }
}