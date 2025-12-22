import org.mindrot.jbcrypt.BCrypt;

/**
 * Admin 비밀번호 해시 생성 스크립트
 * 
 * 사용법:
 * 1. 프로젝트 의존성 다운로드: gradle build (또는 IDE에서 프로젝트 로드)
 * 2. 컴파일: javac -cp "build/libs/*" scripts/GeneratePasswordHash.java
 * 3. 실행: java -cp "build/libs/*;scripts" GeneratePasswordHash
 * 
 * 또는 온라인 BCrypt 생성기 사용:
 * https://bcrypt-generator.com/
 * 비밀번호: qwer!234
 */
public class GeneratePasswordHash {
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
        System.out.println("검증:");
        System.out.println("========================================");
        boolean matches = BCrypt.checkpw(password, hash);
        System.out.println("비밀번호 검증 결과: " + (matches ? "성공" : "실패"));
    }
}

