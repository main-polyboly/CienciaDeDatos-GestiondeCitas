public class Factura {

    public String idFactura;
    public String idCita;
    public String usernamePaciente;
    public String nombrePaciente;
    public String medicoUsername;
    public String especialidad;
    public String fechaCita;
    public double subtotal;
    public double iva15;
    public double total;
    public String metodoPago;
    public String estadoPago;
    public String fechaEmision;

    // Tarifa base por especialidad (sin IVA)
    public static final java.util.Map<String, Double> TARIFAS;
    static {
        TARIFAS = new java.util.LinkedHashMap<>();
        TARIFAS.put("Medicina General",   25.00);
        TARIFAS.put("Cardiología",        80.00);
        TARIFAS.put("Pediatría",          40.00);
        TARIFAS.put("Ginecología",        60.00);
        TARIFAS.put("Traumatología",      70.00);
        TARIFAS.put("Neurología",         90.00);
        TARIFAS.put("Dermatología",       55.00);
        TARIFAS.put("Oftalmología",       65.00);
        TARIFAS.put("Psiquiatría",        85.00);
        TARIFAS.put("Endocrinología",     75.00);
        TARIFAS.put("Gastroenterología",  70.00);
        TARIFAS.put("Urología",           65.00);
        TARIFAS.put("Oncología",         120.00);
        TARIFAS.put("Neumología",         70.00);
        TARIFAS.put("Reumatología",       75.00);
    }

    public static final double TASA_IVA = 0.15;

    public Factura(String idFactura, String idCita,
                   String usernamePaciente, String nombrePaciente,
                   String medicoUsername, String especialidad,
                   String fechaCita, double subtotal,
                   String metodoPago, String estadoPago, String fechaEmision) {
        this.idFactura        = idFactura;
        this.idCita           = idCita;
        this.usernamePaciente = usernamePaciente;
        this.nombrePaciente   = nombrePaciente;
        this.medicoUsername   = medicoUsername;
        this.especialidad     = especialidad;
        this.fechaCita        = fechaCita;
        this.subtotal         = redondear(subtotal);
        this.iva15            = redondear(subtotal * TASA_IVA);
        this.total            = redondear(this.subtotal + this.iva15);
        this.metodoPago       = metodoPago;
        this.estadoPago       = estadoPago;
        this.fechaEmision     = fechaEmision;
    }

    /** Obtiene la tarifa base de la especialidad (sin IVA) */
    public static double tarifaPor(String especialidad) {
        return TARIFAS.getOrDefault(especialidad, 50.00);
    }

    public static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Imprime comprobante en consola */
    public void imprimirComprobante() {
        System.out.println("\n  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║          COMPROBANTE DE PAGO                 ║");
        System.out.println("  ╠══════════════════════════════════════════════╣");
        System.out.printf ("  ║  Factura   : %-31s║%n", idFactura);
        System.out.printf ("  ║  Cita      : %-31s║%n", idCita);
        System.out.printf ("  ║  Paciente  : %-31s║%n", ab(nombrePaciente, 31));
        System.out.printf ("  ║  Medico    : %-31s║%n", ab(medicoUsername, 31));
        System.out.printf ("  ║  Especialid: %-31s║%n", ab(especialidad, 31));
        System.out.printf ("  ║  Fecha     : %-31s║%n", fechaCita);
        System.out.println("  ╠══════════════════════════════════════════════╣");
        System.out.printf ("  ║  Subtotal  : $%-30.2f║%n", subtotal);
        System.out.printf ("  ║  IVA 15%%   : $%-30.2f║%n", iva15);
        System.out.printf ("  ║  TOTAL     : $%-30.2f║%n", total);
        System.out.println("  ╠══════════════════════════════════════════════╣");
        System.out.printf ("  ║  Metodo    : %-31s║%n", metodoPago);
        System.out.printf ("  ║  Estado    : %-31s║%n", estadoPago);
        System.out.printf ("  ║  Emision   : %-31s║%n", fechaEmision);
        System.out.println("  ╚══════════════════════════════════════════════╝");
    }

    private String ab(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }
}