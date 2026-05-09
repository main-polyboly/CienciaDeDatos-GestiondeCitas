public class Paciente extends Usuario {

    private String nombres;
    private String apellidos;
    private String cedula;
    private String telefono;
    private String fechaNacimiento;
    private String codigoHistorial;   // HIS-XXXXX asignado al registrarse

    public Paciente(String username, String correo, String password,
                    String nombres, String apellidos,
                    String cedula, String telefono,
                    String fechaNacimiento, String codigoHistorial) {
        super(username, correo, password);
        this.nombres          = nombres;
        this.apellidos        = apellidos;
        this.cedula           = cedula;
        this.telefono         = telefono;
        this.fechaNacimiento  = fechaNacimiento;
        this.codigoHistorial  = codigoHistorial;
    }

    public String getNombres()          { return nombres;          }
    public String getApellidos()        { return apellidos;        }
    public String getCedula()           { return cedula;           }
    public String getTelefono()         { return telefono;         }
    public String getFechaNacimiento()  { return fechaNacimiento;  }
    public String getCodigoHistorial()  { return codigoHistorial;  }
    public void   setCodigoHistorial(String c) { this.codigoHistorial = c; }

    @Override
    public void menu() {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║        MENU — PACIENTE           ║");
        System.out.println("╠══════════════════════════════════╣");
        System.out.println("║  1. Agendar cita                 ║");
        System.out.println("║  2. Ver mis citas                ║");
        System.out.println("║  3. Ver mi historial medico      ║");
        System.out.println("╚══════════════════════════════════╝");
    }
}
