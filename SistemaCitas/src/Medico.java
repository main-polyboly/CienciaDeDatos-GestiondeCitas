class Medico extends Usuario {
    private String especialidad;

    public Medico(String username, String password, String especialidad) {
        super(username, password);
        this.especialidad = especialidad;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    @Override
    public void menu() {
        System.out.println("1. Ver citas asignadas");
    }
}