import java.util.ArrayList;
import java.util.List;

/**
 * Buscador — utilidad de búsqueda centralizada.
 * Permite buscar por nombre, cédula, username, código de historial,
 * ID de cita, ID de factura, o código de receta.
 * Retorna listas de resultados tipados.
 */
public class Buscador {

    /** Busca pacientes por nombre parcial, cédula o username */
    public static List<Paciente> buscarPacientes(List<Usuario> usuarios, String termino) {
        List<Paciente> resultados = new ArrayList<>();
        String t = normalizar(termino);
        for (Usuario u : usuarios) {
            if (!(u instanceof Paciente)) continue;
            Paciente p = (Paciente) u;
            if (normalizar(p.getNombres()).contains(t)
                    || normalizar(p.getApellidos()).contains(t)
                    || normalizar(p.getUsername()).contains(t)
                    || p.getCedula().contains(t)
                    || normalizar(p.getCodigoHistorial()).contains(t)) {
                resultados.add(p);
            }
        }
        return resultados;
    }

    /** Busca médicos por nombre parcial, cédula, username o especialidad */
    public static List<Medico> buscarMedicos(List<Usuario> usuarios, String termino) {
        List<Medico> resultados = new ArrayList<>();
        String t = normalizar(termino);
        for (Usuario u : usuarios) {
            if (!(u instanceof Medico)) continue;
            Medico m = (Medico) u;
            if (normalizar(m.getNombres()).contains(t)
                    || normalizar(m.getApellidos()).contains(t)
                    || normalizar(m.getUsername()).contains(t)
                    || m.getCedula().contains(t)
                    || normalizar(m.getEspecialidad()).contains(t)) {
                resultados.add(m);
            }
        }
        return resultados;
    }

    /** Busca citas por ID, nombre de paciente, médico, fecha o estado */
    public static List<Cita> buscarCitas(List<Cita> citas, String termino) {
        List<Cita> resultados = new ArrayList<>();
        String t = normalizar(termino);
        for (Cita c : citas) {
            if (normalizar(c.idCita).contains(t)
                    || normalizar(c.nombrePaciente).contains(t)
                    || normalizar(c.medicoNombre).contains(t)
                    || normalizar(c.especialidad).contains(t)
                    || normalizar(c.fecha).contains(t)
                    || normalizar(c.estado).contains(t)
                    || normalizar(c.paciente).contains(t)) {
                resultados.add(c);
            }
        }
        return resultados;
    }

    /** Busca facturas por ID, paciente, médico o estado */
    public static List<Factura> buscarFacturas(List<Factura> facturas, String termino) {
        List<Factura> resultados = new ArrayList<>();
        String t = normalizar(termino);
        for (Factura f : facturas) {
            if (normalizar(f.idFactura).contains(t)
                    || normalizar(f.nombrePaciente).contains(t)
                    || normalizar(f.usernamePaciente).contains(t)
                    || normalizar(f.especialidad).contains(t)
                    || normalizar(f.estadoPago).contains(t)
                    || normalizar(f.idCita).contains(t)) {
                resultados.add(f);
            }
        }
        return resultados;
    }

    /** Busca recetas por código, paciente, médico o estado */
    public static List<RecetaMedica> buscarRecetas(List<RecetaMedica> recetas, String termino) {
        List<RecetaMedica> resultados = new ArrayList<>();
        String t = normalizar(termino);
        for (RecetaMedica r : recetas) {
            if (normalizar(r.codigoReceta).contains(t)
                    || normalizar(r.nombrePaciente).contains(t)
                    || normalizar(r.usernamePaciente).contains(t)
                    || normalizar(r.medicoNombre).contains(t)
                    || normalizar(r.estado).contains(t)
                    || r.cedulaPaciente.contains(t)) {
                resultados.add(r);
            }
        }
        return resultados;
    }

    /** Busca historial por código, nombre o cédula */
    public static List<HistorialMedico> buscarHistoriales(
            List<HistorialMedico> historiales, String termino) {
        List<HistorialMedico> resultados = new ArrayList<>();
        String t = normalizar(termino);
        for (HistorialMedico h : historiales) {
            if (normalizar(h.codigoHistorial).contains(t)
                    || normalizar(h.nombrePaciente).contains(t)
                    || normalizar(h.usernamePaciente).contains(t)
                    || h.cedula.contains(t)) {
                resultados.add(h);
            }
        }
        return resultados;
    }

    /** Normaliza texto: minúsculas y sin tildes para búsqueda tolerante */
    private static String normalizar(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ñ","n")
                .replace("ü","u");
    }
}