import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CitaMed 2.0 — Sistema Clínico Integral
 * Módulos: Autenticación · Citas · Historial · Recetas · Facturación ·
 *          Pagos · Notas de Crédito · KPIs/MIS · Auditoría · Búsqueda
 */
public class Main {

    static List<Usuario>         usuarios    = new ArrayList<>();
    static List<Cita>            citas       = new ArrayList<>();
    static List<HistorialMedico> historiales = new ArrayList<>();
    static List<Factura>         facturas    = new ArrayList<>();
    static List<RecetaMedica>    recetas     = new ArrayList<>();
    static List<NotaCredito>     notas       = new ArrayList<>();
    static Scanner sc = new Scanner(System.in);

    static final String[] HORAS = {
        "08:00","09:00","10:00","11:00","12:00",
        "13:00","14:00","15:00","16:00","17:00"
    };
    static final String[] ESPECIALIDADES_BASE = {
        "Medicina General","Cardiología","Pediatría","Ginecología","Traumatología",
        "Neurología","Dermatología","Oftalmología","Psiquiatría","Endocrinología",
        "Gastroenterología","Urología","Oncología","Neumología","Reumatología"
    };
    static final String[] METODOS_PAGO = {
        "Efectivo","Tarjeta de Credito","Tarjeta de Debito",
        "Transferencia","Seguro Medico"
    };
    static final String[] MOTIVOS_CANCEL = {
        "Emergencia personal","Cambio de horario","Error de agendamiento",
        "Medico no disponible","Otro"
    };
    static final String TS_FMT = "yyyy-MM-dd HH:mm:ss";

    // ════════════════════════════════════════════════════════════
    //  MAIN
    // ════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        ExcelDB.inicializar();
        cargarDatosGuardados();

        // Admin fijo con hash
        if (!existeUsername("admin")) {
            String cred = Seguridad.generarCredencial("Admin123");
            Administrador adm = new Administrador("admin","admin@sistema.ec", cred);
            usuarios.add(adm);
            ExcelDB.guardarCredencial("admin.cred","admin", cred);
        }

        auditoria("Sistema","Sistema","INICIO","Sistema","*","Arranque del sistema");

