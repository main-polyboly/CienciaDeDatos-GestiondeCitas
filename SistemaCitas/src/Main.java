import java.util.*;
import java.time.LocalDate;

public class Main {

    static List<Usuario>        usuarios    = new ArrayList<>();
    static List<Cita>           citas       = new ArrayList<>();
    static List<HistorialMedico> historiales = new ArrayList<>();
    static Scanner sc = new Scanner(System.in);

    // Horas de atención disponibles
    static final String[] HORAS = {
            "08:00","09:00","10:00","11:00","12:00",
            "13:00","14:00","15:00","16:00","17:00"
    };

    // ════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        ExcelDB.inicializar();
        cargarDatosGuardados();

        // Admin fijo — solo se agrega si no existe ya
        if (!existeUsername("admin")) {
            Administrador adm = new Administrador("admin", "admin@sistema.ec", "Admin123");
            usuarios.add(adm);
            ExcelDB.guardarCredencial("admin.cred", "admin", "Admin123");
        }

        int op;
        do {
            System.out.println("\n╔══════════════════════════════════╗");
            System.out.println("║   SISTEMA DE CITAS MEDICAS       ║");
            System.out.println("╠══════════════════════════════════╣");
            System.out.println("║  1. Iniciar sesion               ║");
            System.out.println("║  2. Registrarse                  ║");
            System.out.println("║  3. Salir                        ║");
            System.out.println("╚══════════════════════════════════╝");
            System.out.print("  Opcion: ");
            op = leerEntero();
            switch (op) {
                case 1: login();     break;
                case 2: registrar(); break;
                case 3: System.out.println("\n  Hasta luego!"); break;
                default: System.out.println("  Opcion no valida.");
            }
        } while (op != 3);
    }

    // ════════════════════════════════════════════════════════════
    //  LOGIN  (username O correo)
    // ════════════════════════════════════════════════════════════
    static void login() {
        System.out.println("\n─── INICIAR SESION ───");
        System.out.print("  Usuario o correo electronico: ");
        String id   = sc.nextLine().trim();
        System.out.print("  Contraseña: ");
        String pass = sc.nextLine().trim();

        for (Usuario u : usuarios) {
            if (u.coincideIdentificador(id) && u.login(pass)) {
                System.out.println("  ✓ Bienvenido/a, " + u.getUsername() + "!");
                menuUsuario(u);
                return;
            }
        }
        System.out.println("  ✗ Credenciales incorrectas.");
    }

    // ════════════════════════════════════════════════════════════
    //  REGISTRO
    // ════════════════════════════════════════════════════════════
    static void registrar() {
        System.out.println("\n─── REGISTRO ───");
        System.out.println("  1. Paciente");
        System.out.println("  2. Medico");
        System.out.print("  Tipo: ");
        int tipo = leerEntero();
        if (tipo != 1 && tipo != 2) { System.out.println("  Opcion invalida."); return; }

        String nombres = pedir("Nombres (letras): ",
                s -> Validaciones.validarNombre(s),
                "  Solo letras, minimo 2 caracteres.");

        String apellidos = pedir("Apellidos (letras): ",
                s -> Validaciones.validarNombre(s),
                "  Solo letras, minimo 2 caracteres.");

        String username = pedir("Usuario (solo letras, 3-20): ",
                s -> Validaciones.validarUsername(s) && !existeUsername(s),
                "  Solo letras 3-20 chars o ya esta en uso.");

        String correo = pedir("Correo electronico: ",
                s -> Validaciones.validarCorreo(s) && !existeCorreo(s),
                "  Formato invalido o ya registrado.");

        String password = pedir("Contraseña (8-20, 1 Mayus, 1 minus, 1 num): ",
                s -> Validaciones.validarPassword(s),
                "  Minimo 8 chars, 1 mayusc, 1 minusc, 1 numero.");

        String cedula = pedir("Cedula (10 digitos): ",
                s -> Validaciones.validarCedula(s),
                "  Cedula ecuatoriana invalida (10 digitos).");

        String telefono = pedir("Telefono (exactamente 10 digitos numericos): ",
                s -> Validaciones.validarTelefono(s),
                "  Exactamente 10 digitos, sin letras ni espacios.");

        if (tipo == 1) {
            // ─ Paciente
            String fechaNac = pedir("Fecha de nacimiento (YYYY-MM-DD): ",
                    s -> Validaciones.validarFechaNacimiento(s),
                    "  Formato YYYY-MM-DD, no puede ser futura.");

            String codHistorial = HistorialMedico.generarCodigo(cedula, fechaNac);
            // Asegurarse de que el código sea único
            while (existeCodigoHistorial(codHistorial))
                codHistorial = HistorialMedico.generarCodigo(cedula, fechaNac);

            Paciente p = new Paciente(username, correo, password,
                    nombres, apellidos, cedula, telefono,
                    fechaNac, codHistorial);
            usuarios.add(p);
            ExcelDB.guardarPaciente(p);
            ExcelDB.guardarCredencial("pacientes.cred", username, password);

            // Crear historial vacío
            HistorialMedico h = new HistorialMedico(codHistorial, username,
                    nombres + " " + apellidos, fechaNac, cedula);
            historiales.add(h);
            ExcelDB.guardarHistorial(h);

            System.out.println("\n  ✓ Paciente registrado.");
            System.out.println("  ► Su codigo de historial medico: " + codHistorial);
            System.out.println("    (Guardelo, lo necesitara para referencia futura)");

        } else {
            // ─ Médico: elige su especialidad
            String especialidad = elegirEspecialidad();
            Medico m = new Medico(username, correo, password,
                    nombres, apellidos, cedula, telefono, especialidad);
            usuarios.add(m);
            ExcelDB.guardarMedico(m);
            ExcelDB.guardarCredencial("medicos.cred", username, password);
            System.out.println("\n  ✓ Medico registrado. Ya puede iniciar sesion.");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MENU POR ROL
    // ════════════════════════════════════════════════════════════
    static void menuUsuario(Usuario u) {
        int op;
        do {
            u.menu();
            System.out.println("  0. Cerrar sesion");
            System.out.print("  Opcion: ");
            op = leerEntero();

            if (u instanceof Paciente) {
                Paciente p = (Paciente) u;
                switch (op) {
                    case 1: agendarCita(p);                  break;
                    case 2: verCitasPaciente(p.getUsername()); break;
                    case 3: verHistorialPaciente(p);          break;
                    case 0: break;
                    default: System.out.println("  Opcion invalida.");
                }
            } else if (u instanceof Medico) {
                Medico m = (Medico) u;
                switch (op) {
                    case 1: verCitasMedico(m);                break;
                    case 2: gestionarHistorialMedico(m);      break;
                    case 0: break;
                    default: System.out.println("  Opcion invalida.");
                }
            } else if (u instanceof Administrador) {
                switch (op) {
                    case 1: verTodasCitas();                  break;
                    case 2: eliminarCita();                   break;
                    case 3: verTodosHistoriales();            break;
                    case 0: break;
                    default: System.out.println("  Opcion invalida.");
                }
            }
        } while (op != 0);
        System.out.println("  Sesion cerrada.\n");
    }

    // ════════════════════════════════════════════════════════════
    //  AGENDAR CITA
    //  BUG CORREGIDO: combina médicos registrados + predefinidos
    // ════════════════════════════════════════════════════════════
    static void agendarCita(Paciente p) {
        System.out.println("\n─── AGENDAR CITA ───");

        // 1. Elegir especialidad
        String especialidad = elegirEspecialidad();

        // 2. Obtener lista de médicos de esa especialidad
        //    = médicos registrados en el sistema + ningún hardcoded (ya no se usan)
        List<Medico> medEsp = getMedicosPorEspecialidad(especialidad);
        if (medEsp.isEmpty()) {
            System.out.println("  No hay medicos disponibles para " + especialidad + " en este momento.");
            return;
        }

        System.out.println("\n  Medicos disponibles (" + especialidad + "):");
        for (int i = 0; i < medEsp.size(); i++) {
            Medico m = medEsp.get(i);
            System.out.printf("    %d. %s %s%n", (i+1), m.getNombres(), m.getApellidos());
        }
        int opM;
        while (true) {
            System.out.print("  Seleccione medico: ");
            opM = leerEntero();
            if (opM >= 1 && opM <= medEsp.size()) break;
            System.out.println("  Opcion invalida.");
        }
        Medico medicoElegido = medEsp.get(opM - 1);

        // 3. Fecha
        String fecha = pedir("Fecha de cita (YYYY-MM-DD, desde hoy): ",
                s -> Validaciones.validarFechaFutura(s),
                "  Formato YYYY-MM-DD, no puede ser pasada.");

        // 4. Horas disponibles (sin las ya ocupadas)
        List<String> libres = horasLibres(medicoElegido.getUsername(), fecha);
        if (libres.isEmpty()) {
            System.out.println("  No hay horarios disponibles para ese medico en esa fecha.");
            return;
        }
        System.out.println("  Horas disponibles:");
        for (int i = 0; i < libres.size(); i++)
            System.out.printf("    %2d. %s%n", (i+1), libres.get(i));

        int opH;
        while (true) {
            System.out.print("  Seleccione hora: ");
            opH = leerEntero();
            if (opH >= 1 && opH <= libres.size()) break;
            System.out.println("  Opcion invalida.");
        }
        String hora = libres.get(opH - 1);

        // 5. Crear y guardar cita
        String idCita = ExcelDB.generarIdCita();
        String nomPac = p.getNombres() + " " + p.getApellidos();
        String nomMed = medicoElegido.getNombres() + " " + medicoElegido.getApellidos();
        Cita c = new Cita(idCita, p.getUsername(), nomPac,
                medicoElegido.getUsername(), nomMed,
                especialidad, fecha, hora, "Confirmada");
        citas.add(c);
        ExcelDB.guardarCita(c);

        System.out.println("\n  ✓ Cita agendada:");
        System.out.printf("    ID: %-10s  Medico: %s %s%n", idCita, medicoElegido.getNombres(), medicoElegido.getApellidos());
        System.out.printf("    Especialidad: %-15s Fecha: %s  Hora: %s%n", especialidad, fecha, hora);
    }

    // ════════════════════════════════════════════════════════════
    //  VER CITAS — PACIENTE
    // ════════════════════════════════════════════════════════════
    static void verCitasPaciente(String username) {
        System.out.println("\n─── MIS CITAS ───");
        List<Cita> mias = new ArrayList<>();
        for (Cita c : citas)
            if (c.paciente.equalsIgnoreCase(username)) mias.add(c);

        if (mias.isEmpty()) {
            System.out.println("  No tiene citas registradas por el momento.");
            return;
        }
        System.out.printf("  %-8s %-22s %-16s %-12s %-6s %-12s%n",
                "ID","Medico","Especialidad","Fecha","Hora","Estado");
        System.out.println("  " + "─".repeat(80));
        for (Cita c : mias)
            System.out.printf("  %-8s %-22s %-16s %-12s %-6s %-12s%n",
                    c.idCita, c.medicoNombre, c.especialidad, c.fecha, c.hora, c.estado);
    }

    // ════════════════════════════════════════════════════════════
    //  VER CITAS — MÉDICO
    // ════════════════════════════════════════════════════════════
    static void verCitasMedico(Medico m) {
        System.out.println("\n─── MIS CITAS ASIGNADAS ───");
        List<Cita> mias = new ArrayList<>();
        for (Cita c : citas)
            if (c.medicoUsername.equalsIgnoreCase(m.getUsername())) mias.add(c);

        if (mias.isEmpty()) {
            System.out.println("  No tiene citas asignadas por el momento.");
            return;
        }
        System.out.printf("  %-8s %-22s %-12s %-6s %-12s%n",
                "ID","Paciente","Fecha","Hora","Estado");
        System.out.println("  " + "─".repeat(64));
        for (Cita c : mias)
            System.out.printf("  %-8s %-22s %-12s %-6s %-12s%n",
                    c.idCita, c.nombrePaciente, c.fecha, c.hora, c.estado);
    }

    // ════════════════════════════════════════════════════════════
    //  HISTORIAL MEDICO — PACIENTE (solo lectura)
    // ════════════════════════════════════════════════════════════
    static void verHistorialPaciente(Paciente p) {
        System.out.println("\n─── MI HISTORIAL MEDICO ───");
        HistorialMedico h = buscarHistorial(p.getUsername());
        if (h == null) {
            System.out.println("  No se encontro historial. Codigo: " + p.getCodigoHistorial());
            return;
        }
        imprimirHistorial(h);
    }

    // ════════════════════════════════════════════════════════════
    //  HISTORIAL MEDICO — MÉDICO (puede agregar entradas)
    // ════════════════════════════════════════════════════════════
    static void gestionarHistorialMedico(Medico m) {
        System.out.println("\n─── GESTIONAR HISTORIAL MEDICO ───");
        System.out.print("  Ingrese el codigo de historial del paciente (HIS-XXXXX): ");
        String codigo = sc.nextLine().trim().toUpperCase();

        HistorialMedico h = buscarHistorialPorCodigo(codigo);
        if (h == null) {
            System.out.println("  ✗ No se encontro historial con codigo: " + codigo);
            return;
        }

        System.out.println("\n  Paciente: " + h.nombrePaciente + " | Cedula: " + h.cedula);
        System.out.println("  Codigo:   " + h.codigoHistorial);
        System.out.println("\n  1. Ver historial completo");
        System.out.println("  2. Agregar nueva consulta/entrada");
        System.out.print("  Opcion: ");
        int op = leerEntero();

        if (op == 1) {
            imprimirHistorial(h);
        } else if (op == 2) {
            agregarEntradaHistorial(h, m);
        }
    }

    static void agregarEntradaHistorial(HistorialMedico h, Medico m) {
        System.out.println("\n  ── NUEVA CONSULTA ──");
        System.out.println("  (Complete los campos. Presione Enter para dejar en blanco si aplica)");

        System.out.print("  Motivo de consulta: ");
        String motivo = sc.nextLine().trim();
        if (motivo.isEmpty()) motivo = "No especificado";

        System.out.print("  Diagnostico: ");
        String diag = sc.nextLine().trim();
        if (diag.isEmpty()) diag = "Pendiente";

        System.out.print("  Tratamiento indicado: ");
        String trat = sc.nextLine().trim();
        if (trat.isEmpty()) trat = "Ninguno";

        System.out.print("  Observaciones adicionales: ");
        String obs = sc.nextLine().trim();
        if (obs.isEmpty()) obs = "Sin observaciones";

        String fecha = LocalDate.now().toString();
        String nomMed = m.getNombres() + " " + m.getApellidos();
        HistorialMedico.EntradaHistorial entrada = new HistorialMedico.EntradaHistorial(
                fecha, m.getUsername(), nomMed, m.getEspecialidad(),
                motivo, diag, trat, obs);

        h.agregarEntrada(entrada);
        ExcelDB.guardarEntrada(h.codigoHistorial, entrada);
        System.out.println("\n  ✓ Entrada agregada al historial " + h.codigoHistorial);
    }

    static void imprimirHistorial(HistorialMedico h) {
        System.out.println("\n  ╔══════════════════════════════════════════════════╗");
        System.out.println("  ║            HISTORIAL MEDICO                      ║");
        System.out.println("  ╠══════════════════════════════════════════════════╣");
        System.out.printf ("  ║  Codigo   : %-36s║%n", h.codigoHistorial);
        System.out.printf ("  ║  Paciente : %-36s║%n", h.nombrePaciente);
        System.out.printf ("  ║  Cedula   : %-36s║%n", h.cedula);
        System.out.printf ("  ║  Nac.     : %-36s║%n", h.fechaNacimiento);
        System.out.println("  ╚══════════════════════════════════════════════════╝");

        if (h.entradas.isEmpty()) {
            System.out.println("  Sin consultas registradas aun.");
            return;
        }
        int n = 1;
        for (HistorialMedico.EntradaHistorial e : h.entradas) {
            System.out.println("\n  ┌── Consulta #" + n++ + " ────────────────────────────");
            System.out.println("  │  Fecha        : " + e.fecha);
            System.out.println("  │  Medico       : " + e.nombreMedico);
            System.out.println("  │  Especialidad : " + e.especialidad);
            System.out.println("  │  Motivo       : " + e.motivoConsulta);
            System.out.println("  │  Diagnostico  : " + e.diagnostico);
            System.out.println("  │  Tratamiento  : " + e.tratamiento);
            System.out.println("  │  Observaciones: " + e.observaciones);
            System.out.println("  └──────────────────────────────────────────────");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  ADMIN
    // ════════════════════════════════════════════════════════════
    static void verTodasCitas() {
        System.out.println("\n─── TODAS LAS CITAS ───");
        if (citas.isEmpty()) { System.out.println("  Sin citas registradas."); return; }
        System.out.printf("  %-4s %-14s %-18s %-15s %-12s %-6s %-12s%n",
                "Idx","Paciente","Medico","Especialidad","Fecha","Hora","Estado");
        System.out.println("  " + "─".repeat(86));
        for (int i = 0; i < citas.size(); i++) {
            Cita c = citas.get(i);
            System.out.printf("  [%2d] %-14s %-18s %-15s %-12s %-6s %-12s%n",
                    i, ab(c.nombrePaciente,14), ab(c.medicoNombre,18),
                    ab(c.especialidad,15), c.fecha, c.hora, c.estado);
        }
    }

    static void eliminarCita() {
        verTodasCitas();
        if (citas.isEmpty()) return;
        System.out.print("\n  Indice a eliminar (-1 cancela): ");
        int idx = leerEntero();
        if (idx == -1) { System.out.println("  Cancelado."); return; }
        if (idx < 0 || idx >= citas.size()) { System.out.println("  Indice invalido."); return; }
        Cita c = citas.remove(idx);
        ExcelDB.reescribirCitas(citas);
        System.out.println("  ✓ Cita " + c.idCita + " eliminada.");
    }

    static void verTodosHistoriales() {
        System.out.println("\n─── TODOS LOS HISTORIALES ───");
        if (historiales.isEmpty()) { System.out.println("  Sin historiales."); return; }
        for (HistorialMedico h : historiales) {
            System.out.printf("  %-12s  %-25s  %-12s  %d consulta(s)%n",
                    h.codigoHistorial, h.nombrePaciente, h.cedula, h.entradas.size());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════

    /** Retorna médicos registrados en el sistema para una especialidad dada */
    static List<Medico> getMedicosPorEspecialidad(String especialidad) {
        List<Medico> lista = new ArrayList<>();
        for (Usuario u : usuarios)
            if (u instanceof Medico) {
                Medico m = (Medico) u;
                if (m.getEspecialidad().equalsIgnoreCase(especialidad))
                    lista.add(m);
            }
        return lista;
    }

    /** Retorna todas las especialidades que al menos un médico atiende */
    static String elegirEspecialidad() {
        // Recopilar especialidades únicas de los médicos registrados
        Set<String> espSet = new LinkedHashSet<>();
        for (Usuario u : usuarios)
            if (u instanceof Medico)
                espSet.add(((Medico) u).getEspecialidad());

        List<String> especialidades = new ArrayList<>(espSet);
        if (especialidades.isEmpty()) {
            System.out.println("  No hay especialidades disponibles (ningún médico registrado).");
            return null;
        }
        System.out.println("\n  Especialidades disponibles:");
        for (int i = 0; i < especialidades.size(); i++)
            System.out.printf("    %d. %s%n", (i+1), especialidades.get(i));
        int op;
        while (true) {
            System.out.print("  Seleccione especialidad: ");
            op = leerEntero();
            if (op >= 1 && op <= especialidades.size()) break;
            System.out.println("  Opcion invalida.");
        }
        return especialidades.get(op - 1);
    }

    static List<String> horasLibres(String medicoUsername, String fecha) {
        Set<String> ocupadas = new HashSet<>();
        for (Cita c : citas)
            if (c.medicoUsername.equalsIgnoreCase(medicoUsername) && c.fecha.equals(fecha))
                ocupadas.add(c.hora);
        List<String> libres = new ArrayList<>();
        for (String h : HORAS)
            if (!ocupadas.contains(h)) libres.add(h);
        return libres;
    }

    static HistorialMedico buscarHistorial(String usernamePaciente) {
        for (HistorialMedico h : historiales)
            if (h.usernamePaciente.equalsIgnoreCase(usernamePaciente)) return h;
        return null;
    }

    static HistorialMedico buscarHistorialPorCodigo(String codigo) {
        for (HistorialMedico h : historiales)
            if (h.codigoHistorial.equalsIgnoreCase(codigo)) return h;
        return null;
    }

    static boolean existeCodigoHistorial(String cod) {
        for (HistorialMedico h : historiales)
            if (h.codigoHistorial.equals(cod)) return true;
        return false;
    }

    static boolean existeUsername(String username) {
        for (Usuario u : usuarios)
            if (u.getUsername().equalsIgnoreCase(username)) return true;
        return false;
    }

    static boolean existeCorreo(String correo) {
        for (Usuario u : usuarios)
            if (u.getCorreo().equalsIgnoreCase(correo)) return true;
        return false;
    }

    static void cargarDatosGuardados() {
        for (Paciente p : ExcelDB.cargarPacientes()) usuarios.add(p);
        for (Medico   m : ExcelDB.cargarMedicos())   usuarios.add(m);
        citas.addAll(ExcelDB.cargarCitas());
        historiales.addAll(ExcelDB.cargarHistoriales());
        System.out.printf("  [DB] Pacientes: %d | Medicos: %d | Citas: %d | Historiales: %d%n",
                contarTipo(Paciente.class), contarTipo(Medico.class),
                citas.size(), historiales.size());
    }

    static int contarTipo(Class<?> tipo) {
        int n = 0;
        for (Usuario u : usuarios) if (tipo.isInstance(u)) n++;
        return n;
    }

    /** Solicita campo con validación */
    static String pedir(String prompt, java.util.function.Predicate<String> val, String err) {
        while (true) {
            System.out.print("  " + prompt);
            String v = sc.nextLine().trim();
            if (val.test(v)) return v;
            System.out.println(err);
        }
    }

    static int leerEntero() {
        while (true) {
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.print("  Numero valido: "); }
        }
    }

    static String ab(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max-1) + "." : s;
    }
}
