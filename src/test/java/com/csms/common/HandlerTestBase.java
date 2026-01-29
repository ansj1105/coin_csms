package com.csms.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.csms.MainVerticle;
import com.csms.common.dto.ApiResponse;
import com.csms.common.enums.UserRole;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class HandlerTestBase {
    
    protected static int port = 8089;
    protected static io.vertx.sqlclient.SqlClient sqlClient;
    protected static JWTAuth jwtAuth;
    protected static WebClient webClient;
    protected static ObjectMapper objectMapper;
    protected static Flyway flyway;
    
    protected final String apiUrl;
    
    public HandlerTestBase(String baseUrl) {
        this.apiUrl = baseUrl;
    }
    
    @BeforeAll
    protected static void deployVerticle(final Vertx vertx, final VertxTestContext testContext) throws IOException {
        log.info("Test deployVerticle start");
        ensureTestEncryptionKey();
        
        webClient = WebClient.create(vertx);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        
        // test용 config.json에서 test 환경 설정 로드
        String configContent = vertx.fileSystem().readFileBlocking("src/test/resources/config.json").toString();
        JsonObject fullConfig = new JsonObject(configContent);
        JsonObject config = fullConfig.getJsonObject("test");
        
        if (config == null) {
            throw new IllegalStateException("Test environment config not found in src/test/resources/config.json");
        }
        
        // 환경 변수나 시스템 프로퍼티로 데이터베이스 설정 오버라이드
        JsonObject dbConfig = overrideDatabaseConfig(config.getJsonObject("database"));
        
        // Flyway 설정도 업데이트 (URL만 database 설정에서, user/password는 flyway 설정 그대로 사용)
        JsonObject flywayConfig = config.getJsonObject("flyway");
        if (flywayConfig != null && dbConfig != null) {
            String dbHost = dbConfig.getString("host", "localhost");
            Integer dbPort = dbConfig.getInteger("port", 5432);
            String dbName = dbConfig.getString("database");
            // URL만 database 설정에서 가져오고, user/password는 flyway 설정의 값을 유지
            // flyway 설정에 user/password가 없을 때만 database 설정 사용
            flywayConfig.put("url", String.format("jdbc:postgresql://%s:%d/%s", dbHost, dbPort, dbName));
            if (!flywayConfig.containsKey("user")) {
                flywayConfig.put("user", dbConfig.getString("user"));
            }
            if (!flywayConfig.containsKey("password")) {
                flywayConfig.put("password", dbConfig.getString("password"));
            }
        }
        
        JsonObject jwtConfig = config.getJsonObject("jwt");
        
        // JWT Auth 설정
        jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
            .addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("HS256")
                .setBuffer(jwtConfig.getString("secret"))));
        
        // PgPool 설정
        PgConnectOptions connectOptions = new PgConnectOptions()
            .setHost(dbConfig.getString("host"))
            .setPort(dbConfig.getInteger("port"))
            .setDatabase(dbConfig.getString("database"))
            .setUser(dbConfig.getString("user"))
            .setPassword(dbConfig.getString("password"));
        
        PoolOptions poolOptions = new PoolOptions()
            .setMaxSize(dbConfig.getInteger("pool_size", 5));
        
        sqlClient = PgPool.client(vertx, connectOptions, poolOptions);
        
        testContext.verify(() -> {
            // Flyway 설정
            configureFlyway(flywayConfig);
            
            // MainVerticle 배포 (test용 config.json과 test 환경 지정)
            vertx.deployVerticle(
                new MainVerticle("src/test/resources/config.json", "test"),
                testContext.succeedingThenComplete()
            );
        });
    }
    
    @BeforeEach
    protected void init(Vertx vertx, VertxTestContext testContext) {
        testContext.verify(() -> {
            try {
                migration();
                testContext.completeNow();
            } catch (Exception e) {
                testContext.failNow(e);
            }
        });
    }
    
    @AfterAll
    protected static void close(final Vertx vertx) {
        if (sqlClient != null) {
            sqlClient.close();
        }
        vertx.close();
    }
    
    /**
     * Access Token 생성 (테스트용)
     */
    protected String getAccessToken(Long userId, UserRole role) {
        JsonObject payload = new JsonObject()
            .put("userId", userId.toString())
            .put("role", role.name());
        
        return jwtAuth.generateToken(payload, new JWTOptions().setExpiresInMinutes(30));
    }
    
    /**
     * Admin Access Token 생성
     */
    protected String getAccessTokenOfAdmin(Long userId) {
        return getAccessToken(userId, UserRole.ADMIN);
    }
    
    /**
     * User Access Token 생성
     */
    protected String getAccessTokenOfUser(Long userId) {
        return getAccessToken(userId, UserRole.USER);
    }
    
    /**
     * URL 생성
     */
    protected String getUrl(String path) {
        return apiUrl + path;
    }
    
    /**
     * GET 요청
     */
    protected HttpRequest<Buffer> reqGet(String url) {
        return withDeviceHeaders(webClient.get(port, "localhost", url));
    }
    
    /**
     * POST 요청
     */
    protected HttpRequest<Buffer> reqPost(String url) {
        return withDeviceHeaders(webClient.post(port, "localhost", url));
    }
    
    /**
     * PUT 요청
     */
    protected HttpRequest<Buffer> reqPut(String url) {
        return withDeviceHeaders(webClient.put(port, "localhost", url));
    }
    
    /**
     * DELETE 요청
     */
    protected HttpRequest<Buffer> reqDelete(String url) {
        return withDeviceHeaders(webClient.delete(port, "localhost", url));
    }
    
    /**
     * PATCH 요청
     */
    protected HttpRequest<Buffer> reqPatch(String url) {
        return withDeviceHeaders(webClient.patch(port, "localhost", url));
    }

    private HttpRequest<Buffer> withDeviceHeaders(HttpRequest<Buffer> request) {
        return request
            .putHeader("X-Device-Id", "test-device-default")
            .putHeader("X-Device-Type", "WEB")
            .putHeader("X-Device-Os", "WEB");
    }
    
    /**
     * 성공 응답 검증
     */
    protected void expectSuccess(HttpResponse<Buffer> res) {
        assertEquals(HttpResponseStatus.OK.code(), res.statusCode());
    }
    
    /**
     * 성공 응답 검증 및 데이터 추출
     */
    protected <T> T expectSuccessAndGetResponse(HttpResponse<Buffer> res, TypeReference<ApiResponse<T>> typeRef) {
        assertEquals(HttpResponseStatus.OK.code(), res.statusCode());
        try {
            ApiResponse<T> response = objectMapper.readValue(res.bodyAsString(), typeRef);
            assertEquals("OK", response.getStatus());
            return response.getData();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response", e);
        }
    }
    
    /**
     * 에러 응답 검증
     */
    protected void expectError(HttpResponse<Buffer> res, int expectedStatusCode) {
        assertEquals(expectedStatusCode, res.statusCode());
    }
    
    /**
     * 데이터베이스 설정 오버라이드 (환경 변수 또는 시스템 프로퍼티)
     */
    protected static JsonObject overrideDatabaseConfig(JsonObject dbConfig) {
        if (dbConfig == null) {
            return null;
        }
        
        String dbHost = System.getProperty("test.db.host", System.getenv("TEST_DB_HOST"));
        String dbPort = System.getProperty("test.db.port", System.getenv("TEST_DB_PORT"));
        String dbName = System.getProperty("test.db.database", System.getenv("TEST_DB_DATABASE"));
        String dbUser = System.getProperty("test.db.user", System.getenv("TEST_DB_USER"));
        String dbPassword = System.getProperty("test.db.password", System.getenv("TEST_DB_PASSWORD"));
        
        // null이 아니고 빈 문자열이 아닐 때만 설정을 오버라이드
        if (dbHost != null && !dbHost.trim().isEmpty()) {
            dbConfig.put("host", dbHost);
        }
        if (dbPort != null && !dbPort.trim().isEmpty()) {
            try {
                dbConfig.put("port", Integer.parseInt(dbPort.trim()));
            } catch (NumberFormatException e) {
                // 포트 번호가 올바르지 않으면 기본값 유지
                log.warn("Invalid port number in test.db.port: {}, using default", dbPort);
            }
        }
        if (dbName != null && !dbName.trim().isEmpty()) {
            dbConfig.put("database", dbName);
        }
        if (dbUser != null && !dbUser.trim().isEmpty()) {
            dbConfig.put("user", dbUser);
        }
        if (dbPassword != null && !dbPassword.trim().isEmpty()) {
            dbConfig.put("password", dbPassword);
        }
        
        return dbConfig;
    }
    
    private static void configureFlyway(JsonObject flywayConfig) {
        String jdbcUrl = flywayConfig.getString("url");
        String user = flywayConfig.getString("user");
        String password = flywayConfig.getString("password");
        
        flyway = Flyway.configure()
            .dataSource(jdbcUrl, user, password)
            .locations(
                "filesystem:src/test/resources/db/migration",
                "filesystem:src/test/resources/db/seed"
            )
            .cleanDisabled(false)
            .load();
    }
    
    /**
     * Flyway 마이그레이션 실행 (테스트마다 DB 초기화)
     */
    private void migration() {
        log.debug("TEST migration - clean and migrate");
        flyway.clean();  // 테스트마다 DB 초기화
        flyway.migrate();
    }

    private static void ensureTestEncryptionKey() {
        String key = System.getenv("ENCRYPTION_KEY");
        if (key == null || key.isBlank()) {
            System.setProperty("ENCRYPTION_KEY", "test_encryption_key");
        }
    }
}

