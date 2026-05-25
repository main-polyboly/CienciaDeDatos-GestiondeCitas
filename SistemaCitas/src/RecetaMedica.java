import java.util.ArrayList;
import java.util.List;

/**
 * RecetaMedica — prescripción médica emitida por un médico
 * tras una consulta, ligada a una cita e historial.
 *
 * Cada receta tiene 1..N líneas de medicamento (ItemReceta).
 * Código único: REC-YYMMDD-XXXXX
 */
public class RecetaMedica {

    // ─── Línea de medicamento ────────────────────────────────────
    public static class ItemReceta {
        public String medicamento;    // nombre genérico o comercial
        public String dosis;          // "500 mg", "10 mg/ml"
        public String via;            // Oral | Tópica | Intravenosa | Inhalatoria | Sublingual | Intramuscular
        public String frecuencia;     // "cada 8 horas", "una vez al día"
        public String duracion;       // "7 días", "30 días", "indefinido"
        public String instrucciones;  // "tomar con alimentos", "no conducir"

        public ItemReceta(String medicamento, String dosis, String via,
                          String frecuencia, String duracion, String instrucciones) {
            this.medicamento  = medicamento;
            this.dosis        = dosis;
            this.via          = via;
            this.frecuencia   = frecuencia;
            this.duracion     = duracion;
            this.instrucciones= instrucciones;
        }
    }

    // ─── Campos principales ──────────────────────────────────────
    public String codigoReceta;       // REC-YYMMDD-XXXXX
    public String idCita;             // cita que originó la receta
    public String codigoHistorial;    // historial del paciente
    public String usernamePaciente;
    public String nombrePaciente;
    public String cedulaPaciente;
    public String medicoUsername;
    public String medicoNombre;
    public String especialidad;
    public String fechaEmision;
    public String fechaVencimiento;   // emision + 30 días por defecto
    public String estado;             // Activa | Dispensada | Vencida | Anulada
    public String diagnostico;        // diagnóstico que justifica la receta
    public String observaciones;
    public List<ItemReceta> items;

    public static final String[] VIAS = {
        "Oral", "Tópica", "Intravenosa", "Inhalatoria",
        "Sublingual", "Intramuscular", "Subcutánea", "Oftálmica"
    };

    public RecetaMedica(String codigoReceta, String idCita, String codigoHistorial,
                        String usernamePaciente, String nombrePaciente, String cedulaPaciente,
                        String medicoUsername, String medicoNombre, String especialidad,
                        String fechaEmision, String fechaVencimiento,
                        String estado, String diagnostico, String observaciones) {
        this.codigoReceta    = codigoReceta;
        this.idCita          = idCita;
        this.codigoHistorial = codigoHistorial;
        this.usernamePaciente= usernamePaciente;
        this.nombrePaciente  = nombrePaciente;
        this.cedulaPaciente  = cedulaPaciente;
        this.medicoUsername  = medicoUsername;
        this.medicoNombre    = medicoNombre;
        this.especialidad    = especialidad;
        this.fechaEmision    = fechaEmision;
        this.fechaVencimiento= fechaVencimiento;
        this.estado          = estado;
        this.diagnostico     = diagnostico;
        this.observaciones   = observaciones;
        this.items           = new ArrayList<>();
    }

    public void agregarItem(ItemReceta item) { items.add(item); }

    /** Genera código único: REC-YYMMDD-nnnnn */
    public static String generarCodigo(String fecha, int secuencia) {
        String yymmdd = fecha.replace("-","").substring(2); // YYMMDD
        return String.format("REC-%s-%05d", yymmdd, secuencia);
    }

    /** Imprime la receta en consola con formato de comprobante */
    public void imprimir() {
        System.out.println("\n  ╔═══════════════════════════════════════════════════════╗");
        System.out.println("  ║                 RECETA MEDICA                         ║");
        System.out.println("  ╠═══════════════════════════════════════════════════════╣");
        System.out.printf ("  ║  Codigo     : %-41s║%n", codigoReceta);
        System.out.printf ("  ║  Cita       : %-41s║%n", idCita);
        System.out.printf ("  ║  Fecha      : %-19s  Vence: %-15s║%n", fechaEmision, fechaVencimiento);
        System.out.printf ("  ║  Estado     : %-41s║%n", estado);
        System.out.println("  ╠═══════════════════════════════════════════════════════╣");
        System.out.printf ("  ║  PACIENTE   : %-41s║%n", ab(nombrePaciente,41));
        System.out.printf ("  ║  Cedula     : %-41s║%n", cedulaPaciente);
        System.out.println("  ╠═══════════════════════════════════════════════════════╣");
        System.out.printf ("  ║  MÉDICO     : %-41s║%n", ab(medicoNombre,41));
        System.out.printf ("  ║  Especialid : %-41s║%n", especialidad);
        System.out.printf ("  ║  Diagnostico: %-41s║%n", ab(diagnostico,41));
        System.out.println("  ╠═══════════════════════════════════════════════════════╣");
        System.out.println("  ║  MEDICAMENTOS                                         ║");
        System.out.println("  ╠═══════════════════════════════════════════════════════╣");
        int n = 1;
        for (ItemReceta it : items) {
            System.out.printf ("  ║  %d. %-50s║%n", n++, ab(it.medicamento + " " + it.dosis, 50));
            System.out.printf ("  ║     Via: %-12s  Frecuencia: %-23s║%n",
                    ab(it.via,12), ab(it.frecuencia,23));
            System.out.printf ("  ║     Duración: %-12s  Instruc: %-20s║%n",
                    ab(it.duracion,12), ab(it.instrucciones,20));
            System.out.println("  ║                                                       ║");
        }
        if (!observaciones.isEmpty() && !observaciones.equals("Sin observaciones"))
            System.out.printf ("  ║  Obs: %-49s║%n", ab(observaciones,49));
        System.out.println("  ╚═══════════════════════════════════════════════════════╝");
    }

    private String ab(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max-1) + "." : s;
    }
}
