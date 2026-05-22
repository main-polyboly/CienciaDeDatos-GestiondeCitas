import java.io.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Capa de persistencia: CSV para datos que Java lee/escribe,
 * Excel bonito generado por generar_excel.py.
 * Las contraseñas se guardan en archivos .cred separados.
 */
public class ExcelDB {

    static final String F_PACIENTES = "pacientes.csv";
    static final String F_MEDICOS   = "medicos.csv";
    static final String F_CITAS     = "citas.csv";
    static final String F_HISTORIALES = "historiales.csv";
    static final String F_ENTRADAS    = "entradas_historial.csv";

    static final String H_PACIENTES  = "ID,Nombres,Apellidos,Username,Correo,Cedula,Telefono,FechaNacimiento,CodigoHistorial,FechaRegistro";
    static final String H_MEDICOS    = "ID,Nombres,Apellidos,Username,Correo,Cedula,Telefono,Especialidad,FechaRegistro";
    static final String H_CITAS      = "ID_Cita,Username_Paciente,Nombre_Paciente,Medico_Username,Medico_Nombre,Especialidad,Fecha,Hora,Estado,FechaAgendado";
    static final String H_HISTORIALES= "CodigoHistorial,Username_Paciente,Nombre_Paciente,FechaNacimiento,Cedula,FechaCreacion";
    static final String H_ENTRADAS   = "CodigoHistorial,Fecha,Medico_Username,Medico_Nombre,Especialidad,MotivoConsulta,Diagnostico,Tratamiento,Observaciones";

    // ── Inicializar ───────────────────────────────────────────────
    public static void inicializar() {
        crearSiNoExiste(F_PACIENTES,   H_PACIENTES);
        crearSiNoExiste(F_MEDICOS,     H_MEDICOS);
        crearSiNoExiste(F_CITAS,       H_CITAS);
        crearSiNoExiste(F_HISTORIALES, H_HISTORIALES);
        crearSiNoExiste(F_ENTRADAS,    H_ENTRADAS);
    }

    static void crearSiNoExiste(String archivo, String encabezado) {
        File f = new File(archivo);
        if (!f.exists()) {
            try (FileWriter fw = new FileWriter(f)) { fw.write(encabezado + "\n"); }
            catch (IOException e) { System.err.println("Error creando " + archivo); }
        }
    }

