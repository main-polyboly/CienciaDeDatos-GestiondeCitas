public abstract class Usuario {
    protected String username;
    protected String correo;
    protected String password;

    public Usuario(String username, String correo, String password) {
        this.username = username;
        this.correo   = correo;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getCorreo()   { return correo;   }

    /**
     * Verifica contraseña soportando hash SALT:HASH y texto plano (legado).
     */
    public boolean login(String pass) {
        if (password == null) return false;
        // Hash moderno: SALT:HASH
        if (password.contains(":") && password.length() >= 40) {
            return Seguridad.verificar(pass, password);
        }
        // Legado: texto plano
        return this.password.equals(pass);
    }

    /** Acepta username O correo como identificador al hacer login */
    public boolean coincideIdentificador(String id) {
        return username.equalsIgnoreCase(id) || correo.equalsIgnoreCase(id);
    }

    public abstract void menu();
}