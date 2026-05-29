import java.io.*;
import java.time.LocalDate;
import java.util.*;

/**
 * ExcelDB — capa de persistencia CSV completa.
 * Maneja: pacientes, médicos, citas, historiales, entradas,
 *         facturas, recetas, items_receta, notas_credito, auditoría.
 * Sin librerías externas.
 */
public class ExcelDB {

    // ── Ruta base: carpeta raíz del proyecto (donde están los CSV con datos) ──
    /**
     * Detecta automáticamente la carpeta raíz del proyecto.
     * Sube un nivel desde /src si el directorio de trabajo está dentro de src/.
     * Así el programa encuentra los CSV sin importar desde dónde se ejecute.
     */
    static final String BASE;
    static {
        String cwd = System.getProperty("user.dir");
        // Si estamos dentro de .../src, subir un nivel
        if (cwd.endsWith(File.separator + "src") || cwd.endsWith("/src")) {
            BASE = cwd + File.separator + ".." + File.separator;
        } else {
            BASE = cwd + File.separator;
        }
    }

    // ── Archivos ────────────────────────────────────────────────
    static final String F_PACIENTES    = BASE + "pacientes.csv";
    static final String F_MEDICOS      = BASE + "medicos.csv";
    static final String F_CITAS        = BASE + "citas.csv";
    static final String F_HISTORIALES  = BASE + "historiales.csv";
    static final String F_ENTRADAS     = BASE + "entradas_historial.csv";
    static final String F_FACTURAS     = BASE + "facturas.csv";
    static final String F_RECETAS      = BASE + "recetas.csv";
    static final String F_ITEMS_RECETA = BASE + "items_receta.csv";
    static final String F_NOTAS_CRED   = BASE + "notas_credito.csv";
    static final String F_AUDITORIA    = BASE + "auditoria.csv";
    static final String F_ADMIN        = BASE + "admin.csv";

    // ── Encabezados ─────────────────────────────────────────────
    static final String H_PACIENTES   = "ID,Nombres,Apellidos,Username,Correo,Cedula,Telefono,FechaNacimiento,CodigoHistorial,FechaRegistro";
    static final String H_MEDICOS     = "ID,Nombres,Apellidos,Username,Correo,Cedula,Telefono,Especialidad,FechaRegistro";
    static final String H_CITAS       = "ID_Cita,Username_Paciente,Nombre_Paciente,Medico_Username,Medico_Nombre,Especialidad,Fecha,Hora,Estado,MotivoCancel,FechaAgendado";
    static final String H_HISTORIALES = "CodigoHistorial,Username_Paciente,Nombre_Paciente,FechaNacimiento,Cedula,FechaCreacion";
    static final String H_ENTRADAS    = "CodigoHistorial,Fecha,Medico_Username,Medico_Nombre,Especialidad,MotivoConsulta,Diagnostico,Tratamiento,Observaciones";
    static final String H_FACTURAS    = "ID_Factura,ID_Cita,Username_Paciente,Nombre_Paciente,Medico_Username,Especialidad,Fecha_Cita,Subtotal,IVA_15,Total,MetodoPago,Estado_Pago,FechaEmision";
    static final String H_RECETAS     = "CodigoReceta,ID_Cita,CodigoHistorial,Username_Paciente,Nombre_Paciente,Cedula_Paciente,Medico_Username,Medico_Nombre,Especialidad,FechaEmision,FechaVencimiento,Estado,Diagnostico,Observaciones";
    static final String H_ITEMS       = "CodigoReceta,Medicamento,Dosis,Via,Frecuencia,Duracion,Instrucciones";
    static final String H_NOTAS       = "ID_Nota,ID_Factura,ID_Cita,Username_Paciente,Nombre_Paciente,Motivo,MontoAnulado,FechaEmision,EmitidoPor,Estado";
    static final String H_AUDITORIA   = "Timestamp,Usuario,Rol,Accion,Entidad,ID_Entidad,Detalle";

