import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * HistorialMedico — la clase que almacena el historial clínico de un paciente. Cada paciente tiene UN historial identificado por un código único (HIS-XXXXX).
 * El historial contiene múltiples entradas (consultas), cada una agregada por un médico.
 */
public class HistorialMedico {

    // ─── Clase interna: una entrada/consulta dentro del historial ───
    public static class EntradaHistorial {
        public String fecha;
        public String medico;           // username del médico
        public String nombreMedico;     // Nombres + Apellidos
        public String especialidad;
        public String motivoConsulta;
        public String diagnostico;
        public String tratamiento;
        public String observaciones;

        public EntradaHistorial(String fecha, String medico, String nombreMedico,
                                String especialidad, String motivoConsulta,
                                String diagnostico, String tratamiento, String observaciones) {
            this.fecha          = fecha;
            this.medico         = medico;
            this.nombreMedico   = nombreMedico;
            this.especialidad   = especialidad;
            this.motivoConsulta = motivoConsulta;
            this.diagnostico    = diagnostico;
            this.tratamiento    = tratamiento;
            this.observaciones  = observaciones;
        }
    }

    // ─── Campos del historial ───────────────────────────────────────
    public String codigoHistorial;    // HIS-XXXXX  único por paciente
    public String usernamePaciente;
    public String nombrePaciente;
    public String fechaNacimiento;
    public String cedula;
    public List<EntradaHistorial> entradas;

    public HistorialMedico(String codigoHistorial, String usernamePaciente,
                           String nombrePaciente, String fechaNacimiento, String cedula) {
        this.codigoHistorial  = codigoHistorial;
        this.usernamePaciente = usernamePaciente;
        this.nombrePaciente   = nombrePaciente;
        this.fechaNacimiento  = fechaNacimiento;
        this.cedula           = cedula;
        this.entradas         = new ArrayList<>();
    }

    public void agregarEntrada(EntradaHistorial e) {
        entradas.add(e);
    }

    /** Genera el código único: HIS- + 5 dígitos basados en cédula y fecha */
    public static String generarCodigo(String cedula, String fechaNac) {
        // Tomar últimos 3 dígitos de cédula + año de nacimiento (sin siglo)
        String sufCedula = cedula.length() >= 3 ? cedula.substring(cedula.length() - 3) : cedula;
        String anio = fechaNac != null && fechaNac.length() >= 4
                ? fechaNac.substring(2, 4) : "00";
        // Número aleatorio de 2 dígitos para unicidad en caso de colisión
        int rand = (int)(Math.random() * 90) + 10;
        return "HIS-" + sufCedula + anio + rand;
    }
}
