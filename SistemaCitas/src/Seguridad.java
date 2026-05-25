import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

//implementa los hash para evitar que las contraseñas sean rbadas, y esten seguras, tecnicamente evitamos brechas de inseguridad :)
public class Seguridad {

    private static final int SALT_BYTES = 16;

    /** Genera un salt aleatorio de 16 bytes en Base64 */
    public static String generarSalt() {
        SecureRandom rng = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        rng.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Hash SHA-256 de (salt + password), devuelve Base64 */
    public static String hashear(String password, String saltB64) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] salt = Base64.getDecoder().decode(saltB64);
            md.update(salt);
            byte[] hashed = md.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Error en hash: " + e.getMessage());
        }
    }

    //Genera la cadena completa SALT:HASH lista para guardar.
    
    public static String generarCredencial(String password) {
        String salt = generarSalt();
        String hash = hashear(password, salt);
        return salt + ":" + hash;
    }

    /// Verifica si password coincide con la credencial almacenada.
    
    public static boolean verificar(String password, String credencial) {
        if (credencial == null || !credencial.contains(":")) {
            // Compatibilidad retroactiva: credencial en texto plano (migración)
            return credencial != null && credencial.equals(password);
        }
        int sep = credencial.indexOf(':');
        String salt = credencial.substring(0, sep);
        String hashAlmacenado = credencial.substring(sep + 1);
        String hashIntento = hashear(password, salt);
        return hashAlmacenado.equals(hashIntento);
    }

    //detecta si la credencial esta en un texto plano.
    public static boolean esTextoPlano(String credencial) {
        return credencial == null || !credencial.contains(":") || credencial.length() < 40;
    }
}
