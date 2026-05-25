import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Seguridad — hashing de contraseñas con SHA-256 + salt.
 * Sin librerías externas: usa java.security (JDK estándar).
 *
 * Formato guardado en .cred:
 *   username=SALT:HASH
 *
 * Donde SALT y HASH están en Base64.
 */
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

    /**
     * Genera la cadena completa SALT:HASH lista para guardar.
     * Ejemplo: "abc123==:xyz789=="
     */
    public static String generarCredencial(String password) {
        String salt = generarSalt();
        String hash = hashear(password, salt);
        return salt + ":" + hash;
    }

    /**
     * Verifica si password coincide con la credencial almacenada.
     * credencial tiene formato "SALT:HASH"
     */
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

    /**
     * Detecta si una credencial está en texto plano (legacy).
     * Las credenciales hasheadas siempre contienen ":" y tienen >40 chars.
     */
    public static boolean esTextoPlano(String credencial) {
        return credencial == null || !credencial.contains(":") || credencial.length() < 40;
    }
}
