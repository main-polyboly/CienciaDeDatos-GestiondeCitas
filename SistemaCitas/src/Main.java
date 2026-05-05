import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.nio.file.*;
 
public class Main {
 
    static ArrayList<Usuario> usuarios = new ArrayList<>();
    static ArrayList<Cita>    citas    = new ArrayList<>();
    static Scanner sc = new Scanner(System.in);
 
    static final String[] HORAS = {
        "08:00","09:00","10:00","11:00","12:00",
        "13:00","14:00","15:00","16:00","17:00"
    };
 
    static final String[] ESPECIALIDADES = {
        "Neurologia","Medico General","Pediatria",
        "Ginecologia","Cardiologia","Psicologia","Traumatologia"
    };
 
    static Map<String, List<String>> doctores = new LinkedHashMap<>();
 
    // ─────────────────────────────────────────
    //  ARCHIVOS CSV
    // ─────────────────────────────────────────
    static final String CSV_PACIENTES  = "pacientes.csv";
    static final String CSV_MEDICOS    = "medicos.csv";
    static final String CSV_CITAS      = "citas.csv";
 
    // =========================================================
    public static void main(String[] args) {
 
        cargarDoctores();
        inicializarCSVs();          // crea encabezados si no existen
        cargarDatosCSV();           // carga usuarios y citas guardados
 
        usuarios.add(new Administrador("admin", "123"));
 
        int opcion;
        do {
            System.out.println("\n==============================");
            System.out.println("   SISTEMA DE CITAS MEDICAS");
            System.out.println("==============================");
            System.out.println("1. Iniciar sesion");
            System.out.println("2. Registrarse");
            System.out.println("3. Salir");
            System.out.print("Opcion: ");
            opcion = leerEntero();
 
            switch (opcion) {
                case 1: login();     break;
                case 2: registrar(); break;
                case 3: System.out.println("Hasta luego."); break;
                default: System.out.println("Opcion invalida.");
            }
        } while (opcion != 3);
    }
 
    // =========================================================
    //  LOGIN
    // =========================================================
    public static void login() {
        System.out.print("Usuario: ");
        String user = sc.nextLine().trim();
        System.out.print("Contrasena: ");
        String pass = sc.nextLine().trim();
 
        for (Usuario u : usuarios) {
            if (u.getUsername().equalsIgnoreCase(user) && u.login(pass)) {
                System.out.println("\nBienvenido/a, " + u.getUsername() + "!");
                menuUsuario(u);
                return;
            }
        }
        System.out.println("Credenciales incorrectas. Intente de nuevo.");
    }
 
    // =========================================================
    //  REGISTRO
    // =========================================================
    public static void registrar() {
        System.out.println("\n-- TIPO DE USUARIO --");
        System.out.println("1. Paciente");
        System.out.println("2. Medico");
        System.out.print("Opcion: ");
        int tipo = leerEntero();
        if (tipo != 1 && tipo != 2) {
            System.out.println("Opcion invalida.");
            return;
        }
 
        // --- Usuario (solo letras, min 3, max 20, unico)
        String user;
        while (true) {
            System.out.print("Nombre de usuario (solo letras, 3-20 chars): ");
            user = sc.nextLine().trim();
            if (!validarUsuario(user)) {
                System.out.println("  Error: solo letras, entre 3 y 20 caracteres.");
            } else if (existeUsuario(user)) {
                System.out.println("  Error: ese nombre de usuario ya esta registrado.");
            } else {
                break;
            }
        }
 
        // --- Password (5-10 chars, al menos 1 digito)
        String pass;
        while (true) {
            System.out.print("Contrasena (5-10 chars, al menos 1 numero): ");
            pass = sc.nextLine().trim();
            if (!validarPassword(pass))
                System.out.println("  Error: 5-10 caracteres y al menos un numero.");
            else break;
        }
 
        // --- Cedula (10 digitos, algoritmo modulo 10)
        String cedula;
        while (true) {
            System.out.print("Cedula (10 digitos): ");
            cedula = sc.nextLine().trim();
            if (!validarCedula(cedula))
                System.out.println("  Error: cedula invalida (10 digitos, algoritmo ecuatoriano).");
            else break;
        }
 
        // --- Telefono (7-10 digitos)
        String telefono;
        while (true) {
            System.out.print("Telefono (7-10 digitos): ");
            telefono = sc.nextLine().trim();
            if (!validarTelefono(telefono))
                System.out.println("  Error: solo digitos, entre 7 y 10 caracteres.");
            else break;
        }
 
        // --- Correo (formato basico)
        String correo;
        while (true) {
            System.out.print("Correo electronico: ");
            correo = sc.nextLine().trim();
            if (!validarCorreo(correo))
                System.out.println("  Error: formato de correo invalido (ej: nombre@dominio.com).");
            else break;
        }
 
        if (tipo == 1) {
            usuarios.add(new Paciente(user, pass));
            guardarPaciente(user, pass, cedula, telefono, correo);
            System.out.println("Paciente registrado exitosamente.");
        } else {
            String esp = elegirEspecialidad();
            usuarios.add(new Medico(user, pass, esp));
            guardarMedico(user, pass, cedula, telefono, correo, esp);
            System.out.println("Medico registrado exitosamente.");
        }
    }
 
