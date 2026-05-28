public class Administrador extends Usuario {

    public Administrador(String username, String correo, String password) {
        super(username, correo, password);
    }

    @Override
    public void menu() {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║      MENU — ADMINISTRADOR        ║");
        System.out.println("╠══════════════════════════════════╣");
        System.out.println("║  1. Ver todas las citas          ║");
        System.out.println("║  2. Eliminar una cita            ║");
        System.out.println("║  3. Ver todos los historiales    ║");
        System.out.println("╚══════════════════════════════════╝");
    }
}