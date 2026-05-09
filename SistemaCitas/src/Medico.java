public class Medico extends Usuario {

    private String nombres;
    private String apellidos;
    private String cedula;
    private String telefono;
    private String especialidad;

    public Medico(String username, String correo, String password,
                  String nombres, String apellidos,
                  String cedula, String telefono, String especialidad) {
        super(username, correo, password);
        this.nombres      = nombres;
        this.apellidos    = apellidos;
        this.cedula       = cedula;
        this.telefono     = telefono;
        this.especialidad = especialidad;
    }

    public String getNombres()      { return nombres;      }
    public String getApellidos()    { return apellidos;    }
    public String getCedula()       { return cedula;       }
    public String getTelefono()     { return telefono;     }
    public String getEspecialidad() { return especialidad; }

    /** Nombre para mostrar en listas de selección */
    public String getNombreDisplay() {
        return nombres + " " + apellidos + " (" + especialidad + ")";
    }

    @Override
    public void menu() {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║          MENU — MEDICO           ║");
        System.out.println("╠══════════════════════════════════╣");
        System.out.println("║  1. Ver mis citas                ║");
        System.out.println("║  2. Gestionar historial medico   ║");
        System.out.println("╚══════════════════════════════════╝");
    }
}
