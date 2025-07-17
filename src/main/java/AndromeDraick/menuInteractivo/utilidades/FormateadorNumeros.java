package AndromeDraick.menuInteractivo.utilidades;

import java.text.NumberFormat;
import java.util.Locale;

public class FormateadorNumeros {
    public static String formatear(double numero) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.US);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(numero);
    }
}

