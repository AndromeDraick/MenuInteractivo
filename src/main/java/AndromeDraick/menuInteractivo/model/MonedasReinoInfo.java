package AndromeDraick.menuInteractivo.model;

public class MonedasReinoInfo {
    private final String etiquetaReino;
    private final String nombreMoneda;
    private final double cantidadImpresa;
    private final double cantidadQuemada;
    private final double dineroConvertido;
    private final String fechaCreacion;

    public MonedasReinoInfo(String etiquetaReino, String nombreMoneda,
                            double cantidadImpresa, double cantidadQuemada,
                            double dineroConvertido, String fechaCreacion) {
        this.etiquetaReino = etiquetaReino;
        this.nombreMoneda = nombreMoneda;
        this.cantidadImpresa = cantidadImpresa;
        this.cantidadQuemada = cantidadQuemada;
        this.dineroConvertido = dineroConvertido;
        this.fechaCreacion = fechaCreacion;
    }

    public String getEtiquetaReino() {
        return etiquetaReino;
    }

    public String getNombreMoneda() {
        return nombreMoneda;
    }

    public double getCantidadImpresa() {
        return cantidadImpresa;
    }

    public double getCantidadQuemada() {
        return cantidadQuemada;
    }

    public double getDineroConvertido() {
        return dineroConvertido;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }
}
