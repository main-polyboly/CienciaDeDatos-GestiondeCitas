class Paciente extends Usuario {

    public Paciente(String username, String password) {
        super(username, password);
    }

    @Override
    public void menu() {
        System.out.println("1. Agendar cita");
        System.out.println("2. Ver mis citas");
    }
}