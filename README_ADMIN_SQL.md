# Admin 사용자 SQL 생성 가이드

## 비밀번호 해시 생성 방법

Admin 사용자(`admin` / `qwer!234`)를 생성하기 위한 SQL 파일을 만들었습니다.

### 방법 1: 온라인 BCrypt 생성기 사용 (가장 간단)

1. https://bcrypt-generator.com/ 접속
2. 비밀번호 입력: `qwer!234`
3. Rounds: `10` (기본값)
4. "Generate Hash" 클릭
5. 생성된 해시값을 복사
6. `src/main/resources/db/migration/V999__insert_admin_user.sql` 파일의 `password_hash` 값에 붙여넣기

### 방법 2: Java 코드로 생성 (Gradle 빌드 필요)

1. Gradle Wrapper 생성 (처음 한 번만):
   ```powershell
   gradle wrapper
   ```

2. 프로젝트 빌드:
   ```powershell
   .\gradlew.bat build
   ```

3. 비밀번호 해시 생성:
   ```powershell
   java -cp "build/libs/*-fat.jar" com.csms.util.PasswordHasher
   ```

4. 출력된 해시값을 SQL 파일에 복사

### SQL 파일 위치

`src/main/resources/db/migration/V999__insert_admin_user.sql`

### 사용된 BCrypt 라이브러리

- 라이브러리: `org.mindrot:jbcrypt:0.4`
- Salt Rounds: 10 (기본값)
- 해시 형식: `$2a$10$...`

### 주의사항

- BCrypt 해시는 매번 다른 salt를 사용하므로, 생성할 때마다 다른 해시값이 나옵니다.
- 모든 해시값은 같은 비밀번호를 검증할 수 있습니다.
- SQL 파일의 `TODO` 주석을 제거하고 실제 해시값으로 교체하세요.

