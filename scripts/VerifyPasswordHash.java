import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt 해시 검증 스크립트
 * 
 * 사용법:
 * javac -cp "build/libs/*" scripts/VerifyPasswordHash.java
 * java -cp "build/libs/*;scripts" VerifyPasswordHash
 */
public class VerifyPasswordHash {
    public static void main(String[] args) {
        String password = "password123";
        String hash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        System.out.println("========================================");
        System.out.println("BCrypt 해시 검증");
        System.out.println("========================================");
        System.out.println("비밀번호: " + password);
        System.out.println("해시값: " + hash);
        System.out.println();
        
        boolean matches = BCrypt.checkpw(password, hash);
        System.out.println("검증 결과: " + (matches ? "✓ 일치" : "✗ 불일치"));
        System.out.println();
        
        if (!matches) {
            System.out.println("올바른 해시 생성 중...");
            String correctHash = BCrypt.hashpw(password, BCrypt.gensalt());
            System.out.println("새 해시값: " + correctHash);
            System.out.println();
            System.out.println("SQL UPDATE 문:");
            System.out.println("UPDATE users SET password_hash = '" + correctHash + "' WHERE login_id = 'admin1';");
        }
    }
}