    // ════════════════════════════════════════════════════════════
    //  INICIALIZAR
    // ════════════════════════════════════════════════════════════
    public static void inicializar() {
        crearSiNoExiste(F_PACIENTES,   H_PACIENTES);
        crearSiNoExiste(F_MEDICOS,     H_MEDICOS);
        crearSiNoExiste(F_CITAS,       H_CITAS);
        crearSiNoExiste(F_HISTORIALES, H_HISTORIALES);
        crearSiNoExiste(F_ENTRADAS,    H_ENTRADAS);
        crearSiNoExiste(F_FACTURAS,    H_FACTURAS);
        crearSiNoExiste(F_RECETAS,     H_RECETAS);
        crearSiNoExiste(F_ITEMS_RECETA,H_ITEMS);
        crearSiNoExiste(F_NOTAS_CRED,  H_NOTAS);
        crearSiNoExiste(F_AUDITORIA,   H_AUDITORIA);
    }

    static void crearSiNoExiste(String archivo, String encabezado) {
        File f = new File(archivo);
        if (!f.exists()) {
            try (FileWriter fw = new FileWriter(f)) { fw.write(encabezado + "\n"); }
            catch (IOException e) { System.err.println("Error creando " + archivo); }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  GUARDAR
    // ════════════════════════════════════════════════════════════
    public static void guardarPaciente(Paciente p) {
        try (FileWriter fw = new FileWriter(F_PACIENTES, true)) {
            int id = contarRegistros(F_PACIENTES) + 1;
            fw.write(String.format("P%05d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    id, esc(p.getNombres()), esc(p.getApellidos()),
                    esc(p.getUsername()), esc(p.getCorreo()),
                    p.getCedula(), p.getTelefono(),
                    p.getFechaNacimiento(), p.getCodigoHistorial(), LocalDate.now()));
        } catch (IOException e) { System.err.println("Error guardando paciente."); }
    }

    public static void guardarMedico(Medico m) {
        try (FileWriter fw = new FileWriter(F_MEDICOS, true)) {
            int id = contarRegistros(F_MEDICOS) + 1;
            fw.write(String.format("M%04d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    id, esc(m.getNombres()), esc(m.getApellidos()),
                    esc(m.getUsername()), esc(m.getCorreo()),
                    m.getCedula(), m.getTelefono(),
                    esc(m.getEspecialidad()), LocalDate.now()));
        } catch (IOException e) { System.err.println("Error guardando médico."); }
    }

    public static void guardarCita(Cita c) {
        try (FileWriter fw = new FileWriter(F_CITAS, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    c.idCita, esc(c.paciente), esc(c.nombrePaciente),
                    esc(c.medicoUsername), esc(c.medicoNombre),
                    esc(c.especialidad), c.fecha, c.hora,
                    c.estado, esc(c.motivoCancel), LocalDate.now()));
        } catch (IOException e) { System.err.println("Error guardando cita."); }
    }

    public static void guardarHistorial(HistorialMedico h) {
        try (FileWriter fw = new FileWriter(F_HISTORIALES, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s%n",
                    h.codigoHistorial, esc(h.usernamePaciente),
                    esc(h.nombrePaciente), h.fechaNacimiento,
                    h.cedula, LocalDate.now()));
        } catch (IOException e) { System.err.println("Error guardando historial."); }
    }

