import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Validaciones {

    /** Solo letras (con tildes y ñ), espacios, 2-40 chars */
    public static boolean validarNombre(String nombre) {
        if (nombre == null) return false;
        String s = nombre.trim();
        return s.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]{2,40}") && !s.isEmpty();
    }

    /** Solo letras, 3-20 chars, sin espacios */
    public static boolean validarUsername(String user) {
        return user != null && user.trim().matches("[a-zA-Z]{3,20}");
    }

    /** 8-20 chars, ≥1 mayúscula, ≥1 minúscula, ≥1 dígito */
    public static boolean validarPassword(String pass) {
        if (pass == null) return false;
        int len = pass.length();
        if (len < 8 || len > 20) return false;
        return pass.matches(".*[A-Z].*") && pass.matches(".*[a-z].*") && pass.matches(".*\\d.*");
    }

    /**
     * Cédula ecuatoriana:
     *   - 10 dígitos
     *   - Provincia 01-24
     *   - Tercer dígito < 6
     *   - Algoritmo módulo 10
     */
    public static boolean validarCedula(String cedula) {
        if (cedula == null || !cedula.matches("\\d{10}")) return false;
        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if (provincia < 1 || provincia > 24) return false;
        if (Character.getNumericValue(cedula.charAt(2)) >= 6) return false;
        int[] coef = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int val = Character.getNumericValue(cedula.charAt(i)) * coef[i];
            if (val >= 10) val -= 9;
            suma += val;
        }
        int verificador = (10 - (suma % 10)) % 10;
        return verificador == Character.getNumericValue(cedula.charAt(9));
    }

    /** Exactamente 10 dígitos numéricos */
    public static boolean validarTelefono(String tel) {
        return tel != null && tel.matches("\\d{10}");
    }

    /** Formato correo electrónico básico */
    public static boolean validarCorreo(String correo) {
        return correo != null &&
                correo.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }

    /** Fecha YYYY-MM-DD que no sea anterior a hoy (para citas) */
    public static boolean validarFechaFutura(String fecha) {
        try {
            LocalDate f = LocalDate.parse(fecha, DateTimeFormatter.ISO_LOCAL_DATE);
            return !f.isBefore(LocalDate.now());
        } catch (DateTimeParseException e) { return false; }
    }

    /** Fecha de nacimiento: entre 1900 y hoy */
    public static boolean validarFechaNacimiento(String fecha) {
        try {
            LocalDate f = LocalDate.parse(fecha, DateTimeFormatter.ISO_LOCAL_DATE);
            return !f.isAfter(LocalDate.now()) && f.getYear() >= 1900;
        } catch (DateTimeParseException e) { return false; }
    }

    /** Valida método de pago */
    public static boolean validarMetodoPago(String metodo) {
        if (metodo == null) return false;
        for (String m : new String[]{"Efectivo","Tarjeta de Credito","Tarjeta de Debito","Transferencia","Seguro Medico"})
            if (m.equalsIgnoreCase(metodo.trim())) return true;
        return false;
    }

    /** Valida que un valor double sea positivo */
    public static boolean esPositivo(double v) {
        return v > 0;
    }
}
