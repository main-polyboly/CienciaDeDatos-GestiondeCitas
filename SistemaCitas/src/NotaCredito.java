/**
 * NotaCredito — documento que anula total o parcialmente una Factura.
 * Se genera cuando una cita facturada es cancelada o hay error.
 *
 * Código: NC-XXXXX
 */
public class NotaCredito {

    public String idNotaCredito;   // NC-00001
    public String idFactura;       // factura que se anula
    public String idCita;
    public String usernamePaciente;
    public String nombrePaciente;
    public String motivo;          // Cancelación de cita | Error de facturación | Otro
    public double montoAnulado;    // igual al total de la factura
    public String fechaEmision;
    public String emitidoPor;      // username del admin que la generó
    public String estado;          // Emitida | Aplicada

    public NotaCredito(String idNotaCredito, String idFactura, String idCita,
                       String usernamePaciente, String nombrePaciente,
                       String motivo, double montoAnulado,
                       String fechaEmision, String emitidoPor) {
        this.idNotaCredito   = idNotaCredito;
        this.idFactura       = idFactura;
        this.idCita          = idCita;
        this.usernamePaciente= usernamePaciente;
        this.nombrePaciente  = nombrePaciente;
        this.motivo          = motivo;
        this.montoAnulado    = montoAnulado;
        this.fechaEmision    = fechaEmision;
        this.emitidoPor      = emitidoPor;
        this.estado          = "Emitida";
    }

    public void imprimir() {
        System.out.println("\n  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║              NOTA DE CRÉDITO                 ║");
        System.out.println("  ╠══════════════════════════════════════════════╣");
        System.out.printf ("  ║  Nota Crédito : %-29s║%n", idNotaCredito);
        System.out.printf ("  ║  Factura orig.: %-29s║%n", idFactura);
        System.out.printf ("  ║  Cita         : %-29s║%n", idCita);
        System.out.printf ("  ║  Paciente     : %-29s║%n", ab(nombrePaciente,29));
        System.out.printf ("  ║  Motivo       : %-29s║%n", ab(motivo,29));
        System.out.printf ("  ║  Monto anulado: $%-28.2f║%n", montoAnulado);
        System.out.printf ("  ║  Fecha        : %-29s║%n", fechaEmision);
        System.out.printf ("  ║  Emitido por  : %-29s║%n", emitidoPor);
        System.out.printf ("  ║  Estado       : %-29s║%n", estado);
        System.out.println("  ╚══════════════════════════════════════════════╝");
    }

    private String ab(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max-1)+"." : s;
    }
}