    static int contarRegistros(String archivo) {
        int cnt = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            br.readLine();
            while (br.readLine() != null) cnt++;
        } catch (IOException ignored) {}
        return cnt;
    }

    // ════════════════════════════════════════════════════════════
    //  GUARDAR
    // ════════════════════════════════════════════════════════════
    public static void guardarPaciente(Paciente p) {
        try (FileWriter fw = new FileWriter(F_PACIENTES, true)) {
            int id = contarRegistros(F_PACIENTES) + 1;
            fw.write(String.format("P%03d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    id,
                    esc(p.getNombres()), esc(p.getApellidos()),
                    esc(p.getUsername()), esc(p.getCorreo()),
                    p.getCedula(), p.getTelefono(),
                    p.getFechaNacimiento(), p.getCodigoHistorial(),
                    LocalDate.now()));
        } catch (IOException e) { System.err.println("Error guardando paciente."); }
    }

    public static void guardarMedico(Medico m) {
        try (FileWriter fw = new FileWriter(F_MEDICOS, true)) {
            int id = contarRegistros(F_MEDICOS) + 1;
            fw.write(String.format("M%03d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    id,
                    esc(m.getNombres()), esc(m.getApellidos()),
                    esc(m.getUsername()), esc(m.getCorreo()),
                    m.getCedula(), m.getTelefono(),
                    esc(m.getEspecialidad()), LocalDate.now()));
        } catch (IOException e) { System.err.println("Error guardando medico."); }
    }

    public static void guardarCita(Cita c) {
        try (FileWriter fw = new FileWriter(F_CITAS, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    c.idCita,
                    esc(c.paciente), esc(c.nombrePaciente),
                    esc(c.medicoUsername), esc(c.medicoNombre),
                    esc(c.especialidad), c.fecha, c.hora,
                    c.estado, LocalDate.now()));
        } catch (IOException e) { System.err.println("Error guardando cita."); }
    }

    public static void guardarHistorial(HistorialMedico h) {
        try (FileWriter fw = new FileWriter(F_HISTORIALES, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s%n",
                    h.codigoHistorial, esc(h.usernamePaciente), esc(h.nombrePaciente),
                    h.fechaNacimiento, h.cedula, LocalDate.now()));
        } catch (IOException e) { System.err.println("Error guardando historial."); }
    }

    public static void guardarEntrada(String codigoHistorial, HistorialMedico.EntradaHistorial e) {
        try (FileWriter fw = new FileWriter(F_ENTRADAS, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    codigoHistorial, e.fecha,
                    esc(e.medico), esc(e.nombreMedico), esc(e.especialidad),
                    esc(e.motivoConsulta), esc(e.diagnostico),
                    esc(e.tratamiento), esc(e.observaciones)));
        } catch (IOException ex) { System.err.println("Error guardando entrada historial."); }
    }

    // ════════════════════════════════════════════════════════════
    //  CARGAR
    // ════════════════════════════════════════════════════════════
    public static List<Paciente> cargarPacientes() {
        List<Paciente> lista = new ArrayList<>();
        Map<String, String> creds = cargarCredenciales("pacientes.cred");
        try (BufferedReader br = new BufferedReader(new FileReader(F_PACIENTES))) {
            br.readLine();
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] f = csv(linea);
                if (f.length < 10) continue;
                // P%03d, Nombres, Apellidos, Username, Correo, Cedula, Telefono, FechaNac, CodHistorial, FechaReg
                String user = f[3];
                String pass = creds.getOrDefault(user, "");
                lista.add(new Paciente(user, f[4], pass, f[1], f[2], f[5], f[6], f[7], f[8]));
            }
        } catch (IOException ignored) {}
        return lista;
    }

    public static List<Medico> cargarMedicos() {
        List<Medico> lista = new ArrayList<>();
        Map<String, String> creds = cargarCredenciales("medicos.cred");
        try (BufferedReader br = new BufferedReader(new FileReader(F_MEDICOS))) {
            br.readLine();
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] f = csv(linea);
                if (f.length < 9) continue;
                // M%03d, Nombres, Apellidos, Username, Correo, Cedula, Telefono, Especialidad, FechaReg
                String user = f[3];
                String pass = creds.getOrDefault(user, "");
                lista.add(new Medico(user, f[4], pass, f[1], f[2], f[5], f[6], f[7]));
            }
        } catch (IOException ignored) {}
        return lista;
    }

    public static List<Cita> cargarCitas() {
        List<Cita> lista = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(F_CITAS))) {
            br.readLine();
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] f = csv(linea);
                if (f.length < 9) continue;
                lista.add(new Cita(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8]));
            }
        } catch (IOException ignored) {}
        return lista;
    }

    public static List<HistorialMedico> cargarHistoriales() {
        List<HistorialMedico> lista = new ArrayList<>();
        // Primero cargar cabeceras
        try (BufferedReader br = new BufferedReader(new FileReader(F_HISTORIALES))) {
            br.readLine();
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] f = csv(linea);
                if (f.length < 5) continue;
                lista.add(new HistorialMedico(f[0], f[1], f[2], f[3], f[4]));
            }
        } catch (IOException ignored) {}

        // Luego cargar entradas y asignarlas
        try (BufferedReader br = new BufferedReader(new FileReader(F_ENTRADAS))) {
            br.readLine();
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] f = csv(linea);
                if (f.length < 9) continue;
                String cod = f[0];
                for (HistorialMedico h : lista) {
                    if (h.codigoHistorial.equals(cod)) {
                        h.agregarEntrada(new HistorialMedico.EntradaHistorial(
                                f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8]));
                        break;
                    }
                }
            }
        } catch (IOException ignored) {}
        return lista;
    }

    // ── Reescribir citas tras eliminar ───────────────────────────
    public static void reescribirCitas(List<Cita> citas) {
        try (FileWriter fw = new FileWriter(F_CITAS)) {
            fw.write(H_CITAS + "\n");
            int i = 1;
            for (Cita c : citas) {
                c.idCita = String.format("C%03d", i++);
                fw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        c.idCita, esc(c.paciente), esc(c.nombrePaciente),
                        esc(c.medicoUsername), esc(c.medicoNombre),
                        esc(c.especialidad), c.fecha, c.hora,
                        c.estado, LocalDate.now()));
            }
        } catch (IOException e) { System.err.println("Error reescribiendo citas."); }
    }

    // ── Credenciales ─────────────────────────────────────────────
    public static void guardarCredencial(String archivo, String username, String password) {
        try (FileWriter fw = new FileWriter(archivo, true)) {
            fw.write(username + "=" + password + "\n");
        } catch (IOException e) { System.err.println("Error guardando credencial."); }
    }

    static Map<String, String> cargarCredenciales(String archivo) {
        Map<String, String> map = new HashMap<>();
        File f = new File(archivo);
        if (!f.exists()) return map;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                int idx = linea.indexOf('=');
                if (idx > 0) map.put(linea.substring(0, idx), linea.substring(idx + 1));
            }
        } catch (IOException ignored) {}
        return map;
    }

    // Generar ID cita
    public static String generarIdCita() {
        return String.format("C%03d", contarRegistros(F_CITAS) + 1);
    }

    // ── Utilidades CSV ───────────────────────────────────────────
    static String esc(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    static String[] csv(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean enComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (enComillas && i + 1 < linea.length() && linea.charAt(i+1) == '"') {
                    sb.append('"'); i++;
                } else { enComillas = !enComillas; }
            } else if (c == ',' && !enComillas) {
                campos.add(sb.toString()); sb.setLength(0);
            } else { sb.append(c); }
        }
        campos.add(sb.toString());
        return campos.toArray(new String[0]);
    }
}