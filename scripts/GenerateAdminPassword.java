import org.mindrot.jbcrypt.BCrypt;

/**
 * Admin 비밀번호 해시 생성 유틸리티
 * 
 * 사용법:
 * 1. 프로젝트를 빌드합니다: ./gradlew build
 * 2. 이 파일을 컴파일하고 실행합니다:
 *    javac -cp "build/libs/*" scripts/GenerateAdminPassword.java
 *    java -cp "build/libs/*;scripts" GenerateAdminPassword
 * 
 * 또는 Gradle로 실행:
 * ./gradlew run --args="scripts.GenerateAdminPassword"
 */
public class GenerateAdminPassword {
    public static void main(String[] args) {
        String password = "qwer!234";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        
        System.out.println("========================================");
        System.out.println("Admin 비밀번호 해시 생성");
        System.out.println("========================================");
        System.out.println("아이디: admin");
        System.out.println("비밀번호: " + password);
        System.out.println("해시값: " + hash);
        System.out.println("\n========================================");
        System.out.println("SQL INSERT 문:");
        System.out.println("========================================");
        System.out.println();
        System.out.println("INSERT INTO users (");
        System.out.println("    login_id,");
        System.out.println("    password_hash,");
        System.out.println("    role,");
        System.out.println("    status,");
        System.out.println("    created_at,");
        System.out.println("    updated_at");
        System.out.println(") VALUES (");
        System.out.println("    'admin',");
        System.out.println("    '" + hash + "',");
        System.out.println("    2, -- ADMIN 역할");
        System.out.println("    'ACTIVE',");
        System.out.println("    NOW(),");
        System.out.println("    NOW()");
        System.out.println(")");
        System.out.println("ON CONFLICT (login_id) DO NOTHING;");
        System.out.println();
        System.out.println("========================================");
    }
}