        int op;
        do {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║     CITAMED 2.0 — CITAS MEDICAS    ║");
            System.out.println("╠════════════════════════════════════╣");
            System.out.println("║  1. Iniciar sesion                 ║");
            System.out.println("║  2. Registrarse                    ║");
            System.out.println("║  3. Salir                          ║");
            System.out.println("╚════════════════════════════════════╝");
            System.out.print("  Opcion: ");
            op = leerEntero();
            switch (op) {
                case 1: login();     break;
                case 2: registrar(); break;
                case 3: System.out.println("\n  Hasta luego."); break;
                default: System.out.println("  Opcion no valida.");
            }
        } while (op != 3);
    }

    // ════════════════════════════════════════════════════════════
    //  AUTENTICACIÓN
    // ════════════════════════════════════════════════════════════
    static void login() {
        System.out.println("\n─── INICIAR SESION ───");
        System.out.print("  Usuario o correo: ");
        String id   = sc.nextLine().trim();
        System.out.print("  Contraseña: ");
        String pass = sc.nextLine().trim();

        for (Usuario u : usuarios) {
            if (u.coincideIdentificador(id) && u.login(pass)) {
                System.out.println("  ✓ Bienvenido/a, " + u.getUsername() + "!");
                auditoria(u.getUsername(), rolDe(u),"LOGIN_OK","Usuario",u.getUsername(),"Login exitoso");
                menuUsuario(u);
                return;
            }
        }
        System.out.println("  ✗ Credenciales incorrectas.");
        auditoria(id,"Desconocido","LOGIN_FAIL","Usuario",id,"Credenciales incorrectas");
    }

    // ════════════════════════════════════════════════════════════
    //  REGISTRO
    // ════════════════════════════════════════════════════════════
    static void registrar() {
        System.out.println("\n─── REGISTRO ───");
        System.out.println("  1. Paciente   2. Medico");
        System.out.print("  Tipo: ");
        int tipo = leerEntero();
        if (tipo != 1 && tipo != 2) { System.out.println("  Opcion invalida."); return; }

        String nombres  = pedir("Nombres (letras, 2-40 chars): ",
                s -> Validaciones.validarNombre(s), "  Solo letras, 2-40 chars.");
        String apellidos= pedir("Apellidos (letras, 2-40 chars): ",
                s -> Validaciones.validarNombre(s), "  Solo letras, 2-40 chars.");
        String username = pedir("Usuario (solo letras, 3-20, sin espacios): ",
                s -> Validaciones.validarUsername(s) && !existeUsername(s),
                "  3-20 letras sin espacios, o ya existe.");
        String correo   = pedir("Correo electronico: ",
                s -> Validaciones.validarCorreo(s) && !existeCorreo(s),
                "  Formato invalido o ya registrado.");
        String password = pedir("Contraseña (8-20, 1 May, 1 min, 1 num): ",
                s -> Validaciones.validarPassword(s),
                "  Min 8 chars, 1 mayus, 1 minus, 1 digito.");
        String cedula   = pedir("Cedula (10 digitos): ",
                s -> Validaciones.validarCedula(s) && !existeCedula(s),
                "  Cedula ecuatoriana invalida o ya registrada.");
        String telefono = pedir("Telefono (10 digitos): ",
                s -> Validaciones.validarTelefono(s), "  Exactamente 10 digitos.");

        // Hash de contraseña
        String credencial = Seguridad.generarCredencial(password);

        if (tipo == 1) {
            String fechaNac = pedir("Fecha de nacimiento (YYYY-MM-DD): ",
                    s -> Validaciones.validarFechaNacimiento(s),
                    "  YYYY-MM-DD, no puede ser futura.");
            String codH = HistorialMedico.generarCodigo(cedula, fechaNac);
            while (existeCodigoHistorial(codH))
                codH = HistorialMedico.generarCodigo(cedula, fechaNac);

            Paciente p = new Paciente(username, correo, credencial,
                    nombres, apellidos, cedula, telefono, fechaNac, codH);
            usuarios.add(p);
            ExcelDB.guardarPaciente(p);
            ExcelDB.guardarCredencial("pacientes.cred", username, credencial);

            HistorialMedico h = new HistorialMedico(codH, username,
                    nombres+" "+apellidos, fechaNac, cedula);
            historiales.add(h);
            ExcelDB.guardarHistorial(h);

            System.out.println("\n  ✓ Paciente registrado.");
            System.out.println("  ► Codigo historial: " + codH);
            auditoria(username,"Paciente","REGISTRO_PACIENTE","Paciente",username,"Cedula: "+cedula);

        } else {
            String esp = elegirEspecialidad();
            if (esp == null) return;
            Medico m = new Medico(username, correo, credencial,
                    nombres, apellidos, cedula, telefono, esp);
            usuarios.add(m);
            ExcelDB.guardarMedico(m);
            ExcelDB.guardarCredencial("medicos.cred", username, credencial);
            System.out.println("\n  ✓ Medico registrado.");
            auditoria(username,"Medico","REGISTRO_MEDICO","Medico",username,"Especialidad: "+esp);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MENÚ POR ROL
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
                    case 1: agendarCita(p);                        break;
                    case 2: verCitasPaciente(p);                   break;
                    case 3: cancelarCitaPaciente(p);               break;
                    case 4: verHistorialPaciente(p);               break;
                    case 5: verRecetasPaciente(p.getUsername());   break;
                    case 6: estadoCuentaPaciente(p.getUsername()); break;
                    case 0: break;
                    default: System.out.println("  Opcion invalida.");
                }
            } else if (u instanceof Medico) {
                Medico m = (Medico) u;
                switch (op) {
                    case 1: verCitasMedico(m);             break;
                    case 2: completarCitaFlujo(m);         break;
                    case 3: gestionarHistorialMedico(m);   break;
                    case 4: verRecetasMedico(m);           break;
                    case 5: verIngresosMedico(m);          break;
                    case 0: break;
                    default: System.out.println("  Opcion invalida.");
                }
            } else if (u instanceof Administrador) {
                switch (op) {
                    case 1: busquedaGlobal();              break;
                    case 2: menuGestionCitas();            break;
                    case 3: verTodosHistoriales();         break;
                    case 4: menuFacturacion(u);            break;
                    case 5: menuRecetasAdmin();            break;
                    case 6: menuNotasCredito(u);           break;
                    case 7: mostrarKPIs();                 break;
                    case 8: verAuditoria();                break;
                    case 0: break;
                    default: System.out.println("  Opcion invalida.");
                }
            }
        } while (op != 0);
        System.out.println("  Sesion cerrada.\n");
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: AGENDAR CITA
    // ════════════════════════════════════════════════════════════
    static void agendarCita(Paciente p) {
        System.out.println("\n─── AGENDAR CITA ───");
        String esp = elegirEspecialidad();
        if (esp == null) return;

        List<Medico> medEsp = getMedicosPorEspecialidad(esp);
        if (medEsp.isEmpty()) {
            System.out.println("  No hay medicos para " + esp + " registrados aun."); return;
        }

        System.out.println("\n  Medicos disponibles (" + esp + "):");
        for (int i = 0; i < medEsp.size(); i++)
            System.out.printf("    %d. Dr/a. %s %s%n",(i+1),medEsp.get(i).getNombres(),medEsp.get(i).getApellidos());

        int opM; while (true) {
            System.out.print("  Seleccione medico: "); opM = leerEntero();
            if (opM >= 1 && opM <= medEsp.size()) break;
            System.out.println("  Opcion invalida.");
        }
        Medico med = medEsp.get(opM-1);

        String fecha = pedir("Fecha de cita (YYYY-MM-DD, desde hoy): ",
                s -> Validaciones.validarFechaFutura(s), "  YYYY-MM-DD, no puede ser pasada.");

        List<String> libres = horasLibres(med.getUsername(), fecha);
        if (libres.isEmpty()) { System.out.println("  Sin horarios disponibles ese dia."); return; }

        System.out.println("  Horas disponibles:");
        for (int i = 0; i < libres.size(); i++)
            System.out.printf("    %2d. %s%n",(i+1),libres.get(i));

        int opH; while (true) {
            System.out.print("  Seleccione hora: "); opH = leerEntero();
            if (opH >= 1 && opH <= libres.size()) break;
            System.out.println("  Opcion invalida.");
        }
        String hora = libres.get(opH-1);

        String idCita = ExcelDB.generarIdCita();
        Cita c = new Cita(idCita, p.getUsername(),
                p.getNombres()+" "+p.getApellidos(),
                med.getUsername(), med.getNombres()+" "+med.getApellidos(),
                esp, fecha, hora, "Confirmada");
        citas.add(c);
        ExcelDB.guardarCita(c);

        System.out.printf("%n  ✓ Cita agendada — ID: %s  Fecha: %s %s%n", idCita, fecha, hora);
        System.out.printf("    Médico: %s %s  |  Tarifa aprox.: $%.2f (+ IVA 15%%)%n",
                med.getNombres(), med.getApellidos(), Factura.tarifaPor(esp));
        auditoria(p.getUsername(),"Paciente","CITA_CREADA","Cita",idCita,
                "Medico:"+med.getUsername()+" Fecha:"+fecha+" "+hora);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: VER CITAS PACIENTE
    // ════════════════════════════════════════════════════════════
    static void verCitasPaciente(Paciente p) {
        System.out.println("\n─── MIS CITAS ───");
        List<Cita> mias = new ArrayList<>();
        for (Cita c : citas)
            if (c.paciente.equalsIgnoreCase(p.getUsername())) mias.add(c);

        if (mias.isEmpty()) { System.out.println("  Sin citas registradas."); return; }
        cabeceraCitas();
        for (Cita c : mias) filaCita(c);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: CANCELAR CITA — PACIENTE
    // ════════════════════════════════════════════════════════════
    static void cancelarCitaPaciente(Paciente p) {
        System.out.println("\n─── CANCELAR CITA ───");
        List<Cita> confirmadas = new ArrayList<>();
        for (Cita c : citas)
            if (c.paciente.equalsIgnoreCase(p.getUsername())
                    && c.estado.equalsIgnoreCase("Confirmada"))
                confirmadas.add(c);

        if (confirmadas.isEmpty()) { System.out.println("  Sin citas confirmadas para cancelar."); return; }
        cabeceraCitas();
        for (int i = 0; i < confirmadas.size(); i++) {
            System.out.printf("  [%d] ", i);
            filaCita(confirmadas.get(i));
        }
        System.out.print("\n  Indice a cancelar (-1 cancela): ");
        int idx = leerEntero();
        if (idx < 0 || idx >= confirmadas.size()) { System.out.println("  Cancelado."); return; }

        Cita c = confirmadas.get(idx);
        System.out.println("  Motivo de cancelacion:");
        for (int i = 0; i < MOTIVOS_CANCEL.length; i++)
            System.out.printf("    %d. %s%n",(i+1),MOTIVOS_CANCEL[i]);
        System.out.print("  Seleccione: ");
        int opM = leerEntero();
        String motivo = (opM >= 1 && opM <= MOTIVOS_CANCEL.length)
                ? MOTIVOS_CANCEL[opM-1] : "No especificado";

        c.estado = "Cancelada";
        c.motivoCancel = motivo;
        ExcelDB.reescribirCitas(citas);
        System.out.println("  ✓ Cita " + c.idCita + " cancelada. Motivo: " + motivo);
        auditoria(p.getUsername(),"Paciente","CITA_CANCELADA","Cita",c.idCita,"Motivo: "+motivo);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: VER HISTORIAL — PACIENTE
    // ════════════════════════════════════════════════════════════
    static void verHistorialPaciente(Paciente p) {
        System.out.println("\n─── MI HISTORIAL MEDICO ───");
        HistorialMedico h = buscarHistorial(p.getUsername());
        if (h == null) { System.out.println("  Historial no encontrado."); return; }
        imprimirHistorial(h);
        auditoria(p.getUsername(),"Paciente","HISTORIAL_CONSULTA","Historial",
                h.codigoHistorial,"Consulta propia");
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: VER RECETAS — PACIENTE
    // ════════════════════════════════════════════════════════════
    static void verRecetasPaciente(String username) {
        System.out.println("\n─── MIS RECETAS ───");
        List<RecetaMedica> mias = new ArrayList<>();
        for (RecetaMedica r : recetas)
            if (r.usernamePaciente.equalsIgnoreCase(username)) mias.add(r);

        if (mias.isEmpty()) { System.out.println("  Sin recetas emitidas."); return; }

        System.out.printf("  %-18s %-10s %-16s %-12s %-12s %s%n",
                "Codigo","Cita","Especialidad","Emision","Vencimiento","Estado");
        sep(90);
        for (RecetaMedica r : mias)
            System.out.printf("  %-18s %-10s %-16s %-12s %-12s %s%n",
                    r.codigoReceta, r.idCita, ab(r.especialidad,16),
                    r.fechaEmision, r.fechaVencimiento, r.estado);

        System.out.print("\n  [V] Ver detalle de receta  [Enter] Volver: ");
        String opc = sc.nextLine().trim();
        if (opc.equalsIgnoreCase("V")) {
            System.out.print("  Codigo de receta: ");
            String cod = sc.nextLine().trim().toUpperCase();
            for (RecetaMedica r : mias)
                if (r.codigoReceta.equalsIgnoreCase(cod)) { r.imprimir(); return; }
            System.out.println("  ✗ No encontrada.");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: ESTADO DE CUENTA / PAGOS — PACIENTE
    // ════════════════════════════════════════════════════════════
    static void estadoCuentaPaciente(String username) {
        System.out.println("\n─── ESTADO DE CUENTA ───");
        List<Factura> mias = new ArrayList<>();
        double totalPagado = 0, totalPendiente = 0;
        int cntPag = 0, cntPend = 0;

        for (Factura f : facturas)
            if (f.usernamePaciente.equalsIgnoreCase(username)) mias.add(f);

        if (mias.isEmpty()) { System.out.println("  Sin movimientos registrados."); return; }

        System.out.println("\n  ── FACTURAS PENDIENTES ──");
        boolean hayPend = false;
        for (Factura f : mias) {
            if (f.estadoPago.equalsIgnoreCase("Pendiente")) {
                System.out.printf("  %-8s %-8s %-16s %10.2f  %s%n",
                        f.idFactura, f.idCita, ab(f.especialidad,16), f.total, f.fechaEmision);
                totalPendiente += f.total; cntPend++; hayPend = true;
            }
        }
        if (!hayPend) System.out.println("  Sin pagos pendientes. ✓");

        System.out.println("\n  ── FACTURAS PAGADAS ──");
        boolean hayPag = false;
        for (Factura f : mias) {
            if (f.estadoPago.equalsIgnoreCase("Pagada")) {
                System.out.printf("  %-8s %-8s %-16s %10.2f  %-14s %s%n",
                        f.idFactura, f.idCita, ab(f.especialidad,16),
                        f.total, ab(f.metodoPago,14), f.fechaEmision);
                totalPagado += f.total; cntPag++; hayPag = true;
            }
        }
        if (!hayPag) System.out.println("  Sin pagos registrados.");

        System.out.println("\n  ── NOTAS DE CRÉDITO ──");
        boolean hayNC = false;
        for (NotaCredito nc : notas)
            if (nc.usernamePaciente.equalsIgnoreCase(username)) {
                System.out.printf("  %-8s  Factura: %-8s  Anulado: $%8.2f  %s%n",
                        nc.idNotaCredito, nc.idFactura, nc.montoAnulado, nc.motivo);
                hayNC = true;
            }
        if (!hayNC) System.out.println("  Sin notas de crédito.");

        System.out.println("\n  " + "═".repeat(60));
        System.out.printf("  Facturas pendientes : %d  ($%.2f)%n", cntPend, totalPendiente);
        System.out.printf("  Facturas pagadas    : %d  ($%.2f)%n", cntPag, totalPagado);
        System.out.printf("  SALDO PENDIENTE     : $%.2f%n", totalPendiente);

        if (cntPend > 0) {
            System.out.print("\n  ¿Registrar pago de una factura pendiente? (S/N): ");
            if (sc.nextLine().trim().equalsIgnoreCase("S"))
                pagarFacturaPendiente(username);
        }
    }

    static void pagarFacturaPendiente(String username) {
        System.out.print("  ID de factura a pagar: ");
        String idFac = sc.nextLine().trim().toUpperCase();
        Factura target = null;
        for (Factura f : facturas)
            if (f.idFactura.equalsIgnoreCase(idFac)
                    && f.usernamePaciente.equalsIgnoreCase(username)
                    && f.estadoPago.equalsIgnoreCase("Pendiente")) {
                target = f; break;
            }
        if (target == null) { System.out.println("  ✗ Factura no encontrada o no pendiente."); return; }

        System.out.println("  Metodo de pago:");
        for (int i = 0; i < METODOS_PAGO.length; i++)
            System.out.printf("    %d. %s%n",(i+1),METODOS_PAGO[i]);
        int opM; while(true) {
            System.out.print("  Seleccione: "); opM = leerEntero();
            if (opM >= 1 && opM <= METODOS_PAGO.length) break;
            System.out.println("  Invalido.");
        }
        target.metodoPago = METODOS_PAGO[opM-1];
        target.estadoPago = "Pagada";
        ExcelDB.reescribirFacturas(facturas);
        System.out.printf("  ✓ Pago registrado. Total: $%.2f vía %s%n",
                target.total, target.metodoPago);
        auditoria(username,"Paciente","PAGO_REGISTRADO","Factura",target.idFactura,
                "Monto: $"+target.total+" Metodo: "+target.metodoPago);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: VER CITAS — MÉDICO
    // ════════════════════════════════════════════════════════════
    static void verCitasMedico(Medico m) {
        System.out.println("\n─── MIS CITAS ───");
        List<Cita> mias = new ArrayList<>();
        for (Cita c : citas)
            if (c.medicoUsername.equalsIgnoreCase(m.getUsername())) mias.add(c);
        if (mias.isEmpty()) { System.out.println("  Sin citas asignadas."); return; }
        System.out.printf("  %-8s %-24s %-12s %-6s %-12s%n",
                "ID","Paciente","Fecha","Hora","Estado");
        sep(66);
        for (Cita c : mias)
            System.out.printf("  %-8s %-24s %-12s %-6s %-12s%n",
                    c.idCita, ab(c.nombrePaciente,24), c.fecha, c.hora, c.estado);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: COMPLETAR CITA + RECETA + FACTURA — MÉDICO
    // ════════════════════════════════════════════════════════════
    static void completarCitaFlujo(Medico m) {
        System.out.println("\n─── COMPLETAR CITA ───");

        // Mostrar solo las confirmadas del médico
        List<Cita> confirmadas = new ArrayList<>();
        for (Cita c : citas)
            if (c.medicoUsername.equalsIgnoreCase(m.getUsername())
                    && c.estado.equalsIgnoreCase("Confirmada")) confirmadas.add(c);

        if (confirmadas.isEmpty()) { System.out.println("  Sin citas Confirmadas para completar."); return; }

        System.out.printf("  %-5s %-8s %-24s %-12s %-6s%n","#","ID","Paciente","Fecha","Hora");
        sep(60);
        for (int i = 0; i < confirmadas.size(); i++)
            System.out.printf("  [%2d] %-8s %-24s %-12s %-6s%n",
                    i, confirmadas.get(i).idCita,
                    ab(confirmadas.get(i).nombrePaciente,24),
                    confirmadas.get(i).fecha, confirmadas.get(i).hora);

        System.out.print("  Indice (-1 cancela): ");
        int idx = leerEntero();
        if (idx < 0 || idx >= confirmadas.size()) { System.out.println("  Cancelado."); return; }

        Cita c = confirmadas.get(idx);
        c.estado = "Completada";
        ExcelDB.reescribirCitas(citas);
        System.out.println("  ✓ Cita marcada como Completada.");
        auditoria(m.getUsername(),"Medico","CITA_COMPLETADA","Cita",c.idCita,"");

        // Buscar historial del paciente
        HistorialMedico h = buscarHistorial(c.paciente);

        // ── 1. Agregar entrada al historial ────────────────────
        System.out.println("\n  ── REGISTRO EN HISTORIAL ──");
        System.out.print("  Motivo de consulta: ");
        String motivo = leer("No especificado");
        System.out.print("  Diagnostico: ");
        String diag = leer("Pendiente");
        System.out.print("  Tratamiento: ");
        String trat = leer("Ninguno");
        System.out.print("  Observaciones: ");
        String obs = leer("Sin observaciones");

        if (h != null) {
            HistorialMedico.EntradaHistorial entrada = new HistorialMedico.EntradaHistorial(
                    LocalDate.now().toString(), m.getUsername(),
                    m.getNombres()+" "+m.getApellidos(),
                    m.getEspecialidad(), motivo, diag, trat, obs);
            h.agregarEntrada(entrada);
            ExcelDB.guardarEntrada(h.codigoHistorial, entrada);
            System.out.println("  ✓ Entrada registrada en historial.");
            auditoria(m.getUsername(),"Medico","HISTORIAL_ENTRADA","Historial",
                    h.codigoHistorial,"Cita: "+c.idCita);
        }

        // ── 2. Emitir receta ───────────────────────────────────
        System.out.print("\n  ¿Emitir receta medica? (S/N): ");
        if (sc.nextLine().trim().equalsIgnoreCase("S")) {
            // Obtener cédula del paciente
            String cedPac = obtenerCedulaPaciente(c.paciente);
            String codH   = (h != null) ? h.codigoHistorial : "";
            emitirReceta(m, c, cedPac, codH, diag);
        }

        // ── 3. Generar factura ─────────────────────────────────
        System.out.print("\n  ¿Generar factura ahora? (S/N): ");
        if (sc.nextLine().trim().equalsIgnoreCase("S"))
            generarFactura(m.getUsername(), c);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: RECETA MÉDICA — EMISIÓN
    // ════════════════════════════════════════════════════════════
    static void emitirReceta(Medico m, Cita c, String cedPac, String codH, String diagPrevio) {
        System.out.println("\n  ── EMITIR RECETA MEDICA ──");
        System.out.print("  Diagnostico (Enter para usar el registrado): ");
        String rawDiag = sc.nextLine().trim();
        String diag = rawDiag.isEmpty() ? diagPrevio : rawDiag;

        System.out.print("  Observaciones generales (Enter para omitir): ");
        String obs = leer("Sin observaciones");

        String hoy = LocalDate.now().toString();
        String vence = LocalDate.now().plusDays(30).toString();
        String codRec = ExcelDB.generarIdReceta();

        RecetaMedica r = new RecetaMedica(codRec, c.idCita, codH,
                c.paciente, c.nombrePaciente, cedPac,
                m.getUsername(), m.getNombres()+" "+m.getApellidos(),
                m.getEspecialidad(), hoy, vence, "Activa", diag, obs);

        // Agregar medicamentos
        System.out.println("\n  Agregar medicamentos (Enter en nombre para terminar):");
        int numItem = 1;
        while (true) {
            System.out.printf("  Medicamento %d — Nombre (Enter=terminar): ", numItem);
            String nomMed = sc.nextLine().trim();
            if (nomMed.isEmpty()) break;

            System.out.print("  Dosis (ej: 500mg, 10mg/ml): ");
            String dosis = leer("No especificada");

            System.out.println("  Via de administracion:");
            for (int i = 0; i < RecetaMedica.VIAS.length; i++)
                System.out.printf("    %d. %s%n",(i+1),RecetaMedica.VIAS[i]);
            System.out.print("  Seleccione (Enter=Oral): ");
            String rawVia = sc.nextLine().trim();
            String via = "Oral";
            try {
                int viaIdx = Integer.parseInt(rawVia) - 1;
                if (viaIdx >= 0 && viaIdx < RecetaMedica.VIAS.length)
                    via = RecetaMedica.VIAS[viaIdx];
            } catch (NumberFormatException ignored) {}

            System.out.print("  Frecuencia (ej: cada 8 horas, una vez al dia): ");
            String freq = leer("Segun prescripcion");

            System.out.print("  Duracion (ej: 7 dias, 1 mes, indefinido): ");
            String dur = leer("Segun prescripcion");

            System.out.print("  Instrucciones (ej: tomar con alimentos): ");
            String instr = leer("Ninguna instruccion especial");

            r.agregarItem(new RecetaMedica.ItemReceta(nomMed, dosis, via, freq, dur, instr));
            numItem++;
        }

        if (r.items.isEmpty()) {
            System.out.println("  Receta sin medicamentos, no se emite.");
            return;
        }

        recetas.add(r);
        ExcelDB.guardarReceta(r);
        r.imprimir();
        auditoria(m.getUsername(),"Medico","RECETA_EMITIDA","Receta",codRec,
                "Paciente: "+c.paciente+" Items: "+r.items.size());
        System.out.println("  ✓ Receta " + codRec + " emitida y guardada.");
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: MIS RECETAS — MÉDICO
    // ════════════════════════════════════════════════════════════
    static void verRecetasMedico(Medico m) {
        System.out.println("\n─── RECETAS EMITIDAS ───");
        List<RecetaMedica> mias = new ArrayList<>();
        for (RecetaMedica r : recetas)
            if (r.medicoUsername.equalsIgnoreCase(m.getUsername())) mias.add(r);
        if (mias.isEmpty()) { System.out.println("  Sin recetas emitidas."); return; }

        System.out.printf("  %-18s %-10s %-22s %-12s %-8s%n",
                "Codigo","Cita","Paciente","Emision","Estado");
        sep(76);
        for (RecetaMedica r : mias)
            System.out.printf("  %-18s %-10s %-22s %-12s %-8s%n",
                    r.codigoReceta, r.idCita, ab(r.nombrePaciente,22),
                    r.fechaEmision, r.estado);

        System.out.print("\n  [V] Ver detalle  [Enter] Volver: ");
        if (sc.nextLine().trim().equalsIgnoreCase("V")) {
            System.out.print("  Codigo de receta: ");
            String cod = sc.nextLine().trim().toUpperCase();
            for (RecetaMedica r : mias)
                if (r.codigoReceta.equalsIgnoreCase(cod)) { r.imprimir(); return; }
            System.out.println("  No encontrada.");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: INGRESOS — MÉDICO
    // ════════════════════════════════════════════════════════════
    static void verIngresosMedico(Medico m) {
        System.out.println("\n─── MIS INGRESOS ───");
        double total = 0; int cnt = 0;
        System.out.printf("  %-8s %-8s %-16s %10s %-14s%n",
                "Factura","Cita","Especialidad","Total","Metodo");
        sep(62);
        for (Factura f : facturas) {
            if (!f.medicoUsername.equalsIgnoreCase(m.getUsername())) continue;
            if (!f.estadoPago.equalsIgnoreCase("Pagada")) continue;
            System.out.printf("  %-8s %-8s %-16s %10.2f %-14s%n",
                    f.idFactura, f.idCita, ab(f.especialidad,16),
                    f.total, ab(f.metodoPago,14));
            total += f.total; cnt++;
        }
        System.out.println("  " + "═".repeat(62));
        System.out.printf("  Consultas cobradas: %d  |  TOTAL: $%.2f%n", cnt, total);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: HISTORIAL — MÉDICO (gestión)
    // ════════════════════════════════════════════════════════════
    static void gestionarHistorialMedico(Medico m) {
        System.out.println("\n─── GESTIONAR HISTORIAL ───");
        System.out.print("  Codigo historial (HIS-...) o cedula del paciente: ");
        String termino = sc.nextLine().trim();

        HistorialMedico h = buscarHistorialPorCodigo(termino);
        if (h == null) {
            // Intentar por cédula
            List<HistorialMedico> res = Buscador.buscarHistoriales(historiales, termino);
            if (res.isEmpty()) { System.out.println("  ✗ Historial no encontrado."); return; }
            if (res.size() == 1) { h = res.get(0); }
            else {
                for (int i = 0; i < res.size(); i++)
                    System.out.printf("  [%d] %s  %s  %s%n",
                            i, res.get(i).codigoHistorial,
                            res.get(i).nombrePaciente, res.get(i).cedula);
                System.out.print("  Seleccione: ");
                int idx = leerEntero();
                if (idx < 0 || idx >= res.size()) return;
                h = res.get(idx);
            }
        }

        System.out.println("  Paciente: " + h.nombrePaciente + "  Cedula: " + h.cedula);
        System.out.println("  1. Ver historial completo");
        System.out.println("  2. Agregar entrada manual");
        System.out.print("  Opcion: ");
        int op = leerEntero();
        if (op == 1) {
            imprimirHistorial(h);
            auditoria(m.getUsername(),"Medico","HISTORIAL_CONSULTA","Historial",
                    h.codigoHistorial,"Consulta por medico");
        } else if (op == 2) {
            agregarEntradaManual(h, m);
        }
    }

    static void agregarEntradaManual(HistorialMedico h, Medico m) {
        System.out.println("\n  ── ENTRADA MANUAL ──");
        System.out.print("  Motivo: ");       String mot  = leer("No especificado");
        System.out.print("  Diagnostico: ");  String diag = leer("Pendiente");
        System.out.print("  Tratamiento: ");  String trat = leer("Ninguno");
        System.out.print("  Observaciones: ");String obs  = leer("Sin observaciones");

        HistorialMedico.EntradaHistorial e = new HistorialMedico.EntradaHistorial(
                LocalDate.now().toString(), m.getUsername(),
                m.getNombres()+" "+m.getApellidos(),
                m.getEspecialidad(), mot, diag, trat, obs);
        h.agregarEntrada(e);
        ExcelDB.guardarEntrada(h.codigoHistorial, e);
        System.out.println("  ✓ Entrada registrada.");
        auditoria(m.getUsername(),"Medico","HISTORIAL_ENTRADA","Historial",h.codigoHistorial,"Manual");
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: FACTURACIÓN — ADMIN
    // ════════════════════════════════════════════════════════════
    static void menuFacturacion(Usuario admin) {
        int op;
        do {
            System.out.println("\n─── MODULO DE FACTURACION ───");
            System.out.println("  1. Ver todas las facturas");
            System.out.println("  2. Generar factura manual");
            System.out.println("  3. Reporte por especialidad");
            System.out.println("  4. Reporte por metodo de pago");
            System.out.println("  5. Facturas pendientes de cobro");
            System.out.println("  0. Volver");
            System.out.print("  Opcion: ");
            op = leerEntero();
            switch (op) {
                case 1: verTodasFacturas();        break;
                case 2: generarFacturaManual(admin.getUsername()); break;
                case 3: reportePorEspecialidad();  break;
                case 4: reportePorMetodoPago();    break;
                case 5: facturasPendientes();      break;
            }
        } while (op != 0);
    }

    static void verTodasFacturas() {
        System.out.println("\n─── TODAS LAS FACTURAS ───");
        if (facturas.isEmpty()) { System.out.println("  Sin facturas."); return; }
        double totalG = 0, ivaG = 0;
        int pag = 0, pend = 0, anul = 0;
        System.out.printf("  %-8s %-8s %-14s %-16s %8s %8s %10s %-14s %-10s%n",
                "Factura","Cita","Paciente","Especialidad","Subtotal","IVA","Total","Metodo","Estado");
        sep(102);
        for (Factura f : facturas) {
            System.out.printf("  %-8s %-8s %-14s %-16s %8.2f %8.2f %10.2f %-14s %-10s%n",
                    f.idFactura, f.idCita, ab(f.nombrePaciente,14),
                    ab(f.especialidad,16), f.subtotal, f.iva15, f.total,
                    ab(f.metodoPago,14), f.estadoPago);
            if (f.estadoPago.equalsIgnoreCase("Pagada")) { totalG+=f.total; ivaG+=f.iva15; pag++; }
            else if (f.estadoPago.equalsIgnoreCase("Pendiente")) pend++;
            else anul++;
        }
        sep(102);
        System.out.printf("  Total recaudado: $%.2f  IVA: $%.2f  |  Pagadas:%d  Pendientes:%d  Anuladas:%d%n",
                totalG, ivaG, pag, pend, anul);
    }

    static void generarFactura(String medicoUser, Cita c) {
        // Verificar que no tenga ya factura
        for (Factura f : facturas)
            if (f.idCita.equalsIgnoreCase(c.idCita) && !f.estadoPago.equalsIgnoreCase("Anulada")) {
                System.out.println("  ✗ Esta cita ya tiene factura: " + f.idFactura); return;
            }
        System.out.println("  Metodo de pago:");
        for (int i = 0; i < METODOS_PAGO.length; i++)
            System.out.printf("    %d. %s%n",(i+1),METODOS_PAGO[i]);
        int opM; while (true) {
            System.out.print("  Seleccione: "); opM = leerEntero();
            if (opM >= 1 && opM <= METODOS_PAGO.length) break;
            System.out.println("  Invalido.");
        }
        String metodo = METODOS_PAGO[opM-1];
        System.out.println("  ¿Cobrar ahora (Pagada) o dejar pendiente?  1=Pagada  2=Pendiente");
        System.out.print("  Opcion: ");
        int opE = leerEntero();
        String estadoPago = (opE == 2) ? "Pendiente" : "Pagada";

        double sub = Factura.tarifaPor(c.especialidad);
        Factura f = new Factura(ExcelDB.generarIdFactura(), c.idCita,
                c.paciente, c.nombrePaciente, medicoUser,
                c.especialidad, c.fecha, sub, metodo, estadoPago,
                LocalDate.now().toString());
        facturas.add(f);
        ExcelDB.guardarFactura(f);
        f.imprimirComprobante();
        auditoria(medicoUser,"Medico","FACTURA_EMITIDA","Factura",f.idFactura,
                "Cita:"+c.idCita+" Total:$"+f.total);
    }

    static void generarFacturaManual(String adminUser) {
        System.out.println("\n─── FACTURA MANUAL ───");
        System.out.print("  ID de la cita completada: ");
        String idCita = sc.nextLine().trim().toUpperCase();
        Cita target = null;
        for (Cita c : citas)
            if (c.idCita.equalsIgnoreCase(idCita) && c.estado.equalsIgnoreCase("Completada"))
                { target = c; break; }
        if (target == null) { System.out.println("  ✗ Cita no encontrada o no Completada."); return; }
        for (Factura f : facturas)
            if (f.idCita.equalsIgnoreCase(idCita) && !f.estadoPago.equalsIgnoreCase("Anulada")) {
                System.out.println("  ✗ Ya tiene factura: " + f.idFactura); return;
            }
        generarFactura(adminUser, target);
    }

    static void reportePorEspecialidad() {
        System.out.println("\n─── INGRESOS POR ESPECIALIDAD ───");
        Map<String,double[]> mapa = new TreeMap<>();
        for (Factura f : facturas) {
            if (!f.estadoPago.equalsIgnoreCase("Pagada")) continue;
            double[] d = mapa.getOrDefault(f.especialidad, new double[2]);
            d[0] += f.total; d[1]++;
            mapa.put(f.especialidad, d);
        }
        if (mapa.isEmpty()) { System.out.println("  Sin datos."); return; }
        System.out.printf("  %-22s %8s %12s %10s%n","Especialidad","Citas","Total","Promedio");
        sep(56);
        double totalG = 0;
        for (Map.Entry<String,double[]> e : mapa.entrySet()) {
            System.out.printf("  %-22s %8.0f %12.2f %10.2f%n",
                    e.getKey(), e.getValue()[1], e.getValue()[0],
                    e.getValue()[0]/e.getValue()[1]);
            totalG += e.getValue()[0];
        }
        sep(56);
        System.out.printf("  %-22s %8d %12.2f%n","TOTAL",facturas.size(),totalG);
    }

    static void reportePorMetodoPago() {
        System.out.println("\n─── INGRESOS POR MÉTODO DE PAGO ───");
        Map<String,double[]> mapa = new LinkedHashMap<>();
        for (Factura f : facturas) {
            if (!f.estadoPago.equalsIgnoreCase("Pagada")) continue;
            double[] d = mapa.getOrDefault(f.metodoPago, new double[2]);
            d[0] += f.total; d[1]++;
            mapa.put(f.metodoPago, d);
        }
        if (mapa.isEmpty()) { System.out.println("  Sin datos."); return; }
        System.out.printf("  %-22s %8s %12s%n","Metodo","Transac","Total");
        sep(44);
        for (Map.Entry<String,double[]> e : mapa.entrySet())
            System.out.printf("  %-22s %8.0f %12.2f%n",
                    e.getKey(), e.getValue()[1], e.getValue()[0]);
    }

    static void facturasPendientes() {
        System.out.println("\n─── FACTURAS PENDIENTES ───");
        double totalPend = 0;
        System.out.printf("  %-8s %-8s %-20s %-16s %10s %s%n",
                "Factura","Cita","Paciente","Especialidad","Total","Fecha");
        sep(82);
        for (Factura f : facturas) {
            if (!f.estadoPago.equalsIgnoreCase("Pendiente")) continue;
            System.out.printf("  %-8s %-8s %-20s %-16s %10.2f %s%n",
                    f.idFactura, f.idCita, ab(f.nombrePaciente,20),
                    ab(f.especialidad,16), f.total, f.fechaEmision);
            totalPend += f.total;
        }
        sep(82);
        System.out.printf("  TOTAL PENDIENTE: $%.2f%n", totalPend);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: RECETAS — ADMIN
    // ════════════════════════════════════════════════════════════
    static void menuRecetasAdmin() {
        int op;
        do {
            System.out.println("\n─── MODULO DE RECETAS ───");
            System.out.println("  1. Ver todas las recetas");
            System.out.println("  2. Buscar receta");
            System.out.println("  3. Marcar receta como dispensada");
            System.out.println("  4. Anular receta");
            System.out.println("  0. Volver");
            System.out.print("  Opcion: ");
            op = leerEntero();
            switch (op) {
                case 1: verTodasRecetas();     break;
                case 2: buscarRecetaAdmin();   break;
                case 3: dispensarReceta();     break;
                case 4: anularReceta();        break;
            }
        } while (op != 0);
    }

    static void verTodasRecetas() {
        System.out.println("\n─── TODAS LAS RECETAS ───");
        if (recetas.isEmpty()) { System.out.println("  Sin recetas."); return; }
        System.out.printf("  %-18s %-10s %-20s %-18s %-12s %s%n",
                "Codigo","Cita","Paciente","Medico","Emision","Estado");
        sep(96);
        for (RecetaMedica r : recetas)
            System.out.printf("  %-18s %-10s %-20s %-18s %-12s %s%n",
                    r.codigoReceta, r.idCita,
                    ab(r.nombrePaciente,20), ab(r.medicoNombre,18),
                    r.fechaEmision, r.estado);
    }

    static void buscarRecetaAdmin() {
        System.out.print("  Buscar (nombre/cedula/codigo): ");
        String t = sc.nextLine().trim();
        List<RecetaMedica> res = Buscador.buscarRecetas(recetas, t);
        if (res.isEmpty()) { System.out.println("  Sin resultados."); return; }
        for (RecetaMedica r : res) r.imprimir();
    }

    static void dispensarReceta() {
        System.out.print("  Codigo de receta a dispensar: ");
        String cod = sc.nextLine().trim().toUpperCase();
        for (RecetaMedica r : recetas) {
            if (r.codigoReceta.equalsIgnoreCase(cod)) {
                if (!r.estado.equalsIgnoreCase("Activa")) {
                    System.out.println("  ✗ Solo se pueden dispensar recetas Activas."); return;
                }
                r.estado = "Dispensada";
                ExcelDB.reescribirRecetas(recetas);
                System.out.println("  ✓ Receta " + cod + " marcada como Dispensada.");
                auditoria("admin","Administrador","RECETA_DISPENSADA","Receta",cod,"");
                return;
            }
        }
        System.out.println("  ✗ Receta no encontrada.");
    }

    static void anularReceta() {
        System.out.print("  Codigo de receta a anular: ");
        String cod = sc.nextLine().trim().toUpperCase();
        for (RecetaMedica r : recetas) {
            if (r.codigoReceta.equalsIgnoreCase(cod)) {
                r.estado = "Anulada";
                ExcelDB.reescribirRecetas(recetas);
                System.out.println("  ✓ Receta " + cod + " anulada.");
                return;
            }
        }
        System.out.println("  ✗ No encontrada.");
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: NOTAS DE CRÉDITO — ADMIN
    // ════════════════════════════════════════════════════════════
    static void menuNotasCredito(Usuario admin) {
        int op;
        do {
            System.out.println("\n─── NOTAS DE CREDITO ───");
            System.out.println("  1. Ver todas las notas de credito");
            System.out.println("  2. Emitir nota de credito");
            System.out.println("  0. Volver");
            System.out.print("  Opcion: ");
            op = leerEntero();
            switch (op) {
                case 1: verNotasCredito();               break;
                case 2: emitirNotaCredito(admin.getUsername()); break;
            }
        } while (op != 0);
    }

    static void verNotasCredito() {
        System.out.println("\n─── NOTAS DE CREDITO ───");
        if (notas.isEmpty()) { System.out.println("  Sin notas de crédito."); return; }
        System.out.printf("  %-8s %-8s %-8s %-18s %10s %-10s%n",
                "Nota","Factura","Cita","Paciente","Monto","Estado");
        sep(70);
        for (NotaCredito nc : notas)
            System.out.printf("  %-8s %-8s %-8s %-18s %10.2f %-10s%n",
                    nc.idNotaCredito, nc.idFactura, nc.idCita,
                    ab(nc.nombrePaciente,18), nc.montoAnulado, nc.estado);
    }

    static void emitirNotaCredito(String adminUser) {
        System.out.print("  ID de factura a anular: ");
        String idFac = sc.nextLine().trim().toUpperCase();
        Factura target = null;
        for (Factura f : facturas)
            if (f.idFactura.equalsIgnoreCase(idFac)) { target = f; break; }
        if (target == null) { System.out.println("  ✗ Factura no encontrada."); return; }
        if (target.estadoPago.equalsIgnoreCase("Anulada")) {
            System.out.println("  ✗ Factura ya anulada."); return;
        }

        System.out.println("  Motivo:");
        String[] motivos = {"Cancelacion de cita","Error de facturacion",
                "Servicio no prestado","Otro"};
        for (int i = 0; i < motivos.length; i++)
            System.out.printf("    %d. %s%n",(i+1),motivos[i]);
        System.out.print("  Seleccione: ");
        int opM = leerEntero();
        String motivo = (opM >= 1 && opM <= motivos.length) ? motivos[opM-1] : "Otro";

        // Anular factura
        target.estadoPago = "Anulada";
        ExcelDB.reescribirFacturas(facturas);

        // Crear nota de crédito
        NotaCredito nc = new NotaCredito(ExcelDB.generarIdNota(),
                target.idFactura, target.idCita,
                target.usernamePaciente, target.nombrePaciente,
                motivo, target.total, LocalDate.now().toString(), adminUser);
        notas.add(nc);
        ExcelDB.guardarNotaCredito(nc);
        nc.imprimir();
        auditoria(adminUser,"Administrador","FACTURA_ANULADA","Factura",
                target.idFactura,"NC: "+nc.idNotaCredito);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: BÚSQUEDA GLOBAL — ADMIN
    // ════════════════════════════════════════════════════════════
    static void busquedaGlobal() {
        System.out.println("\n─── BUSQUEDA GLOBAL ───");
        System.out.println("  1. Pacientes   2. Medicos   3. Citas");
        System.out.println("  4. Facturas    5. Recetas   6. Historiales");
        System.out.print("  Que buscar: ");
        int tipo = leerEntero();
        System.out.print("  Termino (nombre, cedula, ID, etc.): ");
        String termino = sc.nextLine().trim();

        switch (tipo) {
            case 1: {
                List<Paciente> res = Buscador.buscarPacientes(usuarios, termino);
                System.out.printf("  %d resultado(s):%n", res.size());
                for (Paciente p : res)
                    System.out.printf("  %-12s %-26s %-14s %-12s%n",
                            p.getUsername(), p.getNombres()+" "+p.getApellidos(),
                            p.getCedula(), p.getCodigoHistorial());
                break;
            }
            case 2: {
                List<Medico> res = Buscador.buscarMedicos(usuarios, termino);
                System.out.printf("  %d resultado(s):%n", res.size());
                for (Medico m : res)
                    System.out.printf("  %-12s %-26s %-18s%n",
                            m.getUsername(), m.getNombres()+" "+m.getApellidos(),
                            m.getEspecialidad());
                break;
            }
            case 3: {
                List<Cita> res = Buscador.buscarCitas(citas, termino);
                System.out.printf("  %d resultado(s):%n", res.size());
                cabeceraCitas();
                for (Cita c : res) filaCita(c);
                break;
            }
            case 4: {
                List<Factura> res = Buscador.buscarFacturas(facturas, termino);
                System.out.printf("  %d resultado(s):%n", res.size());
                for (Factura f : res)
                    System.out.printf("  %-8s %-8s %-20s %10.2f %-10s%n",
                            f.idFactura, f.idCita, ab(f.nombrePaciente,20),
                            f.total, f.estadoPago);
                break;
            }
            case 5: {
                List<RecetaMedica> res = Buscador.buscarRecetas(recetas, termino);
                System.out.printf("  %d resultado(s):%n", res.size());
                for (RecetaMedica r : res)
                    System.out.printf("  %-18s %-22s %-12s %s%n",
                            r.codigoReceta, ab(r.nombrePaciente,22),
                            r.fechaEmision, r.estado);
                break;
            }
            case 6: {
                List<HistorialMedico> res = Buscador.buscarHistoriales(historiales, termino);
                System.out.printf("  %d resultado(s):%n", res.size());
                for (HistorialMedico h : res)
                    System.out.printf("  %-12s %-26s %-12s %d consulta(s)%n",
                            h.codigoHistorial, ab(h.nombrePaciente,26),
                            h.cedula, h.entradas.size());
                break;
            }
            default: System.out.println("  Opcion invalida.");
        }
        auditoria("admin","Administrador","BUSQUEDA","Global",String.valueOf(tipo),termino);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: GESTIÓN DE CITAS — ADMIN
    // ════════════════════════════════════════════════════════════
    static void menuGestionCitas() {
        int op;
        do {
            System.out.println("\n─── GESTION DE CITAS ───");
            System.out.println("  1. Ver todas las citas");
            System.out.println("  2. Eliminar cita");
            System.out.println("  3. Filtrar por estado");
            System.out.println("  0. Volver");
            System.out.print("  Opcion: ");
            op = leerEntero();
            switch (op) {
                case 1: verTodasCitas();     break;
                case 2: eliminarCita();      break;
                case 3: filtrarCitasAdmin(); break;
            }
        } while (op != 0);
    }

    static void verTodasCitas() {
        System.out.println("\n─── TODAS LAS CITAS ───");
        if (citas.isEmpty()) { System.out.println("  Sin citas."); return; }
        cabeceraCitas();
        for (int i = 0; i < citas.size(); i++) {
            System.out.printf("  [%4d] ",i);
            filaCita(citas.get(i));
        }
    }

    static void eliminarCita() {
        verTodasCitas();
        if (citas.isEmpty()) return;
        System.out.print("\n  Indice a eliminar (-1 cancela): ");
        int idx = leerEntero();
        if (idx < 0 || idx >= citas.size()) { System.out.println("  Cancelado."); return; }
        Cita c = citas.remove(idx);
        ExcelDB.reescribirCitas(citas);
        System.out.println("  ✓ Cita " + c.idCita + " eliminada.");
        auditoria("admin","Administrador","CITA_ELIMINADA","Cita",c.idCita,"");
    }

    static void filtrarCitasAdmin() {
        System.out.println("  Estado: 1=Confirmada 2=Completada 3=Cancelada 4=No asistio");
        System.out.print("  Seleccione: ");
        int op = leerEntero();
        String[] estados = {"Confirmada","Completada","Cancelada","No asistio"};
        if (op < 1 || op > 4) { System.out.println("  Invalido."); return; }
        String filtro = estados[op-1];
        System.out.printf("  Citas con estado \"%s\":%n", filtro);
        cabeceraCitas();
        for (Cita c : citas)
            if (c.estado.equalsIgnoreCase(filtro)) filaCita(c);
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: HISTORIALES — ADMIN
    // ════════════════════════════════════════════════════════════
    static void verTodosHistoriales() {
        System.out.println("\n─── TODOS LOS HISTORIALES ───");
        if (historiales.isEmpty()) { System.out.println("  Sin historiales."); return; }
        System.out.printf("  %-12s  %-26s  %-12s  %s%n",
                "Codigo","Paciente","Cedula","Consultas");
        sep(62);
        for (HistorialMedico h : historiales)
            System.out.printf("  %-12s  %-26s  %-12s  %d%n",
                    h.codigoHistorial, ab(h.nombrePaciente,26),
                    h.cedula, h.entradas.size());
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: KPIs / MIS
    // ════════════════════════════════════════════════════════════
    static void mostrarKPIs() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║             PANEL MIS — KPIs DEL SISTEMA                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        // ── Contadores de citas ─────────────────────────────────
        int tot=0, comp=0, canc=0, pend=0, noAsis=0, conf=0;
        Map<String,Integer> porEsp = new TreeMap<>();
        Map<String,Integer> porMes = new TreeMap<>();
        Map<String,Integer> porMed = new TreeMap<>();
        for (Cita c : citas) {
            tot++;
            switch (c.estado) {
                case "Completada": comp++;   break;
                case "Cancelada":  canc++;   break;
                case "Pendiente":  pend++;   break;
                case "No asistio": noAsis++; break;
                default:           conf++;   break;
            }
            porEsp.merge(c.especialidad, 1, Integer::sum);
            if (c.fecha != null && c.fecha.length() >= 7)
                porMes.merge(c.fecha.substring(0,7), 1, Integer::sum);
            porMed.merge(c.medicoNombre, 1, Integer::sum);
        }

        double tasaComp = tot>0 ? comp*100.0/tot : 0;
        double tasaCanc = tot>0 ? canc*100.0/tot : 0;
        double tasaNoAs = tot>0 ? noAsis*100.0/tot : 0;

        // ── KPI 1 ───────────────────────────────────────────────
        System.out.println("\n  ┌── KPI 1: Tasa de Citas Completadas ───────────────────");
        System.out.printf("  │  Total citas   : %d%n", tot);
        System.out.printf("  │  Completadas   : %d (%.1f%%)  %s%n",
                comp, tasaComp, barraASCII(tasaComp, 30));
        System.out.printf("  │  Confirmadas   : %d%n", conf);
        System.out.printf("  │  Canceladas    : %d (%.1f%%)%n", canc, tasaCanc);
        System.out.printf("  │  No asistio    : %d (%.1f%%)%n", noAsis, tasaNoAs);
        System.out.printf("  │  Pendientes    : %d%n", pend);
        System.out.println("  │");
        System.out.println(tasaComp < 40
                ? "  │  ⚠ ALERTA: tasa completadas < 40%. Revisar disponibilidad."
                : "  │  ✓ Tasa de completadas en rango aceptable.");
        System.out.println("  └────────────────────────────────────────────────────────");

        // ── KPI 2 ───────────────────────────────────────────────
        System.out.println("\n  ┌── KPI 2: Especialidades con Mayor Demanda ────────────");
        String espTop=""; int espTopN=0;
        for (Map.Entry<String,Integer> e : porEsp.entrySet()) {
            int bl = tot>0?(int)(e.getValue()*30.0/tot):0;
            System.out.printf("  │  %-22s %4d  %s%n",
                    e.getKey(), e.getValue(), barraASCII(e.getValue()*100.0/(tot==0?1:tot),30));
            if (e.getValue()>espTopN){espTopN=e.getValue();espTop=e.getKey();}
        }
        System.out.printf("  │  Lider: %s (%d citas)%n", espTop, espTopN);
        System.out.println("  └────────────────────────────────────────────────────────");

        // ── KPI 3 ───────────────────────────────────────────────
        System.out.println("\n  ┌── KPI 3: Tendencia Mensual ────────────────────────────");
        int maxMes=0;
        for (int v:porMes.values()) if(v>maxMes)maxMes=v;
        int mosM=0;
        for (Map.Entry<String,Integer> e:porMes.entrySet()){
            if(mosM++>=24)break;
            System.out.printf("  │  %s  %4d  %s%n",
                    e.getKey(), e.getValue(), barraASCII(e.getValue()*100.0/(maxMes==0?1:maxMes),25));
        }
        System.out.println("  └────────────────────────────────────────────────────────");

        // ── KPI 4 ───────────────────────────────────────────────
        double totRec=0,totIVA=0; int fPag=0,fPend=0,fAnul=0;
        for (Factura f:facturas){
            if(f.estadoPago.equalsIgnoreCase("Pagada")){totRec+=f.total;totIVA+=f.iva15;fPag++;}
            else if(f.estadoPago.equalsIgnoreCase("Pendiente"))fPend++;
            else fAnul++;
        }
        System.out.println("\n  ┌── KPI 4: Facturacion Global ───────────────────────────");
        System.out.printf("  │  Facturas totales  : %d%n", facturas.size());
        System.out.printf("  │  Pagadas           : %d%n", fPag);
        System.out.printf("  │  Pendientes        : %d%n", fPend);
        System.out.printf("  │  Anuladas          : %d%n", fAnul);
        System.out.printf("  │  Total recaudado   : $%.2f%n", totRec);
        System.out.printf("  │  IVA recaudado     : $%.2f%n", totIVA);
        System.out.printf("  │  Ticket promedio   : $%.2f%n", fPag>0?totRec/fPag:0);
        System.out.printf("  │  Cuentas por cobrar: $%.2f%n", calcularPendiente());
        System.out.println("  └────────────────────────────────────────────────────────");

        // ── KPI 5 ───────────────────────────────────────────────
        System.out.println("\n  ┌── KPI 5: Medicos con mas Citas ────────────────────────");
        List<Map.Entry<String,Integer>> listaM = new ArrayList<>(porMed.entrySet());
        listaM.sort((a,b)->b.getValue()-a.getValue());
        int top = Math.min(10, listaM.size());
        for (int i=0;i<top;i++) {
            Map.Entry<String,Integer> e = listaM.get(i);
            System.out.printf("  │  %2d. %-28s %4d citas%n",(i+1),ab(e.getKey(),28),e.getValue());
        }
        System.out.println("  └────────────────────────────────────────────────────────");

        // ── KPI 6 ───────────────────────────────────────────────
        System.out.println("\n  ┌── KPI 6: Recetas Medicas ──────────────────────────────");
        int rAct=0,rDisp=0,rAnul=0,rVenc=0;
        for(RecetaMedica r:recetas){
            switch(r.estado){
                case "Activa":      rAct++;  break;
                case "Dispensada":  rDisp++; break;
                case "Anulada":     rAnul++; break;
                case "Vencida":     rVenc++; break;
            }
        }
        System.out.printf("  │  Total recetas    : %d%n", recetas.size());
        System.out.printf("  │  Activas          : %d%n", rAct);
        System.out.printf("  │  Dispensadas      : %d%n", rDisp);
        System.out.printf("  │  Anuladas         : %d%n", rAnul);
        System.out.printf("  │  Tasa dispensacion: %.1f%%%n",
                recetas.isEmpty()?0:rDisp*100.0/recetas.size());
        System.out.println("  └────────────────────────────────────────────────────────");

        // ── Resumen ejecutivo ───────────────────────────────────
        System.out.println("\n  ┌── RESUMEN EJECUTIVO ───────────────────────────────────");
        System.out.printf("  │  Pacientes    : %d%n", contarTipo(Paciente.class));
        System.out.printf("  │  Medicos      : %d%n", contarTipo(Medico.class));
        System.out.printf("  │  Historiales  : %d%n", historiales.size());
        System.out.printf("  │  Citas        : %d%n", tot);
        System.out.printf("  │  Recetas      : %d%n", recetas.size());
        System.out.printf("  │  Facturas     : %d%n", facturas.size());
        System.out.printf("  │  Ingresos     : $%.2f%n", totRec);
        System.out.println("  └────────────────────────────────────────────────────────");
    }

    // ════════════════════════════════════════════════════════════
    //  MÓDULO: AUDITORÍA — ADMIN
    // ════════════════════════════════════════════════════════════
    static void verAuditoria() {
        System.out.println("\n─── LOG DE AUDITORIA ───");
        System.out.println("  1. Ultimas 50 entradas");
        System.out.println("  2. Filtrar por usuario");
        System.out.println("  3. Filtrar por accion");
        System.out.print("  Opcion: ");
        int op = leerEntero();

        List<Auditoria> log = ExcelDB.cargarAuditoria();
        List<Auditoria> mostrar = new ArrayList<>();

        if (op == 1) {
            int desde = Math.max(0, log.size()-50);
            for (int i = desde; i < log.size(); i++) mostrar.add(log.get(i));
        } else if (op == 2) {
            System.out.print("  Usuario: ");
            String u = sc.nextLine().trim().toLowerCase();
            for (Auditoria a : log)
                if (a.usuario.toLowerCase().contains(u)) mostrar.add(a);
        } else if (op == 3) {
            System.out.println("  Acciones: LOGIN_OK LOGIN_FAIL REGISTRO_PACIENTE REGISTRO_MEDICO");
            System.out.println("            CITA_CREADA CITA_COMPLETADA CITA_CANCELADA CITA_ELIMINADA");
            System.out.println("            HISTORIAL_CONSULTA HISTORIAL_ENTRADA FACTURA_EMITIDA");
            System.out.println("            FACTURA_ANULADA RECETA_EMITIDA RECETA_DISPENSADA PAGO_REGISTRADO");
            System.out.print("  Accion: ");
            String acc = sc.nextLine().trim().toUpperCase();
            for (Auditoria a : log)
                if (a.accion.equalsIgnoreCase(acc)) mostrar.add(a);
        }

        System.out.printf("  %-20s %-12s %-8s %-22s %-14s %s%n",
                "Timestamp","Usuario","Rol","Accion","Entidad","Detalle");
        sep(100);
        for (Auditoria a : mostrar)
            System.out.printf("  %-20s %-12s %-8s %-22s %-14s %s%n",
                    a.timestamp, ab(a.usuario,12), ab(a.rol,8),
                    a.accion, ab(a.entidad,14), ab(a.detalle,40));
        System.out.printf("  Mostrando %d entrada(s).%n", mostrar.size());
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS INTERNOS
    // ════════════════════════════════════════════════════════════
    static void imprimirHistorial(HistorialMedico h) {
        System.out.println("\n  ╔══════════════════════════════════════════════════╗");
        System.out.println("  ║              HISTORIAL MEDICO                    ║");
        System.out.println("  ╠══════════════════════════════════════════════════╣");
        System.out.printf ("  ║  Codigo   : %-36s║%n", h.codigoHistorial);
        System.out.printf ("  ║  Paciente : %-36s║%n", ab(h.nombrePaciente,36));
        System.out.printf ("  ║  Cedula   : %-36s║%n", h.cedula);
        System.out.printf ("  ║  Nac.     : %-36s║%n", h.fechaNacimiento);
        System.out.printf ("  ║  Consultas: %-36s║%n", h.entradas.size());
        System.out.println("  ╚══════════════════════════════════════════════════╝");
        if (h.entradas.isEmpty()) { System.out.println("  Sin consultas."); return; }
        int n = 1;
        for (HistorialMedico.EntradaHistorial e : h.entradas) {
            System.out.println("\n  ┌── Consulta #" + n++ + " ──────────────────────────────");
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

    static void cabeceraCitas() {
        System.out.printf("  %-8s %-16s %-20s %-16s %-12s %-6s %-12s%n",
                "ID","Paciente","Medico","Especialidad","Fecha","Hora","Estado");
        sep(94);
    }

    static void filaCita(Cita c) {
        System.out.printf("  %-8s %-16s %-20s %-16s %-12s %-6s %-12s%n",
                c.idCita, ab(c.nombrePaciente,16), ab(c.medicoNombre,20),
                ab(c.especialidad,16), c.fecha, c.hora, c.estado);
    }

    static String barraASCII(double pct, int ancho) {
        int lleno = (int)(pct * ancho / 100);
        return "█".repeat(Math.max(0,lleno)) + "░".repeat(Math.max(0,ancho-lleno))
                + String.format(" %.1f%%", pct);
    }

    static void sep(int n) { System.out.println("  " + "─".repeat(n)); }

    static double calcularPendiente() {
        double t = 0;
        for (Factura f : facturas)
            if (f.estadoPago.equalsIgnoreCase("Pendiente")) t += f.total;
        return t;
    }

    static List<Medico> getMedicosPorEspecialidad(String esp) {
        List<Medico> lista = new ArrayList<>();
        for (Usuario u : usuarios)
            if (u instanceof Medico && ((Medico)u).getEspecialidad().equalsIgnoreCase(esp))
                lista.add((Medico)u);
        return lista;
    }

    static String elegirEspecialidad() {
        Set<String> set = new LinkedHashSet<>();
        for (Usuario u : usuarios)
            if (u instanceof Medico) set.add(((Medico)u).getEspecialidad());
        if (set.isEmpty()) Collections.addAll(set, ESPECIALIDADES_BASE);
        List<String> lista = new ArrayList<>(set);
        System.out.println("\n  Especialidades:");
        for (int i = 0; i < lista.size(); i++)
            System.out.printf("    %2d. %s%n",(i+1),lista.get(i));
        int op; while(true){
            System.out.print("  Seleccione: "); op = leerEntero();
            if (op >= 1 && op <= lista.size()) break;
            System.out.println("  Invalido.");
        }
        return lista.get(op-1);
    }

    static List<String> horasLibres(String medUser, String fecha) {
        Set<String> ocup = new HashSet<>();
        for (Cita c : citas)
            if (c.medicoUsername.equalsIgnoreCase(medUser) && c.fecha.equals(fecha))
                ocup.add(c.hora);
        List<String> lib = new ArrayList<>();
        for (String h : HORAS) if (!ocup.contains(h)) lib.add(h);
        return lib;
    }

    static HistorialMedico buscarHistorial(String user) {
        for (HistorialMedico h : historiales)
            if (h.usernamePaciente.equalsIgnoreCase(user)) return h;
        return null;
    }

    static HistorialMedico buscarHistorialPorCodigo(String cod) {
        for (HistorialMedico h : historiales)
            if (h.codigoHistorial.equalsIgnoreCase(cod)) return h;
        return null;
    }

    static String obtenerCedulaPaciente(String username) {
        for (Usuario u : usuarios)
            if (u instanceof Paciente && u.getUsername().equalsIgnoreCase(username))
                return ((Paciente)u).getCedula();
        return "";
    }

    static boolean existeUsername(String u) {
        for (Usuario usr : usuarios)
            if (usr.getUsername().equalsIgnoreCase(u)) return true;
        return false;
    }

    static boolean existeCorreo(String c) {
        for (Usuario u : usuarios)
            if (u.getCorreo().equalsIgnoreCase(c)) return true;
        return false;
    }

    static boolean existeCedula(String ced) {
        for (Usuario u : usuarios) {
            if (u instanceof Paciente && ((Paciente)u).getCedula().equals(ced)) return true;
            if (u instanceof Medico   && ((Medico)u).getCedula().equals(ced))   return true;
        }
        return false;
    }

    static boolean existeCodigoHistorial(String cod) {
        for (HistorialMedico h : historiales)
            if (h.codigoHistorial.equals(cod)) return true;
        return false;
    }

    static void cargarDatosGuardados() {
        for (Paciente p : ExcelDB.cargarPacientes()) usuarios.add(p);
        for (Medico   m : ExcelDB.cargarMedicos())   usuarios.add(m);
        citas.addAll(ExcelDB.cargarCitas());
        historiales.addAll(ExcelDB.cargarHistoriales());
        facturas.addAll(ExcelDB.cargarFacturas());
        recetas.addAll(ExcelDB.cargarRecetas());
        notas.addAll(ExcelDB.cargarNotasCredito());
        System.out.printf(
            "  [DB] Pacientes:%d | Medicos:%d | Citas:%d | Historiales:%d | Facturas:%d | Recetas:%d%n",
            contarTipo(Paciente.class), contarTipo(Medico.class),
            citas.size(), historiales.size(), facturas.size(), recetas.size());
    }

    static int contarTipo(Class<?> t) {
        int n = 0;
        for (Usuario u : usuarios) if (t.isInstance(u)) n++;
        return n;
    }

    static String rolDe(Usuario u) {
        if (u instanceof Paciente)      return "Paciente";
        if (u instanceof Medico)        return "Medico";
        if (u instanceof Administrador) return "Administrador";
        return "Sistema";
    }

    static void auditoria(String user, String rol, String accion,
                          String entidad, String idEnt, String detalle) {
        String ts = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(TS_FMT));
        Auditoria a = new Auditoria(ts, user, rol, accion, entidad, idEnt, detalle);
        ExcelDB.registrarAuditoria(a);
    }

    static String pedir(String prompt, java.util.function.Predicate<String> val, String err) {
        while (true) {
            System.out.print("  " + prompt);
            String v = sc.nextLine().trim();
            if (val.test(v)) return v;
            System.out.println(err);
        }
    }

    /** Lee una línea; si está vacía devuelve defaultVal */
    static String leer(String defaultVal) {
        String v = sc.nextLine().trim();
        return v.isEmpty() ? defaultVal : v;
    }

    static int leerEntero() {
        while (true) {
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.print("  Ingrese un numero: "); }
        }
    }

    static String ab(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max-1)+"." : s;
    }
}