    // =========================================================
    //  MENU SEGUN ROL
    // =========================================================
    public static void menuUsuario(Usuario u) {
        int op;
        do {
            u.menu();
            System.out.println("0. Cerrar sesion");
            System.out.print("Opcion: ");
            op = leerEntero();
 
            if (u instanceof Paciente) {
                if (op == 1) agendarCita((Paciente) u);
                else if (op == 2) verCitasPaciente(u.getUsername());
                else if (op != 0) System.out.println("Opcion invalida.");
            } else if (u instanceof Medico) {
                if (op == 1) verCitasMedico(u.getUsername());
                else if (op != 0) System.out.println("Opcion invalida.");
            } else if (u instanceof Administrador) {
                if (op == 1) verTodasCitas();
                else if (op == 2) eliminarCita();
                else if (op != 0) System.out.println("Opcion invalida.");
            }
        } while (op != 0);
        System.out.println("Sesion cerrada.");
    }
 
    // =========================================================
    //  AGENDAR CITA
    // =========================================================
    public static void agendarCita(Paciente p) {
        System.out.println("\n-- AGENDAR CITA --");
        String esp    = elegirEspecialidad();
        String medico = elegirMedico(esp);
 
        // Fecha
        String fecha;
        while (true) {
            System.out.print("Fecha (YYYY-MM-DD, desde hoy): ");
            fecha = sc.nextLine().trim();
            if (!validarFecha(fecha))
                System.out.println("  Error: fecha invalida o anterior a hoy.");
            else break;
        }
 
        // Hora
        System.out.println("Horas disponibles:");
        for (int i = 0; i < HORAS.length; i++)
            System.out.printf("  %2d. %s%n", (i+1), HORAS[i]);
 
        int opHora;
        while (true) {
            System.out.print("Seleccione hora: ");
            opHora = leerEntero();
            if (opHora < 1 || opHora > HORAS.length)
                System.out.println("  Opcion invalida.");
            else break;
        }
        String hora = HORAS[opHora - 1];
 
        // Verificar que esa hora no este ya ocupada para ese medico y fecha
        if (citaExiste(medico, fecha, hora)) {
            System.out.println("  Ese horario ya esta ocupado para ese medico. Elija otro.");
            return;
        }
 
        Cita c = new Cita(p.getUsername(), medico, esp, fecha, hora);
        citas.add(c);
        guardarCitaCSV(c);
        System.out.println("Cita agendada correctamente: " + medico + " | " + fecha + " | " + hora);
    }
 
    // =========================================================
    //  VER CITAS
    // =========================================================
    public static void verCitasPaciente(String user) {
        System.out.println("\n-- MIS CITAS --");
        boolean tiene = false;
        for (Cita c : citas) {
            if (c.paciente.equalsIgnoreCase(user)) {
                System.out.printf("  Especialidad: %-15s | Medico: %-15s | Fecha: %s | Hora: %s%n",
                    c.especialidad, c.medico, c.fecha, c.hora);
                tiene = true;
            }
        }
        if (!tiene) System.out.println("  No tiene citas registradas.");
    }
 
    public static void verCitasMedico(String user) {
        System.out.println("\n-- CITAS ASIGNADAS --");
        boolean tiene = false;
        for (Cita c : citas) {
            if (c.medico.equalsIgnoreCase(user)) {
                System.out.printf("  Paciente: %-15s | Fecha: %s | Hora: %s%n",
                    c.paciente, c.fecha, c.hora);
                tiene = true;
            }
        }
        if (!tiene) System.out.println("  No tiene citas asignadas.");
    }
 
    public static void verTodasCitas() {
        System.out.println("\n-- TODAS LAS CITAS --");
        if (citas.isEmpty()) { System.out.println("  Sin citas registradas."); return; }
        for (int i = 0; i < citas.size(); i++) {
            Cita c = citas.get(i);
            System.out.printf("  [%d] Paciente: %-12s | Medico: %-15s | Esp: %-15s | %s %s%n",
                i, c.paciente, c.medico, c.especialidad, c.fecha, c.hora);
        }
    }
 
