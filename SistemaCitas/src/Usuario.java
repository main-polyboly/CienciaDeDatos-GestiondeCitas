abstract class Usuario {
    protected String username;
    protected String password;

    public Usuario(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public boolean login(String pass) {
        return this.password.equals(pass);
    }

    public abstract void menu();
}