import java.util.*;
import java.time.LocalDate;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    static ArrayList<Usuario> usuarios = new ArrayList<>();
    static ArrayList<Cita> citas = new ArrayList<>();
    static Scanner sc = new Scanner(System.in);

    static String[] HORAS = {
            "08:00","09:00","10:00","11:00",
            "12:00","13:00","14:00","15:00","16:00","17:00"
    };

    static String[] ESPECIALIDADES = {
            "Neurologia", "Medico General", "Pediatria",
            "Ginecologia", "Cardiologia", "Psicologia", "Traumatologia"
    };

    static Map<String, List<String>> doctores = new HashMap<>();

    public static void main(String[] args) {

        cargarDoctores();
        usuarios.add(new Administrador("admin", "123"));

        int opcion;

        do {
            System.out.println("\n1. Login");
            System.out.println("2. Registrarse");
            System.out.println("3. Salir");
            opcion = sc.nextInt();
            sc.nextLine();

            switch(opcion) {
                case 1: login(); break;
                case 2: registrar(); break;
            }

        } while(opcion != 3);
    }

    // LOGIN
    public static void login() {
        System.out.print("Usuario: ");
        String user = sc.nextLine();

        System.out.print("Contraseña: ");
        String pass = sc.nextLine();

        for (Usuario u : usuarios) {
            if (u.getUsername().equals(user) && u.login(pass)) {
                System.out.println("Bienvenido " + user);
                menuUsuario(u);
                return;
            }
        }

        System.out.println("Credenciales incorrectas");
    }

    //  REGISTRO (VALIDADO)
    public static void registrar() {
        System.out.println("1. Paciente");
        System.out.println("2. Medico");
        int tipo = sc.nextInt();
        sc.nextLine();

        // 👤 usuario
        System.out.print("Usuario: ");
        String user = sc.nextLine();
        while(!validarUsuario(user)) {
            System.out.println("Error, solo letras:");
            user = sc.nextLine();
        }

        //  contraseña
        System.out.print("Contraseña (5-10): ");
        String pass = sc.nextLine();
        while(!validarPassword(pass)) {
            System.out.println("Error contraseña:");
            pass = sc.nextLine();
        }

        if (tipo == 1) {
            usuarios.add(new Paciente(user, pass));
            guardarUsuario("Paciente", user + "," + pass);
        } else {
            String esp = elegirEspecialidad();
            usuarios.add(new Medico(user, pass, esp));
            guardarUsuario("Medico", user + "," + pass + "," + esp);
        }

        System.out.println("Registro exitoso");
    }

    // MENÚ
    public static void menuUsuario(Usuario u) {
        int op;

        do {
            u.menu();
            System.out.println("0. Salir");
            op = sc.nextInt();
            sc.nextLine();

            if (u instanceof Paciente) {
                if (op == 1) agendarCita((Paciente) u);
                if (op == 2) verCitasPaciente(u.getUsername());
            }

            if (u instanceof Medico) {
                if (op == 1) verCitasMedico(u.getUsername());
            }

            if (u instanceof Administrador) {
                if (op == 1) verTodasCitas();
                if (op == 2) eliminarCita();
            }

        } while(op != 0);
    }

    //  AGENDAR CITA (VALIDADO FULL)
    public static void agendarCita(Paciente p) {

        String esp = elegirEspecialidad();
        String medico = elegirMedico(esp);

        //  fecha cita
        System.out.print("Fecha (YYYY-MM-DD): ");
        String fecha = sc.nextLine();
        while(!validarFecha(fecha)) {
            System.out.println("Fecha inválida:");
            fecha = sc.nextLine();
        }

        //  hora cita
        System.out.println("Horas disponibles:");
        for (int i = 0; i < HORAS.length; i++) {
            System.out.println((i+1) + ". " + HORAS[i]);
        }

        int op = sc.nextInt();
        sc.nextLine();

        while(op < 1 || op > HORAS.length) {
            System.out.println("Opción inválida:");
            op = sc.nextInt();
            sc.nextLine();
        }

        String hora = HORAS[op-1];

        Cita c = new Cita(p.getUsername(), medico, esp, fecha, hora);
        citas.add(c);

        guardarCita(c);

        System.out.println("Cita agendada");
    }

    //  VISUALIZACIÓN
    public static void verCitasPaciente(String user) {
        for (Cita c : citas) {
            if (c.paciente.equals(user)) {
                System.out.println(c.medico + " - " + c.fecha + " - " + c.hora);
            }
        }
    }

    public static void verCitasMedico(String user) {
        for (Cita c : citas) {
            if (c.medico.equals(user)) {
                System.out.println(c.paciente + " - " + c.fecha + " - " + c.hora);
            }
        }
    }

    public static void verTodasCitas() {
        for (int i = 0; i < citas.size(); i++) {
            Cita c = citas.get(i);
            System.out.println(i + ": " + c.paciente + " - " + c.medico + " - " + c.fecha);
        }
    }

    public static void eliminarCita() {
        System.out.print("Indice: ");
        int i = sc.nextInt();

        if (i >= 0 && i < citas.size()) {
            citas.remove(i);
            System.out.println("Eliminada");
        } else {
            System.out.println("No existe");
        }
    }

    //  VALIDACIONES
    public static boolean validarFecha(String fecha) {
        try {
            LocalDate f = LocalDate.parse(fecha);
            return !f.isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validarUsuario(String user) {
        return user.matches("[a-zA-Z]+");
    }

    public static boolean validarPassword(String pass) {
        return pass.length() >= 5 && pass.length() <= 10;
    }

    //  ESPECIALIDADES
    public static String elegirEspecialidad() {
        for (int i = 0; i < ESPECIALIDADES.length; i++) {
            System.out.println((i+1) + ". " + ESPECIALIDADES[i]);
        }
        int op = sc.nextInt();
        sc.nextLine();

        while(op < 1 || op > ESPECIALIDADES.length) {
            System.out.println("Opción inválida:");
            op = sc.nextInt();
            sc.nextLine();
        }

        return ESPECIALIDADES[op-1];
    }

    //  MÉDICOS
    public static String elegirMedico(String esp) {
        List<String> lista = doctores.get(esp);

        for (int i = 0; i < lista.size(); i++) {
            System.out.println((i+1) + ". " + lista.get(i));
        }

        int op = sc.nextInt();
        sc.nextLine();

        while(op < 1 || op > lista.size()) {
            System.out.println("Opción inválida:");
            op = sc.nextInt();
            sc.nextLine();
        }

        return lista.get(op-1);
    }

    public static void cargarDoctores() {
        doctores.put("Neurologia", Arrays.asList("Dr. Perez", "Dra. Lopez", "Dr. Ruiz"));
        doctores.put("Medico General", Arrays.asList("Dr. A", "Dr. B", "Dr. C"));
        doctores.put("Pediatria", Arrays.asList("Dra. Mora", "Dr. Diaz", "Dr. Vega"));
        doctores.put("Ginecologia", Arrays.asList("Dra. Luna", "Dra. Paz", "Dra. Rios"));
        doctores.put("Cardiologia", Arrays.asList("Dr. Castro", "Dra. Leon", "Dr. Silva"));
        doctores.put("Psicologia", Arrays.asList("Dra. Mena", "Dr. Ruiz", "Dra. Soto"));
        doctores.put("Traumatologia", Arrays.asList("Dr. Torres", "Dr. Salas", "Dr. Vela"));
    }

    //  CSV
    public static void guardarUsuario(String tipo, String data) {
        try {
            FileWriter fw = new FileWriter("usuarios.csv", true);
            fw.write(tipo + "," + data + "\n");
            fw.close();
        } catch (IOException e) {
            System.out.println("Error guardando usuario");
        }
    }

    public static void guardarCita(Cita c) {
        try {
            FileWriter fw = new FileWriter("citas.csv", true);
            fw.write(c.paciente + "," + c.medico + "," + c.especialidad + "," + c.fecha + "," + c.hora + "\n");
            fw.close();
        } catch (IOException e) {
            System.out.println("Error guardando cita");
        }
    }
}