    public static void eliminarCita() {
        verTodasCitas();
        if (citas.isEmpty()) return;
        System.out.print("Indice a eliminar: ");
        int i = leerEntero();
        if (i >= 0 && i < citas.size()) {
            citas.remove(i);
            reescribirCitasCSV();
            System.out.println("Cita eliminada y CSV actualizado.");
        } else {
            System.out.println("Indice no valido.");
        }
    }
 
    // =========================================================
    //  VALIDACIONES
    // =========================================================
    public static boolean validarFecha(String fecha) {
        try {
            LocalDate f = LocalDate.parse(fecha, DateTimeFormatter.ISO_LOCAL_DATE);
            return !f.isBefore(LocalDate.now());
        } catch (Exception e) { return false; }
    }
 
    /** Solo letras (a-z A-Z), entre 3 y 20 caracteres */
    public static boolean validarUsuario(String user) {
        return user != null && user.matches("[a-zA-Z]{3,20}");
    }
 
    /** 5-10 caracteres, al menos 1 digito */
    public static boolean validarPassword(String pass) {
        if (pass == null) return false;
        int len = pass.length();
        return len >= 5 && len <= 10 && pass.matches(".*\\d.*");
    }
 
    /**
     * Cedula ecuatoriana: 10 digitos, algoritmo modulo 10.
     * Provincia valida (01-24), tercer digito < 6 (persona natural).
     */
    public static boolean validarCedula(String cedula) {
        if (cedula == null || !cedula.matches("\\d{10}")) return false;
        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if (provincia < 1 || provincia > 24) return false;
        int tercero = Character.getNumericValue(cedula.charAt(2));
        if (tercero >= 6) return false;
 
        int[] coef = {2,1,2,1,2,1,2,1,2};
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int val = Character.getNumericValue(cedula.charAt(i)) * coef[i];
            if (val >= 10) val -= 9;
            suma += val;
        }
        int verificador = (10 - (suma % 10)) % 10;
        return verificador == Character.getNumericValue(cedula.charAt(9));
    }
 
    /** 7-10 digitos numericos */
    public static boolean validarTelefono(String tel) {
        return tel != null && tel.matches("\\d{7,10}");
    }
 
    /** Formato basico de correo */
    public static boolean validarCorreo(String correo) {
        return correo != null && correo.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }
 
    public static boolean existeUsuario(String user) {
        for (Usuario u : usuarios)
            if (u.getUsername().equalsIgnoreCase(user)) return true;
        return false;
    }
 
    public static boolean citaExiste(String medico, String fecha, String hora) {
        for (Cita c : citas)
            if (c.medico.equals(medico) && c.fecha.equals(fecha) && c.hora.equals(hora))
                return true;
        return false;
    }
 
    // =========================================================
    //  HELPERS DE SELECCION
    // =========================================================
    public static String elegirEspecialidad() {
        System.out.println("\nEspecialidades disponibles:");
        for (int i = 0; i < ESPECIALIDADES.length; i++)
            System.out.printf("  %d. %s%n", (i+1), ESPECIALIDADES[i]);
        int op;
        while (true) {
            System.out.print("Seleccione especialidad: ");
            op = leerEntero();
            if (op >= 1 && op <= ESPECIALIDADES.length) break;
            System.out.println("  Opcion invalida.");
        }
        return ESPECIALIDADES[op - 1];
    }
 
    public static String elegirMedico(String esp) {
        List<String> lista = doctores.getOrDefault(esp, Collections.emptyList());
        System.out.println("Medicos disponibles (" + esp + "):");
        for (int i = 0; i < lista.size(); i++)
            System.out.printf("  %d. %s%n", (i+1), lista.get(i));
        int op;
        while (true) {
            System.out.print("Seleccione medico: ");
            op = leerEntero();
            if (op >= 1 && op <= lista.size()) break;
            System.out.println("  Opcion invalida.");
        }
        return lista.get(op - 1);
    }
 
    public static void cargarDoctores() {
        doctores.put("Neurologia",     Arrays.asList("Dr. Perez",  "Dra. Lopez",  "Dr. Ruiz"));
        doctores.put("Medico General", Arrays.asList("Dr. Aguilar","Dr. Bernal",  "Dr. Cobo"));
        doctores.put("Pediatria",      Arrays.asList("Dra. Mora",  "Dr. Diaz",    "Dr. Vega"));
        doctores.put("Ginecologia",    Arrays.asList("Dra. Luna",  "Dra. Paz",    "Dra. Rios"));
        doctores.put("Cardiologia",    Arrays.asList("Dr. Castro", "Dra. Leon",   "Dr. Silva"));
        doctores.put("Psicologia",     Arrays.asList("Dra. Mena",  "Dr. Rosales", "Dra. Soto"));
        doctores.put("Traumatologia",  Arrays.asList("Dr. Torres", "Dr. Salas",   "Dr. Vela"));
    }
 
    // =========================================================
    //  CSV — INICIALIZAR ENCABEZADOS
    // =========================================================
    public static void inicializarCSVs() {
        crearCSVConEncabezado(CSV_PACIENTES,
            "ID_Paciente,Nombre_Usuario,Contrasena,Cedula,Telefono,Correo,Fecha_Registro");
        crearCSVConEncabezado(CSV_MEDICOS,
            "ID_Medico,Nombre_Usuario,Contrasena,Cedula,Telefono,Correo,Especialidad,Fecha_Registro");
        crearCSVConEncabezado(CSV_CITAS,
            "ID_Cita,Paciente,Medico,Especialidad,Fecha,Hora,Estado");
    }
 
    static void crearCSVConEncabezado(String archivo, String encabezado) {
        File f = new File(archivo);
        if (!f.exists()) {
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(encabezado + "\n");
            } catch (IOException e) {
                System.out.println("Error al crear " + archivo);
            }
        }
    }
 
    // =========================================================
    //  CSV — GUARDAR
    // =========================================================
    static int contarLineas(String archivo) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            while (br.readLine() != null) count++;
        } catch (IOException e) { /* archivo nuevo */ }
        return Math.max(0, count - 1); // descontar encabezado
    }
 
    public static void guardarPaciente(String user, String pass,
                                       String cedula, String telefono, String correo) {
        try (FileWriter fw = new FileWriter(CSV_PACIENTES, true)) {
            int id = contarLineas(CSV_PACIENTES) + 1;
            String fecha = LocalDate.now().toString();
            fw.write(String.format("P%03d,%s,%s,%s,%s,%s,%s%n",
                id, user, pass, cedula, telefono, correo, fecha));
        } catch (IOException e) { System.out.println("Error guardando paciente."); }
    }
 
    public static void guardarMedico(String user, String pass, String cedula,
                                     String telefono, String correo, String especialidad) {
        try (FileWriter fw = new FileWriter(CSV_MEDICOS, true)) {
            int id = contarLineas(CSV_MEDICOS) + 1;
            String fecha = LocalDate.now().toString();
            fw.write(String.format("M%03d,%s,%s,%s,%s,%s,%s,%s%n",
                id, user, pass, cedula, telefono, correo, especialidad, fecha));
        } catch (IOException e) { System.out.println("Error guardando medico."); }
    }
 
    public static void guardarCitaCSV(Cita c) {
        try (FileWriter fw = new FileWriter(CSV_CITAS, true)) {
            int id = contarLineas(CSV_CITAS) + 1;
            fw.write(String.format("C%03d,%s,%s,%s,%s,%s,Confirmada%n",
                id, c.paciente, c.medico, c.especialidad, c.fecha, c.hora));
        } catch (IOException e) { System.out.println("Error guardando cita."); }
    }
 
    public static void reescribirCitasCSV() {
        try (FileWriter fw = new FileWriter(CSV_CITAS)) {
            fw.write("ID_Cita,Paciente,Medico,Especialidad,Fecha,Hora,Estado\n");
            int id = 1;
            for (Cita c : citas) {
                fw.write(String.format("C%03d,%s,%s,%s,%s,%s,Confirmada%n",
                    id++, c.paciente, c.medico, c.especialidad, c.fecha, c.hora));
            }
        } catch (IOException e) { System.out.println("Error reescribiendo citas."); }
    }
 
    // =========================================================
    //  CSV — CARGAR AL INICIO
    // =========================================================
    public static void cargarDatosCSV() {
        // Pacientes
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_PACIENTES))) {
            br.readLine(); // saltar encabezado
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] p = linea.split(",");
                if (p.length >= 3) usuarios.add(new Paciente(p[1], p[2]));
            }
        } catch (IOException ignored) {}
 
        // Medicos
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_MEDICOS))) {
            br.readLine();
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] m = linea.split(",");
                if (m.length >= 7) usuarios.add(new Medico(m[1], m[2], m[6]));
            }
        } catch (IOException ignored) {}
 
        // Citas
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_CITAS))) {
            br.readLine();
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] c = linea.split(",");
                if (c.length >= 6) citas.add(new Cita(c[1], c[2], c[3], c[4], c[5]));
            }
        } catch (IOException ignored) {}
    }
 
    // =========================================================
    //  UTIL: leer entero sin explotar
    // =========================================================
    static int leerEntero() {
        while (true) {
            try {
                String s = sc.nextLine().trim();
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.print("  Ingrese un numero valido: ");
            }
        }
    }
}
