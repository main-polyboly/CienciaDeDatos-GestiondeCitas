public class Cita {
    public String idCita;
    public String paciente;         // username del paciente
    public String nombrePaciente;   // Nombres + Apellidos del paciente
    public String medicoUsername;   // username del médico (para buscarlo en la lista)
    public String medicoNombre;     // Nombres + Apellidos del médico 
    public String especialidad;
    public String fecha;
    public String hora;
    public String estado;

    public Cita(String idCita, String paciente, String nombrePaciente,
                String medicoUsername, String medicoNombre,
                String especialidad, String fecha, String hora, String estado) {
        this.idCita         = idCita;
        this.paciente       = paciente;
        this.nombrePaciente = nombrePaciente;
        this.medicoUsername = medicoUsername;
        this.medicoNombre   = medicoNombre;
        this.especialidad   = especialidad;
        this.fecha          = fecha;
        this.hora           = hora;
        this.estado         = estado;
    }
}