    public static void guardarEntrada(String cod, HistorialMedico.EntradaHistorial e) {
        try (FileWriter fw = new FileWriter(F_ENTRADAS, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    cod, e.fecha, esc(e.medico), esc(e.nombreMedico),
                    esc(e.especialidad), esc(e.motivoConsulta),
                    esc(e.diagnostico), esc(e.tratamiento), esc(e.observaciones)));
        } catch (IOException ex) { System.err.println("Error guardando entrada."); }
    }

    public static void guardarFactura(Factura f) {
        try (FileWriter fw = new FileWriter(F_FACTURAS, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%s,%s,%s%n",
                    f.idFactura, f.idCita, esc(f.usernamePaciente),
                    esc(f.nombrePaciente), esc(f.medicoUsername),
                    esc(f.especialidad), f.fechaCita,
                    f.subtotal, f.iva15, f.total,
                    esc(f.metodoPago), f.estadoPago, f.fechaEmision));
        } catch (IOException e) { System.err.println("Error guardando factura."); }
    }

    public static void guardarReceta(RecetaMedica r) {
        try (FileWriter fw = new FileWriter(F_RECETAS, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    r.codigoReceta, r.idCita, r.codigoHistorial,
                    esc(r.usernamePaciente), esc(r.nombrePaciente), r.cedulaPaciente,
                    esc(r.medicoUsername), esc(r.medicoNombre), esc(r.especialidad),
                    r.fechaEmision, r.fechaVencimiento, r.estado,
                    esc(r.diagnostico), esc(r.observaciones)));
        } catch (IOException e) { System.err.println("Error guardando receta."); }
        // Guardar items
        for (RecetaMedica.ItemReceta it : r.items) guardarItemReceta(r.codigoReceta, it);
    }

    public static void guardarItemReceta(String codigoReceta, RecetaMedica.ItemReceta it) {
        try (FileWriter fw = new FileWriter(F_ITEMS_RECETA, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s,%s%n",
                    codigoReceta, esc(it.medicamento), esc(it.dosis),
                    esc(it.via), esc(it.frecuencia), esc(it.duracion),
                    esc(it.instrucciones)));
        } catch (IOException e) { System.err.println("Error guardando item receta."); }
    }

    public static void guardarNotaCredito(NotaCredito nc) {
        try (FileWriter fw = new FileWriter(F_NOTAS_CRED, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s,%.2f,%s,%s,%s%n",
                    nc.idNotaCredito, nc.idFactura, nc.idCita,
                    esc(nc.usernamePaciente), esc(nc.nombrePaciente),
                    esc(nc.motivo), nc.montoAnulado,
                    nc.fechaEmision, esc(nc.emitidoPor), nc.estado));
        } catch (IOException e) { System.err.println("Error guardando nota crédito."); }
    }

    public static void registrarAuditoria(Auditoria a) {
        try (FileWriter fw = new FileWriter(F_AUDITORIA, true)) {
            fw.write(String.format("%s,%s,%s,%s,%s,%s,%s%n",
                    a.timestamp, esc(a.usuario), esc(a.rol),
                    a.accion, esc(a.entidad), esc(a.idEntidad), esc(a.detalle)));
        } catch (IOException e) { /* auditoría nunca interrumpe el flujo */ }
    }

    public static void guardarCredencial(String archivo, String username, String credencial) {
        try (FileWriter fw = new FileWriter(BASE + archivo, true)) {
            fw.write(username + "|" + credencial + "\n");
        } catch (IOException e) { System.err.println("Error guardando credencial."); }
    }

    // ════════════════════════════════════════════════════════════
    //  ACTUALIZAR ESTADO EN CSV (reescribir sección específica)
    // ════════════════════════════════════════════════════════════
    public static void reescribirCitas(List<Cita> citas) {
        try (FileWriter fw = new FileWriter(F_CITAS)) {
            fw.write(H_CITAS + "\n");
            for (Cita c : citas)
                fw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        c.idCita, esc(c.paciente), esc(c.nombrePaciente),
                        esc(c.medicoUsername), esc(c.medicoNombre),
                        esc(c.especialidad), c.fecha, c.hora,
                        c.estado, esc(c.motivoCancel), LocalDate.now()));
        } catch (IOException e) { System.err.println("Error reescribiendo citas."); }
    }

    public static void reescribirFacturas(List<Factura> facturas) {
        try (FileWriter fw = new FileWriter(F_FACTURAS)) {
            fw.write(H_FACTURAS + "\n");
            for (Factura f : facturas)
                fw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%s,%s,%s%n",
                        f.idFactura, f.idCita, esc(f.usernamePaciente),
                        esc(f.nombrePaciente), esc(f.medicoUsername),
                        esc(f.especialidad), f.fechaCita,
                        f.subtotal, f.iva15, f.total,
                        esc(f.metodoPago), f.estadoPago, f.fechaEmision));
        } catch (IOException e) { System.err.println("Error reescribiendo facturas."); }
    }

    public static void reescribirRecetas(List<RecetaMedica> recetas) {
        try (FileWriter fw = new FileWriter(F_RECETAS)) {
            fw.write(H_RECETAS + "\n");
            for (RecetaMedica r : recetas)
                fw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        r.codigoReceta, r.idCita, r.codigoHistorial,
                        esc(r.usernamePaciente), esc(r.nombrePaciente), r.cedulaPaciente,
                        esc(r.medicoUsername), esc(r.medicoNombre), esc(r.especialidad),
                        r.fechaEmision, r.fechaVencimiento, r.estado,
                        esc(r.diagnostico), esc(r.observaciones)));
        } catch (IOException e) { System.err.println("Error reescribiendo recetas."); }
    }

    // ════════════════════════════════════════════════════════════
    //  CARGAR
    // ════════════════════════════════════════════════════════════
    public static List<Paciente> cargarPacientes() {
        List<Paciente> lista = new ArrayList<>();
        Map<String,String> creds = cargarCredenciales("pacientes.cred");
        try (BufferedReader br = br(F_PACIENTES)) {
            br.readLine();
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length < 10) continue;
                lista.add(new Paciente(f[3],f[4],creds.getOrDefault(f[3],""),
                        f[1],f[2],f[5],f[6],f[7],f[8]));
            }
        } catch (IOException ignored) {}
        return lista;
    }

    public static List<Medico> cargarMedicos() {
        List<Medico> lista = new ArrayList<>();
        Map<String,String> creds = cargarCredenciales("medicos.cred");
        try (BufferedReader br = br(F_MEDICOS)) {
            br.readLine();
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length < 9) continue;
                lista.add(new Medico(f[3],f[4],creds.getOrDefault(f[3],""),
                        f[1],f[2],f[5],f[6],f[7]));
            }
        } catch (IOException ignored) {}
        return lista;
    }

    public static List<Cita> cargarCitas() {
        List<Cita> lista = new ArrayList<>();
        try (BufferedReader br = br(F_CITAS)) {
            br.readLine();
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length < 9) continue;
                Cita c = new Cita(f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8]);
                if (f.length >= 10) c.motivoCancel = f[9];
                lista.add(c);
            }
        } catch (IOException ignored) {}
        return lista;
    }

    public static List<HistorialMedico> cargarHistoriales() {
        List<HistorialMedico> lista = new ArrayList<>();
        Map<String,HistorialMedico> mapa = new LinkedHashMap<>();
        try (BufferedReader br = br(F_HISTORIALES)) {
            br.readLine();
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length < 5) continue;
                HistorialMedico h = new HistorialMedico(f[0],f[1],f[2],f[3],f[4]);
                lista.add(h);
                mapa.put(f[0], h);
            }
        } catch (IOException ignored) {}
        try (BufferedReader br = br(F_ENTRADAS)) {
            br.readLine();
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length < 9) continue;
                HistorialMedico h = mapa.get(f[0]);
                if (h != null)
                    h.agregarEntrada(new HistorialMedico.EntradaHistorial(
                            f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8]));
            }
        } catch (IOException ignored) {}
        return lista;
    }

    public static List<Factura> cargarFacturas() {
        List<Factura> lista = new ArrayList<>();
        try (BufferedReader br = br(F_FACTURAS)) {
            br.readLine();
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length < 13) continue;
                try {
                    double sub = Double.parseDouble(f[7]);
                    Factura fac = new Factura(f[0],f[1],f[2],f[3],f[4],f[5],f[6],
                            sub,f[10],f[11],f[12]);
                    lista.add(fac);
                } catch (NumberFormatException ignored2) {}
            }
        } catch (IOException ignored) {}
        return lista;
    }

    public static List<RecetaMedica> cargarRecetas() {
        List<RecetaMedica> lista = new ArrayList<>();
        Map<String,RecetaMedica> mapa = new LinkedHashMap<>();
        try (BufferedReader br = br(F_RECETAS)) {
            br.readLine();
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length < 14) continue;
                RecetaMedica r = new RecetaMedica(f[0],f[1],f[2],f[3],f[4],f[5],
                        f[6],f[7],f[8],f[9],f[10],f[11],f[12],f[13]);
                lista.add(r);
                mapa.put(f[0], r);
            }
        } catch (IOException ignored) {}
        try (BufferedReader br = br(F_ITEMS_RECETA)) {
            br.readLine();
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length < 7) continue;
                RecetaMedica r = mapa.get(f[0]);
                if (r != null)
                    r.agregarItem(new RecetaMedica.ItemReceta(f[1],f[2],f[3],f[4],f[5],f[6]));
            }
        } catch (IOException ignored) {}
        return lista;
    }

    public static List<NotaCredito> cargarNotasCredito() {
        List<NotaCredito> lista = new ArrayList<>();
        try (BufferedReader br = br(F_NOTAS_CRED)) {
            br.readLine();
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length < 10) continue;
                try {
                    NotaCredito nc = new NotaCredito(f[0],f[1],f[2],f[3],f[4],f[5],
                            Double.parseDouble(f[6]),f[7],f[8]);
                    nc.estado = f[9];
                    lista.add(nc);
                } catch (NumberFormatException ignored2) {}
            }
        } catch (IOException ignored) {}
        return lista;
    }

    public static List<Auditoria> cargarAuditoria() {
        List<Auditoria> lista = new ArrayList<>();
        try (BufferedReader br = br(F_AUDITORIA)) {
            br.readLine();
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length < 7) continue;
                lista.add(new Auditoria(f[0],f[1],f[2],f[3],f[4],f[5],f[6]));
            }
        } catch (IOException ignored) {}
        return lista;
    }

    // ════════════════════════════════════════════════════════════
    //  GENERADORES DE ID
    // ════════════════════════════════════════════════════════════

    /** Obtiene el número máximo de IDs existentes en un archivo CSV para la columna 0 con prefijo dado. */
    static int maxIdNumerico(String archivo, String prefijo) {
        int max = 0;
        try (BufferedReader br = br(archivo)) {
            br.readLine(); // saltar encabezado
            String ln;
            while ((ln = br.readLine()) != null) {
                String[] f = csv(ln);
                if (f.length > 0 && f[0].toUpperCase().startsWith(prefijo.toUpperCase())) {
                    try {
                        int n = Integer.parseInt(f[0].substring(prefijo.length()));
                        if (n > max) max = n;
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException ignored) {}
        return max;
    }

    public static String generarIdCita()    { return String.format("C%05d",  maxIdNumerico(F_CITAS,     "C")  + 1); }
    public static String generarIdFactura() { return String.format("F%05d",  maxIdNumerico(F_FACTURAS,  "F")  + 1); }
    public static String generarIdNota()    { return String.format("NC%05d", maxIdNumerico(F_NOTAS_CRED,"NC") + 1); }
    public static String generarIdReceta()  {
        String fecha = LocalDate.now().toString();
        return RecetaMedica.generarCodigo(fecha, contarRegistros(F_RECETAS) + 1);
    }

    // ════════════════════════════════════════════════════════════
    //  CREDENCIALES (con soporte hash)
    // ════════════════════════════════════════════════════════════
    static Map<String,String> cargarCredenciales(String archivo) {
        Map<String,String> map = new LinkedHashMap<>();
        File f = new File(BASE + archivo);
        if (!f.exists()) return map;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f),"UTF-8"))) {
            String ln;
            while ((ln = br.readLine()) != null) {
                ln = ln.trim();
                // Separador: primera '|' (nuevo formato) o primera '=' (legado sin hash)
                int idx = ln.indexOf('|');
                if (idx > 0) {
                    map.put(ln.substring(0,idx).trim(), ln.substring(idx+1).trim());
                } else {
                    // legado: username=textoPlano (sin ':' en el valor)
                    idx = ln.indexOf('=');
                    if (idx > 0) map.put(ln.substring(0,idx).trim(), ln.substring(idx+1).trim());
                }
            }
        } catch (IOException ignored) {}
        return map;
    }

    /** Actualiza o agrega credencial en el archivo (reemplaza si ya existe) */
    public static void actualizarCredencial(String archivo, String username, String credencial) {
        Map<String,String> mapa = cargarCredenciales(archivo);
        mapa.put(username, credencial);
        try (FileWriter fw = new FileWriter(BASE + archivo)) {
            for (Map.Entry<String,String> e : mapa.entrySet())
                fw.write(e.getKey() + "|" + e.getValue() + "\n");
        } catch (IOException e) { System.err.println("Error actualizando credencial."); }
    }

    // ════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ════════════════════════════════════════════════════════════
    static int contarRegistros(String archivo) {
        int cnt = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            br.readLine();
            while (br.readLine() != null) cnt++;
        } catch (IOException ignored) {}
        return cnt;
    }

    static BufferedReader br(String archivo) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(archivo),"UTF-8"));
    }

    static String esc(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"","\"\"") + "\"";
        return v;
    }

    static String[] csv(String linea) {
        if (linea == null) return new String[0];
        linea = linea.replace("\r", ""); // strip Windows line endings
        List<String> campos = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (q && i+1 < linea.length() && linea.charAt(i+1)=='"') { sb.append('"'); i++; }
                else q = !q;
            } else if (c == ',' && !q) { campos.add(sb.toString()); sb.setLength(0); }
            else sb.append(c);
        }
        campos.add(sb.toString());
        return campos.toArray(new String[0]);
    }
}