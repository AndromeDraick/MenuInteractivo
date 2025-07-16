package AndromeDraick.menuInteractivo.model;

import java.util.UUID;

public class Reino {
    private final String etiqueta;
    private final String nombre;
    private final UUID reyUUID;

    public Reino(String etiqueta, String nombre, UUID reyUUID) {
        this.etiqueta = etiqueta;
        this.nombre = nombre;
        this.reyUUID = reyUUID;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getNombre() { return nombre; }
    public UUID getReyUUID() { return reyUUID; }
}
