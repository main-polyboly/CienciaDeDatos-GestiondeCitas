//se conecta al cvs de auditoria 
public class Auditoria {

    public String timestamp;     // YYYY-MM-DD HH:mm:ss
    public String usuario;       // username quien ejecuta la acción
    public String rol;           // Paciente | Medico | Administrador | Sistema
    public String accion;        // ver lista arriba
    public String entidad;       // tabla/objeto afectado
    public String idEntidad;     // ID del registro afectado
    public String detalle;       // descripción libre

    public Auditoria(String timestamp, String usuario, String rol,
                     String accion, String entidad, String idEntidad, String detalle) {
        this.timestamp = timestamp;
        this.usuario   = usuario;
        this.rol       = rol;
        this.accion    = accion;
        this.entidad   = entidad;
        this.idEntidad = idEntidad;
        this.detalle   = detalle;
    }
}
