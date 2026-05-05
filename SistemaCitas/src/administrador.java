class Administrador extends Usuario {

    public Administrador(String username, String password) {
        super(username, password);
    }

    @Override
    public void menu() {
        System.out.println("1. Ver todas las citas");
        System.out.println("2. Eliminar cita");
    }
}