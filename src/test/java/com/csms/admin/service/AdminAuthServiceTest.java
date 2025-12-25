package com.csms.admin.service;

import com.csms.admin.dto.AdminLoginDto;
import com.csms.admin.repository.AdminRepository;
import com.csms.common.HandlerTestBase;
import com.csms.common.utils.RateLimiter;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class AdminAuthServiceTest extends HandlerTestBase {
    
    private static RateLimiter rateLimiter;
    private AdminAuthService adminAuthService;
    private JsonObject jwtConfig;
    
    public AdminAuthServiceTest() {
        super("/api/admin");
    }
    
    @BeforeEach
    void setUp(Vertx vertx) {
        // test용 config.json에서 test 환경 설정 로드
        String configContent = vertx.fileSystem().readFileBlocking("src/test/resources/config.json").toString();
        JsonObject fullConfig = new JsonObject(configContent);
        JsonObject config = fullConfig.getJsonObject("test");
        jwtConfig = config.getJsonObject("jwt");
        JsonObject redisConfig = config.getJsonObject("redis");
        
        // RateLimiter 설정 (Redis가 없어도 동작하도록 null 허용)
        if (rateLimiter == null) {
            try {
                RedisOptions redisOptions = new RedisOptions()
                    .setConnectionString("redis://" + redisConfig.getString("host") + ":" + redisConfig.getInteger("port"));
                if (redisConfig.getString("password") != null && !redisConfig.getString("password").isEmpty()) {
                    redisOptions.setPassword(redisConfig.getString("password"));
                }
                Redis redis = Redis.createClient(vertx, redisOptions);
                rateLimiter = new RateLimiter(RedisAPI.api(redis));
            } catch (Exception e) {
                // Redis 연결 실패 시 null로 설정 (RateLimiter는 null 체크 필요)
                rateLimiter = null;
            }
        }
        
        AdminRepository adminRepository = new AdminRepository();
        
        adminAuthService = new AdminAuthService(
            pool,
            adminRepository,
            jwtAuth,
            jwtConfig,
            rateLimiter
        );
    }
    
    @Test
    void testLogin_Success(VertxTestContext context) {
        // Given - 테스트 데이터에 admin1이 있어야 함 (V1000__insert_test_dummy_data.sql)
        AdminLoginDto dto = new AdminLoginDto();
        dto.setId("admin1");
        dto.setPassword("password123");
        
        // When
        adminAuthService.login(dto, "127.0.0.1")
            .onSuccess(response -> {
                // Then
                assertThat(response).isNotNull();
                assertThat(response.getAdminId()).isEqualTo("admin1");
                assertThat(response.getAccessToken()).isNotNull();
                context.completeNow();
            })
            .onFailure(error -> {
                context.failNow(error);
            });
    }
    
    @Test
    void testLogin_InvalidCredentials(VertxTestContext context) {
        // Given - 잘못된 비밀번호
        AdminLoginDto dto = new AdminLoginDto();
        dto.setId("admin1");
        dto.setPassword("wrong-password");
        
        // When
        adminAuthService.login(dto, "127.0.0.1")
            .onSuccess(response -> context.failNow("Should fail with invalid credentials"))
            .onFailure(error -> {
                // Then
                assertThat(error).isNotNull();
                assertThat(error.getMessage()).contains("Invalid admin credentials");
                context.completeNow();
            });
    }
    
    @Test
    void testLogin_AdminNotFound(VertxTestContext context) {
        // Given - 존재하지 않는 관리자
        AdminLoginDto dto = new AdminLoginDto();
        dto.setId("nonexistent_admin");
        dto.setPassword("password123");
        
        // When
        adminAuthService.login(dto, "127.0.0.1")
            .onSuccess(response -> context.failNow("Should fail with admin not found"))
            .onFailure(error -> {
                // Then
                assertThat(error).isNotNull();
                assertThat(error.getMessage()).contains("Invalid admin credentials");
                context.completeNow();
            });
    }
}
