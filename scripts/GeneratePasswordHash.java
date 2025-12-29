import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt 해시 생성 스크립트
 */
public class GeneratePasswordHash {
    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : "password123";
        
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        
        System.out.println("========================================");
        System.out.println("BCrypt 해시 생성");
        System.out.println("========================================");
        System.out.println("비밀번호: " + password);
        System.out.println("해시값: " + hash);
        System.out.println();
        System.out.println("검증:");
        boolean matches = BCrypt.checkpw(password, hash);
        System.out.println("검증 결과: " + (matches ? "✓ 일치" : "✗ 불일치"));
    }
}
