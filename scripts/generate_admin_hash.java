import org.mindrot.jbcrypt.BCrypt;

public class generate_admin_hash {
    public static void main(String[] args) {
        String password = "qwer!234";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("\nSQL INSERT statement:");
        System.out.println("INSERT INTO users (login_id, password_hash, role, status, created_at, updated_at)");
        System.out.println("VALUES ('admin', '" + hash + "', 2, 'ACTIVE', NOW(), NOW())");
        System.out.println("ON CONFLICT (login_id) DO NOTHING;");
    }
}

